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
import androidx.compose.runtime.collectAsState
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
    val atual = JsonObject(regras.toMap())
    val mudou = atual != salvas
    val pode = !bloqueado && com.pwexpress.feature.gestor.data.Permissoes.estado.value.pode("editar_valor_pedido")
    LaunchedEffect(atual.toString()) {
        if (!mudou) { previa = null; return@LaunchedEffect }
        delay(350)
        previa = Gestao.rpcJson("previa_orcamento_pedido", buildJsonObject { put("p_pedido", pedido); put("p_regras", atual) }).getOrNull() as? JsonObject
    }
    val base = previa ?: orc
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
    PecasPrecoG(regras, base)
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

/** As peças do preço (liga, desliga e mexe nos valores). Muda só o mapa [regras]. */
@Composable
internal fun PecasPrecoG(regras: androidx.compose.runtime.snapshots.SnapshotStateMap<String, JsonElement>, base: JsonObject?) {
    val c = Pw.cores
    var aberto by remember { mutableStateOf<String?>(null) }
    fun ligada(p: PecaPreco) = p.desliga.isEmpty() || !p.desliga.all { (k, v) -> regras[k] == v }
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
}

/** Chaves cobertas pelos editores visuais nos Ajustes (somem da lista comum). */
internal val CHAVES_PRECO_VISUAL = PECAS.flatMap { it.chaves }.toSet()

/** Chaves do pagamento do motoboy que o editor visual cobre nos Ajustes. */
internal val CHAVES_PAG_VISUAL: Set<String> get() = PARAMETROS.filter { it.grupo in setOf("pag_combinar", "pag_km", "pag_zonas", "pag_entrega", "pag_fixo") ||
    it.chave in setOf("repasse_motoboy_pct", "pag_volta", "pag_volta_valor_km", "pag_volta_pct") }.map { it.chave }.toSet()

/**
 * Padrão da empresa no mesmo editor das rotas e pedidos (igual ao site): o pagamento do motoboy e o preço do cliente.
 * Cada mudança salva sozinha como ajuste da empresa.
 */
@Composable
internal fun AjusteVisualG(tipo: String, config: Map<String, JsonElement>, padraoDe: (com.pwexpress.feature.gestor.data.Param) -> JsonElement) {
    val c = Pw.cores
    val chaves = if (tipo == "pagamento") CHAVES_PAG_VISUAL else CHAVES_PRECO_VISUAL
    val perm by com.pwexpress.feature.gestor.data.Permissoes.estado.collectAsState()
    val regras = remember { mutableStateMapOf<String, JsonElement>() }
    var antes by remember { mutableStateOf<Map<String, JsonElement>>(emptyMap()) }
    var pronto by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // o que a empresa já mudou do padrão vira "próprio"
        val iniciais = PARAMETROS.filter { it.chave in chaves }.mapNotNull { p -> config[p.chave]?.takeIf { v -> v.toString() != padraoDe(p).toString() }?.let { p.chave to it } }.toMap()
        regras.clear(); regras.putAll(iniciais); antes = iniciais; pronto = true
    }
    val atual = regras.toMap()
    LaunchedEffect(atual.toString()) {
        if (!pronto || atual == antes) return@LaunchedEffect
        delay(500)
        val mudadas = atual.filter { (k, v) -> antes[k] != v }
        val tiradas = antes.keys - atual.keys
        var ok = true
        mudadas.forEach { (k, v) -> if (perm.nivel(k) == "editar") Gestao.salvarConfig(k, v).onFailure { ok = false; com.pwexpress.core.designsystem.PwToast.mostrar(it.message ?: "Erro", com.pwexpress.core.designsystem.TipoAviso.ERRO) } }
        tiradas.forEach { k -> PARAMETROS.firstOrNull { it.chave == k }?.let { p -> if (perm.nivel(k) == "editar") Gestao.salvarConfig(k, padraoDe(p)) } }
        if (ok) { antes = atual; com.pwexpress.core.designsystem.PwToast.mostrar("Salvo.") }
    }
    com.pwexpress.core.designsystem.PwCartao(destaque = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (tipo == "pagamento") PwIcones.HandCoins else PwIcones.Tags, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if (tipo == "pagamento") "Como o motoboy é pago (padrão)" else "Como o pedido é cobrado (padrão)", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        TextoDica(if (tipo == "pagamento") "Ligue os cálculos que entram, mude os valores e escolha como juntar. Cada rota pode mudar depois. Salva sozinho."
            else "Ligue as peças que entram no preço e mude os valores. Cada cliente e pedido podem mudar depois. Salva sozinho.")
        Spacer(Modifier.height(6.dp))
        if (tipo == "pagamento") EditorCalculosG(regras, null, null) else PecasPrecoG(regras, null)
    }
}
