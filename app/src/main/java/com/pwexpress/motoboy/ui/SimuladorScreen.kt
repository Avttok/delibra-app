package com.pwexpress.motoboy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwPasso
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.mapa.decodificarPolyline
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.StatusParada
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Simulador
import com.pwexpress.feature.tracking.Tracking

/**
 * Simulador de GPS (só no modo teste): o "motoboy de mentira" anda pela rota ou até onde você tocar,
 * mandando pings como o celular faria. Serve para ver o rastreamento suave e testar chegadas.
 */
@Composable
fun SimuladorScreen(onFechar: () -> Unit, kmhPadrao: Double) {
    val c = Pw.cores
    val est by Simulador.estado.collectAsState()
    val info by Tracking.info.collectAsState()
    val ativa by Corridas.ativa.collectAsState()
    var toqueModo by remember { mutableIntStateOf(0) } // 0 = andar até, 1 = pular para
    var alvo by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    val pos = est.posicao ?: info.ultimaPosicao

    val pendentes = ativa?.paradas?.filter { it.status != StatusParada.CONCLUIDA }.orEmpty()
    val proxima = pendentes.firstOrNull()

    PwTelaCheia("Simulador de GPS", onFechar, subtitulo = "Modo teste · o GPS de verdade fica pausado enquanto ligado") {
        Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(20.dp))) {
            PwMapa(
                paradas = ativa?.paradas?.map { PinoMapa(it.lat, it.lng, it.tipo == TipoParada.COLETA, it.id == proxima?.id, it.status == StatusParada.CONCLUIDA) }.orEmpty(),
                rota = ativa?.rotaPlanejada, minhaPosicao = null,
                marcadores = listOfNotNull(
                    pos?.let { MarcadorMapa("sim", it.first, it.second, "#A78BFA", "Eu", pino = true) },
                    alvo?.let { MarcadorMapa("alvo", it.first, it.second, "#F59E0B", "", pino = false) },
                ),
                seguir = if (est.andando) "sim" else null,
                enquadrar = ativa?.id ?: "sim",
                inicio = pos, zoomInicio = 15.0,
                onToque = { la, lo ->
                    alvo = la to lo
                    if (!est.ativo) Simulador.ligar(pos ?: (la to lo), kmhPadrao)
                    if (toqueModo == 1) Simulador.teletransportar(la, lo) else Simulador.andar(listOf(la to lo))
                },
                modifier = Modifier.fillMaxWidth().height(300.dp),
            )
        }
        PwCartao {
            PwInterruptor("Simulador ligado", est.ativo, { lig -> if (lig) Simulador.ligar(pos, kmhPadrao) else { Simulador.desligar(); alvo = null } })
            if (info.modo == Modo.OFFLINE) Text("Fique online para os pings serem enviados.", color = c.vermelho, fontSize = 13.sp)
            Text(
                if (!est.ativo) "Toque no mapa ou escolha uma ação abaixo."
                else if (est.andando) "Andando · faltam ${if (est.restanteM < 1000) "${est.restanteM} m" else "%.1f km".format(est.restanteM / 1000.0)} · ${est.enviados} pings"
                else "Parado · ${est.enviados} pings enviados",
                color = c.texto2, fontFamily = Archivo, fontSize = 13.5.sp,
            )
            if (est.andando) Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(c.borda)) {
                Box(Modifier.fillMaxWidth(est.progresso.coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(99.dp)).background(c.gradOuro))
            }
        }
        PwRotulo("Ao tocar no mapa")
        PwSegmentado(listOf("Andar até lá" to PwIcones.Route, "Pular para lá" to PwIcones.MapPin), toqueModo, { toqueModo = it })
        PwRotulo("Velocidade e ritmo")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwPasso(est.kmh, { Simulador.velocidade(it) }, Modifier.weight(1f), passo = 5.0, min = 5.0, max = 120.0, unidade = "km/h")
            PwPasso(est.envioS.toDouble(), { Simulador.ritmo(it.toInt()) }, Modifier.weight(1f), passo = 1.0, min = 2.0, max = 60.0, unidade = "s/ping")
        }
        if (ativa != null) {
            PwRotulo("Rota ${ativa?.numeroExibicao}")
            proxima?.let { p ->
                PwBotaoOuro("Ir até a próxima parada", onClick = {
                    if (!est.ativo) Simulador.ligar(pos, kmhPadrao)
                    val trecho = ativa?.rotaPlanejada?.let { trechoAte(decodificarPolyline(it), est.posicao ?: pos, p.lat to p.lng) }
                    Simulador.andar(trecho ?: listOf(p.lat to p.lng))
                }, icone = PwIcones.Navigation)
            }
            PwBotaoVidro("Fazer a rota inteira", onClick = {
                if (!est.ativo) Simulador.ligar(pos, kmhPadrao)
                val linha = ativa?.rotaPlanejada?.let { decodificarPolyline(it) }.orEmpty()
                Simulador.andar(if (linha.size >= 2) linha else pendentes.map { it.lat to it.lng })
            }, icone = PwIcones.Route, modifier = Modifier.fillMaxWidth())
        }
        if (est.andando) PwBotaoVidro("Parar aqui", onClick = { Simulador.parar() }, icone = PwIcones.X, modifier = Modifier.fillMaxWidth())
    }
}

/** Pedaço da rota desenhada do ponto mais perto de [de] até o mais perto de [ate]. */
private fun trechoAte(linha: List<Pair<Double, Double>>, de: Pair<Double, Double>?, ate: Pair<Double, Double>): List<Pair<Double, Double>>? {
    if (linha.size < 2) return null
    fun d(a: Pair<Double, Double>, b: Pair<Double, Double>) = (a.first - b.first).let { it * it } + (a.second - b.second).let { it * it }
    val i = de?.let { p -> linha.indices.minByOrNull { d(linha[it], p) } } ?: 0
    val j = linha.indices.minByOrNull { d(linha[it], ate) } ?: return null
    if (j <= i) return listOf(ate)
    return linha.subList(i, j + 1) + listOf(ate)
}
