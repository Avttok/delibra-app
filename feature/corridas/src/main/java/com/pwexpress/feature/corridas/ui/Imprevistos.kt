package com.pwexpress.feature.corridas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.json.str
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.Parada
import com.pwexpress.feature.corridas.data.PosicaoAtual
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val MOTIVOS_PULAR = listOf("Portão fechado", "Endereço não existe", "Sem acesso de moto", "Cliente pediu outro lugar", "GPS errado aqui", "Condomínio não deixa entrar")

// ------------------------------------------------------------------ pular parada (fora do raio)

/** Longe do endereço: pede ao gestor para liberar esta parada. Mostra o pedido aberto e a resposta. */
@Composable
internal fun PedirPularParada(corridaId: String, parada: Parada) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var chamado by remember(corridaId) { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableStateOf(0) }
    var abrir by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    LaunchedEffect(corridaId, parada.id, versao) {
        while (true) {
            Corridas.rpc("meu_pedido_pular", buildJsonObject { put("p_corrida", corridaId) }).onSuccess { chamado = it as? JsonObject }
            // aprovado: a parada vem liberada na próxima sincronização
            if (chamado?.str("status") == "aprovado" && chamado?.str("parada_ordem") == parada.ordem.toString()) Corridas.sincronizar()
            delay(if (chamado?.str("status") == "pendente") 8_000 else 60_000)
        }
    }
    val desta = chamado?.takeIf { it.str("parada_ordem") == parada.ordem.toString() }
    when (desta?.str("status")) {
        "pendente" -> PwCartao(cor = c.ouro.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.LockOpen, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                PwRotulo("Pedido de liberação enviado", Modifier.weight(1f))
            }
            Text("Esperando o gestor. Se ele liberar, o botão acende e você confirma daqui.", color = c.texto2, fontSize = 13.5.sp)
            Text("“${desta.str("motivo").orEmpty()}”", color = c.texto, fontFamily = Archivo, fontSize = 14.sp)
        }
        "negado" -> PwCartao(cor = c.vermelho.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.CircleX, null, tint = c.vermelho, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                PwRotulo("O gestor não liberou", Modifier.weight(1f))
            }
            Text(desta.str("resposta") ?: "Chegue ao endereço para confirmar.", color = c.texto, fontSize = 14.sp)
            PwBotaoVidro("Pedir de novo", onClick = { abrir = true }, icone = PwIcones.RotateCcw, modifier = Modifier.fillMaxWidth())
        }
        "aprovado" -> {}
        else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PwBotaoVidro("Não consigo chegar: pedir para liberar", onClick = { abrir = true }, icone = PwIcones.LockOpen, cor = c.texto2)
        }
    }
    if (abrir) {
        var motivo by remember { mutableStateOf("") }
        PwJanela("Liberar a parada ${parada.ordem + 1}", onFechar = { abrir = false }) {
            Text("O gestor recebe na hora. Se ele liberar, você confirma a etapa de onde está, sem precisar estar no raio do endereço.", color = c.texto2, fontSize = 14.sp)
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MOTIVOS_PULAR.forEach { m -> PwPilula(m, if (motivo == m) PwIcones.Check else PwIcones.MapPinned, { motivo = m }, ligada = motivo == m) }
            }
            PwCampo(motivo, { motivo = it.take(300) }, "Motivo", dica = "O que está acontecendo aí", linhas = 2)
            PwErro(erro)
            PwBotaoOuro(if (ocupado) "Enviando…" else "Enviar ao gestor", onClick = {
                ocupado = true
                escopo.launch {
                    Corridas.rpc("pedir_pular_parada", buildJsonObject { put("p_parada", parada.id); put("p_motivo", motivo.trim()) })
                        .onSuccess { abrir = false; erro = null; PwToast.mostrar("Enviado. O botão acende quando o gestor liberar."); versao++ }
                        .onFailure { erro = it.message }
                    ocupado = false
                }
            }, icone = PwIcones.Send, habilitado = motivo.isNotBlank() && !ocupado, modifier = Modifier.fillMaxWidth())
        }
    }
}

// ------------------------------------------------------------------ sem internet

/** Etapas guardadas no celular esperando sinal. */
@Composable
internal fun AvisoFila(n: Int) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var tentando by remember { mutableStateOf(false) }
    PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.CloudOff, null, tint = c.ouro, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (n == 1) "1 etapa guardada no celular" else "$n etapas guardadas no celular", color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo)
                Text("Vão sozinhas, com a hora em que você fez, quando o sinal voltar.", color = c.texto2, fontSize = 13.sp)
            }
        }
        PwBotaoVidro(if (tentando) "Tentando…" else "Tentar agora", onClick = {
            tentando = true
            escopo.launch {
                if (Corridas.enviarFila()) { PwToast.mostrar("Tudo enviado."); Corridas.sincronizar() } else PwToast.mostrar("Ainda sem sinal. O app segue tentando.")
                tentando = false
            }
        }, icone = PwIcones.RotateCcw, habilitado = !tentando, modifier = Modifier.fillMaxWidth())
    }
}

// ------------------------------------------------------------------ emergência

/**
 * Botão de emergência: segurar 2 segundos (o anel enche) e confirmar. Difícil de apertar sem querer.
 * O gestor escolhe quem recebe (Ajustes → Botão de emergência) e pode desligar.
 */
@Composable
fun BotaoEmergencia(modifier: Modifier = Modifier, tamanho: Dp = 44.dp) {
    val c = Pw.cores
    val perfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    if (perfil?.motoboy != true || perfil?.logico("emergencia_ligada", true) == false) return
    val progresso = remember { Animatable(0f) }
    val escopo = rememberCoroutineScope()
    var abrir by remember { mutableStateOf(false) }
    Box(
        modifier.size(tamanho).clip(CircleShape).background(c.vermelho.copy(alpha = 0.16f)).border(1.5.dp, c.vermelho.copy(alpha = 0.7f), CircleShape)
            .drawBehind {
                if (progresso.value > 0f) drawArc(c.vermelho, -90f, 360f * progresso.value, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    val enchendo = escopo.launch {
                        progresso.animateTo(1f, tween(2000, easing = LinearEasing))
                        abrir = true
                        progresso.snapTo(0f)
                    }
                    val soltou = tryAwaitRelease()
                    if (enchendo.isActive) {
                        enchendo.cancel()
                        escopo.launch { progresso.animateTo(0f, tween(250)) }
                        if (soltou) PwToast.mostrar("Segure o botão por 2 segundos para pedir ajuda.", titulo = "Emergência")
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) { Icon(PwIcones.Siren, "Emergência (segure)", tint = c.vermelho, modifier = Modifier.size(tamanho * 0.48f)) }
    if (abrir) JanelaEmergencia(onFechar = { abrir = false })
}

@Composable
private fun JanelaEmergencia(onFechar: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var enviando by remember { mutableStateOf(false) }
    var enviado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    fun discar(n: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$n"))) }
    PwJanela(if (enviado) "Alerta enviado" else "Precisa de ajuda?", onFechar = onFechar) {
        if (!enviado) {
            Text("O alerta vai na hora para a sua equipe, com a sua localização.", color = c.texto2, fontSize = 14.sp)
            PwErro(erro)
            PwBotaoPerigo(if (enviando) "Enviando…" else "Enviar alerta de emergência", onClick = {
                enviando = true
                escopo.launch {
                    val p = PosicaoAtual.obter(ctx, 6_000)
                    Corridas.rpc("acionar_emergencia", buildJsonObject {
                        p?.let { put("p_lat", it.lat); put("p_lng", it.lng); put("p_precisao", it.precisaoM) }
                    }).onSuccess { enviado = true; erro = null }.onFailure { erro = it.message + " Ligue para a sua equipe ou para os números abaixo." }
                    enviando = false
                }
            }, icone = PwIcones.Siren, habilitado = !enviando, cheio = true, modifier = Modifier.fillMaxWidth())
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.BadgeCheck, null, tint = c.verde, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(10.dp))
                Text("Sua equipe recebeu o alerta e vê onde você está. Se puder, fique num lugar seguro.", color = c.texto, fontSize = 14.sp)
            }
        }
        PwRotulo("Ligar agora", Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PwBotaoVidro("190", onClick = { discar("190") }, icone = PwIcones.Shield, modifier = Modifier.weight(1f))
            PwBotaoVidro("192", onClick = { discar("192") }, icone = PwIcones.Ambulance, modifier = Modifier.weight(1f))
            PwBotaoVidro("193", onClick = { discar("193") }, icone = PwIcones.PhoneCall, modifier = Modifier.weight(1f))
        }
        Text("190 Polícia · 192 SAMU · 193 Bombeiros", color = c.texto3, fontSize = 12.sp)
    }
}

// ------------------------------------------------------------------ minha nota

/** A nota que os clientes deram (média e quantas avaliações). Some enquanto não houver nenhuma. */
@Composable
fun MinhaNota(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val perfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    val id = perfil?.id ?: return
    var nota by remember(id) { mutableStateOf<JsonObject?>(null) }
    LaunchedEffect(id) { Corridas.rpc("nota_motoboy", buildJsonObject { put("p_motoboy", id) }).onSuccess { nota = it as? JsonObject } }
    val total = nota?.str("total")?.toIntOrNull() ?: 0
    if (total == 0) return
    val media = nota?.str("media")?.toDoubleOrNull() ?: return
    PwCartao(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Star, null, tint = c.ouro, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Sua nota: ${String.format(java.util.Locale("pt", "BR"), "%.1f", media)}", color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 16.sp)
                Text("$total avaliação${if (total == 1) "" else "ões"} dos clientes", color = c.texto2, fontSize = 13.sp)
            }
            Row { repeat(5) { i -> Icon(PwIcones.Star, null, tint = if (i < Math.round(media)) c.ouro else c.borda, modifier = Modifier.size(14.dp)) } }
        }
    }
}
