package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.drawText
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.iso
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** Como está o dia: os números que importam agora e o que precisa de você (igual ao "Hoje" do site). */
@Composable
internal fun HojeG(onAba: (String) -> Unit) {
    val c = Pw.cores
    var p by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        while (true) {
            Gestao.rpcJson("painel_do_dia").onSuccess { p = it as? JsonObject; erro = null }.onFailure { erro = it.message }
            delay(30_000)
        }
    }
    PwPagina {
        TituloTela("Hoje", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM", java.util.Locale("pt", "BR"))), PwIcones.LayoutDashboard)
        PwErro(erro)
        val d = p ?: return@PwPagina
        fun n(k: String) = d.int(k) ?: 0
        val alertas = listOfNotNull(
            Triple(PwIcones.Siren, "${n("emergencias")} emergência(s) aberta(s)", "mapa").takeIf { n("emergencias") > 0 },
            Triple(PwIcones.Hand, "${n("chamados")} pedido(s) de motoboy esperando", "operacao").takeIf { n("chamados") > 0 },
            Triple(PwIcones.Alerta, "${n("sem_motoboy")} rota(s) sem motoboy", "operacao").takeIf { n("sem_motoboy") > 0 },
            Triple(PwIcones.ClipboardList, "${n("para_revisar")} pedido(s) para revisar", "operacao").takeIf { n("para_revisar") > 0 },
            Triple(PwIcones.CloudOff, "${n("aguardando_confirmacao")} rota(s) aguardando confirmação", "operacao").takeIf { n("aguardando_confirmacao") > 0 },
            Triple(PwIcones.Star, "${n("notas_ruins_hoje")} avaliação(ões) ruim hoje", "operacao").takeIf { n("notas_ruins_hoje") > 0 },
        )
        if (alertas.isEmpty()) TextoDica("Nada esperando por você agora.")
        alertas.forEachIndexed { i, (ic, t, aba) ->
            val forte = i == 0 && (n("emergencias") > 0 || n("chamados") > 0)
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background((if (forte) c.vermelho else c.ouro).copy(alpha = 0.12f)).clickable { onAba(aba) }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(ic, null, tint = if (forte) c.vermelho else c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(10.dp))
                Text(t, color = c.texto, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(16.dp))
            }
        }
        PwCartao(destaque = true) {
            PwRotulo("Faturado hoje")
            Text(moedaBr(d.dbl("faturamento_hoje")), color = c.ouro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 34.sp)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                MiniValor("Empresa", moedaBr(d.dbl("gestao_hoje")), Modifier.weight(1f))
                MiniValor("Motoboys", moedaBr(d.dbl("pagar_motoboys_hoje")), Modifier.weight(1f))
                MiniValor("Rodado", "${d.dbl("km_hoje") ?: 0.0} km", Modifier.weight(1f))
            }
        }
        val ontem = n("ontem_concluidas")
        val compara = when {
            ontem == 0 && n("concluidas_hoje") == 0 -> "nenhuma ainda"
            ontem == 0 -> "ontem a esta hora: 0"
            else -> { val dif = n("concluidas_hoje") - ontem; if (dif >= 0) "+$dif que ontem a esta hora" else "$dif que ontem a esta hora" }
        }
        Linha2(
            { Numero("Rotas feitas", "${n("concluidas_hoje")}", compara, PwIcones.Route, it) },
            { Numero("Entregas", "${n("entregas_hoje")}", "${n("canceladas_hoje")} rota(s) cancelada(s)", PwIcones.PackageCheck, it) },
        )
        Linha2(
            { Numero("Na rua", "${n("rotas_ativas")}", "${n("rotas_esperando")} esperando motoboy", PwIcones.Bike, it) },
            { Numero("Motoboys", "${n("motoboys_online")}", "online · ${n("motoboys_em_rota")} em rota", PwIcones.Users, it) },
        )
        Linha2(
            { Numero("Pedidos", "${n("pedidos_hoje")}", "${n("para_revisar")} para revisar", PwIcones.ClipboardList, it) },
            { Numero("Tempo médio", d.int("tempo_medio_min")?.let { "$it min" } ?: "—", "do aceite à conclusão", PwIcones.Clock, it) },
        )
        PwCartao {
            val horas = d.arr("por_hora").associate { (it.int("hora") ?: 0) to (it.int("rotas") ?: 0) }
            val agora = d.int("hora_agora") ?: 23
            val ini = minOf(horas.keys.minOrNull() ?: 7, 7).coerceAtLeast(0)
            val fim = maxOf(agora, horas.keys.maxOrNull() ?: 0, ini + 5).coerceAtMost(23)
            val pico = horas.maxByOrNull { it.value }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwRotulo("Rotas concluídas por hora", Modifier.weight(1f))
                if (pico != null && pico.value > 0) Text("Pico às ${pico.key}h (${pico.value})", color = c.texto3, fontSize = 12.sp)
            }
            GraficoHoras(horas, ini, fim, agora)
        }
        val andando = d.arr("em_andamento")
        if (andando.isNotEmpty()) PwCartao {
            PwRotulo("Na rua agora")
            andando.forEach { r ->
                val feitas = r.int("feitas") ?: 0; val total = (r.int("total") ?: 0).coerceAtLeast(1)
                Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("#${r.int("numero") ?: 0}", color = c.ouro, fontWeight = FontWeight.Bold, modifier = Modifier.width(52.dp))
                        Text(r.str("motoboy").orEmpty().ifBlank { "Motoboy" }, color = c.texto, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("$feitas de $total paradas", color = c.texto3, fontSize = 12.sp)
                    }
                    Box(Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.borda)) {
                        Box(Modifier.fillMaxWidth(feitas.toFloat() / total).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(c.ouro))
                    }
                }
            }
        }
        val top = d.arr("top_motoboys")
        PwCartao {
            PwRotulo("Quem mais entregou hoje")
            if (top.isEmpty()) TextoDica("Nenhuma rota concluída ainda.")
            val maxTop = (top.maxOfOrNull { it.int("rotas") ?: 0 } ?: 1).coerceAtLeast(1)
            top.forEachIndexed { i, m ->
                val rotas = m.int("rotas") ?: 0
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}", color = if (i == 0) c.ouro else c.texto3, fontWeight = FontWeight.Black, fontFamily = ArchivoLargo, modifier = Modifier.width(24.dp))
                    Text(m.str("nome").orEmpty(), color = c.texto, modifier = Modifier.width(96.dp), maxLines = 1)
                    Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(c.borda)) {
                        Box(Modifier.fillMaxWidth(rotas.toFloat() / maxTop).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(c.ouro))
                    }
                    Text("$rotas", color = c.ouro, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
            }
        }
    }
}

/** Dois blocos lado a lado, sempre da mesma altura. */
@Composable
private fun Linha2(a: @Composable (Modifier) -> Unit, b: @Composable (Modifier) -> Unit) {
    Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        a(Modifier.weight(1f).fillMaxHeight()); b(Modifier.weight(1f).fillMaxHeight())
    }
}

@Composable
private fun MiniValor(rotulo: String, valor: String, modifier: Modifier) {
    val c = Pw.cores
    Column(modifier) {
        Text(rotulo, color = c.texto3, fontSize = 11.sp, maxLines = 1)
        Text(valor, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
    }
}

/** Barras por hora desenhadas num Canvas: rótulos que não quebram, linha de base e a hora atual marcada. */
@Composable
private fun GraficoHoras(horas: Map<Int, Int>, ini: Int, fim: Int, agora: Int) {
    val c = Pw.cores
    val max = (horas.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val medidor = androidx.compose.ui.text.rememberTextMeasurer()
    val estilo = androidx.compose.ui.text.TextStyle(color = c.texto3, fontSize = 10.sp)
    val estiloV = androidx.compose.ui.text.TextStyle(color = c.texto, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    val n = fim - ini + 1
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(150.dp).padding(top = 10.dp)) {
        val rodape = 18.dp.toPx(); val topo = 14.dp.toPx()
        val area = size.height - rodape - topo
        val passo = size.width / n
        val larg = (passo * 0.62f).coerceAtMost(22.dp.toPx())
        drawLine(c.borda, androidx.compose.ui.geometry.Offset(0f, topo + area), androidx.compose.ui.geometry.Offset(size.width, topo + area), 1.dp.toPx())
        val cada = if (n > 12) 3 else if (n > 7) 2 else 1
        for (k in 0 until n) {
            val h = ini + k
            val v = horas[h] ?: 0
            val cx = passo * k + passo / 2
            val alt = if (v == 0) 2.dp.toPx() else (area * v / max).coerceAtLeast(4.dp.toPx())
            val cor = when { h == agora -> c.ouro; v == 0 -> c.borda; else -> c.ouro.copy(alpha = 0.7f) }
            drawRoundRect(cor, androidx.compose.ui.geometry.Offset(cx - larg / 2, topo + area - alt), androidx.compose.ui.geometry.Size(larg, alt),
                androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
            if (v > 0) {
                val t = medidor.measure(v.toString(), estiloV)
                drawText(t, topLeft = androidx.compose.ui.geometry.Offset(cx - t.size.width / 2f, topo + area - alt - t.size.height - 2f))
            }
            if (k % cada == 0 || h == agora) {
                val t = medidor.measure("${h}h", if (h == agora) estiloV else estilo)
                drawText(t, topLeft = androidx.compose.ui.geometry.Offset((cx - t.size.width / 2f).coerceIn(0f, size.width - t.size.width), size.height - t.size.height))
            }
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, dica: String, icone: ImageVector, modifier: Modifier) {
    val c = Pw.cores
    PwCartao(modifier, padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icone, null, tint = c.ouro, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); PwRotulo(rotulo) }
        Text(valor, color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 28.sp, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text(dica, color = c.texto3, fontSize = 12.sp, fontFamily = Archivo, maxLines = 2, lineHeight = 15.sp)
    }
}

private val TABELA_H = mapOf("pedidos" to "pedido", "corridas" to "rota", "politicas" to "regra", "empresas" to "cliente", "zonas" to "zona",
    "categorias_produto" to "tipo de objeto", "tipos_pedido" to "tipo de pedido", "penalidades" to "penalidade", "chamados_corrida" to "pedido do motoboy", "faixas_horario" to "faixa de horário")
private val ESCONDER_H = setOf("id", "organizacao_id", "criado_em", "atualizado_em", "criada_em", "rota_planejada", "pag_calculo", "orcamento", "referencias")

/** Quem mudou o quê (gestores e supervisores), com o antes e o depois. */
@Composable
internal fun HistoricoG(onVoltar: () -> Unit) {
    val c = Pw.cores
    var busca by remember { mutableStateOf("") }
    var lista by remember { mutableStateOf<List<JsonObject>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(busca) {
        delay(350)
        Gestao.rpcJson("auditoria_lista", buildJsonObject {
            put("p_de", JsonPrimitive(java.time.Instant.now().minus(java.time.Duration.ofDays(30)).toString()))
            if (busca.isNotBlank()) put("p_busca", JsonPrimitive(busca.trim()))
            put("p_limite", JsonPrimitive(300))
        }).onSuccess { lista = (it as? JsonArray)?.mapNotNull { e -> e as? JsonObject }.orEmpty(); erro = null }.onFailure { erro = it.message }
    }
    PwPagina {
        Cabecalho("Histórico de mudanças", onVoltar, subtitulo = "Últimos 30 dias")
        PwCampo(busca, { busca = it }, "Buscar por pessoa, campo ou valor")
        PwErro(erro)
        val l = lista ?: return@PwPagina
        if (l.isEmpty()) TextoDica("Nada mudou nesse período.")
        l.forEach { x ->
            val acao = when (x.str("acao")) { "insert" -> "criou"; "delete" -> "apagou"; else -> "mudou" }
            val antes = x.obj("antes"); val depois = x.obj("depois")
            val num = (depois ?: antes)?.str("numero")?.let { " #$it" } ?: (depois ?: antes)?.str("chave")?.let { " “$it”" } ?: ""
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(when (x.str("acao")) { "insert" -> PwIcones.Plus; "delete" -> PwIcones.Trash2; else -> PwIcones.Pencil }, null, tint = c.ouro, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${x.str("autor") ?: "Alguém"} $acao ${TABELA_H[x.str("tabela")] ?: x.str("tabela")}$num", color = c.texto, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                }
                iso(x.str("quando"))?.let { TextoDica(dataHora(it)) }
                if (x.str("acao") == "update") (depois?.keys.orEmpty() - ESCONDER_H).take(8).forEach { k ->
                    Text("${k.replace('_', ' ')}: ${antes?.get(k)?.let(::curto) ?: "vazio"} → ${depois?.get(k)?.let(::curto) ?: "vazio"}", color = c.texto2, fontSize = 12.5.sp)
                }
            }
        }
    }
}

private fun curto(e: kotlinx.serialization.json.JsonElement): String = when (e) {
    is JsonPrimitive -> when (e.content) { "true" -> "ligado"; "false" -> "desligado"; "null", "" -> "vazio"; else -> e.content.take(60) }
    else -> e.toString().take(60)
}
