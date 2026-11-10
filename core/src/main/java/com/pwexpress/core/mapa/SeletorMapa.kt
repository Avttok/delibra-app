package com.pwexpress.core.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwIcones

/**
 * Escolher um ponto em tela cheia: arraste o mapa até o alfinete do centro ficar no lugar certo
 * (ou toque no mapa para pular até lá) e confirme.
 */
@Composable
fun PwSeletorMapa(inicial: Pair<Double, Double>?, onFechar: () -> Unit, titulo: String = "Marcar no mapa", onEscolher: (Double, Double) -> Unit) {
    val c = Pw.cores
    var centro by remember { mutableStateOf(inicial) }
    var irPara by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onFechar, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(c.fundo)) {
            PwMapa(maximizavel = false,
                paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxSize(),
                inicio = inicial, zoomInicio = 16.0, irPara = irPara,
                onToque = { la, lo -> irPara = la to lo },
                onCentro = { la, lo -> centro = la to lo },
            )
            // alfinete fixo no centro
            Column(Modifier.align(Alignment.Center).padding(bottom = 54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(c.gradOuro).border(3.dp, c.fundo, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(PwIcones.MapPin, null, tint = c.sobreOuro, modifier = Modifier.size(22.dp))
                }
                Box(Modifier.size(width = 3.dp, height = 14.dp).background(c.ouro))
            }
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().statusBarsPadding().padding(12.dp).clip(RoundedCornerShape(20.dp)).background(c.fundo2.copy(alpha = 0.95f))
                    .border(1.dp, c.bordaForte, RoundedCornerShape(20.dp)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PwBotaoIcone(PwIcones.ArrowLeft, "Voltar", onFechar)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(titulo, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                    Text("Arraste o mapa até o alfinete ficar no lugar", color = c.texto3, fontSize = 12.5.sp, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(16.dp)) {
                PwBotaoOuro("Usar este ponto", onClick = { centro?.let { onEscolher(it.first, it.second) } }, habilitado = centro != null, icone = PwIcones.Check)
            }
        }
    }
}

