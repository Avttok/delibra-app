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
    PwCartao {
        PwRotulo("Meus percursos")
        Spacer(Modifier.height(8.dp))
        com.pwexpress.core.mapa.SeletorDia(dia, { dia = it })
        Spacer(Modifier.height(10.dp))
        val r = regs
        val forma = RoundedCornerShape(18.dp)
        Box(Modifier.fillMaxWidth().height(240.dp).clip(forma).border(1.dp, c.borda, forma)) {
            PwMapa(
                paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(240.dp),
                percursos = r.orEmpty().mapIndexed { i, g -> PercursoMapa.de(g, destaque = destaque == i, apagado = destaque != null && destaque != i) },
                percursoTeste = teste, enquadrar = "$dia-${r?.size}-$destaque",
                marcadores = r.orEmpty().flatMap { it.pontos }.let { pts -> listOfNotNull(pts.firstOrNull()?.let { com.pwexpress.core.mapa.MarcadorMapa("ini", it.lat, it.lng, "#FFFFFF", "Início", pino = false) }) },
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    r.withIndex().reversed().forEach { (i, g) ->
                        val sel = destaque == i
                        val f = RoundedCornerShape(12.dp)
                        Row(
                            Modifier.fillMaxWidth().clip(f).background(if (sel) c.ouro.copy(alpha = 0.16f) else c.vidro).border(1.dp, if (sel) c.ouro else c.borda, f)
                                .clickable { destaque = if (sel) null else i }.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(if (g.corrida) PwIcones.Package else PwIcones.Search, null, tint = if (g.corrida) c.ouro else c.texto2, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (g.corrida) "Corrida" else "Procurando corrida", color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("${h(g.inicio)}–${h(g.fim)} · ${String.format(Locale("pt", "BR"), "%.1f km", g.metros / 1000)}", color = c.texto3, fontSize = 12.sp)
                        }
                    }
                    Text("Total: ${String.format(Locale("pt", "BR"), "%.1f km", r.sumOf { it.metros } / 1000)}", color = c.texto2, fontSize = 12.5.sp)
                }
            }
        }
    }
}
