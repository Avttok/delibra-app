package com.pwexpress.motoboy.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLogo
import com.pwexpress.feature.corridas.push.Alarme
import com.pwexpress.feature.tracking.ui.Permissoes

/** Tudo o que o app precisa para funcionar direito. */
data class Preparo(val base: Boolean, val notificacoes: Boolean, val segundoPlano: Boolean, val bateria: Boolean, val telaCheia: Boolean) {
    val completo get() = base && notificacoes && segundoPlano && bateria && telaCheia
    val feitos get() = listOf(base, notificacoes, segundoPlano, bateria, telaCheia).count { it }
}

fun lerPreparo(ctx: Context): Preparo {
    val p = Permissoes.ler(ctx)
    return Preparo(p.localizacao, p.notificacoes, p.segundoPlano, p.semRestricaoBateria, Alarme.podeTelaCheia(ctx))
}

/**
 * Primeira configuração: só deixa entrar no app com tudo ativado.
 * Se algo for desligado depois, a tela volta a aparecer.
 */
@Composable
fun PrimeiraConfiguracao(onPronto: () -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val c = Pw.cores
    var estado by remember { mutableStateOf(lerPreparo(ctx)) }
    var tentativasSegundoPlano by remember { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        estado = lerPreparo(ctx)
        onPauseOrDispose { }
    }

    val pedirBase = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        estado = lerPreparo(ctx)
        if (!estado.base || !estado.notificacoes) Permissoes.abrirConfiguracoesDoApp(ctx)
    }
    val pedirSegundoPlano = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        estado = lerPreparo(ctx)
        tentativasSegundoPlano++
        if (!ok) Permissoes.abrirConfiguracoesDoApp(ctx)
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwLogo(tamanho = 64, soSelo = true)
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Vamos preparar o aparelho", style = MaterialTheme.typography.headlineSmall, color = c.texto)
                Text("${estado.feitos} de 5 concluídos", color = c.texto2, fontFamily = Archivo)
            }
        }
        LinearProgressIndicator(
            progress = { estado.feitos / 5f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
            color = c.ouro, trackColor = c.borda,
        )
        Text(
            "Sem esses ajustes você pode não receber corridas ou o Android pode desligar o GPS no meio da entrega.",
            color = c.texto2, style = MaterialTheme.typography.bodyMedium,
        )

        Passo(1, PwIcones.MapPin, "Localização precisa", "Para receber corridas perto de você e registrar o trajeto.", estado.base) {
            pedirBase.launch(Permissoes.basicas())
        }
        Passo(2, PwIcones.BellRing, "Notificações", "Para as ofertas chegarem com som.", estado.notificacoes) {
            pedirBase.launch(Permissoes.basicas())
        }
        Passo(
            3, PwIcones.LocateFixed, "Localização \"o tempo todo\"",
            if (tentativasSegundoPlano > 0 && !estado.segundoPlano) "Na tela que abriu, toque em Localização → Permitir o tempo todo."
            else "Para o GPS continuar com a tela apagada.",
            estado.segundoPlano, habilitado = estado.base,
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) pedirSegundoPlano.launch(Permissoes.SEGUNDO_PLANO)
        }
        Passo(4, PwIcones.BatteryLow, "Sem restrição de bateria", "Para o Android não pausar o app.", estado.bateria) {
            Permissoes.pedirSemRestricaoBateria(ctx)
        }
        Passo(5, PwIcones.Maximize2, "Corridas em tela cheia", "Para a corrida aparecer mesmo com o celular bloqueado.", estado.telaCheia) {
            abrirTelaCheia(ctx)
        }

        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Volume2, null, tint = c.ouro, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Dica: deixe o volume de ALARME alto. É nele que tocam as ofertas.", color = c.texto, style = MaterialTheme.typography.bodyMedium)
            }
        }

        PwBotaoOuro(
            if (estado.completo) "Tudo pronto, entrar" else "Ative os ${5 - estado.feitos} itens restantes",
            onClick = onPronto,
            habilitado = estado.completo,
            icone = if (estado.completo) PwIcones.Check else PwIcones.ShieldCheck,
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Passo(numero: Int, icone: ImageVector, titulo: String, texto: String, ok: Boolean, habilitado: Boolean = true, onAtivar: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(18.dp)
    Row(
        Modifier.fillMaxWidth().clip(forma)
            .background(if (ok) c.verde.copy(alpha = 0.08f) else c.vidroSutil)
            .border(1.dp, if (ok) c.verde.copy(alpha = 0.5f) else c.borda, forma)
            .clickable(enabled = !ok && habilitado, onClick = onAtivar)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(CircleShape).then(if (ok) Modifier.background(c.verde.copy(alpha = 0.18f)) else Modifier.background(c.gradSuave)),
            contentAlignment = Alignment.Center,
        ) { Icon(if (ok) PwIcones.Check else icone, null, tint = if (ok) c.verde else c.ouro, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("$numero. $titulo", color = c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo)
            Text(texto, color = c.texto2, fontSize = 13.sp, fontFamily = Archivo)
        }
        if (!ok) {
            Spacer(Modifier.width(8.dp))
            Text(
                "Ativar",
                color = if (habilitado) c.sobreOuro else c.texto3,
                fontWeight = FontWeight.Bold,
                fontFamily = Archivo,
                modifier = Modifier.clip(RoundedCornerShape(10.dp))
                    .background(if (habilitado) c.gradOuro else androidx.compose.ui.graphics.SolidColor(c.borda))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
        }
    }
}

private fun abrirTelaCheia(ctx: Context) {
    val i = if (Build.VERSION.SDK_INT >= 34) {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}"))
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
    }
    runCatching { ctx.startActivity(i) }
}
