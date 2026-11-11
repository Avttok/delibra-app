package com.pwexpress.core.jogo

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwIcones
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Pergunta antes de abrir o jogo (o motoboy tocou várias vezes no anel). */
@Composable
fun PerguntaJogo(onJogar: () -> Unit, onFechar: () -> Unit) {
    val c = Pw.cores
    val perfil by JogoServidor.perfil.collectAsState()
    LaunchedEffect(Unit) { JogoServidor.carregar() }
    com.pwexpress.core.designsystem.PwJanela("Jogar enquanto espera?", onFechar) {
        Text("Desvie do trânsito na mão e na contramão, pegue pacotes e moedas. Se chegar uma corrida, o jogo fecha sozinho e a oferta aparece na hora.", color = c.texto2, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Recorde: ${max(perfil?.recorde ?: 0, recorde(LocalContext.current))}", color = c.ouro, fontWeight = FontWeight.Bold, fontFamily = Archivo)
            perfil?.let { Text("Level ${it.level}", color = c.texto2, fontWeight = FontWeight.Bold, fontFamily = Archivo) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwBotaoVidro("Agora não", onClick = onFechar, modifier = Modifier.weight(1f))
            PwBotaoOuro("Jogar", onClick = onJogar, icone = PwIcones.Gamepad2, modifier = Modifier.weight(1f))
        }
    }
}

private fun recorde(ctx: Context) = ctx.getSharedPreferences("pwex_jogo", Context.MODE_PRIVATE).getInt("recorde_pontos", 0)
private fun salvarRecorde(ctx: Context, v: Int) = ctx.getSharedPreferences("pwex_jogo", Context.MODE_PRIVATE).edit().putInt("recorde_pontos", v).apply()

// ------------------------------------------------------------------ mundo

private const val FAIXAS = 4               // 0 e 1: contramão (vêm de frente) · 2 e 3: mão (mesmo sentido)
private const val Y_MOTO = 0.82f
/** Centro da faixa em fração da largura da tela (a pista ocupa 90% no meio). */
private fun xFaixa(f: Float) = 0.05f + 0.9f / FAIXAS * (f + 0.5f)

private enum class Tipo(val obstaculo: Boolean) {
    CARRO_FRENTE(true), CARRO_MESMO(true), MOTO_FRENTE(true), CAMINHAO(true), CONE(true), BURACO(true), OLEO(false),
    PACOTE(false), PACOTE_OURO(false), CAIXA(false), MOEDA(false), ESCUDO(false), IMA(false), TURBO(false),
}

private class Coisa(var faixa: Int, var y: Float, val tipo: Tipo, val cor: Color, var pegou: Boolean = false) {
    /** Quanto anda em relação à pista: contramão vem rápido, mesmo sentido quase para. */
    val rel get() = when (tipo) { Tipo.CARRO_FRENTE -> 1.75f; Tipo.MOTO_FRENTE -> 1.95f; Tipo.CARRO_MESMO -> 0.45f; Tipo.CAMINHAO -> 0.32f; else -> 1f }
    val comprimento get() = when (tipo) { Tipo.CAMINHAO -> 0.2f; Tipo.CARRO_FRENTE, Tipo.CARRO_MESMO -> 0.11f; Tipo.MOTO_FRENTE -> 0.07f; else -> 0.05f }
}
private class Particula(var x: Float, var y: Float, var vx: Float, var vy: Float, var vida: Float, val cor: Color, val tam: Float)
private class Flutua(var x: Float, var y: Float, val texto: String, var vida: Float, val cor: Color)

private val CORES_CARRO = listOf(Color(0xFFEF4444), Color(0xFF38BDF8), Color(0xFFF5F5F5), Color(0xFFA78BFA), Color(0xFF22C55E), Color(0xFFF59E0B), Color(0xFF64748B))

/**
 * "Corre, motoboy!": quatro faixas (duas na contramão, duas na mão), trânsito andando, pacotes, moedas e poderes.
 * Toque à esquerda/direita (ou arraste) para trocar de faixa. Moedas e loja ficam no servidor.
 */
@Composable
fun JogoEntrega(onFechar: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val vibrar = LocalHapticFeedback.current
    val escopo = rememberCoroutineScope()
    val medidor = rememberTextMeasurer()
    val perfil by JogoServidor.perfil.collectAsState()
    LaunchedEffect(Unit) { JogoServidor.carregar() }

    var tela by remember { mutableStateOf("menu") }        // menu · loja · ranking · conquistas · jogo
    var estado by remember { mutableStateOf("pronto") }    // pronto · rodando · pausado · fim
    BackHandler { if (tela != "menu" && tela != "jogo") tela = "menu" else if (estado == "rodando") estado = "pausado" else onFechar() }

    var faixa by remember { mutableIntStateOf(2) }
    var faixaVisual by remember { mutableFloatStateOf(2f) }
    val coisas = remember { mutableStateListOf<Coisa>() }
    val particulas = remember { mutableStateListOf<Particula>() }
    val flutuas = remember { mutableStateListOf<Flutua>() }
    var pacotes by remember { mutableIntStateOf(0) }
    var moedasPartida by remember { mutableIntStateOf(0) }
    var distancia by remember { mutableFloatStateOf(0f) }
    var vel by remember { mutableFloatStateOf(0.45f) }
    var rolagem by remember { mutableFloatStateOf(0f) }
    var tempo by remember { mutableFloatStateOf(0f) }
    var proxima by remember { mutableFloatStateOf(0.5f) }
    var tremor by remember { mutableFloatStateOf(0f) }
    var escudo by remember { mutableIntStateOf(0) }
    var imaAte by remember { mutableFloatStateOf(0f) }
    var turboAte by remember { mutableFloatStateOf(0f) }
    var lentoAte by remember { mutableFloatStateOf(0f) }
    var combo by remember { mutableIntStateOf(0) }
    var recordeNovo by remember { mutableStateOf(false) }
    var resultado by remember { mutableStateOf<String?>(null) }
    val poderesEscolhidos = remember { mutableStateListOf<String>() }
    val pontos = pacotes * 10 + (distancia / 10f).toInt()

    val corMoto = perfil?.equipadoDe("moto")?.cor ?: c.ouro
    val brilhoMoto = perfil?.equipadoDe("moto")?.brilho == true
    val capacete = perfil?.equipadoDe("capacete")
    val rastro = perfil?.equipadoDe("rastro")

    fun reiniciar() {
        coisas.clear(); particulas.clear(); flutuas.clear()
        pacotes = 0; moedasPartida = 0; distancia = 0f; vel = 0.45f; tempo = 0f; proxima = 0.5f; faixa = 2; faixaVisual = 2f
        tremor = 0f; combo = 0; recordeNovo = false; resultado = null
        escudo = if ("poder_escudo" in poderesEscolhidos) 1 else 0
        imaAte = if ("poder_ima" in poderesEscolhidos) 20f else 0f
        turboAte = if ("poder_turbo" in poderesEscolhidos) 20f else 0f
        lentoAte = 0f
        tela = "jogo"; estado = "rodando"
    }
    fun mover(d: Int) { if (estado == "rodando") faixa = (faixa + d).coerceIn(0, FAIXAS - 1) }
    fun estourar(x: Float, y: Float, cor: Color, n: Int = 10) {
        repeat(n) { particulas.add(Particula(x, y, (Random.nextFloat() - 0.5f) * 0.35f, -Random.nextFloat() * 0.7f, 0.6f + Random.nextFloat() * 0.3f, cor, 3f + Random.nextFloat() * 4f)) }
    }
    fun terminar() {
        estado = "fim"
        vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
        val p = pontos
        if (p > recorde(ctx)) { salvarRecorde(ctx, p); recordeNovo = true }
        val gastos = poderesEscolhidos.toList()
        escopo.launch {
            JogoServidor.registrar(p, moedasPartida, distancia.toInt(), gastos)
                .onSuccess { resultado = "+$moedasPartida moedas" }
                .onFailure { resultado = it.message }
            poderesEscolhidos.clear()
        }
    }

    // ---------------------------------------------------------------- laço do jogo
    LaunchedEffect(estado, tela) {
        if (estado != "rodando" || tela != "jogo") return@LaunchedEffect
        var ultimo = 0L
        while (estado == "rodando") {
            val t = withFrameNanos { it }
            if (ultimo == 0L) { ultimo = t; continue }
            val dt = min((t - ultimo) / 1e9f, 0.05f); ultimo = t
            tempo += dt
            val alvo = min(1.35f, 0.45f + tempo * 0.011f)
            vel = if (tempo < lentoAte) alvo * 0.6f else alvo
            rolagem = (rolagem + dt * vel) % 1f
            distancia += dt * vel * 120f
            faixaVisual += (faixa - faixaVisual) * min(1f, dt * 14f)
            tremor = max(0f, tremor - dt * 3f)

            coisas.forEach { it.y += dt * vel * it.rel }
            // ímã: pacotes e moedas das faixas vizinhas vêm para a sua
            if (tempo < imaAte) coisas.forEach { k ->
                if (!k.tipo.obstaculo && k.tipo != Tipo.OLEO && k.y > 0.45f && abs(k.faixa - faixa) == 1) k.faixa = faixa
            }
            coisas.removeAll { it.y > 1.3f }
            particulas.forEach { p -> p.x += p.vx * dt; p.y += p.vy * dt; p.vy += dt * 1.2f; p.vida -= dt }
            particulas.removeAll { it.vida <= 0f }
            flutuas.forEach { f -> f.y -= dt * 0.12f; f.vida -= dt }
            flutuas.removeAll { it.vida <= 0f }
            // rastro da moto (item da loja)
            if (rastro != null && rastro.chave != "rastro_nenhum" && Random.nextFloat() < 0.7f) {
                val cor = if (rastro.arco) Color.hsv((tempo * 220f) % 360f, 0.8f, 1f) else (rastro.cor ?: c.ouro)
                particulas.add(Particula(xFaixa(faixaVisual), Y_MOTO + 0.05f, (Random.nextFloat() - 0.5f) * 0.05f, 0.25f, 0.45f, cor, 3.5f))
            }

            // aparece uma fileira nova: sempre sobra uma faixa livre
            proxima -= dt
            if (proxima <= 0f) {
                val bloqueadas = coisas.filter { it.tipo.obstaculo && it.y < 0.35f }.map { it.faixa }.toSet()
                val livres = (0 until FAIXAS).filter { it !in bloqueadas }.ifEmpty { listOf(Random.nextInt(FAIXAS)) }
                val livre = livres.random()
                val nObst = when { tempo > 40 -> 2 + if (Random.nextFloat() < 0.3f) 1 else 0; tempo > 15 -> if (Random.nextFloat() < 0.5f) 2 else 1; else -> 1 }
                (0 until FAIXAS).filter { it != livre && it !in bloqueadas }.shuffled().take(nObst).forEach { f ->
                    val tipo = if (f <= 1) when (Random.nextInt(10)) { in 0..5 -> Tipo.CARRO_FRENTE; in 6..7 -> Tipo.MOTO_FRENTE; 8 -> Tipo.BURACO; else -> Tipo.CONE }
                               else when (Random.nextInt(12)) { in 0..5 -> Tipo.CARRO_MESMO; in 6..7 -> Tipo.CAMINHAO; 8, 9 -> Tipo.CONE; 10 -> Tipo.BURACO; else -> Tipo.OLEO }
                    coisas.add(Coisa(f, -0.25f, tipo, CORES_CARRO.random()))
                }
                // o que dá para pegar, na faixa livre
                val r = Random.nextFloat()
                val bonus = when {
                    r < 0.03f -> Tipo.ESCUDO; r < 0.06f -> Tipo.IMA; r < 0.085f -> Tipo.TURBO
                    r < 0.16f -> Tipo.PACOTE_OURO; r < 0.3f -> Tipo.CAIXA; r < 0.55f -> Tipo.MOEDA; r < 0.9f -> Tipo.PACOTE
                    else -> null
                }
                bonus?.let { coisas.add(Coisa(livre, -0.2f - Random.nextFloat() * 0.08f, it, Color.Unspecified)) }
                proxima = max(0.36f, 1.0f - tempo * 0.011f) / (vel / 0.45f) * 0.95f
            }

            // batidas e pegas perto da moto
            for (k in coisas) {
                if (k.pegou || k.faixa != faixa) continue
                val metade = k.comprimento / 2f
                if (k.y + metade < Y_MOTO - 0.05f || k.y - metade > Y_MOTO + 0.06f) continue
                val px = faixaVisual
                when (k.tipo) {
                    Tipo.PACOTE, Tipo.PACOTE_OURO, Tipo.CAIXA, Tipo.MOEDA -> {
                        k.pegou = true; combo++
                        val (pac, moe, cor) = when (k.tipo) {
                            Tipo.PACOTE -> Triple(1, 1, c.ouro); Tipo.CAIXA -> Triple(3, 2, Color(0xFFD97706))
                            Tipo.PACOTE_OURO -> Triple(5, 5, Color(0xFFFDE68A)); else -> Triple(0, 1, Color(0xFFFACC15))
                        }
                        val mult = if (tempo < turboAte) 2 else 1
                        pacotes += pac; moedasPartida += moe * mult
                        estourar(xFaixa(px), Y_MOTO - 0.02f, cor, if (k.tipo == Tipo.PACOTE_OURO) 18 else 8)
                        flutuas.add(Flutua(xFaixa(px), Y_MOTO - 0.08f, if (moe * mult > 0) "+${moe * mult}" else "+$pac", 0.8f, cor))
                        vibrar.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                    Tipo.ESCUDO -> { k.pegou = true; escudo = min(escudo + 1, 2); flutuas.add(Flutua(xFaixa(px), Y_MOTO - 0.08f, "ESCUDO", 1f, Color(0xFF38BDF8))) }
                    Tipo.IMA -> { k.pegou = true; imaAte = tempo + 10f; flutuas.add(Flutua(xFaixa(px), Y_MOTO - 0.08f, "ÍMÃ", 1f, Color(0xFFF472B6))) }
                    Tipo.TURBO -> { k.pegou = true; turboAte = tempo + 10f; flutuas.add(Flutua(xFaixa(px), Y_MOTO - 0.08f, "MOEDAS x2", 1f, Color(0xFF22C55E))) }
                    Tipo.OLEO -> { k.pegou = true; lentoAte = tempo + 1.6f; tremor = 0.5f; flutuas.add(Flutua(xFaixa(px), Y_MOTO - 0.08f, "Derrapou!", 0.9f, Color(0xFFA1A1AA))) }
                    else -> if (abs(faixaVisual - faixa) < 0.4f) {
                        if (escudo > 0) {
                            escudo--; k.pegou = true; tremor = 1f; combo = 0
                            estourar(xFaixa(px), Y_MOTO - 0.04f, Color(0xFF38BDF8), 20)
                            vibrar.performHapticFeedback(HapticFeedbackType.LongPress)
                        } else { tremor = 1.4f; terminar(); break }
                    }
                }
            }
            coisas.removeAll { it.pegou }
        }
    }

    Dialog(onDismissRequest = onFechar, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Color(0xFF07070A))) {
            if (tela == "jogo") {
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { o -> if (o.x < size.width / 2f) mover(-1) else mover(1) } }
                        .pointerInput(Unit) {
                            var soma = 0f
                            detectHorizontalDragGestures(onDragStart = { soma = 0f }) { _, d ->
                                soma += d
                                if (soma > 55f) { mover(1); soma = 0f } else if (soma < -55f) { mover(-1); soma = 0f }
                            }
                        },
                ) {
                    val dx = if (tremor > 0f) sin(tempo * 90f) * tremor * 10f else 0f
                    translate(dx, 0f) {
                        val larg = size.width * 0.9f
                        val x0 = (size.width - larg) / 2f
                        val wf = larg / FAIXAS
                        fun cx(f: Float) = x0 + wf * (f + 0.5f)
                        desenharPista(rolagem, c.ouro, x0, larg, wf)
                        coisas.forEach { k -> desenharCoisa(k, cx(k.faixa.toFloat()), k.y * size.height, wf, c.ouro, tempo) }
                        // partículas e textos em fração da tela
                        particulas.forEach { p -> drawCircle(p.cor.copy(alpha = (p.vida * 1.4f).coerceIn(0f, 1f)), p.tam, Offset(p.x * size.width, p.y * size.height)) }
                        desenharMoto(cx(faixaVisual), size.height * Y_MOTO, wf, corMoto, brilhoMoto, capacete, (faixa - faixaVisual) * 18f, escudo > 0, tempo)
                        flutuas.forEach { f ->
                            val x = f.x * size.width
                            val r = medidor.measure(f.texto, TextStyle(color = f.cor.copy(alpha = f.vida.coerceIn(0f, 1f)), fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = ArchivoLargo))
                            drawText(r, topLeft = Offset(x - r.size.width / 2f, f.y * size.height))
                        }
                    }
                }
                // placar
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("$pontos", color = Color.White, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 34.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Selo(PwIcones.Package, "$pacotes", c.ouro)
                            Selo(PwIcones.Coins, "$moedasPartida", Color(0xFFFACC15))
                            if (escudo > 0) Selo(PwIcones.Shield, "$escudo", Color(0xFF38BDF8))
                            if (tempo < imaAte) Selo(PwIcones.Magnet, "${(imaAte - tempo).toInt()}s", Color(0xFFF472B6))
                            if (tempo < turboAte) Selo(PwIcones.Zap, "x2", Color(0xFF22C55E))
                        }
                    }
                    if (estado == "rodando") PwBotaoIcone(PwIcones.Pause, "Pausar", { estado = "pausado" })
                    Spacer(Modifier.width(8.dp))
                    PwBotaoIcone(PwIcones.X, "Sair do jogo", { if (estado == "rodando") terminar(); tela = "menu" })
                }
                if (estado == "pausado" || estado == "fim") {
                    Painel(Modifier.align(Alignment.Center)) {
                        Text(if (estado == "fim") (if (recordeNovo) "Recorde novo!" else "Bateu!") else "Pausado", color = c.ouro, fontFamily = ArchivoLargo,
                            fontWeight = FontWeight.Black, fontSize = 28.sp, textAlign = TextAlign.Center)
                        if (estado == "fim") {
                            Text("$pontos pontos · $pacotes pacotes · ${distancia.toInt()} m", color = Color(0xFFD4D4D8), fontSize = 14.sp, textAlign = TextAlign.Center)
                            resultado?.let { Text(it, color = Color(0xFFFACC15), fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 16.sp) }
                        }
                        Spacer(Modifier.height(4.dp))
                        PwBotaoOuro(if (estado == "pausado") "Continuar" else "Jogar de novo", onClick = { if (estado == "pausado") estado = "rodando" else reiniciar() },
                            icone = PwIcones.Gamepad2, modifier = Modifier.width(230.dp))
                        PwBotaoVidro("Menu do jogo", onClick = { if (estado == "pausado") terminar(); tela = "menu" }, modifier = Modifier.width(230.dp))
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (tela != "menu") { PwBotaoIcone(PwIcones.ChevronLeft, "Voltar", { tela = "menu" }); Spacer(Modifier.width(10.dp)) }
                        Text(when (tela) { "loja" -> "Loja"; "ranking" -> "Ranking"; "conquistas" -> "Conquistas"; else -> "Corre, motoboy!" },
                            color = c.ouro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 24.sp, modifier = Modifier.weight(1f))
                        Selo(PwIcones.Coins, "${perfil?.moedas ?: 0}", Color(0xFFFACC15))
                        Spacer(Modifier.width(10.dp))
                        PwBotaoIcone(PwIcones.X, "Fechar o jogo", onFechar)
                    }
                    when (tela) {
                        "loja" -> Loja(perfil)
                        "ranking" -> Ranking(perfil)
                        "conquistas" -> Conquistas(perfil, recorde(ctx))
                        else -> Menu(perfil, poderesEscolhidos, onJogar = { reiniciar() }, onTela = { tela = it }, corMoto = corMoto, capacete = capacete)
                    }
                }
            }
            Text("Chegou corrida? O jogo fecha e a oferta aparece.", color = Color(0x99A1A1AA), fontSize = 11.sp,
                modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 8.dp))
        }
    }
}

// ------------------------------------------------------------------ telas do jogo

@Composable
private fun Painel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.padding(24.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xF0141418)).border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(26.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

@Composable
private fun Selo(icone: ImageVector, texto: String, cor: Color) {
    Row(Modifier.clip(RoundedCornerShape(99.dp)).background(cor.copy(alpha = 0.16f)).padding(horizontal = 9.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
        Text(texto, color = cor, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 13.sp)
    }
}

@Composable
private fun Menu(perfil: PerfilJogo?, escolhidos: MutableList<String>, onJogar: () -> Unit, onTela: (String) -> Unit, corMoto: Color, capacete: ItemJogo?) {
    val c = Pw.cores
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // a moto equipada, girando devagar
        val giro = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(Unit) { var u = 0L; while (true) { val t = withFrameNanos { it }; if (u != 0L) giro.floatValue += (t - u) / 1e9f; u = t } }
        Canvas(Modifier.size(180.dp)) {
            drawCircle(Brush.radialGradient(listOf(corMoto.copy(alpha = 0.35f), Color.Transparent)), size.minDimension / 2f)
            desenharMoto(size.width / 2, size.height / 2 + 10f, size.width * 0.62f, corMoto, true, capacete, sin(giro.floatValue * 1.4f) * 10f, false, giro.floatValue)
        }
        perfil?.let { p ->
            Text("Level ${p.level}", color = Color.White, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 20.sp)
            val fr = ((p.xp - p.xpDoLevel).toFloat() / max(1, p.proximoXp - p.xpDoLevel)).coerceIn(0f, 1f)
            Box(Modifier.widthIn(max = 320.dp).fillMaxWidth().height(10.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x22FFFFFF))) {
                Box(Modifier.fillMaxWidth(fr).height(10.dp).clip(RoundedCornerShape(99.dp)).background(Brush.horizontalGradient(listOf(c.ouro, Color(0xFFFDE68A)))))
            }
            Text("Recorde ${p.recorde} · ${p.partidas} partidas", color = Color(0xFFA1A1AA), fontSize = 13.sp)
            // poderes guardados: escolha quais usar nesta partida
            val poderes = p.loja.filter { it.poder && p.quantos(it.chave) > 0 }
            if (poderes.isNotEmpty()) {
                Text("Usar nesta partida", color = Color(0xFFD4D4D8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    poderes.forEach { it2 ->
                        val on = it2.chave in escolhidos
                        Row(Modifier.clip(RoundedCornerShape(99.dp)).background(if (on) c.ouro.copy(alpha = 0.25f) else Color(0x14FFFFFF))
                            .border(1.dp, if (on) c.ouro else Color(0x22FFFFFF), RoundedCornerShape(99.dp))
                            .clickable { if (on) escolhidos.remove(it2.chave) else escolhidos.add(it2.chave) }.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(iconePoder(it2.chave), null, tint = if (on) c.ouro else Color(0xFFD4D4D8), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("${it2.nome.substringBefore(" (")} ×${p.quantos(it2.chave)}", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        PwBotaoOuro("Jogar", onClick = onJogar, icone = PwIcones.Gamepad2, modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth(), altura = 60.dp)
        Row(Modifier.widthIn(max = 320.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PwBotaoVidro("Loja", onClick = { onTela("loja") }, icone = PwIcones.ShoppingBag, modifier = Modifier.weight(1f))
            if (perfil?.ranking != false) PwBotaoVidro("Ranking", onClick = { onTela("ranking") }, icone = PwIcones.Trophy, modifier = Modifier.weight(1f))
        }
        PwBotaoVidro("Conquistas", onClick = { onTela("conquistas") }, icone = PwIcones.Medal, modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth())
        Text("Na contramão (esquerda) o trânsito vem rápido; na mão (direita), os carros andam devagar à sua frente.", color = Color(0xFF8B8B93), fontSize = 12.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(30.dp))
    }
}

private fun iconePoder(chave: String) = when (chave) { "poder_escudo" -> PwIcones.Shield; "poder_ima" -> PwIcones.Magnet; else -> PwIcones.Zap }

@Composable
private fun Loja(perfil: PerfilJogo?) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var aviso by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf<String?>(null) }
    val p = perfil ?: return
    val grupos = listOf("moto" to "Motos", "capacete" to "Capacetes", "rastro" to "Rastros", "buzina" to "Buzinas", "poder" to "Poderes (uma partida)")
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        aviso?.let { Text(it, color = Color(0xFFFCA5A5), fontSize = 13.sp) }
        grupos.forEach { (tipo, titulo) ->
            val itens = p.loja.filter { it.tipo == tipo }
            if (itens.isEmpty()) return@forEach
            Text(titulo, color = Color(0xFFD4D4D8), fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp))
            itens.forEach { i ->
                val tem = p.tem(i.chave)
                val equipado = p.equipado[i.tipo] == i.chave
                val bloqueado = p.level < i.nivel
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0x0FFFFFFF))
                        .border(1.dp, if (equipado) c.ouro else Color(0x1AFFFFFF), RoundedCornerShape(18.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(i.cor ?: Color(0x22FFFFFF)), contentAlignment = Alignment.Center) {
                        if (i.poder) Icon(iconePoder(i.chave), null, tint = Color.White, modifier = Modifier.size(20.dp))
                        else if (i.arco) Box(Modifier.size(42.dp).background(Brush.sweepGradient(listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red))))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(i.nome, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(when {
                            equipado -> "Em uso"
                            i.poder && tem -> "Você tem ${p.quantos(i.chave)}"
                            tem -> "Seu"
                            bloqueado -> "Libera no level ${i.nivel}"
                            i.preco == 0 -> "Grátis"
                            else -> "${i.preco} moedas"
                        }, color = if (bloqueado) Color(0xFF8B8B93) else Color(0xFFFACC15), fontSize = 12.5.sp)
                    }
                    when {
                        equipado -> Icon(PwIcones.Check, null, tint = c.ouro)
                        tem && !i.poder -> PwBotaoVidro("Usar", onClick = {
                            ocupado = i.chave; escopo.launch { JogoServidor.equipar(i.chave).onFailure { aviso = it.message }; ocupado = null }
                        }, habilitado = ocupado == null)
                        bloqueado -> Icon(PwIcones.Lock, null, tint = Color(0xFF8B8B93))
                        else -> PwBotaoOuro(if (ocupado == i.chave) "…" else "Comprar", onClick = {
                            ocupado = i.chave; aviso = null
                            escopo.launch { JogoServidor.comprar(i.chave).onFailure { aviso = it.message }; ocupado = null }
                        }, habilitado = ocupado == null && p.moedas >= i.preco)
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun Ranking(perfil: PerfilJogo?) {
    val c = Pw.cores
    var escopo by remember { mutableStateOf("empresa") }
    var periodo by remember { mutableStateOf("mes") }
    var lista by remember { mutableStateOf<List<LinhaRanking>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(escopo, periodo) { lista = null; JogoServidor.ranking(escopo, periodo).onSuccess { lista = it; erro = null }.onFailure { erro = it.message } }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Escolha("Da empresa", escopo == "empresa") { escopo = "empresa" }
            Escolha("Geral", escopo == "global") { escopo = "global" }
            Spacer(Modifier.width(6.dp))
            Escolha("Mês", periodo == "mes") { periodo = "mes" }
            Escolha("Sempre", periodo == "sempre") { periodo = "sempre" }
        }
        erro?.let { Text(it, color = Color(0xFFFCA5A5)) }
        val l = lista
        if (l == null && erro == null) Text("Carregando…", color = Color(0xFF8B8B93))
        if (l != null && l.isEmpty()) Text("Ninguém jogou ainda neste período. Seja o primeiro!", color = Color(0xFF8B8B93))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            l?.forEachIndexed { i, r ->
                val corPos = when (i) { 0 -> Color(0xFFFDE68A); 1 -> Color(0xFFD4D4D8); 2 -> Color(0xFFD97706); else -> Color(0xFF8B8B93) }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (r.eu) c.ouro.copy(alpha = 0.14f) else Color(0x0FFFFFFF))
                        .border(1.dp, if (r.eu) c.ouro else Color(0x14FFFFFF), RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${i + 1}", color = corPos, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.width(30.dp))
                    // capacete equipado
                    Box(Modifier.size(34.dp).clip(CircleShape).background(perfil?.item(r.capacete)?.cor ?: Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(Color(0xCC0B0B0D)))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.nome + if (r.eu) " (você)" else "", color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text("Level ${r.level}", color = Color(0xFFA1A1AA), fontSize = 12.sp)
                    }
                    Text("${r.pontos}", color = c.ouro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun Escolha(texto: String, ligada: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    Text(texto, color = if (ligada) Color(0xFF0A0A0A) else Color(0xFFD4D4D8), fontWeight = FontWeight.Bold, fontSize = 13.sp,
        modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(if (ligada) c.ouro else Color(0x14FFFFFF)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}

@Composable
private fun Conquistas(perfil: PerfilJogo?, recordeLocal: Int) {
    val c = Pw.cores
    val p = perfil ?: PerfilJogo()
    val rec = max(p.recorde, recordeLocal)
    val skins = p.itens.toSet().count { !it.startsWith("poder_") }
    val lista = listOf(
        Triple("Primeira volta", "Jogue uma partida", p.partidas to 1),
        Triple("Viciado em entrega", "Jogue 25 partidas", p.partidas to 25),
        Triple("Lenda da avenida", "Jogue 100 partidas", p.partidas to 100),
        Triple("Esquentando", "Faça 500 pontos", rec to 500),
        Triple("Piloto", "Faça 1.500 pontos", rec to 1500),
        Triple("Imparável", "Faça 4.000 pontos", rec to 4000),
        Triple("Subindo de nível", "Chegue ao level 5", p.level to 5),
        Triple("Veterano", "Chegue ao level 10", p.level to 10),
        Triple("Estiloso", "Tenha 6 itens de visual", skins to 6),
        Triple("Colecionador", "Tenha 12 itens de visual", skins to 12),
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${lista.count { it.third.first >= it.third.second }} de ${lista.size}", color = c.ouro, fontWeight = FontWeight.Bold, fontFamily = Archivo)
        lista.forEach { (nome, dica, prog) ->
            val feita = prog.first >= prog.second
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (feita) c.ouro.copy(alpha = 0.12f) else Color(0x0AFFFFFF))
                    .border(1.dp, if (feita) c.ouro.copy(alpha = 0.6f) else Color(0x14FFFFFF), RoundedCornerShape(16.dp)).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(if (feita) c.ouro else Color(0x14FFFFFF)), contentAlignment = Alignment.Center) {
                    Icon(if (feita) PwIcones.Trophy else PwIcones.Lock, null, tint = if (feita) Color(0xFF0A0A0A) else Color(0xFF8B8B93), modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(nome, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(dica, color = Color(0xFFA1A1AA), fontSize = 12.5.sp)
                    if (!feita) {
                        Spacer(Modifier.height(5.dp))
                        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)).background(Color(0x22FFFFFF))) {
                            Box(Modifier.fillMaxWidth((prog.first.toFloat() / prog.second).coerceIn(0f, 1f)).height(5.dp).clip(RoundedCornerShape(99.dp)).background(c.ouro))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

// ------------------------------------------------------------------ desenho

private fun DrawScope.desenharPista(rolagem: Float, ouro: Color, x0: Float, larg: Float, wf: Float) {
    drawRect(Color(0xFF0D0D11))
    // calçadas com prédios passando
    val passo = size.height / 6f
    var y = -passo + (rolagem * passo * 6f) % passo
    while (y < size.height) {
        drawRect(Color(0xFF15151B), Offset(0f, y), Size(x0 - 4f, passo * 0.8f))
        drawRect(Color(0xFF15151B), Offset(x0 + larg + 4f, y), Size(size.width - x0 - larg - 4f, passo * 0.8f))
        drawCircle(ouro.copy(alpha = 0.45f), 4f, Offset(x0 / 2f, y + passo * 0.4f))
        drawCircle(ouro.copy(alpha = 0.45f), 4f, Offset(size.width - x0 / 2f, y + passo * 0.4f))
        y += passo
    }
    // asfalto: contramão um pouco mais escura
    drawRect(Brush.verticalGradient(listOf(Color(0xFF19191F), Color(0xFF141419))), Offset(x0, 0f), Size(wf * 2, size.height))
    drawRect(Brush.verticalGradient(listOf(Color(0xFF1E1E25), Color(0xFF17171C))), Offset(x0 + wf * 2, 0f), Size(wf * 2, size.height))
    drawRect(Color(0xCCFFFFFF), Offset(x0 - 3f, 0f), Size(3f, size.height))
    drawRect(Color(0xCCFFFFFF), Offset(x0 + larg, 0f), Size(3f, size.height))
    // faixa dupla amarela no meio
    drawRect(Color(0xFFFACC15), Offset(x0 + wf * 2 - 7f, 0f), Size(4f, size.height))
    drawRect(Color(0xFFFACC15), Offset(x0 + wf * 2 + 3f, 0f), Size(4f, size.height))
    // tracejadas que descem
    val p2 = size.height / 8f
    for (i in listOf(1, 3)) {
        val x = x0 + wf * i - 2.5f
        var yy = -p2 + (rolagem * p2 * 8f) % p2
        while (yy < size.height) { drawRoundRect(Color(0x77FFFFFF), Offset(x, yy), Size(5f, p2 * 0.5f), CornerRadius(3f)); yy += p2 }
    }
}

private fun DrawScope.carro(cx: Float, cy: Float, w: Float, h: Float, cor: Color, deFrente: Boolean) {
    drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(cx - w / 2 + 6, cy - h / 2 + 10), Size(w, h), CornerRadius(w * 0.28f))
    drawRoundRect(cor, Offset(cx - w / 2, cy - h / 2), Size(w, h), CornerRadius(w * 0.28f))
    drawRoundRect(Color(0xCC0B0B0D), Offset(cx - w * 0.36f, cy - h * 0.18f), Size(w * 0.72f, h * 0.22f), CornerRadius(8f))
    drawRoundRect(Color(0xCC0B0B0D), Offset(cx - w * 0.34f, cy + h * 0.18f), Size(w * 0.68f, h * 0.14f), CornerRadius(8f))
    if (deFrente) {
        // faróis acesos vindo na sua direção
        drawOval(Brush.radialGradient(listOf(Color(0x66FDE68A), Color.Transparent), Offset(cx, cy + h / 2 + h * 0.3f), h * 0.6f), Offset(cx - w, cy + h / 2 - 4), Size(w * 2, h * 0.7f))
        drawCircle(Color(0xFFFDE68A), w * 0.08f, Offset(cx - w * 0.3f, cy + h / 2 - 6))
        drawCircle(Color(0xFFFDE68A), w * 0.08f, Offset(cx + w * 0.3f, cy + h / 2 - 6))
    } else {
        // lanternas vermelhas (anda à sua frente)
        drawRoundRect(Color(0xFFEF4444), Offset(cx - w * 0.42f, cy + h / 2 - 8), Size(w * 0.2f, 6f), CornerRadius(3f))
        drawRoundRect(Color(0xFFEF4444), Offset(cx + w * 0.22f, cy + h / 2 - 8), Size(w * 0.2f, 6f), CornerRadius(3f))
    }
}

private fun DrawScope.desenharCoisa(k: Coisa, cx: Float, cy: Float, wf: Float, ouro: Color, tempo: Float) {
    val pulso = 1f + sin(tempo * 6f + k.y * 10f) * 0.08f
    when (k.tipo) {
        Tipo.CARRO_FRENTE -> carro(cx, cy, wf * 0.6f, wf * 1.05f, k.cor, true)
        Tipo.CARRO_MESMO -> carro(cx, cy, wf * 0.6f, wf * 1.05f, k.cor, false)
        Tipo.CAMINHAO -> {
            val w = wf * 0.72f; val h = wf * 1.9f
            drawRoundRect(Color.Black.copy(alpha = 0.35f), Offset(cx - w / 2 + 6, cy - h / 2 + 10), Size(w, h), CornerRadius(10f))
            drawRoundRect(Color(0xFFE5E7EB), Offset(cx - w / 2, cy - h / 2), Size(w, h * 0.74f), CornerRadius(10f))
            drawRoundRect(k.cor, Offset(cx - w / 2, cy + h * 0.26f), Size(w, h * 0.24f), CornerRadius(10f))
            drawRoundRect(Color(0xFFEF4444), Offset(cx - w * 0.42f, cy + h / 2 - 8), Size(w * 0.18f, 6f), CornerRadius(3f))
            drawRoundRect(Color(0xFFEF4444), Offset(cx + w * 0.24f, cy + h / 2 - 8), Size(w * 0.18f, 6f), CornerRadius(3f))
        }
        Tipo.MOTO_FRENTE -> {
            val w = wf * 0.24f; val h = w * 2.4f
            drawOval(Brush.radialGradient(listOf(Color(0x55FDE68A), Color.Transparent), Offset(cx, cy + h), h * 0.8f), Offset(cx - h * 0.6f, cy + h * 0.2f), Size(h * 1.2f, h * 1.2f))
            drawRoundRect(Color(0xFF27272A), Offset(cx - w * 0.3f, cy - h / 2), Size(w * 0.6f, h), CornerRadius(w * 0.3f))
            drawRoundRect(k.cor, Offset(cx - w / 2, cy - h * 0.25f), Size(w, h * 0.5f), CornerRadius(w * 0.4f))
            drawCircle(Color(0xFF111827), w * 0.32f, Offset(cx, cy))
            drawCircle(Color(0xFFFDE68A), w * 0.14f, Offset(cx, cy + h / 2 - 4))
        }
        Tipo.CONE -> {
            val w = wf * 0.28f
            val p = Path().apply { moveTo(cx, cy - w); lineTo(cx + w * 0.55f, cy + w * 0.6f); lineTo(cx - w * 0.55f, cy + w * 0.6f); close() }
            drawPath(p, Color(0xFFF97316))
            drawRect(Color.White, Offset(cx - w * 0.3f, cy - w * 0.05f), Size(w * 0.6f, w * 0.18f))
            drawRoundRect(Color(0xFFF97316), Offset(cx - w * 0.75f, cy + w * 0.55f), Size(w * 1.5f, w * 0.22f), CornerRadius(4f))
        }
        Tipo.BURACO -> {
            val w = wf * 0.5f
            drawOval(Color(0xFF050506), Offset(cx - w / 2, cy - w * 0.28f), Size(w, w * 0.56f))
            drawOval(Color(0x553F3F46), Offset(cx - w / 2, cy - w * 0.28f), Size(w, w * 0.56f), style = Stroke(4f))
        }
        Tipo.OLEO -> {
            val w = wf * 0.55f
            drawOval(Brush.radialGradient(listOf(Color(0xFF312E81), Color(0xFF0F172A), Color(0x000F172A)), Offset(cx, cy), w * 0.6f), Offset(cx - w / 2, cy - w * 0.3f), Size(w, w * 0.6f))
        }
        Tipo.PACOTE, Tipo.CAIXA, Tipo.PACOTE_OURO -> {
            val base = when (k.tipo) { Tipo.CAIXA -> 0.42f; Tipo.PACOTE_OURO -> 0.36f; else -> 0.3f }
            val w = wf * base * pulso
            val cores = when (k.tipo) { Tipo.CAIXA -> listOf(Color(0xFFD6A06A), Color(0xFF92582A)); Tipo.PACOTE_OURO -> listOf(Color(0xFFFFF7CC), Color(0xFFFACC15)); else -> listOf(Color(0xFFF3D27A), ouro) }
            drawCircle(cores[1].copy(alpha = if (k.tipo == Tipo.PACOTE_OURO) 0.35f else 0.18f), w, Offset(cx, cy))
            drawRoundRect(Brush.linearGradient(cores), Offset(cx - w / 2, cy - w / 2), Size(w, w), CornerRadius(6f))
            drawRect(Color(0x99000000), Offset(cx - 2f, cy - w / 2), Size(4f, w))
            drawRect(Color(0x99000000), Offset(cx - w / 2, cy - 2f), Size(w, 4f))
        }
        Tipo.MOEDA -> {
            val r = wf * 0.13f
            val achata = abs(sin(tempo * 5f + k.y * 8f)).coerceAtLeast(0.15f)
            drawOval(Color(0xFFFACC15), Offset(cx - r * achata, cy - r), Size(r * 2 * achata, r * 2))
            drawOval(Color(0xFFCA8A04), Offset(cx - r * achata, cy - r), Size(r * 2 * achata, r * 2), style = Stroke(3f))
        }
        Tipo.ESCUDO, Tipo.IMA, Tipo.TURBO -> {
            val cor = when (k.tipo) { Tipo.ESCUDO -> Color(0xFF38BDF8); Tipo.IMA -> Color(0xFFF472B6); else -> Color(0xFF22C55E) }
            val r = wf * 0.2f * pulso
            drawCircle(cor.copy(alpha = 0.25f), r * 1.5f, Offset(cx, cy))
            drawCircle(cor, r, Offset(cx, cy), style = Stroke(5f))
            drawCircle(cor.copy(alpha = 0.6f), r * 0.45f, Offset(cx, cy))
        }
    }
}

private fun DrawScope.desenharMoto(cx: Float, cy: Float, wf: Float, cor: Color, brilho: Boolean, capacete: ItemJogo?, inclinar: Float, escudo: Boolean, tempo: Float) {
    val w = wf * 0.3f; val h = w * 2.6f
    drawOval(Brush.radialGradient(listOf(cor.copy(alpha = 0.3f), Color.Transparent), Offset(cx, cy - h), h * 0.9f), Offset(cx - h * 0.7f, cy - h * 1.9f), Size(h * 1.4f, h * 1.4f))
    if (brilho) drawCircle(cor.copy(alpha = 0.18f + sin(tempo * 4f) * 0.06f), h * 0.75f, Offset(cx, cy))
    rotate(inclinar, Offset(cx, cy)) {
        drawRoundRect(Color.Black.copy(alpha = 0.4f), Offset(cx - w / 2 + 5, cy - h / 2 + 8), Size(w, h), CornerRadius(w / 2))
        drawRoundRect(Color(0xFF27272A), Offset(cx - w * 0.28f, cy - h / 2 - 4), Size(w * 0.56f, h * 0.26f), CornerRadius(w * 0.2f))
        drawRoundRect(Color(0xFF27272A), Offset(cx - w * 0.3f, cy + h * 0.26f), Size(w * 0.6f, h * 0.28f), CornerRadius(w * 0.2f))
        drawRoundRect(Brush.verticalGradient(listOf(cor.copy(alpha = 0.85f).compositeOver(Color.White), cor)), Offset(cx - w / 2, cy - h * 0.3f), Size(w, h * 0.6f), CornerRadius(w * 0.35f))
        drawRoundRect(Color(0xFF0B0B0D), Offset(cx - w * 0.62f, cy + h * 0.08f), Size(w * 1.24f, h * 0.3f), CornerRadius(6f))
        drawRoundRect(cor, Offset(cx - w * 0.62f, cy + h * 0.08f), Size(w * 1.24f, h * 0.3f), CornerRadius(6f), style = Stroke(3f))
        // capacete (item da loja)
        val corCap = capacete?.cor ?: Color.White
        drawCircle(corCap, w * 0.33f, Offset(cx, cy - h * 0.08f))
        when (capacete?.desenho) {
            "raio" -> drawPath(Path().apply { moveTo(cx - w * 0.05f, cy - h * 0.2f); lineTo(cx + w * 0.12f, cy - h * 0.1f); lineTo(cx, cy - h * 0.08f); lineTo(cx + w * 0.08f, cy + h * 0.02f); lineTo(cx - w * 0.12f, cy - h * 0.1f); lineTo(cx, cy - h * 0.12f); close() }, Color(0xFF0B0B0D))
            "chamas" -> repeat(3) { i -> drawCircle(Color(0xFFDC2626), w * 0.08f, Offset(cx - w * 0.16f + i * w * 0.16f, cy - h * 0.0f)) }
            "estrelas" -> repeat(4) { i -> drawCircle(Color.White, 2.5f, Offset(cx - w * 0.2f + (i % 2) * w * 0.4f, cy - h * 0.16f + (i / 2) * h * 0.12f)) }
        }
        drawCircle(Color(0xFF0B0B0D), w * 0.17f, Offset(cx, cy - h * 0.15f))
        drawRoundRect(Color(0xFFD4D4D8), Offset(cx - w * 0.75f, cy - h * 0.3f), Size(w * 1.5f, 5f), CornerRadius(3f))
    }
    if (escudo) drawCircle(Color(0xFF38BDF8).copy(alpha = 0.35f + sin(tempo * 8f) * 0.1f), h * 0.62f, Offset(cx, cy), style = Stroke(5f))
}

private fun Color.compositeOver(fundo: Color): Color {
    val a = alpha
    return Color(red * a + fundo.red * (1 - a), green * a + fundo.green * (1 - a), blue * a + fundo.blue * (1 - a), 1f)
}
