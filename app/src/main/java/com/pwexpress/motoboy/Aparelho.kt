package com.pwexpress.motoboy

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pwexpress.core.PwCore
import com.pwexpress.core.tentar
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Conta ao servidor como está o aparelho (aparece no Console Dev → Aparelhos). No máximo a cada 20 min. */
object Aparelho {
    @Volatile private var ultimo = 0L

    suspend fun registrar(ctx: Context) {
        if (System.currentTimeMillis() - ultimo < 20 * 60_000) return
        ultimo = System.currentTimeMillis()
        tentar {
            val pm = ctx.packageManager
            fun tem(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
            val info = pm.getPackageInfo(ctx.packageName, 0)
            val cm = ctx.getSystemService(ConnectivityManager::class.java)
            val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
            val rede = when {
                caps == null -> "sem internet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "dados móveis"
                else -> "outra"
            }
            val bat = ctx.getSystemService(BatteryManager::class.java)
            val mem = ActivityManager.MemoryInfo().also { ctx.getSystemService(ActivityManager::class.java)?.getMemoryInfo(it) }
            val dm = ctx.resources.displayMetrics
            val dados = buildJsonObject {
                put("fabricante", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                put("modelo", Build.MODEL)
                put("android", Build.VERSION.RELEASE)
                put("sdk", Build.VERSION.SDK_INT)
                put("build", if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong())
                put("versao", info.versionName ?: "")
                put("localizacao_precisa", tem(Manifest.permission.ACCESS_FINE_LOCATION))
                put("localizacao_fundo", if (Build.VERSION.SDK_INT >= 29) tem(Manifest.permission.ACCESS_BACKGROUND_LOCATION) else tem(Manifest.permission.ACCESS_FINE_LOCATION))
                put("gps_ligado", ctx.getSystemService(LocationManager::class.java)?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true)
                put("notificacoes", NotificationManagerCompat.from(ctx).areNotificationsEnabled())
                put("otimizacao_bateria", ctx.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(ctx.packageName) != true)
                put("economia_android", ctx.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true)
                put("carregando", bat?.isCharging == true)
                put("bateria", bat?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1)
                put("rede", rede)
                put("tela", "${dm.widthPixels}×${dm.heightPixels}")
                put("memoria_livre_mb", mem.availMem / 1_048_576)
                put("idioma", java.util.Locale.getDefault().toLanguageTag())
                put("fuso", java.util.TimeZone.getDefault().id)
            }
            PwCore.rpc.chamar("registrar_aparelho", buildJsonObject { put("p_info", dados) })
        }
    }
}
