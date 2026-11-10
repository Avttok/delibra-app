package com.pwexpress.core.chat

import com.pwexpress.core.PwCore
import com.pwexpress.core.tentar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.time.Instant

/** O que dá para mandar no chat além de texto: foto, pedido, rota, cliente, pessoa ou um local. Igual ao site. */
data class Anexo(
    val tipo: String, val id: String? = null, val titulo: String? = null, val sub: String? = null,
    val caminho: String? = null, val lat: Double? = null, val lng: Double? = null,
) {
    fun json(): JsonObject = buildJsonObject {
        put("tipo", tipo); id?.let { put("id", it) }; titulo?.let { put("titulo", it) }; sub?.let { put("sub", it) }
        caminho?.let { put("caminho", it) }; lat?.let { put("lat", it) }; lng?.let { put("lng", it) }
    }
}

data class Conversa(
    val id: String, val tipo: String, val nome: String, val foto: String?, val outro: String?, val membros: Int,
    val ultimaEm: Long?, val previa: String?, val ultimaMinha: Boolean, val silenciada: Boolean, val naoLidas: Int,
)

data class Contato(val id: String, val nome: String, val foto: String?, val papeis: List<String>, val status: String?, val empresa: String?) {
    val papel: String get() = when {
        "dev" in papeis -> "Dev"; "gestor" in papeis -> "Gestor"; "motoboy" in papeis -> "Motoboy"
        empresa != null -> "Cliente · $empresa"; else -> "Cliente"
    }
}

data class Mensagem(val id: String, val autor: String?, val texto: String?, val anexo: Anexo?, val quando: Long, val apagada: Boolean)

/** Chat geral, conversas diretas e grupos (as mesmas do site). */
object Chat {
    private val json = Json { ignoreUnknownKeys = true }
    private val _conversas = MutableStateFlow<List<Conversa>>(emptyList())
    val conversas: StateFlow<List<Conversa>> = _conversas.asStateFlow()
    private val _naoLidas = MutableStateFlow(0)
    val naoLidas: StateFlow<Int> = _naoLidas.asStateFlow()
    private val _contatos = MutableStateFlow<List<Contato>>(emptyList())
    val contatos: StateFlow<List<Contato>> = _contatos.asStateFlow()

    /** Algo para compartilhar (botão "Mandar no chat" de uma rota, pedido, cliente…): a tela do chat pergunta para quem. */
    val anexoPendente = MutableStateFlow<Anexo?>(null)
    /** Pedido para abrir o chat (do topo, de uma notificação ou de um "compartilhar"). */
    val abrir = MutableStateFlow(false)
    /** Conversa a abrir direto (ex.: toque na notificação). */
    val conversaPedida = MutableStateFlow<String?>(null)

    fun compartilhar(a: Anexo) { anexoPendente.value = a; abrir.value = true }

    private fun JsonObject.t(k: String) = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
    private fun instante(s: String?) = s?.let { tentar { Instant.parse(it).toEpochMilli() }.getOrNull() ?: tentar { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }

    private suspend fun rpc(f: String, args: JsonObject = JsonObject(emptyMap())): JsonElement {
        val r = PwCore.rpc.chamar(f, args)
        if (!r.ok) error(traduzir(r.corpo))
        return if (r.corpo.isBlank()) JsonNull else json.parseToJsonElement(r.corpo)
    }

    fun traduzir(corpo: String): String = when {
        "chat_desligado" in corpo -> "A conversa está desligada."
        "fotos_desligadas" in corpo -> "O envio de fotos está desligado."
        "mensagem_vazia" in corpo -> "Escreva algo ou anexe uma foto."
        "conversa_invalida" in corpo || "sem_permissao" in corpo -> "Você não está nessa conversa."
        "anexo_invalido" in corpo -> "Esse anexo não pôde ser enviado."
        else -> "Não deu para falar com o servidor agora."
    }

    suspend fun carregar(): Result<List<Conversa>> = tentar {
        val l = (rpc("chat_conversas") as? JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            Conversa(
                o.t("id") ?: return@mapNotNull null, o.t("tipo") ?: "direta", o.t("nome") ?: "Conversa", o.t("foto"), o.t("outro"),
                (o["membros"] as? JsonPrimitive)?.intOrNull ?: 0, instante(o.t("ultima_em")), o.t("ultima_previa"),
                (o["ultima_minha"] as? JsonPrimitive)?.booleanOrNull == true, (o["silenciada"] as? JsonPrimitive)?.booleanOrNull == true,
                (o["nao_lidas"] as? JsonPrimitive)?.intOrNull ?: 0,
            )
        }
        _conversas.value = l
        _naoLidas.value = l.filter { !it.silenciada }.sumOf { it.naoLidas }
        l
    }

    suspend fun carregarContatos(): Result<List<Contato>> = tentar {
        val l = (rpc("chat_contatos") as? JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            Contato(o.t("id") ?: return@mapNotNull null, o.t("nome") ?: "Sem nome", o.t("foto"),
                (o["papeis"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }, o.t("status"), o.t("empresa"))
        }
        _contatos.value = l
        l
    }

    /** Quem escreveu na conversa, com os cargos (selos ao lado do nome). */
    suspend fun autores(conversa: String): List<Contato> = tentar {
        (rpc("chat_autores", buildJsonObject { put("p_conversa", conversa) }) as? JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            Contato(o.t("id") ?: return@mapNotNull null, o.t("nome") ?: "Sem nome", o.t("foto"),
                (o["papeis"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }, null, o.t("empresa"))
        }
    }.getOrDefault(emptyList())

    suspend fun mensagens(conversa: String, limite: Int = 120): Result<List<Mensagem>> = tentar {
        val r = PwCore.rpc.rest("GET", "/rest/v1/chat_mensagens?select=id,autor_id,texto,anexo,criada_em,apagada_em&conversa_id=eq.$conversa&order=criada_em.desc&limit=$limite")
        if (!r.ok) error(traduzir(r.corpo))
        (json.parseToJsonElement(r.corpo) as? JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            val a = o["anexo"] as? JsonObject
            Mensagem(
                o.t("id") ?: return@mapNotNull null, o.t("autor_id"), o.t("texto"),
                a?.let { x -> Anexo(x.t("tipo") ?: "local", x.t("id"), x.t("titulo"), x.t("sub"), x.t("caminho"), (x["lat"] as? JsonPrimitive)?.doubleOrNull, (x["lng"] as? JsonPrimitive)?.doubleOrNull) },
                instante(o.t("criada_em")) ?: 0L, o.t("apagada_em") != null,
            )
        }.reversed()
    }

    suspend fun enviar(conversa: String, texto: String?, anexo: Anexo? = null): Result<Unit> = tentar {
        rpc("chat_enviar", buildJsonObject {
            put("p_conversa", conversa)
            if (!texto.isNullOrBlank()) put("p_texto", texto.trim())
            if (anexo != null) put("p_anexo", anexo.json())
        })
        Unit
    }

    /** Foto: vai para o Storage (pasta da conversa) e a mensagem leva o caminho. */
    suspend fun enviarFoto(conversa: String, jpeg: ByteArray, texto: String? = null): Result<Unit> = tentar {
        val caminho = "$conversa/${java.util.UUID.randomUUID()}.jpg"
        val r = PwCore.rpc.enviarArquivo("chat", caminho, jpeg, "image/jpeg")
        if (!r.ok) error("Não deu para enviar a foto.")
        enviar(conversa, texto, Anexo("foto", caminho = caminho)).getOrThrow()
    }

    private val links = java.util.concurrent.ConcurrentHashMap<String, Pair<String, Long>>()
    suspend fun linkFoto(caminho: String): String? {
        links[caminho]?.takeIf { it.second > System.currentTimeMillis() }?.let { return it.first }
        return PwCore.rpc.linkAssinado("chat", caminho, 3600)?.also { links[caminho] = it to System.currentTimeMillis() + 3_000_000 }
    }

    suspend fun abrirDireta(usuario: String): Result<String> = tentar {
        (rpc("chat_abrir_direta", buildJsonObject { put("p_usuario", usuario) }) as JsonPrimitive).content
    }

    suspend fun criarGrupo(nome: String, membros: List<String>): Result<String> = tentar {
        (rpc("chat_criar_grupo", buildJsonObject { put("p_nome", nome); put("p_membros", buildJsonArray { membros.forEach { add(JsonPrimitive(it)) } }) }) as JsonPrimitive).content
    }

    suspend fun marcarLida(conversa: String) { tentar { rpc("chat_marcar_lida", buildJsonObject { put("p_conversa", conversa) }) }; carregar() }
    suspend fun silenciar(conversa: String, sim: Boolean) = tentar { rpc("chat_silenciar", buildJsonObject { put("p_conversa", conversa); put("p_silenciar", sim) }); carregar() }
    suspend fun apagar(mensagem: String) = tentar { rpc("chat_apagar_mensagem", buildJsonObject { put("p_mensagem", mensagem) }) }

    fun limpar() { _conversas.value = emptyList(); _naoLidas.value = 0; _contatos.value = emptyList(); anexoPendente.value = null }
}
