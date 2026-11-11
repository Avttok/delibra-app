package com.pwexpress.core.jogo

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwIcones
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Pergunta antes de abrir o jogo (o motoboy tocou várias vezes no anel). */
@Composable
fun PerguntaJogo(onJogar: () -> Unit, onFechar: () -> Unit) {
    val c = Pw.cores
    com.pwexpress.core.designsystem.PwJanela("Jogar enquanto espera?", onFechar) {
        Text("Desvie dos carros e buracos, pegue os pacotes. Se chegar uma corrida, o jogo fecha sozinho e a oferta aparece na hora.", color = c.texto2, fontSize = 14.sp)
        Text("Recorde: ${recorde(LocalContext.current)} pacotes", color = c.ouro, fontWeight = FontWeight.Bold, fontFamily = Archivo)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwBotaoVidro("Agora não", onClick = onFechar, modifier = Modifier.weight(1f))
            PwBotaoOuro("Jogar", onClick = onJogar, icone = PwIcones.Gamepad2, modifier = Modifier.weight(1f))
        }
    }
}

private fun recorde(ctx: Context) = ctx.getSharedPreferences("pwex_jogo", Context.MODE_PRIVATE).getInt("recorde", 0)
private fun salvarRecorde(ctx: Context, v: Int) = ctx.getSharedPreferences("pwex_jogo", Context.MODE_PRIVATE).edit().putInt("recorde", v).apply()

private enum class TipoCoisa { CARRO, BURACO, PACOTE, CONE }
private class Coisa(val faixa: Int, var y: Float, val tipo: TipoCoisa, val cor: Color, var pegou: Boolean = false)

/**
 * "Corre, motoboy!": 3 faixas, a moto embaixo. Toque à esquerda/direita (ou arraste) para trocar de faixa.
 * Pacote = ponto; carro, cone ou buraco = fim. A velocidade sobe aos poucos.
 */
@Composable
fun JogoEntrega(onFechar: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val vibrar = LocalHapticFeedback.current
    BackHandler { onFechar() }
    var faixa by remember { mutableIntStateOf(1) }
    var faixaVisual by remember { mutableFloatStateOf(1f) }
    val coisas = remember { mutableStateListOf<Coisa>() }
    var pontos by remember { mutableIntStateOf(0) }
    var melhor by remember { mutableIntStateOf(recorde(ctx)) }
    var vel by remember { mutableFloatStateOf(0.42f) }      // telas por segundo
    var rolagem by remember { mutableFloatStateOf(0f) }
    var estado by remember { mutableStateOf("pronto") }    // pronto · rodando · pausado · fim
    var tempo by remember { mutableFloatStateOf(0f) }
    var proxima by remember { mutableFloatStateOf(0.6f) }
    var recordeNovo by remember { mutableStateOf(false) }
    val coresCarro = remember { listOf(Color(0xFFEF4444), Color(0xFF38BDF8), Color(0xFFF5F5F5), Color(0xFFA78BFA), Color(0xFF22C55E)) }

    fun reiniciar() {
        coisas.clear(); pontos = 0; vel = 0.42f; tempo = 0f; proxima = 0.6f; faixa = 1; faixaVisual = 1f; recordeNovo = false; estado = "rodando"
    }
    fun mover(d: Int) { if (estado == "rodando") { faixa = (faixa + d).coerceIn(0, 2) } }

    LaunchedEffect(estado) {
        if (estado != "rodando") return@LaunchedEffect
        var ultimo = 0L
        while (estado == "rodando") {
            val t = withFrameNanos { it }
            if (ultimo == 0L) { ultimo = t; continue }
            val dt = min((t - ultimo) / 1e9f, 0.05f); ultimo = t
            tempo += dt
            vel = min(1.25f, 0.42f + tempo * 0.012f)
            rolagem = (rolagem + dt * vel) % 1f
            faixaVisual += (faixa - faixaVisual) * min(1f, dt * 14f)
            // anda tudo para baixo
            coisas.forEach { it.y += dt * vel }
            coisas.removeAll { it.y > 1.2f }
            // aparece coisa nova (mais perto uma da outra conforme acelera)
            proxima -= dt
            if (proxima <= 0f) {
                val livre = Random.nextInt(3)
                val obstaculos = if (tempo > 25 && Random.nextFloat() < 0.35f) 2 else 1
                val faixas = (0..2).filter { it != livre }.shuffled().take(obstaculos)
                faixas.forEach { f ->
                    val tipo = when (Random.nextInt(10)) { in 0..5 -> TipoCoisa.CARRO; in 6..7 -> TipoCoisa.CONE; else -> TipoCoisa.BURACO }
                    coisas.add(Coisa(f, -0.15f, tipo, coresCarro.random()))
                }
                if (Random.nextFloat() < 0.75f) coisas.add(Coisa(livre, -0.15f - Random.nextFloat() * 0.1f, TipoCoisa.PACOTE, Color.Unspecified))
                proxima = max(0.42f, 1.05f - tempo * 0.012f) / (vel / 0.42f) * 0.9f
            }
            // colisão perto da moto (y ~ 0.82)
            coisas.forEach { k ->
                if (k.pegou || k.faixa != faixa || k.y < 0.74f || k.y > 0.9f) return@forEach
                if (k.tipo == TipoCoisa.PACOTE) {
                    k.pegou = true; pontos++
                    vibrar.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } else if (kotlin.math.abs(faixaVisual - faixa) < 0.35f) {
                    vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (pontos > melhor) { melhor = pontos; salvarRecorde(ctx, pontos); recordeNovo = true }
                    estado = "fim"
                }
            }
            coisas.removeAll { it.pegou }
        }
    }

    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0C))) {
            Canvas(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { o -> if (o.x < size.width / 2f) mover(-1) else mover(1) } }
                    .pointerInput(Unit) {
                        var soma = 0f
                        detectHorizontalDragGestures(onDragStart = { soma = 0f }) { _, d ->
                            soma += d
                            if (soma > 60f) { mover(1); soma = 0f } else if (soma < -60f) { mover(-1); soma = 0f }
                        }
                    },
            ) {
                desenharPista(rolagem, c.ouro)
                val larg = size.width * 0.74f
                val x0 = (size.width - larg) / 2f
                val wf = larg / 3f
                fun cx(f: Float) = x0 + wf * (f + 0.5f)
                coisas.forEach { k -> desenharCoisa(k, cx(k.faixa.toFloat()), k.y * size.height, wf, c.ouro) }
                desenharMoto(cx(faixaVisual), size.height * 0.82f, wf, c.ouro, (faixa - faixaVisual) * 18f)
            }
            // placar
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("$pontos", color = Color.White, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 34.sp)
                    Text("pacotes · recorde $melhor", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                }
                if (estado == "rodando") PwBotaoIcone(PwIcones.Pause, "Pausar", { estado = "pausado" })
                Spacer(Modifier.width(8.dp))
                PwBotaoIcone(PwIcones.X, "Sair do jogo", onFechar)
            }
            if (estado != "rodando") {
                Column(
                    Modifier.align(Alignment.Center).padding(28.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xE6141416)).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(when (estado) { "fim" -> if (recordeNovo) "Recorde novo!" else "Bateu!"; "pausado" -> "Pausado"; else -> "Corre, motoboy!" },
                        color = c.ouro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 26.sp, textAlign = TextAlign.Center)
                    Text(when (estado) {
                        "fim" -> "$pontos pacotes entregues."
                        "pausado" -> "Toque em continuar quando quiser."
                        else -> "Toque à esquerda ou à direita (ou arraste) para trocar de faixa. Pegue os pacotes, fuja dos carros, cones e buracos."
                    }, color = Color(0xFFD4D4D8), fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(4.dp))
                    PwBotaoOuro(when (estado) { "pausado" -> "Continuar"; "fim" -> "Jogar de novo"; else -> "Começar" },
                        onClick = { if (estado == "pausado") estado = "rodando" else reiniciar() }, icone = PwIcones.Gamepad2, modifier = Modifier.width(220.dp))
                    PwBotaoVidro("Sair", onClick = onFechar, modifier = Modifier.width(220.dp))
                }
            }
            Text("Chegou corrida? O jogo fecha e a oferta aparece.", color = Color(0x99A1A1AA), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 10.dp))
        }
    }
}

private fun DrawScope.desenharPista(rolagem: Float, ouro: Color) {
    val larg = size.width * 0.74f
    val x0 = (size.width - larg) / 2f
    // acostamento e asfalto
    drawRect(Color(0xFF111114))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1C1C21), Color(0xFF16161A))), Offset(x0, 0f), Size(larg, size.height))
    drawRect(ouro.copy(alpha = 0.85f), Offset(x0 - 6f, 0f), Size(6f, size.height))
    drawRect(ouro.copy(alpha = 0.85f), Offset(x0 + larg, 0f), Size(6f, size.height))
    // faixas tracejadas que descem
    val passo = size.height / 7f
    val tr = passo * 0.5f
    for (i in 1..2) {
        val x = x0 + larg / 3f * i - 3f
        var y = -passo + rolagem * passo * 7f % passo
        while (y < size.height) { drawRoundRect(Color(0x66FFFFFF), Offset(x, y), Size(6f, tr), CornerRadius(3f)); y += passo }
    }
    // postes/pontos de luz nas laterais
    var y = -passo + rolagem * passo * 7f % passo
    while (y < size.height) {
        drawCircle(ouro.copy(alpha = 0.35f), 5f, Offset(x0 / 2f, y))
        drawCircle(ouro.copy(alpha = 0.35f), 5f, Offset(size.width - x0 / 2f, y))
        y += passo * 2
    }
}

private fun DrawScope.desenharCoisa(k: Coisa, cx: Float, cy: Float, wf: Float, ouro: Color) {
    when (k.tipo) {
        TipoCoisa.CARRO -> {
            val w = wf * 0.62f; val h = w * 1.75f
            drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(cx - w / 2 + 6, cy - h / 2 + 10), Size(w, h), CornerRadius(w * 0.28f))
            drawRoundRect(k.cor, Offset(cx - w / 2, cy - h / 2), Size(w, h), CornerRadius(w * 0.28f))
            drawRoundRect(Color(0xCC0B0B0D), Offset(cx - w * 0.36f, cy - h * 0.12f), Size(w * 0.72f, h * 0.22f), CornerRadius(8f))
            drawRoundRect(Color(0xCC0B0B0D), Offset(cx - w * 0.34f, cy + h * 0.22f), Size(w * 0.68f, h * 0.14f), CornerRadius(8f))
            drawCircle(Color(0xFFFDE68A), w * 0.07f, Offset(cx - w * 0.3f, cy + h / 2 - 6))
            drawCircle(Color(0xFFFDE68A), w * 0.07f, Offset(cx + w * 0.3f, cy + h / 2 - 6))
        }
        TipoCoisa.CONE -> {
            val w = wf * 0.32f
            val p = androidx.compose.ui.graphics.Path().apply { moveTo(cx, cy - w); lineTo(cx + w * 0.55f, cy + w * 0.6f); lineTo(cx - w * 0.55f, cy + w * 0.6f); close() }
            drawPath(p, Color(0xFFF97316))
            drawRect(Color.White, Offset(cx - w * 0.3f, cy - w * 0.05f), Size(w * 0.6f, w * 0.18f))
            drawRoundRect(Color(0xFFF97316), Offset(cx - w * 0.75f, cy + w * 0.55f), Size(w * 1.5f, w * 0.22f), CornerRadius(4f))
        }
        TipoCoisa.BURACO -> {
            val w = wf * 0.5f
            drawOval(Color(0xFF050506), Offset(cx - w / 2, cy - w * 0.28f), Size(w, w * 0.56f))
            drawOval(Color(0x553F3F46), Offset(cx - w / 2, cy - w * 0.28f), Size(w, w * 0.56f), style = Stroke(4f))
        }
        TipoCoisa.PACOTE -> {
            val w = wf * 0.34f
            drawCircle(ouro.copy(alpha = 0.18f), w * 0.95f, Offset(cx, cy))
            drawRoundRect(Brush.linearGradient(listOf(Color(0xFFF3D27A), ouro)), Offset(cx - w / 2, cy - w / 2), Size(w, w), CornerRadius(6f))
            drawRect(Color(0x99000000), Offset(cx - 2f, cy - w / 2), Size(4f, w))
            drawRect(Color(0x99000000), Offset(cx - w / 2, cy - 2f), Size(w, 4f))
        }
    }
}

private fun DrawScope.desenharMoto(cx: Float, cy: Float, wf: Float, ouro: Color, inclinar: Float) {
    val w = wf * 0.3f; val h = w * 2.6f
    // farol no chão
    drawOval(Brush.radialGradient(listOf(ouro.copy(alpha = 0.32f), Color.Transparent), Offset(cx, cy - h), h * 0.9f), Offset(cx - h * 0.7f, cy - h * 1.9f), Size(h * 1.4f, h * 1.4f))
    rotate(inclinar, Offset(cx, cy)) {
        drawRoundRect(Color.Black.copy(alpha = 0.4f), Offset(cx - w / 2 + 5, cy - h / 2 + 8), Size(w, h), CornerRadius(w / 2))
        // rodas
        drawRoundRect(Color(0xFF27272A), Offset(cx - w * 0.28f, cy - h / 2 - 4), Size(w * 0.56f, h * 0.26f), CornerRadius(w * 0.2f))
        drawRoundRect(Color(0xFF27272A), Offset(cx - w * 0.3f, cy + h * 0.26f), Size(w * 0.6f, h * 0.28f), CornerRadius(w * 0.2f))
        // corpo + baú
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFF3D27A), ouro)), Offset(cx - w / 2, cy - h * 0.3f), Size(w, h * 0.6f), CornerRadius(w * 0.35f))
        drawRoundRect(Color(0xFF0B0B0D), Offset(cx - w * 0.62f, cy + h * 0.08f), Size(w * 1.24f, h * 0.3f), CornerRadius(6f))
        drawRoundRect(ouro, Offset(cx - w * 0.62f, cy + h * 0.08f), Size(w * 1.24f, h * 0.3f), CornerRadius(6f), style = Stroke(3f))
        // capacete
        drawCircle(Color.White, w * 0.32f, Offset(cx, cy - h * 0.08f))
        drawCircle(Color(0xFF0B0B0D), w * 0.18f, Offset(cx, cy - h * 0.14f))
        // guidão
        drawRoundRect(Color(0xFFD4D4D8), Offset(cx - w * 0.75f, cy - h * 0.3f), Size(w * 1.5f, 5f), CornerRadius(3f))
    }
}
