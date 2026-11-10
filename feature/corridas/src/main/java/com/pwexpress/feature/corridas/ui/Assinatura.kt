package com.pwexpress.feature.corridas.ui

import android.graphics.Bitmap
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.pwexpress.core.designsystem.PwCores
import java.io.ByteArrayOutputStream

/** Traços desenhados com o dedo. */
class EstadoAssinatura {
    val tracos: SnapshotStateList<SnapshotStateList<Offset>> = mutableStateListOf()
    internal var tamanho = IntSize.Zero
    val vazia: Boolean get() = tracos.none { it.size > 1 }
    fun limpar() = tracos.clear()

    /** PNG com fundo branco e traço preto. */
    fun exportarPng(): ByteArray {
        val w = tamanho.width.coerceAtLeast(1)
        val h = tamanho.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        canvas.drawColor(android.graphics.Color.WHITE)
        val tinta = Paint().apply {
            color = android.graphics.Color.BLACK
            strokeWidth = 6f
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        tracos.forEach { t ->
            if (t.size < 2) return@forEach
            val p = android.graphics.Path()
            p.moveTo(t[0].x, t[0].y)
            t.drop(1).forEach { p.lineTo(it.x, it.y) }
            canvas.drawPath(p, tinta)
        }
        return ByteArrayOutputStream().also { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
    }
}

@Composable
fun rememberEstadoAssinatura() = remember { EstadoAssinatura() }

@Composable
fun PainelAssinatura(estado: EstadoAssinatura, modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(12.dp)
    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(forma)
            .background(Color.White)
            .border(2.dp, PwCores.Ouro, forma)
            .onSizeChanged { estado.tamanho = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { estado.tracos.add(mutableStateListOf(it)) },
                    onDrag = { mudanca, _ -> estado.tracos.lastOrNull()?.add(mudanca.position) },
                )
            },
    ) {
        estado.tracos.forEach { t ->
            if (t.size < 2) return@forEach
            val path = Path().apply {
                moveTo(t[0].x, t[0].y)
                t.drop(1).forEach { lineTo(it.x, it.y) }
            }
            drawPath(path, Color.Black, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}
