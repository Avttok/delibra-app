package com.pwexpress.feature.corridas.ui

import com.pwexpress.core.tentar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.mapa.PercursoMapa
import com.pwexpress.core.mapa.PingMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.mapa.RegistroPercurso
import com.pwexpress.core.mapa.coresPercurso
import com.pwexpress.core.mapa.separarRegistros
import com.pwexpress.core.perfil.Perfis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Os percursos do próprio motoboy num dia, separados em registros (procurando / corrida). */
@Composable
fun MeusPercursos() {
    val c = Pw.cores
    val eu by Perfis.perfil.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var dia by remember { mutableIntStateOf(0) }
    var regs by remember { mutableStateOf<List<RegistroPercurso>?>(null) }
    var destaque by remember(dia) { mutableStateOf<Int?>(null) }
    LaunchedEffect(dia, eu?.id, teste) {
        val id = eu?.id ?: return@LaunchedEffect
        regs = null
        val zona = ZoneId.systemDefault()
        val ini = LocalDate.now(zona).minusDays(dia.toLong()).atStartOfDay(zona).toInstant()
        val fim = ini.plusSeconds(86_400)
        // pelo site: todos os pings do dia e o caminho já casado com as ruas
        var tentativas = 0
        while (true) {
            val r = com.pwexpress.core.mapa.PercursosServidor.buscar(ini, fim, id, teste) ?: break
            regs = r.registros(id)
            if (!r.parcial || tentativas++ >= 3) return@LaunchedEffect
            kotlinx.coroutines.delay(4000) // o mapa ainda está traçando algum trecho: busca de novo
        }
        if (regs != null) return@LaunchedEffect
        // site fora do ar: pings direto do banco (linha reta entre eles)
        regs = tentar {
            val r = PwCore.rpc.rest("GET", "/rest/v1/localizacoes?select=lat,lng,status,corrida_id,registrado_em&motoboy_id=eq.$id&teste=eq.$teste&registrado_em=gte.$ini&registrado_em=lt.$fim&order=registrado_em.asc&limit=8000")
            if (!r.ok) emptyList() else separarRegistros(Json.parseToJsonElement(r.corpo).jsonArray.mapNotNull { e ->
                val o = e.jsonObject
                fun t(k: String) = (o[k] as? JsonPrimitive)?.takeIf { it.isString }?.content
                val la = (o["lat"] as? JsonPrimitive)?.doubleOrNull; val lo = (o["lng"] as? JsonPrimitive)?.doubleOrNull
                if (la == null || lo == null) null
                else PingMapa(la, lo, t("status") ?: "", t("corrida_id"), tentar { java.time.OffsetDateTime.parse(t("registrado_em")).toInstant().toEpochMilli() }.getOrDefault(0))
            })
        }.getOrDefault(emptyList())
    }
    val r = regs
    var soRotas by remember { mutableStateOf(false) }
    var todos by remember(dia) { mutableStateOf(false) }
    val br = Locale("pt", "BR")
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Footprints, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("Meus percursos")
        }
        Spacer(Modifier.height(8.dp))
        com.pwexpress.core.mapa.SeletorDia(dia, { dia = it })
        Spacer(Modifier.height(10.dp))
        // resumo do dia
        if (!r.isNullOrEmpty()) {
            val rotas = r.count { it.corrida }
            val kmRota = r.filter { it.corrida }.sumOf { it.metros } / 1000
            val kmDia = r.sumOf { it.metros } / 1000
            val min = r.sumOf { (it.fim - it.inicio).coerceAtLeast(0) } / 60000
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("$rotas" to "rotas", String.format(br, "%.1f", kmRota) to "km em rota", String.format(br, "%.1f", kmDia) to "km no dia",
                    (if (min >= 60) "${min / 60}h${String.format(br, "%02d", min % 60)}" else "${min}min") to "rodando").forEach { (v, rot) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(12.dp)).padding(vertical = 8.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(v, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                        Text(rot, color = c.texto3, fontSize = 10.5.sp, maxLines = 1)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        val forma = RoundedCornerShape(18.dp)
        val pontos = r.orEmpty().flatMap { it.pontos }
        // início (bandeira pequena) e onde terminou, com a SUA foto
        val marcadores = listOfNotNull(
            pontos.firstOrNull()?.let { com.pwexpress.core.mapa.MarcadorMapa("ini", it.lat, it.lng, "#22C55E", "Início", pino = false, prioridade = 1) },
            pontos.lastOrNull()?.let { com.pwexpress.core.mapa.MarcadorMapa("eu", it.lat, it.lng, String.format("#%06X", 0xFFFFFF and c.ouro.toArgb()),
                eu?.nome?.substringBefore(" ") ?: "Você", foto = eu?.fotoUrl, prioridade = 3) },
        )
        Box(Modifier.fillMaxWidth().height(250.dp).clip(forma).border(1.dp, c.borda, forma)) {
            PwMapa(
                paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(250.dp),
                percursos = r.orEmpty().mapIndexed { i, g -> PercursoMapa.de(g, destaque = destaque == i, apagado = destaque != null && destaque != i) },
                percursoTeste = teste, enquadrar = "$dia-${r?.size}-$destaque", marcadores = marcadores, tituloTelaCheia = "Meus percursos",
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Início", color = c.texto3, fontSize = 11.5.sp)
            Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(Brush.horizontalGradient(coresPercurso(teste).map { Color(it) })))
            Text("Fim", color = c.texto3, fontSize = 11.5.sp)
        }
        Spacer(Modifier.height(8.dp))
        when {
            r == null -> PwCarregando(texto = "Buscando seus percursos…")
            r.isEmpty() -> Text("Nenhum percurso neste dia.", color = c.texto2, fontSize = 13.sp)
            else -> {
                val fmt = DateTimeFormatter.ofPattern("HH:mm")
                fun h(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(fmt)
                // parado procurando corrida (menos de 100 m) não ajuda a entender o dia
                val uteis = r.withIndex().filter { (_, g) -> g.corrida || g.metros >= 100 }.filter { (_, g) -> !soRotas || g.corrida }
                val mostrar = (if (todos) uteis else uteis.takeLast(6)).reversed()
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.pwexpress.core.designsystem.PwPilula("Só rotas", PwIcones.Package, { soRotas = !soRotas }, ligada = soRotas)
                    if (uteis.size > 6) com.pwexpress.core.designsystem.PwPilula(if (todos) "Mostrar menos" else "Ver todos (${uteis.size})", PwIcones.Lista, { todos = !todos }, ligada = todos)
                }
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    mostrar.forEach { (i, g) ->
                        val sel = destaque == i
                        val f = RoundedCornerShape(12.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(f).background(if (sel) c.ouro.copy(alpha = 0.16f) else c.vidro).border(1.dp, if (sel) c.ouro else c.borda, f)
                                .clickable { destaque = if (sel) null else i }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(28.dp).clip(RoundedCornerShape(99.dp)).background(if (g.corrida) c.ouro.copy(alpha = 0.18f) else c.vidroForte), contentAlignment = Alignment.Center) {
                                Icon(if (g.corrida) PwIcones.Package else PwIcones.Search, null, tint = if (g.corrida) c.ouro else c.texto2, modifier = Modifier.size(15.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (g.corrida) "Rota" else "Procurando corrida", color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text("${h(g.inicio)} – ${h(g.fim)} · ${(g.fim - g.inicio) / 60000} min", color = c.texto3, fontSize = 11.5.sp, maxLines = 1)
                            }
                            Text(String.format(br, "%.1f km", g.metros / 1000), color = if (g.corrida) c.ouro else c.texto2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    val escondidos = r.size - r.count { it.corrida || it.metros >= 100 }
                    if (escondidos > 0 && !soRotas) Text("$escondidos período${if (escondidos == 1) "" else "s"} parado procurando corrida (menos de 100 m) não aparece${if (escondidos == 1) "" else "m"}.", color = c.texto3, fontSize = 11.5.sp)
                }
            }
        }
    }
}
