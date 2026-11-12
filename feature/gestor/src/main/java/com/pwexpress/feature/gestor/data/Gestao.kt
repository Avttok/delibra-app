package com.pwexpress.feature.gestor.data

import com.pwexpress.core.tentar

import com.pwexpress.core.PwCore
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.exigirOk
import com.pwexpress.core.json.int
import com.pwexpress.core.json.lista
import com.pwexpress.core.json.objeto
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import com.pwexpress.core.network.ErroServidor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.time.Instant

/**
 * Tudo o que o gestor faz no site, para o app: mesmas tabelas, mesmas funções do banco e as
 * mesmas rotas do servidor (criar conta, CNPJ…). As regras de segurança ficam no banco.
 */
object Gestao {

    data class Dados(
        val corridas: List<CorridaG> = emptyList(),
        val ofertas: List<OfertaG> = emptyList(),
        val equipe: List<MembroG> = emptyList(),
        val posicoes: List<PosicaoG> = emptyList(),
        val empresas: List<EmpresaG> = emptyList(),
        val favoritos: Map<String, List<String>> = emptyMap(),
        val config: Map<String, JsonElement> = emptyMap(),
        /** Trilhas das últimas 12 h por motoboy (como no mapa do site). */
        val trilhas: Map<String, List<PontoTrilha>> = emptyMap(),
        /** Caminho pelas ruas de cada registro das trilhas (chave "motoboy|inicioMs"). */
        val ajustesTrilha: Map<String, com.pwexpress.core.mapa.PercursosServidor.Ajuste> = emptyMap(),
        /** Logins de cliente por cadastro (empresa_id → contas). */
        val acessosCliente: Map<String, List<MembroG>> = emptyMap(),
        /** Pedidos dos últimos 30 dias (os "para revisar" ficam no topo da aba Pedidos). */
        val pedidos: List<PedidoG> = emptyList(),
        /** Ícone escolhido para cada pessoa (perfis.icone). */
        val icones: Map<String, String> = emptyMap(),
        val carregado: Boolean = false,
    ) {
        fun membro(id: String?) = equipe.firstOrNull { it.id == id }
        fun membroIcone(id: String?): String? = icones[id]
        fun empresa(id: String?) = empresas.firstOrNull { it.id == id }
        fun posicao(id: String?) = posicoes.firstOrNull { it.id == id }
        fun cfgNum(k: String, padrao: Double) = (config[k] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: padrao
        fun cfgBool(k: String, padrao: Boolean) = (config[k] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: padrao
        fun cliente(c: CorridaG): String = c.empresaId?.let { empresa(it)?.nome ?: "Cliente" } ?: c.clienteNome?.let { "$it (avulso)" } ?: "Avulso"
        fun nomeMotoboy(c: CorridaG): String? = c.motoboyId?.let { membro(it)?.nome } ?: c.motoboyNomeArquivado?.let { "$it (conta excluída)" }
    }

    private val _dados = MutableStateFlow(Dados())
    val dados: StateFlow<Dados> = _dados.asStateFlow()
    private val _erro = MutableStateFlow<String?>(null)
    val erro: StateFlow<String?> = _erro.asStateFlow()
    private val mutex = Mutex()

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    private var voltas = 0
    private var ultimoTeste: Boolean? = null

    /** Recarrega tudo (o app chama a cada poucos segundos enquanto o painel está aberto). */
    suspend fun atualizar() = mutex.withLock {
        if (!Permissoes.estado.value.carregou) Permissoes.carregar()
        tentar {
            val desde = Instant.now().minusSeconds(30L * 86400).toString()
            coroutineScope {
                val rpc = PwCore.rpc
                val corridas = async { rpc.rest("GET", "/rest/v1/corridas?select=*,paradas(*)&or=(criada_em.gte.${enc(desde)},agendada_para.gte.${enc(desde)})&order=criada_em.desc&limit=400").exigirOk().lista() }
                val ofertas = async { rpc.rest("GET", "/rest/v1/ofertas?select=id,corrida_id,motoboy_id,status,distancia_m,expira_em&status=eq.pendente").exigirOk().lista() }
                val equipe = async { rpc.chamar("listar_equipe", JsonObject(emptyMap())).exigirOk().lista() }
                val pos = async { rpc.rest("GET", "/rest/v1/motoboys?select=id,status,lat,lng,posicao_em,modo_teste,modo_economia,bateria,precisao_m,raio_km,raio_modo,raio_lat,raio_lng").exigirOk().lista() }
                // trilhas pesam mais: a cada 5 voltas (~30 s)
                // percursos de teste e reais são separados: só os do modo atual (troca de modo recarrega na hora)
                val teste = com.pwexpress.core.designsystem.TemaApp.modoTeste.value
                val trocouModo = teste != ultimoTeste
                ultimoTeste = teste
                val trilha = if (voltas++ % 5 == 0 || !_dados.value.carregado || trocouModo) async {
                    val desde12h = Instant.now().minusSeconds(12L * 3600).toString()
                    // pelo site: todos os pings (sem o corte de 1000) e o caminho já casado com as ruas
                    com.pwexpress.core.mapa.PercursosServidor.buscar(Instant.parse(desde12h), Instant.now().plusSeconds(600), teste = teste)?.let { r ->
                        r.pings.groupBy({ it.motoboy }, { PontoTrilha(null, it.ping.status, it.ping.lat, it.ping.lng, it.ping.quando, it.ping.corridaId) }) to r.ajustes
                    } ?: tentar {
                        rpc.rest("GET", "/rest/v1/localizacoes?select=motoboy_id,sessao_id,status,lat,lng,registrado_em,corrida_id&teste=eq.$teste&registrado_em=gte.${enc(desde12h)}&order=registrado_em.asc&limit=8000").exigirOk().lista()
                            .mapNotNull { o -> val la = o.dbl("lat"); val lo = o.dbl("lng"); if (la == null || lo == null) null else o.str("motoboy_id")!! to PontoTrilha(o.str("sessao_id"), o.str("status") ?: "", la, lo, iso(o.str("registrado_em")) ?: 0, o.str("corrida_id")) }
                            .groupBy({ it.first }, { it.second }) to emptyMap<String, com.pwexpress.core.mapa.PercursosServidor.Ajuste>()
                    }.getOrNull()
                } else null
                val empresas = async { rpc.rest("GET", "/rest/v1/empresas?select=*&order=nome").exigirOk().lista() }
                val fav = async { rpc.rest("GET", "/rest/v1/empresa_favoritos?select=empresa_id,motoboy_id,ordem&order=ordem").exigirOk().lista() }
                // ajustes da empresa (padrão da plataforma + o que a empresa mudou)
                val cfg = async { rpc.chamar("ajustes", JsonObject(emptyMap())).exigirOk().objeto() ?: JsonObject(emptyMap()) }
                val vinculos = async { tentar { rpc.rest("GET", "/rest/v1/perfis?select=id,empresa_id&empresa_id=not.is.null").exigirOk().lista().associate { it.str("id")!! to it.str("empresa_id")!! } }.getOrDefault(emptyMap()) }
                val pedidos = async {
                    tentar {
                        rpc.rest("GET", "/rest/v1/pedidos?select=*,pedido_pontos(*),pedido_itens(*)&or=(criado_em.gte.${enc(desde)},status.eq.recebido)&order=criado_em.desc&limit=300")
                            .exigirOk().lista().map { it.paraPedidoG() }
                    }.getOrDefault(_dados.value.pedidos)
                }
                val icones = async { tentar { rpc.rest("GET", "/rest/v1/perfis?select=id,icone&icone=not.is.null").exigirOk().lista().associate { it.str("id")!! to it.str("icone")!! } }.getOrDefault(emptyMap()) }
                val trilhaR = trilha?.await()
                val todos = equipe.await().map { it.paraMembroG() }
                val vinc = vinculos.await()
                _dados.value = Dados(
                    corridas = corridas.await().map { it.paraCorridaG() },
                    ofertas = ofertas.await().map { it.paraOfertaG() },
                    equipe = todos.filterNot { it.soCliente },
                    acessosCliente = todos.filter { it.soCliente }.groupBy { vinc[it.id] ?: "" },
                    posicoes = pos.await().map { it.paraPosicaoG() },
                    empresas = empresas.await().map { it.paraEmpresaG() },
                    favoritos = fav.await().groupBy({ it.str("empresa_id")!! }, { it.str("motoboy_id")!! }),
                    config = cfg.await().toMap(),
                    trilhas = trilhaR?.first ?: _dados.value.trilhas,
                    ajustesTrilha = trilhaR?.second ?: _dados.value.ajustesTrilha,
                    pedidos = pedidos.await(),
                    icones = icones.await(),
                    carregado = true,
                )
            }
            _erro.value = null
        }.onFailure { _erro.value = mensagem(it) }
        Unit
    }

    fun limpar() { _dados.value = Dados(); Permissoes.limpar() }

    // ------------------------------------------------------------------ corridas

    suspend fun criarCorrida(dados: JsonObject, datas: List<Long> = emptyList()): Result<String> = acao {
        val sb = PwCore.rpc
        val r = when {
            datas.isEmpty() -> sb.chamar("criar_corrida", buildJsonObject { put("dados", dados) })
            datas.size == 1 -> sb.chamar("criar_corrida", buildJsonObject {
                put("dados", JsonObject(dados + ("agendada_para" to JsonPrimitive(Instant.ofEpochMilli(datas[0]).toString()))))
            })
            else -> sb.chamar("criar_serie", buildJsonObject {
                put("dados", dados)
                put("p_datas", buildJsonArray { datas.forEach { add(JsonPrimitive(Instant.ofEpochMilli(it).toString())) } })
            })
        }.exigirOk(::traduzir)
        val corpo = r.corpo.trim()
        (if (corpo.startsWith("[")) corpo.removePrefix("[").substringBefore(",").substringBefore("]") else corpo).trim('"')
    }

    suspend fun cancelar(id: String) = acao { PwCore.rpc.chamar("cancelar_corrida", buildJsonObject { put("p_corrida", id); put("p_motivo", "Cancelada pelo gestor") }).exigirOk(::traduzir); Unit }
    /** Pagamento recebido por fora (PIX/cartão conferido pelo gestor): libera a corrida para o despacho. */
    suspend fun confirmarPagamento(id: String, metodo: String) = acao {
        PwCore.rpc.chamar("confirmar_pagamento", buildJsonObject { put("p_corrida", id); put("p_metodo", metodo) }).exigirOk(::traduzir); Unit
    }

    /** Chave PIX principal de cada um (usuário → "chave · titular"): para onde vai o repasse. */
    suspend fun chavesPix(): Map<String, String> = tentar {
        PwCore.rpc.rest("GET", "/rest/v1/chaves_pix?principal=is.true&select=usuario_id,tipo_chave,chave,titular").exigirOk().lista()
            .associate { it.str("usuario_id")!! to "${it.str("chave")} · ${it.str("titular")}" }
    }.getOrDefault(emptyMap())

    suspend fun redespachar(id: String, motoboy: String?) = acao {
        PwCore.rpc.chamar("redespachar", buildJsonObject { put("p_corrida", id); put("p_motoboy", motoboy?.let { JsonPrimitive(it) } ?: JsonNull) }).exigirOk(::traduzir); Unit
    }

    suspend fun gerenciarPlanejada(id: String, acao: String, motoboy: String? = null, quando: Long? = null) = acao {
        PwCore.rpc.chamar("gerenciar_planejada", buildJsonObject {
            put("p_corrida", id); put("p_acao", acao)
            put("p_motoboy", motoboy?.let { JsonPrimitive(it) } ?: JsonNull)
            put("p_quando", quando?.let { JsonPrimitive(Instant.ofEpochMilli(it).toString()) } ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    /** Apaga de vez (senha conferida no banco). Devolve o número de tentativas restantes em caso de senha errada. */
    suspend fun excluirCorridas(ids: List<String>, senha: String, motivo: String?): Result<Unit> = acao {
        val r = PwCore.rpc.chamar("excluir_corridas", buildJsonObject {
            put("p_corridas", buildJsonArray { ids.forEach { add(JsonPrimitive(it)) } }); put("p_senha", senha); put("p_motivo", motivo ?: "")
        }).exigirOk(::traduzir).objeto()
        r?.str("erro")?.let { e ->
            val rest = r.int("restantes")
            throw ErroServidor(e, if (e == "senha_incorreta" && rest != null) "Senha incorreta. Restam $rest tentativa${if (rest == 1) "" else "s"}." else traduzir(e) ?: e)
        }
        val arquivos = r?.strs("arquivos").orEmpty()
        if (arquivos.isNotEmpty()) tentar { PwCore.rpc.removerArquivos("comprovantes", arquivos) }
        Unit
    }

    /** Quanto a limpeza de histórico apagaria (corridas finalizadas e pontos de percurso). */
    suspend fun previaHistorico(teste: Boolean, motoboy: String?, ateMs: Long?): Pair<Int, Int>? = tentar {
        val r = PwCore.rpc.chamar("previa_historico", buildJsonObject {
            put("p_teste", teste)
            put("p_motoboy", motoboy?.let { JsonPrimitive(it) } ?: JsonNull)
            put("p_ate", ateMs?.let { JsonPrimitive(Instant.ofEpochMilli(it).toString()) } ?: JsonNull)
        }).exigirOk(::traduzir).objeto()
        (r?.int("corridas") ?: 0) to (r?.int("pings") ?: 0)
    }.getOrNull()

    /** Apaga de uma vez o histórico (de todos ou de um motoboy) do modo teste ou do real. Devolve (corridas, pontos). */
    suspend fun apagarHistorico(senha: String, teste: Boolean, corridas: Boolean, percursos: Boolean, motoboy: String?, ateMs: Long?): Result<Pair<Int, Int>> = acao {
        val r = PwCore.rpc.chamar("apagar_historico", buildJsonObject {
            put("p_senha", senha); put("p_teste", teste); put("p_corridas", corridas); put("p_percursos", percursos)
            put("p_motoboy", motoboy?.let { JsonPrimitive(it) } ?: JsonNull)
            put("p_ate", ateMs?.let { JsonPrimitive(Instant.ofEpochMilli(it).toString()) } ?: JsonNull)
            put("p_motivo", "Limpeza de histórico pelo app")
        }).exigirOk(::traduzir).objeto()
        r?.str("erro")?.let { e ->
            val rest = r.int("restantes")
            throw ErroServidor(e, if (e == "senha_incorreta" && rest != null) "Senha incorreta. Restam $rest tentativa${if (rest == 1) "" else "s"}." else traduzir(e) ?: e)
        }
        val arquivos = r?.strs("arquivos").orEmpty()
        if (arquivos.isNotEmpty()) tentar { PwCore.rpc.removerArquivos("comprovantes", arquivos) }
        (r?.int("corridas") ?: 0) to (r?.int("pings") ?: 0)
    }

    suspend fun eventos(corridaId: String): List<EventoG> = tentar {
        PwCore.rpc.rest("GET", "/rest/v1/eventos_corrida?select=tipo,dados,criado_em&corrida_id=eq.$corridaId&order=criado_em").exigirOk().lista()
            .map { EventoG(it.str("tipo").orEmpty(), (it["dados"] as? JsonObject) ?: JsonObject(emptyMap()), iso(it.str("criado_em")) ?: 0) }
    }.getOrDefault(emptyList())

    suspend fun linkComprovante(caminho: String): String? = tentar { PwCore.rpc.linkAssinado("comprovantes", caminho) }.getOrNull()

    suspend fun clientesAvulsos(): List<String> = tentar {
        PwCore.rpc.chamar("clientes_avulsos", JsonObject(emptyMap())).exigirOk().lista().mapNotNull { it.str("nome") }
    }.getOrDefault(emptyList())

    suspend fun concluidas(desdeMs: Long, teste: Boolean): List<CorridaG> = tentar {
        PwCore.rpc.rest("GET", "/rest/v1/corridas?select=*&status=eq.concluida&teste=eq.$teste&concluida_em=gte.${enc(Instant.ofEpochMilli(desdeMs).toString())}&order=concluida_em.desc")
            .exigirOk().lista().map { it.paraCorridaG() }
    }.getOrDefault(emptyList())

    // ------------------------------------------------------------------ empresas

    suspend fun salvarEmpresa(id: String?, linha: JsonObject, favoritos: List<String>): Result<String> = acao {
        val r = if (id == null) PwCore.rpc.rest("POST", "/rest/v1/empresas", linha.toString(), "return=representation")
                else PwCore.rpc.rest("PATCH", "/rest/v1/empresas?id=eq.$id", linha.toString(), "return=representation")
        r.exigirOk { if (it.contains("duplicate") || it == "23505") "Já existe empresa com esse CNPJ." else null }
        val novo = r.lista().firstOrNull()?.str("id") ?: id ?: error("sem id")
        PwCore.rpc.rest("DELETE", "/rest/v1/empresa_favoritos?empresa_id=eq.$novo").exigirOk()
        if (favoritos.isNotEmpty()) {
            val corpo = JsonArray(favoritos.mapIndexed { i, m -> buildJsonObject { put("empresa_id", novo); put("motoboy_id", m); put("ordem", i) } })
            PwCore.rpc.rest("POST", "/rest/v1/empresa_favoritos", corpo.toString()).exigirOk()
        }
        novo
    }

    suspend fun excluirEmpresa(id: String) = acao { PwCore.rpc.rest("DELETE", "/rest/v1/empresas?id=eq.$id").exigirOk(); Unit }

    data class DadosCnpj(val razaoSocial: String?, val nomeFantasia: String?, val situacao: String?, val telefone: String?, val email: String?, val endereco: String?)

    suspend fun consultarCnpj(cnpj: String): Result<DadosCnpj> = acao {
        val o = PwCore.rpc.site("/api/cnpj/$cnpj", metodo = "GET").exigirOk(::traduzir).objeto() ?: error("vazio")
        val e = o["endereco"] as? JsonObject
        val endereco = e?.let {
            listOfNotNull(listOfNotNull(it.str("logradouro"), it.str("numero")).joinToString(", ").ifBlank { null }, it.str("bairro"),
                listOfNotNull(it.str("cidade"), it.str("uf")).joinToString(" - ").ifBlank { null }).joinToString(", ")
        }
        DadosCnpj(o.str("razaoSocial"), o.str("nomeFantasia"), o.str("situacao"), o.strs("telefones").firstOrNull(), o.strs("emails").firstOrNull(), endereco)
    }

    // ------------------------------------------------------------------ equipe

    suspend fun registro(id: String): Pair<JsonObject, List<ItemRegistro>> = acao {
        val o = PwCore.rpc.chamar("registro_membro", buildJsonObject { put("p_usuario", id); put("p_limite", 500) }).exigirOk(::traduzir).objeto() ?: error("vazio")
        ((o["resumo"] as? JsonObject) ?: JsonObject(emptyMap())) to o.arr("itens").map { it.paraItemRegistro() }
    }.getOrThrow()

    suspend fun editarMembro(id: String, nome: String, papeis: List<String>, repasse: Double?) = acao {
        PwCore.rpc.chamar("editar_membro", buildJsonObject {
            put("p_usuario", id); put("p_nome", nome)
            put("p_papeis", buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
            put("p_repasse", repasse?.let { JsonPrimitive(it) } ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun criarConta(nome: String, email: String, senha: String, papeis: List<String>, repasse: Double?, empresaId: String? = null, organizacaoId: String? = null): Result<String> = acao {
        PwCore.rpc.site("/api/equipe/criar", buildJsonObject {
            put("nome", nome); put("email", email); put("senha", senha)
            empresaId?.let { put("empresa_id", it) }
            organizacaoId?.let { put("organizacao_id", it) }
            put("papeis", buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
            put("repasse", repasse?.let { JsonPrimitive(it) } ?: JsonNull)
        }).exigirOk(::traduzir).objeto()?.str("id") ?: ""
    }

    suspend fun redefinirSenha(id: String, senha: String) = acao { PwCore.rpc.site("/api/equipe/senha", buildJsonObject { put("id", id); put("senha", senha) }).exigirOk(::traduzir); Unit }
    suspend fun bloquear(id: String, bloquear: Boolean) = acao { PwCore.rpc.site("/api/equipe/bloquear", buildJsonObject { put("id", id); put("bloquear", bloquear) }).exigirOk(::traduzir); Unit }

    suspend fun excluirConta(id: String, senha: String, motivo: String?): Result<Unit> = acao {
        val r = PwCore.rpc.site("/api/equipe/excluir", buildJsonObject { put("id", id); put("senha", senha); put("motivo", motivo ?: "") })
        if (!r.ok) {
            val o = r.objeto()
            val e = o?.str("erro") ?: "falha"
            val rest = o?.int("restantes")
            throw ErroServidor(e, if (e == "senha_incorreta" && rest != null) "Senha incorreta. Restam $rest tentativa${if (rest == 1) "" else "s"}." else traduzir(e) ?: e)
        }
        Unit
    }

    // ------------------------------------------------------------------ configurações e perfil

    suspend fun salvarConfig(chave: String, valor: JsonElement) = acao {
        PwCore.rpc.chamar("salvar_ajuste", buildJsonObject { put("p_chave", chave); put("p_valor", valor) }).exigirOk(::traduzir)
        _dados.value = _dados.value.copy(config = _dados.value.config + (chave to valor))
        Unit
    }

    // ------------------------------------------------------------------ Dev

    data class ModuloDev(val chave: String, val nome: String, val descricao: String?, val grupo: String, val ligado: Boolean, val papeis: List<String>)

    suspend fun modulos(): Result<List<ModuloDev>> = acao {
        PwCore.rpc.rest("GET", "/rest/v1/modulos?select=*&order=ordem").exigirOk(::traduzir).lista().map {
            ModuloDev(it.str("chave")!!, it.str("nome").orEmpty(), it.str("descricao"), it.str("grupo") ?: "geral", it.bool("ligado"), it.strs("papeis"))
        }
    }

    suspend fun definirModulo(chave: String, ligado: Boolean, papeis: List<String>) = acao {
        PwCore.rpc.chamar("definir_modulo", buildJsonObject {
            put("p_chave", chave); put("p_ligado", ligado)
            put("p_papeis", if (papeis.isEmpty()) JsonNull else buildJsonArray { papeis.forEach { add(JsonPrimitive(it)) } })
        }).exigirOk(::traduzir); Unit
    }

    /** O sistema "nu": contagens, aparelhos e o registro de exclusões. */
    suspend fun painelDev(): Result<JsonObject> = acao { PwCore.rpc.chamar("painel_dev", JsonObject(emptyMap())).exigirOk(::traduzir).objeto() ?: JsonObject(emptyMap()) }

    // ------------------------------------------------------------------ util

    // ------------------------------------------------------------------ pedidos e rotas automáticas

    /** acao: aprovar | ajustar | recusar | recalcular. [valor] = valor final do pedido (com motivo se mudar do calculado). */
    suspend fun revisarPedido(id: String, acao: String, valor: Double? = null, motivo: String? = null) = acao {
        PwCore.rpc.chamar("revisar_pedido", buildJsonObject {
            put("p_pedido", id); put("p_acao", acao)
            if (valor != null) put("p_valor", valor) else put("p_valor", JsonNull)
            if (motivo != null) put("p_motivo", motivo) else put("p_motivo", JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun cancelarPedido(id: String, motivo: String?) = acao {
        PwCore.rpc.chamar("cancelar_pedido", buildJsonObject { put("p_pedido", id); if (motivo != null) put("p_motivo", motivo) else put("p_motivo", JsonNull) }).exigirOk(::traduzir); Unit
    }

    /** Tipos de pedido e categorias de produto ativos (para o formulário). */
    suspend fun catalogoPedido(): Pair<List<TipoPedidoG>, List<CategoriaG>> = tentar {
        coroutineScope {
            val t = async { PwCore.rpc.rest("GET", "/rest/v1/tipos_pedido?select=chave,nome,descricao,icone&ativo=eq.true&order=ordem").exigirOk().lista().map { it.paraTipoPedidoG() } }
            val k = async { PwCore.rpc.rest("GET", "/rest/v1/categorias_produto?select=*&ativa=eq.true&order=ordem").exigirOk().lista().map { it.paraCategoriaG() } }
            t.await() to k.await()
        }
    }.getOrDefault(emptyList<TipoPedidoG>() to emptyList())

    /** Orçamento na hora (mesmo cálculo que o pedido vai usar). */
    suspend fun orcarPrevia(dados: JsonObject): JsonObject? = tentar {
        PwCore.rpc.chamar("orcar_previa", buildJsonObject { put("dados", dados) }).exigirOk(::traduzir).objeto()
    }.getOrNull()

    suspend fun criarPedido(dados: JsonObject): Result<String> = acao {
        PwCore.rpc.chamar("criar_pedido", buildJsonObject { put("dados", dados) }).exigirOk(::traduzir).corpo.trim().trim('"')
    }

    suspend fun veiculo(id: String): VeiculoG? = tentar {
        PwCore.rpc.rest("GET", "/rest/v1/motoboys?select=veiculo_tipo,veiculo_modelo,veiculo_placa,veiculo_cor,veiculo_ano,cnh_numero,cnh_categoria,cnh_validade&id=eq.$id")
            .exigirOk().lista().firstOrNull()?.paraVeiculoG()
    }.getOrNull()

    /** null em [id] = o próprio motoboy (se a regra deixar). */
    suspend fun salvarVeiculo(id: String?, v: VeiculoG): Result<VeiculoG> = acao {
        val validade = Regex("""(\d{2})/(\d{2})/(\d{4})""").matchEntire(v.cnhValidade.trim())?.destructured?.let { (d, m, a) -> "$a-$m-$d" }
        if (v.cnhValidade.isNotBlank() && validade == null) throw Exception("Validade da CNH no formato dd/mm/aaaa.")
        PwCore.rpc.chamar("salvar_veiculo", buildJsonObject {
            if (id != null) put("p_motoboy", id) else put("p_motoboy", JsonNull)
            put("d", buildJsonObject {
                put("tipo", v.tipo); put("modelo", v.modelo); put("placa", v.placa); put("cor", v.cor); put("ano", v.ano)
                put("cnh_numero", v.cnhNumero); put("cnh_categoria", v.cnhCategoria); put("cnh_validade", validade ?: "")
            })
        }).exigirOk(::traduzir).objeto()?.paraVeiculoG() ?: v
    }

    // ------------------------------------------------------------------ genéricos (catálogos, regras, RPCs do site)

    /** Linhas de uma tabela (as regras de acesso do banco valem). [consulta] = filtros/ordem do PostgREST. */
    suspend fun tabela(nome: String, consulta: String = "select=*"): List<JsonObject> = tentar {
        PwCore.rpc.rest("GET", "/rest/v1/$nome?$consulta").exigirOk().lista()
    }.getOrDefault(emptyList())

    /** Cria (sem id) ou atualiza uma linha; devolve a linha salva. */
    suspend fun salvarLinha(nome: String, id: String?, linha: JsonObject): Result<JsonObject> = acao {
        val r = if (id == null) PwCore.rpc.rest("POST", "/rest/v1/$nome", linha.toString(), "return=representation")
                else PwCore.rpc.rest("PATCH", "/rest/v1/$nome?id=eq.$id", linha.toString(), "return=representation")
        r.exigirOk(::traduzir).lista().firstOrNull() ?: linha
    }

    suspend fun apagarLinha(nome: String, id: String) = acao { PwCore.rpc.rest("DELETE", "/rest/v1/$nome?id=eq.$id").exigirOk(::traduzir); Unit }

    /** Confere no Mercado Pago (pelo site) o último pagamento do pedido. Devolve o status. */
    suspend fun verificarPagamento(pedido: String): Result<String?> = acao {
        val r = PwCore.rpc.site("/api/pagamentos/verificar", buildJsonObject { put("pedido_id", pedido) }).exigirOk(::traduzir)
        val o = runCatching { Json.parseToJsonElement(r.corpo) as? JsonObject }.getOrNull()
        val st = (o?.get("status") as? kotlinx.serialization.json.JsonPrimitive)?.content
        atualizar()
        st
    }

    /** Chama uma função do banco e devolve o JSON da resposta. */
    suspend fun rpcJson(funcao: String, args: JsonObject = JsonObject(emptyMap())): Result<JsonElement> = acao {
        PwCore.rpc.chamar(funcao, args).exigirOk(::traduzir).let { r -> if (r.corpo.isBlank()) JsonNull else Json.parseToJsonElement(r.corpo) }
    }

    /** Exceções (peças do lego) de um cliente, motoboy ou tipo de pedido. */
    /** Tipo de pedido: as regras são da empresa (o banco guarda "empresa:tipo"); a função já resolve. */
    suspend fun politicas(escopo: String, alvo: String): Map<String, JsonElement> =
        (rpcJson("politicas_do_alvo", buildJsonObject { put("p_escopo", escopo); put("p_alvo", alvo) }).getOrNull() as? JsonObject).orEmpty()

    suspend fun contagemPoliticas(): Map<Pair<String, String>, Int> =
        ((rpcJson("contagem_politicas").getOrNull() as? kotlinx.serialization.json.JsonArray) ?: kotlinx.serialization.json.JsonArray(emptyList()))
            .mapNotNull { it as? JsonObject }
            .associate { ((it.str("escopo") ?: "") to (it.str("alvo") ?: "")) to ((it["n"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()?.toInt() ?: 0) }

    /** valor null = volta ao padrão. */
    suspend fun definirPolitica(escopo: String, alvo: String, chave: String, valor: JsonElement?) = acao {
        PwCore.rpc.chamar("definir_politica", buildJsonObject {
            put("p_escopo", escopo); put("p_alvo", alvo); put("p_chave", chave); put("p_valor", valor ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun definirRegrasPedido(id: String, regras: JsonObject) = acao {
        PwCore.rpc.chamar("definir_regras_pedido", buildJsonObject { put("p_pedido", id); put("p_regras", regras) }).exigirOk(::traduzir); Unit
    }

    /** Junta as paradas da rota [nova] no fim da rota de [motoboy] (que já está em corrida). [perguntar] = manda oferta. */
    suspend fun estenderRota(nova: String, motoboy: String, perguntar: Boolean) = acao {
        PwCore.rpc.chamar("estender_rota", buildJsonObject { put("p_nova", nova); put("p_motoboy", motoboy); put("p_perguntar", perguntar) }).exigirOk(::traduzir); Unit
    }

    /** "Mudar o cálculo" do orçamento de um pedido. */
    suspend fun editarOrcamento(pedido: String, linhas: kotlinx.serialization.json.JsonArray, percentuais: kotlinx.serialization.json.JsonArray, minimo: Double, motivo: String?) = acao {
        PwCore.rpc.chamar("editar_orcamento", buildJsonObject {
            put("p_pedido", pedido); put("p_linhas", linhas); put("p_percentuais", percentuais); put("p_minimo", minimo); put("p_motivo", motivo?.let { JsonPrimitive(it) } ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun moverEntrega(parada: String, destino: String?) = acao {
        PwCore.rpc.chamar("mover_entrega", buildJsonObject { put("p_parada", parada); put("p_destino", destino?.let { JsonPrimitive(it) } ?: JsonNull) }).exigirOk(::traduzir); Unit
    }

    suspend fun definirIcone(tipo: String, id: String, icone: String?) = acao {
        PwCore.rpc.chamar("definir_icone", buildJsonObject { put("p_tipo", tipo); put("p_id", id); put("p_icone", icone?.let { JsonPrimitive(it) } ?: JsonNull) }).exigirOk(::traduzir); Unit
    }

    suspend fun simularPosicao(motoboy: String, lat: Double, lng: Double, velocidadeMs: Double? = null, direcao: Double? = null) = acao {
        PwCore.rpc.chamar("simular_posicao", buildJsonObject {
            put("p_motoboy", motoboy); put("p_lat", lat); put("p_lng", lng)
            put("p_velocidade_ms", velocidadeMs?.let { JsonPrimitive(it) } ?: JsonNull); put("p_direcao", direcao?.let { JsonPrimitive(it) } ?: JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun confirmarPagamentoPedido(id: String, metodo: String) = acao {
        PwCore.rpc.chamar("confirmar_pagamento_pedido", buildJsonObject { put("p_pedido", id); put("p_metodo", metodo) }).exigirOk(::traduzir); Unit
    }

    suspend fun roteirizarAgora() = acao { PwCore.rpc.chamar("roteirizar_agora", JsonObject(emptyMap())).exigirOk(::traduzir); Unit }

    /** modo: quadro | melhor_primeiro | cascata; ou motoboy para entregar direto. */
    suspend fun liberarRota(id: String, modo: String?, motoboy: String? = null) = acao {
        PwCore.rpc.chamar("liberar_rota", buildJsonObject {
            put("p_corrida", id)
            if (modo != null) put("p_modo", modo) else put("p_modo", JsonNull)
            if (motoboy != null) put("p_motoboy", motoboy) else put("p_motoboy", JsonNull)
        }).exigirOk(::traduzir); Unit
    }

    suspend fun ajustarPagamento(id: String, ajuste: Double, motivo: String?) = acao {
        PwCore.rpc.chamar("ajustar_pagamento_rota", buildJsonObject { put("p_corrida", id); put("p_ajuste", ajuste); if (motivo != null) put("p_motivo", motivo) else put("p_motivo", JsonNull) }).exigirOk(::traduzir); Unit
    }

    suspend fun recalcularPagamento(id: String) = acao { PwCore.rpc.chamar("recalcular_pagamento_rota", buildJsonObject { put("p_corrida", id) }).exigirOk(::traduzir); Unit }

    private suspend fun <T> acao(bloco: suspend () -> T): Result<T> = tentar { bloco() }.recoverCatching { throw Exception(mensagem(it)) }

    private fun mensagem(e: Throwable): String = when (e) {
        is java.io.IOException -> "Sem conexão com o servidor."
        is ErroServidor -> e.message ?: e.codigo
        else -> e.message ?: "Erro desconhecido."
    }

    private val erros = mapOf(
        "apenas_gestor" to "Apenas gestores podem fazer isso.",
        "corrida_precisa_de_2_paradas" to "A corrida precisa de pelo menos uma coleta e uma entrega.",
        "primeira_parada_deve_ser_coleta" to "A primeira parada precisa ser uma coleta.",
        "escolha_o_motoboy" to "Escolha o motoboy.",
        "corrida_nao_esta_sem_motoboy" to "Essa corrida não está aguardando novo despacho.",
        "motoboy_fora_do_modo_teste" to "Esse motoboy não está com o modo teste ligado.",
        "motoboy_em_modo_teste" to "Esse motoboy está em modo teste: só recebe corridas de teste.",
        "horario_muito_proximo" to "Esse horário está perto demais. Planeje com mais antecedência ou crie para agora.",
        "datas_demais" to "No máximo 60 corridas por repetição.",
        "corrida_nao_esta_planejada" to "Essa corrida não está mais planejada.",
        "senha_incorreta" to "Senha incorreta.",
        "sem_permissao" to "Você não pode mexer no histórico dessa conta.",
        "nada_para_excluir" to "Escolha o que apagar.",
        "muitas_tentativas" to "Muitas senhas erradas. Ficou bloqueado por 15 minutos.",
        "cancele_as_corridas_em_andamento" to "Há corrida aceita, em andamento ou reservada. Cancele antes de apagar.",
        "servidor_sem_chave" to "Falta configurar a chave secreta do Supabase na Vercel (SUPABASE_SECRET_KEY).",
        "email_invalido" to "E-mail inválido.",
        "nome_invalido" to "O nome precisa ter entre 2 e 60 letras.",
        "senha_curta" to "A senha precisa ter pelo menos 8 caracteres.",
        "sem_papel" to "Marque pelo menos um papel.",
        "percentual_invalido" to "O repasse precisa ficar entre 0 e 100%.",
        "email_existe" to "Já existe uma conta com esse e-mail.",
        "nao_exclua_a_si_mesmo" to "Você não pode excluir a sua própria conta.",
        "nao_bloqueie_a_si_mesmo" to "Você não pode bloquear a sua própria conta.",
        "nao_remova_seu_proprio_acesso" to "Você não pode tirar seu próprio acesso de gestor.",
        "conta_protegida" to "Essa conta é protegida.",
        "apenas_dev" to "Sem permissão para isso.",
        "modulo_desligado" to "Essa função está desligada pelo Dev.",
        "membro_em_corrida" to "Essa pessoa está com uma corrida em andamento. Espere terminar antes.",
        "usuario_nao_encontrado" to "Conta não encontrada.",
        "nao_encontrado" to "CNPJ não encontrado na Receita.",
        "pedido_finalizado" to "Esse pedido já terminou.",
        "pedido_nao_pode_voltar" to "Esse pedido não pode voltar para a revisão agora.",
        "placa_invalida" to "Placa inválida (use AAA0A00 ou AAA0000).",
        "cnh_invalida" to "Número da CNH inválido (9 a 11 dígitos).",
        "cnh_categoria_invalida" to "Categoria da CNH inválida.",
        "so_o_gestor_edita" to "Só o gestor muda os dados do veículo.",
        "so_em_modo_teste" to "Só dá para simular motoboys em modo teste.",
        "cadastre_o_veiculo" to "Cadastre a placa do veículo antes de pegar rotas.",
        "versao_nao_autorizada" to "Esta versão do app não está liberada. Atualize.",
        "pedido_ja_faturado" to "Esse pedido já está numa fatura emitida.",
        "pedido_ja_em_rota" to "Esse pedido já saiu com o motoboy.",
        "pedido_em_andamento" to "Esse pedido já está em andamento.",
        "rota_ja_pegada" to "Um motoboy já pegou essa rota.",
        "rota_nao_pode_ser_liberada" to "Essa rota já foi entregue a um motoboy.",
        "rota_sem_motoboy" to "Essa rota ainda não tem motoboy.",
        "informe_o_motivo" to "Escreva o motivo da mudança de valor.",
        "valor_invalido" to "Valor inválido.",
        "ajuste_invalido" to "Ajuste inválido.",
        "holerite_fechado" to "O holerite desse período já foi fechado. Reabra antes de mudar.",
        "motoboy_suspenso" to "Esse motoboy está suspenso por penalidades.",
        "cliente_bloqueado" to "Esse cliente está bloqueado por penalidades.",
        "chamado_ja_respondido" to "Esse pedido de cancelamento já foi respondido.",
        "chamado_nao_encontrado" to "Pedido de cancelamento não encontrado.",
        "pontos_invalidos" to "Pontos inválidos (1 a 1000).",
        "penalidade_nao_encontrada" to "Penalidade não encontrada (pode já ter sido perdoada).",
        "ja_coletou" to "O motoboy já coletou: não dá mais para passar a rota.",
        "corrida_sem_motoboy" to "Essa rota não está com nenhum motoboy agora.",
        "mesmo_motoboy" to "A rota já está com esse motoboy.",
        "passar_desligado" to "Passar rota está desligado em Ajustes → Despacho.",
        "compartilhar_desligado" to "A plataforma não liberou compartilhar motoboys entre empresas.",
        "modo_invalido" to "Opção inválida.",
        "limite_motoboys" to "Seu plano chegou ao limite de motoboys.",
        "limite_gestores" to "Seu plano chegou ao limite de gestores.",
        "limite_clientes" to "Seu plano chegou ao limite de clientes.",
        "limite_gestores_do_plano" to "A empresa já tem o máximo de gestores. Fale com o suporte da plataforma para liberar mais.",
        "limite_supervisores_do_plano" to "A empresa já tem o máximo de supervisores. Fale com o suporte da plataforma para liberar mais.",
        "limite_motoboys_do_plano" to "A empresa já tem o máximo de motoboys do plano. Fale com o suporte da plataforma para liberar mais.",
        "acao_bloqueada" to "A plataforma não liberou isso para gestores.",
        "ajuste_bloqueado" to "Este ajuste está travado pela plataforma.",
        "apenas_dev_cria_gestor" to "Só o suporte da plataforma cria contas de gestor.",
        "holerite_fechado" to "O holerite deste motoboy já foi fechado: lance um ajuste no holerite.",
        "pag_token_invalido" to "Esse token não parece do Mercado Pago (começa com APP_USR- ou TEST-).",
        "rota_compartilhada_desligada" to "Abrir rota pelo chat está desligado.",
    )

    fun traduzir(codigo: String): String? = erros[codigo]
}

/** Acesso rápido a valores de configuração com padrão. */
fun Map<String, JsonElement>.numero(k: String, padrao: Double) = (this[k] as? JsonPrimitive)?.content?.toDoubleOrNull() ?: padrao
fun Map<String, JsonElement>.logico(k: String, padrao: Boolean) = (this[k] as? JsonPrimitive)?.content?.toBooleanStrictOrNull() ?: padrao

fun Map<String, JsonElement>.textoCfg(k: String, padrao: String) = (this[k] as? JsonPrimitive)?.content ?: padrao
