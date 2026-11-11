package com.pwexpress.feature.auth

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwexpress.core.PwCore
import com.pwexpress.core.auth.ContaSalva
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.BotaoTema
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLogo
import com.pwexpress.core.designsystem.PwRotulo
import kotlinx.coroutines.launch

/** Login. Ao entrar, a sessão em PwCore.auth muda e o app troca de tela sozinho. */
@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val contas by PwCore.auth.contas.contas.collectAsState()
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var permanecer by rememberSaveable { mutableStateOf(PwCore.auth.permanecerConectado) }
    var carregando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val foco = LocalFocusManager.current
    val focoSenha = remember { FocusRequester() }

    val podeEntrar = !carregando && email.isNotBlank() && senha.isNotBlank() && PwConfig.configurado

    fun entrar() {
        if (!podeEntrar) return
        foco.clearFocus()
        scope.launch {
            carregando = true
            erro = null
            PwCore.auth.entrar(email, senha, permanecer).onFailure { erro = it.message }
            carregando = false
        }
    }

    fun usarConta(conta: ContaSalva) {
        erro = null
        if (conta.refreshToken != null) {
            scope.launch {
                carregando = true
                PwCore.auth.entrarComContaSalva(conta).onFailure {
                    erro = it.message
                    email = conta.email
                    runCatching { focoSenha.requestFocus() }
                }
                carregando = false
            }
        } else {
            email = conta.email
            senha = ""
            runCatching { focoSenha.requestFocus() }
        }
    }

    Box(modifier.fillMaxSize()) {
        BotaoTema(Modifier.align(Alignment.TopEnd).padding(16.dp))
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 26.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            com.pwexpress.core.designsystem.PwLogoGrande(190.dp)
            Spacer(Modifier.height(28.dp))

            if (!PwConfig.configurado) {
                Text("Servidor não configurado neste APK.", color = c.vermelho, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
            }

            // ------------------------------------------------------ logar com
            if (contas.isNotEmpty()) {
                PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                    PwRotulo("Logar com", Modifier.padding(start = 6.dp, bottom = 6.dp))
                    contas.take(5).forEach { conta -> LinhaConta(conta, onUsar = { usarConta(conta) }, onRemover = { PwCore.auth.contas.remover(conta.email) }) }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ------------------------------------------------------ e-mail e senha
            OutlinedTextField(
                value = email, onValueChange = { email = it }, label = { Text("E-mail") }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            var verSenha by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
            OutlinedTextField(
                value = senha, onValueChange = { senha = it }, label = { Text("Senha") }, singleLine = true,
                visualTransformation = if (verSenha) androidx.compose.ui.text.input.VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    androidx.compose.material3.IconButton(onClick = { verSenha = !verSenha }) {
                        androidx.compose.material3.Icon(if (verSenha) com.pwexpress.core.designsystem.PwIcones.EyeOff else com.pwexpress.core.designsystem.PwIcones.Eye,
                            if (verSenha) "Esconder a senha" else "Ver a senha", tint = c.texto3)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { entrar() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focoSenha),
            )
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { permanecer = !permanecer }.padding(vertical = 6.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Permanecer conectado", color = c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo)
                    Text(
                        if (permanecer) "Fica salvo em \"Logar com\" para entrar com um toque." else "A sessão termina em 12 horas.",
                        color = c.texto3, style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Switch(checked = permanecer, onCheckedChange = { permanecer = it }, colors = SwitchDefaults.colors(checkedThumbColor = c.sobreOuro, checkedTrackColor = c.ouro))
            }

            erro?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = c.vermelho, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(20.dp))
            PwBotaoOuro(if (carregando) "Entrando…" else "Entrar", onClick = { entrar() }, habilitado = podeEntrar, icone = PwIcones.ChevronsRight)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LinhaConta(conta: ContaSalva, onUsar: () -> Unit, onRemover: () -> Unit) {
    val c = Pw.cores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onUsar).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(c.gradOuro), contentAlignment = Alignment.Center) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(c.fundo2), contentAlignment = Alignment.Center) {
                if (conta.fotoUrl != null) {
                    AsyncImage(conta.fotoUrl, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        (conta.nome ?: conta.email).take(1).uppercase(),
                        style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 17.sp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(conta.nome ?: conta.email.substringBefore("@"), color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (conta.refreshToken != null) "${conta.email} · um toque" else conta.email,
                color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Box(
            Modifier.size(34.dp).clip(CircleShape).border(1.dp, c.borda, CircleShape).clickable(onClick = onRemover),
            contentAlignment = Alignment.Center,
        ) { Icon(PwIcones.X, "Remover conta", tint = c.texto3, modifier = Modifier.size(15.dp)) }
    }
}
