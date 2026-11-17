package com.pwexpress.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Um aviso que o Dev mandou (por empresa e por cargo). */
data class AvisoDev(val id: String, val texto: String, val nivel: String)

/** Faixa com os avisos do Dev para esta conta. Os comuns fecham (até reabrir o app); os urgentes ficam. */
@Composable
fun AvisosDevFaixa(modifier: Modifier = Modifier) {
    val c = Pw.cores
    var avisos by remember { mutableStateOf<List<AvisoDev>>(emptyList()) }
    val fechados = remember { mutableStateListOf<String>() }
    LaunchedEffect(Unit) {
        while (true) {
            runCatching {
                val r = PwCore.rpc.chamar("avisos_para_mim", JsonObject(emptyMap()))
                if (r.ok) avisos = (Json.parseToJsonElement(r.corpo) as? JsonArray)?.mapNotNull { e ->
                    val o = e as? JsonObject ?: return@mapNotNull null
                    fun s(k: String) = (o[k] as? JsonPrimitive)?.content
                    AvisoDev(s("id") ?: return@mapNotNull null, s("texto").orEmpty(), s("nivel") ?: "info")
                }.orEmpty()
            }
            delay(60_000)
        }
    }
    val lista = avisos.filter { it.nivel == "urgente" || it.id !in fechados }
    if (lista.isEmpty()) return
    Column(modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lista.forEach { a ->
            val cor = when (a.nivel) { "urgente" -> c.vermelho; "aviso" -> c.ouro; else -> c.texto2 }
            val forma = RoundedCornerShape(14.dp)
            Row(Modifier.fillMaxWidth().clip(forma).background(c.fundo2).border(1.dp, cor.copy(alpha = 0.55f), forma).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(if (a.nivel == "urgente") PwIcones.TriangleAlert else PwIcones.Megaphone, null, tint = cor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(a.texto, color = c.texto, fontSize = 14.sp, modifier = Modifier.weight(1f))
                if (a.nivel != "urgente") PwBotaoIcone(PwIcones.X, "Fechar aviso", { fechados.add(a.id) })
            }
        }
    }
}
