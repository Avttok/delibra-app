package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.TipoParada
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")

/** "hoje às 14:00", "amanhã às 08:30", "sex, 07/11 às 14:00". */
fun quandoTexto(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val hoje = LocalDate.now()
    val dia = when (z.toLocalDate()) {
        hoje -> "hoje"
        hoje.plusDays(1) -> "amanhã"
        else -> z.format(DateTimeFormatter.ofPattern("EEE, dd/MM", br))
    }
    return "$dia às ${z.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

/** "em 2 h 10 min", "em 3 d", "há 5 min". */
fun relativoTexto(ms: Long, agora: Long = System.currentTimeMillis()): String {
    val min = ((ms - agora) / 60_000).toInt()
    val abs = kotlin.math.abs(min)
    val t = when {
        abs < 60 -> "$abs min"
        abs < 1440 -> "${abs / 60} h" + if (abs % 60 > 0) " ${abs % 60} min" else ""
        else -> "${abs / 1440} d" + if ((abs % 1440) / 60 > 0) " ${(abs % 1440) / 60} h" else ""
    }
    return if (min >= 0) "em $t" else "há $t"
}

/**
 * Rotas: o quadro com tudo o que dá para pegar agora (express e planejadas com o mesmo peso),
 * as minhas reservas e o histórico.
 */
@Composable
fun PlanejadasScreen(modifier: Modifier = Modifier, onIniciada: () -> Unit = {}, comPlanejadas: Boolean = true) {
    val c = Pw.cores
    val lista by Corridas.planejadas.collectAsState()
    val rotas by Corridas.rotas.collectAsState()
    val historico by Corridas.historico.collectAsState()
    var aba by rememberSaveable { mutableIntStateOf(0) }
    var carregando by remember { mutableStateOf(true) }
    var erro by remember { mutableStateOf<String?>(null) }
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var ocupado by remember { mutableStateOf<String?>(null) }
    var detalhe by remember { mutableStateOf<String?>(null) }
    var desistindo by remember { mutableStateOf<Corrida?>(null) }
    val escopo = rememberCoroutineScope()

    LaunchedEffect(aba) { if (aba == 2) Corridas.carregarHistorico().onFailure { erro = it.message } }
    LaunchedEffect(Unit) {
        while (true) {
            Corridas.carregarRotas().onFailure { erro = it.message }.onSuccess { erro = null }
            if (comPlanejadas) Corridas.carregarPlanejadas()
            carregando = false
            repeat(2) { delay(4_000); agora = System.currentTimeMillis() }
        }
    }

    val disponiveis = rotas.filter { comPlanejadas || !it.planejada }
    val minhas = lista.filter { it.minha }

    fun acao(id: String, bloco: suspend () -> Result<Unit>, aoTerminar: () -> Unit = {}) {
        ocupado = id
        escopo.launch {
            bloco().onFailure { erro = it.message }.onSuccess { erro = null; aoTerminar() }
            ocupado = null
        }
    }
    // já tenho rota e a nova fica longe: aviso, mas deixo pegar
    var longe by remember { mutableStateOf<Pair<Corrida, Double>?>(null) }
    fun pegarAgora(r: Corrida) {
        ocupado = r.id
        escopo.launch {
            Corridas.pegarRota(r.id)
                .onSuccess { ativa ->
                    erro = null
                    com.pwexpress.core.designsystem.PwToast.mostrar(if (ativa) "A rota é sua. Bora!" else "Reservada para você. Inicie perto do horário.", titulo = "Rota ${r.numeroExibicao}")
                    if (ativa) onIniciada() else aba = 1
                }
                .onFailure { erro = it.message; Corridas.carregarRotas() }
            ocupado = null
        }
    }

    fun pegar(r: Corrida) {
        if (Corridas.ativas.value.isEmpty() || r.planejada) { pegarAgora(r); return }
        ocupado = r.id
        escopo.launch {
            val d = Corridas.distanciaDasMinhas(r.id)
            ocupado = null
            val km = d?.second
            if (d != null && km != null && km > d.third) longe = r to km else pegarAgora(r)
        }
    }
    // já tenho rota e a nova fica longe: aviso, mas deixo pegar
    longe?.let { (r, km) ->
        com.pwexpress.core.designsystem.PwConfirmar("Essa rota fica longe",
            "A coleta dela está a ${String.format(java.util.Locale("pt", "BR"), "%.1f", km)} km das paradas das rotas que você já tem. Recomendamos não pegar, mas a escolha é sua.",
            "Pegar mesmo assim", onConfirmar = { longe = null; pegarAgora(r) }, onCancelar = { longe = null })
    }
    PwPagina(modifier) {
        PwCabeca(PwIcones.Route, "Rotas", when (aba) { 0 -> "Pegue uma rota: quem pegar primeiro leva"; 1 -> "As que você reservou"; else -> "As rotas que você já fez" })
        PwSegmentado(
            listOfNotNull("Disponíveis" to PwIcones.Megaphone, ("Minhas" to PwIcones.CalendarCheck).takeIf { comPlanejadas }, "Histórico" to PwIcones.History),
            if (!comPlanejadas && aba == 2) 1 else aba, { aba = if (!comPlanejadas && it == 1) 2 else it },
        )
        PwErro(erro)
        when (aba) {
            2 -> {
                if (com.pwexpress.core.perfil.Perfis.perfil.value?.modulo("trilhas_mapa") != false) MeusPercursos()
                val h = historico
                when {
                    h == null -> PwCarregando(texto = "Buscando suas rotas…")
                    h.isEmpty() -> PwVazio("Você ainda não fez nenhuma rota${if (com.pwexpress.core.designsystem.TemaApp.modoTeste.value) " de teste" else ""}.", icone = PwIcones.History)
                    else -> h.forEach { CartaoFeita(it) }
                }
            }
            1 -> when {
                minhas.isEmpty() -> PwVazio("Você não tem reservas. As planejadas aparecem em Disponíveis.", icone = PwIcones.CalendarClock)
                else -> minhas.forEach { corrida ->
                    CartaoPlanejada(
                        corrida, agora, ocupado == corrida.id,
                        onReservar = {},
                        onIniciar = { acao(corrida.id, { Corridas.iniciarPlanejada(corrida.id) }, onIniciada) },
                        onDesistir = { desistindo = corrida },
                    )
                }
            }
            else -> when {
                carregando && disponiveis.isEmpty() -> PwCarregando(texto = "Buscando rotas…")
                disponiveis.isEmpty() -> PwVazio("Nenhuma rota disponível agora. Fique online: quando surgir uma, você recebe um aviso.", icone = PwIcones.Megaphone)
                else -> disponiveis.forEach { r -> CartaoRota(r, agora, ocupado == r.id, onPegar = { pegar(r) }, onDetalhes = { detalhe = r.id }) }
            }
        }
    }

    detalhe?.let { id ->
        val r = disponiveis.firstOrNull { it.id == id }
        if (r == null) detalhe = null
        else DetalheRotaQuadro(r, agora, ocupado == r.id, onFechar = { detalhe = null }, onPegar = { detalhe = null; pegar(r) })
    }

    desistindo?.let { cor ->
        var motivo by remember(cor.id) { mutableStateOf("") }
        PwJanela("Desistir da rota ${cor.numeroExibicao}?", onFechar = { desistindo = null }) {
            Text("Ela volta para o quadro e os outros motoboys são avisados.", color = c.texto2)
            PwCampo(motivo, { motivo = it }, "Motivo (opcional)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Manter", onClick = { desistindo = null }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Desistir", onClick = {
                    desistindo = null
                    acao(cor.id, { Corridas.cancelarReserva(cor.id, motivo.trim().ifEmpty { null }) })
                }, modifier = Modifier.weight(1f), icone = PwIcones.CalendarX, cheio = true)
            }
        }
    }
}

/** Uma rota do quadro: express ("Agora") e planejada (com horário) no mesmo formato. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CartaoRota(r: Corrida, agora: Long, ocupado: Boolean, onPegar: () -> Unit, onDetalhes: () -> Unit) {
    val c = Pw.cores
    val coleta = r.paradas.firstOrNull { it.tipo == TipoParada.COLETA }
    val entregas = r.paradas.filter { it.tipo == TipoParada.ENTREGA }
    PwCartao(destaque = r.sugerida, cor = when { r.urgente -> c.vermelho.copy(alpha = 0.7f); r.sugerida -> c.ouro; else -> null }) {
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(r.numeroExibicao, color = c.texto2, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterVertically))
            if (r.sugerida) PwSelo("Melhor pra você", c.ouro, icone = PwIcones.Sparkles)
            if (r.planejada) PwSelo("Planejada", c.azul, icone = PwIcones.CalendarClock) else PwSelo("Agora", c.verde, icone = PwIcones.Zap)
            if (r.urgente) PwSelo("Urgente", c.vermelho, icone = PwIcones.Zap)
            if (r.perigoso) PwSelo("Perigoso", c.vermelho, icone = PwIcones.FlaskConical)
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val titulo = r.agendadaPara?.let { quandoTexto(it).replaceFirstChar { ch -> ch.uppercase() } } ?: "Para agora"
                com.pwexpress.core.designsystem.TextoAjustado(titulo, TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, color = c.texto), 20, minimo = 14)
                Text(
                    listOfNotNull(r.agendadaPara?.let { relativoTexto(it, agora) }, r.distanciaAteColetaM?.let { "coleta a ${kmCurto(it)} de você" }).joinToString(" · "),
                    color = if (r.planejada) c.azul else c.verde, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.widthIn(max = 130.dp), horizontalAlignment = Alignment.End) {
                if (r.valoresVisiveis && r.valorMotoboy != null) PwValor(moedaBr(r.valorMotoboy), tamanhoMax = 26)
                else Text("${r.entregas}", style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 28.sp), color = c.ouro)
                Text(if (r.valoresVisiveis && r.valorMotoboy != null) "${r.entregas} entrega${if (r.entregas == 1) "" else "s"}" else "entrega${if (r.entregas == 1) "" else "s"}", color = c.texto3, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        val forma = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            coleta?.let { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(PwIcones.ArrowUpFromLine, null, tint = c.ouro, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(p.endereco + if (r.coletas > 1) "  (+${r.coletas - 1} coleta${if (r.coletas > 2) "s" else ""})" else "", color = c.texto, fontSize = 14.sp, maxLines = 2)
                }
            }
            entregas.take(3).forEachIndexed { i, p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(PwIcones.ArrowDownToLine, null, tint = c.texto2, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(p.endereco, color = c.texto, fontSize = 13.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                if (i == 2 && entregas.size > 3) Text("+ ${entregas.size - 3} entrega${if (entregas.size > 4) "s" else ""}", color = c.texto3, fontSize = 12.5.sp)
            }
            Text(listOfNotNull(r.clientes.takeIf { it.isNotEmpty() }?.joinToString(", "), r.distanciaM?.let { "rota de ${kmCurto(it)}" }).joinToString(" · "),
                color = c.texto3, fontSize = 12.5.sp, maxLines = 2)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PwBotaoVidro("Detalhes", onClick = onDetalhes, icone = PwIcones.Mapa, modifier = Modifier.weight(1f).height(56.dp))
            PwBotaoOuro(
                if (ocupado) (if (r.planejada) "Reservando…" else "Pegando…") else if (r.planejada) "Reservar" else "Pegar rota",
                onClick = onPegar, icone = if (r.planejada) PwIcones.CalendarCheck else PwIcones.ChevronsRight, habilitado = !ocupado, altura = 56.dp,
                modifier = Modifier.weight(1.4f),
            )
        }
    }
}

/**
 * Tudo da rota antes de pegar ou reservar: mapa com o caminho, horário, distância, tempo,
 * cada parada (com cliente, referências e itens) e os cuidados. O motoboy se planeja antes.
 */
@Composable
internal fun DetalheRotaQuadro(r: Corrida, agora: Long, ocupado: Boolean, onFechar: () -> Unit, onPegar: () -> Unit,
                               /** Rodapé próprio (ex.: rota compartilhada no chat: responder se pode). */
                               rodapeProprio: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
                               aviso: String? = null) {
    val c = Pw.cores
    val perfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    val verEnderecos = perfil?.logico("quadro_mostra_enderecos", true) != false
    val verItens = perfil?.logico("quadro_mostra_itens", true) != false
    com.pwexpress.core.designsystem.PwTelaCheia(
        "Rota ${r.numeroExibicao}", onFechar,
        subtitulo = r.agendadaPara?.let { quandoTexto(it).replaceFirstChar { ch -> ch.uppercase() } + " · " + relativoTexto(it, agora) } ?: "Para agora",
        rodape = rodapeProprio ?: {
            PwBotaoOuro(
                if (ocupado) (if (r.planejada) "Reservando…" else "Pegando…") else if (r.planejada) "Reservar esta rota" else "Pegar esta rota",
                onClick = onPegar, icone = if (r.planejada) PwIcones.CalendarCheck else PwIcones.ChevronsRight, habilitado = !ocupado, altura = 58.dp,
            )
        },
    ) {
        aviso?.let { PwCartao(cor = c.azul.copy(alpha = 0.55f)) { Text(it, color = c.texto, fontSize = 14.sp) } }
        var focoR by remember(r.id) { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }
        val trazer = remember { androidx.compose.foundation.relocation.BringIntoViewRequester() }
        LaunchedEffect(focoR) { if (focoR != null) trazer.bringIntoView() }
        Box(Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp)).bringIntoViewRequester(trazer)) {
            com.pwexpress.core.mapa.PwMapa(
                paradas = r.paradas.map { com.pwexpress.core.mapa.PinoMapa(it.lat, it.lng, it.tipo == TipoParada.COLETA, false, false) },
                rota = r.rotaPlanejada, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(240.dp), enquadrar = r.id, focar = focoR,
            )
        }
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (r.planejada) PwSelo("Planejada", c.azul, icone = PwIcones.CalendarClock) else PwSelo("Agora", c.verde, icone = PwIcones.Zap)
            if (r.sugerida) PwSelo("Melhor pra você", c.ouro, icone = PwIcones.Sparkles)
            if (r.urgente) PwSelo("Urgente", c.vermelho, icone = PwIcones.Zap)
            if (r.perigoso) PwSelo("Carga perigosa", c.vermelho, icone = PwIcones.FlaskConical)
            if (r.exigeFoto) PwSelo("Foto", c.texto2, icone = PwIcones.Camera)
            if (r.exigeAssinatura) PwSelo("Assinatura", c.texto2, icone = PwIcones.Pencil)
            if (r.exigeCodigo) PwSelo("Código", c.texto2, icone = PwIcones.KeyRound)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.pwexpress.core.designsystem.PwNumero("Entregas", "${r.entregas}" + if (r.coletas > 1) " · ${r.coletas} col." else "", Modifier.weight(1f), icone = PwIcones.Package2)
            com.pwexpress.core.designsystem.PwNumero("Rota", r.distanciaM?.let { kmCurto(it) } ?: "—", Modifier.weight(1f), icone = PwIcones.Ruler)
            com.pwexpress.core.designsystem.PwNumero("Tempo", r.duracaoS?.let { if (it < 3600) "${it / 60} min" else "${it / 3600} h ${(it % 3600) / 60}" } ?: "—", Modifier.weight(1f), icone = PwIcones.Clock)
        }
        if (r.valoresVisiveis && r.valorMotoboy != null) {
            PwCartao(destaque = true) { PwRotulo("Você recebe"); PwValor(moedaBr(r.valorMotoboy), tamanhoMax = 36) }
        }
        r.distanciaAteColetaM?.let { Text("A primeira coleta fica a ${kmCurto(it)} de você.", color = c.texto2, fontSize = 13.5.sp) }
        PwRotulo("Paradas, na ordem")
        r.paradas.forEachIndexed { i, p ->
            val coleta = p.tipo == TipoParada.COLETA
            val forma = RoundedCornerShape(16.dp)
            Row(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma)
                .clickable { focoR = com.pwexpress.core.mapa.FocoMapa(p.lat, p.lng) }.padding(12.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(26.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (coleta) c.gradOuro else androidx.compose.ui.graphics.SolidColor(c.texto)), contentAlignment = Alignment.Center) {
                    Text("${i + 1}", color = if (coleta) c.sobreOuro else c.fundo, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (coleta) "COLETA" else "ENTREGA", color = if (coleta) c.ouro else c.texto2, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = Archivo)
                    Text(if (verEnderecos) p.endereco else p.endereco.split(",").drop(1).take(2).joinToString(",").trim().ifEmpty { "Endereço liberado ao pegar" },
                        color = c.texto, fontSize = 14.sp)
                    if (verEnderecos) ReferenciasParada(p.referencias)
                    if (verItens) ItensParada(p) else if (p.cliente != null) Text(p.cliente, color = c.texto2, fontSize = 12.5.sp)
                    p.instrucoes?.takeIf { verEnderecos }?.let { Text(it, color = c.ouro, fontSize = 12.5.sp) }
                }
            }
        }
        r.observacoes?.let { PwCartao { PwRotulo("Observações"); Text(it, color = c.texto) } }
    }
}

private fun kmCurto(m: Int) = if (m < 1000) "$m m" else "%.1f km".format(br, m / 1000.0)

@Composable
private fun CartaoPlanejada(
    corrida: Corrida, agora: Long, ocupado: Boolean,
    onReservar: () -> Unit, onIniciar: () -> Unit, onDesistir: () -> Unit,
) {
    val c = Pw.cores
    val quando = corrida.agendadaPara ?: 0L
    val podeIniciar = corrida.minha && (corrida.iniciarAPartirDe ?: quando) <= agora
    val podeDesistir = corrida.minha && (corrida.cancelarReservaAte ?: 0L) > agora
    val coleta = corrida.paradas.firstOrNull { it.tipo == TipoParada.COLETA }
    val entregas = corrida.paradas.filter { it.tipo == TipoParada.ENTREGA }

    PwCartao(destaque = corrida.minha, cor = if (corrida.minha) c.azul.copy(alpha = 0.7f) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(corrida.numeroExibicao, color = c.texto2, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 13.sp)
                    Spacer(Modifier.width(8.dp))
                    if (corrida.minha) PwSelo("Sua reserva", c.azul, icone = PwIcones.CalendarCheck)
                }
                Spacer(Modifier.height(4.dp))
                com.pwexpress.core.designsystem.TextoAjustado(quandoTexto(quando).replaceFirstChar { it.uppercase() }, TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, color = c.texto), 20, minimo = 14)
                Text(relativoTexto(quando, agora), color = c.azul, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.widthIn(max = 130.dp), horizontalAlignment = Alignment.End) {
                if (corrida.valoresVisiveis && corrida.valorMotoboy != null) PwValor(moedaBr(corrida.valorMotoboy), tamanhoMax = 26)
                else Text("${corrida.entregas} entrega${if (corrida.entregas == 1) "" else "s"}", color = c.ouro, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                corrida.distanciaM?.let { Text(if (it < 1000) "$it m" else "%.1f km".format(br, it / 1000.0), color = c.texto3, fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        val forma = RoundedCornerShape(14.dp)
        Column(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            coleta?.let { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(PwIcones.ArrowUpFromLine, null, tint = c.ouro, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Column { Text(p.endereco, color = c.texto, fontSize = 14.sp); ReferenciasParada(p.referencias) }
                }
            }
            entregas.lastOrNull()?.let { p ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(PwIcones.ArrowDownToLine, null, tint = c.texto, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(p.endereco + if (entregas.size > 1) "  (+${entregas.size - 1})" else "", color = c.texto, fontSize = 14.sp)
                        ReferenciasParada(p.referencias)
                    }
                }
            }
            corrida.clientes.takeIf { it.isNotEmpty() }?.let { Text("Cliente${if (it.size > 1) "s" else ""}: ${it.joinToString(", ")}", color = c.texto3, fontSize = 12.5.sp) }
        }
        Spacer(Modifier.height(12.dp))
        when {
            !corrida.minha -> PwBotaoOuro(if (ocupado) "Reservando…" else "Reservar", onClick = onReservar, icone = PwIcones.CalendarCheck, habilitado = !ocupado)
            podeIniciar -> PwBotaoOuro(if (ocupado) "Iniciando…" else "Iniciar rota", onClick = onIniciar, icone = PwIcones.ChevronsRight, habilitado = !ocupado, altura = 60.dp)
            else -> Text(
                "Você pode iniciar ${corrida.iniciarAPartirDe?.let { quandoTexto(it) } ?: "perto do horário"}.",
                color = c.texto2, fontSize = 13.sp,
            )
        }
        if (corrida.minha) {
            Spacer(Modifier.height(8.dp))
            if (podeDesistir) {
                PwBotaoVidro(
                    "Desistir até ${corrida.cancelarReservaAte?.let { quandoTexto(it).substringAfter(" às ") } ?: "—"}",
                    onClick = onDesistir, icone = PwIcones.CalendarX, habilitado = !ocupado, modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text("O prazo para desistir pelo app já passou. Se não puder ir, fale com o gestor.", color = c.texto3, fontSize = 12.5.sp)
            }
        }
    }
}
