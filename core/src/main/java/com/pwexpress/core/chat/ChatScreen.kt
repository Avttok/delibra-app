package com.pwexpress.core.chat

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwCabecalho
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwContador
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

private val hora = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))
private val diaHora = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR"))
private fun quando(ms: Long?): String {
    if (ms == null) return ""
    val hoje = System.currentTimeMillis() / 86_400_000 == ms / 86_400_000
    return if (hoje) hora.format(java.util.Date(ms)) else diaHora.format(java.util.Date(ms))
}

fun iconeAnexo(tipo: String): ImageVector = when (tipo) {
    "pedido" -> PwIcones.ClipboardList
    "rota" -> PwIcones.Route
    "cliente" -> PwIcones.Building2
    "pessoa" -> PwIcones.UserRound
    "local" -> PwIcones.MapPin
    "foto" -> PwIcones.Image
    else -> PwIcones.Paperclip
}

/** Botão do topo com o número de mensagens não lidas. */
@Composable
fun BotaoChatTopo(onClick: () -> Unit) {
    val n by Chat.naoLidas.collectAsState()
    Box {
        PwBotaoIcone(PwIcones.MessagesSquare, "Conversas", onClick)
        if (n > 0) PwContador(n, Modifier.align(Alignment.TopEnd))
    }
}

/**
 * Conversas da equipe (geral, diretas e grupos) com fotos e cartões de pedido, rota, cliente, pessoa e local.
 * [onAbrirAnexo] abre o que foi compartilhado (no gestor: a tela do pedido, da rota…).
 */
@Composable
fun ChatScreen(meuId: String?, gestor: Boolean, onVoltar: () -> Unit, modifier: Modifier = Modifier, onAbrirAnexo: (Anexo) -> Unit = {}, dev: Boolean = false) {
    val c = Pw.cores
    val conversas by Chat.conversas.collectAsState()
    val contatos by Chat.contatos.collectAsState()
    val pendente by Chat.anexoPendente.collectAsState()
    val pedida by Chat.conversaPedida.collectAsState()
    var aberta by remember { mutableStateOf<String?>(null) }
    var novo by remember { mutableStateOf<String?>(null) } // "direta" | "grupo"
    var carregou by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    // Dev: alterna entre as conversas dele e todas as da plataforma (lê e apaga)
    var todas by remember { mutableStateOf(false) }
    var listaDev by remember { mutableStateOf<List<Conversa>>(emptyList()) }
    LaunchedEffect(todas) { if (todas) Chat.todasDev().onSuccess { listaDev = it } }

    LaunchedEffect(Unit) {
        Chat.carregarContatos()
        while (true) { Chat.carregar(); carregou = true; delay(8_000) }
    }
    LaunchedEffect(pedida) { pedida?.let { aberta = it; Chat.conversaPedida.value = null } }
    BackHandler(enabled = aberta != null || novo != null) { if (novo != null) novo = null else aberta = null }

    val conv = conversas.find { it.id == aberta } ?: listaDev.find { it.id == aberta }
    Column(modifier.fillMaxSize().background(c.fundo)) {
        when {
            aberta != null -> ConversaAberta(aberta!!, conv, meuId, gestor, contatos, onVoltar = { aberta = null }, onAbrirAnexo = onAbrirAnexo, dev = dev,
                onApagada = { aberta = null; escopo.launch { Chat.todasDev().onSuccess { listaDev = it } } })
            novo != null -> NovaConversa(novo!!, contatos, onVoltar = { novo = null }, onCriada = { id -> novo = null; aberta = id; escopo.launch { Chat.carregar() } })
            else -> {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    PwCabecalho("Conversas", onVoltar, subtitulo = if (todas) "Todas as conversas da plataforma" else "Equipe, diretas e grupos") {
                        if (dev) PwBotaoIcone(PwIcones.Eye, if (todas) "Minhas conversas" else "Ver todas as conversas", { todas = !todas }, ativo = todas)
                        PwBotaoIcone(PwIcones.UserPlus, "Nova conversa", { novo = "direta" })
                        if (gestor) PwBotaoIcone(PwIcones.Users, "Novo grupo", { novo = "grupo" })
                    }
                    pendente?.let { a ->
                        Spacer(Modifier.size(10.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.ouro.copy(alpha = 0.12f)).border(1.dp, c.ouro, RoundedCornerShape(14.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(iconeAnexo(a.tipo), null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                            Text("Escolha a conversa para mandar: ${a.titulo ?: a.tipo}", color = c.texto, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
                            PwBotaoIcone(PwIcones.X, "Cancelar", { Chat.anexoPendente.value = null }, tamanho = 34.dp)
                        }
                    }
                }
                if (!carregou) PwCarregando(texto = "Carregando conversas…")
                else if (conversas.isEmpty()) PwVazio("Nenhuma conversa ainda. Toque em + para falar com alguém.", Modifier.padding(16.dp), icone = PwIcones.MessagesSquare)
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(if (todas) listaDev else conversas, key = { it.id }) { cv ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { aberta = cv.id }.padding(horizontal = 8.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (cv.tipo == "direta") PwAvatar(cv.nome, cv.foto, tamanho = 46.dp, oculto = cv.oculto)
                            else Box(Modifier.size(46.dp).clip(CircleShape).background(c.gradOuro), contentAlignment = Alignment.Center) {
                                Icon(if (cv.tipo == "geral") PwIcones.Megaphone else PwIcones.Users, null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(22.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                        Text(cv.nome, color = c.texto, fontWeight = if (cv.naoLidas > 0) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                                        if (cv.tipo == "direta" && cv.papeis.isNotEmpty()) SelosCargo(cv.papeis)
                                        if (cv.silenciada) { Spacer(Modifier.width(4.dp)); Icon(PwIcones.BellOff, "Silenciada", tint = c.texto3, modifier = Modifier.size(13.dp)) }
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(quando(cv.ultimaEm), color = if (cv.naoLidas > 0) c.ouro else c.texto3, fontSize = 11.5.sp, maxLines = 1, textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.widthIn(min = 44.dp))
                                }
                                if (todas && cv.empresa != null) Text(cv.empresa + if (!cv.souMembro) " · você lê como Dev" else "", color = c.texto3, fontSize = 11.5.sp, maxLines = 1)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text((if (cv.ultimaMinha) "Você: " else "") + (cv.previa ?: if (cv.tipo == "grupo") "${cv.membros} pessoas" else "Diga oi!"),
                                        color = c.texto2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                    if (cv.naoLidas > 0 && !cv.silenciada) PwContador(cv.naoLidas)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NovaConversa(modo: String, contatos: List<Contato>, onVoltar: () -> Unit, onCriada: (String) -> Unit) {
    val c = Pw.cores
    var busca by remember { mutableStateOf("") }
    var nome by remember { mutableStateOf("") }
    val marcados = remember { mutableStateListOf<String>() }
    var enviando by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()
    val lista = contatos.filter { busca.isBlank() || "${it.nome} ${it.papel}".contains(busca.trim(), true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PwCabecalho(if (modo == "grupo") "Novo grupo" else "Nova conversa", onVoltar, subtitulo = if (modo == "grupo") "Escolha quem entra" else "Com quem você quer falar?")
        if (modo == "grupo") PwCampo(nome, { nome = it }, "Nome do grupo")
        PwCampo(busca, { busca = it }, "Buscar pessoa…")
        if (contatos.isEmpty()) PwVazio("Ninguém disponível para conversar.", icone = PwIcones.Users)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            items(lista, key = { it.id }) { p ->
                val marc = p.id in marcados
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (marc) c.ouro.copy(alpha = 0.12f) else Color.Transparent).clickable(enabled = !enviando) {
                        if (modo == "grupo") { if (marc) marcados.remove(p.id) else marcados.add(p.id) }
                        else { enviando = true; escopo.launch { Chat.abrirDireta(p.id).onSuccess(onCriada).onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; enviando = false } }
                    }.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PwAvatar(p.nome, p.foto, tamanho = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.nome, color = c.texto, fontWeight = FontWeight.SemiBold)
                        Text(p.papel + (p.status?.let { if (it == "disponivel") " · disponível" else if (it == "em_corrida") " · em corrida" else "" } ?: ""), color = c.texto2, fontSize = 12.5.sp)
                    }
                    if (modo == "grupo") Icon(if (marc) PwIcones.CircleCheck else PwIcones.Plus, null, tint = if (marc) c.ouro else c.texto3, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (modo == "grupo") PwBotaoOuro(if (enviando) "Criando…" else "Criar grupo (${marcados.size})", onClick = {
            enviando = true
            escopo.launch { Chat.criarGrupo(nome.trim().ifEmpty { "Grupo" }, marcados.toList()).onSuccess(onCriada).onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }; enviando = false }
        }, icone = PwIcones.Users, habilitado = !enviando && marcados.isNotEmpty())
    }
}

@Composable
private fun ConversaAberta(id: String, conv: Conversa?, meuId: String?, gestor: Boolean, contatos: List<Contato>, onVoltar: () -> Unit, onAbrirAnexo: (Anexo) -> Unit,
    dev: Boolean = false, onApagada: () -> Unit = {}) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var msgs by remember(id) { mutableStateOf<List<Mensagem>?>(null) }
    var texto by remember(id) { mutableStateOf("") }
    var enviando by remember { mutableStateOf(false) }
    var fotoAberta by remember { mutableStateOf<String?>(null) }
    val pendente by Chat.anexoPendente.collectAsState()
    val estado = rememberLazyListState()
    var autores by remember(id) { mutableStateOf<Map<String, Contato>>(emptyMap()) }
    LaunchedEffect(id) { autores = Chat.autores(id).associateBy { it.id } }
    val nomes = remember(contatos, autores) { contatos.associateBy { it.id } + autores }

    suspend fun recarregar() { Chat.mensagens(id).onSuccess { novo -> val antes = msgs?.size; msgs = novo; if (antes != novo.size && novo.isNotEmpty()) estado.animateScrollToItem(novo.lastIndex) } }
    LaunchedEffect(id) { while (true) { recarregar(); Chat.marcarLida(id); delay(4_000) } }
    androidx.compose.runtime.DisposableEffect(id) { Chat.conversaAtual.value = id; onDispose { if (Chat.conversaAtual.value == id) Chat.conversaAtual.value = null } }

    fun mandar(anexo: Anexo? = null) {
        val t = texto.trim()
        if (t.isEmpty() && anexo == null) return
        enviando = true
        escopo.launch {
            Chat.enviar(id, t.ifEmpty { null }, anexo).onSuccess { texto = ""; if (anexo != null) Chat.anexoPendente.value = null; recarregar() }
                .onFailure { PwToast.mostrar(it.message ?: "Não foi.", TipoAviso.ERRO) }
            enviando = false
        }
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        enviando = true
        escopo.launch {
            val bytes = withContext(Dispatchers.IO) { comprimir(ctx, uri) }
            if (bytes == null) PwToast.mostrar("Não deu para ler a foto.", TipoAviso.ERRO)
            else Chat.enviarFoto(id, bytes, texto.trim().ifEmpty { null }).onSuccess { texto = ""; recarregar() }.onFailure { PwToast.mostrar(it.message ?: "Não foi.", TipoAviso.ERRO) }
            enviando = false
        }
    }

    Column(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            PwCabecalho(conv?.nome ?: "Conversa", onVoltar, subtitulo = when (conv?.tipo) { "geral" -> "Toda a equipe"; "grupo" -> "${conv.membros} pessoas"; else -> nomes[conv?.outro]?.papel }) {
                if (conv?.tipo == "direta") SelosCargo(nomes[conv.outro]?.papeis.orEmpty())
                if (conv?.tipo == "direta" && conv.outro != null) PwBotaoIcone(PwIcones.UserRound, "Ver perfil", { com.pwexpress.core.perfil.PerfilAberto.abrir(conv.outro) })
                if (conv != null && conv.souMembro) PwBotaoIcone(if (conv.silenciada) PwIcones.BellRing else PwIcones.BellOff, if (conv.silenciada) "Voltar a avisar" else "Silenciar",
                    { escopo.launch { Chat.silenciar(id, !conv.silenciada) } })
                if (dev) {
                    var confirmar by remember { mutableStateOf(false) }
                    PwBotaoIcone(PwIcones.Trash2, "Apagar conversa", { confirmar = true }, cor = Color(0xFFEF4444))
                    if (confirmar) com.pwexpress.core.designsystem.PwConfirmar("Apagar a conversa?", "Apaga a conversa inteira e todas as mensagens, para todo mundo. Não dá para desfazer.", "Apagar",
                        onConfirmar = { confirmar = false; escopo.launch { Chat.apagarConversaDev(id).onSuccess { onApagada() }.onFailure { PwToast.mostrar(it.message ?: "Não foi.", TipoAviso.ERRO) } } },
                        onCancelar = { confirmar = false })
                }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val l = msgs
            if (l == null) PwCarregando()
            else if (l.isEmpty()) PwVazio("Nenhuma mensagem ainda. Mande a primeira!", Modifier.padding(16.dp), icone = PwIcones.MessageCircle)
            else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp), state = estado, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(l, key = { _, it -> it.id }) { i, m ->
                    val minha = m.autor == meuId
                    val autor = if (minha) null else nomes[m.autor]
                    val emGrupo = !minha && conv?.tipo != "direta"
                    // foto e nome só na primeira mensagem de uma sequência da mesma pessoa
                    val primeira = l.getOrNull(i - 1)?.autor != m.autor
                    Row(Modifier.fillMaxWidth().padding(top = if (primeira && i > 0) 6.dp else 0.dp), horizontalArrangement = if (minha) Arrangement.End else Arrangement.Start, verticalAlignment = Alignment.Top) {
                        if (emGrupo) {
                            if (primeira) PwAvatar(autor?.nome ?: "?", autor?.foto, tamanho = 30.dp, oculto = autor?.oculto == true,
                                onClick = m.autor?.let { a -> { com.pwexpress.core.perfil.PerfilAberto.abrir(a) } }) else Spacer(Modifier.width(30.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        val forma = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = if (minha) 16.dp else 4.dp, bottomEnd = if (minha) 4.dp else 16.dp)
                        Column(
                            Modifier.widthIn(max = 290.dp).clip(forma)
                                .background(if (minha) c.ouro.copy(alpha = 0.18f) else c.vidroForte)
                                .border(1.dp, if (minha) c.ouro.copy(alpha = 0.5f) else c.borda, forma)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            if (emGrupo && primeira) Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(autor?.nome ?: "Alguém", color = c.ouro, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).clickable(enabled = m.autor != null) { m.autor?.let { com.pwexpress.core.perfil.PerfilAberto.abrir(it) } })
                                SelosCargo(autor?.papeis.orEmpty())
                            }
                            if (m.apagada) Text("Mensagem apagada", color = c.texto3, fontSize = 13.sp)
                            else {
                                m.anexo?.let { a ->
                                    if (a.tipo == "foto" && a.caminho != null) FotoChat(a.caminho) { fotoAberta = it }
                                    else CartaoAnexo(a) { onAbrirAnexo(a) }
                                }
                                m.texto?.let { Text(it, color = c.texto, fontSize = 15.sp) }
                            }
                            Row(Modifier.align(if (minha) Alignment.End else Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
                                Text(quando(m.quando), color = c.texto3, fontSize = 10.5.sp)
                                if (!m.apagada && (minha || gestor || dev)) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(PwIcones.Trash2, "Apagar mensagem", tint = c.texto3, modifier = Modifier.size(14.dp).clip(CircleShape).clickable { escopo.launch { Chat.apagar(m.id); recarregar() } })
                                }
                            }
                        }
                    }
                }
            }
        }
        pendente?.let { a ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(14.dp)).background(c.ouro.copy(alpha = 0.12f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(iconeAnexo(a.tipo), null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text("Mandar: ${a.titulo ?: a.tipo}", color = c.texto, fontSize = 13.5.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Enviar", color = c.ouro, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(enabled = !enviando) { mandar(a) }.padding(6.dp))
                PwBotaoIcone(PwIcones.X, "Cancelar", { Chat.anexoPendente.value = null }, tamanho = 32.dp)
            }
        }
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom) {
            PwBotaoIcone(PwIcones.ImagePlus, "Mandar foto", { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, habilitado = !enviando)
            Spacer(Modifier.width(8.dp))
            val forma = RoundedCornerShape(20.dp)
            Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(forma).background(c.vidro).border(1.dp, c.borda, forma).padding(horizontal = 14.dp, vertical = 11.dp)) {
                if (texto.isEmpty()) Text("Mensagem", color = c.texto3, fontSize = 15.sp)
                BasicTextField(texto, { texto = it.take(4000) }, textStyle = TextStyle(color = c.texto, fontSize = 15.sp), cursorBrush = SolidColor(c.ouro), maxLines = 5, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.width(8.dp))
            PwBotaoIcone(PwIcones.Send, "Enviar", { mandar() }, ativo = texto.isNotBlank(), habilitado = !enviando && texto.isNotBlank())
        }
    }
    fotoAberta?.let { url ->
        BackHandler { fotoAberta = null }
        Box(Modifier.fillMaxSize().background(Color(0xEE000000)).clickable { fotoAberta = null }, contentAlignment = Alignment.Center) {
            AsyncImage(url, "Foto", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxWidth().padding(12.dp))
        }
    }
}

@Composable
private fun FotoChat(caminho: String, onAbrir: (String) -> Unit) {
    var url by remember(caminho) { mutableStateOf<String?>(null) }
    LaunchedEffect(caminho) { url = Chat.linkFoto(caminho) }
    val u = url
    Box(Modifier.size(220.dp, 180.dp).clip(RoundedCornerShape(12.dp)).background(Pw.cores.vidro).then(if (u != null) Modifier.clickable { onAbrir(u) } else Modifier), contentAlignment = Alignment.Center) {
        if (u == null) Icon(PwIcones.Image, null, tint = Pw.cores.texto3)
        else AsyncImage(u, "Foto", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
    Spacer(Modifier.size(4.dp))
}

private val STATUS_CARTAO = mapOf(
    "recebido" to "Para revisar", "aprovado" to "Aprovado", "roteirizado" to "Em rota montada", "em_rota" to "Em rota", "concluido" to "Concluído",
    "cancelado" to "Cancelado", "recusado" to "Recusado", "montada" to "Montada · esperando soltar", "agendada" to "Planejada", "reservada" to "Reservada",
    "aguardando_motoboy" to "Procurando motoboy", "sem_motoboy" to "Sem motoboy", "aceita" to "Aceita", "em_andamento" to "Em andamento", "concluida" to "Concluída",
    "cancelada" to "Cancelada", "disponivel" to "Online", "em_corrida" to "Em corrida", "offline" to "Offline",
)
private fun corStatusCartao(s: String?): Color = when (s) {
    "concluido", "concluida", "disponivel" -> Color(0xFF22C55E); "cancelado", "cancelada", "recusado", "sem_motoboy" -> Color(0xFFEF4444)
    "em_rota", "em_andamento", "aceita", "em_corrida" -> Color(0xFF38BDF8); "offline" -> Color(0xFF8A8A8E); else -> Color(0xFFD6B04C)
}

/** Cartão do que foi compartilhado: título, status ao vivo e as linhas do essencial (o motoboy vê um resumo, sem valores). */
@Composable
private fun CartaoAnexo(a: Anexo, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    var info by remember(a.tipo, a.id) { mutableStateOf<CartaoInfo?>(null) }
    LaunchedEffect(a.tipo, a.id) { if (a.id != null && a.tipo in setOf("pedido", "rota", "cliente", "pessoa")) info = Chat.cartao(a.tipo, a.id) }
    val i = info
    Column(Modifier.widthIn(min = 230.dp).clip(forma).background(c.fundo2).border(1.dp, c.borda, forma).clickable(onClick = onClick).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (a.tipo == "pessoa" && i != null) PwAvatar(i.titulo ?: a.titulo ?: "?", i.foto, tamanho = 36.dp, oculto = i.oculto)
            else Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(c.ouro.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(iconeAnexo(a.tipo), null, tint = c.ouro, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                PwRotulo(mapOf("pedido" to "Pedido", "rota" to "Rota", "cliente" to "Cliente", "pessoa" to "Pessoa", "local" to "Local").getOrElse(a.tipo) { "Anexo" })
                Text(i?.titulo ?: a.titulo ?: "—", color = c.texto, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                (i?.sub ?: a.sub)?.takeIf { it.isNotBlank() }?.let { Text(it, color = c.texto2, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
        i?.status?.let { st ->
            val cor = corStatusCartao(st)
            Text(STATUS_CARTAO[st] ?: st.replace("_", " "), color = cor, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(cor.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 3.dp))
        }
        if (i?.semAcesso == true) Text("Você não tem acesso a este cartão.", color = c.texto3, fontSize = 12.sp)
        i?.linhas?.forEach { (ic, rot, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(iconeLinha(ic), null, tint = c.texto3, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(rot, color = c.texto3, fontSize = 12.sp, modifier = Modifier.width(78.dp), maxLines = 1)
                Text(v, color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (i?.resumo == true) Text("Resumo para a equipe", color = c.texto3, fontSize = 10.5.sp)
    }
    Spacer(Modifier.size(4.dp))
}

private fun iconeLinha(nome: String): ImageVector = when (nome) {
    "bike" -> PwIcones.Bike; "list-checks" -> PwIcones.ListChecks; "ruler" -> PwIcones.Ruler; "map-pin" -> PwIcones.MapPin
    "calendar-clock" -> PwIcones.CalendarClock; "building-2" -> PwIcones.Building2; "wallet" -> PwIcones.Wallet; "package" -> PwIcones.Package
    "clock" -> PwIcones.Clock; "info" -> PwIcones.Info; "phone" -> PwIcones.Phone; "clipboard-list" -> PwIcones.ClipboardList
    "shield-check" -> PwIcones.ShieldCheck; "route" -> PwIcones.Route; else -> PwIcones.Info
}

/** Foto para o chat: até 1600 px no lado maior, JPEG 82%. */
private fun comprimir(ctx: android.content.Context, uri: Uri): ByteArray? = runCatching {
    val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return null
    val maior = maxOf(bmp.width, bmp.height)
    val esc = if (maior > 1600) 1600f / maior else 1f
    val b = if (esc < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * esc).toInt(), (bmp.height * esc).toInt(), true) else bmp
    java.io.ByteArrayOutputStream().also { b.compress(Bitmap.CompressFormat.JPEG, 82, it) }.toByteArray()
}.getOrNull()


/** Selo do cargo de maior hierarquia (Dev > Gestor > Supervisor > Motoboy > Cliente) ao lado do nome. */
@Composable
fun SelosCargo(papeis: List<String>) {
    if (papeis.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 6.dp)) {
        listOf("dev" to ("Dev" to Color(0xFFEF4444)), "gestor" to ("Gestor" to Pw.cores.ouro), "supervisor" to ("Supervisor" to Color(0xFF34D399)),
            "motoboy" to ("Motoboy" to Color(0xFF22C55E)), "cliente" to ("Cliente" to Color(0xFF38BDF8)))
            .firstOrNull { it.first in papeis }?.let { listOf(it) }.orEmpty().forEach { (_, rc) ->
                val (r, cor) = rc
                Text(r, color = cor, fontSize = 9.5.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(99.dp)).border(1.dp, cor, RoundedCornerShape(99.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
            }
    }
}
