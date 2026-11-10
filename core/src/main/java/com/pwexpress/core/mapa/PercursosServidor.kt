package com.pwexpress.core.mapa

import com.pwexpress.core.tentar

import com.pwexpress.core.PwCore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.time.Instant

/**
 * Percursos vindos do site (/api/percursos): todos os pings do período (sem o corte de 1000 linhas)
 * e o caminho de cada registro já casado com as ruas. O mesmo que o painel do gestor usa.
 */
object PercursosServidor {
    data class Ajuste(val ate: Long, val caminho: List<Pair<Double, Double>>)
    data class Ping(val motoboy: String, val ping: PingMapa)
    data class Resultado(val pings: List<Ping>, val ajustes: Map<String, Ajuste>, val linha: List<Pair<Double, Double>>, val parcial: Boolean) {
        fun pingsDe(motoboy: String) = pings.filter { it.motoboy == motoboy }.map { it.ping }
        /** Registros de um motoboy com o caminho pelas ruas. */
        fun registros(motoboy: String) = aplicarAjustes(separarRegistros(pingsDe(motoboy)), motoboy, ajustes)
    }

    /** null = o site não respondeu (a tela pode cair para os pings direto do banco). */
    suspend fun buscar(de: Instant? = null, ate: Instant? = null, motoboy: String? = null, teste: Boolean? = null, corrida: String? = null): Resultado? = tentar {
        val corpo = buildJsonObject {
            de?.let { put("de", it.toString()) }; ate?.let { put("ate", it.toString()) }
            motoboy?.let { put("motoboy", it) }; teste?.let { put("teste", it) }; corrida?.let { put("corrida", it) }
        }
        val r = PwCore.rpc.site("/api/percursos", corpo)
        if (!r.ok) return@tentar null
        val j = Json.parseToJsonElement(r.corpo).jsonObject
        fun par(e: kotlinx.serialization.json.JsonElement): Pair<Double, Double>? {
            val a = e as? JsonArray ?: return null
            val la = (a.getOrNull(0) as? JsonPrimitive)?.doubleOrNull; val lo = (a.getOrNull(1) as? JsonPrimitive)?.doubleOrNull
            return if (la == null || lo == null) null else la to lo
        }
        val pings = (j["pings"] as? JsonArray).orEmpty().mapNotNull { e ->
            val a = e as? JsonArray ?: return@mapNotNull null
            fun t(i: Int) = (a.getOrNull(i) as? JsonPrimitive)?.takeIf { it.isString }?.content
            val la = (a.getOrNull(1) as? JsonPrimitive)?.doubleOrNull; val lo = (a.getOrNull(2) as? JsonPrimitive)?.doubleOrNull
            val ms = (a.getOrNull(5) as? JsonPrimitive)?.longOrNull
            val m = t(0)
            if (m == null || la == null || lo == null || ms == null) null else Ping(m, PingMapa(la, lo, t(3) ?: "", t(4), ms))
        }
        val ajustes = (j["ajustes"] as? JsonObject).orEmpty().mapNotNull { (k, v) ->
            val o = v as? JsonObject ?: return@mapNotNull null
            val ate = (o["ate"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            k to Ajuste(ate, (o["caminho"] as? JsonArray).orEmpty().mapNotNull(::par))
        }.toMap()
        Resultado(pings, ajustes, (j["linha"] as? JsonArray).orEmpty().mapNotNull(::par), (j["parcial"] as? JsonPrimitive)?.booleanOrNull == true)
    }.getOrNull()
}

/** Põe o caminho pelas ruas em cada registro; os pings que chegaram depois entram no fim, crus. */
fun aplicarAjustes(regs: List<RegistroPercurso>, motoboy: String, ajustes: Map<String, PercursosServidor.Ajuste>): List<RegistroPercurso> =
    if (ajustes.isEmpty()) regs else regs.map { r ->
        val a = ajustes["$motoboy|${r.inicio}"] ?: return@map r
        r.copy(caminho = a.caminho + r.pontos.filter { it.quando > a.ate }.map { it.lat to it.lng })
    }

/**
 * O feito de uma corrida (lat, lng), pelas ruas. Em andamento, busca de novo a cada minuto.
 * Vazio enquanto não há pings (ou o site não respondeu).
 */
@androidx.compose.runtime.Composable
fun rememberTrilhaCorrida(corridaId: String?, ativa: Boolean): List<Pair<Double, Double>> {
    val estado = androidx.compose.runtime.produceState(emptyList<Pair<Double, Double>>(), corridaId, ativa) {
        if (corridaId == null) { value = emptyList(); return@produceState }
        while (true) {
            PercursosServidor.buscar(corrida = corridaId)?.let { value = it.linha }
            if (!ativa) break
            kotlinx.coroutines.delay(60_000)
        }
    }
    return estado.value
}
