package com.pwexpress.feature.tracking

/** Estados do motoboy. O valor `api` é o mesmo do enum status_motoboy no banco. */
enum class Modo(val api: String, val rotulo: String) {
    OFFLINE("offline", "Offline"),
    DISPONIVEL("disponivel", "Disponível"),
    EM_CORRIDA("em_corrida", "Em corrida");

    companion object {
        fun de(api: String?): Modo = entries.firstOrNull { it.api == api } ?: OFFLINE
    }
}

/**
 * Ritmo do rastreamento. Valores padrão; o servidor pode mudar pela tela de Configurações
 * do gestor (aplicado em [Tracking.aplicarConfiguracoes]).
 */
object Intervalos {
    @Volatile var corridaNormalMs = 45_000L
    @Volatile var corridaEconomiaMs = 90_000L

    /** Disponível: lê a cada 60 s, mas só envia a cada X min OU se andou Y m. */
    const val DISPONIVEL_LEITURA_MS = 60_000L
    @Volatile var disponivelEnvioMs = 180_000L
    @Volatile var disponivelDistanciaM = 300f

    /** Em corrida, descarta leituras com erro maior que isso (evita "saltos" no mapa). Ajustes → ping_precisao_max_m. */
    @Volatile var precisaoMaximaM = 60f
    /** Teto absoluto: sem leitura boa há muito tempo, aceita até isso para não sumir do mapa. */
    const val PRECISAO_MAXIMA_M = 150f
    /** Em corrida, manda antes do tempo se andou isso (0 = só pelo tempo). Ajustes → ping_distancia_m. */
    @Volatile var corridaDistanciaM = 20f
    /** Em corrida, as posições vão em lote a cada X (economiza bateria). Ajustes → ping_envio_s. */
    @Volatile var envioLoteMs = 12_000L

    /**
     * Ping inteligente (Ajustes → ping_inteligente): em linha reta só manda a cada [retaMaxM];
     * em curva (mudou mais de [curvaGraus]) manda já. O traçado fica igual com bem menos pontos
     * (menos bateria, dados e espaço). O tempo máximo entre pings continua valendo.
     */
    @Volatile var inteligente = true
    @Volatile var curvaGraus = 25f
    @Volatile var retaMaxM = 200f

    const val LOTE_MAXIMO = 200

    fun corridaMs(economia: Boolean) = if (economia) corridaEconomiaMs else corridaNormalMs
}

/** Estado exibido na tela. */
data class TrackingInfo(
    val modo: Modo = Modo.OFFLINE,
    val economia: Boolean = false,
    val ultimaLeitura: Long? = null,
    val ultimaPrecisao: Float? = null,
    val ultimoEnvio: Long? = null,
    val naFila: Int = 0,
    val enviando: Boolean = false,
    val erro: String? = null,
    /** Última posição lida (para o mapa do raio). */
    val ultimaPosicao: Pair<Double, Double>? = null,
)
