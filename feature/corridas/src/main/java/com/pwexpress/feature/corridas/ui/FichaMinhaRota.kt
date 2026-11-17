package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.tentar
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private fun quando(s: String?): String = s?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) }.getOrNull() } ?: "—"
private fun hm(s: String?): String? = s?.let { runCatching { Instant.parse(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) }.getOrNull() }
private fun JsonObject.sim(k: String) = (this[k] as? JsonPrimitive)?.content == "true"

/** Tudo de uma rota que o motoboy fez: mapa com o percurso, paradas com horários, pagamento e ocorrências. */
@Composable
fun FichaMinhaRota(id: String, onFechar: () -> Unit) {
    val c = Pw.cores
    var f by remember(id) { mutableStateOf<JsonObject?>(null) }
    var erro by remember(id) { mutableStateOf<String?>(null) }
    LaunchedEffect(id) {
        tentar {
            val r = PwCore.rpc.chamar("minha_ficha_rota", buildJsonObject { put("p_corrida", id) })
            if (!r.ok) error("Não deu para abrir esta rota agora.")
            Json.parseToJsonElement(r.corpo) as? JsonObject
        }.onSuccess { f = it }.onFailure { erro = it.message }
    }
    val feito = com.pwexpress.core.mapa.rememberTrilhaCorrida(id, false)
    val x = f
    PwJanela(x?.let { "Rota " + (if (it.sim("teste")) "T" else "#") + (it.long("numero") ?: "") } ?: "Rota", onFechar) {
        PwErro(erro)
        if (x == null) { if (erro == null) PwCarregando(); return@PwJanela }
        val concluida = x.str("status") == "concluida"
        Row {
            PwSelo(if (concluida) "Concluída" else "Cancelada", if (concluida) c.verde else c.texto3)
            Spacer(Modifier.weight(1f))
            x.dbl("valor")?.let { Text(moedaBr(it), color = c.texto, fontWeight = FontWeight.Bold, fontSize = 17.sp) }
        }
        Text(listOfNotNull(x.str("cliente"), x.int("distancia_m")?.let { "%.1f km".format(it / 1000.0) }, "aceita ${quando(x.str("aceita_em"))}",
            x.str("concluida_em")?.let { "concluída ${quando(it)}" }).joinToString(" · "), color = c.texto2, fontSize = 13.sp)
        val paradas = x.arr("paradas")
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp))) {
            PwMapa(paradas = paradas.mapNotNull { p -> p.dbl("lat")?.let { la -> p.dbl("lng")?.let { lo -> PinoMapa(la, lo, p.str("tipo") == "coleta", false, p.str("status") == "concluida") } } },
                rota = x.str("rota_planejada"), minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(240.dp), enquadrar = id,
                trilhas = com.pwexpress.core.mapa.trilhasDoFeito(feito, null), caminhoAuto = false)
        }
        PwRotulo("Paradas")
        paradas.forEach { p ->
            val coleta = p.str("tipo") == "coleta"
            PwCartao {
                Text("${(p.int("ordem") ?: 0) + 1}. ${com.pwexpress.core.perfil.Modalidades.nomeParada(coleta, x.str("tipo_pedido"))}" +
                    (p.long("pedido_numero")?.let { " · pedido #$it" } ?: ""), color = c.texto, fontWeight = FontWeight.Bold)
                Text(p.str("endereco").orEmpty(), color = c.texto2, fontSize = 13.sp)
                Text(listOfNotNull(hm(p.str("chegou_em"))?.let { "chegou $it" }, hm(p.str("concluida_em"))?.let { "concluiu $it" },
                    p.str("recebedor_nome")?.let { (if (coleta) "entregou " else "recebeu ") + it },
                    "com foto".takeIf { p.sim("tem_foto") }, "com assinatura".takeIf { p.sim("tem_assinatura") },
                    "liberada pelo gestor".takeIf { p.sim("liberada_local") }, "fora do raio".takeIf { p.sim("local_ignorado") }, "sem internet".takeIf { p.sim("feita_offline") },
                ).joinToString(" · ").ifEmpty { "—" }, color = c.texto3, fontSize = 12.sp)
            }
        }
        val calc = x.obj("calculo")
        if (x.dbl("valor") != null) {
            PwRotulo("Pagamento")
            PwCartao {
                calc?.arr("linhas")?.forEach { l ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(l.str("rotulo").orEmpty(), color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(moedaBr(l.dbl("valor")), color = c.texto, fontSize = 13.sp)
                    }
                }
                x.dbl("ajuste")?.takeIf { it != 0.0 }?.let { a ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("Ajuste" + (x.str("ajuste_motivo")?.let { " ($it)" } ?: ""), color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(moedaBr(a), color = c.texto, fontSize = 13.sp)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Text("Total", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(moedaBr(x.dbl("valor")), color = c.ouro, fontWeight = FontWeight.Bold)
                }
            }
        }
        x.arr("chamados").takeIf { it.isNotEmpty() }?.let { ch ->
            PwRotulo("Seus pedidos ao gestor")
            ch.forEach { h -> Text((if (h.str("tipo") == "pular") "Liberar parada" else "Cancelar") + " · ${h.str("status")} · “${h.str("motivo")}”" + (h.str("resposta")?.let { " → $it" } ?: ""), color = c.texto2, fontSize = 13.sp) }
        }
        x.arr("avaliacoes").takeIf { it.isNotEmpty() }?.let { av ->
            PwRotulo("Avaliações")
            av.forEach { a -> Text("★".repeat(a.int("nota") ?: 0) + (a.str("comentario")?.let { " · $it" } ?: ""), color = c.texto2, fontSize = 13.sp) }
        }
        Spacer(Modifier.width(1.dp))
    }
}
