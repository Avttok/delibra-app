package com.pwexpress.feature.corridas.data

import com.pwexpress.core.tentar

import android.content.Context
import androidx.core.content.edit
import com.google.firebase.messaging.FirebaseMessaging
import com.pwexpress.core.PwCore
import com.pwexpress.core.network.HttpResposta
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Estado das corridas no app: oferta pendente e corrida ativa.
 * Não conhece o rastreamento: o módulo :app liga uma coisa à outra.
 */
object Corridas {

    private lateinit var app: Context
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _oferta = MutableStateFlow<Oferta?>(null)
    val oferta: StateFlow<Oferta?> = _oferta.asStateFlow()

    private val _ativa = MutableStateFlow<Corrida?>(null)
    val ativa: StateFlow<Corrida?> = _ativa.asStateFlow()

    /** true depois da primeira consulta bem-sucedida ao servidor. */
    private val _carregou = MutableStateFlow(false)
    val carregou: StateFlow<Boolean> = _carregou.asStateFlow()

    fun init(context: Context) {
        if (::app.isInitialized) return
        app = context.applicationContext
    }

    private val prefs get() = app.getSharedPreferences("pwex_corridas", Context.MODE_PRIVATE)

    // ------------------------------------------------------------------ sincronização

    /** Busca oferta pendente e corrida ativa no servidor. */
    fun sincronizar() {
        escopo.launch { sincronizarAgora() }
    }

    private var avisadaSumiu: String? = null

    suspend fun sincronizarAgora(): Unit = mutex.withLock {
        if (PwCore.auth.sessao.value == null) return@withLock
        tentar {
            val a = PwCore.rpc.chamar("minha_corrida_ativa", JsonObject(emptyMap()))
            if (a.ok) {
                val antes = _ativa.value
                val agora = a.json()?.paraCorrida()
                _ativa.value = agora
                _carregou.value = true
                // a corrida sumiu sem você concluir: cancelada ou tirada de você (avisa mesmo se o push não chegou)
                if (antes != null && agora == null && antes.status != "concluida" && avisadaSumiu != antes.id) {
                    avisadaSumiu = antes.id
                    tentar {
                        com.pwexpress.feature.corridas.push.Alarme.pararOferta(app)
                        com.pwexpress.feature.corridas.push.Alarme.avisar(app, "Corrida ${antes.numeroExibicao} encerrada", "O gestor cancelou ou tirou a corrida de você.",
                            "A corrida ${antes.numeroExibicao} não está mais com você (cancelada ou passada a outro motoboy). Pode parar; nenhuma etapa a mais é necessária.")
                    }
                }
            }
            val o = PwCore.rpc.chamar("oferta_pendente", JsonObject(emptyMap()))
            if (o.ok) _oferta.value = o.json()?.paraOferta()
            enviarTokenPendente()
        }
        Unit
    }

    fun limpar() {
        _oferta.value = null
        _ativa.value = null
        _carregou.value = false
        _planejadas.value = emptyList()
    }

    // ------------------------------------------------------------------ planejadas (hora marcada)

    private val _planejadas = MutableStateFlow<List<Corrida>>(emptyList())
    /** Livres para reservar (do meu modo) + as que eu reservei, por horário. */
    val planejadas: StateFlow<List<Corrida>> = _planejadas.asStateFlow()

    suspend fun carregarPlanejadas(): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("corridas_planejadas", JsonObject(emptyMap()))
        if (!r.ok) throw Exception(traduzir(r))
        _planejadas.value = (r.json() as? kotlinx.serialization.json.JsonArray)?.map { it.paraCorrida() }.orEmpty()
            .sortedBy { it.agendadaPara ?: Long.MAX_VALUE }
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    // ------------------------------------------------------------------ quadro de rotas (express e planejadas, mesmo peso)

    private val _rotas = MutableStateFlow<List<Corrida>>(emptyList())
    /** Rotas que eu posso pegar agora: a sugerida para mim primeiro. */
    val rotas: StateFlow<List<Corrida>> = _rotas.asStateFlow()

    suspend fun carregarRotas(): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("rotas_disponiveis", JsonObject(emptyMap()))
        if (!r.ok) throw Exception(traduzir(r))
        _rotas.value = (r.json() as? kotlinx.serialization.json.JsonArray)?.map { it.paraCorrida() }.orEmpty()
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Pega a rota do quadro: express já vira a corrida ativa; planejada fica reservada para mim. */
    suspend fun pegarRota(corridaId: String): Result<Boolean> = tentar {
        val r = PwCore.rpc.chamar("pegar_rota", buildJsonObject { put("p_corrida", corridaId) })
        if (!r.ok) throw Exception(traduzir(r))
        val c = r.json()?.paraCorrida()
        val ativa = c != null && (c.status == "aceita" || c.status == "em_andamento")
        if (ativa) _ativa.value = c
        _rotas.value = _rotas.value.filterNot { it.id == corridaId }
        carregarPlanejadas()
        ativa
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    // ------------------------------------------------------------------ holerite

    suspend fun meusHolerites(): Result<Pair<List<PeriodoHolerite>, Boolean>> = tentar {
        val r = PwCore.rpc.chamar("meus_holerites", buildJsonObject { put("p_quantos", 12) })
        if (!r.ok) throw Exception(traduzir(r))
        val o = r.json()?.jsonObject ?: throw Exception("Sem dados.")
        val lista = (o["periodos"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? JsonObject)?.paraPeriodo() }.orEmpty()
        lista to ((o["valores_visiveis"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true")
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    suspend fun meuHolerite(inicio: String, fim: String): Result<Holerite> = tentar {
        val r = PwCore.rpc.chamar("meu_holerite", buildJsonObject { put("p_inicio", inicio); put("p_fim", fim) })
        if (!r.ok) throw Exception(traduzir(r))
        (r.json()?.jsonObject ?: throw Exception("Sem dados.")).paraHolerite()
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    // ------------------------------------------------------------------ histórico e carteira

    private val _historico = MutableStateFlow<List<CorridaFeita>?>(null)
    /** Corridas que eu fiz (concluídas e canceladas), do modo atual. null = ainda não carregou. */
    val historico: StateFlow<List<CorridaFeita>?> = _historico.asStateFlow()

    suspend fun carregarHistorico(dias: Int = 120): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("minhas_corridas", buildJsonObject { put("p_dias", dias) })
        if (!r.ok) throw Exception(traduzir(r))
        _historico.value = (r.json() as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? JsonObject)?.paraCorridaFeita() }.orEmpty()
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    suspend fun reservar(corridaId: String): Result<Unit> = acaoPlanejada("reservar_corrida", buildJsonObject { put("p_corrida", corridaId) })
    suspend fun cancelarReserva(corridaId: String, motivo: String?): Result<Unit> =
        acaoPlanejada("cancelar_reserva", buildJsonObject { put("p_corrida", corridaId); motivo?.let { put("p_motivo", it) } })

    /** Começa a corrida reservada: vira a corrida ativa. */
    suspend fun iniciarPlanejada(corridaId: String): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("iniciar_planejada", buildJsonObject { put("p_corrida", corridaId) })
        if (!r.ok) throw Exception(traduzir(r))
        _ativa.value = r.json()?.paraCorrida()
        carregarPlanejadas()
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    private suspend fun acaoPlanejada(funcao: String, args: JsonObject): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar(funcao, args)
        if (!r.ok) throw Exception(traduzir(r))
        carregarPlanejadas()
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Resposta vinda da notificação (o app pode nem estar aberto). */
    suspend fun responderPorId(ofertaId: String, aceitar: Boolean): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("responder_oferta", buildJsonObject { put("p_oferta", ofertaId); put("p_aceitar", aceitar) })
        if (_oferta.value?.id == ofertaId) _oferta.value = null
        if (!r.ok) throw Exception(traduzir(r))
        if (aceitar) aceita(r.json()?.paraCorrida())
        Unit
    }

    /** Aceitou: corrida agora vira a ativa; agendada vira reserva (começa no horário). */
    private suspend fun aceita(c: Corrida?) {
        if (c?.status == "reservada") {
            com.pwexpress.core.designsystem.PwToast.mostrar("Reservada para ${c.agendadaPara?.let { com.pwexpress.feature.corridas.ui.quandoTexto(it) } ?: "o horário marcado"}. Ela fica em Rotas → Minhas reservas.",
                com.pwexpress.core.designsystem.TipoAviso.OK, "Corrida reservada")
            tentar { carregarPlanejadas() }
        } else _ativa.value = c
    }

    // ------------------------------------------------------------------ oferta

    suspend fun responder(aceitar: Boolean): Result<Unit> {
        val oferta = _oferta.value ?: return Result.failure(Exception("Essa corrida não está mais disponível."))
        return tentar {
            val r = PwCore.rpc.chamar("responder_oferta", buildJsonObject {
                put("p_oferta", oferta.id)
                put("p_aceitar", aceitar)
            })
            _oferta.value = null
            if (!r.ok) throw Exception(traduzir(r))
            if (aceitar) aceita(r.json()?.paraCorrida())
            Unit
        }.onFailure { if (it is java.io.IOException) _oferta.value = oferta }
            .recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet. Tente de novo.") else e }
    }

    /** O prazo da oferta acabou no celular. */
    fun descartarOferta(id: String) {
        if (_oferta.value?.id == id) _oferta.value = null
    }

    // ------------------------------------------------------------------ etapas

    /**
     * Avança a etapa. Envia junto a posição atual (o servidor confere se o motoboy está no local).
     * [ignorarLocal] só funciona em corrida de teste e para contas gestor + motoboy.
     */
    suspend fun avancar(
        paradaId: String,
        acao: String,
        dados: JsonObject = JsonObject(emptyMap()),
        posicao: Posicao? = null,
        ignorarLocal: Boolean = false,
    ): Result<Unit> =
        tentar {
            val completo = buildJsonObject {
                dados.forEach { (k, v) -> put(k, v) }
                posicao?.let {
                    put("lat", it.lat)
                    put("lng", it.lng)
                    put("precisao_m", it.precisaoM)
                }
                if (ignorarLocal) put("ignorar_local", true)
            }
            val r = PwCore.rpc.chamar("avancar_parada", buildJsonObject {
                put("p_parada", paradaId)
                put("p_acao", acao)
                put("p_dados", completo)
            })
            if (!r.ok) throw Exception(traduzir(r))
            _ativa.value = r.json()?.paraCorrida() // null = corrida concluída
            Unit
        }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet. Tente de novo.") else e }

    /** Envia foto/assinatura e devolve o caminho no Storage. */
    suspend fun enviarComprovante(corridaId: String, paradaId: String, nome: String, bytes: ByteArray, tipo: String): Result<String> =
        tentar {
            val caminho = "$corridaId/$paradaId-$nome-${System.currentTimeMillis()}.${if (tipo == "image/png") "png" else "jpg"}"
            val r = PwCore.rpc.enviarArquivo("comprovantes", caminho, bytes, tipo)
            if (!r.ok) throw Exception("Falha ao enviar $nome (${r.codigo}).")
            caminho
        }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet para enviar o comprovante.") else e }

    // ------------------------------------------------------------------ token do Firebase

    fun atualizarTokenPush() {
        tentar {
            runCatching { FirebaseMessaging.getInstance().token.addOnSuccessListener { registrarToken(it) } }
        }
    }

    fun registrarToken(token: String) {
        prefs.edit { putString("token_pendente", token) }
        escopo.launch { enviarTokenPendente() }
    }

    private suspend fun enviarTokenPendente() {
        val token = prefs.getString("token_pendente", null) ?: return
        if (PwCore.auth.sessao.value == null) return
        tentar {
            val r = PwCore.rpc.chamar("registrar_fcm_token", buildJsonObject { put("p_token", token) })
            if (r.ok) prefs.edit { remove("token_pendente") }
        }
    }

    /** Ao entrar com outro usuário, o token precisa ir para o novo motoboy. */
    fun aoEntrar() {
        atualizarTokenPush()
        sincronizar()
    }

    // ------------------------------------------------------------------ util

    private fun HttpResposta.json() =
        corpo.takeIf { it.isNotBlank() && it != "null" }?.let { Json.parseToJsonElement(it) }?.takeIf { it !is JsonNull }

    private val mensagens = mapOf(
        "oferta_expirou" to "O tempo da oferta acabou.",
        "oferta_nao_encontrada" to "Essa oferta não está mais disponível.",
        "voce_ja_tem_corrida_ativa" to "Você já tem uma corrida em andamento.",
        "conclua_a_parada_anterior" to "Conclua a parada anterior primeiro.",
        "corrida_nao_esta_ativa" to "Essa corrida não está mais ativa (pode ter sido cancelada).",
        "informe_o_nome_de_quem_recebeu" to "Informe o nome de quem recebeu.",
        "codigo_incorreto" to "Código de entrega incorreto.",
        "falta_a_foto" to "Tire a foto do comprovante.",
        "falta_a_assinatura" to "Colete a assinatura.",
        "sem_localizacao" to "Não consegui sua localização. Ative o GPS e tente de novo.",
        "corrida_ja_reservada" to "Outro motoboy reservou essa corrida antes.",
        "modo_teste_diferente" to "Essa corrida é de outro modo (teste/real).",
        "horario_passou" to "O horário dessa corrida já passou.",
        "limite_de_reservas" to "Você já tem o máximo de reservas permitido.",
        "conflito_de_horario" to "Você já tem uma reserva nesse horário.",
        "reserva_nao_encontrada" to "Essa reserva não é mais sua (o gestor pode ter mudado).",
        "prazo_de_cancelamento_passou" to "O prazo para desistir já passou. Fale com o gestor.",
        "ainda_nao_pode_iniciar" to "Ainda é cedo para iniciar essa corrida.",
        "apenas_motoboy" to "Só motoboys podem fazer isso.",
        "rota_ja_pegada" to "Outro motoboy pegou essa rota antes.",
    )

    private fun traduzir(r: HttpResposta): String {
        val msg = tentar { Json.parseToJsonElement(r.corpo).jsonObject["message"]?.jsonPrimitive?.content }.getOrNull()
        if (msg != null && msg.startsWith("longe_do_local")) {
            val partes = msg.split(":")
            val dist = partes.getOrNull(1)
            val raio = partes.getOrNull(2)
            return "Você está a ${dist ?: "?"} m do endereço. Aproxime-se (até ${raio ?: "?"} m) para continuar."
        }
        return mensagens[msg] ?: msg ?: "Erro no servidor (${r.codigo})."
    }
}
