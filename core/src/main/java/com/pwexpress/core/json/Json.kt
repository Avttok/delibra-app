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
    throw ErroServidor(codigo, traduzir(codigo) ?: detalhe ?: codigo.replace('_', ' ').replaceFirstChar { it.uppercase() }, this.codigo)
}

fun JsonElement?.comoObjeto(): JsonObject? = runCatching { this?.jsonObject }.getOrNull()
fun JsonElement?.comoLista(): List<JsonObject> = runCatching { this?.jsonArray?.mapNotNull { it as? JsonObject } }.getOrNull().orEmpty()
