package com.pwexpress.feature.corridas.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLogo
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.exigencias
import com.pwexpress.feature.corridas.data.Oferta
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.corridas.data.PosicaoAtual
import com.pwexpress.feature.corridas.push.Alarme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Oferta de corrida com contador. Usada dentro do app e na tela cheia. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfertaScreen(oferta: Oferta, onFim: (aceitou: Boolean) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val scope = rememberCoroutineScope()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val total = remember(oferta.id) { (oferta.prazoMillis - System.currentTimeMillis()).coerceAtLeast(1).toFloat() }
    val restante = (oferta.prazoMillis - agora).coerceAtLeast(0)

    LaunchedEffect(oferta.id) {
        while (System.currentTimeMillis() < oferta.prazoMillis) {
            agora = System.currentTimeMillis()
            delay(200)
        }
        agora = System.currentTimeMillis()
        Alarme.pararOferta(ctx)
        Corridas.descartarOferta(oferta.id)
        onFim(false)
    }

    fun responder(aceitar: Boolean) {
        if (enviando) return
        enviando = true
        erro = null
        Alarme.pararOferta(ctx)
        scope.launch {
            Corridas.responder(aceitar)
                .onSuccess { onFim(aceitar) }
                .onFailure { erro = it.message; enviando = false }
        }
    }

    val corrida = oferta.corrida
    var posicao by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    LaunchedEffect(oferta.id) { PosicaoAtual.obter(ctx, 6_000)?.let { posicao = it.lat to it.lng } }
    val pinos = remember(corrida.id) { corrida.paradas.map { PinoMapa(it.lat, it.lng, it.tipo == TipoParada.COLETA, false, false) } }
    val fracao = (restante / total).coerceIn(0f, 1f)
    // agendada (o gestor despachou antes da hora): aceitar = reservar
    val agendada = corrida.agendadaPara?.takeIf { it > System.currentTimeMillis() }
    val pouco = restante < 10_000
    var todasParadas by remember(oferta.id) { mutableStateOf(corrida.paradas.size <= 3) }
    var foco by remember(oferta.id) { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(c.fundo)
            // qualquer toque silencia o alarme (a oferta continua na tela)
            .pointerInput(Unit) { awaitEachGesture { awaitFirstDown(requireUnconsumed = false); Alarme.pararOferta(ctx) } },
    ) {
        val alturaPainel = maxHeight * 0.56f
        // ------------------------------------------------ mapa em tela cheia, atrás de tudo
        PwMapa(maximizavel = false, paradas = pinos, rota = corrida.rotaPlanejada, minhaPosicao = posicao, modifier = Modifier.fillMaxSize(),
            enquadrar = "${corrida.id}-${posicao != null}", margemBaixo = alturaPainel.value.toInt() + 10, focar = foco)
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.verticalGradient(listOf(c.fundo, c.fundo.copy(alpha = 0.7f), Color.Transparent))))

        // ------------------------------------------------ topo: pílula de vidro + orbe do tempo
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).clip(RoundedCornerShape(99.dp)).background(c.vidroForte).border(1.dp, c.bordaForte, RoundedCornerShape(99.dp))
                    .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PwLogo(tamanho = 38, soSelo = true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(if (oferta.extensao) "AUMENTAR SUA ROTA" else if (agendada != null) "CORRIDA AGENDADA" else if (corrida.entregas > 1) "NOVA ROTA" else "NOVA CORRIDA", maxLines = 1, softWrap = false,
                        style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp))
                    Text(corrida.numeroExibicao + (corrida.empresaNome?.let { " · $it" } ?: ""), color = c.texto2, fontFamily = Archivo, fontSize = 12.5.sp, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(10.dp))
            OrbeTempo((restante + 999) / 1000, fracao, pouco)
        }

        // ------------------------------------------------ painel de vidro embaixo
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(alturaPainel)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(Brush.verticalGradient(listOf(c.fundo2.copy(alpha = 0.97f), c.fundo)))
                .border(1.dp, Brush.verticalGradient(listOf(c.bordaForte, Color.Transparent)), RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .padding(horizontal = 18.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp, bottom = 12.dp).size(width = 42.dp, height = 4.dp).clip(RoundedCornerShape(99.dp)).background(c.bordaForte))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // extensão: as paradas vão para o fim da rota que ele já está fazendo
                if (oferta.extensao) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(c.ouro.copy(alpha = 0.22f), c.ouro.copy(alpha = 0.06f))))
                            .border(1.5.dp, c.ouro.copy(alpha = 0.7f), RoundedCornerShape(20.dp)).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(c.ouro.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                            Icon(PwIcones.Route, null, tint = c.ouro, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("EXTENSÃO DA SUA ROTA", color = c.ouro, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = Archivo)
                            Text("Estas paradas entram no fim da rota${oferta.rotaAtualNumero?.let { " #$it" } ?: ""}", color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 16.sp)
                            Text("Você termina o que já está fazendo${oferta.rotaAtualRestantes?.let { " ($it parada${if (it == 1) "" else "s"})" } ?: ""} e segue para estas. O que você já ia ganhar não diminui.", color = c.texto2, fontSize = 12.5.sp)
                        }
                    }
                }
                // agendada: deixa claro que não é para agora
                if (agendada != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.azul.copy(alpha = 0.14f)).border(1.5.dp, c.azul.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(c.azul.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
                            Icon(PwIcones.CalendarClock, null, tint = c.azul, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("AGENDADA · NÃO É AGORA", color = c.azul, fontWeight = FontWeight.Black, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = Archivo)
                            Text(quandoTexto(agendada).replaceFirstChar { it.uppercase() }, color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 19.sp)
                            Text("${relativoTexto(agendada)} · aceitar reserva para você", color = c.texto2, fontSize = 12.5.sp)
                        }
                    }
                }
                // destaque
                Column {
                    if (corrida.valoresVisiveis && corrida.valorMotoboy != null) {
                        PwRotulo("Você recebe")
                        PwValor(moeda(corrida.valorMotoboy), tamanhoMax = 44)
                        Text("${corrida.entregas} entrega${if (corrida.entregas == 1) "" else "s"}" + if (corrida.coletas > 1) " · ${corrida.coletas} coletas" else "",
                            color = c.texto2, fontFamily = Archivo, fontSize = 14.sp)
                    } else {
                        PwRotulo(if (corrida.planejada) "Rota planejada" else "Rota agora")
                        Text("${corrida.entregas} entrega${if (corrida.entregas == 1) "" else "s"}" + if (corrida.coletas > 1) " · ${corrida.coletas} coletas" else "",
                            style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 32.sp))
                    }
                    if (corrida.teste || corrida.urgente || corrida.perigoso) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (corrida.teste) PwSelo("TESTE", c.violeta, icone = PwIcones.FlaskConical)
                            if (corrida.urgente) PwSelo("URGENTE", c.vermelho, icone = PwIcones.Flame)
                            if (corrida.perigoso) PwSelo("CARGA PERIGOSA", c.vermelho, icone = PwIcones.Alerta)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.vidroSutil).border(1.dp, c.borda, RoundedCornerShape(20.dp))
                        .height(androidx.compose.foundation.layout.IntrinsicSize.Min).padding(vertical = 12.dp),
                ) {
                    Numero(PwIcones.Navigation, "Até coleta", km(oferta.distanciaM), Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(c.borda))
                    Numero(PwIcones.Route, "Percurso", km(corrida.distanciaM), Modifier.weight(1f))
                    Box(Modifier.width(1.dp).fillMaxHeight().background(c.borda))
                    Numero(PwIcones.Clock, "Tempo", minutos(corrida.duracaoS), Modifier.weight(1f))
                }
                // trajeto em linha do tempo
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(c.vidroSutil).border(1.dp, c.borda, RoundedCornerShape(22.dp)).padding(14.dp)) {
                    val visiveis = if (todasParadas) corrida.paradas else listOf(corrida.paradas.first(), corrida.paradas.last())
                    visiveis.forEachIndexed { i, p ->
                        val coleta = p.tipo == TipoParada.COLETA
                        val n = corrida.paradas.indexOf(p)
                        Row(Modifier.clip(RoundedCornerShape(12.dp)).clickable { foco = com.pwexpress.core.mapa.FocoMapa(p.lat, p.lng) }, verticalAlignment = Alignment.Top) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(28.dp).clip(CircleShape).background(if (coleta) c.gradOuro else Brush.linearGradient(listOf(c.vidroForte, c.vidroForte)))
                                    .border(if (coleta) 0.dp else 1.5.dp, c.bordaForte, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(if (coleta) PwIcones.ArrowUpFromLine else PwIcones.ArrowDownToLine, null, tint = if (coleta) c.sobreOuro else c.texto, modifier = Modifier.size(14.dp))
                                }
                                if (i < visiveis.lastIndex) Box(Modifier.width(2.dp).height(30.dp).background(Brush.verticalGradient(listOf(c.ouro.copy(alpha = 0.6f), c.bordaForte))))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f).padding(bottom = if (i < visiveis.lastIndex) 6.dp else 0.dp)) {
                                Text(if (coleta) "COLETA" else "ENTREGA ${corrida.paradas.take(n + 1).count { it.tipo != TipoParada.COLETA }}",
                                    color = if (coleta) c.ouro else c.texto2, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.sp, fontFamily = Archivo)
                                Text(p.endereco, style = MaterialTheme.typography.bodyMedium, color = c.texto)
                                ReferenciasParada(p.referencias)
                                ItensParada(p)
                            }
                        }
                        if (!todasParadas && i == 0 && corrida.paradas.size > 2) {
                            Text("+ ${corrida.paradas.size - 2} parada${if (corrida.paradas.size - 2 == 1) "" else "s"} no caminho · ver todas", color = c.ouro, fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 40.dp, bottom = 8.dp).clickable { todasParadas = true })
                        }
                    }
                }
                val exigencias = listOfNotNull(
                    "Nome".takeIf { corrida.exigeNome || corrida.paradas.any { corrida.exigencias(it).nome } }, "Foto".takeIf { corrida.exigeFoto || corrida.paradas.any { corrida.exigencias(it).foto } },
                    "Assinatura".takeIf { corrida.exigeAssinatura || corrida.paradas.any { corrida.exigencias(it).assinatura } }, "Código".takeIf { corrida.exigeCodigo || corrida.paradas.any { corrida.exigencias(it).codigo } },
                )
                if (exigencias.isNotEmpty() || corrida.exigirNoLocal) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (exigencias.isNotEmpty()) Pilula(PwIcones.PackageCheck, "Comprovante: ${exigencias.joinToString(", ")}")
                        if (corrida.exigirNoLocal) Pilula(PwIcones.MapPinned, "Etapas no local · ${corrida.raioChegadaM} m")
                    }
                }
                corrida.observacoes?.let { obs ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.vidroSutil).border(1.dp, c.borda, RoundedCornerShape(18.dp)).padding(14.dp),
                        verticalAlignment = Alignment.Top) {
                        Icon(PwIcones.Pencil, null, tint = c.ouro, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Column { PwRotulo("Observação"); Text(obs, color = c.texto, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
            erro?.let { Text(it, color = c.vermelho, modifier = Modifier.padding(vertical = 8.dp)) }
            Spacer(Modifier.height(10.dp))
            // ações: recusar (vidro, redondo) e aceitar (o dourado "esvazia" com o tempo)
            Row(Modifier.padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                PwBotaoVidro("Recusar", onClick = { responder(false) }, habilitado = !enviando, icone = PwIcones.X, modifier = Modifier.weight(1f).height(64.dp))
                BotaoAceitar(if (enviando) "Enviando…" else if (agendada != null) "RESERVAR" else "ACEITAR", fracao, pouco, !enviando, Modifier.weight(1.8f)) { responder(true) }
            }
        }
    }
}

/** Orbe de vidro com o anel do tempo que vai acabando. */
@Composable
private fun OrbeTempo(segundos: Long, fracao: Float, pouco: Boolean) {
    val c = Pw.cores
    val cor = if (pouco) c.vermelho else c.ouro
    val anim = rememberInfiniteTransition(label = "tempo")
    val pulso by anim.animateFloat(0.85f, 1f, infiniteRepeatable(tween(if (pouco) 450 else 1200), RepeatMode.Reverse), label = "pulso")
    val fundo = c.vidroForte
    val trilho = c.bordaForte
    Box(Modifier.size(66.dp).drawBehind {
        val esp = 5.dp.toPx()
        drawCircle(Brush.radialGradient(listOf(cor.copy(alpha = 0.30f * pulso), Color.Transparent)), size.minDimension / 2f)
        drawCircle(fundo, size.minDimension / 2f - esp)
        drawCircle(trilho, size.minDimension / 2f - esp / 2, style = Stroke(esp))
        // o degradê começa no topo junto com o arco (sem "quebra" no lado direito)
        androidx.compose.ui.graphics.drawscope.rotate(-90f) {
            drawArc(Brush.sweepGradient(0f to cor.copy(alpha = 0.4f), (fracao.coerceIn(0.05f, 1f)) to cor, 1f to cor), 0f, 360f * fracao, false,
                topLeft = Offset(esp / 2, esp / 2), size = Size(size.width - esp, size.height - esp), style = Stroke(esp, cap = StrokeCap.Round))
        }
    }, contentAlignment = Alignment.Center) {
        Text("$segundos", color = if (pouco) c.vermelho else c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 22.sp)
    }
}

/** Aceitar: dourado sólido (texto sempre legível) com a barra do tempo correndo embaixo. */
@Composable
private fun BotaoAceitar(texto: String, fracao: Float, pouco: Boolean, habilitado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(20.dp)
    Box(
        modifier.height(64.dp).clip(forma).background(c.gradOuro)
            .clickable(enabled = habilitado, onClick = onClick).semantics { contentDescription = "Aceitar" },
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Check, null, tint = c.sobreOuro, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(10.dp))
            Text(texto, color = c.sobreOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 1.sp, maxLines = 1, softWrap = false)
        }
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp).background(c.sobreOuro.copy(alpha = 0.15f))) {
            Box(Modifier.fillMaxWidth(fracao).height(5.dp).background(if (pouco) c.vermelho else c.sobreOuro.copy(alpha = 0.55f)))
        }
    }
}

@Composable
private fun Pilula(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    val c = Pw.cores
    Row(Modifier.clip(RoundedCornerShape(99.dp)).background(c.vidroSutil).border(1.dp, c.borda, RoundedCornerShape(99.dp)).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = c.ouro, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = c.texto2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Numero(icone: androidx.compose.ui.graphics.vector.ImageVector, rotulo: String, valor: String, modifier: Modifier) {
    val c = Pw.cores
    Column(modifier.padding(horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icone, null, tint = c.ouro, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(rotulo, color = c.texto2, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
        }
        Spacer(Modifier.height(4.dp))
        com.pwexpress.core.designsystem.TextoAjustado(valor, TextStyle(fontWeight = FontWeight.Bold, fontFamily = ArchivoLargo, color = c.texto), maximo = 19, minimo = 12)
    }
}

@Composable
private fun Info(rotulo: String, valor: String) {
    Column {
        PwRotulo(rotulo)
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 17.sp, fontFamily = ArchivoLargo, color = Pw.cores.texto)
    }
}

@Composable
private fun Linha(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = Pw.cores.ouro, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, color = Pw.cores.texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun CarregandoOferta() {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        PwLogo(tamanho = 120, soSelo = true)
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(color = Pw.cores.ouro)
        Spacer(Modifier.height(16.dp))
        Text("Carregando a oferta…", color = Pw.cores.texto2)
    }
}
