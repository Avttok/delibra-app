package com.pwexpress.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** "ABC1D23" → "ABC1D23" (Mercosul) ou "ABC-1234" (antiga). */
fun placaFormatada(p: String): String {
    val s = p.uppercase().filter { it.isLetterOrDigit() }
    return if (s.length == 7 && s[4].isDigit()) "${s.take(3)}-${s.drop(3)}" else s
}

/**
 * Placa como a de verdade (padrão Mercosul): faixa azul com BRASIL e bandeira, letras pretas no branco.
 * Igual à do site do gestor. [largura] controla o tamanho (proporção real ~ 3,1 : 1).
 */
@Composable
fun PwPlaca(placa: String, modifier: Modifier = Modifier, largura: Dp = 200.dp) {
    val escala = largura.value / 200f
    val forma = RoundedCornerShape((9 * escala).dp)
    Column(
        modifier.width(largura).shadow((6 * escala).dp, forma).clip(forma)
            .background(Color.White).border((2 * escala).dp, Color(0xFF1F2937), forma),
    ) {
        Row(
            Modifier.fillMaxWidth().background(Brush.horizontalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF2547A8)))).padding(vertical = (3 * escala).dp, horizontal = (6 * escala).dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "Mercosul" (marca) à esquerda e a bandeira à direita, como na placa
            Text("MERCOSUL", color = Color.White.copy(alpha = 0.75f), fontSize = (6.5 * escala).sp, fontWeight = FontWeight.Bold, letterSpacing = (0.5 * escala).sp)
            Text("BRASIL", color = Color.White, fontSize = (10 * escala).sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (2 * escala).sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Bandeira(Modifier.size(width = (14 * escala).dp, height = (10 * escala).dp))
        }
        Text(
            placaFormatada(placa).ifEmpty { "ABC1D23" },
            style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = (30 * escala).sp, letterSpacing = (1.6 * escala).sp, color = Color(0xFF111111), textAlign = TextAlign.Center),
            modifier = Modifier.fillMaxWidth().padding(top = (2 * escala).dp, bottom = (5 * escala).dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun Bandeira(modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(1.dp)).background(Color(0xFF009B3A)), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth(0.72f).padding(vertical = 1.dp).background(Color(0xFFFEDF00), RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            Box(Modifier.size(4.dp).background(Color(0xFF002776), RoundedCornerShape(50)))
        }
        Spacer(Modifier.size(0.dp))
    }
}
