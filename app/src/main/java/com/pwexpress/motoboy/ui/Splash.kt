package com.pwexpress.motoboy.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.clip
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
                // arco girando: o degradê gira junto (um degradê parado "quebrava" sempre no lado direito)
                androidx.compose.ui.graphics.drawscope.rotate(giro) {
                    drawArc(Brush.sweepGradient(0f to Color.Transparent, 0.06f to c.ouro.copy(alpha = 0.15f), 0.30f to c.ouroClaro, 0.31f to Color.Transparent, 1f to Color.Transparent),
                        0f, 110f, false,
                        topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.08f, size.height * 0.08f),
                        size = androidx.compose.ui.geometry.Size(size.width * 0.84f, size.height * 0.84f), style = Stroke(4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
            }, contentAlignment = Alignment.Center) {
                Box(Modifier.graphicsLayer { scaleX = pulso; scaleY = pulso }) { PwSeloLogo(116.dp) }
            }
            Spacer(Modifier.height(18.dp))
            Text(marca?.textoTopo ?: "Delibra", color = c.texto, fontFamily = marca?.fonte?.let { com.pwexpress.core.designsystem.FonteApp.de(it).familia } ?: Archivo,
                fontWeight = FontWeight.Black, fontSize = 26.sp)
            Spacer(Modifier.height(6.dp))
            Text(texto, color = c.texto3, fontSize = 13.sp)
        }
    }
}

/** Faixa fina sob o topo do painel do gestor para voltar à corrida (ou à oferta que chegou). */
@Composable
fun FaixaVoltarCorrida(texto: String, destaque: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    androidx.compose.foundation.layout.Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp).clip(forma)
            .background(if (destaque) c.ouro else c.vidroForte)
            .border(1.dp, if (destaque) c.ouro else c.ouro.copy(alpha = 0.5f), forma)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.Icon(com.pwexpress.core.designsystem.PwIcones.Bike, null, tint = if (destaque) c.sobreOuro else c.ouro, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(texto, color = if (destaque) c.sobreOuro else c.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f), maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Text("Voltar", color = if (destaque) c.sobreOuro else c.ouro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        androidx.compose.material3.Icon(com.pwexpress.core.designsystem.PwIcones.ChevronRight, null, tint = if (destaque) c.sobreOuro else c.ouro, modifier = Modifier.size(16.dp))
    }
}
