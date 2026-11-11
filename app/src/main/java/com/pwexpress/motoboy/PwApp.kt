package com.pwexpress.motoboy

import android.app.Application
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.notificacoes.CorridaEmCurso
import com.pwexpress.core.notificacoes.PwNotificacoes
import com.pwexpress.core.notificacoes.ResumoCorrida
import com.pwexpress.core.update.Atualizador
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.StatusParada
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.tracking.Tracking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class PwApp : Application() {

    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        TemaApp.init(this)
        com.pwexpress.core.designsystem.PwTour.init(this)
        PwCore.init(this)
        Tracking.init(this, BuildConfig.VERSION_NAME)
        Corridas.init(this)
        Atualizador.init(this, BuildConfig.VERSION_CODE)
        com.pwexpress.core.marca.MarcaEmpresa.iniciar(this)
        PwNotificacoes.criarCanais(this)
        ligarModulos()
    }

    /** Único ponto onde os módulos se conhecem. */
    private fun ligarModulos() {
        // modo teste → cor das notificações (violeta)
        escopo.launch { TemaApp.modoTeste.collect { PwNotificacoes.modoTeste = it } }
        // corrida ativa (feature:corridas) → notificação ao vivo (serviço do GPS, feature:tracking)
        escopo.launch { Corridas.ativa.collect { CorridaEmCurso.publicar(it?.let(::resumo)) } }
        // corrida ativa (feature:corridas) → modo do GPS (feature:tracking)
        escopo.launch {
            combine(Corridas.ativa, Corridas.carregou) { ativa, carregou -> carregou to ativa?.id }
                .distinctUntilChanged()
                .collect { (carregou, corridaId) ->
                    if (!carregou) return@collect
                    if (corridaId != null) Tracking.entrarEmCorrida(corridaId) else {
                        Tracking.sairDaCorrida()
                        Perfis.carregar() // corrida concluída: atualiza ganhos
                    }
                }
        }
        // oferta achada pela checagem (push atrasou ou falhou): toca alto e acende a tela do mesmo jeito.
        // Some do servidor (expirou, outro pegou)? Para de tocar.
        escopo.launch {
            var anterior: String? = null
            Corridas.oferta.collect { o ->
                if (o == null) {
                    if (anterior != null) runCatching { com.pwexpress.feature.corridas.push.Alarme.pararOferta(this@PwApp) }
                    anterior = null
                    return@collect
                }
                anterior = o.id
                if (com.pwexpress.feature.corridas.push.Alarme.ofertaCompleta != o.id)
                    runCatching { com.pwexpress.feature.corridas.push.Alarme.mostrarOferta(this@PwApp, o) }
            }
        }
        // chat em segundo plano (reserva do push): mensagem nova numa conversa com o chat fechado → avisa
        escopo.launch {
            val vistas = HashMap<String, Int>()
            var primeira = true
            com.pwexpress.core.chat.Chat.conversas.collect { lista ->
                if (PwCore.auth.sessao.value == null) { vistas.clear(); primeira = true; return@collect }
                for (cv in lista) {
                    val antes = vistas[cv.id] ?: if (primeira) cv.naoLidas else 0
                    val recente = (PwNotificacoes.chatAvisadoEm[cv.id] ?: 0L) > System.currentTimeMillis() - 90_000
                    if (!primeira && cv.naoLidas > antes && !cv.silenciada && !cv.ultimaMinha && !recente && !com.pwexpress.core.chat.Chat.abrir.value)
                        runCatching { PwNotificacoes.mensagemChat(this@PwApp, cv.id, cv.nome, cv.previa ?: "Nova mensagem") }
                    vistas[cv.id] = cv.naoLidas
                }
                if (lista.isNotEmpty()) primeira = false
            }
        }
        // checagem em segundo plano: com o GPS ligado (serviço em primeiro plano) o app segue vivo,
        // então confere corrida, oferta e cancelamentos mesmo com a tela apagada (o push pode atrasar)
        escopo.launch {
            var ciclo = 0
            while (true) {
                val modo = Tracking.info.value.modo
                if (PwCore.auth.sessao.value != null && modo != com.pwexpress.feature.tracking.Modo.OFFLINE) {
                    runCatching { Corridas.sincronizarAgora() }
                    if (ciclo % 6 == 0) runCatching { Corridas.carregarPlanejadas(); Corridas.carregarRotas() }
                    if (ciclo % 3 == 0) runCatching { com.pwexpress.core.chat.Chat.carregar() }
                    ciclo++
                }
                kotlinx.coroutines.delay(if (modo == com.pwexpress.feature.tracking.Modo.OFFLINE) 12_000 else 7_000)
            }
        }
        // configurações do servidor (core:perfil) → ritmo do GPS (feature:tracking)
        escopo.launch {
            Perfis.perfil.collect { p ->
                // modo teste: o app inteiro fica violeta
                TemaApp.definirModoTeste(p?.modoTeste == true)
                if (p == null) return@collect
                // Dev com tema vermelho
                TemaApp.definirVermelho(p.dev && p.corTema == "vermelho")
                // "Logar com": mantém nome e foto atualizados na conta salva
                PwCore.auth.sessao.value?.email?.let { PwCore.auth.contas.atualizarPerfil(it, p.nome, p.fotoUrl) }
                Tracking.aplicarConfiguracoes(
                    corridaS = p.numero("intervalo_corrida_s", 45.0),
                    economiaS = p.numero("intervalo_economia_s", 90.0),
                    disponivelMin = p.numero("disponivel_envio_min", 3.0),
                    disponivelM = p.numero("disponivel_distancia_m", 300.0),
                    distanciaM = p.numero("ping_distancia_m", 20.0),
                    precisaoM = p.numero("ping_precisao_max_m", 60.0),
                    envioS = p.numero("ping_envio_s", 12.0),
                    inteligente = p.logico("ping_inteligente", true),
                    curvaGraus = p.numero("ping_curva_graus", 25.0),
                    retaMaxM = p.numero("ping_reta_max_m", 200.0),
                )
                // conquistas e dados do aparelho já prontos (o menu da foto abre na hora)
                if (p != null) escopo.launch { com.pwexpress.core.perfil.Conquistas.atualizarMinhas(10 * 60_000) }
                if (p?.motoboy == true) escopo.launch { com.pwexpress.motoboy.Aparelho.registrar(this@PwApp) }
                com.pwexpress.core.mapa.PreferenciaRota.definir(p.texto("rota_provedor", "valhalla"), p.texto("rota_perfil", "motorcycle"))
                com.pwexpress.core.mapa.PreferenciasNavegacao.definir(
                    ligada = p.logico("navegacao_ligada", true), voz = p.logico("navegacao_voz", true),
                    recalcularM = p.numero("navegacao_recalcular_m", 60.0), naNotificacao = p.logico("navegacao_na_notificacao", true),
                )
            }
        }
        // login / logout
        escopo.launch {
            PwCore.auth.sessao.collect { sessao ->
                if (sessao != null) {
                    Corridas.aoEntrar()
                    launch { Perfis.carregar() }
                } else {
                    Corridas.limpar()
                    Perfis.limpar()
                    Gestao.limpar()
                    TemaApp.definirVermelho(false)
                }
            }
        }
    }

    private fun resumo(c: Corrida): ResumoCorrida {
        val atual = c.paradaAtual
        val feitas = c.paradas.count { it.status == StatusParada.CONCLUIDA }
        val titulo = when {
            atual == null -> "Finalizando"
            atual.status == StatusParada.CHEGOU -> if (atual.tipo == TipoParada.COLETA) "No local da coleta" else "No local da entrega"
            atual.tipo == TipoParada.COLETA -> "A caminho da coleta"
            else -> "A caminho da entrega" + if (c.paradas.count { it.tipo == TipoParada.ENTREGA } > 1) " ${c.paradas.filter { it.tipo == TipoParada.ENTREGA }.indexOf(atual) + 1}" else ""
        }
        return ResumoCorrida(
            numero = c.numeroExibicao,
            titulo = titulo,
            endereco = atual?.endereco.orEmpty(),
            etapaAtual = feitas,
            etapas = c.paradas.map { it.tipo == TipoParada.COLETA },
            distanciaM = null,
            valor = c.valorMotoboy?.let { java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(it) },
            teste = c.teste,
        )
    }
}
