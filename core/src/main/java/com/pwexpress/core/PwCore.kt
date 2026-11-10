package com.pwexpress.core

import android.content.Context
import com.pwexpress.core.auth.AuthRepository
import com.pwexpress.core.auth.ContasSalvas
import com.pwexpress.core.auth.SessaoStore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.network.SupabaseHttp
import com.pwexpress.core.network.SupabaseRpc

/** Ponto único de acesso aos serviços compartilhados entre os módulos. Inicializado no Application. */
object PwCore {
    lateinit var http: SupabaseHttp
        private set
    lateinit var auth: AuthRepository
        private set
    lateinit var rpc: SupabaseRpc
        private set

    /** Contexto do app (para guardar coisas pequenas entre aberturas, como as conquistas). */
    lateinit var app: Context
        private set

    fun init(context: Context) {
        if (::http.isInitialized) return
        val app = context.applicationContext
        this.app = app
        http = SupabaseHttp(PwConfig.supabaseUrl, PwConfig.supabaseKey)
        auth = AuthRepository(http, SessaoStore(app), ContasSalvas(app))
        auth.aplicarPermanencia()
        rpc = SupabaseRpc(http, auth)
    }
}
