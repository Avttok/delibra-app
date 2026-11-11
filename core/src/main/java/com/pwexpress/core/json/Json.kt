package com.pwexpress.core.json

import com.pwexpress.core.network.ErroServidor
import com.pwexpress.core.network.HttpResposta
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/** Leitura tolerante de JSON (campos ausentes ou nulos viram null/padrão). */
fun JsonObject.str(k: String): String? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
fun JsonObject.int(k: String): Int? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }
fun JsonObject.long(k: String): Long? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.let { it.longOrNull ?: it.doubleOrNull?.toLong() }
fun JsonObject.dbl(k: String): Double? = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }
fun JsonObject.bool(k: String): Boolean = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.booleanOrNull ?: false
fun JsonObject.obj(k: String): JsonObject? = this[k] as? JsonObject
fun JsonObject.arr(k: String): List<JsonObject> = (this[k] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
fun JsonObject.strs(k: String): List<String> = (this[k] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()

private val json = Json { ignoreUnknownKeys = true }

fun HttpResposta.elemento(): JsonElement? = runCatching { json.parseToJsonElement(corpo) }.getOrNull()
fun HttpResposta.objeto(): JsonObject? = elemento() as? JsonObject
fun HttpResposta.lista(): List<JsonObject> = (elemento() as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

/**
 * Lança ErroServidor com a mensagem do banco ("horario_muito_proximo"…), do PostgREST ou do site.
 * `traduzir` converte o código em texto para o usuário.
 */
fun HttpResposta.exigirOk(traduzir: (String) -> String? = { null }): HttpResposta {
    if (ok) return this
    val o = runCatching { json.parseToJsonElement(corpo).jsonObject }.getOrNull()
    val codigo = o?.str("message") ?: o?.str("erro") ?: o?.str("error") ?: o?.str("msg") ?: "erro_$codigo"
    val detalhe = o?.str("detalhe")
    throw ErroServidor(codigo, traduzir(codigo) ?: detalhe?.let { erroAmigavel(it, this.codigo) } ?: erroAmigavel(codigo, this.codigo), this.codigo)
}

/**
 * Mensagem que a pessoa entende: os códigos do banco (ex.: "apenas_gestor") viram frase,
 * e erros técnicos em inglês (do banco ou do servidor) viram um aviso claro, sem jargão.
 */
fun erroAmigavel(msg: String, http: Int = 0): String {
    val m = msg.lowercase()
    return when {
        "row-level security" in m || "permission denied" in m || http == 403 || "sem_permissao" in m -> "Sem permissão para isso."
        "duplicate key" in m || "already exists" in m -> "Isso já existe."
        "timeout" in m || "timed out" in m || http == 504 || http == 502 -> "O servidor demorou para responder. Tente de novo."
        http == 401 || "jwt" in m -> "Sua sessão expirou. Entre de novo."
        Regex("[a-z]+ [a-z]+ [a-z]+").containsMatchIn(m) && Regex("\\b(cannot|violates|does not exist|column|function|syntax|null value|invalid input|relation|operator|scalar|out of range)\\b").containsMatchIn(m) ->
            "Algo deu errado do nosso lado. Já pode tentar de novo; se repetir, avise o suporte."
        Regex("^[a-z0-9_]+$").matches(m) -> msg.replace('_', ' ').replaceFirstChar { it.uppercase() } + "."
        else -> msg
    }
}

fun JsonElement?.comoObjeto(): JsonObject? = runCatching { this?.jsonObject }.getOrNull()
fun JsonElement?.comoLista(): List<JsonObject> = runCatching { this?.jsonArray?.mapNotNull { it as? JsonObject } }.getOrNull().orEmpty()
