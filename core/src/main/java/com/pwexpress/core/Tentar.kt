package com.pwexpress.core

import kotlin.coroutines.cancellation.CancellationException

/**
 * Como runCatching, mas deixa o cancelamento passar. Sem isso, sair de uma tela no meio de um
 * carregamento virava o erro "StandaloneCoroutine was cancelled" na tela ou num aviso.
 */
inline fun <T> tentar(bloco: () -> T): Result<T> =
    try { Result.success(bloco()) } catch (e: CancellationException) { throw e } catch (e: Throwable) { Result.failure(e) }

/** Erro que é só um cancelamento (a tela saiu, o trabalho foi trocado): não mostrar ao usuário. */
fun Throwable?.ehCancelamento(): Boolean {
    var e = this
    while (e != null) { if (e is CancellationException) return true; e = e.cause }
    return false
}

/** Texto de erro que vem de um cancelamento. */
fun ehMensagemDeCancelamento(t: String?): Boolean = t != null && (t.contains("was cancelled", true) || t.contains("StandaloneCoroutine", true))
