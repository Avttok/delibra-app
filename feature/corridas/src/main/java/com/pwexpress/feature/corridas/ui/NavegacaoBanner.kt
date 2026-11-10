package com.pwexpress.feature.corridas.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.mapa.Manobra
import com.pwexpress.core.mapa.Navegacao
import com.pwexpress.core.mapa.PreferenciasNavegacao
import com.pwexpress.core.mapa.RotaNavegacao
import com.pwexpress.core.mapa.TipoManobra
import com.pwexpress.core.mapa.localizar
import com.pwexpress.core.notificacoes.CorridaEmCurso
import kotlinx.coroutines.CancellationException

/** Leitura contínua do GPS enquanto a tela está aberta (1 s), para a navegação acompanhar. */
data class LeituraNav(val lat: Double, val lng: Double, val rumo: Float?, val velocidade: Float?, val precisao: Float)

@SuppressLint("MissingPermission")
@Composable
fun rememberLeituraContinua(ativa: Boolean): LeituraNav? {
    val ctx = LocalContext.current
    var leitura by remember { mutableStateOf<LeituraNav?>(null) }
    DisposableEffect(ativa) {
        if (!ativa || ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return@DisposableEffect onDispose {}
        val cliente = LocationServices.getFusedLocationProviderClient(ctx)
        val cb = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                r.lastLocation?.let { l -> leitura = LeituraNav(l.latitude, l.longitude, if (l.hasBearing() && l.speed > 1.5f) l.bearing else null, if (l.hasSpeed()) l.speed else null, if (l.hasAccuracy()) l.accuracy else 30f) }
            }
        }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1_000L).setMinUpdateIntervalMillis(700L).build()
        runCatching { cliente.requestLocationUpdates(req, cb, Looper.getMainLooper()) }
        onDispose { cliente.removeLocationUpdates(cb) }
    }
    return leitura
}

/** Fala as instruções (português do Brasil). */
private class Voz(ctx: Context) : TextToSpeech.OnInitListener {
    private var pronta = false
    private val tts: TextToSpeech = TextToSpeech(ctx.applicationContext, this)
    override fun onInit(st: Int) {
        pronta = st == TextToSpeech.SUCCESS
        if (pronta) runCatching { tts.language = java.util.Locale.forLanguageTag("pt-BR") }
    }
    fun falar(t: String) { if (pronta) runCatching { tts.speak(t, TextToSpeech.QUEUE_FLUSH, null, "nav") } }
    fun fechar() = runCatching { tts.stop(); tts.shutdown() }
}

private fun distanciaTexto(m: Double): String = when {
    m < 20 -> "agora"
    m < 1000 -> "${(Math.round(m / 10.0) * 10).toInt()} m"
    else -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f km", m / 1000)
}

private fun distanciaFalada(m: Double): String = when {
    m < 1000 -> "${(Math.round(m / 50.0) * 50).toInt().coerceAtLeast(50)} metros"
    else -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f quilômetros", m / 1000).replace(",0 ", " ")
}

private fun iconeManobra(t: TipoManobra): ImageVector = when (t) {
    TipoManobra.DIREITA, TipoManobra.FORTE_DIREITA -> PwIcones.CornerUpRight
    TipoManobra.ESQUERDA, TipoManobra.FORTE_ESQUERDA -> PwIcones.CornerUpLeft
    TipoManobra.LEVE_DIREITA -> PwIcones.ArrowUpRight
    TipoManobra.LEVE_ESQUERDA -> PwIcones.ArrowUpLeft
    TipoManobra.RETORNO -> PwIcones.Undo2
    TipoManobra.ROTATORIA -> PwIcones.RotateCcw
    TipoManobra.CHEGADA -> PwIcones.MapPin
    TipoManobra.SAIDA -> PwIcones.Navigation
    TipoManobra.SEGUIR -> PwIcones.ArrowUp
}

/**
 * Navegação curva a curva até [destino] dentro do app (como no Google Maps): banner com a próxima curva,
 * a seguinte, quanto falta, voz e a instrução na notificação. Recalcula quando sai do caminho.
 */
@Composable
fun NavegacaoBanner(destinoId: String, destino: Pair<Double, Double>, modifier: Modifier = Modifier, onRota: (RotaNavegacao?) -> Unit = {}) {
    if (!PreferenciasNavegacao.ligada) return
    val ctx = LocalContext.current
    val c = Pw.cores
    var vozLigada by com.pwexpress.core.designsystem.lembrarLigado("navegacao_voz_local", true)
    val leitura = rememberLeituraContinua(true)
    var rota by remember(destinoId) { mutableStateOf<RotaNavegacao?>(null) }
    var calculando by remember(destinoId) { mutableStateOf(false) }
    var ultimoCalculo by remember(destinoId) { mutableStateOf(0L) }
    val indiceAtual = remember(destinoId) { intArrayOf(0) }
    var falado by remember(destinoId) { mutableStateOf(setOf<String>()) }
    var falhou by remember(destinoId) { mutableStateOf(false) }
    val voz = remember { Voz(ctx) }
    DisposableEffect(Unit) { onDispose { voz.fechar(); CorridaEmCurso.instruir(null) } }

    // calcula (e recalcula se sair do caminho)
    val pos = leitura?.let { it.lat to it.lng }
    val noCaminho = rota?.let { r -> pos?.let { r.localizar(it, indiceAtual[0]) } }
    val fora = noCaminho != null && noCaminho.foraM > PreferenciasNavegacao.recalcularM + (leitura?.precisao ?: 0f).coerceAtMost(40f)
    LaunchedEffect(destinoId, pos == null, fora, if (fora) System.currentTimeMillis() / 13_000 else 0L) {
        if (pos == null || calculando) return@LaunchedEffect
        if (rota != null && !fora) return@LaunchedEffect
        if (System.currentTimeMillis() - ultimoCalculo < 12_000 && rota != null) return@LaunchedEffect
        calculando = true
        try {
            val r = Navegacao.rota(pos, destino, rumo = leitura?.rumo)
            falhou = r == null
            if (r != null) { rota = r; indiceAtual[0] = 0; falado = emptySet(); onRota(r); if (fora && vozLigada && PreferenciasNavegacao.voz) voz.falar("Recalculando a rota") }
        } catch (e: CancellationException) { throw e } catch (_: Exception) { falhou = true }
        ultimoCalculo = System.currentTimeMillis()
        calculando = false
    }

    val r = rota
    val aqui = noCaminho
    if (r == null || aqui == null) {
        if (calculando || (pos == null && !falhou)) BannerBase(modifier) { Text(if (pos == null) "Procurando o GPS…" else "Calculando o caminho…", color = Color(0xFF0A0A0A), fontWeight = FontWeight.Bold) }
        return
    }
    indiceAtual[0] = aqui.indice
    val proximas = r.manobras.filter { it.indice > aqui.indice || (it.tipo == TipoManobra.SAIDA && aqui.indice == 0 && it.indice == 0) }
        .filter { it.tipo != TipoManobra.SAIDA || aqui.percorridoM < 30 }
    val m: Manobra = proximas.firstOrNull() ?: r.manobras.lastOrNull() ?: return
    val ate = (r.acumulado.getOrElse(m.indice) { r.distanciaM } - aqui.percorridoM).coerceAtLeast(0.0)
    val falta = (r.distanciaM - aqui.percorridoM).coerceAtLeast(0.0)
    val depois = proximas.getOrNull(1)

    // voz: um aviso antes e outro na hora
    LaunchedEffect(m.indice, ate < 220, ate < 45) {
        if (!vozLigada || !PreferenciasNavegacao.voz) return@LaunchedEffect
        val k1 = "a${m.indice}"; val k2 = "h${m.indice}"
        if (ate < 45 && k2 !in falado) { voz.falar(m.instrucao); falado = falado + k1 + k2 }
        else if (ate in 45.0..220.0 && k1 !in falado && m.tipo != TipoManobra.SEGUIR) { voz.falar("Em ${distanciaFalada(ate)}, ${m.instrucao.replaceFirstChar { it.lowercase() }}"); falado = falado + k1 }
    }
    // notificação da corrida mostra a próxima curva
    LaunchedEffect(m.instrucao, (ate / 50).toInt()) {
        CorridaEmCurso.instruir(if (PreferenciasNavegacao.naNotificacao) "${distanciaTexto(ate).replaceFirstChar { it.uppercase() }} · ${m.instrucao}" else null)
    }

    BannerBase(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                AnimatedContent(m.tipo, label = "manobra") { t -> Icon(iconeManobra(t), null, tint = c.ouro, modifier = Modifier.size(32.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(distanciaTexto(ate), color = Color(0xFF0A0A0A), fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 24.sp, maxLines = 1)
                Text(m.instrucao, color = Color(0xFF1A1405), fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(0x22000000)).clickable { vozLigada = !vozLigada }, contentAlignment = Alignment.Center) {
                Icon(if (vozLigada && PreferenciasNavegacao.voz) PwIcones.Volume2 else PwIcones.VolumeX, if (vozLigada) "Desligar voz" else "Ligar voz", tint = Color(0xFF0A0A0A), modifier = Modifier.size(20.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (depois != null) {
                Icon(iconeManobra(depois.tipo), null, tint = Color(0xFF2A2210), modifier = Modifier.size(15.dp))
                Text("Depois: ${depois.instrucao}", color = Color(0xFF2A2210), fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            } else Spacer(Modifier.weight(1f))
            Text("${distanciaTexto(falta)} até a parada${if (r.duracaoS > 0) " · ${((r.duracaoS * (falta / r.distanciaM.coerceAtLeast(1.0))) / 60).toInt().coerceAtLeast(1)} min" else ""}",
                color = Color(0xFF0A0A0A), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun BannerBase(modifier: Modifier, conteudo: @Composable () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(forma)
            .background(Brush.linearGradient(listOf(c.ouroClaro, c.ouro, c.ouroEscuro)))
            .border(1.dp, Color(0x33FFFFFF), forma)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) { conteudo() }
}
