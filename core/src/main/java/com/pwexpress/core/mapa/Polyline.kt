package com.pwexpress.core.mapa

/** Decodifica polyline do Google (precisão 5) em pares (lat, lng). */
fun decodificarPolyline(texto: String): List<Pair<Double, Double>> {
    val pontos = ArrayList<Pair<Double, Double>>()
    var i = 0
    var lat = 0
    var lng = 0
    while (i < texto.length) {
        for (eixo in 0..1) {
            var resultado = 0
            var deslocamento = 0
            var b: Int
            do {
                b = texto[i++].code - 63
                resultado = resultado or ((b and 0x1f) shl deslocamento)
                deslocamento += 5
            } while (b >= 0x20 && i < texto.length)
            val delta = if (resultado and 1 != 0) (resultado shr 1).inv() else resultado shr 1
            if (eixo == 0) lat += delta else lng += delta
        }
        pontos.add(lat / 1e5 to lng / 1e5)
    }
    return pontos
}

/** Decodifica polyline com outra precisão (Valhalla usa 6). */
fun decodificarPolyline(texto: String, precisao: Int): List<Pair<Double, Double>> {
    if (precisao == 5) return decodificarPolyline(texto)
    val fator = Math.pow(10.0, precisao.toDouble())
    val pontos = ArrayList<Pair<Double, Double>>()
    var i = 0
    var lat = 0L
    var lng = 0L
    while (i < texto.length) {
        for (eixo in 0..1) {
            var resultado = 0L
            var deslocamento = 0
            var b: Int
            do {
                b = texto[i++].code - 63
                resultado = resultado or ((b and 0x1f).toLong() shl deslocamento)
                deslocamento += 5
            } while (b >= 0x20 && i < texto.length)
            val delta = if (resultado and 1L != 0L) (resultado shr 1).inv() else resultado shr 1
            if (eixo == 0) lat += delta else lng += delta
        }
        pontos.add(lat / fator to lng / fator)
    }
    return pontos
}

/** Codifica (lat, lng) em polyline de precisão 5 (o formato que os mapas do app leem). */
fun codificarPolyline(pontos: List<Pair<Double, Double>>): String {
    val sb = StringBuilder()
    var pLat = 0L
    var pLng = 0L
    fun enc(v: Long) {
        var x = if (v < 0) (v shl 1).inv() else v shl 1
        while (x >= 0x20) { sb.append(((0x20 or (x and 0x1f).toInt()) + 63).toChar()); x = x shr 5 }
        sb.append((x + 63).toInt().toChar())
    }
    pontos.forEach { (la, lo) ->
        val a = Math.round(la * 1e5); val b = Math.round(lo * 1e5)
        enc(a - pLat); enc(b - pLng); pLat = a; pLng = b
    }
    return sb.toString()
}
