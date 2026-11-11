package com.pwexpress.motoboy.ui

import androidx.compose.runtime.remember

import androidx.compose.ui.graphics.drawscope.rotate

import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.ui.draw.drawBehind

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwSeloLogo
import com.pwexpress.core.update.Atualizador
import com.pwexpress.core.update.EstadoAtualizacao

/**
 * Janela de atualização em tela cheia: logo no centro com um anel em degradê (que vira o progresso
 * do download), notas da versão e os botões. Quando o Dev escolheu a versão e esta não é ela,
 * a janela não fecha: o app só volta a funcionar na versão certa.
 */
@Composable
fun JanelaAtualizacao() {
    val aberta by Atualizador.janela.collectAsState()
    val bloqueio by Atualizador.bloqueio.collectAsState()
    val estado by Atualizador.estado.collectAsState()
    if (!aberta && bloqueio == null) return
    val c = Pw.cores
    val ctx = LocalContext.current
    val baixando = estado is EstadoAtualizacao.Baixando || estado is EstadoAtualizacao.Instalando
    val obrigatoria = bloqueio != null
    val podeFechar = !baixando && !obrigatoria
    Dialog(
        onDismissRequest = { if (podeFechar) Atualizador.fecharJanela() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false, dismissOnBackPress = podeFechar),
    ) {
        if (obrigatoria) BackHandler { /* não sai: versão obrigatória */ }
        val fracao = (estado as? EstadoAtualizacao.Baixando)?.fracao ?: if (estado is EstadoAtualizacao.Instalando) 1f else 0f
        val anim by animateFloatAsState(fracao, label = "progresso")
        val giro = rememberInfiniteTransition(label = "giro")
        val angulo by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "angulo")
        val pulso by giro.animateFloat(0.9f, 1.06f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulso")
        // busca: cometa rápido + ondas de radar saindo da logo
        val cometa by giro.animateFloat(0f, 360f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "cometa")
        val onda by giro.animateFloat(0f, 1f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "onda")
        // fundo: aurora que passeia e partículas subindo
        val tempo by giro.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "tempo")
        val flutua by giro.animateFloat(-6f, 6f, infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing), RepeatMode.Reverse), label = "flutua")
        val verificando = estado == EstadoAtualizacao.Verificando
        val emDia = estado == EstadoAtualizacao.EmDia && !obrigatoria
        // "em dia": o anel fica verde e solta uma onda uma vez
        val pronto = remember { androidx.compose.animation.core.Animatable(0f) }
        androidx.compose.runtime.LaunchedEffect(emDia) { if (emDia) { pronto.snapTo(0f); pronto.animateTo(1f, tween(1400, easing = androidx.compose.animation.core.FastOutSlowInEasing)) } else pronto.snapTo(0f) }
        val verde = Color(0xFF34D399)

        Box(
            Modifier.fillMaxSize().background(c.fundo)
                .drawBehind {
                    val w = size.width; val h = size.height
                    val a = tempo * 2 * Math.PI
                    // aurora: dois brilhos (dourado e um mais claro) passeando devagar
                    val p1 = Offset((w * (0.5f + 0.32f * kotlin.math.sin(a).toFloat())), h * (0.30f + 0.08f * kotlin.math.cos(a * 2).toFloat()))
                    val p2 = Offset((w * (0.5f + 0.30f * kotlin.math.cos(a + 1.3).toFloat())), h * (0.62f + 0.10f * kotlin.math.sin(a + 0.7).toFloat()))
                    drawCircle(Brush.radialGradient(listOf((if (emDia) verde else c.ouro).copy(alpha = 0.30f), Color.Transparent), p1, w * 0.85f), w * 0.85f, p1)
                    drawCircle(Brush.radialGradient(listOf(c.ouroClaro.copy(alpha = 0.14f), Color.Transparent), p2, w * 0.7f), w * 0.7f, p2)
                    // partículas douradas subindo
                    for (i in 0 until 26) {
                        val semente = (i * 7919) % 1000 / 1000f
                        val vel = 0.6f + ((i * 3571) % 100) / 100f
                        val y = h - ((tempo * vel * 3f + semente) % 1f) * h
                        val x = w * ((semente * 13.7f + kotlin.math.sin((tempo * 6.28f * vel + i).toDouble()).toFloat() * 0.03f) % 1f)
                        val raio = (1.2f + (i % 4)) * density
                        val alfa = (0.15f + 0.5f * kotlin.math.sin((y / h) * Math.PI).toFloat()).coerceIn(0f, 0.65f)
                        drawCircle(c.ouroClaro.copy(alpha = alfa), raio, Offset(x, y))
                    }
                },
        ) {
            Column(
                Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
            ) {
                Spacer(Modifier.height(8.dp))
                // logo no centro: anel fechado com brilho que gira devagar; no download, o anel vira o progresso
                Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
                    val grad = Brush.sweepGradient(listOf(c.ouroEscuro, c.ouroClaro, c.ouro, c.ouroEscuro))
                    // ondas de radar (buscando) ou a onda verde de "em dia"
                    Canvas(Modifier.size(270.dp)) {
                        val r0 = 105.dp.toPx(); val rMax = size.minDimension / 2
                        if (verificando) for (k in 0..1) {
                            val t = (onda + k * 0.5f) % 1f
                            drawCircle(c.ouro.copy(alpha = 0.5f * (1f - t)), r0 + (rMax - r0) * t, style = Stroke(2.dp.toPx() * (1.5f - t)))
                        }
                        if (pronto.value in 0.001f..0.999f) {
                            val t = pronto.value
                            drawCircle(verde.copy(alpha = 0.7f * (1f - t)), r0 + (rMax - r0) * t, style = Stroke(5.dp.toPx() * (1.2f - t)))
                        }
                    }
                    Canvas(Modifier.size(210.dp).rotate(if (baixando) -90f else angulo)) {
                        val esp = 7.dp.toPx()
                        when {
                            baixando -> {
                                drawCircle(c.borda, style = Stroke(esp))
                                drawArc(grad, 0f, 360f * anim, false, style = Stroke(esp, cap = StrokeCap.Round))
                            }
                            verificando -> {
                                drawCircle(c.borda, style = Stroke(esp))
                                rotate(cometa - angulo) { drawArc(Brush.sweepGradient(listOf(Color.Transparent, c.ouroClaro, c.ouro)), 0f, 110f, false, style = Stroke(esp, cap = StrokeCap.Round)) }
                            }
                            emDia -> drawCircle(Brush.sweepGradient(listOf(verde, c.ouroClaro, verde)), style = Stroke(esp))
                            else -> drawCircle(grad, style = Stroke(esp))
                        }
                    }
                    Box(Modifier.size(170.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(Brush.radialGradient(listOf((if (emDia) verde else c.ouro).copy(alpha = 0.22f * pulso), Color.Transparent))))
                    Box(Modifier.graphicsLayer { translationY = flutua * density }) { PwSeloLogo(132.dp) }
                    if (emDia) Box(Modifier.align(Alignment.BottomEnd).padding(end = 52.dp, bottom = 52.dp).size(44.dp).graphicsLayer { scaleX = pronto.value.coerceAtLeast(0.01f); scaleY = scaleX }
                        .clip(androidx.compose.foundation.shape.CircleShape).background(verde), contentAlignment = Alignment.Center) {
                        Icon(PwIcones.Check, "Em dia", tint = Color(0xFF052E1A), modifier = Modifier.size(24.dp))
                    }
                    if (baixando) Text("${(anim * 100).toInt()}%", color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp).clip(RoundedCornerShape(99.dp)).background(c.fundo2).padding(horizontal = 12.dp, vertical = 4.dp))
                }
                if (obrigatoria) PwSelo("ATUALIZAÇÃO OBRIGATÓRIA", c.ouro, icone = PwIcones.ShieldCheck)

                when (val e = estado) {
                    is EstadoAtualizacao.Baixando -> {
                        Titulo("Baixando…")
                        val mb = { b: Long -> "%.1f".format(b / 1_048_576.0).replace('.', ',') }
                        Text("${mb(e.baixado)} de ${mb(e.total)} MB · ${(e.fracao * 100).toInt()}%", color = c.texto2, fontFamily = Archivo, textAlign = TextAlign.Center)
                        Barra(anim)
                        Texto("Pode deixar o app aberto. O Android vai pedir para confirmar a instalação.")
                        PwBotaoVidro("Cancelar", onClick = { Atualizador.cancelarDownload() }, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth())
                    }
                    is EstadoAtualizacao.Instalando -> {
                        Titulo("Instalando…")
                        Texto("Confirme no aviso do Android. O app reabre sozinho já atualizado.")
                        Barra(1f)
                    }
                    is EstadoAtualizacao.Erro -> {
                        Titulo("Não deu certo")
                        PwErro(e.mensagem, Modifier.widthIn(max = 420.dp))
                        Instalar(ctx)
                        if (e.versao != null) PwBotaoOuro("Tentar de novo", onClick = { Atualizador.baixar() }, icone = PwIcones.RefreshCw, modifier = Modifier.widthIn(max = 420.dp))
                        if (podeFechar) PwBotaoOuro("Fechar", onClick = { Atualizador.fecharJanela() }, icone = PwIcones.Check, modifier = Modifier.widthIn(max = 420.dp))
                    }
                    EstadoAtualizacao.Verificando -> {
                        Titulo("Procurando atualização…"); Barra(null)
                        if (podeFechar) PwBotaoOuro("Fechar", onClick = { Atualizador.fecharJanela() }, icone = PwIcones.Check, modifier = Modifier.widthIn(max = 420.dp))
                    }
                    EstadoAtualizacao.EmDia -> if (!obrigatoria) {
                        Titulo("O app está em dia")
                        Texto("Você já tem tudo o que há de novo.")
                        PwBotaoOuro("Ótimo", onClick = { Atualizador.fecharJanela() }, icone = PwIcones.Check, modifier = Modifier.widthIn(max = 420.dp))
                    }
                    else -> {
                        val v = (e as? EstadoAtualizacao.Disponivel)?.versao ?: bloqueio?.versao
                        val b = bloqueio
                        when {
                            b != null && b.maisNova -> {
                                Titulo("Instale a versão liberada")
                                Texto("Este celular está com uma versão diferente da liberada. O Android só troca para ela reinstalando: baixe a versão certa, desinstale este app e instale a baixada.")
                                PwBotaoOuro("Baixar a versão liberada", onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(b.versao.urlApk)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                                    icone = PwIcones.Download, altura = 58.dp, modifier = Modifier.widthIn(max = 420.dp))
                                PwBotaoVidro("Desinstalar este app", onClick = { runCatching { ctx.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } },
                                    icone = PwIcones.Trash2, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth())
                            }
                            v != null -> {
                                Titulo(if (obrigatoria) "Atualize para continuar" else "Há uma nova versão")
                                Texto(if (obrigatoria) "Enquanto não atualizar, o app fica parado: nada de rotas nem localização."
                                      else "Leva só um minuto e o app continua de onde parou.")
                                Notas(v.notas)
                                Instalar(ctx)
                                PwBotaoOuro("Atualizar agora", onClick = { Atualizador.baixar() }, icone = PwIcones.Download, altura = 58.dp, modifier = Modifier.widthIn(max = 420.dp))
                                if (podeFechar) PwBotaoVidro("Depois", onClick = { Atualizador.fecharJanela() }, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth())
                            }
                            else -> if (podeFechar) PwBotaoOuro("Fechar", onClick = { Atualizador.fecharJanela() }, icone = PwIcones.Check, modifier = Modifier.widthIn(max = 420.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Titulo(t: String) {
    Text(t, style = TextStyle(brush = Pw.cores.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 32.sp), textAlign = TextAlign.Center)
}

@Composable
private fun Texto(t: String) {
    Text(t, color = Pw.cores.texto2, fontFamily = Archivo, fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 420.dp))
}

@Composable
private fun Notas(notas: String) {
    if (notas.isBlank()) return
    val c = Pw.cores
    val forma = RoundedCornerShape(20.dp)
    Column(
        Modifier.widthIn(max = 460.dp).fillMaxWidth().heightIn(max = 260.dp).clip(forma)
            .background(Brush.verticalGradient(listOf(c.ouro.copy(alpha = 0.12f), c.vidroSutil)))
            .border(1.dp, Brush.linearGradient(listOf(c.ouro.copy(alpha = 0.55f), c.borda)), forma)
            .verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Sparkles, null, tint = c.ouro, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("O QUE HÁ DE NOVO", color = c.ouro, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(notas, color = c.texto, fontSize = 14.sp, lineHeight = 21.sp)
    }
}

/**
 * Barra de progresso. Sem fração (buscando): um trecho dourado desliza de um lado ao outro sem parar.
 * Com fração: enche, com um brilho passando por cima para mostrar que está vivo.
 */
@Composable
private fun Barra(fracao: Float?) {
    val c = Pw.cores
    val t = rememberInfiniteTransition(label = "barra")
    val corre by t.animateFloat(-0.4f, 1.1f, infiniteRepeatable(tween(1300, easing = androidx.compose.animation.core.FastOutSlowInEasing)), label = "corre")
    val brilho by t.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "brilho")
    Box(Modifier.widthIn(max = 420.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(c.borda)
        .drawBehind {
            val w = size.width; val h = size.height
            if (fracao == null) {
                val larg = w * 0.32f
                drawRoundRect(Brush.horizontalGradient(listOf(Color.Transparent, c.ouro, c.ouroClaro, c.ouro, Color.Transparent), startX = corre * w, endX = corre * w + larg),
                    topLeft = Offset(corre * w, 0f), size = androidx.compose.ui.geometry.Size(larg, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
            } else {
                val cheio = w * fracao.coerceIn(0.02f, 1f)
                drawRoundRect(c.gradOuro, size = androidx.compose.ui.geometry.Size(cheio, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
                val x = brilho * cheio
                drawRoundRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.45f), Color.Transparent), startX = x - 40f, endX = x + 40f),
                    size = androidx.compose.ui.geometry.Size(cheio, h), cornerRadius = androidx.compose.ui.geometry.CornerRadius(h / 2))
            }
        })
}

/** Sem a permissão de instalar, mostra o atalho para liberar. */
@Composable
private fun Instalar(ctx: android.content.Context) {
    if (Atualizador.podeInstalar(ctx)) return
    val c = Pw.cores
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()) {
        Icon(PwIcones.ShieldCheck, null, tint = c.ouro, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Libere “Instalar apps desconhecidos” para este app.", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
    }
    PwBotaoVidro("Abrir permissão", onClick = { Atualizador.abrirPermissaoInstalar(ctx) }, icone = PwIcones.Settings2, modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth())
}
