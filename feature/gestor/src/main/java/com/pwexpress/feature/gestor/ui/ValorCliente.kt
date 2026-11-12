package com.pwexpress.feature.gestor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PARAMETROS
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** As peças do preço (iguais às do site): liga/desliga zera só neste pedido. */
private data class PecaPreco(val id: String, val nome: String, val dica: String, val icone: () -> ImageVector, val chaves: List<String>,
                             val desliga: Map<String, JsonElement>, val linha: (String?, String) -> Boolean, val pct: (String) -> Boolean = { false })

private val zero = JsonPrimitive(0)
private val PECAS = listOf(
    PecaPreco("saida", "Saída", "Valor fixo de cada pedido.", { PwIcones.Wallet }, listOf("preco_base"), mapOf("preco_base" to zero),
        { ch, r -> ch == "saida" || r == "Saída" || r == "Preço fixo do cliente" }),
    PecaPreco("distancia", "Distância", "Km × valor, pelas ruas ou em linha reta.", { PwIcones.Route },
        listOf("preco_km", "preco_distancia_modo", "orcamento_trajeto", "soma_inclui_saida", "soma_inclui_entrega_extra"), mapOf("preco_km" to zero),
        { ch, r -> ch == "distancia" || ch?.startsWith("entrega_") == true || r == "Distância" }),
    PecaPreco("pontos", "Coletas e entregas a mais", "Cada ponto além do primeiro.", { PwIcones.Waypoints }, listOf("preco_coleta_extra", "preco_entrega_extra"),
        mapOf("preco_coleta_extra" to zero, "preco_entrega_extra" to zero), { _, r -> r == "Coletas extras" || r == "Entregas extras" }),
    PecaPreco("volumes", "Volumes", "Volumes além dos inclusos.", { PwIcones.Boxes }, listOf("preco_pacotes_inclusos", "preco_pacote_extra"),
        mapOf("preco_pacote_extra" to zero), { _, r -> r == "Volumes extras" }),
    PecaPreco("peso", "Peso", "Kg além do incluso (real ou cubado).", { PwIcones.Scale }, listOf("preco_kg_incluso", "preco_kg", "cubagem_fator"),
        mapOf("preco_kg" to zero), { _, r -> r == "Peso excedente" }),
    PecaPreco("perigoso", "Produto perigoso", "Acréscimo e taxa para carga perigosa.", { PwIcones.Flame }, listOf("preco_perigoso_pct", "preco_perigoso_fixo"),
        mapOf("preco_perigoso_pct" to zero, "preco_perigoso_fixo" to zero), { _, r -> r == "Produto perigoso" }, { it == "Produto perigoso" }),
    PecaPreco("pressa", "Express e urgente", "Acréscimos de quem tem pressa.", { PwIcones.Timer }, listOf("preco_express_pct", "preco_urgente_pct"),
        mapOf("preco_express_pct" to zero, "preco_urgente_pct" to zero), { _, _ -> false }, { it == "Express" || it == "Urgente" }),
    PecaPreco("zonas", "Zonas", "Dinâmica de preço das zonas no mapa.", { PwIcones.MapPinned }, listOf("zona_combinar", "zona_cliente_exclusiva"),
        emptyMap(), { _, r -> r.startsWith("Zona") }, { it.startsWith("Zona") }),
    PecaPreco("final", "Mínimo e arredondamento", "O total nunca fica abaixo do mínimo.", { PwIcones.Package }, listOf("valor_minimo", "arredondamento"),
        emptyMap(), { _, _ -> false }),
)

/** Valor cobrado do cliente, no mesmo jeito do pagamento do motoboy (igual ao site). */
@Composable
internal fun ValorClienteG(pedido: String, orc: JsonObject?, regrasIniciais: JsonObject, bloqueado: Boolean, acao: (suspend () -> Result<Unit>) -> Unit) {
    val c = Pw.cores
    val regras = remember(pedido) { mutableStateMapOf<String, JsonElement>().apply { putAll(regrasIniciais) } }
    var salvas by remember(pedido) { mutableStateOf(regrasIniciais) }
    var previa by remember(pedido) { mutableStateOf<JsonObject?>(null) }
    var aberto by remember(pedido) { mutableStateOf<String?>(null) }
    val atual = JsonObject(regras.toMap())
    val mudou = atual != salvas
    val pode = !bloqueado && com.pwexpress.feature.gestor.data.Permissoes.estado.value.pode("editar_valor_pedido")
    LaunchedEffect(atual.toString()) {
        if (!mudou) { previa = null; return@LaunchedEffect }
        delay(350)
        previa = Gestao.rpcJson("previa_orcamento_pedido", buildJsonObject { put("p_pedido", pedido); put("p_regras", atual) }).getOrNull() as? JsonObject
    }
    val base = previa ?: orc
    fun ligada(p: PecaPreco) = p.desliga.isEmpty() || !p.desliga.all { (k, v) -> regras[k] == v }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(PwIcones.Wallet, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        Text("Valor cobrado do cliente", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        if (salvas.isNotEmpty()) PwSelo("SÓ DESTE PEDIDO", c.ouro, icone = PwIcones.SlidersHorizontal)
    }
    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(moedaBr(base?.dbl("total")), color = c.ouro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 32.sp)
        if (previa != null) { Spacer(Modifier.width(8.dp)); Text("antes ${moedaBr(orc?.dbl("total"))}", color = c.texto3, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp)) }
    }
    if (!pode) { TextoDica(if (bloqueado) "Pedido fechado: o preço não muda mais." else "A plataforma não liberou mudar o valor do pedido."); return }
    Spacer(Modifier.height(6.dp))
    PECAS.forEach { p ->
        val on = ligada(p)
        val v = base?.arr("linhas").orEmpty().filter { p.linha(it.str("chave"), it.str("rotulo").orEmpty()) }.sumOf { it.dbl("valor") ?: 0.0 }
        val pct = base?.arr("percentuais").orEmpty().filter { p.pct(it.str("rotulo").orEmpty()) }.sumOf { it.dbl("pct") ?: 0.0 }
        val forma = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(forma).background(if (on) c.ouro.copy(alpha = 0.08f) else c.vidro)
            .border(1.dp, if (on) c.ouro.copy(alpha = 0.45f) else c.borda, forma)) {
            Row(Modifier.fillMaxWidth().clickable { aberto = if (aberto == p.id) null else p.id }.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(p.icone(), null, tint = if (on) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(p.nome, color = if (on) c.texto else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                    Text(listOfNotNull(v.takeIf { it > 0 }?.let { moedaBr(it) }, pct.takeIf { it > 0 }?.let { "+${numeroBr(it)}%" }).joinToString(" · ").ifEmpty { if (on) p.dica else "fora do preço" },
                        color = c.texto3, fontSize = 12.sp)
                }
                Icon(if (aberto == p.id) PwIcones.ChevronUp else PwIcones.ChevronDown, "Valores", tint = c.texto3, modifier = Modifier.size(18.dp))
                if (p.desliga.isNotEmpty()) Switch(on, { liga -> if (liga) p.desliga.keys.forEach { regras.remove(it) } else regras.putAll(p.desliga) },
                    colors = SwitchDefaults.colors(checkedTrackColor = c.ouro, checkedThumbColor = c.fundo))
            }
            AnimatedVisibility(aberto == p.id && on) {
                Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                    EditorRegrasG(PARAMETROS.filter { it.chave in p.chaves && !it.soDev }, regras, { k, x -> if (x == null) regras.remove(k) else regras[k] = x })
                }
            }
        }
    }
    if (mudou || salvas.isNotEmpty()) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwBotaoOuro("Salvar preço", onClick = {
            acao {
                val r = Gestao.rpcJson("definir_regras_pedido", buildJsonObject { put("p_pedido", pedido); put("p_regras", atual) })
                if (r.isSuccess) { salvas = atual; previa = null; Gestao.atualizar() }
                r.map { }
            }
        }, icone = PwIcones.Check, habilitado = mudou, modifier = Modifier.weight(1f))
        if (salvas.isNotEmpty()) PwBotaoVidro("Padrão", onClick = {
            acao {
                val r = Gestao.rpcJson("definir_regras_pedido", buildJsonObject { put("p_pedido", pedido); put("p_regras", JsonObject(emptyMap())) })
                if (r.isSuccess) { salvas = JsonObject(emptyMap()); regras.clear(); Gestao.atualizar() }
                r.map { }
            }
        }, icone = PwIcones.RotateCcw)
    }
    TextoDica("Muda só este pedido. O padrão do cliente fica no cadastro dele, e o geral em Ajustes → Preço.")
}
