package com.pwexpress.core.busca

/**
 * Busca que perdoa (igual à do site): sem acento e sem caixa; cada palavra pode estar pela
 * metade ("conf" acha "configurações") ou com um erro de digitação ("pagamneto" acha "pagamento").
 */
fun normal(t: String): String = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

private fun distancia(a: String, b: String, limite: Int): Int {
    if (kotlin.math.abs(a.length - b.length) > limite) return limite + 1
    var ant2 = IntArray(0); var ant = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val cur = IntArray(b.length + 1); cur[0] = i
        var menor = i
        for (j in 1..b.length) {
            val custo = if (a[i - 1] == b[j - 1]) 0 else 1
            var v = minOf(ant[j] + 1, cur[j - 1] + 1, ant[j - 1] + custo)
            if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) v = minOf(v, ant2[j - 2] + 1)
            cur[j] = v; if (v < menor) menor = v
        }
        if (menor > limite) return limite + 1
        ant2 = ant; ant = cur
    }
    return ant[b.length]
}

fun combina(termo: String, vararg campos: String?): Boolean {
    val ps = normal(termo).split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (ps.isEmpty()) return true
    val tudo = campos.filterNotNull().joinToString(" ") { normal(it) }
    val palavras = tudo.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
    return ps.all { p ->
        tudo.contains(p) || (p.length >= 4 && run {
            val lim = if (p.length >= 8) 2 else 1
            palavras.any { w -> distancia(p, w.take(p.length + 1), lim) <= lim || distancia(p, w, lim) <= lim }
        })
    }
}
