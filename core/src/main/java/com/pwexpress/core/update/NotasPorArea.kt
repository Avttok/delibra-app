package com.pwexpress.core.update

/**
 * Notas da versão por área. O NOTAS_DA_VERSAO.md é dividido em "## Todos", "## Motoboy", "## Gestor",
 * "## Supervisor", "## Cliente" e "## Dev". Cada pessoa vê só as áreas dos cargos dela; o Dev vê tudo;
 * a área Dev só o Dev vê. Texto antes da primeira área (ou notas antigas sem áreas) vale para todos.
 */
object NotasPorArea {
    val AREAS = listOf("todos", "motoboy", "gestor", "supervisor", "cliente", "dev")

    fun areasDe(papeis: List<String>, dev: Boolean): Set<String> =
        if (dev || "dev" in papeis) AREAS.toSet() else (setOf("todos") + papeis.filter { it in AREAS && it != "dev" })

    /** Nome da área de um título "## …" (sem acento, minúsculo), ou null se não for área. */
    fun areaDoTitulo(linha: String): String? {
        if (!linha.startsWith("## ")) return null
        val t = java.text.Normalizer.normalize(linha.removePrefix("## ").trim(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "").lowercase().removePrefix("para ").removePrefix("o ").trim()
        return when {
            t.startsWith("todo") -> "todos"
            t.startsWith("motoboy") -> "motoboy"
            t.startsWith("gestor") -> "gestor"
            t.startsWith("supervisor") -> "supervisor"
            t.startsWith("cliente") -> "cliente"
            t.startsWith("dev") -> "dev"
            else -> null
        }
    }

    fun filtrar(notas: String, areas: Set<String>): String {
        val saida = StringBuilder()
        var areaAtual: String? = null
        var temAlgo = false
        for (linha in notas.lines()) {
            val a = areaDoTitulo(linha)
            if (a != null) { areaAtual = a; if (a in areas) { saida.appendLine(linha); temAlgo = true }; continue }
            if (linha.startsWith("## ") || linha.startsWith("# ")) areaAtual = null
            if (areaAtual == null || areaAtual in areas) { saida.appendLine(linha); if (areaAtual != null && linha.isNotBlank()) temAlgo = true }
        }
        val r = saida.toString().trimEnd()
        return if (r.isBlank() || (!temAlgo && notas.lines().any { areaDoTitulo(it) != null })) "Nada novo para você nesta versão." else r
    }
}
