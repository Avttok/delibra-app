package com.pwexpress.core.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwLogoGrande
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

/**
 * Aceite dos termos de uso e da política de privacidade (mesma versão do site; o Dev muda em Ajustes → Plataforma).
 * [pendente]: null = não conferido, "" = tudo certo, outro texto = a versão que falta aceitar.
 */
object Termos {
    val pendente = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    suspend fun conferir() {
        val r = PwCore.rpc.chamar("termos_situacao", JsonObject(emptyMap()))
        // banco sem a função (ainda) ou fora do ar: não trava ninguém
        if (!r.ok) { pendente.value = ""; return }
        val o = runCatching { Json.parseToJsonElement(r.corpo).jsonObject }.getOrNull()
        val aceito = (o?.get("aceito") as? JsonPrimitive)?.booleanOrNull ?: true
        pendente.value = if (aceito) "" else (o?.get("versao") as? JsonPrimitive)?.content ?: ""
    }

    suspend fun aceitar(versao: String): Result<Unit> = runCatching {
        val r = PwCore.rpc.chamar("aceitar_termos", buildJsonObject { put("p_versao", versao); put("p_origem", "app") })
        if (!r.ok) { if ("versao_mudou" in r.corpo) conferir(); error("Não deu agora. Tente de novo.") }
        pendente.value = ""
    }
}

@Composable
fun AceiteTermosTela(versao: String, onSair: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var marcado by remember { mutableStateOf(false) }
    var ocupado by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    fun abrir(caminho: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PwConfig.siteUrl + caminho))) }
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PwLogoGrande(110.dp)
            Text("Termos de uso e privacidade", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 20.sp, textAlign = TextAlign.Center)
            Text("Para continuar, leia e aceite os termos de uso e a política de privacidade. Eles explicam como o app usa a sua localização, as fotos das entregas e os seus dados.",
                color = c.texto2, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwBotaoVidro("Termos de uso", onClick = { abrir("/termos") }, icone = PwIcones.FileText, modifier = Modifier.weight(1f))
                PwBotaoVidro("Privacidade", onClick = { abrir("/privacidade") }, icone = PwIcones.ShieldCheck, modifier = Modifier.weight(1f))
            }
            PwInterruptor("Li e aceito os termos e a política de privacidade", marcado, { marcado = it })
            PwErro(erro)
            PwBotaoOuro(if (ocupado) "Salvando…" else "Aceitar e continuar", icone = PwIcones.Check, habilitado = marcado && !ocupado, modifier = Modifier.fillMaxWidth(), onClick = {
                ocupado = true; erro = null
                escopo.launch { Termos.aceitar(versao).onFailure { erro = it.message }; ocupado = false }
            })
            PwBotaoVidro("Sair da conta", onClick = onSair, icone = PwIcones.LogOut, modifier = Modifier.fillMaxWidth())
        }
    }
}
