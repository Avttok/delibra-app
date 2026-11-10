package com.pwexpress.core.designsystem

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Um passo do tour: aponta um alvo (marcado com [alvoTour]) ou aparece no centro. */
data class PassoTour(val alvo: String?, val titulo: String, val texto: String)

/**
 * Tour guiado do app: abre sozinho na primeira vez de cada tour e pode ser revisto pelo menu da conta.
 * Os alvos se registram com [alvoTour]; o [PwTourCamada] desenha o recorte e o balão por cima de tudo.
 */
object PwTour {
    internal val alvos = mutableStateMapOf<String, Rect>()
    internal var atual by mutableStateOf<Pair<String, List<PassoTour>>?>(null)
    internal var passo by mutableStateOf(0)
    private var ctx: Context? = null

    fun init(context: Context) { ctx = context.applicationContext }
    private fun prefs() = ctx?.getSharedPreferences("pwex_tour", Context.MODE_PRIVATE)

    /** Abre o tour (pelo menu, a qualquer momento). */
    fun abrir(id: String, passos: List<PassoTour>) { passo = 0; atual = id to passos }

    /** Abre só se esta pessoa ainda não viu este tour neste celular. */
    fun primeiraVez(id: String, passos: List<PassoTour>) {
        if (atual == null && prefs()?.getBoolean(id, false) != true) abrir(id, passos)
    }

    /** Há um tour aberto? (o app usa para o botão voltar fechar o tour) */
    val aberto: Boolean get() = atual != null

    fun fechar() {
        atual?.first?.let { prefs()?.edit()?.putBoolean(it, true)?.apply() }
        atual = null
    }
}

/** Marca um elemento como alvo do tour. */
fun Modifier.alvoTour(chave: String): Modifier = onGloballyPositioned { PwTour.alvos[chave] = it.boundsInRoot() }

/** Camada do tour: ponha por último na raiz da tela. */
@Composable
fun PwTourCamada() {
    val (id, passos) = PwTour.atual ?: return
    val i = PwTour.passo.coerceIn(0, passos.lastIndex)
    val p = passos[i]
    val c = Pw.cores
    var origem by remember { mutableStateOf(Offset.Zero) }
    val alvo = p.alvo?.let { PwTour.alvos[it] }?.translate(-origem)
    val dens = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxSize().onPlaced { origem = it.positionInRoot() }
            .clickable(remember { MutableInteractionSource() }, null) { /* segura os toques por baixo */ },
    ) {
        val folga = with(dens) { 8.dp.toPx() }
        Canvas(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            drawRect(Color.Black.copy(alpha = 0.64f))
            if (alvo != null) {
                val r = Rect(alvo.left - folga, alvo.top - folga, alvo.right + folga, alvo.bottom + folga)
                drawRoundRect(Color.Transparent, r.topLeft, r.size, CornerRadius(18.dp.toPx()), blendMode = BlendMode.Clear)
                drawRoundRect(c.ouro, r.topLeft, r.size, CornerRadius(18.dp.toPx()), style = Stroke(2.dp.toPx()))
            }
        }
        val alturaTela = with(dens) { maxHeight.toPx() }
        val larguraBalao = minOf(maxWidth - 32.dp, 380.dp)
        // embaixo do alvo, ou em cima se não couber; sem alvo, no centro
        val y: Int? = alvo?.let { a ->
            val abaixo = a.bottom + with(dens) { 18.dp.toPx() }
            if (abaixo + with(dens) { 220.dp.toPx() } < alturaTela) abaixo.toInt()
            else (a.top - with(dens) { 18.dp.toPx() } - with(dens) { 220.dp.toPx() }).coerceAtLeast(with(dens) { 16.dp.toPx() }).toInt()
        }
        val forma = RoundedCornerShape(22.dp)
        Column(
            Modifier.then(if (y == null) Modifier.align(Alignment.Center) else Modifier.align(Alignment.TopCenter).offset { IntOffset(0, y) })
                .width(larguraBalao).clip(forma).background(c.fundo2).border(1.dp, c.bordaForte, forma).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1} de ${passos.size}", color = c.ouro, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                passos.indices.forEach { k ->
                    Box(Modifier.padding(start = 4.dp).size(if (k == i) 8.dp else 6.dp).clip(CircleShape).background(if (k == i) c.ouro else c.bordaForte))
                }
            }
            Text(p.titulo, color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(p.texto, color = c.texto2, fontSize = 14.5.sp, lineHeight = 21.sp)
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (i == passos.lastIndex) "Fechar" else "Pular", color = c.texto3, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).clickable { PwTour.fechar() }.padding(horizontal = 8.dp, vertical = 10.dp))
                Spacer(Modifier.weight(1f))
                if (i > 0) PwBotaoVidro("Voltar", onClick = { PwTour.passo = i - 1 }, modifier = Modifier.widthIn(min = 96.dp))
                PwBotaoOuro(if (i == passos.lastIndex) "Terminar" else "Próximo", onClick = { if (i == passos.lastIndex) PwTour.fechar() else PwTour.passo = i + 1 },
                    altura = 48.dp, tamanhoTexto = 15, modifier = Modifier.widthIn(min = 120.dp))
            }
        }
    }
    @Suppress("UNUSED_VARIABLE") val usado = id
}

/** (?) ao lado de uma opção que pode confundir: toque e a explicação aparece. */
@Composable
fun PwDica(titulo: String, texto: String, modifier: Modifier = Modifier) {
    val c = Pw.cores
    Box(
        modifier.size(22.dp).clip(CircleShape).border(1.dp, c.bordaForte, CircleShape).background(c.vidroSutil)
            .clickable { PwToast.mostrar(texto, TipoAviso.INFO, titulo) },
        contentAlignment = Alignment.Center,
    ) { Text("?", color = c.texto2, fontWeight = FontWeight.Black, fontSize = 12.sp) }
}

@Suppress("unused") private val naoUsado = Size.Zero
