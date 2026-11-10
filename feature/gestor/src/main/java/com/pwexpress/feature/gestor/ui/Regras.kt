package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwPasso
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.feature.gestor.data.GRUPOS_CONFIG
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PARAMETROS
import com.pwexpress.feature.gestor.data.PARAMETROS_VISIVEIS
import com.pwexpress.feature.gestor.data.Param
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Texto curto do valor de uma regra ("ligado", "R$ 2,00", "Quadro"…). */
internal fun textoValor(p: Param, v: JsonElement?): String {
    val prim = v as? JsonPrimitive
    return when (p.tipo) {
        "booleano" -> if ((prim?.content?.toBooleanStrictOrNull() ?: p.padraoBool)) "ligado" else "desligado"
        "escolha" -> (prim?.content ?: p.padraoTxt)?.let { t -> p.opcoesTxt.firstOrNull { it.first == t }?.second ?: t } ?: "—"
        "opcoes" -> (prim?.content?.toDoubleOrNull() ?: p.padraoNum).let { n -> p.opcoes.firstOrNull { it.first == n }?.second ?: numeroBr(n) }
        "multi" -> ((v as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content } ?: p.padraoTxt.orEmpty().split(",").filter { it.isNotBlank() })
            .map { k -> p.opcoesTxt.firstOrNull { it.first == k }?.second ?: k }.ifEmpty { listOf("nenhum") }.joinToString(", ")
        "texto" -> prim?.content ?: p.padraoTxt ?: "—"
        else -> (prim?.content?.toDoubleOrNull() ?: p.padraoNum).let { n ->
            if (p.unidade == "R$") "R$ ${numeroBr(n)}" else numeroBr(n) + (p.unidade?.let { " $it" } ?: "")
        }
    }
}

/**
 * Um parâmetro com o controle certo para o tipo dele: interruptor, passo numérico, opções ou escolha.
 * Igual ao site (o catálogo é gerado de lá).
 */
@Composable
internal fun CampoParametro(p: Param, valor: JsonElement?, onSalvar: (JsonElement) -> Unit, bloqueado: Boolean = false, extra: (@Composable () -> Unit)? = null) {
    val c = Pw.cores
    val prim = valor as? JsonPrimitive
    if (p.tipo == "multi") {
        // vários ao mesmo tempo: o valor é uma lista
        val atuais = (valor as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }
            ?: if (valor == null) p.padraoTxt.orEmpty().split(",").filter { it.isNotBlank() } else emptyList()
        Text(p.rotulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (p.descricao.isNotEmpty()) TextoDica(p.descricao)
        Espaco(8)
        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            p.opcoesTxt.forEach { (k, r) ->
                val on = k in atuais
                com.pwexpress.core.designsystem.PwPilula(r, if (on) com.pwexpress.core.designsystem.PwIcones.Check else com.pwexpress.core.designsystem.PwIcones.Plus, {
                    if (!bloqueado) onSalvar(kotlinx.serialization.json.JsonArray((if (on) atuais - k else atuais + k).map { JsonPrimitive(it) }))
                }, ligada = on)
            }
        }
        TextoDica(if (atuais.isEmpty()) "Nenhum marcado" else "${atuais.size} marcado${if (atuais.size > 1) "s" else ""}")
        if (bloqueado) TextoDica("Só o Dev altera.")
        extra?.invoke()
        return
    }
    if (p.tipo == "numero" && "raio" in p.chave && (p.unidade == "m" || p.unidade == "km")) {
        Text(p.rotulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (p.descricao.isNotEmpty()) TextoDica(p.descricao)
        com.pwexpress.core.mapa.CampoRaio(
            (valor as? JsonPrimitive)?.content?.toDoubleOrNull() ?: p.padraoNum, p.unidade, p.min ?: 0.0, p.max ?: (if (p.unidade == "km") 50.0 else 5000.0), p.passo,
            { if (!bloqueado) onSalvar(JsonPrimitive(it)) }, habilitado = !bloqueado, dupla = "entrega" in p.chave,
        )
        if (bloqueado) TextoDica("Só o Dev altera.")
        extra?.invoke()
        return
    }
    if (p.tipo == "booleano") {
        val lig = prim?.content?.toBooleanStrictOrNull() ?: p.padraoBool
        PwInterruptor(p.rotulo, lig, { if (!bloqueado) onSalvar(JsonPrimitive(it)) }, descricao = (if (lig) p.descricao else p.descricaoDesligado ?: p.descricao).ifEmpty { null })
    } else {
        Text(p.rotulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (p.descricao.isNotEmpty()) TextoDica(p.descricao)
        Espaco(8)
        when (p.tipo) {
            "escolha" -> {
                val atual = prim?.content ?: p.padraoTxt
                if (p.opcoesTxt.size <= 3) PwSegmentado(p.opcoesTxt.map { it.second to null }, p.opcoesTxt.indexOfFirst { it.first == atual }.coerceAtLeast(0),
                    { i -> if (!bloqueado) onSalvar(JsonPrimitive(p.opcoesTxt[i].first)) })
                else PwSelecao("", p.opcoesTxt.map { it.first to it.second }, atual, { v -> if (!bloqueado) onSalvar(JsonPrimitive(v)) })
            }
            "texto" -> {
                val salvo = prim?.content ?: p.padraoTxt ?: ""
                var txt by androidx.compose.runtime.remember(salvo) { androidx.compose.runtime.mutableStateOf(salvo) }
                com.pwexpress.core.designsystem.PwCampo(txt, { txt = it }, "", dica = p.exemplo, habilitado = !bloqueado)
                if (txt != salvo && !bloqueado) com.pwexpress.core.designsystem.PwBotaoVidro("Salvar", { onSalvar(JsonPrimitive(txt.trim())) }, icone = com.pwexpress.core.designsystem.PwIcones.Check)
            }
            "opcoes" -> {
                val atual = prim?.content?.toDoubleOrNull() ?: p.padraoNum
                if (p.opcoes.size <= 4) PwSegmentado(p.opcoes.map { it.second to null }, p.opcoes.indexOfFirst { it.first == atual }.coerceAtLeast(0), { i -> if (!bloqueado) onSalvar(JsonPrimitive(p.opcoes[i].first)) })
                else PwSelecao("", p.opcoes.map { it.first to it.second }, atual, { v -> if (!bloqueado) onSalvar(JsonPrimitive(v)) })
            }
            else -> {
                val atual = prim?.content?.toDoubleOrNull() ?: p.padraoNum
                PwPasso(atual, { if (!bloqueado) onSalvar(JsonPrimitive(it)) }, Modifier.fillMaxWidth(), passo = p.passo, min = p.min ?: 0.0, max = p.max ?: 1e9, unidade = p.unidade)
            }
        }
    }
    if (bloqueado) TextoDica("Só o Dev altera.")
    extra?.invoke()
}

/**
 * Lista de regras com exceção (ou o padrão), por grupo. Serve para cliente, motoboy, tipo de pedido e pedido.
 * [valores] = exceções já salvas; [onDefinir] com null volta ao padrão.
 */
@Composable
internal fun EditorRegrasG(params: List<Param>, valores: Map<String, JsonElement>, onDefinir: (String, JsonElement?) -> Unit, busca: String = "") {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val souDev = com.pwexpress.core.perfil.Perfis.perfil.collectAsState().value?.dev == true
    // organizado como os Ajustes: áreas, seções e "só as exceções"
    var area by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var secao by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var soExc by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    fun sem(t: String) = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
    val grupoArea = GRUPOS_CONFIG.associate { it.chave to it.area }
    val termo = sem(busca.trim())
    val areas = com.pwexpress.feature.gestor.data.AREAS_CONFIG.filter { a -> params.any { grupoArea[it.grupo] == a.chave } }
    if (termo.isEmpty()) {
        com.pwexpress.core.designsystem.PwFiltros(listOf("Tudo" to params.count { it.chave in valores }) + areas.map { a -> a.titulo to params.count { grupoArea[it.grupo] == a.chave && it.chave in valores } },
            areas.indexOfFirst { it.chave == area } + 1, { i -> area = if (i == 0) "" else areas[i - 1].chave; secao = "" })
        val secoes = GRUPOS_CONFIG.filter { it.area == area && params.any { p -> p.grupo == it.chave } }
        if (area.isNotEmpty() && secoes.size > 1) Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            com.pwexpress.core.designsystem.PwPilula("Todas", PwIcones.Lista, { secao = "" }, ligada = secao.isEmpty())
            secoes.forEach { g -> com.pwexpress.core.designsystem.PwPilula(g.titulo, PwIcones.SlidersHorizontal, { secao = if (secao == g.chave) "" else g.chave }, ligada = secao == g.chave) }
        }
    }
    com.pwexpress.core.designsystem.PwInterruptor("Só as exceções", soExc, { soExc = it }, descricao = "${params.count { it.chave in valores }} regra(s) diferentes do padrão")
    var lista = if (termo.isNotEmpty()) params.filter { sem("${it.rotulo} ${it.descricao}").contains(termo) }
        else params.filter { (area.isEmpty() || grupoArea[it.grupo] == area) && (secao.isEmpty() || it.grupo == secao) }
    if (soExc) lista = lista.filter { it.chave in valores }
    if (lista.isEmpty()) { PwVazio(if (soExc) "Nenhuma exceção aqui: tudo usa o padrão." else "Nenhuma regra encontrada.", icone = PwIcones.Search); return }
    GRUPOS_CONFIG.forEach { g ->
        val doGrupo = lista.filter { it.grupo == g.chave }
        if (doGrupo.isEmpty()) return@forEach
        Espaco(4)
        Text((if (area.isEmpty() || termo.isNotEmpty()) "${com.pwexpress.feature.gestor.data.AREAS_CONFIG.find { it.chave == g.area }?.titulo ?: ""} · " else "") + g.titulo,
            color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        TextoDica(g.descricao)
        doGrupo.forEach { p ->
            val tem = p.chave in valores
            PwCartao(padding = PaddingValues(14.dp), cor = if (tem) c.ouro.copy(alpha = 0.55f) else null) {
                CampoParametro(p, if (tem) valores[p.chave] else dados.config[p.chave], { onDefinir(p.chave, it) }, bloqueado = p.soDev && !souDev) {
                    Espaco(6)
                    if (tem) Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Exceção · padrão: ${textoValor(p, dados.config[p.chave])}", color = c.ouro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (!(p.soDev && !souDev)) Text("usar o padrão", color = c.texto2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { onDefinir(p.chave, null) }.padding(6.dp))
                    } else Text("Usando o padrão · mude para criar a exceção", color = c.texto3, fontSize = 12.sp)
                }
            }
        }
    }
}

/** As peças do lego de um cliente, motoboy ou tipo de pedido (salva na hora). */
@Composable
internal fun RegrasDoAlvoG(escopo: String, alvo: String, nome: String) {
    val escopoCo = rememberCoroutineScope()
    val valores = remember(escopo, alvo) { mutableStateMapOf<String, JsonElement>() }
    var carregado by remember(escopo, alvo) { mutableStateOf(false) }
    var busca by remember { mutableStateOf("") }
    LaunchedEffect(escopo, alvo) { valores.clear(); valores.putAll(Gestao.politicas(escopo, alvo)); carregado = true }
    TextoDica("Regras só de $nome · ${if (valores.isEmpty()) "tudo no padrão" else "${valores.size} diferente${if (valores.size == 1) "" else "s"} do padrão"}. " +
        when (escopo) { "cliente" -> "Valem para todos os pedidos dele (um pedido pode ter regras próprias)."; "motoboy" -> "Valem para todas as rotas dele."; else -> "Valem para todos os pedidos deste tipo." })
    PwCampo(busca, { busca = it }, "Buscar regra")
    if (!carregado) { com.pwexpress.core.designsystem.PwCarregando(); return }
    EditorRegrasG(PARAMETROS_VISIVEIS.filter { escopo in it.escopos }, valores, { chave, v ->
        escopoCo.launch {
            Gestao.definirPolitica(escopo, alvo, chave, v).onSuccess {
                if (v == null) valores.remove(chave) else valores[chave] = v
            }.onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
        }
    }, busca)
}

/** Exceções de todo mundo: escolha cliente, motoboy ou tipo e mude só o que for diferente. */
@Composable
internal fun ExcecoesG(onVoltar: () -> Unit) {
    val dados by Gestao.dados.collectAsState()
    var escopo by remember { mutableIntStateOf(0) }
    var alvo by remember { mutableStateOf<String?>(null) }
    var tipos by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var contagem by remember { mutableStateOf<Map<Pair<String, String>, Int>>(emptyMap()) }
    LaunchedEffect(Unit) {
        tipos = Gestao.catalogoPedido().first.map { it.chave to it.nome }
        contagem = Gestao.contagemPoliticas()
    }
    val esc = listOf("cliente", "motoboy", "tipo")[escopo]
    val alvos = when (esc) {
        "cliente" -> dados.empresas.map { it.id to it.nome }
        "motoboy" -> dados.equipe.filter { it.motoboy }.map { it.id to it.nome }
        else -> tipos
    }
    LaunchedEffect(esc, alvos.size) { if (alvos.none { it.first == alvo }) alvo = alvos.firstOrNull()?.first }
    com.pwexpress.core.designsystem.PwPagina {
        Cabecalho("Exceções", onVoltar, subtitulo = "Vence: pedido > motoboy > cliente > tipo > padrão")
        PwSegmentado(listOf("Cliente" to PwIcones.Building2, "Motoboy" to PwIcones.Bike, "Tipo" to PwIcones.ClipboardList), escopo, { escopo = it })
        PwSelecao("", alvos.map { (id, n) -> id to (n + (contagem[esc to id]?.let { "  · $it exceç${if (it == 1) "ão" else "ões"}" } ?: "")) }, alvo, { alvo = it })
        val a = alvo
        if (a == null) PwVazio("Nada para escolher aqui ainda.") else RegrasDoAlvoG(esc, a, alvos.firstOrNull { it.first == a }?.second ?: "")
    }
}

/** Regras que fazem sentido num pedido só (preço, junção em rotas, despacho, revisão). */
internal val PARAMS_PEDIDO = PARAMETROS_VISIVEIS.filter { ("cliente" in it.escopos || "tipo" in it.escopos) && it.grupo !in setOf("pagamento", "pag_combinar", "pag_zonas", "pag_km", "pag_entrega", "pag_fixo", "faturas") && !it.soDev }

/** Regras deste pedido (a peça mais alta do lego). Grava na hora e recalcula o orçamento. */
@Composable
internal fun RegrasPedidoG(pedidoId: String?, inicial: Map<String, JsonElement>, onFechar: () -> Unit, onMudar: (Map<String, JsonElement>) -> Unit = {}) {
    val escopo = rememberCoroutineScope()
    val valores = remember(pedidoId) { mutableStateMapOf<String, JsonElement>().apply { putAll(inicial) } }
    var busca by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    PwTelaCheia("Regras deste pedido", onFechar, subtitulo = if (valores.isEmpty()) "Tudo como o cliente/padrão" else "${valores.size} regra(s) própria(s)") {
        TextoDica("Valem só para este pedido e vencem as do cliente, do tipo e o padrão. O orçamento é recalculado ao mudar.")
        PwCampo(busca, { busca = it }, "Buscar regra")
        PwErro(erro)
        EditorRegrasG(PARAMS_PEDIDO, valores, { chave, v ->
            val novo = valores.toMutableMap().apply { if (v == null) remove(chave) else put(chave, v) }
            if (pedidoId == null) { if (v == null) valores.remove(chave) else valores[chave] = v; onMudar(novo); return@EditorRegrasG }
            escopo.launch {
                Gestao.definirRegrasPedido(pedidoId, JsonObject(novo)).onSuccess {
                    if (v == null) valores.remove(chave) else valores[chave] = v
                    erro = null; Gestao.atualizar()
                }.onFailure { erro = it.message }
            }
        }, busca)
    }
}

// ------------------------------------------------------------------ ícones

/** Nomes que o gestor pode escolher (os mesmos do site) e o desenho que o app usa para cada um. */
internal val ICONES_ESCOLHA = listOf(
    "package", "package-2", "box", "file-text", "mail", "gift", "receipt", "store", "shopping-bag", "building-2", "warehouse", "landmark",
    "house", "school", "hospital", "utensils", "pizza", "coffee", "pill", "heart", "smartphone", "laptop", "headphones", "camera",
    "flask-conical", "flame", "droplet", "shield-check", "truck", "bike", "car", "map-pin", "clock", "zap", "star", "crown", "sparkles", "palette", "user-round", "users",
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SeletorIconeG(atual: String?, onEscolher: (String?) -> Unit, onFechar: () -> Unit) {
    val c = Pw.cores
    PwTelaCheia("Escolher ícone", onFechar) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ICONES_ESCOLHA.forEach { n ->
                val sel = n == atual
                Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(if (sel) c.ouro.copy(alpha = 0.18f) else c.vidroSutil)
                    .border(1.dp, if (sel) c.ouro else c.borda, RoundedCornerShape(16.dp)).clickable { onEscolher(n); onFechar() }, contentAlignment = Alignment.Center) {
                    Icon(iconeDoNome(n), n, tint = if (sel) c.ouro else c.texto, modifier = Modifier.size(24.dp))
                }
            }
        }
        com.pwexpress.core.designsystem.PwBotaoVidro("Sem ícone", onClick = { onEscolher(null); onFechar() }, icone = PwIcones.X, modifier = Modifier.fillMaxWidth())
    }
}

/** Botão pequeno que mostra o ícone atual e abre o seletor. */
@Composable
internal fun BotaoIcone(atual: String?, onEscolher: (String?) -> Unit) {
    val c = Pw.cores
    var aberto by remember { mutableStateOf(false) }
    Row(Modifier.clip(RoundedCornerShape(12.dp)).background(c.vidroSutil).border(1.dp, c.borda, RoundedCornerShape(12.dp)).clickable { aberto = true }.padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(iconeDoNome(atual), null, tint = c.ouro, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text("Ícone", color = c.texto2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
    if (aberto) SeletorIconeG(atual, onEscolher, onFechar = { aberto = false })
}

@Suppress("unused") private val semUso = JsonNull
