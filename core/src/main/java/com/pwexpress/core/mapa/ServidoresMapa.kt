package com.pwexpress.core.mapa

import com.pwexpress.core.PwCore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Servidores de mapa (rotas, percursos e endereços). O padrão é o público gratuito; o Dev aponta para servidores
 * próprios ou pagos em Ajustes → Plataforma. Lido uma vez ao abrir o app (sem resposta, fica o padrão).
 */
object ServidoresMapa {
    @Volatile var osrm = "https://router.project-osrm.org"; private set
    @Volatile var valhalla = "https://valhalla1.openstreetmap.de"; private set
    @Volatile var nominatim = "https://nominatim.openstreetmap.org"; private set

    suspend fun carregar() {
        runCatching {
            val r = PwCore.http.post("/rest/v1/rpc/servidores_mapa", "{}", null)
            if (!r.ok) return
            val o = Json.parseToJsonElement(r.corpo).jsonObject
            fun url(k: String) = (o[k] as? JsonPrimitive)?.content?.trim()?.trimEnd('/')?.takeIf { it.startsWith("https://") }
            url("osrm")?.let { osrm = it }; url("valhalla")?.let { valhalla = it }; url("nominatim")?.let { nominatim = it }
        }
    }

    @Suppress("unused") private fun usar(o: JsonObject) = o
}
