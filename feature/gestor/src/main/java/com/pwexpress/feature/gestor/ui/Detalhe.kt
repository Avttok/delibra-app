package com.pwexpress.feature.gestor.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.EventoG
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put

private val ROTULO_EVENTO = mapOf(
    "parada_liberada" to "Parada liberada pelo gestor", "pediu_pular" to "Motoboy pediu para liberar uma parada", "emergencia" to "Botão de emergência",
    "fechada_sem_noticia" to "Fechou sozinha (motoboy sem sinal)", "confirmada_offline" to "Etapas sem internet chegaram",
    "criada" to "Corrida criada", "oferta_enviada" to "Oferta enviada", "oferta_recusada" to "Oferta recusada", "oferta_expirada" to "Oferta expirou",
    "sem_motoboy" to "Nenhum motoboy disponível", "redespachada" to "Novo despacho", "aceita" to "Aceita", "chegou" to "Chegou na parada",
    "parada_concluida" to "Parada concluída", "concluida" to "Corrida concluída", "cancelada" to "Cancelada", "reservada" to "Reservada",
    "reserva_cancelada" to "Reserva desfeita", "reagendada" to "Horário mudado", "planejada_despachada" to "Planejada foi para o despacho",
    "planejada_atrasada" to "Atrasada: não iniciada no horário",
    "montada" to "Rota montada", "liberada" to "Rota liberada", "no_quadro" to "No quadro para todos", "rota_pegada" to "Pegou a rota no quadro",
    "entrega_movida" to "Entrega movida", "otimizada" to "Caminho otimizado", "pagamento_ajustado" to "Pagamento ajustado",
)

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun DetalheCorridaG(id: String, onVoltar: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val dados by Gestao.dados.collectAsState()
    val corrida = dados.corridas.firstOrNull { it.id == id }
    var eventos by remember { mutableStateOf<List<EventoG>>(emptyList()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf(false) }
    var cancelarPergunta by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    LaunchedEffect(id, corrida?.status) { eventos = Gestao.eventos(id) }

    fun acao(bloco: suspend () -> Result<Unit>) {
        ocupado = true
        escopo.launch { bloco().onFailure { erro = it.message }.onSuccess { erro = null; Gestao.atualizar(); eventos = Gestao.eventos(id) }; ocupado = false }
    }

    var focoC by remember(id) { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }
    PwPagina {
        Cabecalho(
            if (corrida != null) "Corrida ${corrida.numeroExibicao}" else "Corrida", onVoltar,
            subtitulo = corrida?.let { "${dados.cliente(it)} · ${dataHora(it.criadaEm)}" },
        ) {
            if (corrida?.teste == true) Icon(PwIcones.FlaskConical, "Corrida de teste", tint = c.violeta, modifier = Modifier.size(20.dp))
            if (corrida != null) com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Share2, "Mandar no chat", {
                com.pwexpress.core.chat.Chat.compartilhar(com.pwexpress.core.chat.Anexo("rota", corrida.id, "Rota ${corrida.numeroExibicao}",
                    "${dados.cliente(corrida)} · ${corrida.paradas.count { !it.coleta }} entrega(s)"))
            })
        }
        if (corrida == null) { if (!dados.carregado) PwCarregando() else PwErro("Essa corrida não existe mais (pode ter sido apagada)."); return@PwPagina }

        val trazerMapa = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
        androidx.compose.runtime.LaunchedEffect(focoC) { if (focoC != null) trazerMapa.bringIntoView() }
        Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(22.dp)).bringIntoViewRequester(trazerMapa)) {
            val posMoto = dados.posicao(corrida.motoboyId)?.takeIf { corrida.ativa }?.let { p -> p.lat?.let { la -> p.lng?.let { lo -> la to lo } } }
            val feito = com.pwexpress.core.mapa.rememberTrilhaCorrida(corrida.id.takeIf { corrida.motoboyId != null }, corrida.ativa)
            PwMapa(focar = focoC,
                paradas = corrida.paradas.map { PinoMapa(it.lat, it.lng, it.coleta, false, it.status == "concluida") },
                rota = corrida.rota, minhaPosicao = null,
                marcadores = listOfNotNull(posMoto?.let { (la, lo) ->
                    val mb = dados.membro(corrida.motoboyId)
                    com.pwexpress.core.mapa.MarcadorMapa(corrida.motoboyId ?: "moto", la, lo, corHex(c.ouro), mb?.nome?.substringBefore(" ") ?: "Motoboy", foto = mb?.foto, prioridade = 2)
                }),
                modifier = Modifier.fillMaxWidth().height(220.dp),
                trilhas = if (feito.size >= 2) listOf(com.pwexpress.core.mapa.TrilhaMapa(feito + listOfNotNull(posMoto))) else emptyList(),
                restanteDe = posMoto,
            )
        }

        PwCartao {
            Text(Status.rotulo[corrida.status] ?: corrida.status, style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 22.sp), color = corStatus(corrida.status, c))
            dados.ofertas.firstOrNull { it.corridaId == corrida.id }?.let { o ->
                var resta by remember(o.id) { mutableStateOf(0L) }
                LaunchedEffect(o.id) { while (true) { resta = ((o.expiraEm - System.currentTimeMillis()) / 1000).coerceAtLeast(0); delay(1000) } }
                TextoDica("Enviada para ${dados.membro(o.motoboyId)?.nome ?: "motoboy"} (${km(o.distanciaM)} da coleta) · responde em ${resta}s")
            }
            dados.nomeMotoboy(corrida)?.let { n ->
                Espaco(6)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Bike, null, tint = c.texto2, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${if (corrida.status == Status.RESERVADA) "Reservada para" else "Motoboy"}: $n", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Espaco(10)
            PwNumero("Valor da corrida", moedaBr(corrida.valor), Modifier.fillMaxWidth(), icone = PwIcones.Wallet, destaque = true)
            Espaco(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Planejado", km(corrida.distanciaPlanejadaM), Modifier.weight(1f), icone = PwIcones.Ruler)
                PwNumero("Real", km(corrida.distanciaRealM), Modifier.weight(1f), icone = PwIcones.Route)
            }
            if (corrida.fatorEntrega > 0 || corrida.fatorColeta > 0) {
                Espaco(6)
                TextoDica("Base ${moedaBr(corrida.valorBase)}" + (if (corrida.fatorEntrega > 0) " · percurso +${numeroBr(corrida.fatorEntrega)}×" else "") + (if (corrida.fatorColeta > 0) " · motoboy longe da coleta +${numeroBr(corrida.fatorColeta)}×" else ""))
            }
            if (corrida.valorMotoboy != null) {
                Espaco(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwNumero(if (corrida.pagCalculo != null && corrida.pagCalculo["modo"]?.toString()?.contains("percentual") != true) "Motoboy (por km)" else "Motoboy ${corrida.repassePct?.let { numeroBr(it) } ?: "—"}%", moedaBr(corrida.valorMotoboy), Modifier.weight(1f), icone = PwIcones.Bike)
                    PwNumero("Gestão", moedaBr(corrida.valorGestao), Modifier.weight(1f), icone = PwIcones.Landmark)
                }
                if ((corrida.valorDev ?: 0.0) > 0 && com.pwexpress.core.perfil.Perfis.perfil.value?.dev == true) {
                    Espaco(8)
                    PwNumero("Plataforma ${corrida.devPct?.let { numeroBr(it) } ?: "—"}%", moedaBr(corrida.valorDev), Modifier.fillMaxWidth(), icone = PwIcones.CodeXml)
                }
            }
            Espaco(6)
            TextoDica(if (corrida.exigirNoLocal) "Etapas exigem estar no local (raio de ${corrida.raioChegadaM} m)" else "Etapas liberadas em qualquer lugar")
            if (corrida.exigeCodigo && corrida.codigoEntrega != null) {
                Espaco(6)
                Text("Código de entrega: ${corrida.codigoEntrega}", color = c.ouro, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        // de qual pedido veio (rotas montadas de pedidos): volta ao pedido central
        val pedidosDaRota = corrida.paradas.mapNotNull { it.pedidoId }.distinct()
        if (pedidosDaRota.isNotEmpty()) {
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                pedidosDaRota.forEach { pid ->
                    val pd = dados.pedidos.firstOrNull { it.id == pid }
                    PwBotaoVidro(if (pd != null) "Ver pedido ${if (pd.teste) "T-" else "P-"}${pd.numero}" else "Ver pedido", onClick = { GestorNav.abrirPedido(pid) }, icone = PwIcones.ClipboardList)
                }
            }
        }
        ChamadosDaCorridaG(corrida.id) { eventos = Gestao.eventos(id) }
        PwErro(erro)
        if (corrida.status in setOf(Status.ACEITA, Status.EM_ANDAMENTO) && corrida.motoboyId != null && corrida.paradas.none { it.coleta && it.status == "concluida" }
            && dados.cfgBool("passar_corrida_ligado", true)) {
            var para by remember { mutableStateOf<String?>(null) }
            var motivo by remember { mutableStateOf("") }
            var aberto by remember { mutableStateOf(false) }
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.ArrowLeftRight, null, tint = c.ouro, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        PwRotulo("Passar para outro motoboy")
                        TextoDica("Dá enquanto nada foi coletado. ${dados.membro(corrida.motoboyId)?.nome?.substringBefore(" ") ?: "O motoboy"} é avisado.")
                    }
                    PwBotaoIcone(if (aberto) PwIcones.ChevronUp else PwIcones.ChevronDown, if (aberto) "Fechar" else "Abrir", { aberto = !aberto }, tamanho = 38.dp)
                }
                if (aberto) {
                    Espaco(8)
                    PwSelecao("Para quem", listOf<Pair<String?, String>>(null to "O sistema escolhe (mais perto)") +
                        dados.equipe.filter { it.motoboy && it.modoTeste == corrida.teste && it.id != corrida.motoboyId }.map { it.id to it.nome }, para, { para = it })
                    Espaco(8)
                    PwCampo(motivo, { motivo = it }, "Motivo (o motoboy vê)", dica = "Opcional")
                    Espaco(8)
                    PwBotaoOuro(if (ocupado) "Passando…" else if (para == null) "Devolver ao sistema" else "Passar para ${dados.membro(para)?.nome?.substringBefore(" ") ?: "ele"}",
                        onClick = { acao { Gestao.rpcJson("passar_corrida", kotlinx.serialization.json.buildJsonObject {
                            put("p_corrida", corrida.id); para?.let { put("p_motoboy", it) }; motivo.trim().takeIf { it.isNotEmpty() }?.let { put("p_motivo", it) }
                        }).map { } } }, icone = PwIcones.ArrowLeftRight, habilitado = !ocupado)
                }
            }
        }
        if (corrida.status == Status.AGUARDANDO_PAGAMENTO) {
            PwCartao(cor = Color(0xFF38BDF8).copy(alpha = 0.5f)) {
                PwRotulo("Aguardando pagamento do cliente")
                Espaco(4)
                TextoDica("O pedido só vai para os motoboys depois de pago. Conferiu o PIX ou o cartão? Confirme aqui.")
                Espaco(8)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwBotaoOuro("PIX recebido", onClick = { acao { Gestao.confirmarPagamento(corrida.id, "pix") } }, icone = PwIcones.Check, habilitado = !ocupado, modifier = Modifier.weight(1f))
                    PwBotaoVidro("Cartão", onClick = { acao { Gestao.confirmarPagamento(corrida.id, "cartao") } }, icone = PwIcones.Wallet, modifier = Modifier.weight(1f))
                }
            }
        } else if (corrida.pagamentoStatus == "pago") {
            TextoDica("Pago pelo cliente" + (corrida.pagamentoMetodo?.let { " · ${if (it == "pix") "PIX" else "Cartão"}" } ?: ""))
        }
        if (corrida.automatica || corrida.pagCalculo != null || corrida.status == Status.MONTADA) BlocoRotaG(corrida, ocupado) { bloco -> acao(bloco) }
        if (corrida.planejada) BlocoPlanejadaG(corrida, ocupado) { bloco -> acao(bloco) }
        if (corrida.status == Status.SEM_MOTOBOY) {
            var moto by remember { mutableStateOf<String?>(null) }
            PwCartao(cor = c.vermelho.copy(alpha = 0.5f)) {
                PwRotulo("Despachar de novo")
                Espaco(8)
                PwSelecao("", listOf<Pair<String?, String>>(null to "Mais próximo da coleta") + dados.equipe.filter { it.motoboy && it.modoTeste == corrida.teste }.map { it.id to it.nome }, moto, { moto = it })
                Espaco(8)
                PwBotaoOuro(if (ocupado) "Despachando…" else "Despachar", onClick = { acao { Gestao.redespachar(corrida.id, moto) } }, icone = PwIcones.Send, habilitado = !ocupado)
            }
        }

        if (corrida.aguardandoConfirmacao) PwCartao(cor = c.ouro.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.CloudOff, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                PwRotulo("Aguardando confirmação", Modifier.weight(1f))
            }
            TextoDica("O motoboy ficou sem sinal e a rota fechou sozinha pela previsão. Quando o app dele voltar, as etapas chegam com a hora certa.")
        }
        var liberar by remember { mutableStateOf<com.pwexpress.feature.gestor.data.ParadaG?>(null) }
        val ativaAgora = corrida.status == Status.ACEITA || corrida.status == Status.EM_ANDAMENTO
        val podeLiberar = ativaAgora && corrida.exigirNoLocal && dados.cfgBool("saas_pular_parada", true) && dados.cfgBool("pular_parada_ligado", true)
        PwCartao {
            PwRotulo("Paradas · toque para ver no mapa")
            corrida.paradas.forEach { p ->
                Espaco(8)
                Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { focoC = com.pwexpress.core.mapa.FocoMapa(p.lat, p.lng) }, verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(99.dp)).then(Modifier), contentAlignment = Alignment.Center) {
                        Icon(if (p.coleta) PwIcones.ArrowUpFromLine else PwIcones.ArrowDownToLine, null, tint = if (p.coleta) c.ouro else c.texto, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${if (p.coleta) "Coleta" else "Entrega"} · ${rotuloParada(p.status)}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(p.endereco, color = c.texto2, fontSize = 13.5.sp)
                        listOfNotNull(p.contatoNome, p.contatoTelefone).joinToString(" ").takeIf { it.isNotBlank() }?.let { TextoDica(it) }
                        p.instrucoes?.let { TextoDica(it) }
                        if (p.referencias.isNotEmpty()) TextoDica("Referências: " + p.referencias.joinToString(" · ") { r -> r.relacao ?: (r.nome + (r.distanciaM?.let { " ($it m)" } ?: "")) })
                        p.concluidaEm?.let { TextoDica("Concluída às ${hora(it)}") }
                        p.distanciaChegadaM?.let { TextoDica("Marcado a ${km(it)} do endereço" + if (p.localIgnorado) " · localização ignorada (teste)" else "") }
                        run {
                            // o que vale: o que o cliente pediu no ponto ou, vazio, o padrão da rota (só nas entregas)
                            val ent = !p.coleta
                            SelosPontoG(p.exigeFoto ?: (ent && corrida.exigeFoto), p.exigeNome ?: (ent && corrida.exigeNome), p.exigeAssinatura ?: (ent && corrida.exigeAssinatura),
                                p.exigeCodigo ?: (ent && corrida.exigeCodigo), p.codigo ?: corrida.codigoEntrega.takeIf { ent }, p.janelaDe, p.janelaAte)
                        }
                        p.recebedorNome?.let { Text("Recebido por $it", color = c.texto, fontSize = 13.sp) }
                        if (p.liberadaLocal || p.feitaOffline || p.confirmacaoPendente) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (p.liberadaLocal) PwSelo("LIBERADA", c.verde, icone = PwIcones.LockOpen)
                            if (p.feitaOffline) PwSelo("SEM INTERNET", c.ouro, icone = PwIcones.CloudOff)
                            if (p.confirmacaoPendente) PwSelo("AGUARDANDO", c.ouro, icone = PwIcones.Clock)
                        }
                        if (podeLiberar && p.status != "concluida" && !p.liberadaLocal)
                            PwBotaoVidro("Liberar parada", onClick = { liberar = p }, icone = PwIcones.LockOpen, cor = c.texto2)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            p.fotoPath?.let { Comprovante("Foto", it) }
                            p.assinaturaPath?.let { Comprovante("Assinatura", it) }
                        }
                    }
                    p.contatoTelefone?.takeIf { it.isNotBlank() }?.let { tel ->
                        PwBotaoIcone(PwIcones.Phone, "Ligar", { runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$tel"))) } }, tamanho = 38.dp)
                    }
                }
            }
            corrida.observacoes?.let { Espaco(8); Text("Obs.: $it", color = c.ouro) }
        }

        liberar?.let { p ->
            var penalizar by remember(p.id) { mutableStateOf(false) }
            var motivo by remember(p.id) { mutableStateOf("") }
            PwJanela("Liberar a parada ${p.ordem + 1}?", onFechar = { liberar = null }) {
                Text("O motoboy confirma esta etapa de onde estiver, sem precisar estar no raio do endereço. Fica marcado na rota.", color = c.texto2)
                Text(p.endereco, color = c.texto, fontSize = 13.5.sp)
                PwCampo(motivo, { motivo = it }, "Motivo", dica = "Opcional: fica na linha do tempo")
                PwInterruptor("Penalizar", penalizar, { penalizar = it },
                    descricao = "Conta como pulo (${com.pwexpress.core.designsystem.numeroBr(maxOf(dados.cfgNum("pen_pts_pulo", 0.0), 1.0), 1)} pts). Sem marcar, não conta nada.", icone = PwIcones.Gavel, cor = c.vermelho)
                PwBotaoOuro("Liberar", onClick = {
                    liberar = null
                    acao { Gestao.rpcJson("liberar_parada", kotlinx.serialization.json.buildJsonObject {
                        put("p_parada", kotlinx.serialization.json.JsonPrimitive(p.id)); put("p_penalizar", kotlinx.serialization.json.JsonPrimitive(penalizar))
                        if (motivo.isNotBlank()) put("p_motivo", kotlinx.serialization.json.JsonPrimitive(motivo.trim()))
                    }).map { com.pwexpress.core.designsystem.PwToast.mostrar("Parada liberada: o motoboy já pode confirmar.") } }
                }, icone = PwIcones.LockOpen, modifier = Modifier.fillMaxWidth())
            }
        }

        if (eventos.isNotEmpty()) {
            PwCartao {
                PwRotulo("Linha do tempo")
                eventos.forEach { e ->
                    Espaco(6)
                    Row {
                        Text(hora(e.criadoEm), color = c.texto3, fontSize = 12.sp, modifier = Modifier.width(52.dp))
                        Text(ROTULO_EVENTO[e.tipo] ?: e.tipo.replace('_', ' '), color = c.texto, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        if (corrida.status == Status.CANCELADA) {
            PwBotaoVidro("Voltar para revisão", onClick = {
                acao { Gestao.rpcJson("reabrir_corrida", kotlinx.serialization.json.buildJsonObject { put("p_corrida", kotlinx.serialization.json.JsonPrimitive(corrida.id)) })
                    .map { j -> com.pwexpress.core.designsystem.PwToast.mostrar(if ((j as? kotlinx.serialization.json.JsonObject)?.get("rota")?.toString()?.contains("montada") == true)
                        "A rota voltou a \"montada\": solte de novo quando quiser." else "Os pedidos desta rota voltaram para a revisão.") } }
            }, icone = PwIcones.RotateCcw, habilitado = !ocupado, modifier = Modifier.fillMaxWidth())
        }
        if (corrida.status != Status.CONCLUIDA && corrida.status != Status.CANCELADA) {
            PwBotaoPerigo("Cancelar corrida", onClick = { cancelarPergunta = true }, icone = PwIcones.CircleX, habilitado = !ocupado, modifier = Modifier.fillMaxWidth())
        }
        val podeApagar = corrida.status !in setOf(Status.ACEITA, Status.EM_ANDAMENTO, Status.RESERVADA)
        if (!modulo("excluir_corridas")) Unit
        else if (podeApagar) PwBotaoVidro("Apagar corrida", onClick = { excluir = true }, icone = PwIcones.Trash2, modifier = Modifier.fillMaxWidth())
        else TextoDica("Para apagar esta corrida, cancele-a primeiro.")
    }

    if (cancelarPergunta && corrida != null) {
        PwJanela("Cancelar a corrida ${corrida.numeroExibicao}?", onFechar = { cancelarPergunta = false }) {
            Text("O motoboy (se houver) é avisado na hora.", color = c.texto2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Voltar", onClick = { cancelarPergunta = false }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Cancelar", onClick = { cancelarPergunta = false; acao { Gestao.cancelar(corrida.id) } }, modifier = Modifier.weight(1f), cheio = true, icone = PwIcones.CircleX)
            }
        }
    }
    if (excluir && corrida != null) DialogoExcluirCorridas(listOf(corrida), onFechar = { excluir = false }, onExcluidas = { excluir = false; onVoltar() })
}

private fun rotuloParada(s: String) = when (s) { "a_caminho" -> "A caminho"; "chegou" -> "No local"; "concluida" -> "Concluída"; else -> "Pendente" }

@Composable
private fun Comprovante(rotulo: String, caminho: String) {
    var url by remember(caminho) { mutableStateOf<String?>(null) }
    var aberto by remember { mutableStateOf(false) }
    LaunchedEffect(caminho) { url = Gestao.linkComprovante(caminho) }
    PwBotaoVidro(rotulo, onClick = { aberto = true }, icone = PwIcones.Image, habilitado = url != null)
    if (aberto && url != null) {
        PwJanela(rotulo, onFechar = { aberto = false }) {
            AsyncImage(model = url, contentDescription = rotulo, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)))
        }
    }
}

/** Planejada: quando, quem reservou, prazos e as ações do gestor. */
@Composable
private fun BlocoPlanejadaG(corrida: CorridaG, ocupado: Boolean, acao: (suspend () -> Result<Unit>) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var moto by remember { mutableStateOf<String?>(null) }
    var reagendar by remember { mutableStateOf(false) }
    val q = corrida.agendadaPara ?: return
    val agora = System.currentTimeMillis()
    val desistirAte = q - dados.cfgNum("agendada_cancelar_ate_min", 60.0).toLong() * 60_000
    val iniciarDesde = q - dados.cfgNum("agendada_iniciar_antes_min", 60.0).toLong() * 60_000
    PwCartao(cor = (if (corrida.atrasada) c.vermelho else c.azul).copy(alpha = 0.6f)) {
        PwRotulo("Corrida planejada" + if (corrida.serieId != null) " · faz parte de uma repetição" else "")
        Espaco(4)
        Text(quando(q).replaceFirstChar { it.uppercase() }, style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 21.sp), color = c.texto)
        Text(if (corrida.atrasada) "Atrasada: ainda não foi iniciada" else relativo(q), color = if (corrida.atrasada) c.vermelho else c.azul, fontWeight = FontWeight.SemiBold)
        Espaco(6)
        if (corrida.motoboyId != null) {
            TextoDica(
                "Reservada por ${dados.membro(corrida.motoboyId)?.nome ?: "—"}. " +
                    (if (agora < desistirAte) "Pode desistir pelo app até ${quando(desistirAte)}. " else "O prazo de desistência já passou. ") +
                    (if (agora < iniciarDesde) "Pode iniciar a partir de ${quando(iniciarDesde)}." else "Já pode iniciar pelo app."),
            )
        } else {
            TextoDica("Livre: aparece no app de todos os motoboys${if (corrida.teste) " em modo teste" else ""}. " +
                if (dados.cfgBool("agendada_despacho_auto", true)) "Se ninguém reservar até ${dados.cfgNum("agendada_despacho_auto_min", 30.0).toInt()} min antes, vai para o despacho normal." else "Se ninguém reservar, fica sem motoboy no horário.")
        }
        Espaco(10)
        PwSelecao("Reservar para", dados.equipe.filter { it.motoboy && it.modoTeste == corrida.teste && it.id != corrida.motoboyId }.map { it.id to it.nome }, moto, { moto = it },
            vazio = if (corrida.motoboyId != null) "Passar para outro motoboy…" else "Escolha um motoboy…")
        Espaco(8)
        PwBotaoVidro("Reservar", onClick = { moto?.let { m -> acao { Gestao.gerenciarPlanejada(corrida.id, "reservar", m) } } }, icone = PwIcones.CalendarCheck, habilitado = moto != null && !ocupado, modifier = Modifier.fillMaxWidth())
        Espaco(8)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwBotaoVidro("Mudar horário", onClick = { reagendar = true }, icone = PwIcones.Repeat, modifier = Modifier.weight(1f))
            if (corrida.motoboyId != null) PwBotaoVidro("Liberar", onClick = { acao { Gestao.gerenciarPlanejada(corrida.id, "liberar") } }, icone = PwIcones.CalendarX, habilitado = !ocupado, modifier = Modifier.weight(1f))
        }
        Espaco(8)
        PwBotaoVidro("Despachar agora", onClick = { acao { Gestao.gerenciarPlanejada(corrida.id, "despachar_agora") } }, icone = PwIcones.Send, habilitado = !ocupado, modifier = Modifier.fillMaxWidth())
    }
    if (reagendar) {
        SeletorDataHora("Novo horário", q, onFechar = { reagendar = false }, onEscolher = { novo -> reagendar = false; acao { Gestao.gerenciarPlanejada(corrida.id, "reagendar", quando = novo) } })
    }
}

/** Exclusão definitiva: confirma, pede a senha (conferida no banco) e um motivo. */
@Composable
fun DialogoExcluirCorridas(corridas: List<CorridaG>, onFechar: () -> Unit, onExcluidas: () -> Unit) {
    val c = Pw.cores
    var senha by remember { mutableStateOf("") }
    var motivo by remember { mutableStateOf("") }
    var entendi by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    val bloqueadas = corridas.filter { it.status in setOf(Status.ACEITA, Status.EM_ANDAMENTO, Status.RESERVADA) }
    PwJanela(if (corridas.size == 1) "Apagar a corrida ${corridas[0].numeroExibicao}?" else "Apagar ${corridas.size} corridas?", onFechar = onFechar, fecharFora = !enviando) {
        Text("Some de vez do histórico, junto com comprovantes e trajeto. Fica só um registro de quem apagou.", color = c.texto2)
        if (bloqueadas.isNotEmpty()) PwErro("${bloqueadas.joinToString { it.numeroExibicao }} está em andamento ou reservada. Cancele antes de apagar.")
        PwCampo(motivo, { motivo = it }, "Motivo (opcional)")
        PwInterruptor("Entendo que é definitivo", entendi, { entendi = it }, cor = c.vermelho)
        PwCampo(senha, { senha = it }, "Sua senha", senha = true, teclado = KeyboardType.Password)
        PwErro(erro)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwBotaoVidro("Voltar", onClick = onFechar, habilitado = !enviando, modifier = Modifier.weight(1f))
            PwBotaoPerigo(if (enviando) "Apagando…" else "Apagar", cheio = true, modifier = Modifier.weight(1f),
                habilitado = entendi && senha.isNotEmpty() && !enviando && bloqueadas.isEmpty(),
                onClick = {
                    enviando = true
                    escopo.launch {
                        Gestao.excluirCorridas(corridas.map { it.id }, senha, motivo.trim().ifEmpty { null })
                            .onSuccess { Gestao.atualizar(); onExcluidas() }
                            .onFailure { erro = it.message; senha = "" }
                        enviando = false
                    }
                })
        }
    }
}
