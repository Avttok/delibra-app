package com.pwexpress.motoboy

import androidx.lifecycle.lifecycleScope


import android.content.Context
import androidx.compose.foundation.layout.navigationBarsPadding
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.pwexpress.core.designsystem.PwTour
import com.pwexpress.core.designsystem.PwTourCamada
import com.pwexpress.core.designsystem.alvoTour
import com.pwexpress.motoboy.ui.ToursApp
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.PwAba
import com.pwexpress.core.designsystem.PwBarraAbas
import com.pwexpress.core.designsystem.PwFundo
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwTheme
import com.pwexpress.core.designsystem.PwTopo
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.update.Atualizador
import com.pwexpress.feature.auth.LoginScreen
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.ui.CartaoOfertas
import com.pwexpress.feature.corridas.ui.CorridaScreen
import com.pwexpress.feature.corridas.ui.OfertaScreen
import com.pwexpress.feature.corridas.ui.PlanejadasScreen
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.ui.GestorApp
import com.pwexpress.feature.gestor.ui.GestorNav
import com.pwexpress.feature.perfil.CartaoGanhos
import com.pwexpress.feature.perfil.Linha
import com.pwexpress.feature.perfil.NomePerfil
import com.pwexpress.feature.perfil.PerfilScreen
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.ui.TrackingScreen
import com.pwexpress.motoboy.ui.AcoesTopo
import com.pwexpress.motoboy.ui.JanelaAtualizacao
import com.pwexpress.motoboy.ui.JanelaConta
import com.pwexpress.motoboy.ui.Painel
import com.pwexpress.motoboy.ui.PrimeiraConfiguracao
import com.pwexpress.motoboy.ui.lerPreparo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    /** Pedido vindo de uma notificação (ex.: abrir a aba Planejadas). */
    private val abrirAba = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // celular: sempre em pé. Óculos (Meta Quest) e telas grandes: janela livre, o app se ajusta.
        val oculos = android.os.Build.MANUFACTURER.equals("Oculus", true) || android.os.Build.MANUFACTURER.equals("Meta", true) ||
            packageManager.hasSystemFeature("oculus.software.handtracking") || packageManager.hasSystemFeature("com.oculus.feature.PASSTHROUGH")
        val grande = resources.configuration.smallestScreenWidthDp >= 600
        if (!oculos && !grande) requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        abrirAba.value = intent?.getStringExtra("pwex_abrir")
        val prefs = getSharedPreferences("pwex_painel", Context.MODE_PRIVATE)
        setContent {
            PwTheme {
                PwFundo {
                  // janela larga (óculos VR, tablet, paisagem): o conteúdo fica numa coluna central legível
                  androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                   val larga = maxWidth > 760.dp
                   Box(if (larga) Modifier.fillMaxHeight().widthIn(max = 720.dp) else Modifier.fillMaxSize()) {
                    val sessao by PwCore.auth.sessao.collectAsState()
                    val oferta by Corridas.oferta.collectAsState()
                    val ativa by Corridas.ativa.collectAsState()
                    val planejadas by Corridas.planejadas.collectAsState()
                    val rotasQuadro by Corridas.rotas.collectAsState()
                    val info by Tracking.info.collectAsState()
                    val perfil by Perfis.perfil.collectAsState()
                    var noPerfil by remember { mutableStateOf(false) }
                    var naConfig by remember { mutableStateOf(false) }
                    // o raio tem janela própria (atalho "Sem raio definido" / "Raio N km" no Início)
                    var raioAberto by remember { mutableStateOf(false) }
                    if (raioAberto) com.pwexpress.motoboy.ui.JanelaRaio(onFechar = { raioAberto = false })
                    var simulador by remember { mutableStateOf(false) }
                    var conta by remember { mutableStateOf(false) }
                    // rota/pedido aberto do chat (motoboy) e perfil de alguém (de qualquer lugar)
                    var rotaChat by remember { mutableStateOf<Pair<String, String>?>(null) }
                    rotaChat?.let { (t, i) -> com.pwexpress.feature.corridas.ui.RotaCompartilhadaTela(t, i, onFechar = { rotaChat = null }) }
                    val perfilAberto by com.pwexpress.core.perfil.PerfilAberto.id.collectAsState()
                    perfilAberto?.let { pid -> com.pwexpress.core.perfil.PerfilPublicoTela(pid, onFechar = { com.pwexpress.core.perfil.PerfilAberto.id.value = null }) }
                    var aba by com.pwexpress.core.designsystem.lembrarPagina("motoboy_aba", "inicio")
                    var painel by remember { mutableStateOf(runCatching { Painel.valueOf(prefs.getString("painel", "MOTOBOY")!!) }.getOrDefault(Painel.MOTOBOY)) }
                    val scope = rememberCoroutineScope()
                    val ciclo = LocalLifecycleOwner.current
                    val ctx = LocalContext.current
                    var preparado by remember { mutableStateOf(lerPreparo(ctx).completo) }
                    // depois de entrar, espera o perfil (papéis) antes de decidir o lado do app
                    var esperouPerfil by remember { mutableStateOf(false) }
                    LaunchedEffect(sessao?.email) { esperouPerfil = false; delay(8_000); esperouPerfil = true }
                    // splash: segura na logo até puxar o essencial (perfil + dados do painel do gestor)
                    var dadosProntos by remember { mutableStateOf(false) }
                    LaunchedEffect(sessao?.email, perfil != null) {
                        val pf = perfil ?: return@LaunchedEffect
                        if (dadosProntos) return@LaunchedEffect
                        kotlinx.coroutines.withTimeoutOrNull(7_000) {
                            if (pf.gestor) Gestao.atualizar()
                            if (pf.motoboy) { Corridas.carregarRotas(); Corridas.carregarPlanejadas() }
                        }
                        dadosProntos = true
                    }

                    LaunchedEffect(abrirAba.value) { abrirAba.value?.let {
                        if (it == "planejadas" || it == "rotas") { aba = "corridas"; painel = Painel.MOTOBOY; naConfig = false }
                        // avisos do gestor: abre o painel do gestor no pedido / na rota
                        if (it.startsWith("gestor:")) {
                            painel = Painel.GESTOR; prefs.edit { putString("painel", Painel.GESTOR.name) }; naConfig = false; noPerfil = false
                            val partes = it.split(":")
                            when (partes.getOrNull(1)) {
                                "pedido" -> partes.getOrNull(2)?.takeIf { id -> id.isNotBlank() }?.let { id -> GestorNav.abrirPedido(id) }
                                "corrida" -> partes.getOrNull(2)?.takeIf { id -> id.isNotBlank() }?.let { id -> GestorNav.abrir(com.pwexpress.feature.gestor.ui.TelaG.Corrida(id)) }
                            }
                        }
                        if (it.startsWith("chat")) { it.substringAfter("chat:", "").takeIf { c -> c.isNotBlank() }?.let { c -> com.pwexpress.core.chat.Chat.conversaPedida.value = c }; com.pwexpress.core.chat.Chat.abrir.value = true }
                        abrirAba.value = null } }
                    val chatAberto by com.pwexpress.core.chat.Chat.abrir.collectAsState()
                    val chatLigado = perfil?.logico("chat_ligado", true) != false && perfil?.modulo("chat") != false
                    BackHandler(enabled = chatAberto) { com.pwexpress.core.chat.Chat.abrir.value = false }
                    // não lidas do chat (o número no topo), com o app aberto
                    LaunchedEffect(sessao != null, chatLigado) {
                        if (sessao == null || !chatLigado) return@LaunchedEffect
                        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { com.pwexpress.core.chat.Chat.carregar(); delay(30_000) } }
                    }

                    // se algo for desligado depois, a tela de preparo volta
                    LaunchedEffect(Unit) { ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { preparado = lerPreparo(ctx).completo } }

                    // reserva caso o push falhe: com o app aberto e disponível, consulta a cada 10 s
                    LaunchedEffect(sessao != null, info.modo) {
                        if (sessao == null || info.modo != Modo.DISPONIVEL) return@LaunchedEffect
                        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) { Corridas.sincronizarAgora(); delay(6_000) }
                        }
                    }
                    // planejadas: contador da aba sempre em dia
                    LaunchedEffect(sessao != null, perfil?.motoboy) {
                        if (sessao == null || perfil?.motoboy != true) return@LaunchedEffect
                        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) { while (true) { Corridas.carregarPlanejadas(); Corridas.carregarRotas(); delay(15_000) } }
                    }

                    BackHandler(enabled = noPerfil) { noPerfil = false }
                    BackHandler(enabled = naConfig && !noPerfil) { naConfig = false }
                    BackHandler(enabled = !noPerfil && aba != "inicio" && painel == Painel.MOTOBOY) { aba = "inicio" }

                    val s = sessao
                    val p = perfil
                    val podeTrocar = p?.gestor == true && p.motoboy
                    // gestor sem papel de motoboy vai direto para o painel
                    val efetivo = when {
                        p?.gestor == true && !p.motoboy -> Painel.GESTOR
                        p?.gestor == true -> painel
                        else -> Painel.MOTOBOY
                    }
                    fun trocar(novo: Painel) {
                        painel = novo
                        prefs.edit { putString("painel", novo.name) }
                        conta = false
                    }
                    fun sair() {
                        conta = false; noPerfil = false
                        com.pwexpress.core.chat.Chat.limpar(); com.pwexpress.core.chat.Chat.abrir.value = false
                        Gestao.limpar()
                        scope.launch { Tracking.sairDaConta() }
                    }
                    val conteudo = Modifier.safeDrawingPadding()
                    val o = oferta
                    val a = ativa

                    val doisPassos by com.pwexpress.core.auth.DoisPassos.pendente.collectAsState()
                    LaunchedEffect(sessao?.userId) { com.pwexpress.core.auth.DoisPassos.pendente.value = null; if (sessao != null) runCatching { com.pwexpress.core.auth.DoisPassos.conferir() }.onFailure { com.pwexpress.core.auth.DoisPassos.pendente.value = "" } }
                    when {
                        s == null -> LoginScreen(conteudo)
                        !doisPassos.isNullOrEmpty() -> com.pwexpress.core.auth.PedirCodigoTela(doisPassos!!, onSair = ::sair, modifier = conteudo)
                        (p == null || !dadosProntos) && !esperouPerfil -> com.pwexpress.motoboy.ui.SplashMarca(if (p == null) "Carregando sua conta…" else "Preparando tudo…")
                        p == null && !esperouPerfil -> Box(conteudo.fillMaxSize(), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                com.pwexpress.core.designsystem.PwLogoGrande(170.dp)
                                com.pwexpress.core.designsystem.PwCarregando(texto = "Carregando sua conta…")
                            }
                        }
                        p?.empresaBloqueada != null -> Box(conteudo.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                            // empresa suspensa/cancelada pelo Dev: ninguém da equipe trabalha até reativar
                            LaunchedEffect(Unit) { runCatching { Tracking.definirModo(Modo.OFFLINE) } }
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                                com.pwexpress.core.designsystem.PwLogoGrande(130.dp)
                                com.pwexpress.core.designsystem.PwVazio(
                                    (if (p.empresaBloqueada == "cancelada") "O acesso desta empresa foi encerrado." else "O acesso desta empresa está suspenso.") +
                                        (p.empresaBloqueadaMotivo?.let { "\nMotivo: $it" } ?: "") + "\nFale com o suporte da plataforma para reativar.",
                                    icone = PwIcones.Lock,
                                )
                                com.pwexpress.core.designsystem.PwBotaoVidro("Tentar de novo", onClick = { scope.launch { Perfis.carregar() } }, icone = PwIcones.RefreshCw)
                                com.pwexpress.core.designsystem.PwBotaoVidro("Sair da conta", onClick = ::sair, icone = com.pwexpress.core.designsystem.PwIcones.LogOut)
                            }
                        }
                        p?.soCliente == true -> Box(conteudo.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                com.pwexpress.core.designsystem.PwLogoGrande(150.dp)
                                com.pwexpress.core.designsystem.PwVazio(
                                    "Sua conta é de cliente. Pedidos, pagamentos e acompanhamento ficam no site — o app é para motoboys e gestores.",
                                )
                                com.pwexpress.core.designsystem.PwBotaoVidro("Sair da conta", onClick = ::sair, icone = com.pwexpress.core.designsystem.PwIcones.LogOut)
                            }
                        }
                        noPerfil -> PerfilScreen(
                            email = s.email,
                            onVoltar = { noPerfil = false },
                            onSair = ::sair,
                            modifier = conteudo,
                            diagnostico = {
                                Linha("Estado do GPS", info.modo.rotulo)
                                Linha("Pontos na fila", info.naFila.toString())
                                Linha("Último erro", info.erro ?: "nenhum")
                            },
                        )
                        chatAberto && chatLigado -> com.pwexpress.core.chat.ChatScreen(
                            meuId = p?.id, gestor = efetivo == Painel.GESTOR, dev = p?.dev == true, onVoltar = { com.pwexpress.core.chat.Chat.abrir.value = false }, modifier = conteudo,
                            onAbrirAnexo = { an ->
                                val id = an.id
                                if (efetivo == Painel.GESTOR && id != null) {
                                    when (an.tipo) {
                                        "rota" -> { com.pwexpress.core.chat.Chat.abrir.value = false; GestorNav.abrir(com.pwexpress.feature.gestor.ui.TelaG.Corrida(id)) }
                                        "cliente" -> { com.pwexpress.core.chat.Chat.abrir.value = false; GestorNav.abrir(com.pwexpress.feature.gestor.ui.TelaG.Empresa(id)) }
                                        "pessoa" -> { com.pwexpress.core.chat.Chat.abrir.value = false; GestorNav.abrir(com.pwexpress.feature.gestor.ui.TelaG.Membro(id)) }
                                        "pedido" -> { com.pwexpress.core.chat.Chat.abrir.value = false; GestorNav.abrirPedido(id) }
                                    }
                                }
                                else if (id != null) when (an.tipo) {
                                    // motoboy: vê a rota/pedido como no quadro e responde se pode; pessoa abre o perfil
                                    "rota", "pedido" -> { rotaChat = an.tipo to id }
                                    "pessoa" -> { com.pwexpress.core.perfil.PerfilAberto.abrir(id) }
                                }
                                if (an.tipo == "local" && an.lat != null && an.lng != null) runCatching {
                                    ctx.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("geo:${an.lat},${an.lng}?q=${an.lat},${an.lng}")))
                                }
                            },
                        )
                        efetivo == Painel.GESTOR -> androidx.compose.runtime.CompositionLocalProvider(
                            com.pwexpress.core.designsystem.LocalMinhasChavesPix provides { com.pwexpress.feature.corridas.ui.CartaoChavesPix() },
                        ) {
                            GestorApp(
                                modifier = conteudo,
                                topo = { PwTopo { AcoesTopo(efetivo, podeTrocar, ::trocar) { conta = true } } },
                                // gestor + motoboy com corrida ou oferta: faixa embaixo do topo para voltar (não cobre o mapa nem a gaveta)
                                faixa = if (podeTrocar && (a != null || o != null)) ({
                                    com.pwexpress.motoboy.ui.FaixaVoltarCorrida(
                                        if (o != null) "Corrida nova! Ver a oferta" else "Você está na corrida ${a?.numeroExibicao ?: ""}".trim(),
                                        destaque = o != null, onClick = { trocar(Painel.MOTOBOY) })
                                }) else null,
                                meuId = p?.id,
                            )
                        }
                        o != null -> OfertaScreen(o, onFim = { }, modifier = conteudo)
                        !preparado -> PrimeiraConfiguracao(onPronto = { preparado = true }, modifier = conteudo)
                        a != null -> CorridaScreen(a, admin = p?.admin == true, modifier = conteudo, chatLigado = chatLigado,
                            onPainelGestor = if (podeTrocar) ({ trocar(Painel.GESTOR) }) else null)
                        naConfig && efetivo == Painel.MOTOBOY -> com.pwexpress.motoboy.ui.ConfigMotoApp(onVoltar = { naConfig = false }, modifier = conteudo)
                        else -> Box(conteudo.fillMaxSize()) {
                            val comPlanejadas = p?.modulo("corridas_planejadas") != false
                            val comCarteira = p?.modulo("carteira") != false && p?.holeriteVisivel != false
                            val abaMotoboy = when { aba == "corridas" -> "corridas"; aba == "carteira" && comCarteira -> "carteira"; else -> "inicio" }
                            androidx.compose.foundation.layout.Column(Modifier.fillMaxSize()) {
                                PwTopo { AcoesTopo(efetivo, podeTrocar, ::trocar) { conta = true } }
                                Box(Modifier.weight(1f).padding(bottom = 86.dp)) {
                                    when (abaMotoboy) {
                                        "corridas" -> PlanejadasScreen(onIniciada = { aba = "inicio" }, comPlanejadas = comPlanejadas)
                                        "carteira" -> com.pwexpress.feature.corridas.ui.CarteiraScreen()
                                        else -> TrackingScreen(
                                            saudacao = NomePerfil(s.email).substringBefore(" "),
                                            extras = {
                                                androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)) {
                                                    com.pwexpress.feature.corridas.ui.AvisoSuspensao()
                                                    CartaoGanhos(onCarteira = if (comCarteira) ({ aba = "carteira" }) else null)
                                                    com.pwexpress.feature.corridas.ui.MinhaNota()
                                                }
                                            },
                                            chips = { com.pwexpress.feature.tracking.ui.ChipInicio(PwIcones.Radar, com.pwexpress.motoboy.ui.textoRaio(p?.raioKm, p?.raioModo ?: "posicao")) { raioAberto = true } },
                                            onConfiguracoes = { naConfig = true },
                                        )
                                    }
                                    // emergência sempre à mão no início (segurar 2 s + confirmar)
                                    if (abaMotoboy == "inicio") com.pwexpress.feature.corridas.ui.BotaoEmergencia(Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 14.dp))
                                }
                            }
                            val reservas = planejadas.count { it.minha }
                            val livres = (rotasQuadro.map { it.id } + planejadas.filter { !it.minha && it.status == "agendada" }.map { it.id }).toSet().size
                            PwBarraAbas(
                                listOfNotNull(
                                    PwAba("inicio", "Início", PwIcones.House),
                                    PwAba("corridas", "Rotas", PwIcones.Route, livres + if (comPlanejadas) reservas else 0),
                                    if (comCarteira) PwAba("carteira", "Holerite", PwIcones.ReceiptText) else null,
                                ),
                                abaMotoboy, { aba = it },
                                Modifier.align(Alignment.BottomCenter).alvoTour("abas"),
                            )
                        }
                    }

                    if (conta && s != null) {
                        JanelaConta(
                            s.email, efetivo, podeTrocar,
                            onFechar = { conta = false },
                            onPerfil = { conta = false; noPerfil = true },
                            onConfiguracoes = { conta = false; if (efetivo == Painel.GESTOR) GestorNav.abrirConfiguracoes() else naConfig = true },
                            onTrocar = ::trocar, onSair = ::sair,
                            onDev = { conta = false; if (efetivo != Painel.GESTOR) trocar(Painel.GESTOR); GestorNav.abrirDev() },
                        )
                    }
                    // simulador de GPS: só no modo teste, por cima de tudo (dá para usar no meio da rota)
                    val simEstado by com.pwexpress.feature.tracking.Simulador.estado.collectAsState()
                    val podeSimular = s != null && perfil?.modoTeste == true && perfil?.motoboy == true && painel == Painel.MOTOBOY &&
                        perfil?.logico("simulador_ligado", true) != false
                    if (podeSimular) {
                        com.pwexpress.core.designsystem.PwPilula(
                            if (simEstado.ativo) (if (simEstado.andando) "Simulando…" else "Simulador ligado") else "Simulador GPS",
                            PwIcones.FlaskConical, { simulador = true },
                            Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 14.dp, bottom = 100.dp),
                        )
                    } else if (simEstado.ativo) LaunchedEffect(Unit) { com.pwexpress.feature.tracking.Simulador.desligar() }
                    if (simulador && podeSimular) com.pwexpress.motoboy.ui.SimuladorScreen(onFechar = { simulador = false }, kmhPadrao = perfil?.numero("simulador_velocidade_kmh", 35.0) ?: 35.0)
                    if (perfil?.dev == true && s != null) com.pwexpress.motoboy.ui.SeloModoDev(Modifier.align(Alignment.CenterStart).padding(bottom = 120.dp))
                    com.pwexpress.motoboy.ui.JanelaModoDev()
                    JanelaAtualizacao()
                    LaunchedEffect(Unit) { while (true) { delay(15 * 60_000L); Atualizador.verificarObrigatoria() } }
                    // tour na primeira vez de cada painel (depois, no menu da conta)
                    LaunchedEffect(efetivo, s != null, preparado) {
                        if (s == null || !preparado) return@LaunchedEffect
                        delay(1_200)
                        if (efetivo == Painel.GESTOR) PwTour.primeiraVez("gestor-app", ToursApp.gestor) else PwTour.primeiraVez("motoboy", ToursApp.motoboy)
                    }
                    BackHandler(enabled = PwTour.aberto) { PwTour.fechar() }
                    PwTourCamada()
                    com.pwexpress.core.designsystem.PwToasts(Modifier.align(Alignment.BottomCenter))
                   }
                  }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra("pwex_abrir")?.let { abrirAba.value = it }
    }

    override fun onStart() {
        super.onStart()
        // voltou do repouso: conexões velhas fora, e o login já renova em segundo plano (sem esperar a 1ª tela)
        PwCore.http.renovarConexoes()
        if (PwCore.auth.sessao.value != null) {
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) { runCatching { PwCore.auth.tokenValido() } }
            Tracking.retomarSeAtivo()
            Corridas.sincronizar()
        }
        // versão obrigatória (escolhida pelo Dev): confere sempre que o app volta para a frente
        lifecycleScope.launch { Atualizador.verificarObrigatoria() }
        // procura versão nova uma vez por abertura do app (só abre a janela se for nova)
        if (!verificouAtualizacao) {
            verificouAtualizacao = true
            if (Perfis.perfil.value?.modulo("atualizacao_app") != false) Atualizador.verificar(automatico = true)
        }
    }

    private companion object { var verificouAtualizacao = false }
}
