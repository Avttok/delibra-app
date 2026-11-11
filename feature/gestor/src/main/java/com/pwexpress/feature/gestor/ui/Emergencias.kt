package com.pwexpress.feature.gestor.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.iso
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** Alertas de emergência abertos: faixa vermelha pulsando em todas as telas do painel até alguém marcar "Resolvido". */
@Composable
internal fun FaixaEmergenciasG(onAbrirCorrida: (String) -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(versao) {
        while (true) {
            lista = ((Gestao.rpcJson("emergencias_abertas").getOrNull() as? JsonArray) ?: JsonArray(emptyList()))
                .mapNotNull { it as? JsonObject }.filter { it.str("resolvida_em") == null }
            delay(10_000)
        }
    }
    if (lista.isEmpty()) return
    val pulso by rememberInfiniteTransition(label = "sos").animateFloat(0.55f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "sos")
    val forma = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lista.take(3).forEach { e ->
            Column(
                Modifier.fillMaxWidth().clip(forma).background(c.vermelho.copy(alpha = 0.18f)).border(1.5.dp, c.vermelho.copy(alpha = pulso), forma).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Siren, null, tint = c.vermelho, modifier = Modifier.size(22.dp).graphicsLayer { scaleX = 0.9f + pulso * 0.15f; scaleY = scaleX })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("EMERGÊNCIA · ${e.str("motoboy_nome") ?: "Motoboy"}", color = c.vermelho, fontWeight = FontWeight.Black, fontFamily = Archivo, fontSize = 14.sp, maxLines = 1)
                        val quando = iso(e.str("criado_em"))
                        Text(listOfNotNull(quando?.let { "Às ${hora(it)} · ${relativo(it)}" }, "Apertou o botão de emergência").joinToString(" · "), color = c.texto2, fontSize = 12.sp, maxLines = 1)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val lat = e.dbl("lat"); val lng = e.dbl("lng")
                    if (lat != null && lng != null) PwBotaoVidro("Onde está", onClick = {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lng?q=$lat,$lng(Emergência)"))) }
                    }, icone = PwIcones.MapPin, modifier = Modifier.weight(1f))
                    e.str("corrida_id")?.let { id -> PwBotaoVidro("Rota", onClick = { onAbrirCorrida(id) }, icone = PwIcones.Route, modifier = Modifier.weight(1f)) }
                    PwBotaoVidro("Resolvido", onClick = {
                        escopo.launch {
                            Gestao.rpcJson("resolver_emergencia", buildJsonObject { put("p_id", JsonPrimitive(e.str("id").orEmpty())) })
                                .onSuccess { PwToast.mostrar("Marcado como resolvido."); versao++ }.onFailure { PwToast.mostrar(it.message ?: "Erro", com.pwexpress.core.designsystem.TipoAviso.ERRO) }
                        }
                    }, icone = PwIcones.Check, cor = c.verde, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
