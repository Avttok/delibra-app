package com.pwexpress.feature.gestor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.pwexpress.core.designsystem.PwAba
import com.pwexpress.core.designsystem.PwBarraAbas
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.alvoTour
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Status
import kotlinx.coroutines.delay

/** Telas empilhadas por cima das abas (detalhes, formulários). */
sealed interface TelaG {
    data class Corrida(val id: String) : TelaG
    data object NovaCorrida : TelaG
    data object NovoPedido : TelaG
    data class Empresa(val id: String?) : TelaG
    data class Membro(val id: String) : TelaG
    data object Financeiro : TelaG
    data object Configuracoes : TelaG
    data object Dev : TelaG
    data object Excecoes : TelaG
    data object Historico : TelaG
    data class CatalogoT(val cat: Catalogo) : TelaG
}

/** Pedidos de navegação vindos de fora do painel (ex.: "Configurações" no menu da foto). */
object GestorNav {
    internal val pedido = kotlinx.coroutines.flow.MutableStateFlow<TelaG?>(null)
    fun abrirConfiguracoes() { pedido.value = TelaG.Configuracoes }
    fun abrirDev() { pedido.value = TelaG.Dev }
    fun abrir(t: TelaG) { pedido.value = t }
    /** Abre a aba de pedidos já no pedido [id] (do chat ou de um aviso). */
    val pedidoAberto = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    fun abrirPedido(id: String) { pedidoAberto.value = id }
}

/**
 * Painel do gestor no app: tudo o que o site faz.
 * `topo` é o cabeçalho do app (logo + foto, que abre o menu da conta).
 */
@Composable
fun GestorApp(
    modifier: Modifier = Modifier,
    topo: @Composable () -> Unit,
    /** Faixa logo abaixo do topo, em todas as telas (ex.: "Voltar à corrida"). Empurra o conteúdo, não cobre nada. */
    faixa: (@Composable () -> Unit)? = null,
    meuId: String?,
) {
    // volta para onde a pessoa estava (Ajustes → lembrar_pagina)
    var aba by com.pwexpress.core.designsystem.lembrarPagina("gestor_aba", "mapa")
    var subOperacao by com.pwexpress.core.designsystem.lembrarPaginaNumero("gestor_sub_operacao", 0)   // 0 pedidos · 1 rotas
    var subPessoas by com.pwexpress.core.designsystem.lembrarPaginaNumero("gestor_sub_pessoas", 0)
    var subMapa by com.pwexpress.core.designsystem.lembrarPaginaNumero("gestor_sub_mapa", 0)          // 0 mapa · 1 hoje     // 0 clientes · 1 equipe
    val pilha = remember { mutableStateListOf<TelaG>() }
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    val ciclo = LocalLifecycleOwner.current

    // atualiza enquanto o painel está aberto (o site usa tempo real; aqui, a cada 6 s)
    LaunchedEffect(Unit) {
        ciclo.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) { Gestao.atualizar(); delay(6_000) }
        }
    }

    LaunchedEffect(aba) {
        when (aba) { "pedidos" -> { aba = "operacao"; subOperacao = 0 }; "corridas" -> { aba = "operacao"; subOperacao = 1 }
                     "empresas" -> { aba = "pessoas"; subPessoas = 0 }; "equipe" -> { aba = "pessoas"; subPessoas = 1 } }
    }
    fun abrir(t: TelaG) { if (pilha.lastOrNull() != t) pilha.add(t) }
    val pedidoAberto by GestorNav.pedidoAberto.collectAsState()
    LaunchedEffect(pedidoAberto) { if (pedidoAberto != null) { pilha.clear(); aba = "operacao"; subOperacao = 0 } }
    val pedido by GestorNav.pedido.collectAsState()
    LaunchedEffect(pedido) { pedido?.let { abrir(it); GestorNav.pedido.value = null } }
    fun voltar() { if (pilha.isNotEmpty()) pilha.removeAt(pilha.lastIndex) }
    BackHandler(enabled = pilha.isNotEmpty()) { voltar() }

    // corrida em andamento sem posição do motoboy há 30 min: avisa uma vez por corrida
    val avisadas = remember { mutableSetOf<String>() }
    LaunchedEffect(dados) {
        val limite = System.currentTimeMillis() - 30 * 60_000
        dados.corridas.filter { it.status == Status.ACEITA || it.status == Status.EM_ANDAMENTO }.forEach { cor ->
            val id = cor.motoboyId ?: return@forEach
            val t = dados.posicao(id)?.posicaoEm ?: 0L
            if (t < limite && avisadas.add(cor.id)) {
                val min = if (t > 0) ((System.currentTimeMillis() - t) / 60_000) else null
                com.pwexpress.core.designsystem.PwToast.mostrar(
                    "${dados.membro(id)?.nome ?: "O motoboy"} está na corrida ${cor.numeroExibicao} e mandou a localização pela última vez ${min?.let { "há $it min" } ?: "há muito tempo"}.",
                    com.pwexpress.core.designsystem.TipoAviso.AVISO, "Motoboy sem sinal",
                )
            } else if (t >= limite) avisadas.remove(cor.id)
        }
    }
    val permG by com.pwexpress.feature.gestor.data.Permissoes.estado.collectAsState()
    val comCarteira = modulo("carteira") && permG.pode("ver_carteira")
    val planejadas = dados.corridas.count { it.teste == teste && it.status in Status.PLANEJADAS }
    val semMotoboy = dados.corridas.count { it.teste == teste && (it.status == Status.SEM_MOTOBOY || it.status == Status.MONTADA) }
    val paraRevisar = dados.pedidos.count { it.teste == teste && it.status == "recebido" }
    val comPedidos = modulo("pedidos")
    // pedido novo chegando: avisa (uma vez por pedido)
    val vistos = remember { mutableSetOf<String>() }
    var primeira by remember { mutableStateOf(true) }
    LaunchedEffect(dados.pedidos) {
        if (!dados.carregado) return@LaunchedEffect
        val novos = dados.pedidos.filter { it.status == "recebido" && it.teste == teste && vistos.add(it.id) }
        if (!primeira && novos.isNotEmpty()) com.pwexpress.core.designsystem.PwToast.mostrar(
            if (novos.size == 1) "Pedido ${novos[0].numeroExibicao} chegou e espera sua revisão." else "${novos.size} pedidos novos esperando revisão.",
            com.pwexpress.core.designsystem.TipoAviso.INFO, "Pedido novo",
        )
        primeira = false
    }

    Box(modifier.fillMaxSize()) {
        val atual = pilha.lastOrNull()
        Column(Modifier.fillMaxSize()) {
            if (atual == null) topo()
            faixa?.invoke()
            // motoboy pedindo para cancelar: aparece em todas as telas até alguém responder
            // emergência: em todas as telas, até alguém marcar "Resolvido"
            FaixaEmergenciasG(onAbrirCorrida = { abrir(TelaG.Corrida(it)) })
            if (atual !is TelaG.Corrida) FaixaChamadosG(onAbrir = { abrir(TelaG.Corrida(it)) })
            Box(Modifier.weight(1f)) {
                when (atual) {
                    is TelaG.Corrida -> DetalheCorridaG(atual.id, onVoltar = ::voltar)
                    TelaG.NovoPedido -> NovoPedidoG(onVoltar = ::voltar, onCriado = { voltar(); aba = "operacao"; subOperacao = 0 })
                    TelaG.NovaCorrida -> NovaCorridaG(onVoltar = ::voltar, onCriada = { id -> voltar(); abrir(TelaG.Corrida(id)) })
                    is TelaG.Empresa -> EmpresaG(atual.id, onVoltar = ::voltar)
                    is TelaG.Membro -> MembroG(atual.id, meuId = meuId, onVoltar = ::voltar)
                    TelaG.Financeiro -> FinanceiroG(onVoltar = ::voltar)
                    TelaG.Configuracoes -> ConfiguracoesG(onVoltar = ::voltar, onAbrir = { abrir(it) })
                    TelaG.Excecoes -> ExcecoesG(onVoltar = ::voltar)
                    TelaG.Historico -> HistoricoG(onVoltar = ::voltar)
                    is TelaG.CatalogoT -> CatalogoG(atual.cat, onVoltar = ::voltar)
                    TelaG.Dev -> DevG(onVoltar = ::voltar)
                    null -> when (aba) {
                        "mapa" -> ComSubAbas(listOf("Mapa" to PwIcones.Mapa, "Hoje" to PwIcones.LayoutDashboard), subMapa, { subMapa = it }) { i ->
                            if (i == 0) MapaG(onCorrida = { abrir(TelaG.Corrida(it)) }) else HojeG(onAba = { a -> aba = a })
                        }
                        "operacao" -> ComSubAbas(
                            if (comPedidos) listOf("Pedidos" to PwIcones.ClipboardList, "Rotas" to PwIcones.Route) else listOf("Rotas" to PwIcones.Route),
                            if (comPedidos) subOperacao else 0, { subOperacao = it },
                            listOf(paraRevisar, planejadas + semMotoboy).let { if (comPedidos) it else it.drop(1) },
                        ) { i ->
                            if (comPedidos && i == 0) PedidosG(onNovo = { abrir(TelaG.NovoPedido) })
                            else CorridasG(onAbrir = { abrir(TelaG.Corrida(it)) }, onNova = { abrir(TelaG.NovaCorrida) })
                        }
                        "pessoas" -> ComSubAbas(listOf("Clientes" to PwIcones.Building2, "Equipe" to PwIcones.Users), subPessoas, { subPessoas = it }) { i ->
                            if (i == 0) EmpresasG(onAbrir = { abrir(TelaG.Empresa(it)) }) else EquipeG(onAbrir = { abrir(TelaG.Membro(it)) })
                        }
                        "config" -> ConfiguracoesG(onVoltar = null, onAbrir = { abrir(it) })
                        else -> FinanceiroG(onVoltar = null)
                    }
                }
            }
        }
        if (atual == null) {
            PwBarraAbas(
                listOf(
                    PwAba("mapa", "Mapa", PwIcones.Mapa),
                    PwAba("operacao", if (comPedidos) "Pedidos" else "Rotas", if (comPedidos) PwIcones.ClipboardList else PwIcones.Route, (if (comPedidos) paraRevisar else 0) + planejadas + semMotoboy),
                    PwAba("pessoas", "Pessoas", PwIcones.Users),
                ) + (if (comCarteira) listOf(PwAba("financeiro", "Carteira", PwIcones.Wallet)) else emptyList()) +
                    PwAba("config", "Ajustes", PwIcones.Settings2),
                aba, { aba = it },
                Modifier.align(Alignment.BottomCenter).alvoTour("abas-gestor"),
            )
        }
    }
}

/** Duas telas numa aba só: um seletor em cima (com contadores) e a tela escolhida embaixo. */
@Composable
private fun ComSubAbas(
    opcoes: List<Pair<String, androidx.compose.ui.graphics.vector.ImageVector>>,
    selecionada: Int,
    onSelecionar: (Int) -> Unit,
    contadores: List<Int> = emptyList(),
    conteudo: @Composable (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        if (opcoes.size > 1) {
            com.pwexpress.core.designsystem.PwSegmentado(
                opcoes.mapIndexed { i, (r, ic) -> (contadores.getOrNull(i)?.takeIf { it > 0 }?.let { "$r · $it" } ?: r) to ic },
                selecionada, onSelecionar,
                Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 6.dp),
            )
        }
        Box(Modifier.weight(1f)) { conteudo(selecionada) }
    }
}
