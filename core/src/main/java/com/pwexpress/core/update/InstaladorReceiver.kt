package com.pwexpress.core.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build

/** Recebe o andamento do instalador do Android: pede a confirmação do usuário ou avisa o erro. */
class InstaladorReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirmar = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_INTENT)
                confirmar?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { context.startActivity(it) } }
            }
            PackageInstaller.STATUS_SUCCESS -> Atualizador.resultado(true, null)
            PackageInstaller.STATUS_FAILURE_ABORTED -> Atualizador.resultado(false, "A instalação foi cancelada.")
            PackageInstaller.STATUS_FAILURE_CONFLICT -> Atualizador.resultado(false, "Conflito com a versão instalada (assinatura diferente). Desinstale e instale o APK novo manualmente.")
            PackageInstaller.STATUS_FAILURE_STORAGE -> Atualizador.resultado(false, "Sem espaço no celular para instalar.")
            else -> Atualizador.resultado(false, intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "A instalação falhou.")
        }
    }

    companion object { const val ACAO = "com.pwexpress.ATUALIZACAO" }
}
