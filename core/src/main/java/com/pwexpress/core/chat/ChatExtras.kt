package com.pwexpress.core.chat

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwConfirmar
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/** Grava áudio curto (AAC em .m4a) para o chat. */
internal class GravadorAudio(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    private var arquivo: File? = null

    fun comecar(): Boolean = runCatching {
        val f = File(ctx.cacheDir, "chat-${System.currentTimeMillis()}.m4a")
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(48_000)
        r.setAudioSamplingRate(22_050)
        r.setOutputFile(f.absolutePath)
        r.prepare(); r.start()
        rec = r; arquivo = f
        true
    }.getOrElse { cancelar(); false }

    /** Para e devolve o arquivo (null se deu errado). */
    fun parar(): File? {
        val r = rec ?: return null
        val ok = runCatching { r.stop() }.isSuccess
        runCatching { r.release() }
        rec = null
        return arquivo.takeIf { ok }.also { if (!ok) arquivo?.delete(); arquivo = null }
    }

    fun cancelar() {
        runCatching { rec?.stop() }; runCatching { rec?.release() }
        rec = null; arquivo?.delete(); arquivo = null
    }
}

/** Áudio dentro do balão: tocar/pausar e a barra de progresso. */
@Composable
internal fun AudioChat(a: Anexo) {
    val c = Pw.cores
    if (a.expirado || a.path == null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Mic, null, tint = c.texto3, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(6.dp))
            Text("Áudio que já não fica guardado", color = c.texto3, fontSize = 13.sp)
        }
        return
    }
    val escopo = rememberCoroutineScope()
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var tocando by remember { mutableStateOf(false) }
    var progresso by remember { mutableFloatStateOf(0f) }
    var carregando by remember { mutableStateOf(false) }
    DisposableEffect(a.path) { onDispose { runCatching { player?.release() }; player = null } }
    LaunchedEffect(tocando) {
        while (tocando) {
            val p = player ?: break
            progresso = runCatching { p.currentPosition.toFloat() / p.duration.coerceAtLeast(1) }.getOrDefault(0f)
            delay(200)
        }
    }
    Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        PwBotaoIcone(if (tocando) PwIcones.Pause else PwIcones.Play, if (tocando) "Pausar" else "Ouvir", {
            val p = player
            if (p != null) { if (tocando) { p.pause(); tocando = false } else { p.start(); tocando = true }; return@PwBotaoIcone }
            carregando = true
            escopo.launch {
                val url = Chat.linkFoto(a.path)
                if (url == null) { PwToast.mostrar("Não deu para abrir o áudio.", TipoAviso.ERRO); carregando = false; return@launch }
                runCatching {
                    MediaPlayer().apply {
                        setDataSource(url)
                        setOnPreparedListener { start(); tocando = true; carregando = false }
                        setOnCompletionListener { tocando = false; progresso = 0f; seekTo(0) }
                        prepareAsync()
                    }
                }.onSuccess { player = it }.onFailure { carregando = false; PwToast.mostrar("Não deu para tocar o áudio.", TipoAviso.ERRO) }
            }
        }, tamanho = 36.dp, ativo = tocando, habilitado = !carregando)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.width(130.dp).height(4.dp).clip(RoundedCornerShape(99.dp)).background(c.borda)) {
            Box(Modifier.fillMaxWidth(progresso.coerceIn(0f, 1f)).height(4.dp).clip(RoundedCornerShape(99.dp)).background(c.ouro))
        }
        Spacer(Modifier.width(8.dp))
        Text("${(a.duracao ?: 0.0).toInt()}s", color = c.texto2, fontSize = 12.sp)
    }
}

/** Quem está na conversa; quem administra adiciona, tira, faz admin e renomeia. */
@Composable
internal fun MembrosChat(id: String, conv: Conversa?, membros: List<MembroChat>, meuId: String?, gerencia: Boolean, contatos: List<Contato>, onMudou: () -> Unit, onSaiu: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var adicionar by remember { mutableStateOf(false) }
    var sair by remember { mutableStateOf(false) }
    val grupo = conv?.tipo == "grupo" || conv?.tipo == "corrida"
    fun rodar(acao: String, usuario: String? = null) = escopo.launch {
        Chat.grupo(id, acao, usuario).onSuccess { if (acao == "sair") onSaiu() else onMudou() }.onFailure { PwToast.mostrar(it.message ?: "Só quem administra o grupo faz isso.", TipoAviso.ERRO) }
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(16.dp))
            .heightIn(max = 260.dp).verticalScroll(rememberScrollState()).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Users, null, tint = c.ouro, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
            Text("${membros.size} pessoas", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (grupo && gerencia) PwBotaoIcone(PwIcones.Plus, "Adicionar pessoa", { adicionar = !adicionar }, tamanho = 32.dp)
            if (conv?.tipo == "grupo") PwBotaoIcone(PwIcones.LogOut, "Sair do grupo", { sair = true }, tamanho = 32.dp)
        }
        if (adicionar) contatos.filter { k -> membros.none { it.id == k.id } }.take(30).forEach { k ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { adicionar = false; rodar("adicionar", k.id) }.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                PwAvatar(k.nome, k.foto, tamanho = 26.dp); Spacer(Modifier.width(8.dp))
                Text(k.nome, color = c.texto, fontSize = 13.5.sp, modifier = Modifier.weight(1f)); Icon(PwIcones.Plus, null, tint = c.ouro, modifier = Modifier.size(16.dp))
            }
        }
        membros.forEach { mb ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwAvatar(mb.nome, mb.foto, tamanho = 28.dp); Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(mb.nome, color = c.texto, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                        if (mb.admin) { Spacer(Modifier.width(6.dp)); Icon(PwIcones.Shield, "Admin", tint = c.ouro, modifier = Modifier.size(13.dp)) }
                    }
                    SelosCargo(mb.papeis)
                }
                if (grupo && gerencia && mb.id != meuId) {
                    PwBotaoIcone(PwIcones.Shield, if (mb.admin) "Tirar admin" else "Fazer admin", { rodar("admin", mb.id) }, tamanho = 30.dp, ativo = mb.admin)
                    Spacer(Modifier.width(4.dp))
                    PwBotaoIcone(PwIcones.UserX, "Tirar do grupo", { rodar("remover", mb.id) }, tamanho = 30.dp)
                }
            }
        }
    }
    if (sair) PwConfirmar("Sair do grupo?", "Você para de receber as mensagens deste grupo.", "Sair", onConfirmar = { sair = false; rodar("sair") }, onCancelar = { sair = false })
}
