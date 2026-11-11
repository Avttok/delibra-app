package com.pwexpress.motoboy.ui

import android.content.Intent
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** Modo Dev: qual empresa o app está mostrando (Tudo, Em branco ou uma empresa). */
object ModoDevApp {
    private val _aberto = MutableStateFlow(false)
    val aberto = _aberto.asStateFlow()
    /** Rótulo do modo atual, para o selo flutuante. */
    val rotulo = MutableStateFlow<String?>(null)
    fun abrir() { _aberto.value = true }
    fun fechar() { _aberto.value = false }
}

private data class EmpresaDev(val id: String, val nome: String, val nomeApp: String?, val logo: String?, val cor: String?, val status: String, val motoboys: Int)

private fun JsonObject.t(k: String) = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

private suspend fun lerModo(): Triple<String, String?, List<EmpresaDev>>? {
    val r = PwCore.rpc.chamar("dev_empresas_foco", JsonObject(emptyMap()))
    if (!r.ok) return null
    val o = runCatching { kotlinx.serialization.json.Json.parseToJsonElement(r.corpo) as? JsonObject }.getOrNull() ?: return null
    val lista = (o["empresas"] as? JsonArray)?.mapNotNull { it as? JsonObject }?.map {
        EmpresaDev(it.t("id") ?: "", it.t("nome") ?: "", it.t("nome_app"), it.t("logo"), it.t("cor"), it.t("status") ?: "ativa", (it["motoboys"] as? JsonPrimitive)?.intOrNull ?: 0)
    }.orEmpty()
    return Triple(o.t("modo") ?: "tudo", o.t("foco"), lista)
}

private fun rotuloDe(modo: String, foco: String?, lista: List<EmpresaDev>) = when (modo) {
    "vazio" -> "Em branco"
    "empresa" -> lista.firstOrNull { it.id == foco }?.let { it.nomeApp ?: it.nome } ?: "Empresa"
    else -> "Tudo"
}

/** Selo flutuante (só para o Dev), por cima de todas as telas: mostra e troca o modo. */
@Composable
fun SeloModoDev(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val rotulo by ModoDevApp.rotulo.collectAsState()
    LaunchedEffect(Unit) { lerModo()?.let { (m, f, l) -> ModoDevApp.rotulo.value = rotuloDe(m, f, l) } }
    val forma = RoundedCornerShape(99.dp)
    Row(
        modifier.clip(forma).background(c.fundo2.copy(alpha = 0.94f)).border(1.dp, c.dev.copy(alpha = 0.6f), forma)
            .clickable { ModoDevApp.abrir() }.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PwIcones.CodeXml, null, tint = c.dev, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(6.dp))
        Text(rotulo ?: "Modo Dev", color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 140.dp))
    }
}

/** Janela do modo Dev: escolher o que o app mostra. Ao trocar, o app recomeça já na empresa escolhida. */
@Composable
fun JanelaModoDev() {
    val aberto by ModoDevApp.aberto.collectAsState()
    if (!aberto) return
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var dados by remember { mutableStateOf<Triple<String, String?, List<EmpresaDev>>?>(null) }
    var trocando by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { dados = lerModo() }

    fun escolher(modo: String, org: String? = null) {
        if (trocando) return
        trocando = true
        escopo.launch {
            val r = PwCore.rpc.chamar("dev_definir_modo", buildJsonObject { put("p_modo", modo); org?.let { put("p_org", it) } })
            if (!r.ok) { trocando = false; PwToast.mostrar("Não deu para trocar (${r.codigo}).", TipoAviso.ERRO); return@launch }
            // recomeça o app do zero: listas, mapa e ajustes já vêm da empresa escolhida
            ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ctx.startActivity(it)
            }
            Runtime.getRuntime().exit(0)
        }
    }

    PwJanela("Modo Dev", { ModoDevApp.fechar() }) {
        val d = dados
        if (d == null || trocando) { PwCarregando(texto = if (trocando) "Trocando e recomeçando o app…" else null); return@PwJanela }
        val (modo, foco, lista) = d
        Text("Escolha o que o app mostra. Ao trocar, o app recomeça já com a escolha.", color = c.texto2, fontSize = 13.sp)
        Linha(PwIcones.Globe, "Tudo", "A plataforma inteira, sem filtro", null, modo == "tudo") { escolher("tudo") }
        Linha(PwIcones.SquareDashed, "Em branco", "Como uma conta sem empresa", null, modo == "vazio") { escolher("vazio") }
        Text("Empresas", color = c.texto3, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
        if (lista.isEmpty()) Text("Nenhuma empresa ainda. Crie no site, em Dev → Empresas.", color = c.texto3, fontSize = 13.sp)
        lista.forEach { e ->
            Linha(PwIcones.Building2, e.nomeApp ?: e.nome, listOfNotNull(e.nomeApp?.let { e.nome }, "${e.motoboys} motoboy${if (e.motoboys == 1) "" else "s"}", e.status.takeIf { it != "ativa" }).joinToString(" · "),
                e.logo, modo == "empresa" && foco == e.id) { escolher("empresa", e.id) }
        }
    }
}

@Composable
private fun Linha(icone: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, sub: String, logo: String?, ativo: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(forma).background(if (ativo) c.ouro.copy(alpha = 0.12f) else c.vidro)
            .border(1.dp, if (ativo) c.ouro.copy(alpha = 0.6f) else c.borda, forma).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(c.fundo2), contentAlignment = Alignment.Center) {
            if (logo != null) coil.compose.AsyncImage(logo, null, Modifier.size(40.dp), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
            else Icon(icone, null, tint = c.ouro, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (ativo) Box(Modifier.size(24.dp).clip(CircleShape).background(c.ouro), contentAlignment = Alignment.Center) {
            Icon(PwIcones.Check, "Atual", tint = Color.Black, modifier = Modifier.size(14.dp))
        }
    }
}
