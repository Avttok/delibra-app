package com.pwexpress.feature.tracking.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.pwexpress.core.notificacoes.CorridaEmCurso
import com.pwexpress.core.notificacoes.NotificacaoAoVivo
import com.pwexpress.core.notificacoes.PwNotificacoes
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import androidx.core.app.ServiceCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pwexpress.feature.tracking.Intervalos
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.R
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.data.PontoGps
import com.pwexpress.feature.tracking.ui.Permissoes
import java.util.UUID

/**
 * Serviço em primeiro plano (notificação fixa) que coleta o GPS.
 * Continua rodando com a tela apagada ou o app fechado.
 */
class TrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private var configAtual: Triple<Modo, Boolean, Long>? = null
    private var ultimoEnviado: Location? = null
    private var ultimoEnviadoEm = 0L
    /** Rumo do último trecho enviado (para o ping inteligente saber se virou). */
    private var rumoAnterior: Float? = null
    private var redeCallback: ConnectivityManager.NetworkCallback? = null
    private val escopo = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { processar(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        criarCanal()
        registrarRede()
        // corrida em andamento: a notificação fixa vira a notificação "ao vivo" da corrida
        escopo.launch {
            kotlinx.coroutines.flow.combine(CorridaEmCurso.resumo, CorridaEmCurso.instrucao) { r, i -> r to i }.collect {
                val modo = Tracking.prefs.modo
                if (modo != Modo.OFFLINE) runCatching {
                    getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notificacao(modo, Tracking.prefs.economia))
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val modo = Tracking.prefs.modo
        val economia = Tracking.prefs.economia

        // Regra do Android: após startForegroundService, chamar startForeground em até 5 s.
        try {
            val tipo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else 0
            ServiceCompat.startForeground(this, NOTIF_ID, notificacao(modo, economia), tipo)
        } catch (e: Exception) {
            Tracking.reportarErro("Rastreamento bloqueado pelo Android: ${e.message}")
            stopSelf()
            return START_NOT_STICKY
        }

        if (modo == Modo.OFFLINE || !Permissoes.ler(this).localizacao) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        aplicar(modo, economia)
        return START_STICKY // se o Android matar o serviço, ele é recriado
    }

    override fun onDestroy() {
        escopo.cancel()
        fused.removeLocationUpdates(callback)
        redeCallback?.let {
            runCatching { getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        }
        super.onDestroy()
    }

    // ------------------------------------------------------------------ GPS

    @SuppressLint("MissingPermission") // verificado em onStartCommand
    private fun aplicar(modo: Modo, economia: Boolean) {
        val nova = Triple(modo, economia, Intervalos.corridaMs(economia) * 1000 + Intervalos.corridaDistanciaM.toLong())
        if (nova == configAtual) return
        configAtual = nova

        fused.removeLocationUpdates(callback)
        ultimoEnviado = null
        rumoAnterior = null

        val req = if (modo == Modo.EM_CORRIDA) {
            // com "a cada X m" ligado, lê mais rápido e decide em processar() (tempo OU distância)
            val intervalo = Intervalos.corridaMs(economia)
            val leitura = if (Intervalos.corridaDistanciaM > 0f) minOf(intervalo, if (economia) 5_000L else 2_000L) else intervalo
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, leitura)
                .setMinUpdateIntervalMillis(minOf(leitura, 1_000L))
                .setWaitForAccurateLocation(false)
                .build()
        } else {
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, Intervalos.DISPONIVEL_LEITURA_MS)
                .setMinUpdateIntervalMillis(Intervalos.DISPONIVEL_LEITURA_MS / 2)
                .build()
        }
        fused.requestLocationUpdates(req, callback, Looper.getMainLooper())
        getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notificacao(modo, economia))
    }

    private fun processar(loc: Location) {
        if (com.pwexpress.feature.tracking.Simulador.ativo) return // modo teste: quem manda a posição é o simulador
        val modo = Tracking.prefs.modo
        when (modo) {
            Modo.OFFLINE -> return
            Modo.DISPONIVEL -> {
                val ult = ultimoEnviado
                val deveEnviar = ult == null ||
                    loc.time - ultimoEnviadoEm >= Intervalos.disponivelEnvioMs ||
                    loc.distanceTo(ult) >= Intervalos.disponivelDistanciaM
                if (!deveEnviar) return
            }
            Modo.EM_CORRIDA -> {
                val ult = ultimoEnviado
                val passou = if (ult == null) Long.MAX_VALUE else loc.time - ultimoEnviadoEm
                val intervalo = Intervalos.corridaMs(Tracking.prefs.economia)
                // leitura ruim: descarta; mas, sem nada bom há 3 intervalos, aceita até o teto para não sumir do mapa
                val limite = if (passou >= intervalo * 3) Intervalos.PRECISAO_MAXIMA_M else Intervalos.precisaoMaximaM
                if (loc.hasAccuracy() && loc.accuracy > limite) return
                if (ult != null) {
                    val andou = loc.distanceTo(ult)
                    val porTempo = passou >= intervalo - 300
                    var porDistancia = Intervalos.corridaDistanciaM > 0f && andou >= Intervalos.corridaDistanciaM
                    // ping inteligente: andou o mínimo mas segue reto → espera a curva ou o máximo da reta
                    if (porDistancia && Intervalos.inteligente && !porTempo) {
                        val rumo = ult.bearingTo(loc)
                        val antes = rumoAnterior
                        val virou = antes != null && kotlin.math.abs(((rumo - antes + 540f) % 360f) - 180f) >= Intervalos.curvaGraus
                        porDistancia = antes == null || virou || andou >= Intervalos.retaMaxM
                    }
                    if (!porTempo && !porDistancia) return
                    // parado no mesmo lugar (sinal oscilando): não enche o percurso de pontos
                    if (!porDistancia && andou < 3f && passou < intervalo * 2) return
                    if (andou >= 5f) rumoAnterior = ult.bearingTo(loc)
                }
            }
        }
        if (modo != Modo.EM_CORRIDA) rumoAnterior = null
        ultimoEnviado = loc
        ultimoEnviadoEm = loc.time

        Tracking.registrarPonto(
            PontoGps(
                idLocal = UUID.randomUUID().toString(),
                sessaoId = Tracking.prefs.sessaoId,
                status = modo,
                lat = loc.latitude,
                lng = loc.longitude,
                precisao = if (loc.hasAccuracy()) loc.accuracy else null,
                velocidade = if (loc.hasSpeed()) loc.speed else null,
                direcao = if (loc.hasBearing()) loc.bearing else null,
                altitude = if (loc.hasAltitude()) loc.altitude else null,
                bateria = bateria(),
                registradoEm = loc.time,
            )
        )
    }

    private fun bateria(): Int? =
        getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }

    // ------------------------------------------------------------------ rede

    /** Quando a internet volta, esvazia a fila na hora. */
    private fun registrarRede() {
        val cm = getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Tracking.sincronizarAgora()
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(cb) }.onSuccess { redeCallback = cb }
    }

    // ------------------------------------------------------------------ notificação

    private fun criarCanal() {
        PwNotificacoes.criarCanais(this)
        val canal = NotificationChannel(CANAL, "Status e GPS", NotificationManager.IMPORTANCE_LOW).apply {
            group = PwNotificacoes.GRUPO_SISTEMA
            description = "Aviso fixo enquanto o app envia sua localização"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)?.createNotificationChannel(canal)
    }

    private fun notificacao(modo: Modo, economia: Boolean): Notification {
        val abrir = PwNotificacoes.abrirApp(this, 0)
        val resumo = CorridaEmCurso.resumo.value
        if (modo == Modo.EM_CORRIDA && resumo != null) {
            return NotificacaoAoVivo.montar(this, resumo, abrir,
                extra = "GPS a cada ${Intervalos.corridaMs(economia) / 1000} s" + if (economia) " (economia)" else "")
        }
        val (titulo, texto) = when (modo) {
            Modo.EM_CORRIDA -> "Em corrida" to "Enviando sua posição a cada ${Intervalos.corridaMs(economia) / 1000} s" + if (economia) " (economia de bateria)" else ""
            Modo.DISPONIVEL -> "Disponível" to "Aguardando corridas… você será avisado assim que chegar uma."
            Modo.OFFLINE -> (com.pwexpress.core.marca.MarcaEmpresa.marca.value?.nomePara("notificacoes") ?: "Delibra") to "Encerrando…"
        }
        return PwNotificacoes.base(this, CANAL)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(abrir)
            .build()
    }

    private companion object {
        const val CANAL = "pwex_rastreamento"
        const val NOTIF_ID = 1001
    }
}
