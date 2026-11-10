package com.pwexpress.feature.corridas.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class Posicao(val lat: Double, val lng: Double, val precisaoM: Float, val quando: Long)

/** Posição atual do celular (GPS), usada para validar a chegada e mostrar a distância. */
object PosicaoAtual {

    private fun permitido(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Leitura nova de alta precisão (até ~10 s); se falhar, a última conhecida. */
    @SuppressLint("MissingPermission")
    suspend fun obter(ctx: Context, timeoutMs: Long = 10_000): Posicao? {
        if (!permitido(ctx)) return null
        val cliente = LocationServices.getFusedLocationProviderClient(ctx)
        val nova = withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine<Location?> { cont ->
                val token = CancellationTokenSource()
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, token.token)
                    .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                    .addOnFailureListener { if (cont.isActive) cont.resume(null) }
                cont.invokeOnCancellation { token.cancel() }
            }
        }
        val loc = nova ?: suspendCancellableCoroutine<Location?> { cont ->
            cliente.lastLocation
                .addOnSuccessListener { if (cont.isActive) cont.resume(it) }
                .addOnFailureListener { if (cont.isActive) cont.resume(null) }
        }
        return loc?.let { Posicao(it.latitude, it.longitude, if (it.hasAccuracy()) it.accuracy else 50f, it.time) }
    }

    fun distanciaM(de: Posicao, lat: Double, lng: Double): Float {
        val r = FloatArray(1)
        Location.distanceBetween(de.lat, de.lng, lat, lng, r)
        return r[0]
    }
}
