package com.pwexpress.feature.corridas.data

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

enum class TipoParada { COLETA, ENTREGA }
enum class StatusParada { PENDENTE, A_CAMINHO, CHEGOU, CONCLUIDA }

data class Parada(
    val id: String,
    val ordem: Int,
    val tipo: TipoParada,
    val endereco: String,
    val lat: Double,
    val lng: Double,
    val contatoNome: String?,
    val contatoTelefone: String?,
    val instrucoes: String?,
    val status: StatusParada,
    /** Até 3 pontos de referência perto do endereço (definidos pelo gestor). */
    val referencias: List<Referencia> = emptyList(),
    /** Rotas automáticas: de qual cliente/pedido é esta parada e o que levar. */
    val cliente: String? = null,
    val pedidoNumero: Long? = null,
    val itens: List<ItemParada> = emptyList(),
    /** O que esta parada exige (o cliente pode pedir por ponto). null = servidor antigo: vale o da corrida nas entregas. */
    val exige: Exigencias? = null,
    /** Horário em que o endereço atende (ex.: 09:00 às 12:00). */
    val janelaDe: String? = null,
    val janelaAte: String? = null,
)

data class Exigencias(val foto: Boolean, val nome: Boolean, val assinatura: Boolean, val codigo: Boolean) {
    val alguma get() = foto || nome || assinatura || codigo
}

/** O que vale de fato nesta parada. */
fun Corrida.exigencias(p: Parada): Exigencias = p.exige
    ?: if (p.tipo == TipoParada.ENTREGA) Exigencias(exigeFoto, exigeNome, exigeAssinatura, exigeCodigo) else Exigencias(false, false, false, false)

data class ItemParada(
    val descricao: String, val quantidade: Int, val categoria: String?, val perigoso: Boolean, val fragil: Boolean,
    val icone: String? = null, val pesoKg: Double? = null, val medidas: String? = null, val valorDeclarado: Double? = null, val observacoes: String? = null,
    /** Não está preso a este ponto: vai para todas as entregas (ou sai de qualquer coleta). */
    val todas: Boolean = false,
)

data class Referencia(val nome: String, val distanciaM: Int?, val tipo: String?, val relacao: String? = null)

data class Corrida(
    val id: String,
    val numero: Long,
    val status: String,
    val teste: Boolean,
    /** O que o motoboy recebe (nunca o total nem o percentual). */
    val valorMotoboy: Double?,
    val exigirNoLocal: Boolean,
    val raioChegadaM: Int,
    val rotaPlanejada: String?,
    val distanciaM: Int?,
    val duracaoS: Int?,
    val observacoes: String?,
    val exigeFoto: Boolean,
    val exigeAssinatura: Boolean,
    val exigeNome: Boolean,
    val exigeCodigo: Boolean,
    val empresaNome: String?,
    val paradas: List<Parada>,
    // corridas planejadas (hora marcada)
    val agendadaPara: Long? = null,
    val cancelarReservaAte: Long? = null,
    val iniciarAPartirDe: Long? = null,
    val minha: Boolean = false,
    /** O gestor deixa o motoboy ver quanto a rota paga? */
    val valoresVisiveis: Boolean = true,
    val urgente: Boolean = false,
    val perigoso: Boolean = false,
    val entregas: Int = 0,
    val coletas: Int = 0,
    /** Quadro de rotas: o sistema acha que esta é a melhor para mim. */
    val sugerida: Boolean = false,
    val distanciaAteColetaM: Int? = null,
    val noQuadroDesde: Long? = null,
) {
    /** Reais "#12", de teste "T12" (sequências separadas). */
    val numeroExibicao: String get() = if (teste) "T$numero" else "#$numero"
    val planejada: Boolean get() = status == "agendada" || status == "reservada" || agendadaPara != null
    /** Clientes diferentes nesta rota. */
    val clientes: List<String> get() = paradas.mapNotNull { it.cliente }.distinct().ifEmpty { listOfNotNull(empresaNome) }

    /** Parada em que o motoboy está agora (primeira não concluída). */
    val paradaAtual: Parada? get() = paradas.firstOrNull { it.status != StatusParada.CONCLUIDA }
}

data class Oferta(
    val id: String,
    val prazoMillis: Long, // relógio do celular (evita diferença de horário com o servidor)
    val distanciaM: Int?,
    val corrida: Corrida,
    /** Extensão: as paradas entram no fim da rota que você já está fazendo. */
    val extensao: Boolean = false,
    val rotaAtualNumero: Long? = null,
    val rotaAtualRestantes: Int? = null,
)

// ------------------------------------------------------------------ JSON → modelos

private fun JsonObject.texto(k: String): String? =
    this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.content
private fun JsonObject.inteiro(k: String): Int? = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull
private fun JsonObject.decimal(k: String): Double? = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.doubleOrNull
private fun JsonObject.logico(k: String): Boolean = this[k]?.takeIf { it !is JsonNull }?.jsonPrimitive?.booleanOrNull ?: false

internal fun JsonElement.paraCorrida(): Corrida {
    val o = jsonObject
    return Corrida(
        id = o.texto("id")!!,
        numero = o.texto("numero")?.toLongOrNull() ?: 0,
        status = o.texto("status").orEmpty(),
        teste = o.logico("teste"),
        valorMotoboy = o.decimal("valor_motoboy"),
        exigirNoLocal = o.logico("exigir_no_local"),
        raioChegadaM = o.inteiro("raio_chegada_m") ?: 150,
        rotaPlanejada = o.texto("rota_planejada"),
        distanciaM = o.inteiro("distancia_planejada_m"),
        duracaoS = o.inteiro("duracao_planejada_s"),
        observacoes = o.texto("observacoes"),
        exigeFoto = o.logico("exige_foto"),
        exigeAssinatura = o.logico("exige_assinatura"),
        exigeNome = o.logico("exige_nome"),
        exigeCodigo = o.logico("exige_codigo"),
        empresaNome = o.texto("empresa_nome"),
        paradas = o["paradas"]?.jsonArray?.map { it.paraParada() }.orEmpty().sortedBy { it.ordem },
        agendadaPara = o.texto("agendada_para")?.let { instante(it) },
        cancelarReservaAte = o.texto("cancelar_reserva_ate")?.let { instante(it) },
        iniciarAPartirDe = o.texto("iniciar_a_partir_de")?.let { instante(it) },
        minha = o.logico("minha"),
        valoresVisiveis = o["valores_visiveis"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.booleanOrNull ?: true,
        urgente = o.logico("urgente"),
        perigoso = o.logico("perigoso"),
        entregas = o.inteiro("entregas") ?: 0,
        coletas = o.inteiro("coletas") ?: 0,
        sugerida = o.logico("sugerida"),
        distanciaAteColetaM = o.inteiro("distancia_ate_coleta_m"),
        noQuadroDesde = o.texto("no_quadro_desde")?.let { instante(it) },
    ).let { c -> if (c.entregas == 0) c.copy(entregas = c.paradas.count { it.tipo == TipoParada.ENTREGA }, coletas = c.paradas.count { it.tipo == TipoParada.COLETA }) else c }
}

private fun JsonElement.paraParada(): Parada {
    val o = jsonObject
    return Parada(
        id = o.texto("id")!!,
        ordem = o.inteiro("ordem") ?: 0,
        tipo = if (o.texto("tipo") == "entrega") TipoParada.ENTREGA else TipoParada.COLETA,
        endereco = o.texto("endereco").orEmpty(),
        lat = o.decimal("lat") ?: 0.0,
        lng = o.decimal("lng") ?: 0.0,
        contatoNome = o.texto("contato_nome"),
        contatoTelefone = o.texto("contato_telefone"),
        instrucoes = o.texto("instrucoes"),
        status = when (o.texto("status")) {
            "a_caminho" -> StatusParada.A_CAMINHO
            "chegou" -> StatusParada.CHEGOU
            "concluida" -> StatusParada.CONCLUIDA
            else -> StatusParada.PENDENTE
        },
        referencias = (o["referencias"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e ->
            val r = e as? JsonObject ?: return@mapNotNull null
            Referencia(r.texto("nome") ?: return@mapNotNull null, r.inteiro("distancia_m"), r.texto("tipo"), r.texto("relacao"))
        }.orEmpty(),
        cliente = o.texto("cliente"),
        pedidoNumero = o.texto("pedido_numero")?.toLongOrNull(),
        itens = (o["itens"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e ->
            val i = e as? JsonObject ?: return@mapNotNull null
            val c = i.decimal("comprimento_cm")
            fun n(v: Double?) = v?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString().replace('.', ',') } ?: "?"
            ItemParada(i.texto("descricao") ?: return@mapNotNull null, i.inteiro("quantidade") ?: 1, i.texto("categoria"), i.logico("perigoso"), i.logico("fragil"),
                icone = i.texto("icone"), pesoKg = i.decimal("peso_kg"), medidas = c?.let { "${n(it)}×${n(i.decimal("largura_cm"))}×${n(i.decimal("altura_cm"))} cm" },
                valorDeclarado = i.decimal("valor_declarado"), observacoes = i.texto("observacoes"), todas = i.logico("todas"))
        }.orEmpty(),
        exige = (o["exige"] as? JsonObject)?.let { e -> Exigencias(e.logico("foto"), e.logico("nome"), e.logico("assinatura"), e.logico("codigo")) },
        janelaDe = o.texto("janela_de"),
        janelaAte = o.texto("janela_ate"),
    )
}

internal fun JsonElement.paraOferta(): Oferta {
    val o = jsonObject
    val segundos = o.inteiro("segundos_restantes") ?: 0
    return Oferta(
        id = o.texto("oferta_id")!!,
        prazoMillis = System.currentTimeMillis() + segundos * 1000L,
        distanciaM = o.inteiro("distancia_m"),
        corrida = o.getValue("corrida").paraCorrida(),
        extensao = o.logico("extensao"),
        rotaAtualNumero = (o["rota_atual"] as? JsonObject)?.texto("numero")?.toLongOrNull(),
        rotaAtualRestantes = (o["rota_atual"] as? JsonObject)?.inteiro("paradas_restantes"),
    )
}

/** Data/hora do servidor (ISO 8601, com fuso) em milissegundos. */
internal fun instante(iso: String): Long? = runCatching { java.time.OffsetDateTime.parse(iso).toInstant().toEpochMilli() }
    .recoverCatching { java.time.OffsetDateTime.parse(iso.replace(" ", "T")).toInstant().toEpochMilli() }
    .getOrNull()


/** Uma corrida que o motoboy fez (histórico e carteira). Sem percentuais: só o que ele recebeu. */
data class CorridaFeita(
    val id: String, val numero: Long?, val teste: Boolean, val status: String, val valor: Double?, val distanciaM: Int?,
    val quando: Long?, val aceitaEm: Long?, val concluidaEm: Long?, val empresa: String?, val coleta: String?, val entrega: String?, val paradas: Int,
    val entregas: Int = 0, val planejada: Boolean = false,
) {
    val concluida get() = status == "concluida"
    val numeroExibicao: String get() = numero?.let { if (teste) "T$it" else "#$it" } ?: "—"
}

internal fun JsonObject.paraCorridaFeita(): CorridaFeita? {
    fun s(k: String) = (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    fun t(k: String) = s(k)?.let { instante(it) }
    val ps = (this["paradas"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
    fun end(o: JsonObject) = (o["endereco"] as? kotlinx.serialization.json.JsonPrimitive)?.content
    fun tipo(o: JsonObject) = (o["tipo"] as? kotlinx.serialization.json.JsonPrimitive)?.content
    return CorridaFeita(
        id = s("id") ?: return null, numero = s("numero")?.toLongOrNull(), teste = s("teste") == "true", status = s("status") ?: "",
        valor = s("valor_motoboy")?.toDoubleOrNull(), distanciaM = s("distancia_m")?.toDoubleOrNull()?.toInt(),
        quando = t("quando"), aceitaEm = t("aceita_em"), concluidaEm = t("concluida_em"), empresa = s("empresa_nome"),
        coleta = ps.firstOrNull { tipo(it) == "coleta" }?.let(::end), entrega = ps.lastOrNull { tipo(it) == "entrega" }?.let(::end), paradas = ps.size,
        entregas = s("entregas")?.toIntOrNull() ?: ps.count { tipo(it) == "entrega" }, planejada = s("planejada") == "true",
    )
}


// ------------------------------------------------------------------ holerite

/** Um período de pagamento (semana, quinzena…). `total` só vem quando o gestor deixa o motoboy ver. */
data class PeriodoHolerite(val inicio: String, val fim: String, val status: String, val rotas: Int, val entregas: Int, val km: Double, val total: Double?, val pagoEm: Long?)

data class RotaHolerite(val id: String, val numero: Long, val concluidaEm: Long?, val entregas: Int, val km: Double, val clientes: String?, val valor: Double?)

data class Holerite(
    val inicio: String, val fim: String, val status: String, val rotas: Int, val entregas: Int, val km: Double,
    val valoresVisiveis: Boolean, val total: Double?, val ajustes: List<Pair<String, Double>>, val itens: List<RotaHolerite>,
)

internal fun JsonObject.paraPeriodo(): PeriodoHolerite {
    fun s(k: String) = (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is JsonNull }?.content
    return PeriodoHolerite(s("inicio").orEmpty(), s("fim").orEmpty(), s("status") ?: "aberto", s("rotas")?.toIntOrNull() ?: 0,
        s("entregas")?.toIntOrNull() ?: 0, s("km")?.toDoubleOrNull() ?: 0.0, s("total")?.toDoubleOrNull(), s("pago_em")?.let { instante(it) })
}

internal fun JsonObject.paraHolerite(): Holerite {
    fun s(o: JsonObject, k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is JsonNull }?.content
    return Holerite(
        inicio = s(this, "inicio").orEmpty(), fim = s(this, "fim").orEmpty(), status = s(this, "status") ?: "aberto",
        rotas = s(this, "rotas")?.toIntOrNull() ?: 0, entregas = s(this, "entregas")?.toIntOrNull() ?: 0, km = s(this, "km")?.toDoubleOrNull() ?: 0.0,
        valoresVisiveis = s(this, "valores_visiveis") == "true", total = s(this, "total")?.toDoubleOrNull(),
        ajustes = (this["ajustes"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e -> (e as? JsonObject)?.let { a -> (s(a, "descricao") ?: "") to (s(a, "valor")?.toDoubleOrNull() ?: 0.0) } }.orEmpty(),
        itens = (this["itens"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { e ->
            val o = e as? JsonObject ?: return@mapNotNull null
            RotaHolerite(s(o, "id") ?: return@mapNotNull null, s(o, "numero")?.toLongOrNull() ?: 0, s(o, "concluida_em")?.let { instante(it) },
                s(o, "entregas")?.toIntOrNull() ?: 0, s(o, "km")?.toDoubleOrNull() ?: 0.0, s(o, "clientes"), s(o, "valor")?.toDoubleOrNull())
        }.orEmpty(),
    )
}
