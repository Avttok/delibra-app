package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.iso
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Pedido de cancelamento aberto por um motoboy. */
internal data class ChamadoG(
    val id: String, val corridaId: String, val corridaNumero: Long, val corridaTeste: Boolean, val motoboyId: String, val motoboyNome: String,
    val motivo: String, val status: String, val resposta: String?, val criadoEm: Long?, val feitas: Int, val paradas: Int, val abandono: Boolean,
    /** "cancelar" ou "pular" (liberar uma parada fora do raio). */
    val tipo: String = "cancelar", val paradaOrdem: Int? = null, val paradaEndereco: String? = null,
) {
    val pular get() = tipo == "pular"
    val numeroExibicao get() = (if (corridaTeste) "T" else "#") + corridaNumero
}

private fun JsonObject.paraChamado() = ChamadoG(
    str("id").orEmpty(), str("corrida_id").orEmpty(), long("corrida_numero") ?: 0, bool("corrida_teste"), str("motoboy_id").orEmpty(),
    str("motoboy_nome") ?: "Motoboy", str("motivo").orEmpty(), str("status") ?: "pendente", str("resposta"), iso(str("criado_em")),
    int("feitas") ?: 0, int("paradas") ?: 0, bool("abandono"),
    str("tipo") ?: "cancelar", int("parada_ordem"), str("parada_endereco"),
)

internal object ChamadosG {
    suspend fun abertos(): List<ChamadoG> =
        ((Gestao.rpcJson("chamados_abertos").getOrNull() as? JsonArray) ?: JsonArray(emptyList())).mapNotNull { (it as? JsonObject)?.paraChamado() }

    suspend fun responder(id: String, aprovar: Boolean, resposta: String?, penalizar: Boolean?) =
        Gestao.rpcJson("responder_chamado", buildJsonObject {
            put("p_chamado", id); put("p_aprovar", aprovar); resposta?.takeIf { it.isNotBlank() }?.let { put("p_resposta", it) }; penalizar?.let { put("p_penalizar", it) }
        })

    suspend fun abandono(corrida: String, motivo: String?) =
        Gestao.rpcJson("marcar_abandono", buildJsonObject { put("p_corrida", corrida); motivo?.takeIf { it.isNotBlank() }?.let { put("p_motivo", it) } })
}

/** Faixa no topo do painel: motoboys pedindo para cancelar (toque abre a rota). */
@Composable
internal fun FaixaChamadosG(onAbrir: (String) -> Unit) {
    val c = Pw.cores
    var lista by remember { mutableStateOf<List<ChamadoG>>(emptyList()) }
    LaunchedEffect(Unit) { while (true) { lista = ChamadosG.abertos().filter { it.status == "pendente" }; delay(15_000) } }
    if (lista.isEmpty()) return
    val forma = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp).clip(forma)
            .background(c.vermelho.copy(alpha = 0.14f)).border(1.dp, c.vermelho.copy(alpha = 0.5f), forma)
            .clickable { onAbrir(lista.first().corridaId) }.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val um = lista.first()
        Icon(if (lista.size == 1 && um.pular) PwIcones.LockOpen else PwIcones.Hand, null, tint = c.vermelho, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(when {
                lista.size > 1 -> "${lista.size} motoboys esperam sua resposta"
                um.pular -> "${um.motoboyNome.substringBefore(" ")} pede para liberar a parada ${(um.paradaOrdem ?: 0) + 1} da rota ${um.numeroExibicao}"
                else -> "${um.motoboyNome.substringBefore(" ")} quer cancelar a rota ${um.numeroExibicao}"
            },
                color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 13.5.sp, maxLines = 1)
            Text(if (lista.size == 1) lista.first().motivo else "Toque para responder o primeiro", color = c.texto2, fontSize = 12.sp, maxLines = 1)
        }
        Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
    }
}

/** Dentro da rota: o pedido de cancelamento dela (responder, ou marcar abandono depois de negado). */
@Composable
internal fun ChamadosDaCorridaG(corridaId: String, onMudou: suspend () -> Unit) {
    val c = Pw.cores
    var lista by remember(corridaId) { mutableStateOf<List<ChamadoG>>(emptyList()) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(corridaId, versao) { lista = ChamadosG.abertos().filter { it.corridaId == corridaId } }
    val ch = lista.firstOrNull { it.status == "pendente" } ?: lista.firstOrNull { it.status == "negado" && !it.abandono && !it.pular } ?: return
    val escopo = rememberCoroutineScope()
    var resposta by remember(ch.id) { mutableStateOf("") }
    // pular: sem penalidade, a não ser que você marque
    var penalizar by remember(ch.id) { mutableStateOf(!ch.pular && Gestao.dados.value.cfgNum("pen_pts_desistencia", 2.0) > 0) }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var abandono by remember { mutableStateOf(false) }
    fun rodar(msg: String, bloco: suspend () -> Result<*>) {
        ocupado = true
        escopo.launch {
            bloco().onSuccess { PwToast.mostrar(msg); erro = null; versao++; onMudou(); Gestao.atualizar() }.onFailure { erro = it.message }
            ocupado = false
        }
    }
    PwCartao(cor = (if (ch.status == "pendente") c.vermelho else c.ouro).copy(alpha = 0.55f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (ch.pular) PwIcones.LockOpen else PwIcones.Hand, null, tint = if (ch.status == "pendente") c.vermelho else c.ouro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            PwRotulo(when {
                ch.pular && ch.status == "pendente" -> "${ch.motoboyNome.substringBefore(" ")} pede para liberar a parada ${(ch.paradaOrdem ?: 0) + 1}"
                ch.pular -> "Liberação negada"
                ch.status == "pendente" -> "${ch.motoboyNome.substringBefore(" ")} pediu para cancelar"
                else -> "Cancelamento negado"
            }, Modifier.weight(1f))
            PwSelo("${ch.feitas}/${ch.paradas} feitas", c.texto2, icone = PwIcones.ListChecks)
        }
        Espaco(6)
        Text("“${ch.motivo}”", color = c.texto, fontSize = 14.5.sp, fontFamily = Archivo)
        if (ch.pular) ch.paradaEndereco?.let { TextoDica("Parada: $it") }
        ch.criadoEm?.let { TextoDica("Às ${hora(it)} · ${relativo(it)}") }
        if (ch.status == "pendente") {
            Espaco(8)
            PwCampo(resposta, { resposta = it }, "Resposta para ele", dica = "Opcional: ele vê no aviso")
            Espaco(6)
            if (ch.pular) PwInterruptor("Penalizar ao liberar", penalizar, { penalizar = it },
                descricao = "Conta como pulo (${numeroBr(maxOf(Gestao.dados.value.cfgNum("pen_pts_pulo", 0.0), 1.0), 1)} pts). Sem marcar, não conta nada.", icone = PwIcones.Gavel, cor = c.vermelho)
            else PwInterruptor("Penalizar se aprovar", penalizar, { penalizar = it },
                descricao = "Conta como desistência (${numeroBr(Gestao.dados.value.cfgNum("pen_pts_desistencia", 2.0), 1)} pts em Ajustes)", icone = PwIcones.Gavel, cor = c.vermelho)
            Espaco(8)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwBotaoVidro("Negar", onClick = { rodar(if (ch.pular) "Negado: ele precisa chegar ao endereço." else "Negado: a rota continua com ele.") { ChamadosG.responder(ch.id, false, resposta, null) } },
                    icone = PwIcones.CircleX, habilitado = !ocupado, modifier = Modifier.weight(1f))
                PwBotaoOuro(if (ch.pular) "Liberar" else "Aprovar", onClick = { rodar(if (ch.pular) "Liberada: ele confirma de onde está." else "Aprovado: a rota voltou para o sistema.") { ChamadosG.responder(ch.id, true, resposta, penalizar) } },
                    icone = if (ch.pular) PwIcones.LockOpen else PwIcones.Check, habilitado = !ocupado, modifier = Modifier.weight(1f))
            }
        } else if (ch.pular) {
            ch.resposta?.let { TextoDica("Sua resposta: $it") }
        } else {
            ch.resposta?.let { TextoDica("Sua resposta: $it") }
            Espaco(8)
            TextoDica("Se ele não seguir com a rota, marque abandono: a rota volta para o sistema e ele leva ${numeroBr(Gestao.dados.value.cfgNum("pen_pts_abandono", 5.0), 1)} pts.")
            Espaco(6)
            PwBotaoPerigo("Marcar abandono", onClick = { abandono = true }, icone = PwIcones.UserX, habilitado = !ocupado, modifier = Modifier.fillMaxWidth())
        }
        PwErro(erro)
    }
    if (abandono) {
        var motivo by remember { mutableStateOf("") }
        PwJanela("Marcar abandono da rota ${ch.numeroExibicao}?", onFechar = { abandono = false }) {
            Text("A rota sai do motoboy, volta para o sistema e ele recebe a penalidade de abandono.", color = c.texto2)
            PwCampo(motivo, { motivo = it }, "O que aconteceu", dica = "Fica no histórico de penalidades")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Voltar", onClick = { abandono = false }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Marcar", onClick = { abandono = false; rodar("Abandono marcado.") { ChamadosG.abandono(ch.corridaId, motivo) } }, modifier = Modifier.weight(1f), cheio = true, icone = PwIcones.UserX)
            }
        }
    }
}

/** Penalidades de um motoboy ou cliente: pontos na janela, suspensão e histórico (perdoar, penalizar à mão, tirar suspensão). */
@Composable
internal fun PenalidadesG(alvoTipo: String, alvoId: String) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var dados by remember(alvoId) { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableStateOf(0) }
    var erro by remember { mutableStateOf<String?>(null) }
    var nova by remember { mutableStateOf(false) }
    LaunchedEffect(alvoId, versao) {
        Gestao.rpcJson("penalidades_de", buildJsonObject { put("p_alvo_tipo", alvoTipo); put("p_alvo", alvoId) })
            .onSuccess { dados = it as? JsonObject; erro = null }.onFailure { erro = it.message }
    }
    fun rodar(msg: String, bloco: suspend () -> Result<*>) = escopo.launch {
        bloco().onSuccess { PwToast.mostrar(msg); versao++ }.onFailure { erro = it.message }
    }
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Gavel, null, tint = c.ouro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            PwRotulo("Penalidades", Modifier.weight(1f))
            com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Plus, "Penalizar", { nova = true }, tamanho = 36.dp)
        }
        val d = dados
        if (d == null) { PwErro(erro); return@PwCartao }
        val pontos = d.dbl("pontos") ?: 0.0
        val limite = d.dbl("limite") ?: 0.0
        Espaco(8)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(numeroBr(pontos, 1), color = if (limite > 0 && pontos >= limite) c.vermelho else c.texto, fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = Archivo)
            Spacer(Modifier.width(6.dp))
            Text(if (limite > 0) "de ${numeroBr(limite, 0)} pts nos últimos ${d.int("janela_dias") ?: 30} dias" else "pts (sem limite)", color = c.texto3, fontSize = 12.5.sp, modifier = Modifier.padding(bottom = 5.dp))
        }
        if (limite > 0) {
            Espaco(6)
            val fr = (pontos / limite).coerceIn(0.0, 1.0).toFloat()
            androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)).background(c.bordaForte)) {
                androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth(fr).height(8.dp).clip(RoundedCornerShape(99.dp)).background(if (fr >= 1f) c.vermelho else c.ouro))
            }
        }
        d.str("suspenso_ate")?.let { ate ->
            Espaco(10)
            PwCartao(cor = c.vermelho.copy(alpha = 0.6f), padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Text((if (alvoTipo == "motoboy") "Suspenso" else "Bloqueado") + " até ${dataHora(iso(ate))}", color = c.vermelho, fontWeight = FontWeight.Bold)
                d.str("motivo")?.let { TextoDica(it) }
                Espaco(6)
                PwBotaoVidro(if (alvoTipo == "motoboy") "Tirar suspensão" else "Tirar bloqueio",
                    onClick = { rodar("Liberado.") { Gestao.rpcJson("levantar_suspensao", buildJsonObject { put("p_alvo_tipo", alvoTipo); put("p_alvo", alvoId) }) } },
                    icone = PwIcones.Unplug, modifier = Modifier.fillMaxWidth())
            }
        }
        val itens = d.arr("itens")
        if (itens.isEmpty()) { Espaco(6); TextoDica("Nenhuma penalidade.") }
        itens.take(20).forEach { i ->
            Espaco(8)
            val vale = i.bool("vale")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${ROTULO_PEN[i.str("tipo")] ?: i.str("tipo")} · ${numeroBr(i.dbl("pontos") ?: 0.0, 1)} pts", color = if (vale) c.texto else c.texto3,
                        fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
                    Text(listOfNotNull(i.str("motivo"), i.long("corrida_numero")?.let { "rota #$it" }, dataHora(iso(i.str("quando")))).joinToString(" · "),
                        color = c.texto3, fontSize = 12.sp, maxLines = 2)
                }
                if (i.bool("perdoada")) PwSelo("Perdoada", c.verde, icone = PwIcones.Check)
                else if (vale) com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Eraser, "Perdoar",
                    { rodar("Perdoada.") { Gestao.rpcJson("perdoar_penalidade", buildJsonObject { put("p_id", i.str("id").orEmpty()) }) } }, tamanho = 36.dp)
                else PwSelo("Venceu", c.texto3)
            }
        }
        PwErro(erro)
    }
    if (nova) {
        var pts by remember { mutableStateOf(2.0) }
        var motivo by remember { mutableStateOf("") }
        PwJanela("Penalizar", onFechar = { nova = false }) {
            com.pwexpress.core.designsystem.PwPasso(pts, { pts = it }, Modifier.fillMaxWidth(), passo = 1.0, min = 1.0, max = 100.0, unidade = "pts")
            PwCampo(motivo, { motivo = it }, "Motivo", dica = "Fica no histórico")
            PwBotaoPerigo("Aplicar ${numeroBr(pts, 0)} pts", onClick = {
                nova = false
                rodar("Penalidade aplicada.") { Gestao.rpcJson("penalizar_manual", buildJsonObject { put("p_alvo_tipo", alvoTipo); put("p_alvo", alvoId); put("p_pontos", pts); put("p_motivo", motivo.trim()) }) }
            }, icone = PwIcones.Gavel, habilitado = motivo.isNotBlank(), cheio = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

private val ROTULO_PEN = mapOf(
    "ignorada" to "Oferta ignorada", "recusa" to "Recusou oferta", "desistencia" to "Desistência", "abandono" to "Abandono",
    "atraso" to "Atraso", "manual" to "Manual", "cancelamento" to "Cancelamento",
    "pulo" to "Parada liberada fora do raio", "avaliacao" to "Avaliação ruim",
)
