package com.pwexpress.core.perfil

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwSelosCargo
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.tentar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/** Pedido para abrir o perfil de alguém (toque na foto/nome no chat, num cartão de pessoa…). */
object PerfilAberto {
    val id = MutableStateFlow<String?>(null)
    fun abrir(pessoa: String) { id.value = pessoa }
}

private suspend fun carregarPerfil(id: String): Result<JsonObject> = tentar {
    val r = PwCore.rpc.chamar("perfil_publico", buildJsonObject { put("p_id", id) })
    if (!r.ok) error(when {
        r.corpo.contains("sem_permissao") -> "Você não tem acesso a este perfil."
        r.corpo.contains("nao_encontrado") -> "Essa pessoa não existe mais."
        else -> "Não deu para abrir o perfil (${r.codigo})."
    })
    Json.parseToJsonElement(r.corpo).jsonObject
}

/**
 * Perfil completo da pessoa: foto, cargo, status, números, conquistas, veículo e bairros onde mais entrega.
 * Cada parte segue os ajustes do gestor (perfil_mostra_*); o gestor vê tudo.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PerfilPublicoTela(id: String, onFechar: () -> Unit) {
    val c = Pw.cores
    var p by remember(id) { mutableStateOf<JsonObject?>(null) }
    var erro by remember(id) { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { carregarPerfil(id).onSuccess { p = it }.onFailure { erro = it.message } }
    PwTelaCheia(p?.str("nome") ?: "Perfil", onFechar, subtitulo = p?.str("empresa")) {
        val x = p
        if (x == null) { if (erro != null) PwErro(erro) else PwCarregando(); return@PwTelaCheia }
        val cargo = x.str("cargo") ?: "cliente"
        val status = x.str("status")
        val corStatus = when (status) { "disponivel" -> c.verde; "em_corrida" -> c.ouro; else -> c.texto3 }
        // cabeçalho com a foto grande e o status (verde online, dourado em rota, cinza offline)
        val forma = RoundedCornerShape(26.dp)
        Column(
            Modifier.fillMaxWidth().clip(forma).background(Brush.verticalGradient(listOf(c.ouro.copy(alpha = 0.18f), c.vidro)))
                .border(1.dp, c.ouro.copy(alpha = 0.35f), forma).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box {
                PwAvatar(x.str("nome") ?: "?", x.str("foto"), tamanho = 104.dp, oculto = x.bool("oculto"))
                if (status != null) Box(Modifier.align(Alignment.BottomEnd).padding(6.dp).size(22.dp).clip(CircleShape).background(c.fundo2).padding(3.dp)) {
                    Box(Modifier.size(16.dp).clip(CircleShape).background(corStatus))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(x.str("nome") ?: "", color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 22.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            PwSelosCargo(cargo == "dev", cargo == "gestor", x.bool("motoboy"), supervisor = cargo == "supervisor")
            if (status != null) {
                Spacer(Modifier.height(8.dp))
                PwSelo(when (status) { "disponivel" -> "Online"; "em_corrida" -> "Em rota"; else -> "Offline" }, corStatus,
                    icone = when (status) { "disponivel" -> PwIcones.Radar; "em_corrida" -> PwIcones.Route; else -> PwIcones.Moon })
            }
            x.str("desde")?.take(10)?.let { d ->
                Spacer(Modifier.height(8.dp))
                Text("Na equipe desde ${d.substring(8, 10)}/${d.substring(5, 7)}/${d.substring(0, 4)}", color = c.texto3, fontSize = 12.5.sp)
            }
        }
        if (x.bool("fechado")) {
            PwCartao { Text("O gestor deixou os perfis fechados: só nome e foto aparecem.", color = c.texto2) }
            return@PwTelaCheia
        }
        val n = x.obj("numeros")
        if (n != null && x.bool("mostra_numeros")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Rotas", numeroBr(n.dbl("corridas") ?: 0.0, 0), Modifier.weight(1f), icone = PwIcones.Route)
                PwNumero("Entregas", numeroBr(n.dbl("entregas") ?: 0.0, 0), Modifier.weight(1f), icone = PwIcones.Package2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Km rodados", numeroBr(n.dbl("km") ?: 0.0, 0), Modifier.weight(1f), icone = PwIcones.Ruler)
                PwNumero("Horas online", numeroBr(n.dbl("horas_online") ?: 0.0, 0), Modifier.weight(1f), icone = PwIcones.Clock)
            }
        }
        if (n != null && x.bool("mostra_conquistas")) PwCartao { GradeConquistas(n) }
        x.obj("veiculo")?.takeIf { v -> listOf("modelo", "cor", "placa").any { v.str(it) != null } }?.let { v ->
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Bike, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); PwRotulo("Veículo")
                }
                Spacer(Modifier.height(6.dp))
                Text(listOfNotNull(v.str("modelo"), v.str("cor"), v.str("ano")).joinToString(" · ").ifEmpty { "Veículo" }, color = c.texto, fontWeight = FontWeight.SemiBold)
                v.str("placa")?.let { Spacer(Modifier.height(10.dp)); com.pwexpress.core.designsystem.PwPlaca(it, largura = 190.dp) }
            }
        }
        val regioes = x.arr("regioes")
        if (regioes.isNotEmpty()) PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.MapPinned, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); PwRotulo("Onde mais entrega")
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                regioes.forEach { r -> PwSelo("${r.str("bairro")} · ${r.int("paradas") ?: 0}", c.texto2, icone = PwIcones.MapPin) }
            }
        }
        x.dbl("penalidade_pontos")?.takeIf { it > 0 }?.let { pts ->
            PwCartao(cor = c.vermelho.copy(alpha = 0.45f)) {
                Text("${numeroBr(pts, 1)} pts de penalidade na janela" + (x.str("suspenso_ate")?.let { " · suspenso" } ?: ""), color = c.vermelho, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
