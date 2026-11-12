package com.pwexpress.core.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLogoGrande
import com.pwexpress.core.designsystem.PwToast
import kotlinx.coroutines.launch

/** Estado do "segundo passo": null = ainda conferindo; "" = não precisa; id = precisa do código desse fator. */
object DoisPassos {
    val pendente = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    suspend fun conferir() { pendente.value = PwCore.auth.precisaCodigo() ?: "" }
}

/** Tela que pede o código do autenticador depois do login (igual ao site). */
@Composable
fun PedirCodigoTela(fator: String, onSair: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var codigo by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    fun entrar() {
        ocupado = true; erro = null
        escopo.launch {
            PwCore.auth.verificarCodigo(fator, codigo).onSuccess { DoisPassos.pendente.value = "" }.onFailure { erro = it.message; codigo = "" }
            ocupado = false
        }
    }
    Box(modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
        Column(Modifier.widthIn(max = 420.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PwLogoGrande(120.dp)
            Text("Verificação em dois passos", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Abra o aplicativo autenticador e digite o código de 6 números da Delibra.", color = c.texto2, textAlign = TextAlign.Center)
            PwCampo(codigo, { v -> codigo = v.filter { it.isDigit() }.take(6); if (codigo.length == 6 && !ocupado) entrar() }, "Código", teclado = KeyboardType.Number)
            PwErro(erro)
            PwBotaoOuro(if (ocupado) "Conferindo…" else "Entrar", icone = PwIcones.ShieldCheck, habilitado = codigo.length == 6 && !ocupado, onClick = ::entrar)
            PwBotaoVidro("Sair da conta", onClick = onSair, icone = PwIcones.LogOut)
        }
    }
}

/** Ajustes da conta: ligar, ver e tirar o autenticador. */
@Composable
fun DoisPassosJanela(onFechar: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var fatores by remember { mutableStateOf<List<AuthRepository.Fator>?>(null) }
    var novo by remember { mutableStateOf<AuthRepository.NovoFator?>(null) }
    var codigo by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(versao) { PwCore.auth.fatores().onSuccess { fatores = it }.onFailure { erro = it.message; fatores = emptyList() } }
    PwJanela("Verificação em dois passos", onFechar = onFechar) {
        Text("Além da senha, a conta pede um código do aplicativo autenticador (Google Authenticator, Microsoft Authenticator, Authy…). Vale no app e no site.", color = c.texto2)
        PwErro(erro)
        val l = fatores ?: run { PwCarregando(); return@PwJanela }
        val ativos = l.filter { it.verificado }
        val n = novo
        when {
            n != null -> {
                Text("1. Toque para abrir no autenticador, ou copie a chave e cole nele.", color = c.texto)
                PwBotaoVidro("Abrir no autenticador", icone = PwIcones.ShieldCheck, modifier = Modifier.fillMaxWidth(), onClick = {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(n.uri))) }.onFailure { PwToast.mostrar("Nenhum autenticador instalado. Copie a chave.") }
                })
                Text(n.segredo.chunked(4).joinToString(" "), fontFamily = FontFamily.Monospace, color = c.ouro, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
                PwBotaoVidro("Copiar a chave", icone = PwIcones.Copy, modifier = Modifier.fillMaxWidth(), onClick = {
                    ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Chave", n.segredo)); PwToast.mostrar("Chave copiada.")
                })
                Text("2. Digite o código de 6 números que aparece lá.", color = c.texto)
                PwCampo(codigo, { v -> codigo = v.filter { it.isDigit() }.take(6) }, "Código", teclado = KeyboardType.Number)
                PwBotaoOuro(if (ocupado) "Conferindo…" else "Ligar", icone = PwIcones.Check, habilitado = codigo.length == 6 && !ocupado, onClick = {
                    ocupado = true
                    escopo.launch {
                        PwCore.auth.verificarCodigo(n.id, codigo).onSuccess { PwToast.mostrar("Verificação em dois passos ligada."); novo = null; codigo = ""; versao++; DoisPassos.pendente.value = "" }
                            .onFailure { erro = it.message }
                        ocupado = false
                    }
                })
            }
            ativos.isEmpty() -> PwBotaoOuro("Ligar a verificação", icone = PwIcones.ShieldCheck, habilitado = !ocupado, onClick = {
                ocupado = true
                escopo.launch { PwCore.auth.cadastrarFator().onSuccess { novo = it; erro = null }.onFailure { erro = it.message }; ocupado = false }
            })
            else -> {
                ativos.forEach { f ->
                    Text("Ligada · ${f.nome}", color = c.verde, fontWeight = FontWeight.SemiBold)
                    PwBotaoPerigo("Desligar", {
                        escopo.launch { PwCore.auth.removerFator(f.id).onSuccess { PwToast.mostrar("Verificação desligada."); versao++ }.onFailure { erro = it.message } }
                    })
                }
            }
        }
    }
}
