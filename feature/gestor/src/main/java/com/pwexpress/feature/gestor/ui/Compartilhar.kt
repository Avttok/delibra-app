package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.str
import com.pwexpress.core.json.strs
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

private suspend fun carregarCompartilhamento(): Result<JsonObject?> = Gestao.rpcJson("compartilhamento_motoboys").map { it as? JsonObject }

private suspend fun salvarCompartilhamento(modo: String, orgs: Set<String>, motoboy: String?) =
    Gestao.rpcJson("salvar_compartilhamento_motoboys", buildJsonObject {
        put("p_modo", modo); put("p_orgs", buildJsonArray { orgs.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } }); motoboy?.let { put("p_motoboy", it) }
    })

/** Escolha de modo + empresas (quando "escolhidas"). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EscolhaCompartilhar(opcoes: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>, modo: String, orgs: Set<String>,
                                empresas: List<Pair<String, String>>, onMudar: (String, Set<String>) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        opcoes.forEach { (k, rot, ic) -> PwPilula(rot, ic, { onMudar(k, orgs) }, ligada = modo == k) }
    }
    if (modo == "escolhidas") {
        Espaco(8)
        if (empresas.isEmpty()) TextoDica("Ainda não há outras empresas na plataforma.")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            empresas.forEach { (id, nome) -> PwPilula(nome, if (id in orgs) PwIcones.Check else PwIcones.Building2, { onMudar(modo, if (id in orgs) orgs - id else orgs + id) }, ligada = id in orgs) }
        }
    }
}

/** Ajustes → Despacho: com quem a empresa compartilha os motoboys (se o Dev deixar). */
@Composable
internal fun CompartilharEmpresaG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var d by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { carregarCompartilhamento().onSuccess { d = it }.onFailure { erro = it.message } }
    val x = d ?: return
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Handshake, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("Seus motoboys em outras empresas")
        }
        Espaco(4)
        if (!x.bool("permitido")) { TextoDica("O Dev não liberou o compartilhamento de motoboys entre empresas."); return@PwCartao }
        TextoDica("Rotas de outras empresas podem chegar aos seus motoboys (sempre depois dos motoboys delas). Cada motoboy pode ter a escolha dele em Equipe → motoboy → Regras.")
        Espaco(8)
        val empresas = x.arr("empresas").map { it.str("id").orEmpty() to (it.str("nome") ?: "Empresa") }
        EscolhaCompartilhar(listOf(
            Triple("padrao", "Como o Dev definiu", PwIcones.CodeXml), Triple("nenhuma", "Nenhuma", PwIcones.Lock),
            Triple("todas", "Todas", PwIcones.Globe), Triple("escolhidas", "Escolher", PwIcones.ListChecks),
        ), x.str("modo") ?: "padrao", x.strs("orgs").toSet(), empresas) { modo, orgs ->
            escopo.launch {
                salvarCompartilhamento(modo, orgs, null).onSuccess { PwToast.mostrar("Compartilhamento salvo."); carregarCompartilhamento().onSuccess { d = it } }.onFailure { erro = it.message }
            }
        }
        if ((x.str("modo") ?: "padrao") == "padrao") TextoDica(if (x.bool("padrao_dev")) "Hoje: o Dev deixa seus motoboys receberem de outras empresas." else "Hoje: o Dev não manda rotas de outras empresas para os seus motoboys.")
        PwErro(erro)
    }
}

/** Equipe → motoboy → Regras: a escolha só dele. */
@Composable
internal fun CompartilharMotoboyG(motoboyId: String, nome: String) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var d by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(motoboyId) { carregarCompartilhamento().onSuccess { d = it }.onFailure { erro = it.message } }
    val x = d ?: return
    if (!x.bool("permitido")) return
    val eu = x.arr("motoboys").firstOrNull { it.str("id") == motoboyId } ?: return
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Handshake, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("${nome.substringBefore(" ")} em outras empresas")
        }
        Espaco(8)
        val empresas = x.arr("empresas").map { it.str("id").orEmpty() to (it.str("nome") ?: "Empresa") }
        EscolhaCompartilhar(listOf(
            Triple("empresa", "Igual à empresa", PwIcones.Building2), Triple("nunca", "Nunca", PwIcones.Lock),
            Triple("todas", "Todas", PwIcones.Globe), Triple("escolhidas", "Escolher", PwIcones.ListChecks),
        ), eu.str("modo") ?: "empresa", eu.strs("orgs").toSet(), empresas) { modo, orgs ->
            escopo.launch {
                salvarCompartilhamento(modo, orgs, motoboyId).onSuccess { PwToast.mostrar("Salvo para ${nome.substringBefore(" ")}."); carregarCompartilhamento().onSuccess { d = it } }.onFailure { erro = it.message }
            }
        }
        PwErro(erro)
    }
}
