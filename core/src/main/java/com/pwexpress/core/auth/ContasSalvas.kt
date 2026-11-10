package com.pwexpress.core.auth

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Contas que já entraram neste aparelho ("Logar com").
 * Senha nunca é guardada. Só quem marcou "Permanecer conectado" guarda o token de
 * renovação, que permite entrar com um toque; as outras só preenchem o e-mail.
 */
data class ContaSalva(
    val email: String,
    val nome: String?,
    val fotoUrl: String?,
    val refreshToken: String?,
    val ultimoAcesso: Long,
)

class ContasSalvas(context: Context) {
    private val prefs = context.getSharedPreferences("pwex_contas", Context.MODE_PRIVATE)
    private val _contas = MutableStateFlow(ler())
    val contas: StateFlow<List<ContaSalva>> = _contas.asStateFlow()

    private fun ler(): List<ContaSalva> = runCatching {
        val bruto = prefs.getString("contas", null) ?: return emptyList()
        Json.parseToJsonElement(bruto).jsonArray.map { e ->
            val o = e.jsonObject
            fun txt(k: String) = (o[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
            ContaSalva(
                email = txt("email").orEmpty(),
                nome = txt("nome"),
                fotoUrl = txt("foto"),
                refreshToken = txt("refresh"),
                ultimoAcesso = txt("ultimo")?.toLongOrNull() ?: 0L,
            )
        }.filter { it.email.isNotBlank() }.sortedByDescending { it.ultimoAcesso }
    }.getOrDefault(emptyList())

    private fun gravar(lista: List<ContaSalva>) {
        val arr = JsonArray(lista.map { c ->
            buildJsonObject {
                put("email", c.email)
                c.nome?.let { put("nome", it) }
                c.fotoUrl?.let { put("foto", it) }
                c.refreshToken?.let { put("refresh", it) }
                put("ultimo", c.ultimoAcesso.toString())
            }
        })
        prefs.edit { putString("contas", arr.toString()) }
        _contas.value = lista.sortedByDescending { it.ultimoAcesso }
    }

    /** Registra/atualiza a conta. `refreshToken = null` remove o acesso de um toque. */
    fun lembrar(email: String, refreshToken: String?) {
        val chave = email.trim().lowercase()
        val atual = _contas.value.firstOrNull { it.email.equals(chave, true) }
        val nova = ContaSalva(chave, atual?.nome, atual?.fotoUrl, refreshToken, System.currentTimeMillis())
        gravar(listOf(nova) + _contas.value.filterNot { it.email.equals(chave, true) })
    }

    /** Atualiza nome e foto exibidos (vindos do perfil). */
    fun atualizarPerfil(email: String, nome: String?, fotoUrl: String?) {
        val lista = _contas.value.map { if (it.email.equals(email, true)) it.copy(nome = nome ?: it.nome, fotoUrl = fotoUrl ?: it.fotoUrl) else it }
        if (lista != _contas.value) gravar(lista)
    }

    /** O token de renovação muda a cada renovação: mantém o salvo sempre atual. */
    fun atualizarToken(email: String, refreshToken: String) {
        val lista = _contas.value.map { if (it.email.equals(email, true) && it.refreshToken != null) it.copy(refreshToken = refreshToken) else it }
        if (lista != _contas.value) gravar(lista)
    }

    fun esquecerToken(email: String) = gravar(_contas.value.map { if (it.email.equals(email, true)) it.copy(refreshToken = null) else it })

    fun remover(email: String) = gravar(_contas.value.filterNot { it.email.equals(email, true) })
}
