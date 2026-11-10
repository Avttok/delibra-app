package com.pwexpress.feature.corridas.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPonto
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.feature.corridas.push.Alarme

/** Cartão para a tela inicial: verifica se as ofertas vão conseguir chamar atenção. */
@Composable
fun CartaoOfertas() {
    val ctx = LocalContext.current
    var telaCheia by remember { mutableStateOf(Alarme.podeTelaCheia(ctx)) }
    LifecycleResumeEffect(Unit) {
        telaCheia = Alarme.podeTelaCheia(ctx)
        onPauseOrDispose { }
    }

    PwCartao(destaque = !telaCheia) {
        PwRotulo("Corridas novas")
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PwPonto(if (telaCheia) Pw.cores.verde else Pw.cores.vermelho, 10.dp)
            Spacer(Modifier.width(10.dp))
            Text("Abrir em tela cheia", Modifier.weight(1f), color = Pw.cores.texto)
            if (!telaCheia && Build.VERSION.SDK_INT >= 34) {
                TextButton(onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${ctx.packageName}")))
                    }
                }) { Text("Liberar", color = Pw.cores.ouro) }
            } else {
                Text("OK", color = Pw.cores.verde, modifier = Modifier.padding(12.dp))
            }
        }
        Text(
            "O som usa o volume de ALARME do celular. Deixe-o alto.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        PwBotaoVidro("Testar som", onClick = { Alarme.testarSom(ctx) }, icone = PwIcones.Volume2)
    }
}
