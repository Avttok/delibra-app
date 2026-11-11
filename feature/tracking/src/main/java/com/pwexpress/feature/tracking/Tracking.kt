package com.pwexpress.feature.tracking

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.network.HttpResposta
import com.pwexpress.core.network.NaoAutenticadoException
import com.pwexpress.feature.tracking.data.FilaLocalizacoes
import com.pwexpress.feature.tracking.data.PontoGps
import com.pwexpress.feature.tracking.data.TrackingPrefs
import com.pwexpress.feature.tracking.service.TrackingService
import com.pwexpress.feature.tracking.ui.Permissoes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.util.UUID

/**
 * Controlador do rastreamento: estado, fila offline e sincronização com o servidor.
 * O TrackingService só coleta pontos; quem decide e envia é este objeto.
 */
object Tracking {

    private lateinit var app: Context
    private var versaoApp: String = ""
    internal lateinit var prefs: TrackingPrefs
        private set
    internal lateinit var fila: FilaLocalizacoes
        private set

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val envioMutex = Mutex()

    private val _info = MutableStateFlow(TrackingInfo())
    val info: StateFlow<TrackingInfo> = _info.asStateFlow()

    fun init(context: Context, versaoApp: String) {
        if (::app.isInitialized) return
        app = context.applicationContext
        this.versaoApp = versaoApp
        prefs = TrackingPrefs(app)
        fila = FilaLocalizacoes(app)
        _info.value = TrackingInfo(modo = prefs.modo, economia = prefs.economia, naFila = fila.total())
    }

    // ---------------------------------------------------------------- ações da tela

    /** Escolha manual do motoboy (Offline / Disponível). Bloqueada durante uma corrida. */
    fun definirModo(modo: Modo) {
        if (modo == prefs.modo) return
        if (prefs.modo == Modo.EM_CORRIDA || modo == Modo.EM_CORRIDA) return
        prefs.modo = modo
        prefs.sessaoId = if (modo == Modo.EM_CORRIDA) UUID.randomUUID().toString() else null
        prefs.statusPendente = true
        _info.update { it.copy(modo = modo, erro = null) }
        if (modo == Modo.OFFLINE) pararServico() else iniciarServico()
        sincronizarAgora()
    }

    /** Aplica os intervalos vindos do servidor (Configurações do gestor), em segundos/minutos/metros. */
    fun aplicarConfiguracoes(corridaS: Double?, economiaS: Double?, disponivelMin: Double?, disponivelM: Double?,
                             distanciaM: Double? = null, precisaoM: Double? = null, envioS: Double? = null,
                             inteligente: Boolean? = null, curvaGraus: Double? = null, retaMaxM: Double? = null) {
        var mudou = false
        inteligente?.let { Intervalos.inteligente = it }
        curvaGraus?.takeIf { it in 5.0..90.0 }?.let { Intervalos.curvaGraus = it.toFloat() }
        retaMaxM?.takeIf { it >= 20 }?.let { Intervalos.retaMaxM = it.toFloat() }
        distanciaM?.takeIf { it >= 0 }?.let { val v = it.toFloat(); if (v != Intervalos.corridaDistanciaM) { Intervalos.corridaDistanciaM = v; mudou = true } }
        precisaoM?.takeIf { it >= 10 }?.let { Intervalos.precisaoMaximaM = it.toFloat() }
        envioS?.takeIf { it >= 1 }?.let { Intervalos.envioLoteMs = (it * 1000).toLong() }
        corridaS?.takeIf { it >= 5 }?.let { val v = (it * 1000).toLong(); if (v != Intervalos.corridaNormalMs) { Intervalos.corridaNormalMs = v; mudou = true } }
        economiaS?.takeIf { it >= 10 }?.let { val v = (it * 1000).toLong(); if (v != Intervalos.corridaEconomiaMs) { Intervalos.corridaEconomiaMs = v; mudou = true } }
        disponivelMin?.takeIf { it >= 1 }?.let { Intervalos.disponivelEnvioMs = (it * 60_000).toLong() }
        disponivelM?.takeIf { it >= 50 }?.let { Intervalos.disponivelDistanciaM = it.toFloat() }
        if (mudou && prefs.modo != Modo.OFFLINE) iniciarServico() // reaplica o ritmo do GPS
        _info.update { it.copy() }
    }

    /** Chamado quando uma corrida é aceita: liga o GPS de alta precisão vinculado a ela. */
    fun entrarEmCorrida(corridaId: String) {
        if (prefs.modo == Modo.EM_CORRIDA && prefs.sessaoId == corridaId) return
        prefs.modo = Modo.EM_CORRIDA
        prefs.sessaoId = corridaId
        prefs.statusPendente = true
        _info.update { it.copy(modo = Modo.EM_CORRIDA, erro = null) }
        iniciarServico()
        sincronizarAgora()
    }

    /** Corrida concluída ou cancelada: volta a ficar disponível. */
    fun sairDaCorrida() {
        if (prefs.modo != Modo.EM_CORRIDA) return
        sincronizarAgora() // envia os últimos pontos ainda vinculados à corrida
        prefs.modo = Modo.DISPONIVEL
        prefs.sessaoId = null
        prefs.statusPendente = true
        _info.update { it.copy(modo = Modo.DISPONIVEL) }
        iniciarServico()
    }

    fun definirEconomia(ativo: Boolean) {
        if (ativo == prefs.economia) return
        prefs.economia = ativo
        prefs.statusPendente = true
        _info.update { it.copy(economia = ativo) }
        if (prefs.modo != Modo.OFFLINE) iniciarServico() // reaplica o intervalo
        sincronizarAgora()
    }

    /** Chamado quando o app volta à tela: garante que o serviço segue rodando. */
    fun retomarSeAtivo() {
        if (prefs.modo == Modo.OFFLINE) return
        if (!Permissoes.ler(app).localizacao) return
        iniciarServico()
        sincronizarAgora()
    }

    /** Fica offline, envia o que der, limpa a fila e encerra a sessão. */
    suspend fun sairDaConta() {
        Simulador.desligar()
        prefs.modo = Modo.OFFLINE
        prefs.sessaoId = null
        prefs.statusPendente = true
        pararServico()
        withTimeoutOrNull(8_000) { sincronizar() }
        fila.limpar()
        prefs.statusPendente = false
        _info.value = TrackingInfo(economia = prefs.economia)
        PwCore.auth.sair()
    }

    // ---------------------------------------------------------------- usado pelo serviço

    internal fun registrarPonto(p: PontoGps) {
        fila.inserir(p)
        _info.update {
            it.copy(ultimaLeitura = p.registradoEm, ultimaPrecisao = p.precisao, naFila = fila.total(), ultimaPosicao = p.lat to p.lng, ultimaVelocidadeMs = p.velocidade)
        }
        // em corrida, junta as posições e manda em lote (o tempo do lote vem dos Ajustes)
        if (p.status == Modo.EM_CORRIDA && System.currentTimeMillis() - ultimaSync < Intervalos.envioLoteMs && fila.total() < Intervalos.LOTE_MAXIMO) {
            if (loteAgendado?.isActive != true) loteAgendado = escopo.launch {
                kotlinx.coroutines.delay((Intervalos.envioLoteMs - (System.currentTimeMillis() - ultimaSync)).coerceAtLeast(500))
                sincronizar()
            }
            return
        }
        sincronizarAgora()
    }
    @Volatile private var ultimaSync = 0L
    private var loteAgendado: kotlinx.coroutines.Job? = null

    internal fun reportarErro(mensagem: String) {
        _info.update { it.copy(erro = mensagem) }
    }

    fun sincronizarAgora() {
        escopo.launch { sincronizar() }
    }

    // ---------------------------------------------------------------- envio

    private suspend fun sincronizar() = envioMutex.withLock {
        ultimaSync = System.currentTimeMillis()
        if (!PwConfig.configurado || PwCore.auth.sessao.value == null) return@withLock
        _info.update { it.copy(enviando = true) }
        try {
            if (prefs.statusPendente) {
                val args = buildJsonObject {
                    put("p_status", prefs.modo.api)
                    put("p_modo_economia", prefs.economia)
                    put("p_versao_app", versaoApp)
                }
                val r = PwCore.rpc.chamar("atualizar_status", args)
                if (r.ok) prefs.statusPendente = false else _info.update { it.copy(erro = erroLegivel(r)) }
            }

            while (true) {
                val lote = fila.proximos(Intervalos.LOTE_MAXIMO)
                if (lote.isEmpty()) break
                val args = buildJsonObject {
                    put("pontos", buildJsonArray { lote.forEach { add(it.second.toJson()) } })
                }
                val r = PwCore.rpc.chamar("registrar_localizacoes", args)
                when {
                    r.ok -> {
                        fila.remover(lote.map { it.first })
                        _info.update {
                            it.copy(ultimoEnvio = System.currentTimeMillis(), naFila = fila.total(), erro = null)
                        }
                    }
                    r.codigo == 400 -> {
                        // lote com dado inválido: descarta para não travar a fila para sempre
                        fila.remover(lote.map { it.first })
                        _info.update { it.copy(erro = "Lote descartado: ${erroLegivel(r)}") }
                    }
                    else -> {
                        _info.update { it.copy(erro = erroLegivel(r)) }
                        break
                    }
                }
            }
        } catch (e: NaoAutenticadoException) {
            _info.update { it.copy(erro = e.message) }
        } catch (e: IOException) {
            _info.update { it.copy(erro = "Sem internet: pontos guardados no aparelho.") }
        } catch (e: Exception) {
            _info.update { it.copy(erro = "Falha no envio: ${e.message}") }
        } finally {
            _info.update { it.copy(enviando = false, naFila = fila.total()) }
        }
    }

    private fun erroLegivel(r: HttpResposta): String {
        val msg = runCatching {
            Json.parseToJsonElement(r.corpo).jsonObject["message"]?.jsonPrimitive?.content
        }.getOrNull()
        return when (msg) {
            "usuario_nao_e_motoboy" -> "Este usuário não tem o papel de motoboy."
            null, "" -> "Servidor respondeu ${r.codigo}."
            else -> "Servidor: $msg (${r.codigo})"
        }
    }

    // ---------------------------------------------------------------- serviço

    private fun iniciarServico() {
        try {
            ContextCompat.startForegroundService(app, Intent(app, TrackingService::class.java))
        } catch (e: Exception) {
            reportarErro("Não foi possível iniciar o rastreamento: ${e.message}")
        }
    }

    private fun pararServico() {
        app.stopService(Intent(app, TrackingService::class.java))
    }
}
