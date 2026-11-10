package com.pwexpress.core.perfil

/**
 * Identifica o tipo de uma chave PIX pelo que foi digitado (igual ao site, em src/lib/pix):
 *  • tem "@"                          → e-mail
 *  • 32 hex ou UUID com hífens        → aleatória
 *  • 14 caracteres (números ou CNPJ alfanumérico) → CNPJ (dígitos conferidos)
 *  • 11 números com CPF válido        → CPF
 *  • 11 números com DDD + 9 na frente → celular (também +55…)
 * Quando 11 números servem para CPF e celular ao mesmo tempo, devolve os dois e a pessoa escolhe.
 */
object IdentificadorPix {

    enum class Tipo(val valor: String, val rotulo: String) {
        CPF("cpf", "CPF"), CNPJ("cnpj", "CNPJ"), TELEFONE("telefone", "Celular"), EMAIL("email", "E-mail"), ALEATORIA("aleatoria", "Aleatória");
        companion object { fun de(v: String) = entries.firstOrNull { it.valor == v } }
    }

    /** Resultado: tipos possíveis (o primeiro é o mais provável) ou o motivo de não servir. */
    data class Leitura(val tipos: List<Tipo>, val erro: String? = null) {
        val tipo: Tipo? get() = tipos.firstOrNull()
        val ambiguo: Boolean get() = tipos.size > 1
    }

    private val UUID = Regex("^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$")
    private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private val CNPJ_ALFA = Regex("^[A-Z0-9]{12}[0-9]{2}$")

    fun ler(entrada: String): Leitura {
        val t = entrada.trim()
        if (t.isEmpty()) return Leitura(emptyList())
        if ('@' in t) return if (EMAIL.matches(t)) Leitura(listOf(Tipo.EMAIL)) else Leitura(emptyList(), "E-mail incompleto.")
        if (UUID.matches(t)) return Leitura(listOf(Tipo.ALEATORIA))
        val semPontuacao = t.replace(Regex("[\\s.\\-/()]"), "")
        if (semPontuacao.startsWith("+")) {
            val d = semPontuacao.drop(1)
            return if (d.all(Char::isDigit) && celularValido(d.removePrefix("55")) && d.startsWith("55")) Leitura(listOf(Tipo.TELEFONE))
            else Leitura(emptyList(), "Celular inválido. Use DDD + número.")
        }
        val alfa = semPontuacao.uppercase()
        if (alfa.length == 14 && CNPJ_ALFA.matches(alfa)) return if (cnpjValido(alfa)) Leitura(listOf(Tipo.CNPJ)) else Leitura(emptyList(), "CNPJ com dígitos errados.")
        if (!semPontuacao.all(Char::isDigit)) return Leitura(emptyList(), "Não parece CPF, CNPJ, celular, e-mail ou chave aleatória.")
        val d = semPontuacao
        return when (d.length) {
            11 -> {
                val cpf = cpfValido(d); val cel = celularValido(d)
                // pontuação de CPF (000.000.000-00) desempata para CPF; parênteses ou espaço, para celular
                val pareceCpf = Regex("^\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}$").matches(t)
                val pareceCel = t.contains('(') || Regex("^\\d{2}\\s").containsMatchIn(t)
                when {
                    cpf && pareceCpf -> Leitura(listOf(Tipo.CPF))
                    cel && pareceCel -> Leitura(listOf(Tipo.TELEFONE))
                    cpf && cel -> Leitura(listOf(Tipo.CPF, Tipo.TELEFONE))
                    cpf -> Leitura(listOf(Tipo.CPF))
                    cel -> Leitura(listOf(Tipo.TELEFONE))
                    else -> Leitura(emptyList(), "CPF com dígitos errados.")
                }
            }
            13 -> if (d.startsWith("55") && celularValido(d.drop(2))) Leitura(listOf(Tipo.TELEFONE)) else Leitura(emptyList(), "Celular inválido.")
            14 -> if (cnpjValido(d)) Leitura(listOf(Tipo.CNPJ)) else Leitura(emptyList(), "CNPJ com dígitos errados.")
            10 -> Leitura(emptyList(), "Telefone fixo não serve: use o celular com o 9.")
            else -> Leitura(emptyList(), if (d.length < 11) "Faltam números." else "Números demais.")
        }
    }

    /** Como a chave é guardada: CPF/CNPJ só caracteres, celular +55…, e-mail minúsculo, aleatória com hífens. */
    fun normalizar(entrada: String, tipo: Tipo): String {
        val t = entrada.trim()
        val limpo = t.replace(Regex("[\\s.\\-/()+]"), "")
        return when (tipo) {
            Tipo.CPF -> limpo
            Tipo.CNPJ -> limpo.uppercase()
            Tipo.TELEFONE -> "+" + (if (limpo.length == 13) limpo else "55$limpo")
            Tipo.EMAIL -> t.lowercase()
            Tipo.ALEATORIA -> t.lowercase().replace("-", "").let { "${it.substring(0, 8)}-${it.substring(8, 12)}-${it.substring(12, 16)}-${it.substring(16, 20)}-${it.substring(20)}" }
        }
    }

    /** Chave guardada → como mostrar. */
    fun formatar(chave: String, tipo: String): String = when (tipo) {
        "cpf" -> if (chave.length == 11) "${chave.substring(0, 3)}.${chave.substring(3, 6)}.${chave.substring(6, 9)}-${chave.substring(9)}" else chave
        "cnpj" -> if (chave.length == 14) "${chave.substring(0, 2)}.${chave.substring(2, 5)}.${chave.substring(5, 8)}/${chave.substring(8, 12)}-${chave.substring(12)}" else chave
        "telefone" -> Regex("^\\+55(\\d{2})(\\d{5})(\\d{4})$").find(chave)?.destructured?.let { (a, b, c) -> "($a) $b-$c" } ?: chave
        else -> chave
    }

    private fun celularValido(d: String): Boolean =
        d.length == 11 && d.all(Char::isDigit) && d[0] in '1'..'9' && d[1] in '1'..'9' && d[2] == '9'

    private fun cpfValido(d: String): Boolean {
        if (d.length != 11 || d.toSet().size == 1) return false
        val n = d.map { it - '0' }
        fun dv(ate: Int): Int { val s = (0 until ate).sumOf { n[it] * (ate + 1 - it) }; val r = (s * 10) % 11; return if (r == 10) 0 else r }
        return dv(9) == n[9] && dv(10) == n[10]
    }

    /** CNPJ numérico ou alfanumérico (valor de cada caractere = código ASCII − 48). */
    private fun cnpjValido(v: String): Boolean {
        if (v.length != 14 || v.toSet().size == 1) return false
        val n = v.map { it.code - 48 }
        fun dv(pesos: List<Int>): Int { val s = pesos.indices.sumOf { n[it] * pesos[it] }; val r = s % 11; return if (r < 2) 0 else 11 - r }
        val p1 = listOf(5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2)
        val p2 = listOf(6) + p1
        return dv(p1) == n[12] && dv(p2) == n[13]
    }
}
