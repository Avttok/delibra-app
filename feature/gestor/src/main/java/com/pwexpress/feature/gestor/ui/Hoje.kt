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
            TextoDica("${moedaBr(d.dbl("gestao_hoje"))} para a empresa · ${d.dbl("km_hoje") ?: 0.0} km")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Numero("Rotas", "${n("concluidas_hoje")}", "${n("rotas_ativas")} andando · ${n("rotas_esperando")} esperando", PwIcones.Route, Modifier.weight(1f))
            Numero("Motoboys", "${n("motoboys_online")}", "online · ${n("motoboys_em_rota")} em rota", PwIcones.Bike, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Numero("Pedidos", "${n("pedidos_hoje")}", "hoje · ${n("para_revisar")} para revisar", PwIcones.ClipboardList, Modifier.weight(1f))
            Numero("Nota (30 dias)", d.dbl("nota_media_30d")?.let { String.format(java.util.Locale("pt", "BR"), "%.1f", it) } ?: "—", "média das avaliações", PwIcones.Star, Modifier.weight(1f))
        }
        PwCartao {
            PwRotulo("Rotas concluídas por hora")
            val horas = d.arr("por_hora").associate { (it.int("hora") ?: 0) to (it.int("rotas") ?: 0) }
            val max = (horas.values.maxOrNull() ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                (6..23).forEach { h ->
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().fillMaxHeight(((horas[h] ?: 0).toFloat() / max).coerceAtLeast(0.02f)).clip(RoundedCornerShape(4.dp)).background(c.ouro))
                        }
                        Text(if (h % 3 == 0) "${h}h" else "", color = c.texto3, fontSize = 9.sp)
                    }
                }
            }
        }
        val top = d.arr("top_motoboys")
        PwCartao {
            PwRotulo("Quem mais entregou hoje")
            if (top.isEmpty()) TextoDica("Nenhuma rota concluída ainda.")
            top.forEachIndexed { i, m ->
                Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                    Text("${i + 1}. ${m.str("nome").orEmpty()}", color = c.texto, modifier = Modifier.weight(1f))
                    Text("${m.int("rotas") ?: 0}", color = c.ouro, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Numero(rotulo: String, valor: String, dica: String, icone: ImageVector, modifier: Modifier) {
    val c = Pw.cores
    PwCartao(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(icone, null, tint = c.ouro, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp)); PwRotulo(rotulo) }
        Text(valor, color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 28.sp)
        Text(dica, color = c.texto3, fontSize = 12.sp, fontFamily = Archivo)
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
