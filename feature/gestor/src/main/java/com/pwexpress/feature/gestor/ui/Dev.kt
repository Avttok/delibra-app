package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject

private val CONTAGENS_GRUPOS = listOf(
    "Pessoas" to listOf("contas" to "Contas", "motoboys" to "Motoboys", "gestores" to "Gestores", "clientes" to "Clientes", "motoboys_online" to "Online agora", "em_corrida" to "Em corrida"),
    "Pedidos e rotas" to listOf("pedidos" to "Pedidos", "pedidos_abertos" to "Abertos", "corridas_reais" to "Rotas reais", "corridas_teste" to "De teste",
        "corridas_ativas" to "Ativas", "no_quadro" to "No quadro", "ofertas_pendentes" to "Ofertas pendentes"),
    "GPS e mapa" to listOf("pings_24h" to "Pings 24 h", "pings_total" to "Pings no banco", "trechos_ruas" to "Trechos pelas ruas"),
    "Dinheiro (30 dias)" to listOf("faturado_30d" to "Faturado", "motoboys_30d" to "Motoboys", "dev_30d" to "Dev", "faturas" to "Faturas", "holerites" to "Holerites"),
    "Sistema" to listOf("mensagens_chat" to "Mensagens no chat", "excecoes" to "Exceções", "configuracoes_mudadas" to "Ajustes mudados", "corridas_apagadas" to "Rotas apagadas",
        "contas_excluidas" to "Contas excluídas", "tamanho_banco_mb" to "Banco (MB)"),
)
private val PAPEIS = listOf(Triple("gestor", "Gestores", PwIcones.ShieldCheck), Triple("motoboy", "Motoboys", PwIcones.Bike), Triple("cliente", "Clientes", PwIcones.UserRound))

/** Console do Dev (igual ao do site): módulos liga/desliga e quem usa, sistema, aparelhos e registro. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevG(onVoltar: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var aba by remember { mutableIntStateOf(0) }
    var modulos by remember { mutableStateOf<List<Gestao.ModuloDev>?>(null) }
    var painel by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }

    suspend fun carregar() {
        Gestao.modulos().onSuccess { modulos = it }.onFailure { erro = it.message }
        Gestao.painelDev().onSuccess { painel = it }.onFailure { erro = it.message }
    }
    LaunchedEffect(Unit) { carregar() }

    fun salvar(m: Gestao.ModuloDev, ligado: Boolean, papeis: List<String>) {
        modulos = modulos?.map { if (it.chave == m.chave) it.copy(ligado = ligado, papeis = papeis) else it }
        escopo.launch {
            Gestao.definirModulo(m.chave, ligado, papeis)
                .onSuccess { PwToast.mostrar("${m.nome}: ${if (ligado) "ligado" else "desligado"}"); Perfis.carregar() }
                .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO); carregar() }
        }
    }

    PwPagina {
        Cabecalho("Console Dev", onVoltar, subtitulo = "O sistema inteiro, sem filtro")
        PwFiltros(listOf("Módulos" to 0, "Sistema" to 0, "Aparelhos" to 0, "Registro" to 0, "Versão do app" to 0, "Tarefas" to 0, "Apagar" to 0), aba, { aba = it })
        PwErro(erro)
        if (aba == 4) { VersaoAppG(); return@PwPagina }
        if (aba == 6) { ApagarTudoG(); return@PwPagina }
        if (aba == 5) { TarefasDevG(painel?.arr("tarefas"), onMudou = { escopo.launch { carregar() } }); return@PwPagina }
        when (aba) {
            0 -> {
                val l = modulos ?: run { PwCarregando(); return@PwPagina }
                ModulosDevG(l) { m, ligado, papeis -> salvar(m, ligado, papeis) }
            }
            1 -> {
                val cont = painel?.obj("contagens") ?: run { PwCarregando(); return@PwPagina }
                PwBotaoVidro("Atualizar números", onClick = { escopo.launch { carregar() } }, icone = PwIcones.RefreshCw, modifier = Modifier.fillMaxWidth())
                CONTAGENS_GRUPOS.forEach { (g, lista) ->
                    PwRotulo(g)
                    lista.chunked(2).forEach { par ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            par.forEach { (k, r) ->
                                val v = cont.dbl(k)
                                PwNumero(r, if (k.endsWith("_30d")) moedaBr(v ?: 0.0) else if (v != null && v % 1.0 != 0.0) numeroBr(v) else "%,d".format(BR, v?.toLong() ?: 0L), Modifier.weight(1f))
                            }
                            if (par.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            2 -> AparelhosDevG(painel?.arr("aparelhos"))
            else -> {
                val ex = painel?.arr("exclusoes") ?: run { PwCarregando(); return@PwPagina }
                if (ex.isEmpty()) PwVazio("Nada apagado até agora.", icone = PwIcones.History)
                ex.forEach { x ->
                    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("${if (x.str("tipo") == "conta") "Conta" else "Corrida"} ${x.str("rotulo") ?: ""}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica(listOfNotNull(x.str("por")?.let { "por $it" }, com.pwexpress.feature.gestor.data.iso(x.str("quando"))?.let { dataHora(it) }, x.str("motivo")?.let { "“$it”" }).joinToString(" · "))
                    }
                }
            }
        }
    }
}


/**
 * Versão do app que deve rodar (igual ao site): uma build escolhida ou "sempre a mais nova".
 * Com "forçar" ligado, qualquer outra versão trava na tela de atualização e o servidor recusa as chamadas dela.
 */
@Composable
private fun VersaoAppG() {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val escopo = rememberCoroutineScope()
    var builds by remember { mutableStateOf<List<Pair<Int, String>>?>(null) }
    var emUso by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var digitada by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    fun cfg(k: String) = (dados.config[k] as? kotlinx.serialization.json.JsonPrimitive)?.content
    val exigida = cfg("app_versao_exigida")?.toDoubleOrNull()?.toInt() ?: 0
    val forcar = cfg("app_forcar_versao") != "false"
    val devLivre = cfg("app_dev_livre") != "false"
    LaunchedEffect(Unit) {
        emUso = (Gestao.rpcJson("versoes_em_uso").getOrNull() as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        builds = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val req = okhttp3.Request.Builder().url("https://api.github.com/repos/${com.pwexpress.core.config.PwConfig.githubRepo}/releases?per_page=40")
                    .header("Accept", "application/vnd.github+json").header("User-Agent", "Delibra-app").build()
                com.pwexpress.core.PwCore.http.client.newCall(req).execute().use { r ->
                    if (!r.isSuccessful) error("GitHub ${r.code}")
                    (kotlinx.serialization.json.Json.parseToJsonElement(r.body!!.string()) as kotlinx.serialization.json.JsonArray).mapNotNull { e ->
                        val o = e as? JsonObject ?: return@mapNotNull null
                        val tag = (o["tag_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                        val b = tag.removePrefix("build-").toIntOrNull()?.takeIf { tag.startsWith("build-") } ?: return@mapNotNull null
                        b to ((o["published_at"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.take(10).orEmpty())
                    }.sortedByDescending { it.first }
                }
            }.onFailure { erro = "Lista do GitHub indisponível (${it.message}). Digite a build." }.getOrDefault(emptyList())
        }
    }
    fun salvar(k: String, v: kotlinx.serialization.json.JsonElement) = escopo.launch {
        Gestao.salvarConfig(k, v).onSuccess { Gestao.atualizar(); PwToast.mostrar("Salvo.", TipoAviso.OK) }.onFailure { erro = it.message }
    }
    PwCartao(destaque = true) {
        PwRotulo("Rodando agora")
        Text(if (exigida > 0) "Build $exigida" else "Sempre a mais nova", style = androidx.compose.ui.text.TextStyle(fontFamily = com.pwexpress.core.designsystem.ArchivoLargo,
            fontWeight = FontWeight.Black, fontSize = 26.sp), color = c.texto)
        TextoDica("Este celular: build ${com.pwexpress.core.update.Atualizador.versaoInstalada}")
    }
    com.pwexpress.core.designsystem.PwInterruptor("Forçar a versão", forcar, { salvar("app_forcar_versao", kotlinx.serialization.json.JsonPrimitive(it)) },
        descricao = if (forcar) "Outra versão trava na tela de atualização e o servidor recusa as chamadas dela." else "Só avisa que há versão nova.")
    com.pwexpress.core.designsystem.PwInterruptor("Dev pode usar qualquer versão", devLivre, { salvar("app_dev_livre", kotlinx.serialization.json.JsonPrimitive(it)) },
        descricao = "Para testar uma build antes de liberar para todos.")
    com.pwexpress.core.designsystem.PwBotaoVidro("Sempre a mais nova", onClick = { salvar("app_versao_exigida", kotlinx.serialization.json.JsonPrimitive(0)) },
        icone = PwIcones.RefreshCw, modifier = Modifier.fillMaxWidth())
    PwErro(erro)
    PwRotulo("Escolher uma build")
    when (val l = builds) {
        null -> PwCarregando()
        else -> l.forEach { (b, data) ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (b == exigida) c.ouro.copy(alpha = 0.14f) else c.vidroSutil)
                .border(1.dp, if (b == exigida) c.ouro else c.borda, RoundedCornerShape(14.dp)).clickable { salvar("app_versao_exigida", kotlinx.serialization.json.JsonPrimitive(b)) }
                .padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Build $b", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(data, color = c.texto3, fontSize = 12.sp)
                if (b == exigida) { Spacer(Modifier.width(8.dp)); Icon(PwIcones.Check, null, tint = c.ouro, modifier = Modifier.size(18.dp)) }
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        com.pwexpress.core.designsystem.PwCampo(digitada, { digitada = it.filter(Char::isDigit) }, "Build", teclado = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1f))
        com.pwexpress.core.designsystem.PwBotaoVidro("Usar", onClick = { digitada.toIntOrNull()?.let { salvar("app_versao_exigida", kotlinx.serialization.json.JsonPrimitive(it)) } })
    }
    if (emUso.isNotEmpty()) {
        PwRotulo("Quem está em qual versão")
        emUso.forEach { m ->
            val v = (m["versao"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text((m["nome"] as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty(), color = c.texto, modifier = Modifier.weight(1f))
                Text(v?.let { "build $it" } ?: "antiga", color = if (exigida > 0 && v != exigida) c.vermelho else c.texto2, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}


/** Zera os pedidos (teste, reais ou ambos) e a numeração volta ao 1. */
@Composable
private fun ZerarPedidosG(onFeito: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var alvo by remember { mutableIntStateOf(0) }
    var confirmar by remember { mutableStateOf("") }
    var rodando by remember { mutableStateOf(false) }
    val alvos = listOf("teste" to "Só teste", "reais" to "Só reais", "ambos" to "Ambos")
    PwCartao(cor = c.vermelho.copy(alpha = 0.5f)) {
        com.pwexpress.core.designsystem.PwRotulo("Zerar pedidos")
        TextoDica("Apaga os pedidos escolhidos, as rotas deles e as faturas que ficarem vazias. A numeração volta ao 1. As rotas apagadas ficam copiadas no registro de exclusões. Não dá para desfazer.")
        Spacer(Modifier.size(8.dp))
        com.pwexpress.core.designsystem.PwSegmentado(alvos.map { it.second to null }, alvo, { alvo = it })
        Spacer(Modifier.size(8.dp))
        com.pwexpress.core.designsystem.PwCampo(confirmar, { confirmar = it.uppercase() }, "Digite ZERAR para confirmar")
        Spacer(Modifier.size(8.dp))
        com.pwexpress.core.designsystem.PwBotaoPerigo(if (rodando) "Zerando…" else "Zerar ${alvos[alvo].second.lowercase()}", onClick = {
            if (confirmar != "ZERAR" || rodando) return@PwBotaoPerigo
            rodando = true
            escopo.launch {
                Gestao.rpcJson("resetar_pedidos", kotlinx.serialization.json.buildJsonObject {
                    put("p_alvo", kotlinx.serialization.json.JsonPrimitive(alvos[alvo].first)); put("p_confirmar", kotlinx.serialization.json.JsonPrimitive("ZERAR"))
                }).onSuccess { j ->
                    val o = j as? JsonObject
                    PwToast.mostrar("${o?.long("pedidos") ?: 0} pedidos, ${o?.long("rotas") ?: 0} rotas e ${o?.long("faturas") ?: 0} faturas apagados. O próximo pedido é o nº 1.", TipoAviso.OK, "Pedidos zerados")
                    confirmar = ""; Gestao.atualizar(); onFeito()
                }.onFailure { PwToast.mostrar(if (it.message.orEmpty().contains("resetar_pedidos")) "Rode a migração 20261110000005_reset_pedidos.sql." else it.message ?: "Erro", TipoAviso.ERRO) }
                rodando = false
            }
        }, cheio = true, modifier = Modifier.fillMaxWidth())
    }
}


/**
 * Apagar do jeito que quiser: um registro, os marcados, uma categoria inteira, a operação toda ou absolutamente tudo.
 * Sempre com a senha do Dev (igual ao site).
 */
@Composable
private fun ApagarTudoG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var cats by remember { mutableStateOf<List<JsonObject>?>(null) }
    var cat by remember { mutableStateOf<JsonObject?>(null) }
    var itens by remember { mutableStateOf<List<JsonObject>?>(null) }
    var busca by remember { mutableStateOf("") }
    val marcados = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    var pedir by remember { mutableStateOf<Pair<String, List<String>?>?>(null) } // categoria + ids (null = toda)
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var apagando by remember { mutableStateOf(false) }
    suspend fun contar() { Gestao.rpcJson("dev_contagem").onSuccess { cats = (it as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e -> e as? JsonObject }.orEmpty() }.onFailure { erro = it.message } }
    suspend fun listar() {
        val k = cat?.str("chave") ?: return
        itens = null
        Gestao.rpcJson("dev_listar", kotlinx.serialization.json.buildJsonObject { put("p_categoria", k); put("p_busca", busca); put("p_limite", 200) })
            .onSuccess { itens = (it as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e -> e as? JsonObject }.orEmpty() }.onFailure { erro = it.message; itens = emptyList() }
    }
    LaunchedEffect(Unit) { contar() }
    LaunchedEffect(cat?.str("chave"), busca) { marcados.clear(); if (cat != null) { kotlinx.coroutines.delay(300); listar() } }

    ZerarPedidosG(onFeito = { escopo.launch { contar() } })
    TextoDica("Apague um por um, os marcados, uma categoria inteira ou tudo. Não tem volta: sempre pede a senha do Dev.")
    val cs = cats ?: run { PwCarregando(); return }
    if (cat == null) {
        cs.chunked(2).forEach { par ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                par.forEach { k ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(c.vidro).clickable { cat = k }.padding(12.dp)) {
                        Text(k.str("nome") ?: "", color = c.texto, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("%,d".format(BR, k.long("total") ?: 0L), color = c.ouro, fontWeight = FontWeight.Black, fontSize = 20.sp)
                        Text(k.str("descricao") ?: "", color = c.texto3, fontSize = 11.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (par.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Espaco(6)
        com.pwexpress.core.designsystem.PwBotaoPerigo("Apagar a operação (pedidos, rotas, pings, faturas, folha, chat)", { pedir = "tudo" to null })
        com.pwexpress.core.designsystem.PwBotaoPerigo("Apagar absolutamente tudo", { pedir = "tudo_mesmo" to null }, cheio = true)
        TextoDica("\"Tudo\" também apaga clientes, exceções, zonas, horários, PIX, conquistas e volta os ajustes ao padrão. Contas de acesso e módulos ficam.")
    } else {
        val k = cat!!
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.ArrowLeft, "Categorias", { cat = null; itens = null })
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(k.str("nome") ?: "", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                TextoDica("${"%,d".format(BR, k.long("total") ?: 0L)} no total")
            }
        }
        com.pwexpress.core.designsystem.PwCampo(busca, { busca = it }, "Buscar…")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.pwexpress.core.designsystem.PwBotaoPerigo("Apagar marcados (${marcados.size})", { pedir = k.str("chave")!! to marcados.toList() }, Modifier.weight(1f), habilitado = marcados.isNotEmpty())
            com.pwexpress.core.designsystem.PwBotaoPerigo("Apagar a categoria", { pedir = k.str("chave")!! to null }, Modifier.weight(1f))
        }
        val l = itens
        when {
            l == null -> PwCarregando()
            l.isEmpty() -> PwVazio("Nada aqui.", icone = PwIcones.Search)
            else -> l.forEach { x ->
                val id = x.str("id") ?: return@forEach
                val marc = id in marcados
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (marc) c.vermelho.copy(alpha = 0.1f) else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { if (marc) marcados.remove(id) else marcados.add(id) }.padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (marc) PwIcones.CircleCheck else PwIcones.Plus, null, tint = if (marc) c.vermelho else c.texto3, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(x.str("titulo") ?: id, color = c.texto, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(listOfNotNull(x.str("sub"), com.pwexpress.feature.gestor.data.iso(x.str("quando"))?.let { dataHora(it) }).joinToString(" · "), color = c.texto3, fontSize = 12.sp, maxLines = 1)
                    }
                    com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Trash2, "Apagar este", { pedir = k.str("chave")!! to listOf(id) }, tamanho = 34.dp)
                }
            }
        }
    }
    pedir?.let { (categoria, ids) ->
        com.pwexpress.core.designsystem.PwJanela("Confirmar com a senha", { pedir = null; senha = ""; erro = null }) {
            Text(when {
                categoria == "tudo_mesmo" -> "Apagar ABSOLUTAMENTE TUDO do sistema."
                categoria == "tudo" -> "Apagar toda a operação."
                ids == null -> "Apagar toda a categoria ${cats?.find { it.str("chave") == categoria }?.str("nome") ?: categoria}."
                else -> "Apagar ${ids.size} registro${if (ids.size == 1) "" else "s"}."
            }, color = c.texto, fontWeight = FontWeight.Bold)
            TextoDica("Não dá para desfazer.")
            com.pwexpress.core.designsystem.PwCampo(senha, { senha = it }, "Sua senha", senha = true)
            PwErro(erro)
            com.pwexpress.core.designsystem.PwBotaoPerigo(if (apagando) "Apagando…" else "Apagar", {
                apagando = true
                escopo.launch {
                    Gestao.rpcJson("dev_apagar", kotlinx.serialization.json.buildJsonObject {
                        put("p_senha", senha); put("p_categoria", categoria)
                        if (ids != null) put("p_ids", kotlinx.serialization.json.buildJsonArray { ids.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                    }).onSuccess { r ->
                        val o = r as? JsonObject
                        when (o?.str("erro")) {
                            null -> { PwToast.mostrar("Apagado: ${o?.long("apagados") ?: 0} registro(s).", TipoAviso.OK); pedir = null; senha = ""; erro = null; marcados.clear(); contar(); listar() }
                            "senha_incorreta" -> erro = "Senha incorreta." + (o.long("restantes")?.let { " Restam $it tentativas." } ?: "")
                            "muitas_tentativas" -> erro = "Muitas tentativas. Espere alguns minutos."
                            else -> erro = o.str("erro")
                        }
                    }.onFailure { erro = it.message }
                    apagando = false
                }
            }, cheio = true, habilitado = senha.isNotBlank() && !apagando)
        }
    }
}


// ------------------------------------------------------------------ módulos (explicados)

/** Módulos: cada parte do sistema que dá para desligar, e para quem ela aparece. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModulosDevG(l: List<Gestao.ModuloDev>, salvar: (Gestao.ModuloDev, Boolean, List<String>) -> Unit) {
    val c = Pw.cores
    var busca by remember { mutableStateOf("") }
    var filtro by remember { mutableIntStateOf(0) }
    PwCartao(cor = c.ouro.copy(alpha = 0.4f)) {
        Text("O que é um módulo?", color = c.texto, fontWeight = FontWeight.Bold)
        TextoDica("Uma parte do sistema (chat, carteira, pedidos, conquistas…). Desligado, some do site e do app para todo mundo — menos para você. Ligado, dá para deixar só para alguns cargos (ex.: carteira só para gestores).")
    }
    com.pwexpress.core.designsystem.PwCampo(busca, { busca = it }, "Buscar módulo")
    val n = listOf(l.size, l.count { it.ligado }, l.count { !it.ligado }, l.count { it.ligado && it.papeis.isNotEmpty() })
    PwFiltros(listOf("Todos" to n[0], "Ligados" to n[1], "Desligados" to n[2], "Só alguns" to n[3]), filtro, { filtro = it })
    val lista = l.filter { m -> (busca.isBlank() || "${m.nome} ${m.descricao ?: ""}".contains(busca.trim(), true)) &&
        when (filtro) { 1 -> m.ligado; 2 -> !m.ligado; 3 -> m.ligado && m.papeis.isNotEmpty(); else -> true } }
    lista.groupBy { it.grupo }.forEach { (g, ms) ->
        PwRotulo(g)
        ms.forEach { m ->
            val modo = if (!m.ligado) 0 else if (m.papeis.isEmpty()) 1 else 2
            PwCartao(cor = if (modo == 2) c.ouro.copy(alpha = 0.45f) else null) {
                Text(m.nome, color = if (m.ligado) c.texto else c.texto3, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                m.descricao?.let { TextoDica(it) }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(if (modo == 0) PwIcones.X else PwIcones.Check, null, tint = when (modo) { 0 -> c.texto3; 1 -> c.verde; else -> c.ouro }, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(when (modo) { 0 -> "Desligado para todos (você, Dev, continua vendo)"; 1 -> "Ligado para todos"
                        else -> "Ligado só para " + m.papeis.joinToString(" e ") { p -> PAPEIS.firstOrNull { it.first == p }?.second?.lowercase() ?: p } },
                        color = when (modo) { 0 -> c.texto3; 1 -> c.verde; else -> c.ouro }, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
                com.pwexpress.core.designsystem.PwSegmentado(listOf("Desligado" to null, "Todos" to null, "Só alguns" to null), modo, { i ->
                    when (i) { 0 -> salvar(m, false, m.papeis); 1 -> salvar(m, true, emptyList()); else -> salvar(m, true, m.papeis.ifEmpty { listOf("gestor") }) }
                })
                if (modo == 2) FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PAPEIS.forEach { (p, rot, ic) ->
                        val on = p in m.papeis
                        PwPilula(rot, ic, { val novo = if (on) m.papeis - p else m.papeis + p; salvar(m, true, novo) }, ligada = on)
                    }
                }
            }
        }
    }
    if (lista.isEmpty()) PwVazio("Nenhum módulo com esse filtro.", icone = PwIcones.Search)
}

// ------------------------------------------------------------------ tarefas automáticas (pg_cron)

private val DIAS = listOf("domingo", "segunda", "terça", "quarta", "quinta", "sexta", "sábado")
/** "*\/5 * * * *" → "a cada 5 minutos" (o pg_cron usa UTC; o texto mostra no horário de Brasília). */
internal fun cronParaTexto(c: String): String {
    val s = c.trim()
    Regex("^(\\d+)\\s+seconds?$").find(s)?.let { return "a cada ${it.groupValues[1]} segundos" }
    val p = s.split(Regex("\\s+"))
    if (p.size != 5) return s
    val (mi, h, dm, me, ds) = p
    fun hora(hh: String, mm: String) = "%02d:%02d".format((hh.toInt() + 21) % 24, mm.toInt())
    val num = Regex("^\\d+$")
    if (s == "* * * * *") return "a cada minuto"
    Regex("^\\*/(\\d+)$").find(mi)?.let { if (h == "*" && dm == "*" && me == "*" && ds == "*") return "a cada ${it.groupValues[1]} minutos" }
    Regex("^\\*/(\\d+)$").find(h)?.let { if (num.matches(mi) && dm == "*" && me == "*" && ds == "*") return "a cada ${it.groupValues[1]} horas (no minuto $mi)" }
    if (num.matches(mi) && h == "*" && dm == "*" && me == "*" && ds == "*") return "toda hora, no minuto $mi"
    if (num.matches(mi) && num.matches(h) && me == "*") {
        if (dm == "*" && ds == "*") return "todo dia às ${hora(h, mi)}"
        if (dm == "*" && Regex("^\\d$").matches(ds)) return (if (ds == "0" || ds == "6") "todo " else "toda ") + "${DIAS[ds.toInt()]} às ${hora(h, mi)}"
        if (num.matches(dm) && ds == "*") return "todo dia $dm do mês às ${hora(h, mi)}"
    }
    return "$s (expressão cron, horário UTC)"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TarefasDevG(tf: List<JsonObject>?, onMudou: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var aberta by remember { mutableStateOf<String?>(null) }
    var cron by remember { mutableStateOf("") }
    var horaTxt by remember { mutableStateOf("03:00") }
    var ocupado by remember { mutableStateOf(false) }
    fun acao(nome: String, a: String, v: String? = null) {
        ocupado = true
        escopo.launch {
            Gestao.rpcJson("dev_tarefa", kotlinx.serialization.json.buildJsonObject { put("p_nome", nome); put("p_acao", a); v?.let { put("p_valor", it) } })
                .onSuccess { PwToast.mostrar(when (a) { "rodar" -> "Rodou."; "horario" -> "Horário mudado."; "ligar" -> "Tarefa ligada."; else -> "Tarefa parada." }); onMudou() }
                .onFailure { PwToast.mostrar(if (it.message.orEmpty().contains("horario")) "Horário inválido: use 5 partes (ex.: */5 * * * *)." else it.message ?: "Erro", TipoAviso.ERRO) }
            ocupado = false
        }
    }
    val l = tf ?: run { PwCarregando(); return }
    TextoDica("Tarefas que o banco roda sozinho (montar rotas, abrir planejadas, faturas…). Os horários aparecem no horário de Brasília.")
    if (l.isEmpty()) { PwVazio("Nenhuma tarefa automática (o pg_cron não está ligado neste banco).", icone = PwIcones.Clock); return }
    l.forEach { t ->
        val nome = t.str("nome") ?: return@forEach
        val quando = t.str("quando").orEmpty()
        val ult = t.obj("ultima")
        val ab = aberta == nome
        PwCartao(cor = if (!t.bool("ativa")) null else if (ult?.str("status") == "failed") c.vermelho.copy(alpha = 0.5f) else null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(nome, color = c.texto, fontWeight = FontWeight.Bold)
                    Text((if (t.bool("ativa")) "Roda " else "Parada · rodaria ") + cronParaTexto(quando), color = c.texto2, fontSize = 13.5.sp)
                    TextoDica(ult?.let { u -> "Última vez: " + (com.pwexpress.feature.gestor.data.iso(u.str("inicio"))?.let { dataHora(it) } ?: "—") +
                        if (u.str("status") == "succeeded") " · deu certo" else " · ${if (u.str("status") == "failed") "falhou" else u.str("status")}" } ?: "Ainda não rodou")
                    if ((t.long("falhas_24h") ?: 0) > 0) Text("${t.long("falhas_24h")} falha(s) em 24 h", color = c.vermelho, fontSize = 12.sp)
                }
                androidx.compose.material3.Switch(t.bool("ativa"), { acao(nome, if (it) "ligar" else "desligar") }, enabled = !ocupado)
                PwBotaoIcone(if (ab) PwIcones.ChevronUp else PwIcones.ChevronDown, "Mais", { aberta = if (ab) null else nome; cron = quando }, tamanho = 36.dp)
            }
            if (ab) {
                Espaco(8)
                PwRotulo("Mudar quando roda")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("* * * * *" to "A cada minuto", "*/5 * * * *" to "A cada 5 min", "*/15 * * * *" to "A cada 15 min", "0 * * * *" to "Toda hora", "0 */6 * * *" to "A cada 6 h").forEach { (cr, r) ->
                        PwPilula(r, PwIcones.Clock, { acao(nome, "horario", cr) }, ligada = quando == cr)
                    }
                }
                Espaco(6)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.pwexpress.core.designsystem.PwCampo(horaTxt, { horaTxt = it.take(5) }, "Todo dia às (Brasília)", dica = "03:00", modifier = Modifier.weight(1f))
                    PwBotaoVidro("Usar", onClick = {
                        val pr = horaTxt.split(":"); val hh = pr.getOrNull(0)?.toIntOrNull(); val mm = pr.getOrNull(1)?.toIntOrNull() ?: 0
                        if (hh != null && hh in 0..23 && mm in 0..59) acao(nome, "horario", "$mm ${(hh + 3) % 24} * * *") else PwToast.mostrar("Use HH:MM.", TipoAviso.AVISO)
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.pwexpress.core.designsystem.PwCampo(cron, { cron = it }, "Expressão cron (UTC)", modifier = Modifier.weight(1f))
                    PwBotaoVidro("Salvar", onClick = { acao(nome, "horario", cron.trim()) }, habilitado = cron.trim() != quando && !ocupado)
                }
                TextoDica("= ${cronParaTexto(cron)}")
                Espaco(6)
                com.pwexpress.core.designsystem.PwBotaoOuro("Rodar agora", onClick = { acao(nome, "rodar") }, icone = PwIcones.Zap, habilitado = !ocupado)
                ult?.str("mensagem")?.let { TextoDica("Resposta da última vez: $it") }
                t.str("comando")?.let { TextoDica("O que ela roda: $it") }
            }
        }
    }
}

// ------------------------------------------------------------------ aparelhos

@Composable
private fun AparelhosDevG(ap: List<JsonObject>?) {
    val c = Pw.cores
    var aberto by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableStateOf("") }
    val l = ap ?: run { PwCarregando(); return }
    if (l.isEmpty()) { PwVazio("Nenhum aparelho registrado ainda.", icone = PwIcones.Smartphone); return }
    com.pwexpress.core.designsystem.PwCampo(busca, { busca = it }, "Buscar motoboy ou aparelho")
    l.filter { busca.isBlank() || "${it.str("nome")} ${it.str("email")} ${it.obj("aparelho")?.str("modelo")}".contains(busca.trim(), true) }.forEach { a ->
        val d = a.obj("aparelho")
        val id = a.str("id") ?: return@forEach
        val ab = aberto == id
        fun sim(k: String) = d?.get(k)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toBooleanStrictOrNull() }
        val alertas = listOfNotNull("sem localização em segundo plano".takeIf { sim("localizacao_fundo") == false }, "notificações bloqueadas".takeIf { sim("notificacoes") == false },
            "o Android pode pausar o app (economia de bateria)".takeIf { sim("otimizacao_bateria") == true }, "sem push".takeIf { !a.bool("push") }, "GPS desligado".takeIf { sim("gps_ligado") == false })
        PwCartao(cor = if (alertas.isNotEmpty()) c.vermelho.copy(alpha = 0.35f) else null) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { aberto = if (ab) null else id }, verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Smartphone, null, tint = c.ouro, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.str("nome") ?: "—", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextoDica(listOfNotNull(d?.str("fabricante")?.let { "$it ${d.str("modelo").orEmpty()}" }, d?.str("android")?.let { "Android $it" }, a.str("versao_app")?.let { "app $it" }).joinToString(" · ")
                        .ifEmpty { "O aparelho ainda não mandou os dados (abre o app atualizado)" })
                }
                com.pwexpress.core.designsystem.PwSelo(rotuloMotoboy[a.str("status")] ?: "Offline", if (a.str("status") == "offline") c.texto3 else c.verde)
                Icon(if (ab) PwIcones.ChevronUp else PwIcones.ChevronDown, null, tint = c.texto3, modifier = Modifier.size(18.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 6.dp)) {
                Detalhe(PwIcones.Timer, haQuanto(com.pwexpress.feature.gestor.data.iso(a.str("posicao_em"))))
                a.int("bateria")?.let { Detalhe(PwIcones.BatteryMedium, "$it%") }
                a.dbl("precisao_m")?.let { Detalhe(PwIcones.Crosshair, "± ${it.toInt()} m") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 4.dp)) {
                Detalhe(PwIcones.Footprints, "${a.long("pings_1h") ?: 0}/h · ${a.long("pings_24h") ?: 0} em 24 h")
                Detalhe(PwIcones.BellRing, if (a.bool("push")) "push" else "sem push", if (a.bool("push")) c.verde else c.vermelho)
            }
            if (alertas.isNotEmpty()) Text("Atenção: " + alertas.joinToString(" · "), color = c.vermelho, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
            if (ab) {
                Espaco(8)
                fun txt(k: String) = d?.get(k)?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content } ?: "—"
                fun sn(k: String) = when (sim(k)) { true -> "sim"; false -> "não"; null -> "—" }
                val linhas = listOf(
                    "E-mail" to (a.str("email") ?: "—"), "Cargos" to a.strs("papeis").joinToString(", ").ifEmpty { "—" },
                    "Corrida agora" to (a.obj("corrida")?.let { "#${it.long("numero")} (${it.str("status")})" } ?: "nenhuma"), "Raio escolhido" to (a.dbl("raio_km")?.let { "${numeroBr(it)} km" } ?: "—"),
                    "Android" to "${txt("android")} (API ${txt("sdk")})", "Build do app" to txt("build"),
                    "Localização precisa" to sn("localizacao_precisa"), "Em segundo plano" to sn("localizacao_fundo"), "GPS ligado" to sn("gps_ligado"), "Notificações" to sn("notificacoes"),
                    "Economia de bateria do Android" to when (sim("otimizacao_bateria")) { true -> "pode pausar o app"; false -> "liberado"; null -> "—" }, "Modo economia do celular" to sn("economia_android"),
                    "Carregando" to sn("carregando"), "Rede" to txt("rede"), "Tela" to txt("tela"), "Memória livre" to (txt("memoria_livre_mb").takeIf { it != "—" }?.let { "$it MB" } ?: "—"),
                    "Idioma / fuso" to "${txt("idioma")} · ${txt("fuso")}", "Tokens de push" to "${a.long("push_tokens") ?: 0}",
                    "Última posição" to (a.obj("ultima_posicao")?.let { u -> haQuanto(com.pwexpress.feature.gestor.data.iso(u.str("quando"))) + (u.dbl("velocidade")?.let { " · ${(it * 3.6).toInt()} km/h" } ?: "") } ?: "—"),
                    "Dados do aparelho" to haQuanto(com.pwexpress.feature.gestor.data.iso(a.str("aparelho_em"))),
                )
                linhas.forEach { (r, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                        Text(r, color = c.texto3, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                        Text(v, color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f))
                    }
                }
            }
        }
    }
}
