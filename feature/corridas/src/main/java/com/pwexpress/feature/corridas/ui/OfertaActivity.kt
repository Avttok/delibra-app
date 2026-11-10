package com.pwexpress.feature.corridas.ui

import android.os.Build
import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pwexpress.core.designsystem.PwFundo
import com.pwexpress.core.designsystem.PwTheme
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.push.Alarme
import kotlinx.coroutines.delay

/** Abre por cima da tela bloqueada quando chega uma oferta. */
class OfertaActivity : ComponentActivity() {

    companion object { const val ACAO_ACEITAR = "com.pwexpress.OFERTA_ACEITAR" }

    /** Veio do botão "Aceitar" da notificação: aceita assim que a oferta carregar. */
    private val aceitarAoAbrir = androidx.compose.runtime.mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == ACAO_ACEITAR) aceitarAoAbrir.value = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        aceitarAoAbrir.value = intent?.action == ACAO_ACEITAR
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        setContent {
            PwTheme {
                PwFundo {
                    val oferta by Corridas.oferta.collectAsState()
                    val atual = oferta
                    if (atual == null) {
                        CarregandoOferta()
                        LaunchedEffect(Unit) {
                            Corridas.sincronizarAgora()
                            delay(8_000)
                            if (Corridas.oferta.value == null) fechar(abrirApp = false)
                        }
                    } else if (aceitarAoAbrir.value) {
                        CarregandoOferta()
                        LaunchedEffect(atual.id) {
                            Alarme.pararOferta(this@OfertaActivity)
                            val r = Corridas.responder(aceitar = true)
                            aceitarAoAbrir.value = false
                            if (r.isSuccess) fechar(abrirApp = true)
                        }
                    } else {
                        OfertaScreen(atual, onFim = { aceitou -> fechar(abrirApp = aceitou) }, modifier = Modifier.safeDrawingPadding())
                    }
                }
            }
        }
    }

    private fun fechar(abrirApp: Boolean) {
        Alarme.pararOferta(this)
        if (abrirApp) {
            packageManager.getLaunchIntentForPackage(packageName)?.let { startActivity(it) }
        }
        finish()
    }
}
