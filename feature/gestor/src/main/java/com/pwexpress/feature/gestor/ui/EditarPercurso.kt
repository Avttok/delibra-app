package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.Navegacao
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.mapa.codificarPolyline
import com.pwexpress.core.mapa.decodificarPolyline
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Ponto "passar por" e em qual trecho (entre a parada [trecho] e a seguinte) ele fica. */
private data class Via(val lat: Double, val lng: Double, val trecho: Int, val t: Double)

/** Distância (graus, só para comparar) do ponto ao segmento a→b e a posição relativa nele (0..1). */
private fun projecao(p: Pair<Double, Double>, a: Pair<Double, Double>, b: Pair<Double, Double>): Pair<Double, Double> {
    val k = kotlin.math.cos(Math.toRadians(a.first))
    val ax = a.second * k; val ay = a.first; val bx = b.second * k; val by = b.first; val px = p.second * k; val py = p.first
    val dx = bx - ax; val dy = by - ay
    val len = dx * dx + dy * dy
    val t = if (len == 0.0) 0.0 else (((px - ax) * dx + (py - ay) * dy) / len).coerceIn(0.0, 1.0)
    val qx = ax + t * dx; val qy = ay + t * dy
    return kotlin.math.hypot(px - qx, py - qy) to t
}

/**
 * Editar percurso: toque no mapa para o caminho passar por aquela rua (o ponto entra no trecho
 * mais perto, entre duas paradas); toque num ponto para tirar. O caminho é refeito pelas ruas
 * a cada mudança. Salvar grava o traçado que o motoboy e o mapa mostram.
 */
@Composable
internal fun EditarPercursoG(corrida: CorridaG, onFechar: () -> Unit) {
    val c = Pw.cores
    val paradas = remember(corrida.id) { corrida.paradas.sortedBy { it.ordem } }
    val pontos = paradas.map { it.lat to it.lng }
    val vias = remember(corrida.id) { mutableStateListOf<Via>() }
    var polyline by remember { mutableStateOf<String?>(null) }
    var distancia by remember { mutableStateOf<Double?>(null) }
    var duracao by remember { mutableStateOf<Double?>(null) }
    var calculando by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val escopo = androidx.compose.runtime.rememberCoroutineScope()

    // abre com os pontos salvos antes (se houver)
    LaunchedEffect(corrida.id) {
        Gestao.rpcJson("corrida_percurso_via", buildJsonObject { put("p_corrida", corrida.id) }).getOrNull()?.let { j ->
            (j as? JsonArray)?.forEach { e ->
                val o = e as? kotlinx.serialization.json.JsonObject ?: return@forEach
                val la = (o["lat"] as? JsonPrimitive)?.content?.toDoubleOrNull(); val lo = (o["lng"] as? JsonPrimitive)?.content?.toDoubleOrNull()
                val tr = (o["trecho"] as? JsonPrimitive)?.content?.toIntOrNull(); val t = (o["t"] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: 0.5
                if (la != null && lo != null && tr != null) vias.add(Via(la, lo, tr, t))
            }
        }
    }

    val chave = vias.joinToString("|") { "${it.lat},${it.lng}" }
    LaunchedEffect(chave) {
        if (pontos.size < 2) return@LaunchedEffect
        if (vias.isEmpty()) { polyline = null; distancia = null; return@LaunchedEffect }
        calculando = true
        delay(250)
        // paradas na ordem com os pontos "passar por" encaixados em cada trecho
        val seq = buildList {
            pontos.forEachIndexed { i, p ->
                if (i > 0) addAll(vias.filter { it.trecho == i - 1 }.sortedBy { it.t }.map { it.lat to it.lng })
                add(p)
            }
        }
        val r = Navegacao.rota(seq.first(), seq.last(), seq.subList(1, seq.size - 1))
        calculando = false
        if (r == null) { erro = "O mapa não respondeu. Tente de novo em instantes."; return@LaunchedEffect }
        erro = null
        polyline = codificarPolyline(r.pontos); distancia = r.distanciaM; duracao = r.duracaoS
    }

    PwJanela("Editar percurso", onFechar, fecharFora = !salvando) {
        TextoDica("Toque numa rua para o caminho passar por ela. Toque num ponto branco para tirar. As paradas continuam na mesma ordem.")
        val pinos = paradas.mapIndexed { i, p -> PinoMapa(p.lat, p.lng, p.coleta, false, p.status == "concluida", numero = i + 1) }
        val marcas = vias.mapIndexed { i, v -> MarcadorMapa("via_$i", v.lat, v.lng, "#FFFFFF", "", pino = false, prioridade = 3) }
        Box(Modifier.fillMaxWidth().height(420.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, c.borda, RoundedCornerShape(18.dp))) {
            PwMapa(paradas = pinos, rota = polyline ?: corrida.rota, minhaPosicao = null, modifier = Modifier.fillMaxSize(),
                marcadores = marcas, caminhoAuto = corrida.rota == null && polyline == null, maximizavel = false, enquadrar = corrida.id, legenda = false,
                onToque = { la, lo ->
                    if (pontos.size < 2) return@PwMapa
                    // trecho mais perto do toque
                    var melhor = 0; var dist = Double.MAX_VALUE; var tt = 0.5
                    for (i in 0 until pontos.size - 1) {
                        val (d, t) = projecao(la to lo, pontos[i], pontos[i + 1])
                        if (d < dist) { dist = d; melhor = i; tt = t }
                    }
                    if (vias.size >= 20) { PwToast.mostrar("Até 20 pontos por rota.", com.pwexpress.core.designsystem.TipoAviso.AVISO); return@PwMapa }
                    vias.add(Via(la, lo, melhor, tt))
                },
                onMarcador = { id -> id.removePrefix("via_").toIntOrNull()?.let { if (it in vias.indices) vias.removeAt(it) } })
        }
        val antes = corrida.distanciaPlanejadaM
        PwLinhaInfo(PwIcones.Route, "Distância", when {
            calculando -> "calculando…"
            distancia != null -> "${numeroBr(distancia!! / 1000, 1)} km" + (antes?.let { " (antes ${numeroBr(it / 1000.0, 1)} km)" } ?: "")
            antes != null -> "${numeroBr(antes / 1000.0, 1)} km"
            else -> "—"
        })
        PwLinhaInfo(PwIcones.MapPin, "Pontos de passagem", "${vias.size}")
        PwErro(erro)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwBotaoVidro("Desfazer", onClick = { if (vias.isNotEmpty()) vias.removeAt(vias.lastIndex) }, icone = PwIcones.RotateCcw, habilitado = vias.isNotEmpty() && !salvando, modifier = Modifier.weight(1f))
            PwBotaoVidro("Limpar", onClick = { vias.clear() }, icone = PwIcones.Trash2, habilitado = vias.isNotEmpty() && !salvando, modifier = Modifier.weight(1f))
        }
        PwBotaoOuro(if (salvando) "Salvando…" else "Salvar percurso", icone = PwIcones.Check, habilitado = !salvando && !calculando && polyline != null, onClick = {
            val pl = polyline ?: return@PwBotaoOuro
            salvando = true
            escopo.launch {
                Gestao.rpcJson("definir_percurso_rota", buildJsonObject {
                    put("p_corrida", corrida.id); put("p_polyline", pl)
                    distancia?.let { put("p_distancia_m", it) }; duracao?.let { put("p_duracao_s", it) }
                    put("p_via", buildJsonArray { vias.forEach { v -> add(buildJsonObject { put("lat", v.lat); put("lng", v.lng); put("trecho", v.trecho); put("t", v.t) }) } })
                }).onSuccess { PwToast.mostrar("O motoboy já vê o caminho novo.", titulo = "Percurso salvo"); Gestao.atualizar(); onFechar() }
                    .onFailure { erro = it.message }
                salvando = false
            }
        })
        if (corrida.rota != null) TextoDica("Sem pontos de passagem, fica o caminho que o sistema calculou.")
    }
}

@Suppress("unused") private fun usoDecodificar(s: String) = decodificarPolyline(s)
