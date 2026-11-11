package com.pwexpress.feature.tracking.ui

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.togetherWith
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.alvoTour
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.feature.tracking.Intervalos
import com.pwexpress.feature.tracking.Modo
import com.pwexpress.feature.tracking.Tracking
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Início do MotoApp: saudação, o botão redondo de disponível/offline e os cartões que
 * vêm de outros módulos pelo :app (ganhos…). Detalhes técnicos ficam em Configurações.
 * [chips] mostra atalhos curtos (raio…); [onConfiguracoes] abre a tela de ajustes.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrackingScreen(
    modifier: Modifier = Modifier,
    saudacao: String = "",
    extras: @Composable () -> Unit = {},
    chips: @Composable () -> Unit = {},
    onConfiguracoes: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val info by Tracking.info.collectAsState()
    var perms by remember { mutableStateOf(Permissoes.ler(ctx)) }
    var aviso by remember { mutableStateOf<String?>(null) }
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { agora = System.currentTimeMillis(); delay(1_000) } }
    LifecycleResumeEffect(Unit) {
        perms = Permissoes.ler(ctx)
        onPauseOrDispose { }
    }
    val pedirBasicas = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms = Permissoes.ler(ctx) }

    fun escolher(m: Modo) {
        if (m != Modo.OFFLINE && !perms.basicasOk) {
            aviso = "Libere a localização precisa e as notificações para ficar disponível."
            pedirBasicas.launch(Permissoes.basicas())
            return
        }
        aviso = null
        val antes = Tracking.info.value.modo
        Tracking.definirModo(m)
        if (antes != m) {
            if (m == Modo.DISPONIVEL) com.pwexpress.core.designsystem.PwDestaque.mostrar("Você está online", "As corridas perto de você chegam aqui e no aviso de chamada.", com.pwexpress.core.designsystem.EstiloDestaque.ONLINE)
            else if (m == Modo.OFFLINE) com.pwexpress.core.designsystem.PwDestaque.mostrar("Você ficou offline", "Nenhuma corrida nova vai chegar até você voltar.", com.pwexpress.core.designsystem.EstiloDestaque.OFFLINE)
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (saudacao.isNotBlank()) {
            Text("Olá, $saudacao", style = MaterialTheme.typography.headlineMedium, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        // ---------------------------------------------------------- o botão
        var toquesSeguidos by remember { mutableStateOf(0) }
        var ultimoToque by remember { mutableStateOf(0L) }
        var perguntaJogo by remember { mutableStateOf(false) }
        var jogando by remember { mutableStateOf(false) }
        if (perguntaJogo) com.pwexpress.core.jogo.PerguntaJogo(onJogar = { perguntaJogo = false; jogando = true }, onFechar = { perguntaJogo = false })
        // em corrida (oferta aceita) o jogo some sozinho
        if (jogando && info.modo != Modo.EM_CORRIDA) com.pwexpress.core.jogo.JogoEntrega(onFechar = { jogando = false })
        Box(Modifier.fillMaxWidth().padding(vertical = 4.dp), contentAlignment = Alignment.Center) {
            BotaoStatus(
                info.modo,
                onAlternar = {
                    when (info.modo) {
                        Modo.OFFLINE -> escolher(Modo.DISPONIVEL)
                        Modo.DISPONIVEL -> escolher(Modo.OFFLINE)
                        Modo.EM_CORRIDA -> Unit
                    }
                },
                onToque = {
                    // vários toques seguidos no anel (sem segurar): convite para o joguinho enquanto espera
                    val agoraT = System.currentTimeMillis()
                    toquesSeguidos = if (agoraT - ultimoToque < 1_300) toquesSeguidos + 1 else 1
                    ultimoToque = agoraT
                    val perfilJ = com.pwexpress.core.perfil.Perfis.perfil.value
                    val precisa = (perfilJ?.numero("jogo_toques", 5.0) ?: 5.0).toInt().coerceIn(2, 30)
                    if (info.modo != Modo.EM_CORRIDA && perfilJ?.logico("jogo_ligado", true) != false && toquesSeguidos >= precisa) {
                        toquesSeguidos = 0; aviso = null; perguntaJogo = true
                        return@BotaoStatus
                    }
                    aviso = when (info.modo) {
                        Modo.EM_CORRIDA -> "Conclua a rota para mudar o status."
                        Modo.OFFLINE -> "Segure o botão até o anel encher para ficar online."
                        Modo.DISPONIVEL -> "Segure o botão até o anel encher para ficar offline."
                    }
                },
                modifier = Modifier.alvoTour("status"),
            )
        }

        // atalhos (GPS, raio, economia, aparelho)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (info.modo != Modo.OFFLINE) {
                val semLeituraMs = agora - (info.ultimaLeitura ?: agora)
                if (semLeituraMs > 5 * 60_000) Chip(PwIcones.Alerta, "Sem GPS há ${semLeituraMs / 60_000} min", c.vermelho)
                else Chip(PwIcones.Crosshair, info.ultimaPrecisao?.let { "GPS ± ${it.toInt()} m" } ?: "Buscando GPS…", if (info.ultimaPrecisao != null) c.verde else c.texto3)
            }
            if (info.economia) Chip(PwIcones.BatteryLow, "Economia", c.ouro)
            Box(Modifier.alvoTour("raio")) { chips() }
            if (!perms.tudoOk) Chip(PwIcones.Alerta, "Preparar o aparelho", c.vermelho, onConfiguracoes)
            if (info.naFila > 0) Chip(PwIcones.Clock, "${info.naFila} na fila", c.ouro, onConfiguracoes)
        }
        aviso?.let { Text(it, color = c.vermelho, fontSize = 13.5.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }

        Box(Modifier.alvoTour("dia")) { extras() }
    }

}

/**
 * O botão de status, refeito do zero: um orbe escuro de vidro com um anel de luz.
 * - Segure para trocar (o anel enche enquanto você segura e o celular vibra no fim): nada de toque sem querer.
 * - Offline: o anel dorme em tracinhos dourados e o orbe respira devagar.
 * - Online: o anel vira uma aurora verde girando, com um cometa na ponta, e dentro do orbe
 *   um radar varre o mapa procurando rotas (com "blips" que acendem e apagam).
 * - Em rota: anel dourado pulsando; trocar só depois de concluir.
 */
@Composable
private fun BotaoStatus(modo: Modo, onAlternar: () -> Unit, onToque: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val online = modo == Modo.DISPONIVEL
    val emRota = modo == Modo.EM_CORRIDA
    val verde = Color(0xFF34D399)
    // a orbe é sempre escura: se a cor do tema for escura (preto no branco), usa branco no lugar
    val claroNaOrbe = c.ouro.luminance() < 0.35f
    val base1 = if (claroNaOrbe) Color(0xFFF4F4F5) else c.ouro
    val base2 = if (claroNaOrbe) Color(0xFFA1A1AA) else c.ouroEscuro
    val base3 = if (claroNaOrbe) Color(0xFFFFFFFF) else c.ouroClaro
    val cor by animateColorAsState(if (online) verde else base1, tween(600), label = "cor")
    val cor2 by animateColorAsState(if (online) Color(0xFF10B981) else base2, tween(600), label = "cor2")
    val cor3 by animateColorAsState(if (online) Color(0xFFA7F3D0) else base3, tween(600), label = "cor3")
    val anim = rememberInfiniteTransition(label = "orbe")
    val giro by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(if (online) 3800 else 24000, easing = androidx.compose.animation.core.LinearEasing)), label = "giro")
    val radar by anim.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.LinearEasing)), label = "radar")
    val respiro by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(if (online) 1400 else 2600), RepeatMode.Reverse), label = "respiro")
    val ligar = remember { androidx.compose.animation.core.Animatable(if (online || emRota) 1f else 0f) }
    LaunchedEffect(online, emRota) { ligar.animateTo(if (online || emRota) 1f else 0f, tween(700)) }
    val segurar = remember { androidx.compose.animation.core.Animatable(0f) }
    // onda que sai do anel na hora da troca (0 → 1)
    val onda = remember { androidx.compose.animation.core.Animatable(1f) }
    val vibrar = androidx.compose.ui.platform.LocalHapticFeedback.current
    val escopo = androidx.compose.runtime.rememberCoroutineScope()
    var apertado by remember { mutableStateOf(false) }
    val escalaToque by androidx.compose.animation.core.animateFloatAsState(if (apertado) 0.94f else 1f, androidx.compose.animation.core.spring(0.5f, 380f), label = "escala")
    val escala = escalaToque * (1f + 0.07f * kotlin.math.sin(Math.PI * onda.value).toFloat())
    val tam = 236.dp
    // ícones desenhados no radar: a moto que gira por fora e os itens que os clientes pedem
    // o veículo cadastrado gira em volta do anel (padrão: moto)
    val veiculo by com.pwexpress.core.perfil.Perfis.veiculo.collectAsState()
    val pintorMoto = androidx.compose.ui.graphics.vector.rememberVectorPainter(when (veiculo) {
        "carro" -> PwIcones.Car; "bicicleta" -> PwIcones.Bicycle; "van" -> PwIcones.Truck; else -> PwIcones.Moto
    })
    val pintoresItens = listOf(PwIcones.FileText, PwIcones.Package, PwIcones.ShoppingBag, PwIcones.Pill, PwIcones.Key, PwIcones.Boxes)
        .map { androidx.compose.ui.graphics.vector.rememberVectorPainter(it) }
    // o orbe é sempre escuro: nos temas "preto no branco" o destaque preto some, então vira claro
    val ouroMarca = Pw.cores.ouro.let { if (it.luminance() < 0.3f) Color(0xFFF4F4F5) else it }
    val ouroFora = Pw.cores.ouro
    // o detector de toque não reinicia quando o modo muda (senão perde o "segurando")
    val emRotaAgora = androidx.compose.runtime.rememberUpdatedState(emRota)
    val onAlternarAgora = androidx.compose.runtime.rememberUpdatedState(onAlternar)
    val onToqueAgora = androidx.compose.runtime.rememberUpdatedState(onToque)

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(tam + 40.dp).scale(escala)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            apertado = true
                            if (emRotaAgora.value) { tryAwaitRelease(); apertado = false; onToqueAgora.value(); return@detectTapGestures }
                            vibrar.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            var trocou = false
                            // troca assim que o anel enche, sem precisar soltar
                            val enchendo = escopo.launch {
                                segurar.animateTo(1f, tween(750, easing = androidx.compose.animation.core.LinearEasing))
                                trocou = true
                                vibrar.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                onAlternarAgora.value()
                                // a cor nova aparece já: o anel de "segurar" some e a onda sai do botão
                                escopo.launch { segurar.animateTo(0f, tween(320)) }
                                escopo.launch { onda.snapTo(0f); onda.animateTo(1f, tween(950, easing = androidx.compose.animation.core.FastOutSlowInEasing)) }
                            }
                            val soltou = tryAwaitRelease()
                            apertado = false
                            if (!trocou) {
                                enchendo.cancel()
                                if (soltou && segurar.value < 0.2f) onToqueAgora.value()
                            }
                            if (!trocou) escopo.launch { segurar.animateTo(0f, tween(260)) }
                        },
                    )
                }
                .semantics { role = androidx.compose.ui.semantics.Role.Button; contentDescription = if (online) "Online. Segure para ficar offline" else if (emRota) "Em rota" else "Offline. Segure para ficar online" }
                .drawBehind {
                    val centro = center
                    val rAnel = size.minDimension / 2f - 22.dp.toPx()
                    val esp = 9.dp.toPx()
                    val l = ligar.value
                    // halo
                    drawCircle(Brush.radialGradient(listOf(cor.copy(alpha = 0.10f + 0.28f * l * (0.6f + 0.4f * respiro)), Color.Transparent), centro, size.minDimension / 2f), size.minDimension / 2f)
                    // trilho do anel
                    drawCircle(Color.White.copy(alpha = 0.06f), rAnel, style = Stroke(esp))
                    // anel: dormindo (tracinhos) → aurora girando
                    if (l < 1f) {
                        val n = 48
                        for (i in 0 until n) {
                            val ang = Math.toRadians((i * 360.0 / n) + giro)
                            val a = (0.18f + 0.22f * respiro) * (1f - l)
                            val p1 = Offset(centro.x + (rAnel - esp * 0.35f) * kotlin.math.cos(ang).toFloat(), centro.y + (rAnel - esp * 0.35f) * kotlin.math.sin(ang).toFloat())
                            val p2 = Offset(centro.x + (rAnel + esp * 0.35f) * kotlin.math.cos(ang).toFloat(), centro.y + (rAnel + esp * 0.35f) * kotlin.math.sin(ang).toFloat())
                            drawLine(cor.copy(alpha = a), p1, p2, strokeWidth = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        }
                    }
                    if (l > 0f) rotate(giro, centro) {
                        drawCircle(Brush.sweepGradient(listOf(cor2.copy(alpha = 0.15f * l), cor.copy(alpha = l), cor3.copy(alpha = l), cor2.copy(alpha = 0.15f * l)), centro), rAnel, style = Stroke(esp))
                    }
                    // a moto anda por FORA do anel, como numa pista, apontando para onde vai
                    if (l > 0f) {
                        val t = 26.dp.toPx()
                        val rMoto = rAnel + esp / 2f + 14.dp.toPx()
                        val ang = Math.toRadians(giro.toDouble())
                        val pc = Offset(centro.x + rMoto * kotlin.math.cos(ang).toFloat(), centro.y + rMoto * kotlin.math.sin(ang).toFloat())
                        drawCircle(Brush.radialGradient(listOf(ouroFora.copy(alpha = 0.22f * l), Color.Transparent), pc, t * 0.9f), t * 0.9f, pc)
                        rotate(giro + 90f, pc) {
                            translate(pc.x - t / 2f, pc.y - t / 2f) {
                                with(pintorMoto) { draw(androidx.compose.ui.geometry.Size(t, t), alpha = l, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(ouroFora)) }
                            }
                        }
                    }
                    // onda da troca: anel que se expande e some, na cor nova
                    val o = onda.value
                    if (o < 1f) {
                        val rMax = size.minDimension / 2f
                        val r = rAnel + (rMax - rAnel) * o
                        drawCircle(cor.copy(alpha = 0.85f * (1f - o)), r, centro, style = Stroke(esp * (1.6f - o)))
                        drawCircle(Brush.radialGradient(listOf(cor.copy(alpha = 0.45f * (1f - o)), Color.Transparent), centro, rMax), rMax, centro)
                    }
                    // enchendo enquanto segura
                    val s = segurar.value
                    if (s > 0f) {
                        val alvo = if (online) Color(0xFFF87171) else verde
                        drawArc(alvo, -90f, 360f * s, false, Offset(centro.x - rAnel, centro.y - rAnel), androidx.compose.ui.geometry.Size(rAnel * 2, rAnel * 2),
                            style = Stroke(esp + 3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            // orbe de vidro escuro
            val rOrbe = tam - 72.dp
            Box(
                Modifier.size(rOrbe).clip(CircleShape).drawBehind {
                    val r = size.minDimension / 2f
                    drawCircle(Brush.radialGradient(listOf(Color(0xFF1D1D21), Color(0xFF09090B)), Offset(center.x, center.y * 0.7f), r * 1.3f), r)
                    // luz que sobe de baixo, na cor do estado
                    drawCircle(Brush.radialGradient(listOf(cor.copy(alpha = 0.10f + 0.30f * ligar.value * (0.7f + 0.3f * respiro)), Color.Transparent), Offset(center.x, size.height * 1.05f), r * 1.25f), r)
                    // radar varrendo (online)
                    if (online) {
                        rotate(radar) {
                            drawCircle(Brush.sweepGradient(0f to Color.Transparent, 0.78f to Color.Transparent, 0.97f to cor.copy(alpha = 0.38f), 1f to cor.copy(alpha = 0.0f)), r)
                        }
                        // o que os clientes pedem (documento, pacote, sacola, remédio, chave, caixas) acende quando o radar passa
                        listOf(0.58f to 30f, 0.66f to 95f, 0.62f to 160f, 0.70f to 215f, 0.60f to 265f, 0.72f to 320f).forEachIndexed { i, (dist, ang) ->
                            val idade = (((radar - ang) % 360f) + 360f) % 360f / 360f // 0 logo depois do radar passar
                            // acende quando o radar passa e some rápido (bem antes de ele voltar)
                            val a = ((0.38f - idade) / 0.38f).coerceIn(0f, 1f).let { it * it } * 0.95f
                            val rad = Math.toRadians(ang.toDouble())
                            val p = Offset(center.x + r * dist * kotlin.math.cos(rad).toFloat(), center.y + r * dist * kotlin.math.sin(rad).toFloat())
                            val t = 15.dp.toPx() * (0.85f + 0.25f * a)
                            drawCircle(cor.copy(alpha = a * 0.28f), 11.dp.toPx(), p)
                            translate(p.x - t / 2f, p.y - t / 2f) {
                                with(pintoresItens[i % pintoresItens.size]) { draw(androidx.compose.ui.geometry.Size(t, t), alpha = a, colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(cor3)) }
                            }
                        }
                        drawCircle(cor.copy(alpha = 0.10f), r * 0.66f, style = Stroke(1.dp.toPx()))
                        drawCircle(cor.copy(alpha = 0.08f), r * 0.33f, style = Stroke(1.dp.toPx()))
                    }
                    // reflexo de vidro
                    drawOval(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent), size.height * 0.04f, size.height * 0.42f),
                        Offset(size.width * 0.17f, size.height * 0.05f), androidx.compose.ui.geometry.Size(size.width * 0.66f, size.height * 0.36f))
                    drawCircle(Color.White.copy(alpha = 0.10f), r - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
                },
                contentAlignment = Alignment.Center,
            ) {
                // ícone e texto trocam com um giro + zoom
                androidx.compose.animation.AnimatedContent(
                    modo,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(tween(380, 120)) + androidx.compose.animation.scaleIn(tween(480, 120), initialScale = 0.55f))
                            .togetherWith(androidx.compose.animation.fadeOut(tween(200)) + androidx.compose.animation.scaleOut(tween(260), targetScale = 1.35f))
                    },
                    label = "estado",
                ) { m ->
                    val giroIcone = remember { androidx.compose.animation.core.Animatable(-90f) }
                    LaunchedEffect(Unit) { giroIcone.animateTo(0f, androidx.compose.animation.core.spring(0.45f, 220f)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            when (m) { Modo.OFFLINE -> PwIcones.Power; Modo.DISPONIVEL -> PwIcones.Capacete; Modo.EM_CORRIDA -> PwIcones.Route },
                            null, tint = if (m == Modo.DISPONIVEL) ouroMarca else cor, modifier = Modifier.size(40.dp).graphicsLayer { rotationZ = giroIcone.value },
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when (m) { Modo.OFFLINE -> "OFFLINE"; Modo.DISPONIVEL -> "ONLINE"; Modo.EM_CORRIDA -> "EM ROTA" },
                            style = TextStyle(brush = Brush.linearGradient(listOf(cor3, cor, cor2)), fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 1.2.sp),
                            maxLines = 1, softWrap = false,
                        )
                    }
                }
            }
        }
        Text(
            when (modo) { Modo.OFFLINE -> "Segure para ficar online"; Modo.DISPONIVEL -> "Procurando rotas perto de você · segure para sair"; Modo.EM_CORRIDA -> "Conclua a rota para mudar o status" },
            color = c.texto3, fontSize = 12.5.sp, fontFamily = Archivo, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Chip(icone: ImageVector, texto: String, cor: Color, onClick: (() -> Unit)? = null) {
    val c = Pw.cores
    val forma = RoundedCornerShape(99.dp)
    Row(
        Modifier.clip(forma).background(cor.copy(alpha = 0.12f)).border(1.dp, cor.copy(alpha = 0.35f), forma)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1, softWrap = false)
    }
}

/** Atalho do Início em forma de ficha (o :app usa para o raio). */
@Composable
fun ChipInicio(icone: ImageVector, texto: String, onClick: (() -> Unit)? = null) = Chip(icone, texto, Pw.cores.ouro, onClick)

// =====================================================================
// Painéis da tela Configurações do MotoApp
// =====================================================================

/** Envio de localização: últimas leituras, fila e erro. */
@Composable
fun PainelEnvio() {
    val c = Pw.cores
    val info by Tracking.info.collectAsState()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { agora = System.currentTimeMillis(); delay(1_000) } }
    PwCartao {
        PwRotulo("Envio de localização")
        Spacer(Modifier.height(6.dp))
        PwLinhaInfo(PwIcones.Radio, "Status", info.modo.rotulo)
        PwLinhaInfo(PwIcones.Crosshair, "Última leitura GPS", info.ultimaLeitura?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
        PwLinhaInfo(PwIcones.LocateFixed, "Precisão", info.ultimaPrecisao?.let { "± ${it.toInt()} m" } ?: "—")
        PwLinhaInfo(PwIcones.Send, "Último envio", info.ultimoEnvio?.let { hora(it) + " · " + ha(agora, it) } ?: "—")
        PwLinhaInfo(PwIcones.Clock, "Na fila do aparelho", if (info.enviando) "${info.naFila} (enviando…)" else info.naFila.toString(), destaque = info.naFila > 0)
        PwLinhaInfo(PwIcones.Timer, "Intervalo em corrida", "${Intervalos.corridaMs(info.economia) / 1000} s")
        info.erro?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = c.vermelho, style = MaterialTheme.typography.bodyMedium)
        }
        if (info.naFila > 0) {
            Spacer(Modifier.height(8.dp))
            PwBotaoVidro("Enviar agora", onClick = { Tracking.sincronizarAgora() }, icone = PwIcones.RefreshCw)
        }
    }
}

/** Aparelho pronto: permissões que o GPS precisa para não ser pausado. */
@Composable
fun PainelAparelho() {
    val ctx = LocalContext.current
    val c = Pw.cores
    var perms by remember { mutableStateOf(Permissoes.ler(ctx)) }
    LifecycleResumeEffect(Unit) {
        perms = Permissoes.ler(ctx)
        onPauseOrDispose { }
    }
    val pedirBasicas = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms = Permissoes.ler(ctx) }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        perms = Permissoes.ler(ctx)
        if (!ok) Permissoes.abrirConfiguracoesDoApp(ctx)
    }
    PwCartao(destaque = !perms.tudoOk) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.ShieldCheck, null, tint = if (perms.tudoOk) c.verde else c.ouro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            PwRotulo(if (perms.tudoOk) "Aparelho pronto" else "Preparar o aparelho")
        }
        Spacer(Modifier.height(8.dp))
        ItemPermissao("Localização precisa", perms.localizacao) { pedirBasicas.launch(Permissoes.basicas()) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ItemPermissao("Notificações", perms.notificacoes) { pedirBasicas.launch(Permissoes.basicas()) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ItemPermissao("Localização \"o tempo todo\"", perms.segundoPlano, habilitado = perms.localizacao) {
                pedirSegundoPlano.launch(Permissoes.SEGUNDO_PLANO)
            }
        }
        ItemPermissao("Sem restrição de bateria", perms.semRestricaoBateria) { Permissoes.pedirSemRestricaoBateria(ctx) }
        if (!perms.tudoOk) {
            Spacer(Modifier.height(6.dp))
            Text("Sem esses ajustes o Android pode pausar o GPS com a tela apagada.", style = MaterialTheme.typography.bodyMedium, color = c.texto2)
        }
    }
}

@Composable
private fun ItemPermissao(nome: String, ok: Boolean, habilitado: Boolean = true, onLiberar: () -> Unit) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (ok) PwIcones.CircleCheck else PwIcones.Alerta, null, tint = if (ok) c.verde else c.vermelho, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(nome, Modifier.weight(1f), color = c.texto, maxLines = 2)
        if (!ok) {
            Text(
                "Liberar",
                color = if (habilitado) c.ouro else c.texto3,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = habilitado, onClick = onLiberar).padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    }
}

private val formatoHora = SimpleDateFormat("HH:mm:ss", Locale.forLanguageTag("pt-BR"))
private fun hora(t: Long): String = formatoHora.format(Date(t))

private fun ha(agora: Long, t: Long): String {
    val s = ((agora - t) / 1000).coerceAtLeast(0)
    return when {
        s < 60 -> "há ${s}s"
        s < 3600 -> "há ${s / 60} min"
        else -> "há ${s / 3600} h"
    }
}
