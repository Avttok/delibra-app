package com.pwexpress.core.mapa

import com.pwexpress.core.tentar

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.HttpURLConnection
import java.net.URL

/** Qual mapa calcula as rotas e para qual veículo (Ajustes → Navegação). Igual ao site. */
object PreferenciaRota {
    @Volatile var provedor: String = "valhalla"
    @Volatile var perfil: String = "motorcycle"
    fun definir(provedor: String?, perfil: String?) {
        this.provedor = if (provedor == "osrm") "osrm" else "valhalla"
        if (!perfil.isNullOrBlank()) this.perfil = perfil
    }
}

/** Navegação curva a curva (Ajustes → Navegação; pode ter exceção por motoboy). */
object PreferenciasNavegacao {
    @Volatile var ligada = true
    @Volatile var voz = true
    @Volatile var recalcularM = 60.0
    @Volatile var naNotificacao = true
    fun definir(ligada: Boolean, voz: Boolean, recalcularM: Double, naNotificacao: Boolean) {
        this.ligada = ligada; this.voz = voz; this.recalcularM = recalcularM.coerceIn(20.0, 500.0); this.naNotificacao = naNotificacao
    }
}

/** Tipo da manobra (para o ícone do banner). */
enum class TipoManobra { SEGUIR, DIREITA, ESQUERDA, LEVE_DIREITA, LEVE_ESQUERDA, FORTE_DIREITA, FORTE_ESQUERDA, RETORNO, ROTATORIA, CHEGADA, SAIDA }

data class Manobra(
    val tipo: TipoManobra,
    /** Texto do banner: "Vire à direita na Rua XV de Novembro". */
    val instrucao: String,
    /** Para falar antes ("Em 200 metros, vire à direita…"), quando o provedor manda. */
    val falaAntes: String?,
    val rua: String?,
    /** Índice no [RotaNavegacao.pontos] onde a manobra acontece. */
    val indice: Int,
    val distanciaM: Double,
)

data class RotaNavegacao(val pontos: List<Pair<Double, Double>>, val manobras: List<Manobra>, val distanciaM: Double, val duracaoS: Double, val provedor: String) {
    /** Distância acumulada até cada ponto (para "faltam X m"). */
    val acumulado: DoubleArray by lazy {
        val a = DoubleArray(pontos.size)
        for (i in 1 until pontos.size) a[i] = a[i - 1] + metros(pontos[i - 1], pontos[i])
        a
    }
}

fun metros(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
    val r = 6371000.0
    val dLat = Math.toRadians(b.first - a.first); val dLng = Math.toRadians(b.second - a.second)
    val h = Math.sin(dLat / 2).let { it * it } + Math.cos(Math.toRadians(a.first)) * Math.cos(Math.toRadians(b.first)) * Math.sin(dLng / 2).let { it * it }
    return 2 * r * Math.asin(Math.sqrt(h.coerceIn(0.0, 1.0)))
}

/** Onde estou na rota: ponto mais perto (projeção no segmento), distância até ela e quanto já andei. */
data class PosicaoNaRota(val indice: Int, val foraM: Double, val percorridoM: Double)

fun RotaNavegacao.localizar(eu: Pair<Double, Double>, aPartirDe: Int = 0): PosicaoNaRota {
    var melhor = PosicaoNaRota(0, Double.MAX_VALUE, 0.0)
    val ini = (aPartirDe - 5).coerceAtLeast(0)
    val cosLat = Math.cos(Math.toRadians(eu.first))
    for (i in ini until pontos.size - 1) {
        val a = pontos[i]; val b = pontos[i + 1]
        // projeção plana local (suficiente para dezenas de metros)
        val ax = (a.second - eu.second) * cosLat; val ay = a.first - eu.first
        val bx = (b.second - eu.second) * cosLat; val by = b.first - eu.first
        val dx = bx - ax; val dy = by - ay
        val l2 = dx * dx + dy * dy
        val t = if (l2 == 0.0) 0.0 else (-(ax * dx + ay * dy) / l2).coerceIn(0.0, 1.0)
        val px = ax + t * dx; val py = ay + t * dy
        val d = Math.sqrt(px * px + py * py) * 111_320.0
        if (d < melhor.foraM) melhor = PosicaoNaRota(i, d, acumulado[i] + t * (acumulado[i + 1] - acumulado[i]))
        if (melhor.foraM < 8 && i > melhor.indice + 60) break // já achou e passou longe
    }
    return melhor
}

object Navegacao {
    private val json = Json { ignoreUnknownKeys = true }

    /** Rota de [de] até [ate] passando por [via] (opcional), com as manobras em português. */
    suspend fun rota(de: Pair<Double, Double>, ate: Pair<Double, Double>, via: List<Pair<Double, Double>> = emptyList(), rumo: Float? = null): RotaNavegacao? = withContext(Dispatchers.IO) {
        val pts = listOf(de) + via + ate
        val primeiro = if (PreferenciaRota.provedor == "osrm") null else tentar { valhalla(pts, rumo) }.getOrNull()
        primeiro ?: tentar { osrm(pts) }.getOrNull() ?: if (PreferenciaRota.provedor == "osrm") tentar { valhalla(pts, rumo) }.getOrNull() else null
    }

    private fun http(url: String, corpo: String? = null): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 8_000; c.readTimeout = 12_000
        c.setRequestProperty("User-Agent", "Delibra-app (delibra-web.vercel.app)")
        if (corpo != null) {
            c.requestMethod = "POST"; c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(corpo.toByteArray()) }
        }
        if (c.responseCode !in 200..299) error("http ${c.responseCode}")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun valhalla(pts: List<Pair<Double, Double>>, rumo: Float?): RotaNavegacao {
        val corpo = buildJsonObject {
            put("locations", buildJsonArray {
                pts.forEachIndexed { i, (la, lo) ->
                    add(buildJsonObject {
                        put("lat", la); put("lon", lo); put("type", if (i == 0 || i == pts.lastIndex) "break" else "through")
                        if (i == 0 && rumo != null) { put("heading", rumo.toDouble()); put("heading_tolerance", 60) }
                    })
                }
            })
            put("costing", PreferenciaRota.perfil)
            putJsonObject("directions_options") { put("language", "pt-BR"); put("units", "kilometers") }
        }.toString()
        val r = json.parseToJsonElement(http("https://valhalla1.openstreetmap.de/route", corpo)).jsonObject
        val trip = r["trip"]!!.jsonObject
        val pontos = ArrayList<Pair<Double, Double>>()
        val manobras = ArrayList<Manobra>()
        val pernas = trip["legs"]!!.jsonArray
        pernas.forEachIndexed { li, leg ->
            val l = leg.jsonObject
            val base = if (pontos.isEmpty()) 0 else pontos.size - 1
            val forma = decodificarPolyline((l["shape"] as JsonPrimitive).content, 6)
            if (pontos.isEmpty()) pontos.addAll(forma) else pontos.addAll(forma.drop(1))
            (l["maneuvers"] as? JsonArray).orEmpty().forEach { m ->
                val o = m.jsonObject
                val tipo = (o["type"] as? JsonPrimitive)?.intOrNull ?: 8
                // chegadas no meio (paradas "through") não interessam
                if (tipo in 4..6 && li != pernas.lastIndex) return@forEach
                manobras += Manobra(
                    tipoValhalla(tipo), (o["instruction"] as? JsonPrimitive)?.content ?: "",
                    (o["verbal_pre_transition_instruction"] as? JsonPrimitive)?.content,
                    (o["street_names"] as? JsonArray)?.firstOrNull()?.let { (it as? JsonPrimitive)?.content },
                    base + ((o["begin_shape_index"] as? JsonPrimitive)?.intOrNull ?: 0),
                    ((o["length"] as? JsonPrimitive)?.doubleOrNull ?: 0.0) * 1000,
                )
            }
        }
        val s = trip["summary"]!!.jsonObject
        return RotaNavegacao(pontos, manobras, ((s["length"] as? JsonPrimitive)?.doubleOrNull ?: 0.0) * 1000, (s["time"] as? JsonPrimitive)?.doubleOrNull ?: 0.0, "valhalla")
    }

    /** Tipos do Valhalla: https://valhalla.github.io/valhalla/api/turn-by-turn/api-reference/ */
    private fun tipoValhalla(t: Int) = when (t) {
        1, 2, 3 -> TipoManobra.SAIDA
        4, 5, 6 -> TipoManobra.CHEGADA
        9 -> TipoManobra.LEVE_DIREITA
        10 -> TipoManobra.DIREITA
        11 -> TipoManobra.FORTE_DIREITA
        12, 13 -> TipoManobra.RETORNO
        14 -> TipoManobra.FORTE_ESQUERDA
        15 -> TipoManobra.ESQUERDA
        16 -> TipoManobra.LEVE_ESQUERDA
        18, 20, 23 -> TipoManobra.LEVE_DIREITA
        19, 21, 24 -> TipoManobra.LEVE_ESQUERDA
        26, 27 -> TipoManobra.ROTATORIA
        else -> TipoManobra.SEGUIR
    }

    private fun osrm(pts: List<Pair<Double, Double>>): RotaNavegacao {
        val coords = pts.joinToString(";") { "%.6f,%.6f".format(java.util.Locale.US, it.second, it.first) }
        val r = json.parseToJsonElement(http("https://router.project-osrm.org/route/v1/driving/$coords?overview=full&geometries=polyline6&steps=true")).jsonObject
        val rota = (r["routes"] as JsonArray).first().jsonObject
        val pontos = decodificarPolyline((rota["geometry"] as JsonPrimitive).content, 6)
        val manobras = ArrayList<Manobra>()
        var busca = 0
        val pernas = rota["legs"]!!.jsonArray
        pernas.forEachIndexed { li, leg ->
            leg.jsonObject["steps"]!!.jsonArray.forEach { st ->
                val o = st.jsonObject
                val m = o["maneuver"]!!.jsonObject
                val tipo = (m["type"] as? JsonPrimitive)?.content ?: "turn"
                if (tipo == "arrive" && li != pernas.lastIndex) return@forEach
                val mod = (m["modifier"] as? JsonPrimitive)?.content
                val loc = m["location"]!!.jsonArray.let { ((it[1] as JsonPrimitive).doubleOrNull ?: 0.0) to ((it[0] as JsonPrimitive).doubleOrNull ?: 0.0) }
                var idx = busca; var melhor = Double.MAX_VALUE
                for (i in busca until minOf(pontos.size, busca + 400)) { val d = metros(pontos[i], loc); if (d < melhor) { melhor = d; idx = i } }
                busca = idx
                val rua = (o["name"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
                val (tm, frase) = fraseOsrm(tipo, mod, (m["exit"] as? JsonPrimitive)?.intOrNull)
                manobras += Manobra(tm, frase + (rua?.let { if (tm == TipoManobra.CHEGADA) "" else " na $it" } ?: ""), null, rua, idx, (o["distance"] as? JsonPrimitive)?.doubleOrNull ?: 0.0)
            }
        }
        return RotaNavegacao(pontos, manobras, (rota["distance"] as? JsonPrimitive)?.doubleOrNull ?: 0.0, (rota["duration"] as? JsonPrimitive)?.doubleOrNull ?: 0.0, "osrm")
    }

    private fun fraseOsrm(tipo: String, mod: String?, saida: Int?): Pair<TipoManobra, String> = when (tipo) {
        "depart" -> TipoManobra.SAIDA to "Siga"
        "arrive" -> TipoManobra.CHEGADA to "Você chegou ao destino"
        "roundabout", "rotary", "roundabout turn" -> TipoManobra.ROTATORIA to (saida?.let { "Na rotatória, pegue a ${it}ª saída" } ?: "Entre na rotatória")
        "exit roundabout", "exit rotary" -> TipoManobra.SEGUIR to "Saia da rotatória"
        else -> when (mod) {
            "right" -> TipoManobra.DIREITA to "Vire à direita"
            "left" -> TipoManobra.ESQUERDA to "Vire à esquerda"
            "slight right" -> TipoManobra.LEVE_DIREITA to "Mantenha-se à direita"
            "slight left" -> TipoManobra.LEVE_ESQUERDA to "Mantenha-se à esquerda"
            "sharp right" -> TipoManobra.FORTE_DIREITA to "Vire bem à direita"
            "sharp left" -> TipoManobra.FORTE_ESQUERDA to "Vire bem à esquerda"
            "uturn" -> TipoManobra.RETORNO to "Faça o retorno"
            else -> TipoManobra.SEGUIR to "Continue"
        }
    }
}
