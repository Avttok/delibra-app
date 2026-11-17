package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwConfirmar
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.comoLista
import com.pwexpress.core.json.comoObjeto
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.gestor.data.ACOES_GESTOR
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PARAMETROS
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Delibra Dev no app: as mesmas áreas do site (/dev). Comando ao vivo, Regras do Fantasma,
 * matriz pessoa × permissão com o porquê, explorador de dados, registro do Dev, novidades e emergência.
 */

private fun traduzirDev(m: String?): String = when {
    m == null -> "Erro"
    "apenas_dev" in m -> "Só o Dev."
    "chave_obrigatoria" in m -> "Escolha o que a regra controla."
    "alvo_obrigatorio" in m -> "Escolha para quem a regra vale."
    else -> m
}
private fun args(vararg p: Pair<String, Any?>): JsonObject = buildJsonObject {
    p.forEach { (k, v) ->
        when (v) {
            null -> put(k, JsonNull)
            is String -> put(k, v)
            is Number -> put(k, v)
            is Boolean -> put(k, v)
            is JsonElement -> put(k, v)
            else -> put(k, v.toString())
        }
    }
}
private suspend fun dev(f: String, a: JsonObject = JsonObject(emptyMap())) = Gestao.rpcJson(f, a)
private fun valorTexto(v: JsonElement?): String = when {
    v == null || v is JsonNull -> "—"
    v is JsonPrimitive && v.content == "true" -> "Sim"
    v is JsonPrimitive && v.content == "false" -> "Não"
    v is JsonPrimitive -> v.content
    else -> v.toString()
}

/** Tudo que dá para decidir: módulos, ações, permissões especiais e ajustes. */
internal data class ChaveDev(val chave: String, val rotulo: String, val tipo: String, val valor: String)
internal fun catalogoDev(modulos: List<Gestao.ModuloDev>): List<ChaveDev> =
    modulos.map { ChaveDev("modulo:${it.chave}", it.nome, "Módulo", "booleano") } +
        ACOES_GESTOR.map { (k, r) -> ChaveDev("acao:$k", r, "Ação", "booleano") } +
        PARAMETROS.map { p -> ChaveDev(p.chave, p.rotulo.removePrefix("Permissão especial: "), if (p.chave.startsWith("especial_")) "Permissão especial" else "Ajuste",
            when (p.tipo) { "booleano" -> "booleano"; "numero", "opcoes" -> "numero"; else -> "texto" }) } +
        // o nível de cada ajuste no painel do gestor
        PARAMETROS.filter { !it.soDev }.map { p -> ChaveDev("nivel:${p.chave}", "${p.rotulo} (no painel do gestor)", "O que o gestor vê", "nivel") }

@Composable
private fun rememberCatalogo(): List<ChaveDev> {
    var mods by remember { mutableStateOf<List<Gestao.ModuloDev>>(emptyList()) }
    LaunchedEffect(Unit) { Gestao.modulos().onSuccess { mods = it } }
    return remember(mods) { catalogoDev(mods) }
}

/** Faixa vermelha do Delibra Dev: deixa claro que esta é a parte de trás do sistema. */
@Composable
internal fun FaixaDelibraDev(area: String) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().clip(forma).background(c.vermelho.copy(alpha = 0.10f)).border(1.dp, c.vermelho.copy(alpha = 0.35f), forma).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(PwIcones.Radar, null, tint = c.vermelho, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Delibra Dev", color = c.texto, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text(area, color = c.texto3, fontSize = 12.5.sp)
        }
    }
}

// ───────────────────────────────────────────────────────────── Comando ao vivo + emergência
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ComandoDevG() {
    val c = Pw.cores
    var d by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    LaunchedEffect(versao) {
        while (true) {
            dev("dev_comando").onSuccess { d = it.comoObjeto(); erro = null }.onFailure { erro = traduzirDev(it.message) }
            delay(15_000)
        }
    }
    PwErro(erro)
    val x = d ?: run { PwCarregando(); return }
    val emp = x.arr("empresas")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwNumero("Online", emp.sumOf { it.int("online") ?: 0 }.toString(), Modifier.weight(1f), icone = PwIcones.Bike)
        PwNumero("Rotas ativas", emp.sumOf { it.int("rotas_ativas") ?: 0 }.toString(), Modifier.weight(1f), icone = PwIcones.Route)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwNumero("Pedidos 24 h", emp.sumOf { it.int("pedidos_hoje") ?: 0 }.toString(), Modifier.weight(1f), icone = PwIcones.Package)
        val em = emp.sumOf { it.int("emergencias") ?: 0 }
        PwNumero("Emergências", em.toString(), Modifier.weight(1f), icone = PwIcones.Siren, destaque = em > 0)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwNumero("Regras ativas", (x.int("regras_ativas") ?: 0).toString(), Modifier.weight(1f), icone = PwIcones.ShieldAlert)
        PwNumero("Módulos desligados", x.strs("modulos_desligados").size.toString(), Modifier.weight(1f), icone = PwIcones.ToggleRight)
    }
    PwRotulo("Empresas agora")
    emp.forEach { e ->
        PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.str("nome") ?: "—", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (e.str("status") != "ativa") Text(e.str("status") ?: "", color = c.vermelho, fontSize = 12.sp)
            }
            TextoDica("${e.int("online") ?: 0} online · ${e.int("em_corrida") ?: 0} em corrida · ${e.int("rotas_ativas") ?: 0} rotas · ${e.int("pedidos_hoje") ?: 0} pedidos 24 h · ${e.int("para_revisar") ?: 0} para revisar" +
                ((e.int("emergencias") ?: 0).takeIf { it > 0 }?.let { " · $it emergência(s)" } ?: ""))
        }
    }
    PwRotulo("Na rua")
    val motos = x.arr("motoboys")
    if (motos.isEmpty()) PwVazio("Ninguém online.", icone = PwIcones.Bike)
    motos.forEach { m -> TextoDica("• ${m.str("nome")} · ${m.str("empresa") ?: "—"} · ${m.str("status")?.replace("_", " ")}${m.int("bateria")?.let { " · $it%" } ?: ""}${m.str("versao")?.let { " · v$it" } ?: ""}") }
    PwRotulo("Acabou de acontecer")
    x.arr("ultimas").forEach { u -> TextoDica("${if (u.str("tipo") == "dev") "Dev" else "Pedido"} · ${u.str("texto")}") }
    EmergenciaDevG(x, empresas = emp) { versao++ }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmergenciaDevG(cmd: JsonObject, empresas: List<JsonObject>, onMudou: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var texto by remember { mutableStateOf("") }
    var nivel by remember { mutableIntStateOf(0) }
    val papeis = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var empAviso by remember { mutableStateOf<String?>(null) }
    var empCong by remember { mutableStateOf<String?>(null) }
    var motivo by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }
    PwRotulo("Emergência")
    PwCartao(cor = c.vermelho.copy(alpha = 0.06f)) {
        Text("Avisar", color = c.texto, fontWeight = FontWeight.Bold)
        PwCampo(texto, { texto = it }, "Texto do aviso", linhas = 2)
        PwSegmentado(listOf("Informação" to null, "Atenção" to null, "Urgente" to null), nivel, { nivel = it })
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("motoboy" to "Motoboys", "gestor" to "Gestores", "supervisor" to "Supervisores", "cliente" to "Clientes").forEach { (k, r) ->
                ChipDev(r, k in papeis) { if (k in papeis) papeis.remove(k) else papeis.add(k) }
            }
        }
        TextoDica(if (papeis.isEmpty()) "Para todos os cargos" else "Só para os cargos marcados")
        SeletorEmpresa(empresas, empAviso, "Todas as empresas") { empAviso = it }
        PwBotaoOuro("Mandar aviso", habilitado = texto.isNotBlank(), onClick = {
            escopo.launch {
                dev("dev_avisar", args("p" to buildJsonObject {
                    put("texto", texto); put("nivel", listOf("info", "aviso", "urgente")[nivel]); put("organizacao_id", empAviso ?: "")
                    put("papeis", buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
                })).onSuccess { PwToast.mostrar("Aviso no ar."); texto = ""; onMudou() }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
            }
        })
        cmd.arr("avisos").forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${a.str("texto")} · ${a.str("empresa") ?: "todas"}", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.X, "Encerrar aviso", { escopo.launch { dev("dev_encerrar_aviso", args("p_id" to a.str("id"))); onMudou() } })
            }
        }
    }
    PwCartao(cor = c.vermelho.copy(alpha = 0.06f)) {
        Text("Congelar empresa", color = c.texto, fontWeight = FontWeight.Bold)
        SeletorEmpresa(empresas, empCong, "Escolha a empresa") { empCong = it }
        PwCampo(motivo, { motivo = it }, "Motivo (a equipe vê)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwBotaoPerigo("Congelar", habilitado = empCong != null, icone = PwIcones.Lock, modifier = Modifier.weight(1f), onClick = {
                confirmar = "Equipe e clientes ficam sem acesso até você descongelar." to {
                    escopo.launch { dev("dev_congelar_empresa", args("p_org" to empCong, "p_congelar" to true, "p_motivo" to motivo)).onSuccess { PwToast.mostrar("Empresa congelada.", TipoAviso.AVISO); onMudou() } }
                }
            })
            PwBotaoVidro("Descongelar", habilitado = empCong != null, icone = PwIcones.LockOpen, modifier = Modifier.weight(1f), onClick = {
                escopo.launch { dev("dev_congelar_empresa", args("p_org" to empCong, "p_congelar" to false, "p_motivo" to null)).onSuccess { PwToast.mostrar("Empresa de volta."); onMudou() } }
            })
        }
        PwBotaoVidro("Desconectar todos desta empresa", habilitado = empCong != null, icone = PwIcones.Unplug, modifier = Modifier.fillMaxWidth(), onClick = {
            confirmar = "Cada pessoa da empresa vai precisar entrar de novo." to {
                escopo.launch { dev("dev_derrubar_sessoes", args("p_pessoa" to null, "p_org" to empCong)).onSuccess { PwToast.mostrar("${valorTexto(it)} conta(s) desconectada(s).") } }
            }
        })
        TextoDica("Para desligar uma parte do sistema para todos, use Módulos. Para obrigar a atualizar o app, Ajustes → Versão do app.")
    }
    confirmar?.let { (t, acao) -> PwConfirmar("Tem certeza?", t, "Confirmar", onConfirmar = { acao(); confirmar = null }, onCancelar = { confirmar = null }) }
}

@Composable
private fun SeletorEmpresa(empresas: List<JsonObject>, atual: String?, vazio: String, onEscolher: (String?) -> Unit) {
    val c = Pw.cores
    var aberto by remember { mutableStateOf(false) }
    PwBotaoVidro(empresas.firstOrNull { it.str("id") == atual }?.str("nome") ?: vazio, onClick = { aberto = true }, icone = PwIcones.Building2, modifier = Modifier.fillMaxWidth())
    if (aberto) PwJanela("Empresa", { aberto = false }) {
        Text(vazio, color = c.texto2, modifier = Modifier.fillMaxWidth().clickable { onEscolher(null); aberto = false }.padding(12.dp))
        empresas.forEach { e -> Text(e.str("nome") ?: "—", color = c.texto, modifier = Modifier.fillMaxWidth().clickable { onEscolher(e.str("id")); aberto = false }.padding(12.dp)) }
    }
}

@Composable
private fun ChipDev(texto: String, ligado: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(99.dp)
    Text(texto, color = if (ligado) c.texto else c.texto2, fontSize = 13.sp,
        modifier = Modifier.clip(forma).background(if (ligado) c.vermelho.copy(alpha = 0.16f) else c.vidroSutil)
            .border(1.dp, if (ligado) c.vermelho else c.borda, forma).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

// ───────────────────────────────────────────────────────────── Busca de pessoas, clientes e empresas (pelo explorador)
@Composable
private fun BuscaAlvo(tabela: String, rotulo: String, escolhido: String?, onEscolher: (String, String) -> Unit) {
    val c = Pw.cores
    var q by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    LaunchedEffect(q, tabela) {
        if (q.trim().length < 2) { res = emptyList(); return@LaunchedEffect }
        delay(300)
        dev("dev_explorar", args("p_tabela" to tabela, "p_busca" to q.trim(), "p_limite" to 10, "p_pagina" to 0, "p_ordem" to null))
            .onSuccess { res = it.comoObjeto()?.arr("linhas").orEmpty() }
    }
    PwCampo(q, { q = it }, rotulo, dica = escolhido?.let { "Escolhido: $it" })
    res.forEach { r ->
        val nome = r.str("nome") ?: r.str("id") ?: "—"
        Text(nome + (r.strs("papeis").takeIf { it.isNotEmpty() }?.let { " · ${it.joinToString()}" } ?: ""), color = c.texto,
            modifier = Modifier.fillMaxWidth().clickable { onEscolher(r.str("id") ?: "", nome); q = ""; res = emptyList() }.padding(10.dp))
    }
}

// ───────────────────────────────────────────────────────────── Regras do Fantasma
private val ESCOPOS = listOf("global" to "Sistema", "empresa" to "Empresa", "cliente" to "Cliente", "papel" to "Cargo", "pessoa" to "Pessoa")
private val DIAS = listOf("Seg", "Ter", "Qua", "Qui", "Sex", "Sáb", "Dom")

@Composable
internal fun RegrasDevG(soMinhas: Boolean = false) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    val catalogo = rememberCatalogo()
    val meuId = Perfis.perfil.value?.id
    var regras by remember { mutableStateOf<List<JsonObject>?>(null) }
    var editando by remember { mutableStateOf<JsonObject?>(null) }
    var novo by remember { mutableStateOf(false) }
    var apagar by remember { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    LaunchedEffect(versao) { dev("dev_regras").onSuccess { regras = it.comoLista() }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) } }
    if (!soMinhas) TextoDica("Uma regra passa por cima de qualquer ajuste, módulo ou permissão — no site, no app e no servidor. Vale a mais específica: pessoa › cliente › empresa › cargo › sistema. Você só é afetado pelas regras que fizer para a sua conta.")
    PwBotaoOuro("Nova regra", icone = PwIcones.Plus, onClick = { novo = true })
    val lista = regras ?: run { PwCarregando(); return }
    val visiveis = lista.filter { !soMinhas || (it.str("escopo") == "pessoa" && it.str("alvo") == meuId) }
    if (visiveis.isEmpty()) PwVazio(if (soMinhas) "Nenhuma regra para você: tudo liberado." else "Nenhuma regra. O sistema segue os ajustes de cada empresa.", icone = PwIcones.ShieldAlert)
    visiveis.forEach { r ->
        val def = catalogo.firstOrNull { it.chave == r.str("chave") }
        val parada = !r.bool("ativo") || r.bool("vencida")
        PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(14.dp), cor = if (parada) null else c.vermelho.copy(alpha = 0.05f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(def?.rotulo ?: r.str("chave").orEmpty(), color = c.texto, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    TextoDica("${def?.tipo ?: "Chave"} · ${ESCOPOS.firstOrNull { it.first == r.str("escopo") }?.second}${r.str("alvo_nome")?.let { ": $it" } ?: ""}")
                }
                Text(if (def?.tipo == "Módulo") (if (r.str("valor") == "true") "Ligado" else "Desligado") else valorTexto(r["valor"]),
                    color = if (r.str("valor") == "false") c.vermelho else c.verde, fontWeight = FontWeight.Bold)
            }
            r.str("porque")?.let { TextoDica("“$it”") }
            if (r.bool("vencida")) TextoDica("Venceu.")
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwInterruptor("Ativa", r.bool("ativo"), { lig ->
                    escopo.launch {
                        val p = buildJsonObject { r.forEach { (k, v) -> put(k, v) }; put("ativo", lig) }
                        dev("dev_salvar_regra", args("p" to p)).onSuccess { versao++; Perfis.carregar() }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
                    }
                }, modifier = Modifier.weight(1f))
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Pencil, "Editar", { editando = r })
                Spacer(Modifier.width(6.dp))
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Trash2, "Apagar", { apagar = r })
            }
        }
    }
    if (!soMinhas) ExplicarDevG(catalogo)
    if (novo || editando != null) EditorRegraG(editando, catalogo, if (soMinhas) meuId else null, onFechar = { novo = false; editando = null }) { novo = false; editando = null; versao++; escopo.launch { Perfis.carregar() } }
    apagar?.let { r -> PwConfirmar("Apagar regra?", "As versões ficam guardadas.", "Apagar", onConfirmar = {
        escopo.launch { dev("dev_apagar_regra", args("p_id" to r.str("id"))).onSuccess { versao++ } }; apagar = null }, onCancelar = { apagar = null }) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorRegraG(inicial: JsonObject?, catalogo: List<ChaveDev>, soPara: String?, onFechar: () -> Unit, onSalvou: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var chave by remember { mutableStateOf(inicial?.str("chave").orEmpty()) }
    var busca by remember { mutableStateOf("") }
    var esc by remember { mutableIntStateOf(ESCOPOS.indexOfFirst { it.first == (inicial?.str("escopo") ?: if (soPara != null) "pessoa" else "global") }.coerceAtLeast(0)) }
    var alvo by remember { mutableStateOf(inicial?.str("alvo") ?: soPara) }
    var alvoNome by remember { mutableStateOf(inicial?.str("alvo_nome") ?: if (soPara != null) "Você" else null) }
    var valorBool by remember { mutableStateOf(inicial?.str("valor") == "true") }
    var valorTxt by remember { mutableStateOf(inicial?.get("valor")?.let { (it as? JsonPrimitive)?.content }.orEmpty()) }
    var porque by remember { mutableStateOf(inicial?.str("porque").orEmpty()) }
    val cond = inicial?.obj("condicoes")
    val dias = remember { androidx.compose.runtime.mutableStateListOf<Int>().apply { cond?.get("dias")?.let { (it as? JsonArray)?.forEach { d -> (d as? JsonPrimitive)?.content?.toIntOrNull()?.let(::add) } } } }
    var de by remember { mutableStateOf(cond?.str("de").orEmpty()) }
    var ate by remember { mutableStateOf(cond?.str("ate").orEmpty()) }
    var teste by remember { mutableIntStateOf(when (cond?.get("teste")?.let { (it as? JsonPrimitive)?.content }) { "false" -> 1; "true" -> 2; else -> 0 }) }
    var duracao by remember { mutableIntStateOf(0) }
    var aparelho by remember { mutableIntStateOf(when (cond?.str("aparelho")) { "app" -> 1; "site" -> 2; else -> 0 }) }
    var nivel by remember { mutableIntStateOf(listOf("editar", "ver", "oculto").indexOf(inicial?.str("valor")).coerceAtLeast(0)) }
    val def = catalogo.firstOrNull { it.chave == chave }
    PwJanela(if (inicial == null) "Nova regra" else "Regra · versão ${inicial.int("versao") ?: 1}", onFechar) {
        PwRotulo("O que ela controla")
        if (def != null || chave.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${def?.tipo ?: "Chave"}: ${def?.rotulo ?: chave}", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                PwBotaoVidro("Trocar", onClick = { chave = "" })
            }
        } else {
            PwCampo(busca, { busca = it }, "Buscar módulo, ação, permissão ou ajuste")
            val t = busca.trim().lowercase()
            if (t.isNotEmpty()) catalogo.filter { "${it.rotulo} ${it.chave}".lowercase().contains(t) }.take(25).forEach { k ->
                Text("${k.tipo} · ${k.rotulo}", color = c.texto, modifier = Modifier.fillMaxWidth().clickable { chave = k.chave; busca = "" }.padding(10.dp))
            }
        }
        if (soPara == null) {
            PwRotulo("Para quem")
            PwSegmentado(ESCOPOS.map { it.second to null }, esc, { esc = it; alvo = null; alvoNome = null })
            when (ESCOPOS[esc].first) {
                "empresa" -> BuscaAlvo("organizacoes", "Nome da empresa", alvoNome) { id, n -> alvo = id; alvoNome = n }
                "cliente" -> BuscaAlvo("empresas", "Nome do cliente", alvoNome) { id, n -> alvo = id; alvoNome = n }
                "pessoa" -> BuscaAlvo("perfis", "Nome da pessoa", alvoNome) { id, n -> alvo = id; alvoNome = n }
                "papel" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("motoboy" to "Motoboys", "gestor" to "Gestores", "supervisor" to "Supervisores", "cliente" to "Clientes").forEach { (k, r) -> ChipDev(r, alvo == k) { alvo = k; alvoNome = r } }
                }
            }
        }
        PwRotulo("Valor")
        if (def?.valor == "nivel") PwSegmentado(listOf("Vê e muda" to null, "Só vê" to null, "Escondido" to null), nivel, { nivel = it })
        else if (def == null || def.valor == "booleano") PwInterruptor(if (def?.tipo == "Módulo") "Ligado" else "Sim / libera", valorBool, { valorBool = it })
        else PwCampo(valorTxt, { valorTxt = it }, "Valor", teclado = if (def.valor == "numero") androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Text)
        PwRotulo("Quando (opcional)")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DIAS.forEachIndexed { i, d -> ChipDev(d, (i + 1) in dias) { if ((i + 1) in dias) dias.remove(i + 1) else dias.add(i + 1) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(de, { de = it }, "Das (HH:MM)", Modifier.weight(1f))
            PwCampo(ate, { ate = it }, "Até (HH:MM)", Modifier.weight(1f))
        }
        PwSegmentado(listOf("Real e teste" to null, "Só real" to null, "Só teste" to null), teste, { teste = it })
        PwSegmentado(listOf("Site e app" to null, "Só no app" to null, "Só no site" to null), aparelho, { aparelho = it })
        if (inicial == null) PwSegmentado(listOf("Sempre" to null, "1 hora" to null, "1 dia" to null, "1 semana" to null), duracao, { duracao = it })
        PwCampo(porque, { porque = it }, "Por quê", dica = "Aparece no “por quê” e no registro")
        val pronto = chave.isNotBlank() && (ESCOPOS[esc].first == "global" || alvo != null)
        PwBotaoOuro(if (inicial == null) "Criar regra" else "Salvar mudanças", habilitado = pronto, onClick = {
            val horas = listOf(0, 1, 24, 168)[duracao]
            val p = buildJsonObject {
                inicial?.str("id")?.let { put("id", it) }
                put("chave", chave); put("escopo", ESCOPOS[esc].first); put("alvo", alvo ?: "")
                if (def?.valor == "nivel") put("valor", listOf("editar", "ver", "oculto")[nivel])
                else if (def == null || def.valor == "booleano") put("valor", valorBool)
                else if (def.valor == "numero") put("valor", valorTxt.replace(",", ".").toDoubleOrNull() ?: 0.0) else put("valor", valorTxt)
                put("condicoes", buildJsonObject {
                    if (dias.isNotEmpty()) put("dias", buildJsonArray { dias.sorted().forEach { add(JsonPrimitive(it)) } })
                    if (de.isNotBlank() && ate.isNotBlank()) { put("de", de.trim()); put("ate", ate.trim()) }
                    if (teste == 1) put("teste", false); if (teste == 2) put("teste", true)
                    if (aparelho == 1) put("aparelho", "app"); if (aparelho == 2) put("aparelho", "site")
                })
                put("porque", porque); put("ativo", inicial?.bool("ativo") ?: true)
                inicial?.str("valida_ate")?.let { put("valida_ate", it) }
                if (horas > 0) put("valida_ate", java.time.Instant.now().plusSeconds(horas * 3600L).toString())
            }
            escopo.launch {
                dev("dev_salvar_regra", args("p" to p)).onSuccess { PwToast.mostrar(if (inicial == null) "Regra criada. Já está valendo." else "Regra atualizada."); onSalvou() }
                    .onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
            }
        })
    }
}

@Composable
private fun ExplicarDevG(catalogo: List<ChaveDev>) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var pessoa by remember { mutableStateOf<Pair<String, String>?>(null) }
    var chave by remember { mutableStateOf<ChaveDev?>(null) }
    var busca by remember { mutableStateOf("") }
    var res by remember { mutableStateOf<JsonObject?>(null) }
    PwRotulo("Testar: por que fulano vê isso?")
    PwCartao {
        BuscaAlvo("perfis", "Pessoa", pessoa?.second) { id, n -> pessoa = id to n; res = null }
        if (chave == null) {
            PwCampo(busca, { busca = it }, "O quê (módulo, ação, ajuste…)")
            val t = busca.trim().lowercase()
            if (t.isNotEmpty()) catalogo.filter { "${it.rotulo} ${it.chave}".lowercase().contains(t) }.take(12).forEach { k ->
                Text("${k.tipo} · ${k.rotulo}", color = c.texto, modifier = Modifier.fillMaxWidth().clickable { chave = k; busca = ""; res = null }.padding(10.dp))
            }
        } else Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${chave!!.tipo}: ${chave!!.rotulo}", color = c.texto, modifier = Modifier.weight(1f))
            PwBotaoVidro("Trocar", onClick = { chave = null; res = null })
        }
        PwBotaoOuro("Explicar", habilitado = pessoa != null && chave != null, onClick = {
            escopo.launch { dev("dev_explicar", args("p_pessoa" to pessoa!!.first, "p_chave" to chave!!.chave)).onSuccess { res = it.comoObjeto() }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) } }
        })
        res?.let { Text("${valorTexto(it["valor"])} — ${it.str("motivo")}", color = c.texto, fontWeight = FontWeight.SemiBold) }
    }
}

// ───────────────────────────────────────────────────────────── Matriz pessoa × permissão
@Composable
internal fun MatrizDevG() {
    val c = Pw.cores
    val catalogo = rememberCatalogo()
    var empresas by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var org by remember { mutableStateOf<String?>(null) }
    val chaves = remember { androidx.compose.runtime.mutableStateListOf("modulo:pedidos", "modulo:carteira", "modulo:chat", "acao:criar_contas", "especial_ignora_local") }
    var linhas by remember { mutableStateOf<List<JsonObject>?>(null) }
    var regras by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var busca by remember { mutableStateOf("") }
    var versao by remember { mutableIntStateOf(0) }
    var salvando by remember { mutableStateOf<String?>(null) }
    val escopo = rememberCoroutineScope()
    LaunchedEffect(Unit) { dev("dev_comando").onSuccess { empresas = it.comoObjeto()?.arr("empresas").orEmpty() } }
    fun regraDe(esc: String, alvo: String, chave: String) = regras.firstOrNull { it.str("escopo") == esc && it.str("alvo") == alvo && it.str("chave") == chave }
    // muda ali mesmo: sim/não com um toque, só para a pessoa (ou para a empresa toda)
    fun gravar(esc: String, alvo: String, nome: String, chave: String, valor: Boolean, celula: String) {
        salvando = celula
        escopo.launch {
            val atual = regraDe(esc, alvo, chave)
            val p = buildJsonObject {
                atual?.forEach { (k, v) -> put(k, v) }
                put("chave", chave); put("escopo", esc); put("alvo", alvo); put("valor", valor); put("ativo", true)
                if (atual == null) { put("condicoes", buildJsonObject { }); put("porque", "Mudado na matriz para $nome") }
            }
            dev("dev_salvar_regra", args("p" to p)).onSuccess { PwToast.mostrar("${catalogo.firstOrNull { it.chave == chave }?.rotulo ?: chave}: ${if (valor) "sim" else "não"} para $nome.") }
                .onFailure { PwToast.mostrar("Não salvou: ${traduzirDev(it.message)}", TipoAviso.ERRO) }
            salvando = null; versao++
        }
    }
    fun tirar(esc: String, alvo: String, chave: String) {
        val id = regraDe(esc, alvo, chave)?.str("id") ?: return
        escopo.launch { dev("dev_apagar_regra", args("p_id" to id)).onSuccess { PwToast.mostrar("Voltou ao que vinha antes.") }; versao++ }
    }
    LaunchedEffect(org, chaves.toList(), versao) {
        val o = org ?: return@LaunchedEffect
        dev("dev_regras").onSuccess { regras = it.comoLista() }
        dev("dev_matriz", args("p_org" to o, "p_chaves" to buildJsonArray { chaves.forEach { add(JsonPrimitive(it)) } })).onSuccess { linhas = it.comoLista() }
            .onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
    }
    SeletorEmpresa(empresas, org, "Escolha a empresa") { org = it }
    PwCampo(busca, { busca = it }, "Adicionar coluna (módulo, ação, ajuste…)")
    val t = busca.trim().lowercase()
    if (t.isNotEmpty()) catalogo.filter { "${it.rotulo} ${it.chave}".lowercase().contains(t) && it.chave !in chaves }.take(10).forEach { k ->
        Text("+ ${k.tipo} · ${k.rotulo}", color = c.texto, modifier = Modifier.fillMaxWidth().clickable { chaves.add(k.chave); busca = "" }.padding(10.dp))
    }
    if (org == null) { PwVazio("Escolha uma empresa para ver o que cada pessoa (equipe e clientes) pode, e por quê.", icone = PwIcones.ShieldCheck); return }
    val l = linhas ?: run { PwCarregando(); return }
    TextoDica("Toque no valor para mudar só para aquela pessoa; ↺ volta ao que vinha antes. Para a empresa toda, use os botões abaixo de cada coluna. Vale na hora.")
    val nomeOrg = empresas.firstOrNull { it.str("id") == org }?.str("nome") ?: "a empresa"
    PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
        Text("Para $nomeOrg inteira", color = c.texto, fontWeight = FontWeight.Bold)
        chaves.forEach { k ->
            val daEmp = regraDe("empresa", org!!, k)
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(catalogo.firstOrNull { it.chave == k }?.rotulo ?: k, color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                ChipDev("Sim", daEmp?.str("valor") == "true") { gravar("empresa", org!!, nomeOrg, k, true, "e:$k") }
                Spacer(Modifier.width(4.dp))
                ChipDev("Não", daEmp?.str("valor") == "false") { gravar("empresa", org!!, nomeOrg, k, false, "e:$k") }
                if (daEmp != null) { Spacer(Modifier.width(4.dp)); ChipDev("↺", false) { tirar("empresa", org!!, k) } }
            }
        }
    }
    l.forEach { p ->
        PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(14.dp)) {
            Text(p.str("nome") ?: "—", color = c.texto, fontWeight = FontWeight.Bold)
            TextoDica(p.str("cliente")?.let { "cliente · $it" } ?: p.str("cargo") ?: p.strs("papeis").joinToString())
            chaves.forEach { k ->
                val v = p.obj("valores")?.obj(k)
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(catalogo.firstOrNull { it.chave == k }?.rotulo ?: k, color = c.texto2, fontSize = 13.sp)
                        Text(v?.str("motivo") ?: "", color = if (v?.str("fonte") == "regra") c.vermelho else c.texto3, fontSize = 11.5.sp)
                    }
                    val celula = "${p.str("id")}:$k"
                    val bool = v?.str("valor") == "true" || v?.str("valor") == "false"
                    if (regraDe("pessoa", p.str("id") ?: "", k) != null) { ChipDev("↺", false) { tirar("pessoa", p.str("id") ?: "", k) }; Spacer(Modifier.width(4.dp)) }
                    ChipDev(if (salvando == celula) "…" else valorTexto(v?.get("valor")), v?.str("fonte") == "regra") {
                        if (bool && salvando == null) gravar("pessoa", p.str("id") ?: "", p.str("nome") ?: "", k, v?.str("valor") != "true", celula)
                        else if (!bool) PwToast.mostrar("Para números e textos, use Regras do Fantasma → Pessoa.")
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────── Explorador de dados
@Composable
internal fun ExploradorDevG() {
    val c = Pw.cores
    var tabelas by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var tabela by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableStateOf("") }
    var pagina by remember { mutableIntStateOf(0) }
    var res by remember { mutableStateOf<JsonObject?>(null) }
    var aberta by remember { mutableStateOf<JsonObject?>(null) }
    var filtroT by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { dev("dev_tabelas").onSuccess { tabelas = it.comoLista() } }
    LaunchedEffect(tabela, busca, pagina) {
        val t = tabela ?: return@LaunchedEffect
        delay(300)
        dev("dev_explorar", args("p_tabela" to t, "p_busca" to busca.trim().ifBlank { null }, "p_limite" to 30, "p_pagina" to pagina, "p_ordem" to null))
            .onSuccess { res = it.comoObjeto() }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
    }
    if (tabela == null) {
        TextoDica("Qualquer tabela do sistema. Só para olhar: nada aqui muda dados.")
        PwCampo(filtroT, { filtroT = it }, "Buscar tabela")
        tabelas.filter { (it.str("tabela") ?: "").contains(filtroT.lowercase()) }.forEach { t ->
            Row(Modifier.fillMaxWidth().clickable { tabela = t.str("tabela"); pagina = 0; busca = ""; res = null }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Database, null, tint = c.texto3, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                Text(t.str("tabela") ?: "", color = c.texto, modifier = Modifier.weight(1f))
                Text("%,d".format(BR, t.long("linhas") ?: 0L), color = c.texto3, fontSize = 12.sp)
            }
        }
        return
    }
    PwBotaoVidro("Tabelas", onClick = { tabela = null; aberta = null }, icone = PwIcones.ChevronLeft)
    Text(tabela!!, color = c.texto, fontWeight = FontWeight.Black, fontSize = 18.sp)
    PwCampo(busca, { busca = it; pagina = 0 }, "Buscar em qualquer coluna")
    val r = res ?: run { PwCarregando(); return }
    val total = r.long("total") ?: 0L
    TextoDica("$total linha(s) · página ${pagina + 1}")
    r.arr("linhas").forEach { l ->
        val resumo = l.entries.take(4).joinToString(" · ") { (k, v) -> "$k: ${valorTexto(v).take(40)}" }
        Text(resumo, color = c.texto2, fontSize = 12.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { aberta = l }.background(c.vidroSutil).padding(10.dp))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwBotaoVidro("Anterior", habilitado = pagina > 0, onClick = { pagina-- }, modifier = Modifier.weight(1f))
        PwBotaoVidro("Próxima", habilitado = (pagina + 1) * 30 < total, onClick = { pagina++ }, modifier = Modifier.weight(1f))
    }
    aberta?.let { a -> PwJanela("Linha", { aberta = null }) { a.entries.forEach { (k, v) -> Text("$k", color = c.texto3, fontSize = 12.sp); Text(valorTexto(v), color = c.texto, fontSize = 14.sp) } } }
}

// ───────────────────────────────────────────────────────────── Registro do Dev
@Composable
internal fun RegistroDevG() {
    val c = Pw.cores
    var l by remember { mutableStateOf<List<JsonObject>?>(null) }
    var aberto by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) { dev("dev_registro", args("p_limite" to 150, "p_antes" to null, "p_area" to null)).onSuccess { l = it.comoLista() } }
    TextoDica("Tudo que o Dev mudou, com o antes e o depois. Só você vê.")
    val lista = l ?: run { PwCarregando(); return }
    if (lista.isEmpty()) PwVazio("Nada registrado ainda.", icone = PwIcones.History)
    lista.forEach { r ->
        val acao = when (r.str("acao")) { "insert" -> "criou"; "update" -> "mudou"; "delete" -> "apagou"; else -> r.str("acao") }
        Column(Modifier.fillMaxWidth().clickable { aberto = if (aberto == r.long("id")) null else r.long("id") }.padding(vertical = 6.dp)) {
            Text("${r.str("dev") ?: "Dev"} $acao · ${r.str("area")}${r.str("alvo")?.let { " · $it" } ?: ""}", color = c.texto, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            TextoDica(com.pwexpress.feature.gestor.data.iso(r.str("em"))?.let { dataHora(it) } ?: "")
            if (aberto == r.long("id")) Text(r["detalhe"]?.toString() ?: "", color = c.texto2, fontSize = 11.5.sp)
        }
    }
}

// ───────────────────────────────────────────────────────────── Novidades por área
private val AREAS_NOV = listOf("todos" to "Todos", "motoboy" to "Motoboy", "gestor" to "Gestor", "supervisor" to "Supervisor", "cliente" to "Cliente", "dev" to "Dev")

@Composable
internal fun NovidadesDevG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var l by remember { mutableStateOf<List<JsonObject>?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    var ver by remember { mutableStateOf("") }
    var area by remember { mutableIntStateOf(0) }
    var titulo by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    LaunchedEffect(versao) { dev("dev_novidades").onSuccess { l = it.comoLista() } }
    TextoDica("Cada pessoa vê só as novidades das áreas dela. O Dev vê todas; a área Dev só o Dev vê.")
    PwCartao {
        PwCampo(ver, { ver = it }, "Versão", dica = "Ex.: Lote 51")
        PwSegmentado(AREAS_NOV.map { it.second to null }, area, { area = it })
        PwCampo(titulo, { titulo = it }, "Título")
        PwCampo(texto, { texto = it }, "Texto", linhas = 2)
        PwBotaoOuro("Publicar", habilitado = ver.isNotBlank() && titulo.isNotBlank(), onClick = {
            escopo.launch {
                dev("dev_salvar_novidade", args("p" to buildJsonObject { put("versao", ver); put("area", AREAS_NOV[area].first); put("titulo", titulo); put("texto", texto) }))
                    .onSuccess { PwToast.mostrar("Novidade publicada."); titulo = ""; texto = ""; versao++ }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
            }
        })
    }
    val lista = l ?: run { PwCarregando(); return }
    lista.groupBy { it.str("versao") ?: "" }.forEach { (v, itens) ->
        PwRotulo(v)
        itens.forEach { n ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(n.str("titulo") ?: "", color = if (n.bool("publicado")) c.texto else c.texto3, fontWeight = FontWeight.SemiBold)
                    TextoDica("${AREAS_NOV.firstOrNull { it.first == n.str("area") }?.second ?: n.str("area")}${n.str("texto")?.let { " · $it" } ?: ""}")
                }
                com.pwexpress.core.designsystem.PwBotaoIcone(if (n.bool("publicado")) PwIcones.Eye else PwIcones.EyeOff, if (n.bool("publicado")) "Esconder" else "Publicar", {
                    escopo.launch { dev("dev_salvar_novidade", args("p" to buildJsonObject { n.forEach { (k, x) -> put(k, x) }; put("publicado", !n.bool("publicado")) })).onSuccess { versao++ } }
                })
                Spacer(Modifier.width(6.dp))
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Trash2, "Apagar", { escopo.launch { dev("dev_apagar_novidade", args("p_id" to n.str("id"))).onSuccess { versao++ } } })
            }
        }
    }
}

/** Ícone e nome das áreas do Delibra Dev (mesma ordem do site). */
internal val AREAS_DEV: List<Triple<String, String, ImageVector>> = listOf(
    Triple("comando", "Comando ao vivo", PwIcones.Radar),
    Triple("regras", "Regras do Fantasma", PwIcones.ShieldAlert),
    Triple("modulos", "Módulos", PwIcones.ToggleRight),
    Triple("permissoes", "Permissões", PwIcones.ShieldCheck),
    Triple("ajustes", "Ajustes da plataforma", PwIcones.Settings2),
    Triple("empresas", "Empresas", PwIcones.Building2),
    Triple("pessoas", "Pessoas", PwIcones.Users),
    Triple("dados", "Explorador de dados", PwIcones.Database),
    Triple("auditoria", "Auditoria", PwIcones.History),
    Triple("novidades", "Novidades", PwIcones.Megaphone),
    Triple("sistema", "Sistema", PwIcones.Activity),
    Triple("conta", "Minha conta Dev", PwIcones.UserRound),
)

// ───────────────────────────────────────────────────────────── Ver como
/** O sistema responde para o Dev exatamente como responderia para a pessoa (sem o Dev perder nada). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VerComoDevG() {
    val c = Pw.cores
    val catalogo = rememberCatalogo()
    var pessoa by remember { mutableStateOf<Pair<String, String>?>(null) }
    var v by remember { mutableStateOf<JsonObject?>(null) }
    var busca by remember { mutableStateOf("") }
    LaunchedEffect(pessoa) {
        val p = pessoa ?: return@LaunchedEffect
        v = null
        dev("dev_ver_como", args("p_pessoa" to p.first)).onSuccess { v = it.comoObjeto() }.onFailure { PwToast.mostrar(traduzirDev(it.message), TipoAviso.ERRO) }
    }
    fun rot(k: String) = catalogo.firstOrNull { it.chave == k }?.rotulo ?: k
    BuscaAlvo("perfis", "Ver o sistema como", pessoa?.second) { id, n -> pessoa = id to n }
    if (pessoa == null) { PwVazio("Escolha uma pessoa: você vê os módulos, ações, ajustes, avisos e novidades como chegam para ela.", icone = PwIcones.Eye); return }
    val x = v ?: run { PwCarregando(); return }
    val perfil = x.obj("perfil")
    Text("${perfil?.str("nome")} · ${perfil?.strs("papeis")?.joinToString()}${perfil?.obj("cargo")?.str("nome")?.let { " · cargo $it" } ?: ""}", color = c.texto, fontWeight = FontWeight.Bold)
    val mods = perfil?.obj("modulos").orEmpty()
    val off = mods.filter { (it.value as? JsonPrimitive)?.content == "false" }.keys
    PwRotulo("Módulos desligados (${off.size})")
    if (off.isEmpty()) TextoDica("Nenhum: vê tudo que a empresa tem.")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { off.forEach { ChipDev(rot("modulo:$it"), false) {} } }
    val acoes = x.obj("permissoes")?.obj("acoes").orEmpty().filter { (it.value as? JsonPrimitive)?.content == "false" }.keys
    PwRotulo("Ações bloqueadas (${acoes.size})")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { acoes.forEach { ChipDev(rot("acao:$it"), false) {} } }
    PwRotulo("Avisos e novidades que chegam")
    val av = (x["avisos"] as? JsonArray).comoLista()
    if (av.isEmpty()) TextoDica("Nenhum aviso agora.") else av.forEach { TextoDica("• ${it.str("texto")}") }
    TextoDica("${(x["novidades"] as? JsonArray)?.size ?: 0} novidade(s)")
    PwRotulo("Ajustes que o app e o site recebem")
    PwCampo(busca, { busca = it }, "Buscar ajuste")
    val t = busca.trim().lowercase()
    perfil?.obj("configuracoes").orEmpty().entries.filter { t.isEmpty() || "${it.key} ${rot(it.key)}".lowercase().contains(t) }.take(80).forEach { (k, valor) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(rot(k), color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(valorTexto(valor), color = c.texto, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}
