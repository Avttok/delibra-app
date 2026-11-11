package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private val MOTIVOS = listOf("Pneu furou", "Moto quebrou", "Acidente", "Problema de saúde", "Emergência em casa", "Não acho o endereço")

/**
 * Em corrida: pedir ao gestor para cancelar. Mostra o pedido aberto (esperando, negado) e deixa retirar.
 * Some se o gestor desligou (Ajustes → Pedido de cancelamento).
 */
@Composable
internal fun PedirCancelamentoCorrida(corridaId: String) {
    val c = Pw.cores
    val perfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    if (perfil?.logico("chamado_motoboy_pode", true) == false) return
    val escopo = rememberCoroutineScope()
    var chamado by remember(corridaId) { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableStateOf(0) }
    var abrir by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    LaunchedEffect(corridaId, versao) {
        while (true) {
            Corridas.rpc("meu_chamado", buildJsonObject { put("p_corrida", corridaId) }).onSuccess { chamado = it as? JsonObject }
            delay(if (chamado?.str("status") == "pendente") 10_000 else 60_000)
        }
    }
    val st = chamado?.str("status")
    when (st) {
        "pendente" -> PwCartao(cor = c.ouro.copy(alpha = 0.55f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Hand, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                PwRotulo("Pedido de cancelamento enviado", Modifier.weight(1f))
            }
            Text("Esperando o gestor responder. Até lá, a rota continua com você.", color = c.texto2, fontSize = 13.5.sp)
            Text("“${chamado?.str("motivo").orEmpty()}”", color = c.texto, fontFamily = Archivo, fontSize = 14.sp)
            PwBotaoVidro(if (ocupado) "Retirando…" else "Retirar o pedido", onClick = {
                ocupado = true
                escopo.launch {
                    Corridas.rpc("retirar_chamado", buildJsonObject { put("p_chamado", chamado?.str("id").orEmpty()) })
                        .onSuccess { PwToast.mostrar("Pedido retirado: você segue com a rota."); versao++ }.onFailure { erro = it.message }
                    ocupado = false
                }
            }, icone = PwIcones.RotateCcw, habilitado = !ocupado, modifier = Modifier.fillMaxWidth())
            PwErro(erro)
        }
        "negado" -> PwCartao(cor = c.vermelho.copy(alpha = 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.CircleX, null, tint = c.vermelho, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                PwRotulo("O gestor negou o cancelamento", Modifier.weight(1f))
            }
            Text(chamado?.str("resposta") ?: "Siga com a rota como combinado.", color = c.texto, fontSize = 14.sp)
            Text("Largar a rota agora conta como abandono (penalidade).", color = c.texto3, fontSize = 12.5.sp)
        }
        else -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            PwBotaoVidro("Pedir para cancelar esta rota", onClick = { abrir = true }, icone = PwIcones.Hand, cor = c.texto3)
        }
    }
    if (abrir) {
        var motivo by remember { mutableStateOf("") }
        PwJanela("Pedir para cancelar", onFechar = { abrir = false }) {
            Text("O gestor recebe na hora. Se ele aprovar, a rota sai de você e volta para o sistema. Se negar, você segue com ela.", color = c.texto2, fontSize = 14.sp)
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MOTIVOS.forEach { m -> PwPilula(m, if (motivo == m) PwIcones.Check else PwIcones.MessageCircle, { motivo = m }, ligada = motivo == m) }
            }
            PwCampo(motivo, { motivo = it.take(300) }, "Motivo", dica = "Conte o que aconteceu", linhas = 2)
            PwErro(erro)
            PwBotaoPerigo(if (ocupado) "Enviando…" else "Enviar ao gestor", onClick = {
                ocupado = true
                escopo.launch {
                    Corridas.rpc("pedir_cancelamento_corrida", buildJsonObject { put("p_corrida", corridaId); put("p_motivo", motivo.trim()) })
                        .onSuccess { abrir = false; erro = null; PwToast.mostrar("Enviado. Avisamos quando o gestor responder."); versao++ }
                        .onFailure { erro = it.message }
                    ocupado = false
                }
            }, icone = PwIcones.Send, habilitado = motivo.isNotBlank() && !ocupado, cheio = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Suspenso por penalidades: aviso claro de até quando (some sozinho quando acaba). */
@Composable
fun AvisoSuspensao(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val perfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    val ate = perfil?.suspensoAte ?: return
    val ms = runCatching { java.time.OffsetDateTime.parse(ate).toInstant().toEpochMilli() }.getOrNull() ?: return
    if (ms < System.currentTimeMillis()) return
    val quando = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm"))
    PwCartao(modifier, cor = c.vermelho.copy(alpha = 0.6f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Gavel, null, tint = c.vermelho, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Suspenso até $quando", color = c.vermelho, fontWeight = FontWeight.Black, fontFamily = Archivo, fontSize = 16.sp)
                Text(perfil?.suspensoMotivo ?: "Passou do limite de penalidades.", color = c.texto2, fontSize = 13.sp)
            }
        }
        Text("Enquanto isso, nenhuma rota nova chega até você. Fale com o gestor se achar que foi engano.", color = c.texto3, fontSize = 12.5.sp)
    }
}
