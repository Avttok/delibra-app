package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import com.pwexpress.core.json.str
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.long
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.obj
import com.pwexpress.feature.gestor.data.PARAMETROS
import com.pwexpress.feature.gestor.data.logico
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PedidoG
import com.pwexpress.feature.gestor.data.StatusPedido
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

/**
 * Pedidos (do site do cliente e do gestor). Por padrão todo pedido novo passa por aqui:
 * o gestor aprova (vai para o roteirizador), ajusta o valor ou recusa.
 */
@Composable
fun PedidosG(onNovo: () -> Unit = {}) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var aberto by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    val pedidoPedido by GestorNav.pedidoAberto.collectAsState()
    LaunchedEffect(pedidoPedido) { pedidoPedido?.let { aberto = it; GestorNav.pedidoAberto.value = null } }

    var busca by rememberSaveable { mutableStateOf("") }
    var tipo by rememberSaveable { mutableIntStateOf(0) }
    val tipos: List<Pair<String, (PedidoG) -> Boolean>> = listOf("Todos" to { _ -> true }, "Agora" to { p -> !p.planejado }, "Planejados" to { p -> p.planejado },
        "Urgentes" to { p -> p.urgente }, "Aguardando pagamento" to { p -> p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente" })
    // motoboy de cada pedido: pelas rotas que levam os endereços dele
    fun motoboys(p: PedidoG): String = dados.corridas.filter { cor -> cor.teste == p.teste && cor.paradas.any { pa -> p.pontos.any { it.endereco == pa.endereco } } && cor.criadaEm >= p.criadoEm - 60_000 }
        .mapNotNull { cor -> cor.motoboyId?.let { dados.membro(it)?.nome } ?: cor.motoboyNomeArquivado }.joinToString(" ")
    val todos = dados.pedidos.filter { it.teste == teste }.filter { p ->
        tipos[tipo].second(p) && (busca.isBlank() || combinaBusca(busca, p.numeroExibicao, p.numero.toString(), dados.empresa(p.empresaId)?.nome, p.clienteNome, p.observacoes,
            p.tipoPedido, StatusPedido.rotulo[p.status], p.pontos.joinToString(" ") { "${it.endereco} ${it.contatoNome ?: ""}" }, p.itens.joinToString(" ") { it.descricao }, motoboys(p)))
    }
    val listas = listOf(
        todos.filter { it.status == "recebido" }.sortedWith(compareByDescending<PedidoG> { it.urgente }.thenBy { it.quando ?: it.criadoEm }),
        todos.filter { it.status in setOf("aprovado", "roteirizado", "em_rota") },
        todos.filter { it.status == "concluido" },
        todos.filter { it.status in setOf("cancelado", "recusado") },
    )
    val lista = listas[filtro]

    PwPagina {
        PwCabeca(PwIcones.ClipboardList, "Pedidos", if (listas[0].isEmpty()) "Nada esperando revisão" else "${listas[0].size} esperando você") {
            PwBotaoIcone(PwIcones.Sparkles, "Montar rotas agora", {
                if (!ocupado) { ocupado = true; escopo.launch { Gestao.roteirizarAgora().onSuccess { PwToast.mostrar("Rotas montadas com os pedidos aprovados.", TipoAviso.OK) }.onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; Gestao.atualizar(); ocupado = false } }
            }, tamanho = 38.dp)
            PwBotaoIcone(PwIcones.Plus, "Novo pedido", onNovo, ativo = true, tamanho = 38.dp)
        }
        CampoBusca(busca, { busca = it }, "Buscar nº, cliente, endereço, motoboy…")
        PwFiltros(listOf("Revisar" to listas[0].size, "Em andamento" to listas[1].size, "Concluídos" to 0, "Cancelados" to 0), filtro, { filtro = it })
        PwFiltros(tipos.map { it.first to 0 }, tipo, { tipo = it })
        when {
            !dados.carregado -> PwCarregando()
            lista.isEmpty() -> PwVazio(if (busca.isNotBlank() || tipo > 0) "Nenhum pedido encontrado." else if (filtro == 0) "Nenhum pedido esperando revisão." else "Nenhum pedido aqui.", icone = PwIcones.ClipboardList)
            else -> lista.forEach { p -> CartaoPedido(p, dados.empresa(p.empresaId)?.nome) { aberto = p.id } }
        }
    }
    aberto?.let { id -> dados.pedidos.firstOrNull { it.id == id }?.let { JanelaPedido(it, dados.empresa(it.empresaId)?.nome) { aberto = null } } }
}

private fun nomeCliente(p: PedidoG, empresa: String?) = empresa ?: p.clienteNome?.let { "$it (avulso)" } ?: "Avulso"

@Composable
private fun CartaoPedido(p: PedidoG, empresa: String?, onClick: () -> Unit) {
    val c = Pw.cores
    PwCartao(destaque = p.status == "recebido", modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p.numeroExibicao, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp)
            Spacer(Modifier.width(8.dp))
            // express e planejado com o mesmo peso
            PwSelo(if (p.planejado) "Planejado · ${p.quando?.let { dataHora(it) } ?: "—"}" else "Agora", if (p.planejado) c.azul else c.ouro, icone = if (p.planejado) PwIcones.CalendarClock else PwIcones.Zap)
            Spacer(Modifier.weight(1f))
            Text(moedaBr(p.valor), color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Spacer(Modifier.size(4.dp))
        Text(nomeCliente(p, empresa), color = c.texto2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val entregas = p.pontos.count { !it.coleta }
        Text(
            listOfNotNull(
                "${p.pontos.count { it.coleta }} coleta${if (p.pontos.count { it.coleta } == 1) "" else "s"} · $entregas entrega${if (entregas == 1) "" else "s"}",
                km(p.distanciaM).takeIf { p.distanciaM != null },
                "${p.itens.sumOf { it.quantidade }} ite${if (p.itens.sumOf { it.quantidade } == 1) "m" else "ns"}".takeIf { p.itens.isNotEmpty() },
                StatusPedido.rotulo[p.status],
            ).joinToString(" · "),
            color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
        val esperaPag = p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente" && p.status !in setOf("cancelado", "recusado", "concluido")
        if (p.urgente || esperaPag) {
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (p.urgente) PwSelo("URGENTE", c.vermelho, icone = PwIcones.Flame)
                if (esperaPag) PwSelo("Aguardando pagamento", androidx.compose.ui.graphics.Color(0xFF38BDF8), icone = PwIcones.Wallet)
            }
        }
    }
}

@Composable
private fun JanelaPedido(p: PedidoG, empresa: String?, onFechar: () -> Unit) {
    val c = Pw.cores
    var valor by remember(p.id) { mutableStateOf("%.2f".format(BR, p.valor ?: p.valorCalculado ?: 0.0)) }
    var motivo by remember(p.id) { mutableStateOf(p.ajusteMotivo.orEmpty()) }
    var recusar by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    fun rodar(msg: String, fechar: Boolean = true, bloco: suspend () -> Result<Unit>) {
        ocupado = true
        escopo.launch {
            bloco().onSuccess { erro = null; PwToast.mostrar(msg, TipoAviso.OK); Gestao.atualizar(); if (fechar) onFechar() }.onFailure { erro = it.message }
            ocupado = false
        }
    }
    val valorNum = valor.replace(".", "").replace(",", ".").toDoubleOrNull()
    val mudou = valorNum != null && kotlin.math.abs(valorNum - (p.valor ?: 0.0)) > 0.004

    PwTelaCheia("Pedido ${p.numeroExibicao}", onFechar, subtitulo = "${nomeCliente(p, empresa)} · ${dataHora(p.criadoEm)}") {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PwSelo(StatusPedido.rotulo[p.status] ?: p.status, if (p.status == "recebido") c.ouro else c.texto3)
            PwSelo(if (p.planejado) "Planejado" else "Agora", if (p.planejado) c.azul else c.ouro)
            if (p.urgente) PwSelo("URGENTE", c.vermelho, icone = PwIcones.Flame)
            PwSelo(if (p.cobranca == "credito") "Crédito (fatura)" else "Pré-pago", c.texto2)
            Spacer(Modifier.weight(1f))
            com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Share2, "Mandar no chat", {
                com.pwexpress.core.chat.Chat.compartilhar(com.pwexpress.core.chat.Anexo("pedido", p.id, "Pedido ${p.numeroExibicao}", nomeCliente(p, empresa))); onFechar()
            }, tamanho = 34.dp)
        }
        if (p.planejado && p.quando != null) Text("Para ${dataHora(p.quando)}", color = c.azul, fontWeight = FontWeight.SemiBold)

        var focoP by remember(p.id) { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }
        MapaPedidoG(p, foco = focoP)
        DetalhesPedidoG(p) { la, lo -> focoP = com.pwexpress.core.mapa.FocoMapa(la, lo) }

        PwCartao(destaque = true) {
            var editando by remember(p.id) { mutableStateOf(false) }
            p.orcamento?.let { o ->
                if (editando) EditarOrcamentoG(p.id, o, onFechar = { editando = false })
                else {
                    OrcamentoG(o, p.pontos)
                    if (p.status !in setOf("concluido", "cancelado", "recusado") && p.faturaId == null)
                        PwBotaoVidro("Mudar o cálculo", onClick = { editando = true }, icone = PwIcones.Pencil, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            } ?: TextoDica("Sem orçamento.")
            Spacer(Modifier.size(8.dp))
            Text("Calculado: ${moedaBr(p.valorCalculado)}" + (if (p.valorAjuste != 0.0) " · ajuste ${if (p.valorAjuste > 0) "+" else "−"}${moedaBr(kotlin.math.abs(p.valorAjuste))}" else "") +
                (p.ajusteMotivo?.takeIf { p.valorAjuste != 0.0 }?.let { " ($it)" } ?: "") + " · " +
                when { p.cobranca == "credito" -> if (p.faturaId != null) "já na fatura" else "entra na próxima fatura"; p.pagamentoStatus == "pago" -> "pago (${p.pagamentoMetodo ?: ""})"; else -> "pagamento antecipado pendente" },
                color = c.texto2, fontSize = 13.sp)
            Spacer(Modifier.size(6.dp))
            PwCampo(valor, { valor = it }, "Valor do pedido", teclado = KeyboardType.Decimal, prefixo = "R$ ")
            if (mudou || p.valorAjuste != 0.0) PwCampo(motivo, { motivo = it }, "Motivo da mudança", dica = "Ex.: cliente fixo, rota longa, desconto combinado")
            if (mudou && p.status != "recebido") {
                Spacer(Modifier.size(6.dp))
                PwBotaoVidro("Salvar valor", onClick = { rodar("Valor ajustado.", fechar = false) { Gestao.revisarPedido(p.id, "ajustar", valorNum, motivo.ifBlank { null }) } }, icone = PwIcones.Check, modifier = Modifier.fillMaxWidth())
            }
            if (p.status in setOf("recebido", "aprovado")) {
                Spacer(Modifier.size(6.dp))
                Text("Recalcular com as regras de agora", color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    modifier = Modifier.clickable { rodar("Recalculado.", fechar = false) { Gestao.revisarPedido(p.id, "recalcular") } }.padding(vertical = 6.dp))
            }
        }
        if (p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente" && p.status !in setOf("cancelado", "recusado", "concluido")) {
            PwCartao(cor = androidx.compose.ui.graphics.Color(0xFF38BDF8).copy(alpha = 0.6f)) {
                com.pwexpress.core.designsystem.PwRotulo("Cliente pré-pago · aguardando pagamento")
                TextoDica((if (p.status == "recebido") "Depois de aprovado, o pedido" else "O pedido") + " só vira rota quando o pagamento for confirmado. " +
                    "Ou ligue “Montar rota sem esperar o pagamento” nas regras deste pedido ou do cliente.")
                Spacer(Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoVidro("PIX recebido", onClick = { rodar("Pagamento confirmado: o pedido segue para as rotas.", fechar = false) { Gestao.confirmarPagamentoPedido(p.id, "pix") } },
                        icone = PwIcones.Check, modifier = Modifier.weight(1f))
                    PwBotaoVidro("Cartão", onClick = { rodar("Pagamento confirmado: o pedido segue para as rotas.", fechar = false) { Gestao.confirmarPagamentoPedido(p.id, "cartao") } },
                        icone = PwIcones.Wallet, modifier = Modifier.weight(1f))
                }
            }
        }
        if (p.status !in setOf("cancelado", "recusado") && (p.status != "recebido" || Gestao.dados.value.config.logico("revisao_previa_rotas", true)))
            RotasDoPedidoG(p.id, p.status, "${p.status}|${p.valor}|${p.regras}")
        if (p.status !in setOf("recebido", "cancelado", "recusado", "concluido")) SituacaoRotaG(p.id, "${p.status}|${p.pagamentoStatus}|${p.regras}")
        PwErro(erro)
        p.recusaMotivo?.let { TextoDica("Motivo da recusa: $it") }

        if (p.status == "recebido") {
            var segurar by com.pwexpress.core.designsystem.lembrarLigado("aprovar_segurando", false)
            com.pwexpress.core.designsystem.PwInterruptor("Segurar as rotas antes de soltar", segurar, { segurar = it }, icone = PwIcones.Hand,
                descricao = if (segurar) "Aprova e monta as rotas, mas elas esperam você mudar a ordem, separar ou juntar e soltar." else "Aprova e as rotas seguem sozinhas para os motoboys.")
            PwBotaoOuro(if (ocupado) "Aprovando…" else "Aprovar" + if (mudou) " com ${moedaBr(valorNum)}" else "", onClick = {
                if (segurar) rodar("Aprovado. As rotas estão montadas e seguras: ajuste e solte.", fechar = false) {
                    (if (mudou) Gestao.revisarPedido(p.id, "ajustar", valorNum, motivo.ifBlank { null }) else Result.success(Unit))
                        .mapCatching { Gestao.rpcJson("aprovar_e_montar", kotlinx.serialization.json.buildJsonObject { put("p_pedido", p.id) }).getOrThrow(); Unit }
                }
                else rodar(if (p.cobranca == "pre_pago" && p.pagamentoStatus == "pendente") "Aprovado. Cliente pré-pago: a rota sai quando o pagamento for confirmado." else "Aprovado: vai para as rotas.") { Gestao.revisarPedido(p.id, "aprovar", if (mudou) valorNum else null, motivo.ifBlank { null }) }
            }, icone = PwIcones.Check, habilitado = !ocupado)
        }
        if (p.status in setOf("recebido", "aprovado")) {
            if (!recusar) PwBotaoPerigo("Recusar pedido", onClick = { recusar = true }, modifier = Modifier.fillMaxWidth())
            else {
                PwCampo(motivo, { motivo = it }, "Por que recusar?", dica = "O cliente vê esse motivo")
                PwBotaoPerigo("Confirmar recusa", onClick = { rodar("Pedido recusado.") { Gestao.revisarPedido(p.id, "recusar", null, motivo.ifBlank { null }) } }, cheio = true, modifier = Modifier.fillMaxWidth())
            }
        }
        if (p.status in setOf("recebido", "aprovado")) {
            var regras by remember { mutableStateOf(false) }
            PwBotaoVidro("Regras deste pedido" + if (p.regras.isNotEmpty()) " (${p.regras.size})" else "", onClick = { regras = true }, icone = PwIcones.Layers, modifier = Modifier.fillMaxWidth())
            if (regras) RegrasPedidoG(p.id, p.regras, onFechar = { regras = false })
        }
        if (p.status in setOf("aprovado", "roteirizado", "em_rota")) {
            PwBotaoVidro("Voltar para revisão", onClick = {
                rodar("Pedido voltou para revisão: as rotas dele foram desfeitas.", fechar = false) { Gestao.revisarPedido(p.id, "voltar_revisao", null, motivo.ifBlank { null }) }
            }, icone = PwIcones.History, modifier = Modifier.fillMaxWidth())
            TextoDica("Só enquanto nenhuma coleta ou entrega dele foi feita. Serve para refazer as rotas com regras novas.")
        }
        Spacer(Modifier.size(12.dp))
    }
}

/** Ícone de cada modelo de pagamento. */
internal val ICONE_MODO = mapOf("zonas" to PwIcones.Radar, "rota" to PwIcones.Ruler, "percentual" to PwIcones.Percent, "por_entrega" to PwIcones.Package, "fixo" to PwIcones.Coins, "combinado" to PwIcones.Layers)

/** Botão "mover" de uma entrega: abre um menu com as outras rotas abertas e "rota nova". */
@Composable
internal fun MenuMoverEntrega(outras: List<Pair<String, String>>, podeSeparar: Boolean, habilitado: Boolean, onEscolher: (String?) -> Unit) {
    var aberto by remember { mutableStateOf(false) }
    Box {
        PwBotaoIcone(PwIcones.ArrowRightLeft, "Mover esta entrega", { aberto = true }, tamanho = 32.dp, habilitado = habilitado)
        androidx.compose.material3.DropdownMenu(aberto, { aberto = false }, containerColor = Pw.cores.fundo2) {
            if (podeSeparar) androidx.compose.material3.DropdownMenuItem(
                text = { Text("Separar numa rota nova", color = Pw.cores.texto) }, leadingIcon = { Icon(PwIcones.Split, null, tint = Pw.cores.ouro) },
                onClick = { aberto = false; onEscolher(null) })
            outras.forEach { (id, nome) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text("Mover para $nome", color = Pw.cores.texto) }, leadingIcon = { Icon(PwIcones.Route, null, tint = Pw.cores.texto2) },
                    onClick = { aberto = false; onEscolher(id) })
            }
        }
    }
}

private data class ModoDespacho(val chave: String, val nome: String, val dica: String, val icone: androidx.compose.ui.graphics.vector.ImageVector)
private val MODOS_DESPACHO = listOf(
    ModoDespacho("quadro", "Quadro", "Todos veem; o primeiro que pegar leva.", PwIcones.Users),
    ModoDespacho("melhor_primeiro", "Melhor primeiro", "Só o mais indicado recebe; se não aceitar, vai ao quadro.", PwIcones.Crown),
    ModoDespacho("cascata", "Cascata", "A oferta passa de um em um, do mais perto ao mais longe.", PwIcones.Shuffle),
    ModoDespacho("direto", "Direto", "Você escolhe quem recebe a oferta.", PwIcones.Send),
)

/**
 * Peças das rotas automáticas no detalhe: despacho (escolha o modo e solte), estender a rota de quem está
 * rodando, mover entregas e o pagamento do motoboy (prévia editável antes de soltar).
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun BlocoRotaG(corrida: com.pwexpress.feature.gestor.data.CorridaG, ocupado: Boolean, acao: (suspend () -> Result<Unit>) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val aberta = corrida.motoboyId == null && corrida.status in setOf("montada", "agendada", "aguardando_motoboy", "sem_motoboy")
    // editar percurso: arrastar o caminho para as ruas que o gestor quer
    var editarPercurso by remember(corrida.id) { mutableStateOf(false) }
    if (corrida.status !in setOf("concluida", "cancelada") && corrida.paradas.size >= 2) {
        PwBotaoVidro("Editar percurso", onClick = { editarPercurso = true }, icone = PwIcones.Route, modifier = Modifier.fillMaxWidth())
        if (editarPercurso) EditarPercursoG(corrida, onFechar = { editarPercurso = false })
    }
    if (aberta) {
        var modo by remember(corrida.id) { mutableStateOf(if (corrida.modoDespacho in MODOS_DESPACHO.map { it.chave }) corrida.modoDespacho ?: "cascata" else "cascata") }
        var moto by remember(corrida.id) { mutableStateOf(corrida.sugeridoMotoboyId) }
        PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Send, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(if (corrida.status == "montada") "Soltar para os motoboys" else "Despacho", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            }
            if (corrida.modoDespacho == "quadro" && corrida.status != "montada") TextoDica("No quadro${corrida.quadroDesde?.let { " desde ${hora(it)}" } ?: ""}: todos veem, o primeiro que pegar leva.")
            if (corrida.agendadaPara != null) TextoDica("Agendada para ${dataHora(corrida.agendadaPara)}: quem aceitar fica com a reserva.")
            Espaco(8)
            // os 4 modos lado a lado (2 por linha), cada um com ícone e uma frase
            MODOS_DESPACHO.chunked(2).forEach { linha ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    linha.forEach { m ->
                        val sel = modo == m.chave
                        val forma = RoundedCornerShape(16.dp)
                        Column(
                            Modifier.weight(1f).heightIn(min = 96.dp).clip(forma).background(if (sel) c.ouro.copy(alpha = 0.14f) else c.vidro)
                                .border(if (sel) 1.6.dp else 1.dp, if (sel) c.ouro else c.borda, forma).clickable { modo = m.chave }.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(m.icone, null, tint = if (sel) c.ouro else c.texto2, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(m.nome, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            }
                            Text(m.dica, color = c.texto3, fontSize = 11.5.sp, lineHeight = 14.sp)
                        }
                    }
                }
            }
            if (modo == "direto") {
                val candidatos = dados.equipe.filter { it.motoboy && it.modoTeste == corrida.teste }
                    .sortedWith(compareBy({ it.id != corrida.sugeridoMotoboyId }, { dados.posicao(it.id)?.status != "disponivel" }, { it.nome }))
                com.pwexpress.core.designsystem.PwRotulo("Quem recebe")
                Espaco(4)
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    candidatos.forEach { m ->
                        val sel = moto == m.id
                        val st = dados.posicao(m.id)?.status
                        val forma = RoundedCornerShape(99.dp)
                        Row(Modifier.clip(forma).background(if (sel) c.ouro.copy(alpha = 0.16f) else c.vidro).border(1.dp, if (sel) c.ouro else c.borda, forma)
                            .clickable { moto = m.id }.padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            com.pwexpress.core.designsystem.PwAvatar(m.nome, m.foto, tamanho = 26.dp)
                            Spacer(Modifier.width(6.dp))
                            if (m.id == corrida.sugeridoMotoboyId) { Icon(PwIcones.Star, "Melhor agora", tint = c.ouro, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(3.dp)) }
                            Text(m.nome.substringBefore(" "), color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Spacer(Modifier.width(6.dp))
                            Box(Modifier.size(7.dp).clip(androidx.compose.foundation.shape.CircleShape).background(corMotoboy(st, c)))
                        }
                    }
                }
                Espaco(8)
            } else corrida.sugeridoMotoboyId?.let { TextoDica("Melhor agora: ${dados.membro(it)?.nome ?: "motoboy"}") }
            val nomeModo = MODOS_DESPACHO.first { it.chave == modo }.nome
            PwBotaoOuro(if (modo == "direto") "Enviar para ${moto?.let { dados.membro(it)?.nome?.substringBefore(" ") } ?: "…"}" else "Soltar · $nomeModo", onClick = {
                if (modo == "direto") moto?.let { m -> acao { Gestao.liberarRota(corrida.id, null, m) } }
                else acao { Gestao.liberarRota(corrida.id, modo) }
            }, icone = MODOS_DESPACHO.first { it.chave == modo }.icone, habilitado = !ocupado && (modo != "direto" || moto != null))
        }
    }

    // estender a rota de quem já está em corrida e está perto da coleta desta
    if (aberta && corrida.entregas > 0) {
        val coleta = corrida.paradas.firstOrNull { it.coleta }
        val emCorrida = dados.corridas.filter { it.id != corrida.id && it.teste == corrida.teste && it.motoboyId != null && it.status in setOf("aceita", "em_andamento") }
            .mapNotNull { r -> r.motoboyId?.let { m -> Triple(r, m, dados.posicao(m)?.let { p -> p.lat?.let { la -> p.lng?.let { lo -> coleta?.let { cl -> com.pwexpress.core.mapa.metros(la to lo, cl.lat to cl.lng) } } } }) } }
            .sortedBy { it.third ?: Double.MAX_VALUE }
        if (emCorrida.isNotEmpty()) {
            var escolhido by remember(corrida.id) { mutableStateOf<String?>(null) }
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Merge, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Estender a rota de quem está rodando", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    com.pwexpress.core.designsystem.PwDica("Extensão de rota", "As paradas desta rota vão para o fim da rota de um motoboy que já está rodando (o mais perto da coleta aparece primeiro). O valor das duas soma e o motoboy nunca ganha menos do que já ia ganhar. \"Perguntar\" manda uma oferta; \"Estender direto\" já junta.")
                }
                Espaco(6)
                com.pwexpress.core.designsystem.PwSelecao("Motoboy em corrida", emCorrida.map { (r, m, d) ->
                    m to "${dados.membro(m)?.nome ?: "Motoboy"} · rota ${r.numeroExibicao}" + (d?.let { " · ${if (it < 1000) "${it.toInt()} m" else "%.1f km".format(java.util.Locale.forLanguageTag("pt-BR"), it / 1000)} da coleta" } ?: "")
                }, escolhido ?: emCorrida.first().second, { escolhido = it })
                Espaco(6)
                val m = escolhido ?: emCorrida.first().second
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoVidro("Perguntar", onClick = { acao { Gestao.estenderRota(corrida.id, m, true) } }, icone = PwIcones.Send, modifier = Modifier.weight(1f), habilitado = !ocupado)
                    PwBotaoVidro("Estender direto", onClick = { acao { Gestao.estenderRota(corrida.id, m, false) } }, icone = PwIcones.Merge, modifier = Modifier.weight(1f), habilitado = !ocupado)
                }
            }
        }
    }

    // mover entregas: uma linha por entrega, com o botão de mover (menu)
    if (aberta && corrida.entregas > 0) {
        val outras = dados.corridas.filter { it.id != corrida.id && it.teste == corrida.teste && it.motoboyId == null && it.status in setOf("montada", "agendada", "aguardando_motoboy", "sem_motoboy") }
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.ArrowRightLeft, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Entregas desta rota", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            }
            TextoDica("Toque no botão de cada entrega para separar numa rota nova ou mandar para outra rota aberta.")
            corrida.paradas.filter { !it.coleta }.sortedBy { it.ordem }.forEach { p ->
                Espaco(6)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.vidro).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(c.texto), contentAlignment = Alignment.Center) {
                        Text("${p.ordem + 1}", color = c.fundo, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(p.endereco.substringBefore(","), color = c.texto, fontSize = 13.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    MenuMoverEntrega(outras.map { it.id to "Rota ${it.numeroExibicao}" }, podeSeparar = corrida.entregas > 1, habilitado = !ocupado) { destino -> acao { Gestao.moverEntrega(p.id, destino) } }
                }
            }
        }
    }

    val calc = corrida.pagCalculo ?: return
    var ajuste by remember(corrida.id) { mutableStateOf(numeroBr(corrida.pagAjuste)) }
    var motivo by remember(corrida.id) { mutableStateOf(corrida.pagAjusteMotivo.orEmpty()) }
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.HandCoins, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text("Pagamento do motoboy" + if (corrida.motoboyId == null) " (prévia)" else "", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
        }
        Espaco(4)
        LinhasCalculo(calc)
        if (aberta) PagamentoEditavelG(corrida, ocupado, acao)
        if (corrida.motoboyId != null) {
            Espaco(8)
            PwCampo(ajuste, { ajuste = it }, "Ajuste (+/−)", teclado = KeyboardType.Text, prefixo = "R$ ")
            PwCampo(motivo, { motivo = it }, "Motivo", dica = "Ex.: chuva, espera longa, rota mais curta")
            Espaco(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwBotaoVidro("Salvar ajuste", onClick = { acao { Gestao.ajustarPagamento(corrida.id, ajuste.replace(",", ".").toDoubleOrNull() ?: 0.0, motivo.ifBlank { null }) } }, icone = PwIcones.Check, modifier = Modifier.weight(1f))
                PwBotaoVidro("Recalcular", onClick = { acao { Gestao.recalcularPagamento(corrida.id) } }, icone = PwIcones.RefreshCw, modifier = Modifier.weight(1f))
            }
            Espaco(6)
            Text("A pagar: ${moedaBr(corrida.valorMotoboy)}", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

/** Antes de soltar: mudar o modelo e os parâmetros do pagamento desta rota, com prévia antes de salvar. */
@Composable
private fun PagamentoEditavelG(corrida: com.pwexpress.feature.gestor.data.CorridaG, ocupado: Boolean, acao: (suspend () -> Result<Unit>) -> Unit) {
    val c = Pw.cores
    var aberto by remember(corrida.id) { mutableStateOf(false) }
    val salvas = remember(corrida.id) { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    val regras = remember(corrida.id) { androidx.compose.runtime.mutableStateMapOf<String, kotlinx.serialization.json.JsonElement>() }
    var previa by remember(corrida.id) { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    androidx.compose.runtime.LaunchedEffect(corrida.id, aberto) {
        if (!aberto || salvas.value != null) return@LaunchedEffect
        val r = Gestao.rpcJson("previa_pagamento_rota", kotlinx.serialization.json.buildJsonObject { put("p_corrida", corrida.id) }).getOrNull() as? kotlinx.serialization.json.JsonObject
        val atuais = (r?.get("regras_rota") as? kotlinx.serialization.json.JsonObject) ?: kotlinx.serialization.json.JsonObject(emptyMap())
        salvas.value = atuais; regras.clear(); regras.putAll(atuais)
    }
    val chave = kotlinx.serialization.json.JsonObject(regras.toMap()).toString()
    val mudou = salvas.value != null && kotlinx.serialization.json.JsonObject(regras.toMap()) != salvas.value
    androidx.compose.runtime.LaunchedEffect(chave) {
        if (!mudou) { previa = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(400)
        previa = Gestao.rpcJson("previa_pagamento_rota", kotlinx.serialization.json.buildJsonObject { put("p_corrida", corrida.id); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) }).getOrNull() as? kotlinx.serialization.json.JsonObject
    }
    Espaco(8)
    PwBotaoVidro(if (aberto) "Fechar" else "Mudar como paga antes de soltar", onClick = { aberto = !aberto }, icone = PwIcones.SlidersHorizontal, modifier = Modifier.fillMaxWidth())
    if (!aberto) return
    TextoDica("Vale só para esta rota, para quem pegar, por cima do cadastro do motoboy.")
    val modos = listOf("zonas" to "Zonas", "rota" to "Por km", "percentual" to "Percentual", "por_entrega" to "Por entrega", "fixo" to "Fixo")
    val atual = (regras["pag_modo"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: corrida.pagCalculo?.str("modo")
    Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        modos.forEach { (k, r) -> PwPilula(r, ICONE_MODO[k] ?: PwIcones.Wallet, { regras["pag_modo"] = kotlinx.serialization.json.JsonPrimitive(k) }, ligada = atual == k) }
    }
    EditorRegrasG(PARAMETROS.filter { it.grupo in GRUPOS_PAG && !it.soDev && it.chave != "dev_origem" }, regras, { k, v -> if (v == null) regras.remove(k) else regras[k] = v })
    previa?.let { pv ->
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.ouro.copy(alpha = 0.08f)).border(1.dp, c.ouro.copy(alpha = 0.4f), RoundedCornerShape(14.dp)).padding(10.dp)) {
            com.pwexpress.core.designsystem.PwRotulo("Com as mudanças")
            LinhasCalculo(pv)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        PwBotaoOuro("Salvar pagamento", onClick = { acao { Gestao.rpcJson("definir_regras_rota", kotlinx.serialization.json.buildJsonObject { put("p_corrida", corrida.id); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) }).map { salvas.value = kotlinx.serialization.json.JsonObject(regras.toMap()); Unit } } },
            icone = PwIcones.Check, habilitado = mudou && !ocupado, modifier = Modifier.weight(1f))
        if ((salvas.value?.size ?: 0) > 0) PwBotaoIcone(PwIcones.Trash2, "Voltar às regras de sempre", {
            acao { Gestao.rpcJson("definir_regras_rota", kotlinx.serialization.json.buildJsonObject { put("p_corrida", corrida.id); put("p_regras", kotlinx.serialization.json.JsonObject(emptyMap())) }).map { salvas.value = kotlinx.serialization.json.JsonObject(emptyMap()); regras.clear(); Unit } }
        }, tamanho = 48.dp)
    }
}

private val REGRA = mapOf(
    "mediana" to "entrega do meio", "percentil" to "percentil", "media" to "média das entregas",
    "mais_longe" to "entrega mais longe", "mais_perto" to "entrega mais perto", "total_rota" to "rota inteira",
)

@Composable
internal fun LinhasCalculo(calc: kotlinx.serialization.json.JsonObject) {
    val c = Pw.cores
    fun n(k: String) = (calc[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
    fun s(k: String) = (calc[k] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    @Composable fun Linha(a: String, b: String, forte: Boolean = false) {
        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
            Text(a, color = if (forte) c.texto else c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(b, color = c.texto, fontSize = 13.sp, fontWeight = if (forte) FontWeight.Bold else FontWeight.SemiBold)
        }
    }
    (calc["regras_rota"] as? kotlinx.serialization.json.JsonObject)?.takeIf { it.isNotEmpty() }?.let { TextoDica("Esta rota tem regras de pagamento próprias (${it.size}).") }
    val nomeModo = mapOf("zonas" to "Zonas", "rota" to "Por km", "percentual" to "Percentual", "por_entrega" to "Por entrega", "fixo" to "Fixo")
    when (s("modo")) {
        "combinado" -> {
            val comb = mapOf("soma" to "somados", "maior" to "vale o maior", "menor" to "vale o menor", "media" to "a média")[s("combinar")] ?: "somados"
            val partes = (calc["partes"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }.orEmpty()
            TextoDica("${partes.size} cálculos juntos · $comb")
            var aberta by remember { mutableStateOf<Int?>(null) }
            partes.forEachIndexed { i, pt ->
                val v = (pt["calculado"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: (pt["total"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { aberta = if (aberta == i) null else i }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (aberta == i) PwIcones.ChevronDown else PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(14.dp))
                    Text(nomeModo[(pt["parte"] as? kotlinx.serialization.json.JsonPrimitive)?.content] ?: "Cálculo", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(moedaBr(v), color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (aberta == i) Column(Modifier.padding(start = 14.dp)) { LinhasCalculo(pt) }
            }
            if ((n("acrescimo_pct") ?: 0.0) != 0.0) Linha("Acréscimo", "${numeroBr(n("acrescimo_pct")!!)}%")
            if ((n("acrescimo") ?: 0.0) != 0.0) Linha("Valor somado no fim", moedaBr(n("acrescimo")))
            if (s("limitado") == "true" && n("calculado") != null) Linha("Pelo cálculo", moedaBr(n("calculado")))
            Linha(if (s("limitado") == "true") "Fica (teto da rota)" else "O motoboy recebe", moedaBr(n("total")), true)
            return
        }
        "por_entrega" -> {
            (calc["linhas"] as? kotlinx.serialization.json.JsonArray)?.forEach { e ->
                val o = e as? kotlinx.serialization.json.JsonObject ?: return@forEach
                fun on(k: String) = (o[k] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
                Linha("Entrega ${(on("ordem") ?: 0.0).toInt() + 1} · ${numeroBr(on("km") ?: 0.0)} km × ${moedaBr(n("valor_km"))}" + if ((n("fixo_entrega") ?: 0.0) > 0) " + ${moedaBr(n("fixo_entrega"))}" else "", moedaBr(on("valor")))
            }
            if ((n("minimo") ?: 0.0) > 0) Linha("Mínimo", moedaBr(n("minimo")))
            Linha(if (s("limitado") == "true") "Fica (teto da rota)" else "Calculado", moedaBr(n("total")), true)
            return
        }
        "fixo" -> {
            if ((n("valor_rota_fixo") ?: 0.0) > 0) Linha("Por rota", moedaBr(n("valor_rota_fixo")))
            if ((n("por_entrega") ?: 0.0) > 0) Linha("Por entrega", "${n("entregas")?.toInt()} × ${moedaBr(n("por_entrega"))}")
            if ((n("por_coleta") ?: 0.0) > 0) Linha("Por coleta", "${n("coletas")?.toInt()} × ${moedaBr(n("por_coleta"))}")
            Linha(if (s("limitado") == "true") "Fica (teto da rota)" else "Calculado", moedaBr(n("total")), true)
            return
        }
    }
    (calc["zona"] as? kotlinx.serialization.json.JsonObject)?.let { z ->
        fun zn(k: String) = (z[k] as? kotlinx.serialization.json.JsonPrimitive)?.content
        val junta = zn("agrupada") == "true"
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(12.dp)).background((if (junta) c.verde else c.ouro).copy(alpha = 0.12f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Radar, null, tint = if (junta) c.verde else c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(if (junta) "Entregas na mesma zona" else "Entregas em zonas separadas", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                Text("Raio de ${zn("raio_m") ?: "?"} m por entrega · ${zn("juntas") ?: 0} de ${zn("entregas") ?: 0} se encontram" +
                    if (junta) " → paga o km da entrega mais perto + valor fixo por entrega a mais." else " → paga o percentual da rota.", color = c.texto2, fontSize = 12.5.sp)
            }
        }
    }
    val limitado = s("limitado") == "true"
    val avisoTeto = "Limitado a ${moedaBr(n("teto"))}: o motoboy nunca leva mais do que a rota paga."
    if (s("modo") == "percentual") {
        Linha("Valor da rota", moedaBr(n("valor_rota")))
        Linha("Parte do motoboy", "${numeroBr(n("pct") ?: 0.0)}%")
        Linha("O motoboy recebe", moedaBr(n("total")), true)
        if (limitado) TextoDica(avisoTeto)
        return
    }
    val dist = (calc["distancias_km"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() }.orEmpty()
    val regra = (REGRA[s("regra")] ?: s("regra").orEmpty()) + if (s("regra") == "percentil") " ${s("percentil")}%" else ""
    Linha("Km de referência ($regra)", "${numeroBr(n("km_referencia") ?: 0.0)} km × ${moedaBr(n("valor_km"))}")
    if (dist.isNotEmpty()) TextoDica("Entregas: ${dist.joinToString(", ") { numeroBr(it) }} km · ${if (s("distancia") == "acumulada") "acumulado na rota" else "coleta → entrega"}")
    if ((n("entregas_extras") ?: 0.0) > 0) Linha("Entregas a mais", "${n("entregas_extras")!!.toInt()} × ${moedaBr(n("valor_entrega_extra"))}")
    if ((n("coletas_extras") ?: 0.0) > 0 && (n("valor_coleta_extra") ?: 0.0) > 0) Linha("Coletas a mais", "${n("coletas_extras")!!.toInt()} × ${moedaBr(n("valor_coleta_extra"))}")
    if ((n("fixo") ?: 0.0) > 0) Linha("Fixo por rota", moedaBr(n("fixo")))
    if ((n("minimo") ?: 0.0) > 0) Linha("Mínimo", moedaBr(n("minimo")))
    if (limitado && n("calculado") != null) Linha("Pelo cálculo", moedaBr(n("calculado")))
    Linha(if (limitado) "Fica (teto da rota)" else "Calculado", moedaBr(n("total")), true)
    if (limitado) TextoDica(avisoTeto)
}


/** Por que o pedido ainda não virou rota (ou em que pé a rota está), com "Montar rota agora". */
@Composable
private fun SituacaoRotaG(pedidoId: String, chave: String) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var codigo by remember { mutableStateOf<String?>(null) }
    var motivo by remember { mutableStateOf<String?>(null) }
    var rodando by remember { mutableStateOf(false) }
    fun ler(r: Result<kotlinx.serialization.json.JsonElement>) {
        r.onSuccess { j -> val o = j as? kotlinx.serialization.json.JsonObject; codigo = o?.str("codigo"); motivo = o?.str("motivo") }
         .onFailure { e -> codigo = "erro"; motivo = if (e.message.orEmpty().contains("situacao_rota") || e.message.orEmpty().contains("roteirizar_pedido"))
             "Rode a migração 20261110000004_situacao_rota.sql para ver aqui por que o pedido ainda não virou rota." else "Erro: ${e.message}" }
    }
    androidx.compose.runtime.LaunchedEffect(pedidoId, chave) {
        while (true) {
            ler(Gestao.rpcJson("situacao_rota", kotlinx.serialization.json.buildJsonObject { put("p_pedido", kotlinx.serialization.json.JsonPrimitive(pedidoId)) }))
            kotlinx.coroutines.delay(20_000)
        }
    }
    val m = motivo ?: return
    val semRota = codigo?.startsWith("rota_") != true
    PwCartao(cor = if (semRota) androidx.compose.ui.graphics.Color(0xFFFB923C).copy(alpha = 0.6f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Route, null, tint = if (semRota) androidx.compose.ui.graphics.Color(0xFFFB923C) else c.ouro, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(m, color = c.texto, fontSize = 13.5.sp)
        }
        if (semRota && !m.startsWith("Rode a migração")) {
            Spacer(Modifier.size(8.dp))
            PwBotaoVidro(if (rodando) "Montando…" else "Montar rota agora", onClick = {
                rodando = true
                escopo.launch {
                    ler(Gestao.rpcJson("roteirizar_pedido", kotlinx.serialization.json.buildJsonObject { put("p_pedido", kotlinx.serialization.json.JsonPrimitive(pedidoId)) }))
                    rodando = false
                }
            }, icone = PwIcones.RefreshCw, modifier = Modifier.fillMaxWidth())
        }
    }
}


/** Mapa do pedido: coleta e entregas, o caminho da rota (quando já tem) e o motoboy ao vivo. */
@Composable
internal fun MapaPedidoG(p: PedidoG, altura: androidx.compose.ui.unit.Dp = 220.dp, foco: com.pwexpress.core.mapa.FocoMapa? = null) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val pontos = p.pontos.filter { it.lat != null && it.lng != null }
    if (pontos.isEmpty()) return
    var categorias by remember { mutableStateOf<List<com.pwexpress.feature.gestor.data.CategoriaG>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { categorias = runCatching { Gestao.catalogoPedido().second }.getOrDefault(emptyList()) }
    // cada ponto mostra o ícone do que vai nele; mais de um tipo de item = selo "+N"
    val pinos = pontos.mapIndexed { i, pt ->
        val doPonto = p.itens.filter { if (pt.coleta) it.coletaId == null || it.coletaId == pt.id else it.entregaId == null || it.entregaId == pt.id }
        val tipos = doPonto.map { it.categoriaId to it.descricao }.distinct()
        com.pwexpress.core.mapa.PinoMapa(pt.lat!!, pt.lng!!, pt.coleta, false, pt.status == "concluido", numero = i + 1,
            icone = if (pt.coleta && tipos.size != 1) "store" else categorias.firstOrNull { it.id == doPonto.firstOrNull()?.categoriaId }?.icone ?: (if (pt.coleta) "store" else "package"),
            mais = (tipos.size - 1).coerceAtLeast(0))
    }
    // rota que leva este pedido (mesmos endereços, criada depois dele)
    val rota = dados.corridas.firstOrNull { cor -> cor.teste == p.teste && cor.status != "cancelada" && cor.criadaEm >= p.criadoEm - 60_000 &&
        cor.paradas.any { pa -> p.pontos.any { it.endereco == pa.endereco } } }
    val moto = rota?.motoboyId?.let { id -> dados.posicao(id)?.takeIf { it.lat != null && it.lng != null }?.let { pos ->
        com.pwexpress.core.mapa.MarcadorMapa(id, pos.lat!!, pos.lng!!, corHex(c.ouro), dados.membro(id)?.nome?.substringBefore(" ") ?: "Motoboy", foto = dados.membro(id)?.foto, prioridade = 2) } }
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val trazer = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
    androidx.compose.runtime.LaunchedEffect(foco) { if (foco != null) trazer.bringIntoView() }
    Box(Modifier.fillMaxWidth().height(altura).clip(forma).border(1.dp, c.borda, forma).bringIntoViewRequester(trazer)) {
        val feito = com.pwexpress.core.mapa.rememberTrilhaCorrida(rota?.takeIf { it.motoboyId != null }?.id, rota?.ativa == true)
        val posMoto = moto?.takeIf { rota?.ativa == true }?.let { it.lat to it.lng }
        com.pwexpress.core.mapa.PwMapa(paradas = pinos, rota = rota?.rota, minhaPosicao = null, marcadores = listOfNotNull(moto),
            modifier = Modifier.fillMaxSize(), enquadrar = "${p.id}-${pinos.size}-${moto != null}", focar = foco, tituloTelaCheia = "Pedido ${p.numeroExibicao}",
            trilhas = if (feito.size >= 2) listOf(com.pwexpress.core.mapa.TrilhaMapa(feito + listOfNotNull(posMoto))) else emptyList(),
            restanteDe = posMoto)
        if (rota != null) Row(Modifier.align(Alignment.TopEnd).padding(10.dp)) {
            PwSelo("Rota ${rota.numeroExibicao}" + (rota.motoboyId?.let { " · ${dados.membro(it)?.nome?.substringBefore(" ") ?: ""}" } ?: ""), c.ouro, icone = PwIcones.Route)
        }
    }
}


/** Pontos numerados (como no mapa) com contato, instruções, itens de cada um e a entrega feita; itens com categoria e medidas; rotas. */
@Composable
private fun DetalhesPedidoG(p: PedidoG, onFocar: (Double, Double) -> Unit = { _, _ -> }) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var categorias by remember { mutableStateOf<List<com.pwexpress.feature.gestor.data.CategoriaG>>(emptyList()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { categorias = runCatching { Gestao.catalogoPedido().second }.getOrDefault(emptyList()) }
    PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Pontos · toque para ver no mapa")
        Spacer(Modifier.size(6.dp))
        p.pontos.forEachIndexed { i, pt ->
            // pacotes deste ponto (os presos a ele e os que vão para todas as entregas / saem de qualquer coleta)
            val itens = p.itens.filter { if (pt.coleta) it.coletaId == pt.id || it.coletaId == null else it.entregaId == pt.id || it.entregaId == null }
            Row(Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                .clickable(enabled = pt.lat != null) { onFocar(pt.lat!!, pt.lng!!) }.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(24.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (pt.coleta) c.ouro else c.texto), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = if (pt.coleta) c.sobreOuro else c.fundo, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text((if (pt.coleta) "Coleta" else "Entrega") + if (pt.status == "concluido") " · feita" else "", color = if (pt.coleta) c.ouro else c.texto2,
                        fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(pt.endereco, color = c.texto, fontSize = 14.sp)
                    listOfNotNull(pt.contatoNome, pt.contatoTelefone, pt.instrucoes).takeIf { it.isNotEmpty() }?.let { TextoDica(it.joinToString(" · ")) }
                    if (itens.isNotEmpty()) PacotesPontoG(itens, categorias, pt.coleta)
                    pt.concluidoEm?.let { TextoDica("✓ ${dataHora(it)}" + (pt.recebedorNome?.let { r -> " · recebido por $r" } ?: "")) }
                    SelosPontoG(pt.exigeFoto, pt.exigeNome, pt.exigeAssinatura, pt.exigeCodigo, de = pt.janelaDe, ate = pt.janelaAte)
                }
            }
        }
        if (p.distanciaM != null) TextoDica("Distância estimada: ${km(p.distanciaM)}")
    }
    if (p.itens.isNotEmpty()) PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Itens · por entrega")
        val entregas = p.pontos.filter { !it.coleta }
        p.itens.sortedBy { it2 -> entregas.indexOfFirst { it.id == it2.entregaId }.let { if (it < 0) 999 else it } }.forEach { it2 ->
            val k = categorias.firstOrNull { it.id == it2.categoriaId }
            Column(Modifier.padding(vertical = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Package2, null, tint = if (k?.perigoso == true) c.vermelho else c.ouro, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${it2.quantidade}× ${it2.descricao}", color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (k?.perigoso == true) PwSelo("Perigoso", c.vermelho)
                }
                val dest = p.pontos.indexOfFirst { it.id == it2.entregaId }
                Text(if (dest >= 0) "Vai para ${dest + 1}. ${p.pontos[dest].endereco.substringBefore(",")}" else "Vai para todas as entregas",
                    color = if (dest >= 0) c.ouro else c.texto3, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                TextoDica(listOfNotNull(k?.nome ?: "Sem categoria", it2.pesoKg?.let { "${numeroBr(it)} kg" },
                    it2.comprimento?.let { "${numeroBr(it)}×${numeroBr(it2.largura ?: 0.0)}×${numeroBr(it2.altura ?: 0.0)} cm" },
                    it2.valorDeclarado?.let { "declarado ${moedaBr(it)}" }, it2.observacoes).joinToString(" · "))
            }
        }
    }
    p.observacoes?.let { o -> PwCartao { com.pwexpress.core.designsystem.PwRotulo("Observações"); Text(o, color = c.texto, fontSize = 14.sp) } }
    // rotas que levam este pedido
    val rotas = dados.corridas.filter { cor -> cor.teste == p.teste && cor.criadaEm >= p.criadoEm - 60_000 && cor.paradas.any { pa -> p.pontos.any { it.endereco == pa.endereco } } }
    if (rotas.isNotEmpty()) PwCartao {
        com.pwexpress.core.designsystem.PwRotulo("Rotas")
        rotas.forEach { r ->
            TextoDica("Rota ${r.numeroExibicao} · ${com.pwexpress.feature.gestor.data.Status.rotulo[r.status] ?: r.status}" +
                (r.motoboyId?.let { " · ${dados.membro(it)?.nome ?: ""}" } ?: "") + " · ${r.entregas} entrega${if (r.entregas == 1) "" else "s"}")
        }
    }
}

/** Como o valor foi calculado (as mesmas linhas do site). */
@Composable
internal fun OrcamentoG(o: kotlinx.serialization.json.JsonObject, pontos: List<com.pwexpress.feature.gestor.data.PontoPedidoG> = emptyList()) {
    val c = Pw.cores
    fun num(e: kotlinx.serialization.json.JsonElement?) = (e as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
    fun txt(e: kotlinx.serialization.json.JsonElement?) = (e as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    var noMapa by com.pwexpress.core.designsystem.lembrarLigado("orcamento_no_mapa", false)
    val comMapa = pontos.count { it.lat != null && it.lng != null } >= 2
    Row(verticalAlignment = Alignment.CenterVertically) {
        com.pwexpress.core.designsystem.PwRotulo("Como chegou no valor", Modifier.weight(1f))
        if (comMapa) PwPilula(if (noMapa) "Esconder mapa" else "Ver no mapa", PwIcones.Mapa, { noMapa = !noMapa }, ligada = noMapa)
    }
    if (comMapa && noMapa) OrcamentoNoMapaG(o, pontos)
    if (txt(o["editado"]) == "true") TextoDica("Mudado à mão" + (txt(o["motivo"])?.let { ": $it" } ?: "") + (num((o["original"] as? kotlinx.serialization.json.JsonObject)?.get("total"))?.let { " · era ${moedaBr(it)}" } ?: ""))
    if (txt(o["modo_distancia"]) == "soma_entregas") TextoDica("Distância: coleta → cada entrega, somadas.")
    Spacer(Modifier.size(4.dp))
    (o["linhas"] as? kotlinx.serialization.json.JsonArray)?.forEach { l ->
        val x = l as? kotlinx.serialization.json.JsonObject ?: return@forEach
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(txt(x["rotulo"]).orEmpty(), color = c.texto, fontSize = 14.sp)
                val q = num(x["qtd"]); val u = num(x["unit"])
                if (q != null && u != null) Text("${numeroBr(q)}${txt(x["unidade"])?.let { " $it" } ?: ""} × ${moedaBr(u)}" + if (txt(x["fixo"]) == "true") " · fixo" else "", color = c.texto3, fontSize = 12.sp)
                txt(x["detalhe"])?.let { Text(it, color = c.texto3, fontSize = 12.sp) }
            }
            Text(moedaBr(num(x["valor"])), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    (o["percentuais"] as? kotlinx.serialization.json.JsonArray)?.forEach { l ->
        val x = l as? kotlinx.serialization.json.JsonObject ?: return@forEach
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text(txt(x["rotulo"]).orEmpty(), color = c.texto2, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text("+${numeroBr(num(x["pct"]) ?: 0.0)}%", color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    num(o["minimo"])?.takeIf { it > 0 }?.let { m ->
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Text("Mínimo", color = c.texto2, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(moedaBr(m), color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(c.borda))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Total", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(moedaBr(num(o["total"])), color = c.ouro, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
    val vol = num(o["volumes"])?.toInt() ?: 0
    TextoDica("$vol volume${if (vol == 1) "" else "s"} · peso real ${numeroBr(num(o["peso_real_kg"]) ?: 0.0)} kg · cubado ${numeroBr(num(o["peso_cubado_kg"]) ?: 0.0)} kg" +
        (if (txt(o["perigoso"]) == "true") " · perigoso" else ""))
}


/**
 * O orçamento desenhado: coleta e entregas numeradas, uma linha da coleta até cada entrega
 * e, no meio de cada linha, quanto aquela entrega custou (km × valor).
 */
@Composable
private fun OrcamentoNoMapaG(o: kotlinx.serialization.json.JsonObject, pontos: List<com.pwexpress.feature.gestor.data.PontoPedidoG>) {
    val c = Pw.cores
    val validos = pontos.filter { it.lat != null && it.lng != null }
    val coleta = validos.firstOrNull { it.coleta } ?: return
    val entregas = validos.filter { !it.coleta }
    val linhas = (o["linhas"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }.orEmpty()
    fun valorEntrega(i: Int) = linhas.firstOrNull { (it["chave"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "entrega_${i + 1}" }
        ?.let { (it["valor"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() }
    val pinos = listOf(com.pwexpress.core.mapa.PinoMapa(coleta.lat!!, coleta.lng!!, true, false, false, icone = "store", numero = 1)) +
        entregas.mapIndexed { i, e -> com.pwexpress.core.mapa.PinoMapa(e.lat!!, e.lng!!, false, false, false, numero = i + 2) }
    // pelas ruas (padrão): o caminho que o motoboy faz; linha reta só quando o gestor escolheu assim
    val reta = (o["trajeto"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "reta"
    val caminhos = remember(coleta.lat, coleta.lng, entregas.size, reta) { androidx.compose.runtime.mutableStateMapOf<Int, List<Pair<Double, Double>>>() }
    LaunchedEffect(coleta.lat, coleta.lng, entregas.size, reta) {
        if (reta) return@LaunchedEffect
        entregas.forEachIndexed { i, e ->
            com.pwexpress.core.mapa.Navegacao.rota(coleta.lat to coleta.lng, e.lat!! to e.lng!!)?.pontos?.takeIf { it.size >= 2 }?.let { caminhos[i] = it }
        }
    }
    val linhasMapa = entregas.mapIndexed { i, e -> com.pwexpress.core.mapa.TrilhaMapa(caminhos[i] ?: listOf(coleta.lat to coleta.lng, e.lat!! to e.lng!!)) }
    val rotulos = entregas.mapIndexedNotNull { i, e ->
        val meio = caminhos[i]?.let { it[it.size / 2] } ?: (((coleta.lat + e.lat!!) / 2) to ((coleta.lng + e.lng!!) / 2))
        valorEntrega(i)?.let { v -> com.pwexpress.core.mapa.MarcadorMapa("orc_$i", meio.first, meio.second, corHex(c.ouro), moedaBr(v), pino = false, prioridade = 2) }
    }
    val forma = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(220.dp).clip(forma).border(1.dp, c.borda, forma)) {
        com.pwexpress.core.mapa.PwMapa(paradas = pinos, rota = null, minhaPosicao = null, caminhoAuto = false, trilhas = linhasMapa, marcadores = rotulos, legenda = false,
            modifier = Modifier.fillMaxSize(), enquadrar = pinos.size, tituloTelaCheia = "Como chegou no valor")
    }
    TextoDica(if (reta) "Linha reta da coleta até cada entrega (ajuste do gestor); o valor no meio é o que aquela entrega custou."
        else "O caminho pelas ruas da coleta até cada entrega; o valor no meio é o que aquela entrega custou.")
}

/** "Mudar o cálculo": cada linha (quantidade × valor), os percentuais, o mínimo e o motivo. Igual ao site. */
@Composable
private fun EditarOrcamentoG(pedido: String, o: kotlinx.serialization.json.JsonObject, onFechar: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    fun num(e: kotlinx.serialization.json.JsonElement?) = (e as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull()
    fun txt(e: kotlinx.serialization.json.JsonElement?) = (e as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content
    fun br(v: Double?) = v?.let { numeroBr(it) } ?: ""
    class L(rot: String, val chave: String?, val unidade: String?, qtd: String, unit: String, valor: String, fixo: Boolean) {
        var rotulo by mutableStateOf(rot); var qtd by mutableStateOf(qtd); var unit by mutableStateOf(unit); var valor by mutableStateOf(valor); var fixo by mutableStateOf(fixo)
    }
    class P(rot: String, pct: String) { var rotulo by mutableStateOf(rot); var pct by mutableStateOf(pct) }
    fun d(s: String) = s.trim().replace(".", "").replace(",", ".").toDoubleOrNull()
    val linhas = remember(pedido) { androidx.compose.runtime.mutableStateListOf<L>().apply {
        (o["linhas"] as? kotlinx.serialization.json.JsonArray)?.forEach { e -> val x = e as? kotlinx.serialization.json.JsonObject ?: return@forEach
            add(L(txt(x["rotulo"]).orEmpty(), txt(x["chave"]), txt(x["unidade"]), br(num(x["qtd"])), br(num(x["unit"])), br(num(x["valor"])), txt(x["fixo"]) == "true")) }
    } }
    val pcts = remember(pedido) { androidx.compose.runtime.mutableStateListOf<P>().apply {
        (o["percentuais"] as? kotlinx.serialization.json.JsonArray)?.forEach { e -> val x = e as? kotlinx.serialization.json.JsonObject ?: return@forEach; add(P(txt(x["rotulo"]).orEmpty(), br(num(x["pct"])))) }
    } }
    var minimo by remember(pedido) { mutableStateOf(br(num(o["minimo"]))) }
    var motivo by remember(pedido) { mutableStateOf(txt(o["motivo"]).orEmpty()) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    fun valorDe(l: L) = if (d(l.qtd) != null && d(l.unit) != null) d(l.qtd)!! * d(l.unit)!! else d(l.valor) ?: 0.0
    val sub = linhas.filter { !it.fixo }.sumOf(::valorDe); val fixos = linhas.filter { it.fixo }.sumOf(::valorDe)
    val pct = pcts.sumOf { d(it.pct) ?: 0.0 }
    val total = maxOf(sub * (1 + pct / 100) + fixos, d(minimo) ?: 0.0)

    com.pwexpress.core.designsystem.PwRotulo("Mudar o cálculo")
    TextoDica("Mude a quantidade, o valor de cada um ou o valor da linha. O total é refeito na hora.")
    linhas.forEachIndexed { i, l ->
        val porQtd = l.qtd.isNotBlank() || l.unit.isNotBlank()
        Column(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(16.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(16.dp)).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", color = c.ouro, fontWeight = FontWeight.Black, fontSize = 15.sp, modifier = Modifier.width(22.dp))
                PwCampo(l.rotulo, { l.rotulo = it }, "Nome da linha", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                PwBotaoIcone(PwIcones.Trash2, "Tirar linha", { linhas.removeAt(i) }, tamanho = 40.dp)
            }
            com.pwexpress.core.designsystem.PwSegmentado(listOf("Quantidade × valor" to null, "Valor direto" to null), if (porQtd) 0 else 1, { m ->
                if (m == 0 && !porQtd) { l.qtd = "1"; l.unit = l.valor } else if (m == 1 && porQtd) { l.valor = numeroBr(valorDe(l)); l.qtd = ""; l.unit = "" }
            })
            if (porQtd) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PwCampo(l.qtd, { l.qtd = it }, l.unidade?.let { "Quantidade ($it)" } ?: "Quantidade", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                    Text("×", color = c.texto3, fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    PwCampo(l.unit, { l.unit = it }, "Valor de cada", teclado = KeyboardType.Decimal, prefixo = "R$ ", modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Valor da linha", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(moedaBr(valorDe(l)), color = c.texto, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            } else PwCampo(l.valor, { l.valor = it }, "Valor da linha", teclado = KeyboardType.Decimal, prefixo = "R$ ", modifier = Modifier.fillMaxWidth())
            com.pwexpress.core.designsystem.PwInterruptor("Taxa fixa", l.fixo, { l.fixo = it }, descricao = if (l.fixo) "Entra depois dos percentuais (não recebe acréscimo)." else "Recebe os percentuais abaixo.")
        }
    }
    PwBotaoVidro("Linha nova", onClick = { linhas.add(L("Outro", null, null, "", "", "0", false)) }, icone = PwIcones.Plus, modifier = Modifier.fillMaxWidth())
    if (pcts.isNotEmpty()) PwRotulo("Percentuais (sobre as linhas que não são fixas)")
    pcts.forEachIndexed { i, p ->
        Column(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(c.vidro).padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwCampo(p.rotulo, { p.rotulo = it }, "Nome do percentual", modifier = Modifier.weight(1f))
                Spacer(Modifier.width(6.dp))
                PwBotaoIcone(PwIcones.Trash2, "Tirar", { pcts.removeAt(i) }, tamanho = 40.dp)
            }
            PwCampo(p.pct, { p.pct = it }, "Porcentagem", teclado = KeyboardType.Decimal, sufixo = "%", modifier = Modifier.fillMaxWidth())
        }
    }
    PwBotaoVidro("Percentual novo", onClick = { pcts.add(P("Acréscimo", "0")) }, icone = PwIcones.Percent, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    PwCampo(minimo, { minimo = it }, "Mínimo do pedido", teclado = KeyboardType.Decimal, prefixo = "R$ ", modifier = Modifier.padding(top = 6.dp))
    PwCampo(motivo, { motivo = it }, "Motivo (aparece no pedido)", dica = "Ex.: combinado com o cliente")
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp).clip(RoundedCornerShape(16.dp)).background(c.ouro.copy(alpha = 0.1f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        @Composable fun l2(r: String, v: String) = Row(Modifier.fillMaxWidth()) { Text(r, color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(v, color = c.texto, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold) }
        l2("Linhas", moedaBr(sub))
        if (pct != 0.0) l2("Percentuais (${numeroBr(pct)}%)", moedaBr(sub * pct / 100))
        if (fixos != 0.0) l2("Taxas fixas", moedaBr(fixos))
        if ((d(minimo) ?: 0.0) > 0) l2("Mínimo", moedaBr(d(minimo)))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Novo total", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(moedaBr(total), color = c.ouro, fontWeight = FontWeight.Black, fontSize = 22.sp)
        }
    }
    PwErro(erro)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwBotaoVidro("Cancelar", onClick = onFechar, modifier = Modifier.weight(1f).height(52.dp))
        PwBotaoOuro(if (salvando) "Salvando…" else "Salvar", altura = 52.dp, onClick = {
            salvando = true
            escopo.launch {
                Gestao.editarOrcamento(pedido, kotlinx.serialization.json.buildJsonArray {
                    linhas.forEach { l -> add(kotlinx.serialization.json.buildJsonObject {
                        put("rotulo", l.rotulo.trim()); l.chave?.let { put("chave", it) }; l.unidade?.let { put("unidade", it) }; put("fixo", l.fixo)
                        if (d(l.qtd) != null && d(l.unit) != null) { put("qtd", d(l.qtd)!!); put("unit", d(l.unit)!!) } else put("valor", d(l.valor) ?: 0.0)
                    }) }
                }, kotlinx.serialization.json.buildJsonArray { pcts.forEach { p -> add(kotlinx.serialization.json.buildJsonObject { put("rotulo", p.rotulo.trim()); put("pct", d(p.pct) ?: 0.0) }) } },
                    d(minimo) ?: 0.0, motivo.trim().ifEmpty { null })
                    .onSuccess { PwToast.mostrar("Cálculo salvo. Novo total ${moedaBr(total)}.", TipoAviso.OK); Gestao.atualizar(); onFechar() }
                    .onFailure { erro = it.message }
                salvando = false
            }
        }, icone = PwIcones.Check, habilitado = !salvando, modifier = Modifier.weight(1f))
    }
}


/** Ícones dos pacotes de um ponto; toque num deles para ver o detalhe (igual ao site). */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PacotesPontoG(itens: List<com.pwexpress.feature.gestor.data.ItemPedidoG>, categorias: List<com.pwexpress.feature.gestor.data.CategoriaG>, coleta: Boolean) {
    val c = Pw.cores
    var aberto by remember(itens) { mutableStateOf<Int?>(null) }
    Spacer(Modifier.size(2.dp))
    // iguais (mesma categoria, descrição e destino) viram um só: "5× Documentos"
    data class Grupo(val i: Int, val qtd: Int)
    val grupos = itens.withIndex().groupBy { (_, x) -> Triple(x.categoriaId, x.descricao, if (coleta) x.coletaId else x.entregaId) }
        .values.map { g -> Grupo(g.first().index, g.sumOf { it.value.quantidade }) }
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        grupos.forEach { gr -> val i = gr.i; val it2 = itens[i]
            val k = categorias.firstOrNull { it.id == it2.categoriaId }
            val todas = if (coleta) it2.coletaId == null else it2.entregaId == null
            val forma = androidx.compose.foundation.shape.RoundedCornerShape(99.dp)
            Row(Modifier.clip(forma).background(if (aberto == i) c.ouro.copy(alpha = 0.16f) else c.vidro)
                .border(1.dp, if (aberto == i) c.ouro else if (k?.perigoso == true) c.vermelho.copy(alpha = 0.6f) else c.borda, forma)
                .clickable { aberto = if (aberto == i) null else i }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(iconeDoNome(k?.icone), null, tint = if (k?.perigoso == true) c.vermelho else c.ouro, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text((if (gr.qtd > 1) "${gr.qtd}× " else "") + it2.descricao, color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                if (todas) { Spacer(Modifier.width(4.dp)); Icon(PwIcones.Layers, "Vale para todas as entregas", tint = c.texto3, modifier = Modifier.size(12.dp)) }
            }
        }
    }
    aberto?.let { itens.getOrNull(it) }?.let { it2 ->
        val k = categorias.firstOrNull { it.id == it2.categoriaId }
        TextoDica(listOfNotNull(k?.nome ?: "Sem categoria", "${it2.quantidade} un.", it2.pesoKg?.let { "${numeroBr(it)} kg cada" },
            it2.comprimento?.let { "${numeroBr(it)}×${numeroBr(it2.largura ?: 0.0)}×${numeroBr(it2.altura ?: 0.0)} cm" },
            it2.valorDeclarado?.let { "declarado ${moedaBr(it)}" }, it2.observacoes, "perigoso".takeIf { k?.perigoso == true }).joinToString(" · "))
    }
}


// ------------------------------------------------------------------ rotas do pedido (revisão)

private val GRUPOS_PAG = setOf("pagamento", "pag_combinar", "pag_zonas", "pag_km", "pag_entrega", "pag_fixo")
private val EDITAVEIS = setOf("montada", "agendada", "aguardando_motoboy", "sem_motoboy")

/**
 * Prévia das rotas que o pedido forma (nada é gravado), "Aprovar e montar" (segura as rotas) e,
 * em cada rota: ordem, separar, mover, juntar, valor, regras de pagamento com prévia e soltar. Igual ao site.
 */
@Composable
internal fun RotasDoPedidoG(pedidoId: String, status: String, chave: String) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var dados by remember(pedidoId) { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    var sug by remember(pedidoId) { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    var aberta by remember(pedidoId) { mutableIntStateOf(0) }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    var revisando by remember(pedidoId) { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(pedidoId, chave, versao) {
        Gestao.rpcJson("previa_rotas_pedido", kotlinx.serialization.json.buildJsonObject { put("p_pedido", pedidoId) })
            .onSuccess { dados = it as? kotlinx.serialization.json.JsonObject; erro = null }
            .onFailure { erro = if (it.message.orEmpty().contains("previa_rotas")) "Rode a migração 20261110000019 para ver a prévia das rotas." else it.message }
        sug = Gestao.rpcJson("sugestoes_rota", kotlinx.serialization.json.buildJsonObject { put("p_pedido", pedidoId) }).getOrNull() as? kotlinx.serialization.json.JsonObject
    }
    fun rodar(msg: String, bloco: suspend () -> Result<*>) {
        ocupado = true
        escopo.launch {
            bloco().onSuccess { PwToast.mostrar(msg, TipoAviso.OK); versao++; Gestao.atualizar() }.onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
            ocupado = false
        }
    }
    fun rpc(f: String, args: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): suspend () -> Result<*> = { Gestao.rpcJson(f, kotlinx.serialization.json.buildJsonObject(args)) }

    PwCartao {
        val previa = dados?.get("previa")?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content == "true" } ?: true
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Route, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text(if (previa) "Prévia das rotas" else "Rotas deste pedido", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            com.pwexpress.core.designsystem.PwDica("Rotas do pedido", "A prévia mostra como o roteirizador vai montar as rotas (com outros pedidos, se juntar) e quanto cada motoboy recebe. “Aprovar e montar” cria as rotas e segura: você muda a ordem, separa, junta, muda o valor e as regras de pagamento, e solta quando quiser.")
        }
        PwErro(erro)
        val d = dados ?: run { if (erro == null) PwCarregando(texto = "Montando a prévia…"); return@PwCartao }
        val rotas = (d["rotas"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }.orEmpty()
        if (previa) TextoDica("Nada foi gravado ainda: é como o sistema montaria agora.")
        if (previa && status == "recebido") {
            Espaco(6)
            if (!revisando) PwBotaoOuro("Revisar rota", onClick = { revisando = true }, icone = PwIcones.SlidersHorizontal, habilitado = !ocupado)
            else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.ouro.copy(alpha = 0.1f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.SlidersHorizontal, null, tint = c.ouro, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                Text("Revisando: escolha como o motoboy vai ser pago. A prévia muda na hora; aprove no botão do fim da janela.", color = c.texto, fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                PwBotaoIcone(PwIcones.X, "Parar de revisar", { revisando = false }, tamanho = 30.dp)
            }
        }
        sug?.let { sg ->
            val acoes = (sg["acoes"] as? kotlinx.serialization.json.JsonObject)?.mapNotNull { (k, v) ->
                val nm = mapOf("separar" to "separou entregas", "mover" to "moveu entregas", "juntar" to "juntou rotas", "ordenar" to "mudou a ordem", "regras" to "mudou o pagamento", "valor" to "mudou o valor")[k]
                nm?.let { "$it ${(v as? kotlinx.serialization.json.JsonPrimitive)?.content}×" }
            }.orEmpty()
            if (acoes.isNotEmpty()) {
                Espaco(6)
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.ouro.copy(alpha = 0.1f)).padding(10.dp)) {
                    Icon(PwIcones.Sparkles, null, tint = c.ouro, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                    Text("Com este cliente, nos últimos 60 dias, você ${acoes.joinToString(", ")}.", color = c.texto, fontSize = 13.sp)
                }
            }
        }
        if (rotas.isEmpty()) { TextoDica(if (previa) "O roteirizador ainda não formaria rota com este pedido (veja as regras de rota e o pagamento antecipado)." else "Sem rotas."); return@PwCartao }
        if (rotas.size > 1) { Espaco(6); com.pwexpress.core.designsystem.PwFiltros(rotas.mapIndexed { i, r -> (if (previa) "Rota ${i + 1}" else "#${r.str("numero")}") to 0 }, aberta.coerceAtMost(rotas.lastIndex), { aberta = it }) }
        val r = rotas[aberta.coerceAtMost(rotas.lastIndex)]
        val editaveis = rotas.filter { !previa && it.str("status") in EDITAVEIS }
        androidx.compose.runtime.key(r.str("id"), versao) { CartaoRotaG(r, previa, editaveis.filter { it.str("id") != r.str("id") }, sug, ocupado, ::rodar, ::rpc, pedidoId, revisando) }
    }
}

@Composable
private fun CartaoRotaG(
    r: kotlinx.serialization.json.JsonObject, previa: Boolean, outras: List<kotlinx.serialization.json.JsonObject>, sug: kotlinx.serialization.json.JsonObject?, ocupado: Boolean,
    rodar: (String, suspend () -> Result<*>) -> Unit, rpc: (String, kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) -> (suspend () -> Result<*>),
    pedidoId: String = "", revisando: Boolean = false,
) {
    val c = Pw.cores
    val id = r.str("id") ?: return
    val editavel = !previa && r.str("status") in EDITAVEIS
    // na prévia, "Revisar rota" deixa mudar o pagamento (guardado no pedido, passa para a rota quando nascer)
    val mudaPagamento = editavel || (previa && revisando)
    val original = (r["paradas"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }.orEmpty()
    val paradas = remember(id) { androidx.compose.runtime.mutableStateListOf<kotlinx.serialization.json.JsonObject>().apply { addAll(original) } }
    val regrasSalvas = (r["pag_regras"] as? kotlinx.serialization.json.JsonObject) ?: kotlinx.serialization.json.JsonObject(emptyMap())
    val regras = remember(id) { androidx.compose.runtime.mutableStateMapOf<String, kotlinx.serialization.json.JsonElement>().apply { putAll(regrasSalvas) } }
    var verRegras by remember(id) { mutableStateOf(regrasSalvas.isNotEmpty()) }
    var previaPag by remember(id) { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    var valorTxt by remember(id) { mutableStateOf(r.str("valor_manual")?.toDoubleOrNull()?.let { numeroBr(it) } ?: "") }
    val mudouRegras = kotlinx.serialization.json.JsonObject(regras.toMap()) != regrasSalvas
    val mudouOrdem = paradas.map { it.str("id") } != original.map { it.str("id") }
    val chaveRegras = kotlinx.serialization.json.JsonObject(regras.toMap()).toString()
    androidx.compose.runtime.LaunchedEffect(chaveRegras) {
        if (!mudouRegras || previa) { previaPag = null; return@LaunchedEffect }
        kotlinx.coroutines.delay(400)
        previaPag = Gestao.rpcJson("previa_pagamento_rota", kotlinx.serialization.json.buildJsonObject { put("p_corrida", id); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) }).getOrNull() as? kotlinx.serialization.json.JsonObject
    }
    val entregas = paradas.count { it.str("tipo") == "entrega" }
    // pagamento por zonas: cada entrega mostra o próprio raio no mapa
    val raioZona = (previaPag ?: r.obj("pagamento"))?.let { pg -> pg.obj("zona") ?: (pg["partes"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonObject)?.obj("zona") }?.firstOrNull() }?.dbl("raio_m")
    Espaco(8)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(if (previa) "Rota nova" else "Rota #${r.str("numero")}", color = c.texto, fontWeight = FontWeight.Bold)
            TextoDica("$entregas entrega${if (entregas == 1) "" else "s"} · ${km(r.str("distancia_m")?.toDoubleOrNull()?.toInt())}" +
                ((r.long("outros_pedidos") ?: 0).takeIf { it > 0 }?.let { " · junto com $it outro(s) pedido(s)" } ?: ""))
        }
        PwSelo(if (previa) "prévia" else if (r.str("status") == "montada") "segura" else (r.str("status") ?: "").replace("_", " "), if (r.str("status") == "montada") c.ouro else c.texto2)
    }
    Espaco(6)
    val forma = RoundedCornerShape(16.dp)
    Box(Modifier.fillMaxWidth().height(200.dp).clip(forma).border(1.dp, c.borda, forma)) {
        com.pwexpress.core.mapa.PwMapa(
            paradas = paradas.mapIndexedNotNull { i, x -> x.dbl("lat")?.let { la -> x.dbl("lng")?.let { lo ->
                com.pwexpress.core.mapa.PinoMapa(la, lo, x.str("tipo") == "coleta", false, false, numero = i + 1,
                    raioM = if (x.str("tipo") == "entrega") raioZona else null) } } },
            rota = if (mudouOrdem) null else r.str("rota_planejada"), minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(200.dp),
            enquadrar = id + paradas.size, tituloTelaCheia = if (previa) "Prévia da rota" else "Rota #${r.str("numero")}",
        )
    }
    Espaco(6)
    paradas.forEachIndexed { i, x ->
        val deste = x.bool("deste_pedido")
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(if (deste) c.vidro else c.vidro.copy(alpha = 0.4f)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(22.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (x.str("tipo") == "coleta") c.ouro else c.texto), contentAlignment = Alignment.Center) {
                Text("${i + 1}", color = if (x.str("tipo") == "coleta") c.sobreOuro else c.fundo, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text((if (x.str("tipo") == "coleta") "Coleta" else "Entrega") + if (!deste) " · outro pedido" else "", color = c.texto2, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                Text(x.str("endereco").orEmpty().split(",").take(2).joinToString(","), color = c.texto, fontSize = 13.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            if (editavel) {
                PwBotaoIcone(PwIcones.ChevronUp, "Subir", { if (i > 0) { val t = paradas[i]; paradas[i] = paradas[i - 1]; paradas[i - 1] = t } }, tamanho = 32.dp, habilitado = i > 0)
                PwBotaoIcone(PwIcones.ChevronDown, "Descer", { if (i < paradas.lastIndex) { val t = paradas[i]; paradas[i] = paradas[i + 1]; paradas[i + 1] = t } }, tamanho = 32.dp, habilitado = i < paradas.lastIndex)
                if (x.str("tipo") == "entrega" && (entregas > 1 || outras.isNotEmpty()))
                    MenuMoverEntrega(outras.map { o -> o.str("id")!! to "Rota #${o.str("numero")}" }, podeSeparar = entregas > 1, habilitado = !ocupado) { dest ->
                        rodar(if (dest == null) "Entrega separada numa rota nova." else "Entrega movida.", rpc("mover_entrega") { put("p_parada", x.str("id")); if (dest == null) put("p_destino", kotlinx.serialization.json.JsonNull) else put("p_destino", dest) })
                    }
            }
        }
    }
    if (editavel && mudouOrdem) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwBotaoOuro("Salvar a ordem", onClick = { rodar("Ordem salva.", rpc("ordenar_paradas") { put("p_corrida", id); put("p_ordem", kotlinx.serialization.json.JsonArray(paradas.map { kotlinx.serialization.json.JsonPrimitive(it.str("id")) })) }) },
            icone = PwIcones.Check, modifier = Modifier.weight(1f))
        PwBotaoVidro("Desfazer", onClick = { paradas.clear(); paradas.addAll(original) }, icone = PwIcones.RotateCcw, modifier = Modifier.weight(1f))
    }
    Espaco(8)
    // valor e o que o motoboy recebe
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Valor da rota", color = c.texto2, modifier = Modifier.weight(1f))
        Text(moedaBr(r.dbl("valor")), color = c.texto, fontWeight = FontWeight.Black, fontSize = 18.sp)
    }
    if (r.dbl("valor_manual") != null) TextoDica("Valor fixo")
    if (editavel) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwCampo(valorTxt, { valorTxt = it }, "Fixo", teclado = KeyboardType.Decimal, prefixo = "R$ ", modifier = Modifier.weight(1f))
        val vn = valorTxt.trim().takeIf { it.isNotEmpty() }?.replace(".", "")?.replace(",", ".")?.toDoubleOrNull()
        PwBotaoVidro(if (valorTxt.isBlank()) "Usar a soma" else "Salvar", onClick = {
            rodar(if (vn == null) "Valor volta a ser a soma dos pedidos." else "Valor da rota salvo.", rpc("definir_valor_rota") { put("p_corrida", id); if (vn != null) put("p_valor", vn) else put("p_valor", kotlinx.serialization.json.JsonNull) })
        }, habilitado = !ocupado && (valorTxt.isBlank() || vn != null), modifier = Modifier.padding(top = 8.dp).height(56.dp))
    }
    if (editavel && valorTxt.isBlank()) TextoDica("Vazio: a rota vale a soma dos pedidos.")
    Espaco(8)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.vidro).padding(12.dp)) {
        PwRotulo("O motoboy que pegar recebe" + if (previaPag != null) " (prévia com as mudanças)" else "")
        Espaco(4)
        (previaPag ?: r.obj("pagamento"))?.let { LinhasCalculo(it) } ?: TextoDica("Sem cálculo ainda.")
    }
    // raio da zona direto aqui (sem ir em Ajustes): vale para esta rota e a prévia mostra na hora
    if (mudaPagamento && raioZona != null) {
        val raioAtual = (regras["zona_entrega_raio_m"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: raioZona
        Espaco(8)
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(PwIcones.Radar, null, tint = c.ouro, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f)) {
                Text("Raio da mesma zona", color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                TextoDica("Só nesta rota")
            }
            PwBotaoIcone(PwIcones.Minus, "Diminuir o raio", { regras["zona_entrega_raio_m"] = kotlinx.serialization.json.JsonPrimitive((raioAtual - 100).coerceAtLeast(100.0)) }, tamanho = 40.dp, habilitado = !ocupado)
            Text("${raioAtual.toInt()} m", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.widthIn(min = 64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            PwBotaoIcone(PwIcones.Plus, "Aumentar o raio", { regras["zona_entrega_raio_m"] = kotlinx.serialization.json.JsonPrimitive((raioAtual + 100).coerceAtMost(20000.0)) }, tamanho = 40.dp, habilitado = !ocupado)
        }
        if (mudouRegras) PwBotaoOuro("Salvar", onClick = {
            if (previa) rodar("Raio salvo nesta rota. A prévia já mostra o valor novo.", rpc("definir_regras_rota_pedido") { put("p_pedido", pedidoId); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) })
            else rodar("Raio salvo nesta rota.", rpc("definir_regras_rota") { put("p_corrida", id); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) })
        }, icone = PwIcones.Check, habilitado = !ocupado, modifier = Modifier.padding(top = 6.dp))
    }
    if (mudaPagamento) {
        Espaco(8)
        PwBotaoVidro((if (verRegras || (previa && revisando)) "Esconder" else "Mudar") + " como o motoboy é pago" + if (regrasSalvas.isNotEmpty()) " (${regrasSalvas.size})" else "",
            onClick = { verRegras = !verRegras }, icone = PwIcones.SlidersHorizontal, modifier = Modifier.fillMaxWidth())
        if (verRegras || (previa && revisando)) {
            TextoDica(if (previa) "Vale para a rota deste pedido, para quem pegar, por cima do cadastro do motoboy. Salve e a prévia mostra o valor novo."
                else "Valem só para esta rota, para quem pegar, por cima do cadastro do motoboy. Mude e veja a prévia acima antes de salvar.")
            // modelo em um toque (os parâmetros ficam logo abaixo)
            val modos = listOf("zonas" to "Zonas", "rota" to "Por km", "percentual" to "Percentual", "por_entrega" to "Por entrega", "fixo" to "Fixo")
            val atualModo = (regras["pag_modo"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: (r.obj("pagamento")?.str("modo"))
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                modos.forEach { (k, rot) -> PwPilula(rot, ICONE_MODO[k] ?: PwIcones.Wallet, { regras["pag_modo"] = kotlinx.serialization.json.JsonPrimitive(k) }, ligada = atualModo == k) }
            }
            (sug?.get("regras_comuns") as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? kotlinx.serialization.json.JsonObject }?.forEach { rc ->
                PwPilula("Usar as de costume (${rc.long("vezes")}×)", PwIcones.Sparkles, { regras.clear(); rc.obj("regras")?.let { regras.putAll(it) } })
            }
            EditorRegrasG(PARAMETROS.filter { it.grupo in GRUPOS_PAG && !it.soDev && it.chave != "dev_origem" }, regras, { k, v -> if (v == null) regras.remove(k) else regras[k] = v })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwBotaoOuro("Salvar pagamento", onClick = {
                    if (previa) rodar("Pagamento da rota salvo. A prévia já mostra o valor novo.", rpc("definir_regras_rota_pedido") { put("p_pedido", pedidoId); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) })
                    else rodar("Regras desta rota salvas.", rpc("definir_regras_rota") { put("p_corrida", id); put("p_regras", kotlinx.serialization.json.JsonObject(regras.toMap())) })
                }, icone = PwIcones.Check, habilitado = mudouRegras && !ocupado, modifier = Modifier.weight(1f))
                if (regrasSalvas.isNotEmpty()) PwBotaoIcone(PwIcones.Trash2, "Voltar às regras de sempre", {
                    if (previa) rodar("O pagamento volta ao de sempre.", rpc("definir_regras_rota_pedido") { put("p_pedido", pedidoId); put("p_regras", kotlinx.serialization.json.JsonObject(emptyMap())) })
                    else rodar("A rota volta às regras de sempre.", rpc("definir_regras_rota") { put("p_corrida", id); put("p_regras", kotlinx.serialization.json.JsonObject(emptyMap())) })
                }, tamanho = 48.dp, habilitado = !ocupado)
            }
        }
    }
    if (editavel) {
        if (outras.isNotEmpty()) {
            Espaco(6)
            com.pwexpress.core.designsystem.PwSelecao("", outras.map { o -> o.str("id") to "Juntar com a rota #${o.str("numero")}" }, null as String?, { dest ->
                rodar("Rotas juntadas.", rpc("juntar_rotas") { put("p_origem", id); put("p_destino", dest) })
            }, vazio = "Juntar esta rota com…")
        }
        if (r.str("status") == "montada") {
            Espaco(6)
            PwBotaoOuro("Soltar para os motoboys", onClick = { rodar("Rota solta para os motoboys.", rpc("liberar_rota") { put("p_corrida", id) }) }, icone = PwIcones.Send, habilitado = !ocupado)
        }
    }
    TextoDica("Tudo o que você muda aqui (ordem, separar, juntar, valor e regras) fica registrado e ajuda o sistema a sugerir da próxima vez.")
}
