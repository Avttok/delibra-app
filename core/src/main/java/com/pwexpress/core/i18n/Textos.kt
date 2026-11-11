package com.pwexpress.core.i18n

/**
 * Estrutura de idiomas do app (pronta; até a versão 1.0 tudo é português do Brasil).
 * Para traduzir: crie o mapa do idioma com as mesmas chaves e registre em [idiomas].
 * Uso: Textos.t("comum.salvar") ou Textos.t("pedido.numero", "n" to 12).
 */
object Textos {
    private val ptBR = mapOf(
        "comum.salvar" to "Salvar", "comum.cancelar" to "Cancelar", "comum.voltar" to "Voltar", "comum.carregando" to "Carregando…",
        "comum.sem_internet" to "Sem internet.", "pedido.numero" to "Pedido #{n}", "pedido.pago" to "PAGO", "rota.numero" to "Rota #{n}",
        "chat.resposta_automatica" to "Resposta automática",
    )
    private val idiomas = mapOf("pt-BR" to ptBR)
    @Volatile var atual: String = "pt-BR"

    fun t(chave: String, vararg vars: Pair<String, Any>): String {
        var s = idiomas[atual]?.get(chave) ?: ptBR[chave] ?: chave
        vars.forEach { (k, v) -> s = s.replace("{$k}", v.toString()) }
        return s
    }
}
