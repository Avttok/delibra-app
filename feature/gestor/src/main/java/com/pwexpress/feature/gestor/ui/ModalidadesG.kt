package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.StatusCorrida
import com.pwexpress.feature.gestor.data.StatusPedido
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put

/*
 * Ajustes → Modalidades (igual ao site): campos próprios, nomes próprios, taxas e bônus, cargos.
 */

private fun JsonObject.t(k: String) = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
private fun JsonObject.b(k: String) = (this[k] as? JsonPrimitive)?.booleanOrNull == true
private fun JsonObject.n(k: String) = (this[k] as? JsonPrimitive)?.doubleOrNull
private fun JsonObject.l(k: String) = (this[k] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }
private fun lista(e: JsonElement?) = (e as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
private fun numero(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

private val ERROS_MOD = mapOf(
    "acao_bloqueada" to "Você não tem permissão para isso.", "campo_repetido" to "Já existe um campo com esse nome.",
    "cargo_repetido" to "Já existe um cargo com esse nome.", "so_supervisor" to "Dê o cargo de supervisor à pessoa antes.",
)
private fun erroMod(m: String?) = ERROS_MOD.entries.firstOrNull { m?.contains(it.key) == true }?.value ?: "Não deu agora. Tente de novo."

@Composable
private fun ChipM(texto: String, ligado: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    Text(texto, color = if (ligado) c.fundo else c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(if (ligado) c.ouro else c.campo).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

@Composable
private fun LinhaChips(content: @Composable () -> Unit) =
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { content() }

@Composable
private fun LinhaLista(titulo: String, sub: String, inativa: Boolean = false, onClick: () -> Unit) {
    val c = Pw.cores
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.campo).clickable(onClick = onClick).padding(12.dp)) {
        Text(titulo, color = if (inativa) c.texto3 else c.texto, fontWeight = FontWeight.SemiBold)
        Text(sub, color = c.texto3, fontSize = 12.sp)
    }
}

/** Qual tela de Modalidades abrir (pelo menu de Ajustes). */
@Composable
fun ModalidadeG(qual: String, onVoltar: () -> Unit) {
    PwPagina {
        Cabecalho(when (qual) { "campos" -> "Campos próprios"; "nomes" -> "Nomes próprios"; "taxas" -> "Taxas e bônus"; else -> "Cargos" }, onVoltar)
        when (qual) { "campos" -> CamposG(); "nomes" -> NomesG(); "taxas" -> TaxasG(); else -> CargosG() }
    }
}

// =================================================================== campos próprios
private val ENTIDADES = listOf("pedido" to "No pedido", "ponto" to "Na parada", "cliente" to "No cliente", "motoboy" to "No motoboy")
private val TIPOS_CAMPO = listOf("texto" to "Texto", "numero" to "Número", "dinheiro" to "Dinheiro", "sim_nao" to "Sim/não", "escolha" to "Escolha",
    "data" to "Data", "telefone" to "Telefone", "documento" to "Documento", "foto" to "Foto")
private val QUEM = listOf("qualquer" to "Qualquer um", "cliente" to "Cliente", "gestor" to "Equipe", "motoboy" to "Motoboy")

@Composable
private fun CamposG() {
    var itens by remember { mutableStateOf<List<JsonObject>?>(null) }
    var ed by remember { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(versao) { Gestao.rpcJson("campos_editor").onSuccess { itens = lista(it) }.onFailure { itens = emptyList(); PwToast.mostrar(erroMod(it.message), TipoAviso.ERRO) } }
    TextoDica("Campos a mais no pedido, na parada (o motoboy responde ao concluir), no cliente e no motoboy. Aparecem no app, no site e na API.")
    val l = itens ?: run { PwCarregando(); return }
    ENTIDADES.forEach { (ent, titulo) ->
        PwCartao {
            Row { PwRotulo(titulo, Modifier.weight(1f)); }
            l.filter { it.t("entidade") == ent }.forEach { k ->
                LinhaLista(k.t("rotulo").orEmpty() + if (k.b("obrigatorio")) " *" else "",
                    "${TIPOS_CAMPO.firstOrNull { it.first == k.t("tipo") }?.second} · ${QUEM.firstOrNull { it.first == k.t("quem_preenche") }?.second}${if (k.b("da_plataforma")) " · da plataforma" else ""}",
                    inativa = !k.b("ativo")) { if (!k.b("da_plataforma")) ed = k }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
            }
            PwBotaoVidro("Novo campo", onClick = { ed = buildJsonObject { put("entidade", ent); put("tipo", "texto"); put("ativo", true); put("quem_preenche", if (ent == "ponto") "motoboy" else "qualquer") } }, icone = PwIcones.Plus, modifier = Modifier.fillMaxWidth())
        }
    }
    ed?.let { e -> EditorCampo(e, onFechar = { ed = null }, onSalvo = { ed = null; versao++ }) }
}

@Composable
private fun EditorCampo(inicial: JsonObject, onFechar: () -> Unit, onSalvo: () -> Unit) {
    val escopo = rememberCoroutineScope()
    var rotulo by remember { mutableStateOf(inicial.t("rotulo").orEmpty()) }
    var dica by remember { mutableStateOf(inicial.t("dica").orEmpty()) }
    var tipo by remember { mutableStateOf(inicial.t("tipo") ?: "texto") }
    var opcoes by remember { mutableStateOf(inicial.l("opcoes").joinToString("\n")) }
    var quem by remember { mutableStateOf(inicial.t("quem_preenche") ?: "qualquer") }
    var pontoTipo by remember { mutableStateOf(inicial.t("ponto_tipo")) }
    var obrig by remember { mutableStateOf(inicial.b("obrigatorio")) }
    var vCli by remember { mutableStateOf(inicial["visivel_cliente"]?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: true) }
    var vMoto by remember { mutableStateOf(inicial["visivel_motoboy"]?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: true) }
    var ativo by remember { mutableStateOf(inicial["ativo"]?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: true) }
    var ocupado by remember { mutableStateOf(false) }
    val ent = inicial.t("entidade") ?: "pedido"
    PwJanela(if (inicial.t("id") == null) "Novo campo" else "Editar campo", onFechar) {
        PwCampo(rotulo, { rotulo = it }, "Nome do campo", dica = "Ex.: Dinheiro recebido")
        PwCampo(dica, { dica = it }, "Dica (opcional)")
        PwRotulo("Tipo")
        LinhaChips { TIPOS_CAMPO.filter { it.first != "foto" || ent == "ponto" }.forEach { (k, r) -> ChipM(r, tipo == k) { tipo = k } } }
        if (tipo == "escolha") PwCampo(opcoes, { opcoes = it }, "Opções (uma por linha)")
        if (ent == "ponto") { PwRotulo("Em qual parada"); LinhaChips { listOf(null to "As duas", "coleta" to "Só coleta", "entrega" to "Só entrega").forEach { (k, r) -> ChipM(r, pontoTipo == k) { pontoTipo = k } } } }
        PwRotulo("Quem preenche")
        LinhaChips { QUEM.forEach { (k, r) -> ChipM(r, quem == k) { quem = k } } }
        PwInterruptor("Obrigatório", obrig, { obrig = it })
        PwInterruptor("O cliente vê", vCli, { vCli = it })
        PwInterruptor("O motoboy vê", vMoto, { vMoto = it })
        PwInterruptor("Ativo", ativo, { ativo = it })
        PwBotaoOuro(if (ocupado) "Salvando…" else "Salvar", icone = PwIcones.Check, habilitado = rotulo.isNotBlank() && !ocupado, modifier = Modifier.fillMaxWidth(), onClick = {
            ocupado = true
            escopo.launch {
                Gestao.rpcJson("salvar_campo", buildJsonObject { put("p", buildJsonObject {
                    inicial.t("id")?.let { put("id", it) }; put("entidade", ent); put("rotulo", rotulo.trim()); put("dica", dica.trim()); put("tipo", tipo)
                    put("opcoes", buildJsonArray { opcoes.lines().map { it.trim() }.filter { it.isNotEmpty() }.forEach { add(JsonPrimitive(it)) } })
                    put("quem_preenche", quem); put("ponto_tipo", pontoTipo ?: ""); put("obrigatorio", obrig); put("visivel_cliente", vCli); put("visivel_motoboy", vMoto); put("ativo", ativo)
                }) }).onSuccess { PwToast.mostrar("Campo salvo."); onSalvo() }.onFailure { PwToast.mostrar(erroMod(it.message), TipoAviso.ERRO) }
                ocupado = false
            }
        })
        inicial.t("id")?.let { id -> PwBotaoPerigo("Apagar campo", { escopo.launch { Gestao.rpcJson("apagar_campo", buildJsonObject { put("p_id", id) }).onSuccess { onSalvo() } } }, modifier = Modifier.fillMaxWidth()) }
    }
}

// =================================================================== nomes próprios
@Composable
private fun NomesG() {
    val escopo = rememberCoroutineScope()
    val valores = remember { mutableStateMapOf<String, String>() }
    val salvos = remember { mutableStateMapOf<String, String>() }
    var carregou by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        Gestao.rpcJson("rotulos_editor").onSuccess { e -> lista(e).filter { (it.t("tipo_pedido") ?: "").isEmpty() && !it.b("da_plataforma") }.forEach { r ->
            valores[r.t("alvo")!!] = r.t("texto").orEmpty(); salvos[r.t("alvo")!!] = r.t("texto").orEmpty() } }
        carregou = true
    }
    if (!carregou) { PwCarregando(); return }
    TextoDica("Os nomes que a equipe e os clientes veem. Vazio = o padrão. O funcionamento continua o mesmo.")
    @Composable fun linha(alvo: String, padrao: String) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(valores[alvo] ?: "", { valores[alvo] = it }, padrao, modifier = Modifier.weight(1f))
            PwBotaoVidro("Salvar", habilitado = (valores[alvo] ?: "") != (salvos[alvo] ?: ""), onClick = {
                escopo.launch { Gestao.rpcJson("salvar_rotulo", buildJsonObject { put("p_alvo", alvo); put("p_texto", valores[alvo] ?: "") })
                    .onSuccess { salvos[alvo] = valores[alvo] ?: ""; PwToast.mostrar("Nome salvo. Vale ao abrir o app de novo.") }.onFailure { PwToast.mostrar(erroMod(it.message), TipoAviso.ERRO) } }
            })
        }
    }
    PwCartao { PwRotulo("Tipos de parada"); linha("parada:coleta", "Coleta"); linha("parada:entrega", "Entrega") }
    PwCartao { PwRotulo("Status do pedido"); StatusPedido.padroes.forEach { (s, r) -> linha("pedido:$s", r) } }
    PwCartao { PwRotulo("Status da rota"); StatusCorrida.padroes.forEach { (s, r) -> linha("rota:$s", r) } }
}

// =================================================================== taxas e bônus
private val UNID_PRECO = listOf("fixo" to "Fixo", "km" to "Por km", "ponto" to "Por ponto", "coleta" to "Por coleta", "entrega" to "Por entrega",
    "volume" to "Por volume", "kg" to "Por kg", "pct_subtotal" to "% do subtotal", "pct_declarado" to "% do declarado")
private val UNID_PAG = listOf("fixo" to "Fixo", "km" to "Por km", "ponto" to "Por ponto", "coleta" to "Por coleta", "entrega" to "Por entrega",
    "minuto_parado" to "Por minuto parado", "pct_rota" to "% da rota")
private val DIAS = listOf(0 to "Dom", 1 to "Seg", 2 to "Ter", 3 to "Qua", 4 to "Qui", 5 to "Sex", 6 to "Sáb")
private val VEIC = listOf("moto" to "Moto", "carro" to "Carro", "bicicleta" to "Bicicleta", "van" to "Van")

@Composable
private fun TaxasG() {
    var itens by remember { mutableStateOf<List<JsonObject>?>(null) }
    var ed by remember { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(versao) { Gestao.rpcJson("regras_extras_editor").onSuccess { itens = lista(it) }.onFailure { itens = emptyList() } }
    TextoDica("Taxas e bônus que o cálculo padrão não tem: espera, pedágio, seguro, noturno, fim de semana. Cada regra é uma linha a mais na conta.")
    val l = itens ?: run { PwCarregando(); return }
    listOf("preco" to "No preço do cliente", "pagamento" to "No pagamento do motoboy").forEach { (alvo, titulo) ->
        PwCartao {
            PwRotulo(titulo)
            val us = if (alvo == "preco") UNID_PRECO else UNID_PAG
            l.filter { it.t("alvo") == alvo }.forEach { r ->
                val u = r.t("unidade").orEmpty()
                LinhaLista(r.t("nome").orEmpty(), "${if (u.startsWith("pct_")) "${r.n("valor")}%" else moedaBr(r.n("valor") ?: 0.0)} · ${us.firstOrNull { it.first == u }?.second ?: u}",
                    inativa = !r.b("ativo")) { if (!r.b("da_plataforma")) ed = r }
                androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
            }
            if (l.none { it.t("alvo") == alvo }) PwVazio("Nenhuma regra ainda.")
            PwBotaoVidro("Nova regra", onClick = { ed = buildJsonObject { put("alvo", alvo); put("unidade", "fixo"); put("ativo", true) } }, icone = PwIcones.Plus, modifier = Modifier.fillMaxWidth())
        }
    }
    ed?.let { e -> EditorTaxa(e, onFechar = { ed = null }, onSalvo = { ed = null; versao++ }) }
}

@Composable
private fun EditorTaxa(inicial: JsonObject, onFechar: () -> Unit, onSalvo: () -> Unit) {
    val escopo = rememberCoroutineScope()
    val alvo = inicial.t("alvo") ?: "preco"
    val cond = inicial["condicoes"] as? JsonObject ?: JsonObject(emptyMap())
    var nome by remember { mutableStateOf(inicial.t("nome").orEmpty()) }
    var unidade by remember { mutableStateOf(inicial.t("unidade") ?: "fixo") }
    var valor by remember { mutableStateOf(inicial.n("valor")?.toString() ?: "") }
    var franquia by remember { mutableStateOf(inicial.n("franquia")?.takeIf { it != 0.0 }?.toString() ?: "") }
    var minimo by remember { mutableStateOf(inicial.n("minimo")?.toString() ?: "") }
    var maximo by remember { mutableStateOf(inicial.n("maximo")?.toString() ?: "") }
    var dias by remember { mutableStateOf((cond["dias"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() }) }
    var de by remember { mutableStateOf(cond.t("de").orEmpty()) }
    var ate by remember { mutableStateOf(cond.t("ate").orEmpty()) }
    var veiculos by remember { mutableStateOf(cond.l("veiculos")) }
    var urgente by remember { mutableStateOf(cond.b("urgente")) }
    var limitar by remember { mutableStateOf(inicial.b("limitar_ao_valor")) }
    var ativo by remember { mutableStateOf(inicial["ativo"]?.let { (it as? JsonPrimitive)?.booleanOrNull } ?: true) }
    var ocupado by remember { mutableStateOf(false) }
    val pct = unidade.startsWith("pct_")
    PwJanela(if (inicial.t("id") == null) "Nova regra" else "Editar regra", onFechar) {
        PwCampo(nome, { nome = it }, "Nome (aparece na conta)", dica = if (alvo == "preco") "Ex.: Adicional noturno" else "Ex.: Espera na coleta")
        PwRotulo("Como cobra")
        LinhaChips { (if (alvo == "preco") UNID_PRECO else UNID_PAG).forEach { (k, r) -> ChipM(r, unidade == k) { unidade = k } } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(valor, { valor = it }, if (pct) "Percentual (%)" else "Valor (R$)", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            if (unidade != "fixo") PwCampo(franquia, { franquia = it }, if (pct) "Não cobra até (R$)" else "Grátis até", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(minimo, { minimo = it }, "Mínimo (R$)", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            PwCampo(maximo, { maximo = it }, "Máximo (R$)", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        PwRotulo("Quando vale (nada marcado = sempre)")
        LinhaChips { DIAS.forEach { (d, r) -> ChipM(r, d in dias) { dias = if (d in dias) dias - d else dias + d } } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(de, { de = it }, "Das (HH:MM)", modifier = Modifier.weight(1f))
            PwCampo(ate, { ate = it }, "Até (HH:MM)", modifier = Modifier.weight(1f))
        }
        PwRotulo("Veículo")
        LinhaChips { VEIC.forEach { (k, r) -> ChipM(r, k in veiculos) { veiculos = if (k in veiculos) veiculos - k else veiculos + k } } }
        PwInterruptor("Só em pedido urgente", urgente, { urgente = it })
        if (alvo == "pagamento") PwInterruptor("Respeitar o teto (valor da rota)", limitar, { limitar = it })
        PwInterruptor("Ativa", ativo, { ativo = it })
        PwBotaoOuro(if (ocupado) "Salvando…" else "Salvar", icone = PwIcones.Check, habilitado = nome.isNotBlank() && numero(valor) != null && !ocupado, modifier = Modifier.fillMaxWidth(), onClick = {
            ocupado = true
            escopo.launch {
                Gestao.rpcJson("salvar_regra_extra", buildJsonObject { put("p", buildJsonObject {
                    inicial.t("id")?.let { put("id", it) }; put("alvo", alvo); put("nome", nome.trim()); put("unidade", unidade)
                    put("valor", numero(valor) ?: 0.0); put("franquia", numero(franquia) ?: 0.0)
                    numero(minimo)?.let { put("minimo", it) }; numero(maximo)?.let { put("maximo", it) }
                    put("condicoes", buildJsonObject {
                        if (dias.isNotEmpty()) put("dias", buildJsonArray { dias.sorted().forEach { add(JsonPrimitive(it)) } })
                        if (Regex("^\\d{1,2}:\\d{2}$").matches(de.trim()) && Regex("^\\d{1,2}:\\d{2}$").matches(ate.trim())) { put("de", de.trim()); put("ate", ate.trim()) }
                        if (veiculos.isNotEmpty()) put("veiculos", buildJsonArray { veiculos.forEach { add(JsonPrimitive(it)) } })
                        if (urgente) put("urgente", true)
                    })
                    put("limitar_ao_valor", limitar); put("ativo", ativo)
                }) }).onSuccess { PwToast.mostrar("Regra salva."); onSalvo() }.onFailure { PwToast.mostrar(erroMod(it.message), TipoAviso.ERRO) }
                ocupado = false
            }
        })
        inicial.t("id")?.let { id -> PwBotaoPerigo("Apagar regra", { escopo.launch { Gestao.rpcJson("apagar_regra_extra", buildJsonObject { put("p_id", id) }).onSuccess { onSalvo() } } }, modifier = Modifier.fillMaxWidth()) }
    }
}

// =================================================================== cargos
private val PERM_SUP = listOf("pedidos" to "Revisar e aprovar pedidos", "rotas" to "Montar, mover e despachar rotas", "motoboys" to "Acompanhar e falar com os motoboys",
    "mapa" to "Ver o mapa ao vivo", "clientes" to "Ver e editar clientes", "cadastrar" to "Criar contas de motoboys e clientes", "chat" to "Criar grupos no chat",
    "ajustes" to "Mudar os ajustes da empresa", "financeiro" to "Ver holerites, faturas e valores", "apagar" to "Apagar pedidos, rotas e históricos")
private val PADRAO_SUP = setOf("pedidos", "rotas", "motoboys", "mapa", "clientes", "cadastrar", "chat")

@Composable
private fun CargosG() {
    var itens by remember { mutableStateOf<List<JsonObject>?>(null) }
    var ed by remember { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(versao) { Gestao.rpcJson("cargos_da_empresa").onSuccess { itens = lista(it) }.onFailure { itens = emptyList() } }
    TextoDica("Cargos como Atendente, Financeiro ou Despachante. Dê o cargo a um supervisor em Pessoas: ele recebe as permissões do cargo, e as ações bloqueadas aqui valem só para ele.")
    val l = itens ?: run { PwCarregando(); return }
    if (l.isEmpty()) PwVazio("Nenhum cargo ainda.")
    l.forEach { k ->
        LinhaLista(k.t("nome").orEmpty(), "${(k["permissoes"] as? JsonObject)?.values?.count { (it as? JsonPrimitive)?.booleanOrNull == true } ?: 0} permissões · ${(k["acoes"] as? JsonObject)?.size ?: 0} ações bloqueadas · ${k.t("pessoas") ?: "0"} pessoa(s)") { ed = k }
        androidx.compose.foundation.layout.Spacer(Modifier.padding(2.dp))
    }
    PwBotaoOuro("Novo cargo", onClick = { ed = JsonObject(emptyMap()) }, icone = PwIcones.Plus, modifier = Modifier.fillMaxWidth())
    ed?.let { e -> EditorCargo(e, onFechar = { ed = null }, onSalvo = { ed = null; versao++ }) }
}

@Composable
private fun EditorCargo(inicial: JsonObject, onFechar: () -> Unit, onSalvo: () -> Unit) {
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf(inicial.t("nome").orEmpty()) }
    val perm = remember { mutableStateMapOf<String, Boolean>().apply {
        val p = inicial["permissoes"] as? JsonObject
        PERM_SUP.forEach { (k, _) -> put(k, (p?.get(k) as? JsonPrimitive)?.booleanOrNull ?: (k in PADRAO_SUP)) } } }
    val bloqueadas = remember { mutableStateMapOf<String, Boolean>().apply { (inicial["acoes"] as? JsonObject)?.keys?.forEach { put(it, true) } } }
    var ocupado by remember { mutableStateOf(false) }
    PwJanela(if (inicial.t("id") == null) "Novo cargo" else "Editar cargo", onFechar) {
        PwCampo(nome, { nome = it }, "Nome do cargo", dica = "Ex.: Atendente")
        PwRotulo("O que pode fazer")
        PERM_SUP.forEach { (k, r) -> PwInterruptor(r, perm[k] == true, { perm[k] = it }) }
        PwRotulo("Ações bloqueadas para este cargo")
        com.pwexpress.feature.gestor.data.ACOES_GESTOR.forEach { (k, r) -> PwInterruptor(r, bloqueadas[k] == true, { if (it) bloqueadas[k] = true else bloqueadas.remove(k) }, cor = Pw.cores.vermelho) }
        PwBotaoOuro(if (ocupado) "Salvando…" else "Salvar", icone = PwIcones.Check, habilitado = nome.isNotBlank() && !ocupado, modifier = Modifier.fillMaxWidth(), onClick = {
            ocupado = true
            escopo.launch {
                Gestao.rpcJson("salvar_cargo", buildJsonObject { put("p", buildJsonObject {
                    inicial.t("id")?.let { put("id", it) }; put("nome", nome.trim())
                    put("permissoes", buildJsonObject { perm.forEach { (k, v) -> put(k, v) } })
                    put("acoes", buildJsonObject { bloqueadas.keys.forEach { put(it, false) } })
                }) }).onSuccess { PwToast.mostrar("Cargo salvo."); onSalvo() }.onFailure { PwToast.mostrar(erroMod(it.message), TipoAviso.ERRO) }
                ocupado = false
            }
        })
        inicial.t("id")?.let { id -> PwBotaoPerigo("Apagar cargo", { escopo.launch { Gestao.rpcJson("apagar_cargo", buildJsonObject { put("p_id", id) }).onSuccess { onSalvo() } } }, modifier = Modifier.fillMaxWidth()) }
    }
}

/** Pessoas → editar supervisor: escolher o cargo. */
@Composable
internal fun EscolherCargoG(pessoa: String, onMudou: () -> Unit = {}) {
    val escopo = rememberCoroutineScope()
    var cargos by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    LaunchedEffect(Unit) { Gestao.rpcJson("cargos_da_empresa").onSuccess { cargos = lista(it) } }
    if (cargos.isEmpty()) return
    PwRotulo("Cargo")
    LinhaChips {
        ChipM("Sem cargo", false) { escopo.launch { Gestao.rpcJson("definir_cargo_pessoa", buildJsonObject { put("p_pessoa", pessoa); put("p_cargo", JsonNull) }).onSuccess { PwToast.mostrar("Cargo tirado."); onMudou() } } }
        cargos.forEach { k -> ChipM(k.t("nome").orEmpty(), false) {
            escopo.launch { Gestao.rpcJson("definir_cargo_pessoa", buildJsonObject { put("p_pessoa", pessoa); put("p_cargo", k.t("id")) })
                .onSuccess { PwToast.mostrar("Cargo ${k.t("nome")} aplicado."); onMudou() }.onFailure { PwToast.mostrar(erroMod(it.message), TipoAviso.ERRO) } }
        } }
    }
}
