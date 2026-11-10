package com.pwexpress.feature.tracking

import com.pwexpress.feature.tracking.data.PontoGps
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Estado do simulador (modo teste). */
data class EstadoSimulador(
    val ativo: Boolean = false,
    val andando: Boolean = false,
    val posicao: Pair<Double, Double>? = null,
    val kmh: Double = 35.0,
    val envioS: Int = 5,
    /** 0..1 do caminho atual. */
    val progresso: Float = 0f,
    val restanteM: Int = 0,
    val enviados: Int = 0,
)

/**
 * GPS de mentira para o MODO TESTE: anda sozinho por um caminho (a rota da corrida ou até um ponto
 * tocado no mapa) na velocidade escolhida e manda os pings como se fossem do celular. Assim dá para
 * ver o rastreamento suave, sem "pulos", e testar chegadas e etapas sem sair de casa.
 * Enquanto ligado, o GPS de verdade é ignorado.
 */
object Simulador {
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var job: Job? = null
    private var caminho: List<Pair<Double, Double>> = emptyList()

    private val _estado = MutableStateFlow(EstadoSimulador())
    val estado: StateFlow<EstadoSimulador> = _estado.asStateFlow()
    val ativo: Boolean get() = _estado.value.ativo

    fun ligar(inicio: Pair<Double, Double>?, kmh: Double = _estado.value.kmh) {
        _estado.update { it.copy(ativo = true, posicao = inicio ?: it.posicao ?: Tracking.info.value.ultimaPosicao, kmh = kmh) }
        _estado.value.posicao?.let { enviar(it, 0f, null) }
    }

    fun desligar() {
        job?.cancel()
        caminho = emptyList()
        _estado.value = EstadoSimulador(kmh = _estado.value.kmh, envioS = _estado.value.envioS)
    }

    fun velocidade(kmh: Double) = _estado.update { it.copy(kmh = kmh.coerceIn(5.0, 120.0)) }
    fun ritmo(segundos: Int) = _estado.update { it.copy(envioS = segundos.coerceIn(2, 60)) }

    /** Pula para o ponto (sem andar). */
    fun teletransportar(lat: Double, lng: Double) {
        if (!ativo) ligar(lat to lng) else { parar(); _estado.update { it.copy(posicao = lat to lng) }; enviar(lat to lng, 0f, null) }
    }

    fun parar() {
        job?.cancel()
        _estado.update { it.copy(andando = false) }
    }

    /** Anda pelos pontos (o primeiro é a partida; se não houver posição, começa nele). */
    fun andar(pontos: List<Pair<Double, Double>>) {
        if (pontos.isEmpty()) return
        if (!ativo) ligar(pontos.first())
        val inicio = _estado.value.posicao ?: pontos.first()
        caminho = listOf(inicio) + pontos
        job?.cancel()
        job = escopo.launch {
            val total = comprimento(caminho)
            var andado = 0.0
            var ultimoEnvio = 0L
            var seg = 0
            var noSeg = 0.0
            _estado.update { it.copy(andando = true, progresso = 0f, restanteM = total.toInt()) }
            var anterior = inicio
            while (isActive && seg < caminho.size - 1) {
                delay(1000)
                var passo = _estado.value.kmh / 3.6 // metros neste segundo
                var pos = anterior
                while (passo > 0 && seg < caminho.size - 1) {
                    val a = caminho[seg]; val b = caminho[seg + 1]
                    val lado = metros(a, b)
                    val falta = lado - noSeg
                    if (passo < falta) { noSeg += passo; andado += passo; passo = 0.0; pos = interpolar(a, b, if (lado > 0) noSeg / lado else 1.0) }
                    else { passo -= falta; andado += falta; seg++; noSeg = 0.0; pos = b }
                }
                val rumo = rumo(anterior, pos)
                anterior = pos
                val agora = System.currentTimeMillis()
                val fim = seg >= caminho.size - 1
                _estado.update { it.copy(posicao = pos, progresso = if (total > 0) (andado / total).toFloat() else 1f, restanteM = (total - andado).toInt().coerceAtLeast(0)) }
                if (fim || agora - ultimoEnvio >= _estado.value.envioS * 1000L) {
                    ultimoEnvio = agora
                    enviar(pos, (_estado.value.kmh / 3.6).toFloat(), rumo)
                }
            }
            _estado.update { it.copy(andando = false) }
        }
    }

    private fun enviar(p: Pair<Double, Double>, velocidade: Float, rumo: Float?) {
        val modo = Tracking.prefs.modo
        if (modo == Modo.OFFLINE) return
        Tracking.registrarPonto(
            PontoGps(
                idLocal = UUID.randomUUID().toString(), sessaoId = Tracking.prefs.sessaoId, status = modo,
                lat = p.first, lng = p.second, precisao = 5f, velocidade = velocidade, direcao = rumo, altitude = null, bateria = null,
                registradoEm = System.currentTimeMillis(),
            ),
        )
        _estado.update { it.copy(enviados = it.enviados + 1) }
    }

    private fun comprimento(l: List<Pair<Double, Double>>) = l.zipWithNext { a, b -> metros(a, b) }.sum()

    private fun interpolar(a: Pair<Double, Double>, b: Pair<Double, Double>, t: Double) =
        (a.first + (b.first - a.first) * t) to (a.second + (b.second - a.second) * t)

    private fun metros(a: Pair<Double, Double>, b: Pair<Double, Double>): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(b.first - a.first); val dLng = Math.toRadians(b.second - a.second)
        val h = sin(dLat / 2).let { it * it } + cos(Math.toRadians(a.first)) * cos(Math.toRadians(b.first)) * sin(dLng / 2).let { it * it }
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun rumo(a: Pair<Double, Double>, b: Pair<Double, Double>): Float {
        val y = sin(Math.toRadians(b.second - a.second)) * cos(Math.toRadians(b.first))
        val x = cos(Math.toRadians(a.first)) * sin(Math.toRadians(b.first)) - sin(Math.toRadians(a.first)) * cos(Math.toRadians(b.first)) * cos(Math.toRadians(b.second - a.second))
        return ((Math.toDegrees(atan2(y, x)) + 360) % 360).toFloat()
    }
}
