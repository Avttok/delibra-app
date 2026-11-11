package com.pwexpress.motoboy.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/** Ajustes do ícone (igual ao editor do site). */
private data class Ajuste(val zoom: Float = 1f, val dx: Float = 0f, val dy: Float = 0f, val margem: Float = 0.08f, val tirarFundo: Boolean = false, val tolerancia: Float = 40f)

private fun temTransparencia(b: Bitmap): Boolean {
    if (!b.hasAlpha()) return false
    val p = Bitmap.createScaledBitmap(b, 48, 48, false)
    for (x in 0 until 48) for (y in 0 until 48) if ((p.getPixel(x, y) ushr 24) < 250) return true
    return false
}

/** A cor das bordas vira transparente (preenche a partir das bordas), com borda suave. */
private fun tirarFundo(b: Bitmap, tolerancia: Float): Bitmap {
    val w = b.width; val h = b.height
    val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h)
    val cantos = intArrayOf(px[0], px[w - 1], px[(h - 1) * w], px[w * h - 1])
    val r0 = cantos.sumOf { (it shr 16) and 0xFF } / 4f; val g0 = cantos.sumOf { (it shr 8) and 0xFF } / 4f; val b0 = cantos.sumOf { it and 0xFF } / 4f
    fun dist(c: Int): Float { val dr = ((c shr 16) and 0xFF) - r0; val dg = ((c shr 8) and 0xFF) - g0; val db = (c and 0xFF) - b0; return sqrt(dr * dr + dg * dg + db * db) }
    val suave = tolerancia * 1.6f
    val visto = BooleanArray(w * h)
    val fila = ArrayDeque<Int>()
    for (x in 0 until w) { fila.add(x); fila.add((h - 1) * w + x) }
    for (y in 0 until h) { fila.add(y * w); fila.add(y * w + w - 1) }
    while (fila.isNotEmpty()) {
        val p = fila.removeLast()
        if (visto[p]) continue
        visto[p] = true
        val c = px[p]; val a = c ushr 24
        if (a != 0) {
            val e = dist(c)
            if (e > suave) continue
            val novo = if (e <= tolerancia) 0 else (a * ((e - tolerancia) / (suave - tolerancia))).toInt()
            px[p] = (novo shl 24) or (c and 0x00FFFFFF)
            if (e > tolerancia) continue
        }
        val x = p % w; val y = p / w
        if (x > 0) fila.add(p - 1); if (x < w - 1) fila.add(p + 1)
        if (y > 0) fila.add(p - w); if (y < h - 1) fila.add(p + w)
    }
    return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
}

private fun desenhar(fonte: Bitmap, a: Ajuste, lado: Int = 512): Bitmap {
    val base0 = if (a.tirarFundo) tirarFundo(fonte, a.tolerancia) else fonte
    val saida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    val area = lado * (1 - a.margem * 2)
    val esc = minOf(area / base0.width, area / base0.height) * a.zoom
    val lw = base0.width * esc; val lh = base0.height * esc
    val x = (lado - lw) / 2 + a.dx * lado; val y = (lado - lh) / 2 + a.dy * lado
    Canvas(saida).drawBitmap(base0, null, RectF(x, y, x + lw, y + lh), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    return saida
}

/**
 * Editor do ícone da empresa: zoom, posição, margem e "tirar o fundo" (o branco/cor das bordas
 * vira transparente). A prévia mostra no escuro com o brilho da cor da empresa.
 */
@Composable
fun EditorIconeApp(original: Bitmap, cor: String?, onCancelar: () -> Unit, onPronto: (ByteArray) -> Unit) {
    val c = Pw.cores
    // trabalha numa cópia de até 1024 px (rápido e com qualidade de sobra para 512)
    val fonte = remember(original) {
        val m = maxOf(original.width, original.height)
        if (m > 1024) Bitmap.createScaledBitmap(original, original.width * 1024 / m, original.height * 1024 / m, true) else original
    }
    var a by remember { mutableStateOf(Ajuste(tirarFundo = !temTransparencia(fonte))) }
    var previa by remember { mutableStateOf<Bitmap?>(null) }
    var dxS by remember { mutableFloatStateOf(0f) }; var dyS by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(a) {
        delay(80)
        previa = withContext(Dispatchers.Default) { desenhar(fonte, a, 384) }
    }
    val brilho = cor?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: c.ouro
    val cores = SliderDefaults.colors(thumbColor = c.ouro, activeTrackColor = c.ouro, inactiveTrackColor = c.bordaForte)

    PwJanela("Ajustar o ícone", onCancelar) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1.5f).aspectRatio(1f).clip(RoundedCornerShape(26.dp)).background(Color(0xFF0B0B0D)).border(1.dp, c.borda, RoundedCornerShape(26.dp))
                .drawBehind { drawCircle(Brush.radialGradient(listOf(brilho.copy(alpha = 0.5f), brilho.copy(alpha = 0.15f), Color.Transparent), center, size.minDimension * 0.55f), size.minDimension * 0.55f, center) }) {
                previa?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
            }
            Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(20.dp)).background(Color(0xFFF2F2F2))) {
                previa?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize()) }
            }
        }
        Text("Zoom", color = c.texto2, fontSize = 13.sp)
        Slider(a.zoom, { a = a.copy(zoom = it) }, valueRange = 0.4f..2.5f, colors = cores)
        Text("Para os lados", color = c.texto2, fontSize = 13.sp)
        Slider(a.dx, { dxS = it; a = a.copy(dx = it) }, valueRange = -0.4f..0.4f, colors = cores)
        Text("Para cima / baixo", color = c.texto2, fontSize = 13.sp)
        Slider(a.dy, { dyS = it; a = a.copy(dy = it) }, valueRange = -0.4f..0.4f, colors = cores)
        Text("Margem", color = c.texto2, fontSize = 13.sp)
        Slider(a.margem, { a = a.copy(margem = it) }, valueRange = 0f..0.25f, colors = cores)
        PwInterruptor("Tirar o fundo", a.tirarFundo, { a = a.copy(tirarFundo = it) }, descricao = "A cor das bordas (branco, por exemplo) vira transparente.", icone = PwIcones.Image)
        if (a.tirarFundo) {
            Text("Quanto tirar", color = c.texto2, fontSize = 13.sp)
            Slider(a.tolerancia, { a = a.copy(tolerancia = it) }, valueRange = 8f..140f, colors = cores)
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwBotaoVidro("Voltar", onClick = { a = Ajuste(tirarFundo = !temTransparencia(fonte)) }, icone = PwIcones.RotateCcw, modifier = Modifier.weight(1f).height(52.dp))
            PwBotaoOuro("Salvar", onClick = {
                val png = ByteArrayOutputStream().also { desenhar(fonte, a, 512).compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
                onPronto(png)
            }, icone = PwIcones.Check, altura = 52.dp, modifier = Modifier.weight(1f))
        }
    }
}
