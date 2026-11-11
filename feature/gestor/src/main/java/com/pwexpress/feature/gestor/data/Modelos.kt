package com.pwexpress.feature.gestor.data

import com.pwexpress.core.tentar

import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import kotlinx.serialization.json.JsonObject

/** Data/hora ISO do servidor → milissegundos. */
fun iso(s: String?): Long? = s?.let {
    tentar { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }
        .recoverCatching { _ -> java.time.OffsetDateTime.parse(it.replace(" ", "T")).toInstant().toEpochMilli() }
        .getOrNull()
}

object Status {
    const val MONTADA = "montada"
    const val AGENDADA = "agendada"
    const val RESERVADA = "reservada"
    const val AGUARDANDO = "aguardando_motoboy"
    const val AGUARDANDO_PAGAMENTO = "aguardando_pagamento"
    const val SEM_MOTOBOY = "sem_motoboy"
    const val ACEITA = "aceita"
    const val EM_ANDAMENTO = "em_andamento"
    const val CONCLUIDA = "concluida"
    const val CANCELADA = "cancelada"
    val ATIVAS = setOf(AGUARDANDO_PAGAMENTO, AGUARDANDO, SEM_MOTOBOY, ACEITA, EM_ANDAMENTO)
    val PLANEJADAS = setOf(AGENDADA, RESERVADA)
    val rotulo = mapOf(
        MONTADA to "Rota montada · esperando você", AGENDADA to "Planejada · livre", RESERVADA to "Planejada · reservada", AGUARDANDO to "Procurando motoboy", AGUARDANDO_PAGAMENTO to "Aguardando pagamento",
        SEM_MOTOBOY to "Sem motoboy", ACEITA to "Aceita", EM_ANDAMENTO to "Em andamento", CONCLUIDA to "Concluída", CANCELADA to "Cancelada",
    )
}

data class RefG(val nome: String, val distanciaM: Int?, val tipo: String?, val lat: Double? = null, val lng: Double? = null, val relacao: String? = null)

data class ParadaG(
    val id: String, val ordem: Int, val coleta: Boolean, val endereco: String, val lat: Double, val lng: Double,
    val contatoNome: String?, val contatoTelefone: String?, val instrucoes: String?, val status: String,
    val concluidaEm: Long?, val recebedorNome: String?, val fotoPath: String?, val assinaturaPath: String?,
    val distanciaChegadaM: Int?, val localIgnorado: Boolean, val referencias: List<RefG>,
    /** Confirmações pedidas neste ponto (null = segue o padrão da rota), código próprio e horário em que atende. */
    val exigeFoto: Boolean? = null, val exigeNome: Boolean? = null, val exigeAssinatura: Boolean? = null, val exigeCodigo: Boolean? = null,
    val codigo: String? = null, val janelaDe: String? = null, val janelaAte: String? = null,
    /** Pedido de onde veio a parada (rotas montadas de pedidos). */
    val pedidoId: String? = null,
    /** Você liberou: o motoboy confirma de onde estiver. */
    val liberadaLocal: Boolean = false,
    /** Feita sem internet (veio depois com a hora certa). */
    val feitaOffline: Boolean = false,
    /** A rota fechou sozinha e esta etapa espera o app mandar os dados. */
    val confirmacaoPendente: Boolean = false,
)

data class CorridaG(
    val id: String, val numero: Long, val teste: Boolean, val status: String,
    val urgente: Boolean = false,
    val empresaId: String?, val clienteNome: String?, val criadaPor: String?,
    val motoboyId: String?, val motoboyNomeArquivado: String?,
    val valor: Double?, val valorBase: Double?, val fatorEntrega: Double, val fatorColeta: Double,
    val valorMotoboy: Double?, val valorGestao: Double?, val repassePct: Double?,
    val valorDev: Double? = null, val devPct: Double? = null,
    val pagamentoStatus: String = "nao_exigido", val pagamentoMetodo: String? = null, val porCliente: Boolean = false,
    val distanciaPlanejadaM: Int?, val duracaoS: Int?, val distanciaRealM: Int?, val rota: String?,
    val observacoes: String?, val exigirNoLocal: Boolean, val raioChegadaM: Int,
    val exigeNome: Boolean, val exigeFoto: Boolean, val exigeAssinatura: Boolean, val exigeCodigo: Boolean, val codigoEntrega: String?,
    val criadaEm: Long, val concluidaEm: Long?, val agendadaPara: Long?, val reservadaEm: Long?, val serieId: String?, val atrasada: Boolean,
    val paradas: List<ParadaG>,
    /** Rotas automáticas (roteirizador). */
    val origem: String = "manual", val sugeridoMotoboyId: String? = null, val modoDespacho: String? = null, val quadroDesde: Long? = null,
    val pagCalculo: JsonObject? = null, val pagAjuste: Double = 0.0, val pagAjusteMotivo: String? = null, val perigoso: Boolean = false,
    /** Fechou sozinha (motoboy sem sinal): espera o app mandar as etapas. */
    val aguardandoConfirmacao: Boolean = false,
) {
    val automatica get() = origem == "roteirizador"
    val entregas get() = paradas.count { !it.coleta }
    val numeroExibicao get() = if (teste) "T$numero" else "#$numero"
    val ativa get() = status in Status.ATIVAS
    val planejada get() = status in Status.PLANEJADAS
}

data class OfertaG(val id: String, val corridaId: String, val motoboyId: String, val distanciaM: Int?, val expiraEm: Long)

data class MembroG(
    val id: String, val email: String, val nome: String, val foto: String?, val papeis: List<String>,
    val status: String?, val modoTeste: Boolean, val repassePct: Double?, val bloqueado: Boolean, val ultimoAcesso: Long?,
) {
    val gestor get() = "gestor" in papeis
    val supervisor get() = "supervisor" in papeis
    val motoboy get() = "motoboy" in papeis
    val dev get() = "dev" in papeis
    /** Conta de acesso de um cliente (pede pelo site); não aparece na equipe. */
    val soCliente get() = "cliente" in papeis && !gestor && !motoboy && !dev
}

data class PosicaoG(val id: String, val status: String, val lat: Double?, val lng: Double?, val posicaoEm: Long?, val modoTeste: Boolean, val bateria: Int?, val precisaoM: Double?, val economia: Boolean = false,
    /** Raio de disponibilidade (km; 0 = sem raio) e de onde ele conta (posição atual ou ponto fixo). */
    val raioKm: Double? = null, val raioLat: Double? = null, val raioLng: Double? = null) {
    /** Centro e raio (km) para desenhar no mapa, se houver. */
    val raio: Pair<Pair<Double, Double>, Double>? get() {
        val km = raioKm ?: return null
        if (km <= 0) return null
        val la = raioLat ?: lat ?: return null; val lo = raioLng ?: lng ?: return null
        return (la to lo) to km
    }
}

/** Ponto da trilha (últimas 12 h). */
data class PontoTrilha(val sessao: String?, val status: String, val lat: Double, val lng: Double, val quando: Long = 0, val corridaId: String? = null)

data class EmpresaG(
    val id: String, val nome: String, val cnpj: String?, val razaoSocial: String?, val nomeFantasia: String?, val situacao: String?,
    val telefone: String?, val email: String?, val endereco: String?, val lat: Double?, val lng: Double?, val instrucoesColeta: String?,
    val modalidade: String, val valorKm: Double?, val valorFixo: Double?, val clienteVeAoVivo: Boolean, val observacoes: String?,
    /** "pj" (CNPJ) ou "pf" (CPF). */
    val tipo: String = "pj", val cpf: String? = null,
    /** Pode pedir Express (na hora); sem isso, só agendados. */
    val podeExpress: Boolean = true,
    val repassePct: Double? = null, val devPct: Double? = null,
    val pagamentoMetodos: List<String> = listOf("pix", "cartao"),
    val modoTeste: Boolean = false, val icone: String? = null,
) {
    val documento get() = if (tipo == "pf") cpf else cnpj
}

data class EventoG(val tipo: String, val dados: JsonObject, val criadoEm: Long)

data class ItemRegistro(
    val quando: Long, val tipo: String, val dados: JsonObject, val fez: Boolean, val numero: Long?, val teste: Boolean,
    val ganho: Double?, val valor: Double?, val cliente: String?,
)

// ------------------------------------------------------------------ JSON → modelos

fun JsonObject.paraParadaG() = ParadaG(
    id = str("id")!!, ordem = int("ordem") ?: 0, coleta = str("tipo") != "entrega", endereco = str("endereco").orEmpty(),
    lat = dbl("lat") ?: 0.0, lng = dbl("lng") ?: 0.0, contatoNome = str("contato_nome"), contatoTelefone = str("contato_telefone"),
    instrucoes = str("instrucoes"), status = str("status") ?: "pendente", concluidaEm = iso(str("concluida_em")),
    recebedorNome = str("recebedor_nome"), fotoPath = str("foto_path"), assinaturaPath = str("assinatura_path"),
    distanciaChegadaM = int("distancia_chegada_m"), localIgnorado = bool("local_ignorado"),
    referencias = arr("referencias").mapNotNull { r -> r.str("nome")?.let { RefG(it, r.int("distancia_m"), r.str("tipo"), r.dbl("lat"), r.dbl("lng"), r.str("relacao")) } },
    exigeFoto = boolOuNulo("exige_foto"), exigeNome = boolOuNulo("exige_nome"), exigeAssinatura = boolOuNulo("exige_assinatura"), exigeCodigo = boolOuNulo("exige_codigo"),
    codigo = str("codigo"), janelaDe = str("janela_de")?.take(5), janelaAte = str("janela_ate")?.take(5),
    pedidoId = str("pedido_id"),
    liberadaLocal = bool("liberada_local"), feitaOffline = bool("feita_offline"), confirmacaoPendente = bool("confirmacao_pendente"),
)

/** true/false do JSON, ou null quando vazio. */
internal fun JsonObject.boolOuNulo(k: String): Boolean? = (this[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content?.toBooleanStrictOrNull()

fun JsonObject.paraCorridaG() = CorridaG(
    id = str("id")!!, numero = long("numero") ?: 0, teste = bool("teste"), status = str("status").orEmpty(),
    urgente = bool("urgente"),
    empresaId = str("empresa_id"), clienteNome = str("cliente_nome"), criadaPor = str("criada_por"),
    motoboyId = str("motoboy_id"), motoboyNomeArquivado = str("motoboy_nome_arquivado"),
    valor = dbl("valor"), valorBase = dbl("valor_base"), fatorEntrega = dbl("fator_entrega") ?: 0.0, fatorColeta = dbl("fator_coleta") ?: 0.0,
    valorMotoboy = dbl("valor_motoboy"), valorGestao = dbl("valor_gestao"), repassePct = dbl("repasse_pct"),
    valorDev = dbl("valor_dev"), devPct = dbl("dev_pct"),
    pagamentoStatus = str("pagamento_status") ?: "nao_exigido", pagamentoMetodo = str("pagamento_metodo"), porCliente = bool("por_cliente"),
    distanciaPlanejadaM = int("distancia_planejada_m"), duracaoS = int("duracao_planejada_s"), distanciaRealM = int("distancia_real_m"),
    rota = str("rota_planejada"), observacoes = str("observacoes"), exigirNoLocal = bool("exigir_no_local"), raioChegadaM = int("raio_chegada_m") ?: 150,
    exigeNome = bool("exige_nome"), exigeFoto = bool("exige_foto"), exigeAssinatura = bool("exige_assinatura"), exigeCodigo = bool("exige_codigo"),
    codigoEntrega = str("codigo_entrega"), criadaEm = iso(str("criada_em")) ?: 0, concluidaEm = iso(str("concluida_em")),
    agendadaPara = iso(str("agendada_para")), reservadaEm = iso(str("reservada_em")), serieId = str("serie_id"),
    atrasada = str("atraso_avisado_em") != null,
    paradas = arr("paradas").map { it.paraParadaG() }.sortedBy { it.ordem },
    origem = str("origem") ?: "manual", sugeridoMotoboyId = str("sugerido_motoboy_id"), modoDespacho = str("modo_despacho"), quadroDesde = iso(str("quadro_desde")),
    pagCalculo = obj("pag_calculo"), pagAjuste = dbl("pag_ajuste") ?: 0.0, pagAjusteMotivo = str("pag_ajuste_motivo"), perigoso = bool("tem_perigoso"),
    aguardandoConfirmacao = bool("aguardando_confirmacao"),
)

// ------------------------------------------------------------------ pedidos (do site do cliente e do gestor)

data class PontoPedidoG(
    val ordem: Int, val coleta: Boolean, val endereco: String, val contatoNome: String?, val lat: Double? = null, val lng: Double? = null, val status: String = "pendente",
    val id: String = "", val contatoTelefone: String? = null, val instrucoes: String? = null, val concluidoEm: Long? = null, val recebedorNome: String? = null,
    val exigeFoto: Boolean = false, val exigeNome: Boolean = false, val exigeAssinatura: Boolean = false, val exigeCodigo: Boolean = false,
    val janelaDe: String? = null, val janelaAte: String? = null,
)
data class ItemPedidoG(
    val descricao: String, val quantidade: Int, val pesoKg: Double?,
    val categoriaId: String? = null, val comprimento: Double? = null, val largura: Double? = null, val altura: Double? = null,
    val valorDeclarado: Double? = null, val coletaId: String? = null, val entregaId: String? = null, val observacoes: String? = null,
)

data class PedidoG(
    val id: String, val numero: Long, val teste: Boolean, val empresaId: String?, val clienteNome: String?, val origem: String,
    val tipo: String, val tipoPedido: String, val quando: Long?, val urgente: Boolean, val status: String, val observacoes: String?,
    val distanciaM: Int?, val valorCalculado: Double?, val valorAjuste: Double, val ajusteMotivo: String?, val valor: Double?,
    val cobranca: String, val pagamentoStatus: String, val criadoEm: Long, val recusaMotivo: String?,
    val pontos: List<PontoPedidoG>, val itens: List<ItemPedidoG>,
    val regras: JsonObject = JsonObject(emptyMap()),
    /** Orçamento calculado (linhas, percentuais, mínimo, total, pesos). */
    val orcamento: JsonObject? = null,
    val valorDev: Double? = null, val faturaId: String? = null, val pagamentoMetodo: String? = null,
    val tipoPedidoNome: String? = null,
) {
    val numeroExibicao get() = if (teste) "P-T$numero" else "P-$numero"
    val planejado get() = tipo == "planejado"
}

object StatusPedido {
    val rotulo = mapOf(
        "recebido" to "Para revisar", "aprovado" to "Aprovado", "roteirizado" to "Na rota", "em_rota" to "Em rota",
        "concluido" to "Concluído", "cancelado" to "Cancelado", "recusado" to "Recusado",
    )
}

fun JsonObject.paraPedidoG() = PedidoG(
    id = str("id")!!, numero = long("numero") ?: 0, teste = bool("teste"), empresaId = str("empresa_id"), clienteNome = str("cliente_nome"),
    origem = str("origem") ?: "gestor", tipo = str("tipo") ?: "express", tipoPedido = str("tipo_pedido") ?: "padrao", quando = iso(str("quando")),
    urgente = bool("urgente"), status = str("status") ?: "recebido", observacoes = str("observacoes"), distanciaM = int("distancia_estimada_m"),
    valorCalculado = dbl("valor_calculado"), valorAjuste = dbl("valor_ajuste") ?: 0.0, ajusteMotivo = str("ajuste_motivo"), valor = dbl("valor"),
    cobranca = str("cobranca") ?: "credito", pagamentoStatus = str("pagamento_status") ?: "nao_exigido", criadoEm = iso(str("criado_em")) ?: 0,
    recusaMotivo = str("recusa_motivo"),
    pontos = arr("pedido_pontos").map {
        PontoPedidoG(it.int("ordem") ?: 0, it.str("tipo") == "coleta", it.str("endereco").orEmpty(), it.str("contato_nome"), it.dbl("lat"), it.dbl("lng"), it.str("status") ?: "pendente",
            it.str("id").orEmpty(), it.str("contato_telefone"), it.str("instrucoes"), iso(it.str("concluido_em")), it.str("recebedor_nome"),
            it.boolOuNulo("exige_foto") == true, it.boolOuNulo("exige_nome") == true, it.boolOuNulo("exige_assinatura") == true, it.boolOuNulo("exige_codigo") == true,
            it.str("janela_de")?.take(5), it.str("janela_ate")?.take(5))
    }.sortedBy { it.ordem },
    itens = arr("pedido_itens").map {
        ItemPedidoG(it.str("descricao").orEmpty(), it.int("quantidade") ?: 1, it.dbl("peso_kg"), it.str("categoria_id"), it.dbl("comprimento_cm"), it.dbl("largura_cm"), it.dbl("altura_cm"),
            it.dbl("valor_declarado"), it.str("coleta_id"), it.str("entrega_id"), it.str("observacoes"))
    },
    regras = obj("regras") ?: JsonObject(emptyMap()),
    orcamento = obj("orcamento"), valorDev = dbl("valor_dev"), faturaId = str("fatura_id"), pagamentoMetodo = str("pagamento_metodo"),
)

fun JsonObject.paraOfertaG() = OfertaG(str("id")!!, str("corrida_id")!!, str("motoboy_id")!!, int("distancia_m"), iso(str("expira_em")) ?: 0)

fun JsonObject.paraMembroG() = MembroG(
    id = str("id")!!, email = str("email").orEmpty(), nome = str("nome").orEmpty().ifBlank { str("email").orEmpty().substringBefore("@") },
    foto = str("foto_url"), papeis = strs("papeis"), status = str("status"), modoTeste = bool("modo_teste"),
    repassePct = dbl("repasse_pct"), bloqueado = bool("bloqueado"), ultimoAcesso = iso(str("ultimo_acesso")),
)

fun JsonObject.paraPosicaoG() = PosicaoG(str("id")!!, str("status") ?: "offline", dbl("lat"), dbl("lng"), iso(str("posicao_em")), bool("modo_teste"), int("bateria"), dbl("precisao_m"), bool("modo_economia"),
    dbl("raio_km"), if (str("raio_modo") == "fixo") dbl("raio_lat") else null, if (str("raio_modo") == "fixo") dbl("raio_lng") else null)

fun JsonObject.paraEmpresaG() = EmpresaG(
    id = str("id")!!, nome = str("nome").orEmpty(), cnpj = str("cnpj"), razaoSocial = str("razao_social"), nomeFantasia = str("nome_fantasia"),
    situacao = str("situacao"), telefone = str("telefone"), email = str("email"), endereco = str("endereco"), lat = dbl("lat"), lng = dbl("lng"),
    instrucoesColeta = str("instrucoes_coleta"), modalidade = str("modalidade_preco") ?: "por_km", valorKm = dbl("valor_km"), valorFixo = dbl("valor_fixo"),
    clienteVeAoVivo = bool("cliente_ve_ao_vivo"), observacoes = str("observacoes"),
    tipo = str("tipo") ?: "pj", cpf = str("cpf"), podeExpress = (this["pode_express"] as? kotlinx.serialization.json.JsonPrimitive)?.content != "false",
    repassePct = dbl("repasse_pct"), devPct = dbl("dev_pct"), pagamentoMetodos = strs("pagamento_metodos").ifEmpty { listOf("pix", "cartao") },
    modoTeste = bool("modo_teste"), icone = str("icone"),
)

fun JsonObject.paraItemRegistro() = ItemRegistro(
    quando = iso(str("quando")) ?: 0, tipo = str("tipo").orEmpty(), dados = obj("dados") ?: JsonObject(emptyMap()), fez = bool("fez"),
    numero = long("numero"), teste = bool("teste"), ganho = dbl("ganho"), valor = dbl("valor"), cliente = str("cliente"),
)


// ------------------------------------------------------------------ catálogo do pedido

data class TipoPedidoG(val chave: String, val nome: String, val descricao: String?, val icone: String?)

/** Campo do item: "oculto", "opcional" ou "obrigatorio" (o gestor define por categoria). */
data class CategoriaG(
    val id: String, val nome: String, val perigoso: Boolean, val fragil: Boolean, val icone: String?,
    val campoDescricao: String, val campoPeso: String, val campoMedidas: String, val campoValor: String, val campoQuantidade: String,
)

fun JsonObject.paraTipoPedidoG() = TipoPedidoG(str("chave")!!, str("nome").orEmpty(), str("descricao"), str("icone"))
fun JsonObject.paraCategoriaG() = CategoriaG(
    str("id")!!, str("nome").orEmpty(), bool("perigoso"), bool("fragil"), str("icone"),
    str("campo_descricao") ?: "opcional", str("campo_peso") ?: "opcional", str("campo_medidas") ?: "opcional",
    str("campo_valor") ?: "oculto", str("campo_quantidade") ?: "opcional",
)


/** Veículo e CNH (campos de texto como no formulário; validade em dd/mm/aaaa). */
data class VeiculoG(
    val tipo: String = "moto", val modelo: String = "", val placa: String = "", val cor: String = "", val ano: String = "",
    val cnhNumero: String = "", val cnhCategoria: String = "", val cnhValidade: String = "", val venceEmDias: Long? = null,
)

fun JsonObject.paraVeiculoG(): VeiculoG {
    val validade = str("cnh_validade")?.takeIf { it.length >= 10 }
    return VeiculoG(
        tipo = str("tipo") ?: str("veiculo_tipo") ?: "moto", modelo = (str("modelo") ?: str("veiculo_modelo")).orEmpty(),
        placa = (str("placa") ?: str("veiculo_placa")).orEmpty(), cor = (str("cor") ?: str("veiculo_cor")).orEmpty(),
        ano = (str("ano") ?: str("veiculo_ano") ?: int("veiculo_ano")?.toString()).orEmpty(),
        cnhNumero = str("cnh_numero").orEmpty(), cnhCategoria = str("cnh_categoria").orEmpty(),
        cnhValidade = validade?.let { "${it.substring(8, 10)}/${it.substring(5, 7)}/${it.substring(0, 4)}" }.orEmpty(),
        venceEmDias = validade?.let { tentar { java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), java.time.LocalDate.parse(it.take(10))) }.getOrNull() },
    )
}
