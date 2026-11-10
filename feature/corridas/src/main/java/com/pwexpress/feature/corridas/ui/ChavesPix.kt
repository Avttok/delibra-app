package com.pwexpress.feature.corridas.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.perfil.ChavePix
import com.pwexpress.core.perfil.IdentificadorPix
import com.pwexpress.core.perfil.IdentificadorPix.Tipo
import com.pwexpress.core.perfil.Perfis
import kotlinx.coroutines.launch

internal fun iconePix(tipo: String): ImageVector = when (tipo) {
    "cpf" -> PwIcones.User; "cnpj" -> PwIcones.Building2; "telefone" -> PwIcones.Smartphone; "email" -> PwIcones.Mail; else -> PwIcones.KeyRound
}

/**
 * Chaves PIX na Carteira: quantas quiser, uma é a principal (para onde vão os repasses).
 * O tipo é reconhecido sozinho pelo que é digitado.
 */
@Composable
fun CartaoChavesPix() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var chaves by remember { mutableStateOf<List<ChavePix>?>(null) }
    var adicionando by remember { mutableStateOf(false) }
    var removendo by remember { mutableStateOf<ChavePix?>(null) }
    suspend fun recarregar() { chaves = Perfis.minhasChavesPix() ?: chaves.orEmpty() }
    LaunchedEffect(Unit) { recarregar() }

    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwRotulo("Chaves PIX", Modifier.weight(1f))
            PwBotaoIcone(PwIcones.Plus, "Adicionar chave PIX", { adicionando = true }, ativo = true, tamanho = 36.dp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            if (chaves.isNullOrEmpty()) "Cadastre a chave para onde seus ganhos vão quando a corrida terminar." else "Os repasses vão para a chave principal.",
            color = c.texto2, fontSize = 13.sp, fontFamily = Archivo,
        )
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            chaves.orEmpty().forEach { ch ->
                LinhaChave(
                    ch,
                    onPrincipal = { escopo.launch { Perfis.definirChavePixPrincipal(ch.id).onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; recarregar() } },
                    onRemover = { removendo = ch },
                )
            }
        }
    }

    if (adicionando) {
        JanelaNovaChave(onFechar = { adicionando = false }) { escopo.launch { recarregar() } }
    }
    removendo?.let { ch ->
        PwJanela("Remover chave?", onFechar = { removendo = null }) {
            Text(IdentificadorPix.formatar(ch.chave, ch.tipo) + if (ch.principal) "\nEra a principal: a próxima chave assume." else "", color = c.texto2)
            PwBotaoPerigo("Remover", onClick = {
                removendo = null
                escopo.launch { Perfis.removerChavePix(ch.id).onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; recarregar() }
            }, cheio = true, modifier = Modifier.fillMaxWidth())
            PwBotaoVidro("Manter", onClick = { removendo = null }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LinhaChave(ch: ChavePix, onPrincipal: () -> Unit, onRemover: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val forma = RoundedCornerShape(16.dp)
    val tipo = Tipo.de(ch.tipo)
    Row(
        Modifier.fillMaxWidth().clip(forma).background(if (ch.principal) c.ouro.copy(alpha = 0.10f) else c.vidroSutil)
            .border(1.dp, if (ch.principal) c.ouro.copy(alpha = 0.5f) else c.borda, forma).padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.ouro.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(iconePix(ch.tipo), null, tint = c.ouro, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(tipo?.rotulo ?: ch.tipo, color = c.texto3, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                if (ch.principal) { Spacer(Modifier.width(6.dp)); PwSelo("Principal", c.ouro, icone = PwIcones.Star) }
            }
            Text(IdentificadorPix.formatar(ch.chave, ch.tipo), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(ch.titular, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!ch.principal) PwBotaoIcone(PwIcones.Star, "Tornar principal", onPrincipal, tamanho = 36.dp)
        PwBotaoIcone(PwIcones.Copy, "Copiar chave", {
            (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Chave PIX", ch.chave))
            PwToast.mostrar("Chave PIX copiada.")
        }, tamanho = 36.dp)
        PwBotaoIcone(PwIcones.Trash2, "Remover chave", onRemover, tamanho = 36.dp)
    }
}

@Composable
private fun JanelaNovaChave(onFechar: () -> Unit, onSalva: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var chave by remember { mutableStateOf("") }
    var titular by remember { mutableStateOf(Perfis.perfil.value?.nome.orEmpty()) }
    var escolhido by remember { mutableStateOf<Tipo?>(null) }
    var salvando by remember { mutableStateOf(false) }
    val leitura = IdentificadorPix.ler(chave)
    // se a pessoa tocou num dos tipos possíveis, vale a escolha; senão, o mais provável
    val tipo = escolhido?.takeIf { it in leitura.tipos } ?: leitura.tipo

    PwJanela("Nova chave PIX", onFechar = onFechar) {
        PwCampo(chave, { chave = it; escolhido = null }, "Chave", dica = "CPF, CNPJ, celular, e-mail ou aleatória")
        // o tipo reconhecido aparece embaixo do campo
        when {
            chave.isBlank() -> Text("O tipo é reconhecido sozinho.", color = c.texto3, fontSize = 12.5.sp)
            leitura.erro != null -> Text(leitura.erro.orEmpty(), color = c.vermelho, fontSize = 12.5.sp)
            leitura.ambiguo -> {
                Text("Serve como CPF e como celular. Qual é?", color = c.texto2, fontSize = 12.5.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    leitura.tipos.forEach { t -> OpcaoTipo(t, t == tipo) { escolhido = t } }
                }
            }
            tipo != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconePix(tipo.valor), null, tint = c.verde, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("${tipo.rotulo} · ${IdentificadorPix.formatar(IdentificadorPix.normalizar(chave, tipo), tipo.valor)}", color = c.verde, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        PwCampo(titular, { titular = it }, "Nome do titular")
        PwBotaoOuro(
            if (salvando) "Salvando…" else "Adicionar chave", icone = PwIcones.Check, altura = 52.dp,
            habilitado = tipo != null && titular.trim().length >= 3 && !salvando,
            onClick = {
                val t = tipo ?: return@PwBotaoOuro
                salvando = true
                escopo.launch {
                    Perfis.adicionarChavePix(t.valor, IdentificadorPix.normalizar(chave, t), titular.trim())
                        .onSuccess { PwToast.mostrar("${t.rotulo} cadastrado.", titulo = "Chave PIX salva"); onSalva(); onFechar() }
                        .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
                    salvando = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OpcaoTipo(t: Tipo, ativo: Boolean, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(99.dp)
    Row(
        Modifier.clip(forma).background(if (ativo) c.ouro.copy(alpha = 0.18f) else c.vidroSutil)
            .border(1.dp, if (ativo) c.ouro else c.borda, forma).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(iconePix(t.valor), null, tint = if (ativo) c.ouro else c.texto2, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(t.rotulo, color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
