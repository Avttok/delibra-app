package com.pwexpress.core.auth

import android.content.Context
import androidx.core.content.edit

data class Sessao(
    val accessToken: String,
    val refreshToken: String,
    val expiraEm: Long,
    val userId: String,
    val email: String,
)

class SessaoStore(context: Context) {
    private val prefs = context.getSharedPreferences("pwex_sessao", Context.MODE_PRIVATE)

    fun ler(): Sessao? {
        val access = prefs.getString("access", null) ?: return null
        val refresh = prefs.getString("refresh", null) ?: return null
        return Sessao(
            accessToken = access,
            refreshToken = refresh,
            expiraEm = prefs.getLong("expira", 0L),
            userId = prefs.getString("user_id", "").orEmpty(),
            email = prefs.getString("email", "").orEmpty(),
        )
    }

    fun salvar(s: Sessao) = prefs.edit {
        putString("access", s.accessToken)
        putString("refresh", s.refreshToken)
        putLong("expira", s.expiraEm)
        putString("user_id", s.userId)
        putString("email", s.email)
    }

    /** "Permanecer conectado" (padrão: sim). Fica fora do limpar() para lembrar a escolha. */
    var permanecer: Boolean
        get() = escolhas.getBoolean("permanecer", true)
        set(v) = escolhas.edit { putBoolean("permanecer", v) }

    /** Quando entrou (para expirar sessões sem "Permanecer conectado"). */
    var entrouEm: Long
        get() = escolhas.getLong("entrou_em", 0L)
        set(v) = escolhas.edit { putLong("entrou_em", v) }

    private val escolhas = context.getSharedPreferences("pwex_login", Context.MODE_PRIVATE)

    fun limpar() = prefs.edit { clear() }
}
