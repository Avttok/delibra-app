package com.pwexpress.core.jogo

import androidx.compose.ui.graphics.Color
import com.pwexpress.core.PwCore
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Item da loja do jogo (o preço vale do servidor). */
data class ItemJogo(val chave: String, val tipo: String, val nome: String, val preco: Int, val nivel: Int, val cor: Color?, val brilho: Boolean, val desenho: String?, val arco: Boolean) {
    val poder get() = tipo == "poder"
}

data class PerfilJogo(
    val moedas: Int = 0, val xp: Int = 0, val level: Int = 1, val proximoXp: Int = 100, val recorde: Int = 0, val partidas: Int = 0,
    val itens: List<String> = emptyList(), val equipado: Map<String, String> = emptyMap(), val loja: List<ItemJogo> = emptyList(),
    val ranking: Boolean = true,
) {
    fun item(chave: String?) = loja.firstOrNull { it.chave == chave }
    fun equipadoDe(tipo: String) = item(equipado[tipo])
    fun tem(chave: String) = chave in itens
    fun quantos(chave: String) = itens.count { it == chave }
    /** Começo do level atual (para a barra). */
    val xpDoLevel get() = (level - 1) * (level - 1) * 100
}

data class LinhaRanking(val id: String, val nome: String, val foto: String?, val pontos: Int, val eu: Boolean, val level: Int, val capacete: String?, val moto: String?)

/** Moedas, loja, partidas e ranking do jogo: tudo no servidor (as moedas não dá para inventar no celular). */
object JogoServidor {
    private val _perfil = MutableStateFlow<PerfilJogo?>(null)
    val perfil: StateFlow<PerfilJogo?> = _perfil.asStateFlow()

    private fun cor(s: String?): Color? = runCatching { s?.let { Color(android.graphics.Color.parseColor(it)) } }.getOrNull()

    private fun JsonObject.paraPerfil() = PerfilJogo(
        moedas = int("moedas") ?: 0, xp = int("xp") ?: 0, level = int("level") ?: 1, proximoXp = int("proximo_xp") ?: 100,
        recorde = int("recorde") ?: 0, partidas = int("partidas") ?: 0, itens = strs("itens"),
        equipado = obj("equipado")?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.content?.let { k to it } }?.toMap().orEmpty(),
        loja = arr("loja").map { i ->
            val d = i.obj("dados")
            ItemJogo(i.str("chave").orEmpty(), i.str("tipo").orEmpty(), i.str("nome").orEmpty(), i.int("preco") ?: 0, i.int("nivel") ?: 1,
                cor(d?.str("cor")), d?.bool("brilho") == true, d?.str("desenho"), d?.bool("arco") == true)
        },
        ranking = this["ranking"]?.let { (it as? JsonPrimitive)?.content != "false" } ?: true,
    )

    private suspend fun chamar(f: String, args: JsonObject = JsonObject(emptyMap())): Result<JsonObject> = runCatching {
        val r = PwCore.rpc.chamar(f, args)
        if (!r.ok) throw Exception(traduzir(r.corpo))
        (Json.parseToJsonElement(r.corpo) as JsonObject)
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet: moedas e loja voltam quando o sinal voltar.") else e }

    private fun traduzir(corpo: String) = when {
        "moedas_insuficientes" in corpo -> "Moedas insuficientes."
        "level_baixo" in corpo -> "Suba de level para liberar este item."
        "ja_tem" in corpo -> "Você já tem este item."
        "nao_tem" in corpo -> "Compre o item antes de usar."
        else -> "Não deu agora. Tente de novo."
    }

    suspend fun carregar(): Result<PerfilJogo> = chamar("jogo_meu").map { it.paraPerfil().also { p -> _perfil.value = p } }

    suspend fun comprar(item: String): Result<PerfilJogo> =
        chamar("jogo_comprar", buildJsonObject { put("p_item", item) }).map { it.paraPerfil().also { p -> _perfil.value = p } }

    suspend fun equipar(item: String): Result<PerfilJogo> =
        chamar("jogo_equipar", buildJsonObject { put("p_item", item) }).map { it.paraPerfil().also { p -> _perfil.value = p } }

    /** Fim da partida: manda pontos, moedas pegas e poderes gastos. */
    suspend fun registrar(pontos: Int, moedas: Int, distancia: Int, poderes: List<String>): Result<PerfilJogo> =
        chamar("jogo_registrar_partida", buildJsonObject {
            put("p_pontos", pontos); put("p_moedas", moedas); put("p_distancia", distancia)
            put("p_poderes", JsonArray(poderes.map { JsonPrimitive(it) }))
        }).map { it.paraPerfil().also { p -> _perfil.value = p } }

    suspend fun ranking(escopo: String, periodo: String): Result<List<LinhaRanking>> = runCatching {
        val r = PwCore.rpc.chamar("jogo_ranking", buildJsonObject { put("p_escopo", escopo); put("p_periodo", periodo) })
        if (!r.ok) throw Exception("Não deu para carregar o ranking.")
        (Json.parseToJsonElement(r.corpo) as? JsonArray)?.mapNotNull { it as? JsonObject }?.map { o ->
            LinhaRanking(o.str("id").orEmpty(), o.str("nome") ?: "Motoboy", o.str("foto"), o.int("pontos") ?: 0, o.bool("eu"), o.int("level") ?: 1,
                o.obj("equipado")?.str("capacete"), o.obj("equipado")?.str("moto"))
        }.orEmpty()
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }
}
