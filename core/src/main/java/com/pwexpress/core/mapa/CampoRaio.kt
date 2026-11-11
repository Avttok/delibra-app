package com.pwexpress.core.mapa

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwIcones
import kotlinx.coroutines.delay

/** Barra grossa com bolinha dourada (a mesma do "Raio de disponibilidade"). */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PwBarraBolinha(valor: Float, onMudar: (Float) -> Unit, faixa: ClosedFloatingPointRange<Float>, passo: Float, modifier: Modifier = Modifier, habilitado: Boolean = true) {
    val c = Pw.cores
    val fracao = ((valor - faixa.start) / (faixa.endInclusive - faixa.start)).coerceIn(0f, 1f)
    Slider(
        value = valor, onValueChange = { v -> onMudar(if (passo > 0) (Math.round(v / passo) * passo) else v) }, valueRange = faixa, enabled = habilitado,
        modifier = modifier.height(48.dp),
        thumb = {
            Box(Modifier.size(30.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(c.gradOuro).border(3.dp, c.fundo2, CircleShape))
        },
        track = {
            Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(c.bordaForte)) {
                Box(Modifier.fillMaxWidth(fracao).height(10.dp).clip(RoundedCornerShape(99.dp)).background(c.gradOuro))
            }
        },
    )
}

private var centroExemplo: Pair<Double, Double> = -27.5954 to -48.5482

/**
 * Um raio do sistema: barra com bolinha, o tamanho e (tocando no mapa) o círculo de verdade.
 * [dupla] = duas entregas de exemplo, para ver quando os raios se encostam.
 */
@Composable
fun CampoRaio(valor: Double, unidade: String, min: Double, max: Double, passo: Double, onSalvar: (Double) -> Unit, habilitado: Boolean = true, dupla: Boolean = false) {
    val c = Pw.cores
    // barra e número mudam o rascunho; salva só no ✓
    var v by remember(valor) { mutableFloatStateOf(valor.toFloat()) }
    var mapa by remember { mutableStateOf(false) }
    var centro by remember { mutableStateOf(centroExemplo) }
    val mudou = v.toDouble() != valor
    val metros = if (unidade == "km") v * 1000.0 else v.toDouble()
    val faixa = min.toFloat()..max.toFloat().coerceAtLeast(min.toFloat() + 1f)
    val controles: @Composable () -> Unit = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwBarraBolinha(v, { v = it }, faixa, passo.toFloat(), Modifier.weight(1f), habilitado)
                Spacer(Modifier.width(10.dp))
                PwBotaoIcone(PwIcones.Mapa, if (mapa) "Esconder o mapa" else "Ver no mapa", { mapa = !mapa }, ativo = mapa, tamanho = 38.dp)
            }
            Spacer(Modifier.height(6.dp))
            // número à mão (útil para raios grandes, que a barra deixa difícil acertar)
            com.pwexpress.core.designsystem.PwPasso(v.toDouble(), { n -> v = n.toFloat().coerceIn(faixa) }, Modifier.fillMaxWidth(), passo = passo, min = min, max = max, unidade = unidade)
            if (v <= 0f) Text("0 = sem limite", color = c.texto3, fontSize = 12.sp)
            if (mudou && habilitado) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    com.pwexpress.core.designsystem.PwBotaoOuro("Salvar ${if (v <= 0f) "sem limite" else if (unidade == "km") "${com.pwexpress.core.designsystem.numeroBr(v.toDouble(), 1)} km" else "${v.toInt()} m"}",
                        onClick = { onSalvar(v.toDouble()) }, icone = PwIcones.Check, altura = 44.dp, modifier = Modifier.weight(1f))
                    PwBotaoIcone(PwIcones.X, "Desfazer", { v = valor.toFloat() }, tamanho = 44.dp)
                }
            }
        }
    }
    Column {
        controles()
        if (mapa) {
            val forma = RoundedCornerShape(16.dp)
            // a segunda entrega fica a 2,4× o raio inicial: ao aumentar o raio, dá para ver quando encostam
            val dist = remember { maxOf(metros * 2.4, 400.0) }
            val outra = centro.first to centro.second + dist / (111_320.0 * Math.cos(Math.toRadians(centro.first)))
            Box(Modifier.fillMaxWidth().height(220.dp).clip(forma).border(1.dp, c.borda, forma)) {
                PwMapa(
                    paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(220.dp),
                    marcadores = listOfNotNull(
                        MarcadorMapa("a", centro.first, centro.second, "#D4AF37", "1", pino = false),
                        if (dupla) MarcadorMapa("b", outra.first, outra.second, "#38BDF8", "2", pino = false) else null,
                    ),
                    circulo = if (metros > 0) centro to metros else null,
                    enquadrar = "${centro.first},${centro.second},${metros.toInt()}",
                    onToque = { la, lo -> centro = la to lo; centroExemplo = centro },
                    tituloTelaCheia = "Raio",
                    // na tela cheia: o mesmo ajuste do raio embaixo
                    rodapeTelaCheia = {
                        Column(Modifier.fillMaxWidth().clip(forma).background(c.vidroForte).border(1.dp, c.borda, forma).padding(14.dp)) { controles() }
                    },
                )
            }
            Text(
                (if (dupla) "Entrega 2 a ${com.pwexpress.core.designsystem.numeroBr(dist / 1000.0, 1)} km: " + (if (dist <= 2 * metros) "os raios se encostam (mesma zona). " else "os raios não se encostam. ") else "") +
                    "Toque no mapa para mudar o centro do exemplo.",
                color = c.texto3, fontSize = 12.sp,
            )
        }
    }
}
