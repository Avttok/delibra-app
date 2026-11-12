package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val CARGOS_DEV = listOf("gestor" to "Gestor", "supervisor" to "Supervisor", "motoboy" to "Motoboy", "cliente" to "Cliente", "dev" to "Dev")
private fun JsonObject.bool(k: String) = (this[k] as? JsonPrimitive)?.booleanOrNull == true
private fun JsonObject.lista(k: String) = (this[k] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }

internal fun haQuanto(iso: String?): String {
    if (iso.isNullOrBlank()) return "nunca"
    val t = runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull() ?: return "—"
    val s = java.time.Duration.between(t, java.time.Instant.now()).seconds.coerceAtLeast(0)
    return when { s < 60 -> "agora"; s < 3600 -> "há ${s / 60} min"; s < 86400 -> "há ${s / 3600} h"; else -> "há ${s / 86400} d" }
}

private fun corCargo(p: String, c: com.pwexpress.core.designsystem.PwPaleta) = when (p) {
    "gestor" -> c.ouro; "supervisor" -> c.azul; "motoboy" -> c.verde; "dev" -> c.dev; else -> c.texto2
}

private fun senhaDev(): String {
    val l = "abcdefghjkmnpqrstuvwxyz"; val n = "23456789"; val r = java.security.SecureRandom()
    fun p() = (1..4).map { l[r.nextInt(l.length)] }.joinToString("")
    return "${p()}-${p()}-${n[r.nextInt(n.length)]}${n[r.nextInt(n.length)]}"
}

/** Dev: todas as contas do sistema, com cargos, empresa, senha, bloqueio e offline (igual ao site). */
@Composable
internal fun DevUsuariosG() {
    val c = Pw.cores
    var busca by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf<String?>(null) }
    var lista by remember { mutableStateOf<List<JsonObject>?>(null) }
    var orgs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var aberto by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(busca, versao) {
        delay(250)
        Gestao.rpcJson("dev_usuarios", buildJsonObject { put("p_busca", busca.trim().ifBlank { null }?.let { JsonPrimitive(it) } ?: JsonNull); put("p_limite", 500) })
            .onSuccess { j -> lista = (j as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }; erro = null }.onFailure { erro = it.message }
    }
    LaunchedEffect(Unit) {
        Gestao.rpcJson("dev_organizacoes").onSuccess { j -> orgs = (j as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let { o -> (o.str("id") ?: return@let null) to (o.str("nome") ?: "Empresa") } } }
    }
    PwCampo(busca, { busca = it }, "Buscar por nome, e-mail ou empresa")
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Chip("Todos", cargo == null) { cargo = null }
        CARGOS_DEV.forEach { (p, r) -> Chip("$r ${lista.orEmpty().count { p in it.lista("papeis") }}", cargo == p) { cargo = p } }
    }
    PwErro(erro)
    val l = lista ?: run { PwCarregando(); return }
    val vis = l.filter { cargo == null || cargo in it.lista("papeis") }
    TextoDica("${vis.size} de ${l.size} contas")
    if (vis.isEmpty()) PwVazio("Ninguém com esse filtro.")
    // separados por empresa (sem empresa e o Dev no fim)
    vis.groupBy { it.str("organizacao") ?: if ("dev" in it.lista("papeis")) "Plataforma (Dev)" else "Sem empresa" }
        .toSortedMap(compareBy<String>({ it.startsWith("Plataforma") || it == "Sem empresa" }, { it })).forEach { (nomeOrg, us) ->
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Building2, null, tint = c.ouro, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(6.dp))
                Text(nomeOrg, color = c.ouro, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("${us.size}", color = c.texto3, fontSize = 12.sp)
            }
            us.forEach { u ->
                val id = u.str("id") ?: return@forEach
                UsuarioDevG(u, orgs, aberto == id, { aberto = if (aberto == id) null else id }, { versao++ })
            }
        }
}

@Composable
private fun Chip(texto: String, ligado: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    Text(texto, color = if (ligado) c.sobreOuro else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(if (ligado) c.ouro else c.campo).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

@Composable
private fun UsuarioDevG(u: JsonObject, orgs: List<Pair<String, String>>, aberto: Boolean, onAbrir: () -> Unit, onMudou: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    val id = u.str("id")!!
    val papeisOrig = u.lista("papeis")
    var papeis by remember(id, papeisOrig.joinToString()) { mutableStateOf(papeisOrig) }
    var ocupado by remember { mutableStateOf(false) }
    var senha by remember { mutableStateOf<String?>(null) }
    var escolherOrg by remember { mutableStateOf(false) }
    val mb = u.obj("motoboy")
    val online = mb != null && mb.str("status") != "offline"
    val bloqueado = u.bool("bloqueado")
    fun rodar(ok: String, f: suspend () -> Result<*>) {
        ocupado = true
        escopo.launch { f().onSuccess { PwToast.mostrar(ok); onMudou() }.onFailure { PwToast.mostrar(it.message ?: "Não deu certo.", TipoAviso.ERRO) }; ocupado = false }
    }
    PwCartao(cor = if (aberto) c.dev.copy(alpha = 0.5f) else null, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onAbrir), verticalAlignment = Alignment.CenterVertically) {
            PwAvatar(u.str("nome").orEmpty(), u.str("foto"), tamanho = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(u.str("nome").orEmpty(), color = c.texto, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(u.str("email"), u.str("organizacao")).joinToString(" · "), color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(haQuanto(u.str("ultimo_acesso")), color = c.texto3, fontSize = 11.sp)
        }
        Row(Modifier.padding(top = 6.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            papeisOrig.forEach { p -> PwSelo(CARGOS_DEV.firstOrNull { it.first == p }?.second ?: p, corCargo(p, c)) }
            if (bloqueado) PwSelo("Bloqueada", c.vermelho, icone = PwIcones.Lock)
            if (online) PwSelo(mb!!.str("status").orEmpty().replace("_", " "), c.verde, icone = PwIcones.Wifi)
        }
        if (aberto) {
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (mb != null) TextoDica("App ${mb.str("versao") ?: "—"}${mb.str("aparelho")?.let { " · $it" } ?: ""} · bateria ${mb.int("bateria")?.let { "$it%" } ?: "—"} · posição ${haQuanto(mb.str("posicao_em"))}")
                TextoDica("Criada ${u.str("criado_em")?.take(10) ?: "—"} · ID ${id.take(8)}…")
                PwRotulo("Cargos")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CARGOS_DEV.forEach { (p, r) -> Chip(r, p in papeis) { papeis = if (p in papeis) papeis - p else papeis + p } }
                }
                if (papeis.sorted() != papeisOrig.sorted()) PwBotaoOuro("Salvar cargos", icone = PwIcones.Check, habilitado = !ocupado, onClick = {
                    rodar("Cargos salvos.") { Gestao.rpcJson("dev_definir_papeis", buildJsonObject { put("p_id", id); put("p_papeis", buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } }) }) }
                })
                PwBotaoVidro("Empresa: ${u.str("organizacao") ?: "sem empresa"}", onClick = { escolherOrg = !escolherOrg }, icone = PwIcones.Building2, modifier = Modifier.fillMaxWidth())
                if (escolherOrg) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    (listOf("" to "Sem empresa") + orgs).forEach { (oid, nome) ->
                        Text(nome, color = if (oid == (u.str("organizacao_id") ?: "")) c.ouro else c.texto, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                            escolherOrg = false
                            rodar("Conta movida para $nome.") { Gestao.rpcJson("dev_mover_pessoa", buildJsonObject { put("p_usuario", id); put("p_org", if (oid.isBlank()) JsonNull else JsonPrimitive(oid)) }) }
                        }.padding(10.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoVidro("Nova senha", icone = PwIcones.KeyRound, habilitado = !ocupado, modifier = Modifier.weight(1f), onClick = {
                        val s = senhaDev(); rodar("Senha trocada.") { Gestao.redefinirSenha(id, s).onSuccess { senha = s } }
                    })
                    PwBotaoVidro(if (bloqueado) "Liberar" else "Bloquear", icone = if (bloqueado) PwIcones.LockOpen else PwIcones.Lock, habilitado = !ocupado, modifier = Modifier.weight(1f), onClick = {
                        rodar(if (bloqueado) "Conta liberada." else "Conta bloqueada.") { Gestao.bloquear(id, !bloqueado) }
                    })
                }
                if (online) PwBotaoVidro("Deixar offline", icone = PwIcones.Power, habilitado = !ocupado, modifier = Modifier.fillMaxWidth(), onClick = {
                    rodar("Motoboy ficou offline.") { Gestao.rpcJson("dev_forcar_offline", buildJsonObject { put("p_motoboy", id) }) }
                })
                senha?.let { Text("E-mail: ${u.str("email")}\nSenha: $it", fontFamily = FontFamily.Monospace, color = c.texto,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.campo).padding(12.dp)) }
            }
        }
    }
}

/** Dev: saúde do sistema (banco, GPS, tarefas e migrações) e o botão de afinar o GPS. */
@Composable
internal fun DevSaudeG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var s by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(versao) { Gestao.rpcJson("dev_saude").onSuccess { s = it as? JsonObject; erro = null }.onFailure { erro = it.message } }
    PwErro(erro)
    val d = s ?: run { PwCarregando(); return }
    val tarefas = d.arr("tarefas")
    val falhando = tarefas.count { (it.int("falhas_24h") ?: 0) > 0 }
    listOf(
        listOf("Banco" to "${d.dbl("banco_mb") ?: 0.0} MB", "Pings 1 h" to "%,d".format(d.int("pings_1h") ?: 0)),
        listOf("Pings 24 h" to "%,d".format(d.int("pings_24h") ?: 0), "Online" to "${d.int("motoboys_online") ?: 0}"),
        listOf("Rotas na rua" to "${d.int("rotas_ativas") ?: 0}", "Tarefas" to if (falhando > 0) "$falhando com falha" else "Tudo certo"),
        listOf("Contas" to "${d.int("contas") ?: 0}", "Empresas" to "${d.int("empresas") ?: 0}"),
    ).forEach { par -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { par.forEach { (r, v) -> PwNumero(r, v, Modifier.weight(1f)) } } }
    PwBotaoOuro(if (ocupado) "Afinando…" else "Afinar GPS agora", icone = PwIcones.Activity, habilitado = !ocupado, onClick = {
        ocupado = true
        escopo.launch {
            Gestao.rpcJson("dev_afinar_gps").onSuccess { j -> val o = j as? JsonObject
                PwToast.mostrar("Saíram ${o?.int("rotas") ?: 0} pontos de rotas antigas e ${o?.int("fora_de_rota") ?: 0} fora de rota.", titulo = "GPS afinado"); versao++ }
                .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
            ocupado = false
        }
    })
    TextoDica("Tira pontos repetidos de rotas antigas e deixa um a cada intervalo fora de rota (Ajustes → Rastreamento).")
    PwCartao {
        PwRotulo("Maiores tabelas")
        val tab = d.arr("tabelas"); val max = (tab.maxOfOrNull { it.dbl("mb") ?: 0.0 } ?: 1.0).coerceAtLeast(0.1)
        tab.forEach { t ->
            Column(Modifier.padding(top = 8.dp)) {
                Row { Text(t.str("nome").orEmpty(), color = c.texto, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text("${t.dbl("mb") ?: 0.0} MB", color = c.texto3, fontSize = 12.sp) }
                Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.borda)) {
                    Box(Modifier.fillMaxWidth(((t.dbl("mb") ?: 0.0) / max).toFloat()).fillMaxHeight().background(c.dev))
                }
            }
        }
    }
    PwCartao {
        PwRotulo("Tarefas automáticas")
        if (tarefas.isEmpty()) TextoDica("O agendador não respondeu.")
        tarefas.forEach { t ->
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t.str("nome").orEmpty().removePrefix("pwex-"), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("${t.str("quando")} · última ${haQuanto(t.str("ultima"))}", color = c.texto3, fontSize = 11.sp)
                }
                val f = t.int("falhas_24h") ?: 0
                PwSelo(if (f > 0) "$f falha(s)" else "ok", if (f > 0) c.vermelho else c.verde)
            }
        }
    }
    val mig = d.arr("migracoes")
    if (mig.isNotEmpty()) PwCartao {
        PwRotulo("Últimas atualizações do banco")
        mig.forEach { m -> Text(m.str("nome").orEmpty().replace(Regex("^\\d+_"), "").removeSuffix(".sql").replace('_', ' '), color = c.texto2, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

/** Dev: módulos externos (pagamento, nota, e-mail, WhatsApp) e se já estão conectados. */
@Composable
internal fun DevIntegracoesG() {
    val c = Pw.cores
    var lista by remember { mutableStateOf<List<JsonObject>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching { PwCore.rpc.site("/api/integracoes/status", buildJsonObject { }) }
            .onSuccess { r -> lista = runCatching { (kotlinx.serialization.json.Json.parseToJsonElement(r.corpo) as? JsonObject)?.get("modulos") as? JsonArray }.getOrNull()?.mapNotNull { it as? JsonObject }
                if (lista == null) erro = "Não deu para ver os módulos agora." }
            .onFailure { erro = "Não deu para ver os módulos agora." }
    }
    TextoDica("Cada módulo está pronto e desligado até conectar: a chave vai nas variáveis do Vercel e o provedor se escolhe em Ajustes → Integrações. Não mande chaves no chat.")
    PwErro(erro)
    val l = lista ?: run { if (erro == null) PwCarregando(); return }
    l.groupBy { it.str("area").orEmpty() }.forEach { (area, itens) ->
        PwCartao {
            PwRotulo(area)
            itens.forEach { m ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.str("nome").orEmpty(), color = c.texto, fontWeight = FontWeight.SemiBold)
                        Text(m.str("descricao").orEmpty(), color = c.texto3, fontSize = 12.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    if (m.bool("conectado")) PwSelo("Conectado", c.verde, icone = PwIcones.Check) else PwSelo("Conectar depois", c.texto3, icone = PwIcones.Plug)
                }
            }
        }
    }
}

/** Dev: todas as conversas da plataforma; apagar uma conversa inteira. */
@Composable
internal fun DevConversasG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<com.pwexpress.core.chat.Conversa>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var versao by remember { mutableStateOf(0) }
    var apagar by remember { mutableStateOf<com.pwexpress.core.chat.Conversa?>(null) }
    LaunchedEffect(versao) { com.pwexpress.core.chat.Chat.todasDev().onSuccess { lista = it }.onFailure { erro = it.message } }
    TextoDica("Para ler e responder, abra o Chat: lá aparecem também as conversas de que você não participa.")
    PwErro(erro)
    val l = lista ?: run { PwCarregando(); return }
    if (l.isEmpty()) PwVazio("Nenhuma conversa ainda.")
    l.groupBy { it.empresa ?: "Plataforma" }.forEach { (emp, cs) ->
        PwRotulo(emp)
        cs.forEach { cv ->
            PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(cv.nome, color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(cv.previa ?: "Sem mensagens", color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Trash2, "Apagar conversa", { apagar = cv })
                }
            }
        }
    }
    apagar?.let { cv ->
        com.pwexpress.core.designsystem.PwJanela("Apagar \"${cv.nome}\"?", onFechar = { apagar = null }) {
            TextoDica("Some para todos, com as mensagens. Não dá para desfazer.")
            PwBotaoPerigo("Apagar conversa", {
                escopo.launch { com.pwexpress.core.chat.Chat.apagarConversaDev(cv.id).onSuccess { PwToast.mostrar("Conversa apagada."); versao++ }.onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; apagar = null }
            })
        }
    }
}

/** Dev: o que foi apagado no sistema (contas e rotas), por quem e por quê. */
@Composable
internal fun DevRegistroG(painel: JsonObject?) {
    val c = Pw.cores
    val l = painel?.arr("exclusoes") ?: run { PwCarregando(); return }
    if (l.isEmpty()) { PwVazio("Nada apagado até agora."); return }
    l.forEach { x ->
        PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
            Text("${if (x.str("tipo") == "conta") "Conta" else "Rota"} ${x.str("rotulo").orEmpty()}", color = c.texto, fontWeight = FontWeight.SemiBold)
            Text("${x.str("por") ?: "—"} · ${haQuanto(x.str("quando"))}", color = c.texto3, fontSize = 12.sp)
            x.str("motivo")?.let { Text(it, color = c.texto2, fontSize = 13.sp) }
        }
    }
}

/** Dev: ficar oculto para todos (some de chats, registros e listas). */
@Composable
internal fun DevOcultoG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    val perfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    val oculto = perfil?.devOculto == true
    PwCartao(cor = if (oculto) c.dev.copy(alpha = 0.6f) else null) {
        PwInterruptor("Dev oculto", oculto, { sim ->
            escopo.launch { com.pwexpress.core.perfil.Perfis.definirOculto(sim).onSuccess { PwToast.mostrar(if (sim) "Agora ninguém vê você em lugar nenhum." else "Você voltou a aparecer.") }
                .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) } }
        }, descricao = "Ligado: você some de chats, grupos, registros, exclusões, ranking e listas para quem não é Dev.", icone = PwIcones.EyeOff, cor = c.dev)
    }
}
