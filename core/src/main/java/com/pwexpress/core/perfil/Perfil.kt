package com.pwexpress.core.perfil

import com.pwexpress.core.tentar

import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.network.HttpResposta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Quem está usando o app: nome, foto, papéis, modo teste e configurações enviadas pelo servidor. */
data class Perfil(
    val id: String,
    val nome: String,
    val fotoUrl: String?,
    val gestor: Boolean,
    val motoboy: Boolean,
    val modoTeste: Boolean,
    val configuracoes: Map<String, JsonElement>,
    /** Dev: administra o sistema inteiro (acima do gestor). */
    val dev: Boolean = false,
    /** "ouro" ou "vermelho" (vermelho só para o Dev). */
    val corTema: String = "ouro",
    /** Módulos ligados para esta conta (o Dev liga/desliga no console). */
    val modulos: Map<String, Boolean> = emptyMap(),
    /** Raio de disponibilidade (null = sem limite). */
    val raioKm: Double? = null,
    /** "posicao" (de onde o motoboy estiver) ou "fixo" (de um ponto escolhido). */
    val raioModo: String = "posicao",
    val raioLat: Double? = null,
    val raioLng: Double? = null,
    /** Conta só de cliente (pede pelo site, não usa o app). */
    val soCliente: Boolean = false,
    /** O gestor deixa este motoboy ver a aba Holerite. */
    val holeriteVisivel: Boolean = true,
    /** Nome da empresa (transportadora) da conta na Delibra. */
    val organizacao: String? = null,
    /** Dev oculto: os outros veem só "Dev", com o ícone </>. */
    val devOculto: Boolean = false,
    /** Líder de equipe (o gestor escolhe o que ele pode). */
    val supervisor: Boolean = false,
    val permissoes: Map<String, Boolean> = emptyMap(),
) {
    /** Supervisor pode fazer isto? (gestor e Dev podem tudo) */
    fun pode(acao: String, padrao: Boolean = true): Boolean = dev || !supervisor || (permissoes[acao] ?: padrao)
    /** O módulo está valendo para esta conta? (sem registro = ligado; o Dev sempre vê) */
    fun modulo(chave: String): Boolean = dev || modulos[chave] != false

    /** Conta gestor + motoboy: tem as ferramentas de administrador no app. */
    val admin: Boolean get() = gestor && motoboy

    fun numero(chave: String, padrao: Double): Double =
        (configuracoes[chave] as? JsonPrimitive)?.doubleOrNull ?: padrao

    fun logico(chave: String, padrao: Boolean): Boolean =
        (configuracoes[chave] as? JsonPrimitive)?.booleanOrNull ?: padrao

    fun texto(chave: String, padrao: String): String =
        (configuracoes[chave] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: padrao
}

/** Uma chave PIX da pessoa. A [principal] é para onde o dinheiro das corridas é repassado. */
data class ChavePix(val id: String, val tipo: String, val chave: String, val titular: String, val principal: Boolean)

data class Ganhos(
    val hoje: Double, val semana: Double, val mes: Double, val corridasHoje: Int,
    val total: Double = 0.0, val corridasSemana: Int = 0, val corridasMes: Int = 0, val kmHoje: Double = 0.0, val kmMes: Double = 0.0,
    /** O gestor deixa o motoboy ver valores? (sem isso o app mostra rotas, entregas e km) */
    val valoresVisiveis: Boolean = true,
    val entregasHoje: Int = 0, val entregasPeriodo: Int = 0, val corridasPeriodo: Int = 0, val kmPeriodo: Double = 0.0,
    val periodoTotal: Double = 0.0, val periodoInicio: String? = null, val periodoFim: String? = null,
)

object Perfis {
    /** Ícone e nome da empresa vêm junto com o perfil: o app troca o Delibra por eles. */
    private fun publicarMarca(o: kotlinx.serialization.json.JsonElement) {
        val org = ((o as? JsonObject)?.get("organizacao") as? JsonObject)
        fun t(k: String) = (org?.get(k) as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
        com.pwexpress.core.marca.MarcaEmpresa.atualizar(t("nome")?.let { com.pwexpress.core.marca.Marca(it, t("logo"), t("cor"), t("nome_app"), t("logo_escala")?.toFloatOrNull() ?: 1.9f) })
    }


    private val _perfil = MutableStateFlow<Perfil?>(null)
    val perfil: StateFlow<Perfil?> = _perfil.asStateFlow()

    private val _ganhos = MutableStateFlow<Ganhos?>(null)
    val ganhos: StateFlow<Ganhos?> = _ganhos.asStateFlow()

    /** Busca perfil e ganhos. Falha silenciosa (sem internet mantém o último). */
    suspend fun carregar() {
        if (PwCore.auth.sessao.value == null) return
        tentar {
            val r = PwCore.rpc.chamar("meu_perfil", JsonObject(emptyMap()))
            if (r.ok) r.json()?.let { _perfil.value = it.paraPerfil(); publicarMarca(it) }
            val g = PwCore.rpc.chamar("meus_ganhos", JsonObject(emptyMap()))
            if (g.ok) g.json()?.jsonObject?.let { o ->
                _ganhos.value = Ganhos(
                    hoje = o.num("hoje"), semana = o.num("semana"), mes = o.num("mes"),
                    corridasHoje = o.num("corridas_hoje").toInt(),
                    total = o.num("total"), corridasSemana = o.num("corridas_semana").toInt(), corridasMes = o.num("corridas_mes").toInt(),
                    kmHoje = o.num("km_hoje"), kmMes = o.num("km_mes"),
                    valoresVisiveis = (o["valores_visiveis"] as? JsonPrimitive)?.booleanOrNull ?: true,
                    entregasHoje = o.num("entregas_hoje").toInt(), entregasPeriodo = o.num("entregas_periodo").toInt(),
                    corridasPeriodo = o.num("corridas_periodo").toInt(), kmPeriodo = o.num("km_periodo"), periodoTotal = o.num("periodo_total"),
                    periodoInicio = (o["periodo_inicio"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content,
                    periodoFim = (o["periodo_fim"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content,
                )
            }
        }
    }

    fun limpar() {
        _perfil.value = null
        _ganhos.value = null
    }

    /** Muda nome e/ou foto. A foto vai para o bucket público "avatares/<id>/". */
    suspend fun atualizar(nome: String?, foto: ByteArray?): Result<Unit> = tentar {
        val atual = _perfil.value ?: error("Perfil ainda não carregado.")
        var url: String? = null
        if (foto != null) {
            val caminho = "${atual.id}/foto-${System.currentTimeMillis()}.jpg"
            val up = PwCore.rpc.enviarArquivo("avatares", caminho, foto, "image/jpeg")
            if (!up.ok) error("Não foi possível enviar a foto (${up.codigo}).")
            url = "${PwConfig.supabaseUrl}/storage/v1/object/public/avatares/$caminho"
        }
        val r = PwCore.rpc.chamar("atualizar_meu_perfil", buildJsonObject {
            nome?.let { put("p_nome", it) }
            url?.let { put("p_foto_url", it) }
        })
        if (!r.ok) error(if (r.corpo.contains("nome_invalido")) "O nome deve ter entre 2 e 60 letras." else "Erro ao salvar (${r.codigo}).")
        r.json()?.let { _perfil.value = it.paraPerfil(); publicarMarca(it) }
        Unit
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Liga/desliga o modo teste (só contas gestor + motoboy). */
    suspend fun definirModoTeste(ligado: Boolean): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("definir_modo_teste", buildJsonObject { put("p_ligado", ligado) })
        if (!r.ok) error(
            when {
                r.corpo.contains("conclua_a_corrida_antes") -> "Conclua a corrida em andamento antes de trocar o modo."
                r.corpo.contains("apenas_gestor") -> "Só gestores podem usar o modo teste."
                r.corpo.contains("modulo_desligado") -> "O modo teste está desligado pelo Dev."
                else -> "Erro (${r.codigo})."
            },
        )
        carregar()
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Raio de disponibilidade: 0/null = sem limite. Modo "posicao" ou "fixo" (com o ponto). */
    suspend fun definirRaio(km: Double?, modo: String, lat: Double?, lng: Double?): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("definir_meu_raio", buildJsonObject {
            put("p_km", km ?: 0.0); put("p_modo", modo)
            lat?.let { put("p_lat", it) }; lng?.let { put("p_lng", it) }
        })
        if (!r.ok) error(
            when {
                r.corpo.contains("raio_invalido") -> "Raio fora do permitido."
                r.corpo.contains("escolha_o_ponto") -> "Escolha o ponto no mapa."
                else -> "Erro (${r.codigo})."
            },
        )
        _perfil.value = _perfil.value?.copy(raioKm = km?.takeIf { it > 0 }, raioModo = modo, raioLat = lat, raioLng = lng)
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Dev: troca entre o tema dourado e o vermelho (vale no site também). */
    /** Dev: ficar oculto (aparece como "Dev" com o ícone </>). */
    suspend fun definirOculto(sim: Boolean): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("dev_ocultar", buildJsonObject { put("p_ocultar", sim) })
        if (!r.ok) error("Não deu para mudar agora.")
        _perfil.value = _perfil.value?.copy(devOculto = sim)
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    suspend fun definirCorTema(cor: String): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("definir_cor_tema", buildJsonObject { put("p_cor", cor) })
        if (!r.ok) error(if (r.corpo.contains("apenas_dev")) "Só o Dev pode usar o tema vermelho." else "Erro (${r.codigo}).")
        _perfil.value = _perfil.value?.copy(corTema = cor)
        carregar()
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Todas as chaves PIX da pessoa (a principal primeiro). null = não deu para carregar. */
    suspend fun minhasChavesPix(): List<ChavePix>? = tentar {
        val id = _perfil.value?.id ?: return null
        val r = PwCore.rpc.rest("GET", "/rest/v1/chaves_pix?usuario_id=eq.$id&select=id,tipo_chave,chave,titular,principal&order=principal.desc,criado_em.asc")
        if (!r.ok) return null
        r.json()?.jsonArray?.map { e ->
            val o = e.jsonObject
            ChavePix(
                o["id"]!!.jsonPrimitive.content, o["tipo_chave"]!!.jsonPrimitive.content, o["chave"]!!.jsonPrimitive.content,
                o["titular"]!!.jsonPrimitive.content, o["principal"]?.jsonPrimitive?.booleanOrNull == true,
            )
        }.orEmpty()
    }.getOrNull()

    /** Cadastra mais uma chave (a primeira vira a principal sozinha). */
    suspend fun adicionarChavePix(tipo: String, chave: String, titular: String): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("adicionar_chave_pix", buildJsonObject { put("p_tipo", tipo); put("p_chave", chave); put("p_titular", titular) })
        if (!r.ok) error(erroPix(r))
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    suspend fun removerChavePix(id: String): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("remover_chave_pix", buildJsonObject { put("p_id", id) })
        if (!r.ok) error(erroPix(r))
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    /** Escolhe para qual chave os repasses vão. */
    suspend fun definirChavePixPrincipal(id: String): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("definir_chave_pix_principal", buildJsonObject { put("p_id", id) })
        if (!r.ok) error(erroPix(r))
    }.recoverCatching { e -> throw if (e is java.io.IOException) Exception("Sem internet.") else e }

    private fun erroPix(r: HttpResposta) = when {
        r.corpo.contains("chave_repetida") -> "Essa chave já está cadastrada."
        r.corpo.contains("chave_invalida") -> "A chave não confere com o tipo."
        r.corpo.contains("titular_invalido") -> "Informe o nome do titular."
        r.corpo.contains("tipo_invalido") -> "Tipo de chave inválido."
        r.corpo.contains("limite_chaves") -> "Limite de chaves atingido."
        r.codigo == 404 -> "Atualize o banco: a função de chaves PIX ainda não existe."
        else -> "Erro (${r.codigo})."
    }

    // ------------------------------------------------------------------ util

    private fun HttpResposta.json(): JsonElement? =
        corpo.takeIf { it.isNotBlank() && it != "null" }?.let { Json.parseToJsonElement(it) }?.takeIf { it !is JsonNull }

    private fun JsonObject.num(k: String): Double = (this[k] as? JsonPrimitive)?.doubleOrNull ?: 0.0

    private fun JsonElement.paraPerfil(): Perfil {
        val o = jsonObject
        fun txt(k: String) = (o[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
        fun bool(k: String) = (o[k] as? JsonPrimitive)?.booleanOrNull ?: false
        return Perfil(
            id = txt("id").orEmpty(),
            nome = txt("nome").orEmpty(),
            fotoUrl = txt("foto_url"),
            gestor = bool("gestor"),
            motoboy = bool("motoboy") || (o["papeis"]?.jsonArray?.any { it.jsonPrimitive.content == "motoboy" } ?: false),
            modoTeste = bool("modo_teste"),
            configuracoes = (o["configuracoes"] as? JsonObject)?.toMap() ?: emptyMap(),
            dev = bool("dev"),
            corTema = txt("cor_tema") ?: "ouro",
            modulos = (o["modulos"] as? JsonObject)?.mapValues { (it.value as? JsonPrimitive)?.booleanOrNull ?: true } ?: emptyMap(),
            raioKm = (o["raio"] as? JsonObject)?.let { (it["km"] as? JsonPrimitive)?.doubleOrNull },
            raioModo = (o["raio"] as? JsonObject)?.let { (it["modo"] as? JsonPrimitive)?.takeIf { p -> p !is JsonNull }?.content } ?: "posicao",
            raioLat = (o["raio"] as? JsonObject)?.let { (it["lat"] as? JsonPrimitive)?.doubleOrNull },
            raioLng = (o["raio"] as? JsonObject)?.let { (it["lng"] as? JsonPrimitive)?.doubleOrNull },
            holeriteVisivel = (o["holerite_visivel"] as? JsonPrimitive)?.booleanOrNull ?: true,
            organizacao = ((o["organizacao"] as? JsonObject)?.get("nome") as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content,
            devOculto = bool("dev_oculto"),
            supervisor = bool("supervisor") && !bool("dev"),
            permissoes = (o["permissoes"] as? JsonObject)?.mapValues { (it.value as? JsonPrimitive)?.booleanOrNull ?: false } ?: emptyMap(),
            soCliente = (o["papeis"] as? kotlinx.serialization.json.JsonArray)?.map { it.jsonPrimitive.content }?.let { p -> "cliente" in p && p.none { it in listOf("gestor", "motoboy", "dev", "supervisor") } } ?: false,
        )
    }
}
