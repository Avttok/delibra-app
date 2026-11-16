package com.pwexpress.core.perfil

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * O que a empresa ajustou na forma do trabalho (Ajustes → Modalidades), vindo do perfil:
 * nomes próprios de status e paradas, e os campos próprios (o motoboy responde os da parada).
 */
object Modalidades {
    data class Campo(
        val chave: String, val rotulo: String, val dica: String?, val tipo: String, val opcoes: List<String>, val obrigatorio: Boolean,
        val quemPreenche: String, val entidade: String, val pontoTipo: String?, val tiposPedido: List<String>, val visivelMotoboy: Boolean,
    )

    @Volatile var rotulos: Map<String, String> = emptyMap(); private set
    @Volatile var campos: List<Campo> = emptyList(); private set

    internal fun atualizar(o: JsonObject) {
        rotulos = (o["rotulos"] as? JsonObject)?.mapNotNull { (k, v) -> (v as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.let { k to it } }?.toMap() ?: emptyMap()
        campos = (o["campos"] as? JsonArray).orEmpty().mapNotNull { e ->
            val c = e as? JsonObject ?: return@mapNotNull null
            fun t(k: String) = (c[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            fun l(k: String) = (c[k] as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.content }
            Campo(t("chave") ?: return@mapNotNull null, t("rotulo") ?: "", t("dica"), t("tipo") ?: "texto", l("opcoes"),
                (c["obrigatorio"] as? JsonPrimitive)?.booleanOrNull == true, t("quem_preenche") ?: "qualquer", t("entidade") ?: "",
                t("ponto_tipo"), l("tipos_pedido"), (c["visivel_motoboy"] as? JsonPrimitive)?.booleanOrNull != false)
        }
    }

    /** "Coleta"/"Entrega" ou o nome que a empresa deu (por tipo de pedido, se houver). */
    fun nomeParada(coleta: Boolean, tipoPedido: String? = null): String {
        val alvo = if (coleta) "parada:coleta" else "parada:entrega"
        return tipoPedido?.let { rotulos["$it|$alvo"] } ?: rotulos["|$alvo"] ?: if (coleta) "Coleta" else "Entrega"
    }

    /** Nome de um status ("pedido" ou "rota") com o nome próprio da empresa, se houver. */
    fun nomeStatus(grupo: String, status: String, padrao: String, tipoPedido: String? = null): String =
        tipoPedido?.let { rotulos["$it|$grupo:$status"] } ?: rotulos["|$grupo:$status"] ?: padrao

    /** Campos que o motoboy responde nesta parada. */
    fun camposDaParada(coleta: Boolean, tipoPedido: String?): List<Campo> = campos.filter { c ->
        c.entidade == "ponto" && c.visivelMotoboy && c.quemPreenche in setOf("motoboy", "qualquer")
            && (c.pontoTipo == null || c.pontoTipo == if (coleta) "coleta" else "entrega")
            && (c.tiposPedido.isEmpty() || tipoPedido == null || tipoPedido in c.tiposPedido)
    }

    @Suppress("unused") private fun usar(e: JsonElement) = e
}
