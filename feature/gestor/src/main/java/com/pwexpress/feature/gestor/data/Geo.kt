package com.pwexpress.feature.gestor.data

import com.pwexpress.core.tentar

import com.pwexpress.core.PwCore
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLEncoder

data class EnderecoG(val descricao: String, val lat: Double, val lng: Double)
data class RotaG(val distanciaM: Int, val duracaoS: Int, val polyline: String)

/**
 * Endereços, rotas e pontos próximos (OpenStreetMap: Nominatim, OSRM e Overpass), os mesmos do site.
 * Para trocar pelo Google, basta reescrever este arquivo.
 */
object Geo {
    private const val UA = "Delibra-app (delibra-web.vercel.app)"

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        tentar {
            PwCore.http.client.newCall(Request.Builder().url(url).header("User-Agent", UA).header("Accept-Language", "pt-BR").build())
                .execute().use { if (it.isSuccessful) it.body?.string() else null }
        }.getOrNull()
    }

    private fun encurtar(nome: String) = nome.split(", ").filter { it != "Brasil" && it != "Região Sul" }.take(5).joinToString(", ")

    suspend fun buscarEnderecos(texto: String): List<EnderecoG> {
        if (texto.trim().length < 4) return emptyList()
        val corpo = get("https://nominatim.openstreetmap.org/search?format=jsonv2&countrycodes=br&limit=6&accept-language=pt-BR&q=${URLEncoder.encode(texto, "UTF-8")}") ?: return emptyList()
        return tentar {
            (Json.parseToJsonElement(corpo) as JsonArray).mapNotNull { e ->
                val o = e as? JsonObject ?: return@mapNotNull null
                EnderecoG(encurtar(o.str("display_name").orEmpty()), o.dbl("lat") ?: return@mapNotNull null, o.dbl("lon") ?: return@mapNotNull null)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun enderecoDoPonto(lat: Double, lng: Double): String {
        val corpo = get("https://nominatim.openstreetmap.org/reverse?format=jsonv2&accept-language=pt-BR&lat=$lat&lon=$lng")
        return corpo?.let { tentar { encurtar(Json.parseToJsonElement(it).jsonObject.str("display_name").orEmpty()) }.getOrNull() }
            ?.ifBlank { null } ?: "%.5f, %.5f".format(lat, lng)
    }

    suspend fun calcularRota(pontos: List<Pair<Double, Double>>): RotaG? {
        if (pontos.size < 2) return null
        val coords = pontos.joinToString(";") { "${it.second},${it.first}" }
        val corpo = get("https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=polyline") ?: return null
        return tentar {
            val o = Json.parseToJsonElement(corpo).jsonObject
            val r = (o["routes"] as JsonArray).first().jsonObject
            RotaG(r.dbl("distance")!!.toInt(), r.dbl("duration")!!.toInt(), r.str("geometry")!!)
        }.getOrNull()
    }

    private val tipos = mapOf(
        "fuel" to "Posto", "pharmacy" to "Farmácia", "supermarket" to "Mercado", "bank" to "Banco", "restaurant" to "Restaurante",
        "school" to "Escola", "hospital" to "Hospital", "place_of_worship" to "Igreja", "bakery" to "Padaria", "cafe" to "Café",
        "fast_food" to "Lanchonete", "convenience" to "Conveniência", "park" to "Praça/parque", "police" to "Polícia", "post_office" to "Correios",
        "clothes" to "Loja", "hairdresser" to "Salão", "beauty" to "Salão", "dentist" to "Dentista", "doctors" to "Clínica", "clinic" to "Clínica",
        "bar" to "Bar", "pub" to "Bar", "ice_cream" to "Sorveteria", "hotel" to "Hotel", "hardware" to "Ferragem", "car_repair" to "Oficina",
        "butcher" to "Açougue", "optician" to "Ótica", "mall" to "Shopping", "townhall" to "Prefeitura", "gym" to "Academia", "fitness_centre" to "Academia",
        "veterinary" to "Veterinário", "pet" to "Pet shop", "atm" to "Caixa eletrônico", "kindergarten" to "Escola", "university" to "Faculdade",
    )

    /** Overpass tem vários espelhos; o principal vive lotado (429/504). */
    private val overpass = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )
    private val fora = setOf("bus", "bus_stop", "bus_station", "platform", "stop_position", "tram_stop", "subway", "subway_entrance", "taxi", "parking", "parking_entrance", "bicycle_parking", "motorcycle_parking", "bicycle_rental")

    /** Estabelecimentos perto do ponto (amplia o raio se não achar nada). */
    suspend fun pontosProximos(lat: Double, lng: Double, raioM: Int = 250): List<RefG> =
        buscarProximos(lat, lng, raioM).ifEmpty { if (raioM < 600) buscarProximos(lat, lng, 600) else emptyList() }

    private suspend fun buscarProximos(lat: Double, lng: Double, raioM: Int): List<RefG> = withContext(Dispatchers.IO) {
        val q = "[out:json][timeout:10];(nwr(around:$raioM,$lat,$lng)[name][~\"^(amenity|shop|leisure|tourism|office|healthcare|craft)$\"~\".\"];);out center 60;way(around:${minOf(raioM, 180)},$lat,$lng)[highway][name];out geom 40;"
        for (url in overpass) {
            val corpo = tentar {
                val req = Request.Builder().url(url).header("User-Agent", UA).post(FormBody.Builder().add("data", q).build()).build()
                PwCore.http.client.newCall(req).execute().use { r -> if (r.isSuccessful) r.body?.string() else null }
            }.getOrNull() ?: continue
            val lista = tentar { montarReferencias(corpo, lat, lng) }.getOrNull() ?: continue
            return@withContext lista
        }
        emptyList()
    }

    private fun montarReferencias(corpo: String, lat: Double, lng: Double): List<RefG> {
            val vistos = mutableSetOf<String>()
            val elementos = (Json.parseToJsonElement(corpo).jsonObject["elements"] as JsonArray).mapNotNull { it as? JsonObject }
            val xy = Plano(lat, lng)
            val vias = elementos.filter { it.str("type") == "way" && it.obj("tags")?.str("highway") != null }.mapNotNull { w ->
                val pts = (w["geometry"] as? JsonArray)?.mapNotNull { g -> (g as? JsonObject)?.let { gg -> val a = gg.dbl("lat"); val b = gg.dbl("lon"); if (a != null && b != null) xy(a, b) else null } }
                if (pts.isNullOrEmpty()) null else Via(w.obj("tags")!!.str("name").orEmpty(), pts)
            }
            val destino = xy(lat, lng)
            return elementos.filterNot { it.str("type") == "way" && it.obj("tags")?.str("highway") != null }.mapNotNull { o ->
                val tags = o.obj("tags") ?: return@mapNotNull null
                val nome = tags.str("name") ?: return@mapNotNull null
                val c = o.obj("center")
                val plat = o.dbl("lat") ?: c?.dbl("lat") ?: return@mapNotNull null
                val plng = o.dbl("lon") ?: c?.dbl("lon") ?: return@mapNotNull null
                val t = tags.str("amenity") ?: tags.str("shop") ?: tags.str("leisure") ?: tags.str("tourism") ?: tags.str("office") ?: tags.str("healthcare") ?: tags.str("craft")
                if (t in fora || !vistos.add(nome.lowercase())) return@mapNotNull null
                val rel = tentar { relacaoNaRua(destino, xy(plat, plng), vias) }.getOrNull()
                val frase = when (rel) { null -> null; "frente" -> "Fica do outro lado da rua de $nome"; else -> "Fica à $rel de $nome (olhando da rua)" }
                RefG(nome, metros(lat, lng, plat, plng), tipos[t], plat, plng, frase)
            }.sortedBy { it.distanciaM ?: 0 }.take(8)
    }

    fun metros(aLat: Double, aLng: Double, bLat: Double, bLng: Double): Int {
        val r = 6371000.0
        val dLat = Math.toRadians(bLat - aLat); val dLng = Math.toRadians(bLng - aLng)
        val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(aLat)) * Math.cos(Math.toRadians(bLat)) * Math.sin(dLng / 2).let { it * it }
        return (2 * r * Math.asin(Math.sqrt(h))).toInt()
    }

    // ------------------------------------------------------------- referências inteligentes (igual ao site)

    private data class Xy(val x: Double, val y: Double)
    private data class Via(val nome: String, val pontos: List<Xy>)
    private class Plano(lat0: Double, private val lng0: Double) {
        private val lat0 = lat0
        private val kx = Math.cos(Math.toRadians(lat0)) * 111_320.0
        operator fun invoke(lat: Double, lng: Double) = Xy((lng - lng0) * kx, (lat - lat0) * 110_540.0)
    }
    private data class Proj(val dist: Double, val aoLongo: Double, val dir: Xy, val pe: Xy)

    private fun projetar(p: Xy, via: Via): Proj? {
        var melhor: Proj? = null
        var acumulado = 0.0
        for (i in 0 until via.pontos.size - 1) {
            val a = via.pontos[i]; val b = via.pontos[i + 1]
            val dx = b.x - a.x; val dy = b.y - a.y; val l2 = dx * dx + dy * dy; val l = Math.sqrt(l2)
            if (l < 0.5) continue
            val t = (((p.x - a.x) * dx + (p.y - a.y) * dy) / l2).coerceIn(0.0, 1.0)
            val pe = Xy(a.x + t * dx, a.y + t * dy)
            val d = Math.hypot(p.x - pe.x, p.y - pe.y)
            if (melhor == null || d < melhor.dist) melhor = Proj(d, acumulado + t * l, Xy(dx / l, dy / l), pe)
            acumulado += l
        }
        return melhor
    }

    /** "frente" (outro lado da rua), "esquerda"/"direita" (olhando da rua para o destino) ou null sem confiança. */
    private fun relacaoNaRua(destino: Xy, ref: Xy, vias: List<Via>): String? {
        val rua = vias.mapNotNull { v -> projetar(destino, v)?.let { v to it } }.minByOrNull { it.second.dist } ?: return null
        if (rua.second.dist !in 4.0..45.0) return null
        val daRef = vias.mapNotNull { v -> projetar(ref, v)?.let { v to it } }.minByOrNull { it.second.dist } ?: return null
        if (daRef.first.nome != rua.first.nome || daRef.second.dist !in 4.0..45.0) return null
        val pRef = projetar(ref, rua.first) ?: return null
        fun lado(p: Xy, pr: Proj) = Math.signum(pr.dir.x * (p.y - pr.pe.y) - pr.dir.y * (p.x - pr.pe.x))
        val sep = Math.abs(pRef.aoLongo - rua.second.aoLongo)
        if (sep > 120) return null
        if (lado(destino, rua.second) != lado(ref, pRef)) return if (sep <= 30) "frente" else null
        if (sep < 10) return null
        val fx = destino.x - rua.second.pe.x; val fy = destino.y - rua.second.pe.y
        val vx = pRef.pe.x - rua.second.pe.x; val vy = pRef.pe.y - rua.second.pe.y
        return if (vx * fy + vy * -fx > 0) "esquerda" else "direita"
    }
}
