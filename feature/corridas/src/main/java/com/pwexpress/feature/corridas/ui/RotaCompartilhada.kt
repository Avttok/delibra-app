package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.str
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.paraCorrida
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Rota ou pedido que o gestor mandou no chat: o motoboy vê como no quadro (paradas, km, quanto recebe),
 * mesmo em revisão, e responde se pode fazer (vai como mensagem na conversa). Se a rota estiver livre, dá para pegar.
 */
@Composable
fun RotaCompartilhadaTela(tipo: String, id: String, onFechar: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var rotas by remember(id) { mutableStateOf<List<Pair<Corrida, JsonObject>>?>(null) }
    var pedido by remember(id) { mutableStateOf<JsonObject?>(null) }
    var erro by remember(id) { mutableStateOf<String?>(null) }
    var escolhida by remember(id) { mutableStateOf(0) }
    var ocupado by remember { mutableStateOf(false) }
    val conversa by com.pwexpress.core.chat.Chat.conversaAtual.collectAsState()
    LaunchedEffect(id) {
        if (tipo == "rota") Corridas.rotaCompartilhada(id).onSuccess { rotas = listOf(it) }.onFailure { erro = it.message }
        else Corridas.rpc("pedido_compartilhado", buildJsonObject { put("p_pedido", id) }).onSuccess { j ->
            val o = j as? JsonObject
            pedido = o
            rotas = o?.arr("rotas")?.mapNotNull { r -> runCatching { r.paraCorrida() to r }.getOrNull() }.orEmpty()
        }.onFailure { erro = it.message }
    }
    val lista = rotas
    if (lista == null || lista.isEmpty()) {
        PwTelaCheia(if (tipo == "rota") "Rota" else "Pedido ${pedido?.str("numero")?.let { "#$it" } ?: ""}".trim(), onFechar) {
            when {
                erro != null -> PwErro(erro)
                lista == null -> PwCarregando()
                else -> PwCartao {
                    Text("Este pedido ainda não virou rota. Ele tem ${pedido?.arr("pontos")?.size ?: 0} endereços.", color = c.texto2)
                    pedido?.arr("pontos")?.forEach { p -> Text((if (p.str("tipo") == "coleta") "Coleta · " else "Entrega · ") + (p.str("endereco") ?: ""), color = c.texto, fontSize = 13.5.sp) }
                }
            }
        }
        return
    }
    val (r, j) = lista[escolhida.coerceIn(0, lista.lastIndex)]
    val respostas = (pedido?.bool("respostas") ?: j.bool("respostas")) && conversa != null
    fun responder(pode: Boolean) {
        val cv = conversa ?: return
        ocupado = true
        escopo.launch {
            com.pwexpress.core.chat.Chat.enviar(cv, if (pode) "Posso fazer a rota ${r.numeroExibicao}." else "Agora não consigo a rota ${r.numeroExibicao}.", null)
                .onSuccess { PwToast.mostrar("Resposta enviada na conversa."); onFechar() }.onFailure { erro = it.message }
            ocupado = false
        }
    }
    DetalheRotaQuadro(
        r, System.currentTimeMillis(), ocupado, onFechar, onPegar = {},
        aviso = when {
            j.bool("minha") -> "Esta rota já é sua."
            j.bool("em_revisao") -> "Em revisão: ainda não saiu para os motoboys. O gestor quer saber se você pode."
            !j.bool("pode_pegar") && tipo == "rota" -> "Esta rota já está com alguém ou foi encerrada."
            else -> null
        },
        rodapeProprio = {
            if (lista.size > 1) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lista.forEachIndexed { i, (cr, _) -> PwPilula(cr.numeroExibicao, PwIcones.Route, { escolhida = i }, ligada = i == escolhida) }
            }
            PwErro(erro)
            if (j.bool("pode_pegar")) PwBotaoOuro(if (ocupado) "Pegando…" else "Pegar esta rota", onClick = {
                ocupado = true
                escopo.launch {
                    Corridas.pegarRota(r.id).onSuccess { PwToast.mostrar("A rota é sua."); onFechar() }.onFailure { erro = it.message }
                    ocupado = false
                }
            }, icone = PwIcones.ChevronsRight, habilitado = !ocupado, altura = 56.dp)
            if (respostas) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Agora não", onClick = { responder(false) }, icone = PwIcones.CircleX, habilitado = !ocupado, modifier = Modifier.weight(1f))
                PwBotaoOuro("Posso fazer", onClick = { responder(true) }, icone = PwIcones.Check, habilitado = !ocupado, modifier = Modifier.weight(1f))
            } else if (!j.bool("pode_pegar")) PwBotaoVidro("Fechar", onClick = onFechar, modifier = Modifier.fillMaxWidth())
        },
    )
}
