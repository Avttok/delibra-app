package com.pwexpress.feature.tracking.data

import android.content.Context
import androidx.core.content.edit
import com.pwexpress.feature.tracking.Modo

/** Estado persistente: sobrevive se o Android matar o app. */
internal class TrackingPrefs(context: Context) {
    private val p = context.getSharedPreferences("pwex_tracking", Context.MODE_PRIVATE)

    var modo: Modo
        get() = Modo.de(p.getString("modo", null))
        set(v) = p.edit { putString("modo", v.api) }

    var economia: Boolean
        get() = p.getBoolean("economia", false)
        set(v) = p.edit { putBoolean("economia", v) }

    /** Identifica um período "em corrida" (vira corrida_id na próxima fase). */
    var sessaoId: String?
        get() = p.getString("sessao_id", null)
        set(v) = p.edit { putString("sessao_id", v) }

    /** Mudança de estado ainda não confirmada pelo servidor. */
    var statusPendente: Boolean
        get() = p.getBoolean("status_pendente", false)
        set(v) = p.edit { putBoolean("status_pendente", v) }
}
