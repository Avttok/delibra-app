package com.pwexpress.feature.tracking.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

data class EstadoPermissoes(
    val localizacao: Boolean,
    val notificacoes: Boolean,
    val segundoPlano: Boolean,
    val semRestricaoBateria: Boolean,
) {
    val basicasOk: Boolean get() = localizacao && notificacoes
    val tudoOk: Boolean get() = basicasOk && segundoPlano && semRestricaoBateria
}

object Permissoes {

    fun basicas(): Array<String> = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    }.toTypedArray()

    const val SEGUNDO_PLANO = Manifest.permission.ACCESS_BACKGROUND_LOCATION

    fun ler(ctx: Context): EstadoPermissoes {
        fun ok(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
        val pm = ctx.getSystemService(PowerManager::class.java)
        return EstadoPermissoes(
            localizacao = ok(Manifest.permission.ACCESS_FINE_LOCATION),
            notificacoes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ok(Manifest.permission.POST_NOTIFICATIONS)
            } else true,
            segundoPlano = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ok(SEGUNDO_PLANO) else true,
            semRestricaoBateria = pm?.isIgnoringBatteryOptimizations(ctx.packageName) ?: true,
        )
    }

    /** Pede para o Android não "adormecer" o app (crítico em Samsung/Xiaomi/Motorola). */
    @SuppressLint("BatteryLife") // distribuição por sideload; revisar antes da Play Store
    fun pedirSemRestricaoBateria(ctx: Context) {
        val pedido = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${ctx.packageName}"))
        runCatching { ctx.startActivity(pedido) }.onFailure { abrirConfiguracoesDoApp(ctx) }
    }

    fun abrirConfiguracoesDoApp(ctx: Context) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
        runCatching { ctx.startActivity(i) }
    }
}
