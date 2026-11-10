package com.pwexpress.feature.corridas.push

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pwexpress.core.PwCore
import com.pwexpress.feature.corridas.data.Corridas
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Botão "Recusar" da notificação da oferta: responde sem abrir o app. */
class OfertaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACAO_RECUSAR) return
        Alarme.pararOferta(context)
        val id = intent.getStringExtra("oferta_id") ?: Corridas.oferta.value?.id ?: return
        val pendente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PwCore.init(context)
                Corridas.init(context)
                val r = withTimeoutOrNull(9_000) { Corridas.responderPorId(id, aceitar = false) }
                if (r == null || r.isFailure) Alarme.avisarSilencioso(context, "Não consegui recusar pela notificação. Abra o app.", "Oferta de corrida")
            } finally {
                pendente.finish()
            }
        }
    }

    companion object { const val ACAO_RECUSAR = "com.pwexpress.OFERTA_RECUSAR" }
}
