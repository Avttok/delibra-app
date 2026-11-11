package com.pwexpress.motoboy.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwSeloLogo

/** Tela de abertura: a logo (da empresa, se houver) respirando com o brilho, enquanto os dados chegam. */
@Composable
fun SplashMarca(texto: String) {
    val c = Pw.cores
    val marca by com.pwexpress.core.marca.MarcaEmpresa.marca.collectAsState()
    val t = rememberInfiniteTransition(label = "splash")
    val pulso by t.animateFloat(0.92f, 1.06f, infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulso")
    val giro by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "giro")
    Box(Modifier.fillMaxSize().background(c.fundo), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(200.dp).drawBehind {
                drawCircle(Brush.radialGradient(listOf(c.ouro.copy(alpha = 0.28f * pulso), Color.Transparent), center, size.minDimension / 2f))
                // arco girando em volta enquanto carrega
                drawArc(Brush.sweepGradient(listOf(Color.Transparent, c.ouroClaro, c.ouro)), giro, 110f, false,
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.08f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.84f), style = Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }, contentAlignment = Alignment.Center) {
                Box(Modifier.graphicsLayer { scaleX = pulso; scaleY = pulso }) { PwSeloLogo(116.dp) }
            }
            Spacer(Modifier.height(18.dp))
            Text(marca?.nomeExibido ?: "Delibra", color = c.texto, fontFamily = marca?.fonte?.let { com.pwexpress.core.designsystem.FonteApp.de(it).familia } ?: Archivo,
                fontWeight = FontWeight.Black, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(texto, color = c.texto3, fontSize = 13.sp)
        }
    }
}
