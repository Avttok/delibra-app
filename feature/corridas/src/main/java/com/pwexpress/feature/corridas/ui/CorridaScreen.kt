package com.pwexpress.feature.corridas.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPonto
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwValor
import androidx.compose.foundation.clickable
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.corridas.data.Corrida
import com.pwexpress.feature.corridas.data.Corridas
import com.pwexpress.feature.corridas.data.Parada
import com.pwexpress.feature.corridas.data.Posicao
import com.pwexpress.feature.corridas.data.PosicaoAtual
import com.pwexpress.feature.corridas.data.StatusParada
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.corridas.data.exigencias
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * Corrida em andamento: mapa com a rota dourada, uma parada por vez.
 * [admin] = conta gestor + motoboy (pode ignorar a localização em corridas de teste).
 */
@Composable
fun CorridaScreen(
    corrida: Corrida, admin: Boolean = false, modifier: Modifier = Modifier,
    /** Chat à mão no meio da corrida (conversa com o gestor e a equipe). */
    chatLigado: Boolean = false,
    /** Conta gestor + motoboy: vai ao painel sem largar a corrida. */
    onPainelGestor: (() -> Unit)? = null,
) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val scope = rememberCoroutineScope()
    val parada = corrida.paradaAtual
    var enviando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var comprovanteAberto by remember { mutableStateOf(false) }
    var posicao by remember { mutableStateOf<Posicao?>(null) }
    var minimizado by com.pwexpress.core.designsystem.lembrarLigado("corrida_minimizada", false)

    // posição ao vivo enquanto a tela está aberta
    LaunchedEffect(Unit) {
        while (true) {
            PosicaoAtual.obter(ctx, 6_000)?.let { posicao = it }
            delay(5_000)
        }
    }

    val distancia = if (parada != null) posicao?.let { PosicaoAtual.distanciaM(it, parada.lat, parada.lng) } else null
    val tolerancia = (posicao?.precisaoM ?: 0f).coerceAtMost(80f)
    val noLocal = !corrida.exigirNoLocal || (distancia != null && distancia - tolerancia <= corrida.raioChegadaM)
    val podeIgnorar = admin && corrida.teste && corrida.exigirNoLocal

    fun avancar(acao: String, ignorar: Boolean = false) {
        val p = parada ?: return
        enviando = true
        erro = null
        scope.launch {
            val agora = PosicaoAtual.obter(ctx, 8_000) ?: posicao
            posicao = agora ?: posicao
            Corridas.avancar(p.id, acao, posicao = agora, ignorarLocal = ignorar).onFailure { erro = it.message }
            enviando = false
        }
    }

    if (comprovanteAberto && parada != null) {
        ComprovanteEntrega(corrida, parada, posicao, admin, onFechar = { comprovanteAberto = false }, modifier = modifier)
        return
    }

    val pinos = remember(corrida) {
        corrida.paradas.map { PinoMapa(it.lat, it.lng, it.tipo == TipoParada.COLETA, it.id == corrida.paradaAtual?.id, it.status == StatusParada.CONCLUIDA) }
    }

    var focoT by remember(corrida.id) { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }
    Column(modifier.fillMaxSize()) {
        // ---------------------------------------------------------- navegação curva a curva (dentro do app)
        if (parada != null && !comprovanteAberto) {
            NavegacaoBanner(parada.id, parada.lat to parada.lng, Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp))
        }
        // ---------------------------------------------------------- mapa
        Box(Modifier.fillMaxWidth().then(if (minimizado) Modifier.weight(1f) else Modifier.height(320.dp))) {
            // feito (pelas ruas, do servidor) + onde estou agora; a fazer: daqui até as paradas que faltam
            val feito = com.pwexpress.core.mapa.rememberTrilhaCorrida(corrida.id, ativa = true)
            val eu = posicao?.let { it.lat to it.lng }
            PwMapa(botaoTelaCheiaEm = Alignment.CenterStart, tituloTelaCheia = "Corrida ${corrida.numeroExibicao}",
                botoesTelaCheia = if (parada != null) ({
                    com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Navigation, "Abrir no Maps", { abrirMaps(ctx, parada) }, tamanho = 44.dp)
                    com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Route, "Abrir no Waze", { abrirWaze(ctx, parada) }, tamanho = 44.dp)
                }) else null,
                paradas = pinos,
                rota = corrida.rotaPlanejada,
                minhaPosicao = eu,
                modifier = Modifier.fillMaxSize(),
                focar = focoT,
                trilhas = if (feito.size >= 2) listOf(com.pwexpress.core.mapa.TrilhaMapa(feito + listOfNotNull(eu))) else emptyList(),
                restanteDe = eu,
            )
            // barra de vidro sobre o mapa: corrida + selo de teste à esquerda, valor à direita
            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(12.dp)
                    .clip(RoundedCornerShape(18.dp)).background(c.vidroForte).border(1.dp, c.borda, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "CORRIDA ${corrida.numeroExibicao}", maxLines = 1, softWrap = false,
                            style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 1.sp),
                        )
                        if (corrida.teste) {
                            Spacer(Modifier.width(8.dp))
                            PwSelo("TESTE", c.violeta, icone = PwIcones.FlaskConical)
                        }
                    }
                    corrida.empresaNome?.let { Text(it, color = c.texto2, fontSize = 12.sp, fontFamily = Archivo, maxLines = 1) }
                }
                Spacer(Modifier.width(10.dp))
                if (corrida.valoresVisiveis && corrida.valorMotoboy != null) PwValor(moeda(corrida.valorMotoboy), Modifier.widthIn(max = 150.dp), tamanhoMax = 22, cor = c.texto)
                else Text("${corrida.paradas.count { it.tipo == TipoParada.ENTREGA && it.status == StatusParada.CONCLUIDA }}/${corrida.paradas.count { it.tipo == TipoParada.ENTREGA }} entregas", color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
            }
            // chat e painel do gestor, sem sair da corrida
            if (chatLigado || onPainelGestor != null) {
                val naoLidas by com.pwexpress.core.chat.Chat.naoLidas.collectAsState()
                Row(Modifier.align(Alignment.BottomStart).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (chatLigado) Box {
                        BotaoMapa("Chat", PwIcones.MessageCircle) { com.pwexpress.core.chat.Chat.abrir.value = true }
                        if (naoLidas > 0) Box(
                            Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp).size(20.dp).clip(RoundedCornerShape(99.dp)).background(c.vermelho),
                            contentAlignment = Alignment.Center,
                        ) { Text(if (naoLidas > 9) "9+" else "$naoLidas", color = androidx.compose.ui.graphics.Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    }
                    if (onPainelGestor != null) BotaoMapa("Gestor", PwIcones.ShieldCheck, onPainelGestor)
                }
            }
            // navegação externa, sempre à mão
            if (parada != null) {
                Row(Modifier.align(Alignment.BottomEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BotaoMapa("Maps", PwIcones.Navigation) { abrirMaps(ctx, parada) }
                    BotaoMapa("Waze", PwIcones.Route) { abrirWaze(ctx, parada) }
                }
            }
        }

        // ---------------------------------------------------------- versão minimizada: só o essencial
        if (minimizado && parada != null) {
            val coleta = parada.tipo == TipoParada.COLETA
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PwRotulo("${parada.ordem + 1}/${corrida.paradas.size} · ${if (coleta) "Coleta" else "Entrega"}")
                        Text(parada.endereco, color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.width(8.dp))
                    IndicadorDistancia(distancia, noLocal, corrida.exigirNoLocal)
                    Spacer(Modifier.width(8.dp))
                    BotaoMapa("", PwIcones.Maximize2) { minimizado = false }
                }
                erro?.let { Text(it, color = c.vermelho) }
                BotaoAcao(corrida, parada, enviando, noLocal, podeIgnorar, onAvancar = { a, i -> avancar(a, i) }, onComprovante = { comprovanteAberto = true })
            }
            return@Column
        }

        // ---------------------------------------------------------- etapas
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (parada != null) {
                val coleta = parada.tipo == TipoParada.COLETA
                PwCartao(destaque = true) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PwRotulo("Parada ${parada.ordem + 1} de ${corrida.paradas.size} · ${if (coleta) "Coleta" else "Entrega"}", Modifier.weight(1f))
                        IndicadorDistancia(distancia, noLocal, corrida.exigirNoLocal)
                        Spacer(Modifier.width(6.dp))
                        BotaoMapa("", PwIcones.EyeOff) { minimizado = true }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(parada.endereco, style = MaterialTheme.typography.titleLarge, color = c.texto)
                    parada.contatoNome?.let { Text("Contato: $it", color = c.texto2) }
                    parada.instrucoes?.let { Text(it, color = c.ouro) }
                    if (parada.janelaDe != null || parada.janelaAte != null) {
                        Text("Atende das ${parada.janelaDe ?: "…"} às ${parada.janelaAte ?: "…"}", color = c.texto2, fontSize = 13.sp)
                    }
                    corrida.exigencias(parada).takeIf { it.alguma }?.let { e ->
                        Text("Confirmar com: " + listOfNotNull("foto".takeIf { e.foto }, "nome".takeIf { e.nome }, "assinatura".takeIf { e.assinatura }, "código".takeIf { e.codigo }).joinToString(", "),
                            color = c.texto2, fontSize = 13.sp)
                    }
                    ReferenciasParada(parada.referencias)
                    ItensParada(parada)
                    parada.contatoTelefone?.let { tel ->
                        Spacer(Modifier.height(10.dp))
                        PwBotaoVidro("Ligar para $tel", onClick = { ligar(ctx, tel) }, icone = PwIcones.Phone)
                    }
                }

                erro?.let { Text(it, color = c.vermelho) }
                BotaoAcao(corrida, parada, enviando, noLocal, podeIgnorar, onAvancar = { a, i -> avancar(a, i) }, onComprovante = { comprovanteAberto = true })
            }
            PwCartao {
                PwRotulo("Trajeto")
                Spacer(Modifier.height(6.dp))
                corrida.paradas.forEach { p ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { focoT = com.pwexpress.core.mapa.FocoMapa(p.lat, p.lng) }.padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        PwPonto(
                            when {
                                p.status == StatusParada.CONCLUIDA -> c.verde
                                p.id == parada?.id -> c.ouro
                                else -> c.bordaForte
                            },
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "${p.ordem + 1}. ${if (p.tipo == TipoParada.COLETA) "Coleta" else "Entrega"}" +
                                    if (p.status == StatusParada.CONCLUIDA) " · concluída" else "",
                                fontWeight = FontWeight.SemiBold, color = c.texto,
                            )
                            Text(p.endereco, color = c.texto2, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                corrida.observacoes?.let {
                    Spacer(Modifier.height(6.dp))
                    Text("Obs.: $it", color = c.ouro)
                }
            }
        }
    }
}


/** Ação principal da parada (+ "ignorar localização" do admin em corrida de teste). */
@Composable
private fun BotaoAcao(
    corrida: Corrida,
    parada: Parada,
    enviando: Boolean,
    noLocal: Boolean,
    podeIgnorar: Boolean,
    onAvancar: (acao: String, ignorar: Boolean) -> Unit,
    onComprovante: () -> Unit,
) {
    val c = Pw.cores
    val coleta = parada.tipo == TipoParada.COLETA
    val precisa = corrida.exigencias(parada).alguma
    val (rotulo, icone, acao) = when {
        parada.status != StatusParada.CHEGOU -> Triple("Cheguei no local", PwIcones.MapPin, { onAvancar("chegou", false) })
        coleta && precisa -> Triple("Confirmar coleta", PwIcones.PackageCheck, onComprovante)
        coleta -> Triple("Coletado", PwIcones.PackageCheck, { onAvancar("concluir", false) })
        precisa -> Triple("Finalizar entrega", PwIcones.Check, onComprovante)
        else -> Triple("Entregue", PwIcones.CircleCheck, { onAvancar("concluir", false) })
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PwBotaoOuro(
            when {
                enviando -> "Enviando…"
                !noLocal -> "Aproxime-se do endereço"
                else -> rotulo
            },
            onClick = acao,
            icone = if (noLocal) icone else PwIcones.MapPinned,
            habilitado = !enviando && noLocal,
            altura = 64.dp,
            tamanhoTexto = 19,
        )
        if (podeIgnorar && !noLocal) {
            PwBotaoVidro(
                "Ignorar localização (teste)",
                onClick = {
                    if (parada.status != StatusParada.CHEGOU) onAvancar("chegou", true)
                    else if (precisa) onComprovante()
                    else onAvancar("concluir", true)
                },
                icone = PwIcones.FlaskConical,
                cor = c.violeta,
                habilitado = !enviando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** Botão redondo de vidro sobre o mapa. */
@Composable
private fun BotaoMapa(texto: String, icone: ImageVector, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(99.dp)
    Row(
        Modifier.clip(forma).background(c.vidroForte).border(1.dp, c.bordaForte, forma).clickable(onClick = onClick)
            .padding(horizontal = if (texto.isEmpty()) 10.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, texto.ifEmpty { null }, tint = c.ouro, modifier = Modifier.size(18.dp))
        if (texto.isNotEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(texto, color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = Archivo)
        }
    }
}

@Composable
private fun IndicadorDistancia(distancia: Float?, noLocal: Boolean, exige: Boolean) {
    val c = Pw.cores
    val texto = when {
        distancia == null -> "Localizando…"
        distancia < 1000 -> "${distancia.toInt()} m"
        else -> String.format(localeBr, "%.1f km", distancia / 1000)
    }
    val cor = when {
        distancia == null -> c.texto3
        noLocal && exige -> c.verde
        exige -> c.vermelho
        else -> c.texto2
    }
    PwSelo(if (noLocal && exige && distancia != null) "No local · $texto" else texto, cor, icone = PwIcones.LocateFixed)
}

// ------------------------------------------------------------------ comprovante

@Composable
private fun ComprovanteEntrega(corrida: Corrida, parada: Parada, posicaoInicial: Posicao?, admin: Boolean, onFechar: () -> Unit, modifier: Modifier) {
    val ctx = LocalContext.current
    val c = Pw.cores
    val scope = rememberCoroutineScope()
    var nome by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var foto by remember { mutableStateOf<ByteArray?>(null) }
    var arquivoFoto by remember { mutableStateOf<File?>(null) }
    val assinatura = rememberEstadoAssinatura()
    var enviando by remember { mutableStateOf(false) }
    var etapa by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    val ex = corrida.exigencias(parada)
    val coleta = parada.tipo == TipoParada.COLETA

    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val arq = arquivoFoto
        if (ok && arq != null) {
            scope.launch {
                foto = runCatching { withContext(Dispatchers.Default) { Fotos.preparar(arq) } }
                    .onFailure { erro = "Não foi possível ler a foto." }.getOrNull()
                arq.delete()
            }
        }
    }

    fun finalizar() {
        erro = when {
            ex.nome && nome.isBlank() -> if (coleta) "Informe o nome de quem entregou." else "Informe o nome de quem recebeu."
            ex.codigo && codigo.length != 4 -> "Digite o código de 4 dígitos."
            ex.foto && foto == null -> "Tire a foto do comprovante."
            ex.assinatura && assinatura.vazia -> "Colete a assinatura."
            else -> null
        }
        if (erro != null) return
        enviando = true
        scope.launch {
            var fotoPath: String? = null
            var assinaturaPath: String? = null
            val f = foto
            if (ex.foto && f != null) {
                etapa = "Enviando foto…"
                fotoPath = Corridas.enviarComprovante(corrida.id, parada.id, "foto", f, "image/jpeg")
                    .onFailure { erro = it.message }.getOrNull() ?: run { enviando = false; return@launch }
            }
            if (ex.assinatura) {
                etapa = "Enviando assinatura…"
                val png = withContext(Dispatchers.Default) { assinatura.exportarPng() }
                assinaturaPath = Corridas.enviarComprovante(corrida.id, parada.id, "assinatura", png, "image/png")
                    .onFailure { erro = it.message }.getOrNull() ?: run { enviando = false; return@launch }
            }
            etapa = "Conferindo localização…"
            val posicao = PosicaoAtual.obter(ctx, 8_000) ?: posicaoInicial
            etapa = "Finalizando…"
            Corridas.avancar(
                parada.id, "concluir",
                buildJsonObject {
                    put("recebedor_nome", nome.trim())
                    put("codigo", codigo)
                    fotoPath?.let { put("foto_path", it) }
                    assinaturaPath?.let { put("assinatura_path", it) }
                },
                posicao = posicao,
                ignorarLocal = admin && corrida.teste,
            ).onSuccess { onFechar() }.onFailure { erro = it.message }
            enviando = false
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (coleta) "Comprovante de coleta" else "Comprovante de entrega", style = MaterialTheme.typography.headlineSmall, color = c.texto, modifier = Modifier.weight(1f))
            TextButton(onClick = onFechar, enabled = !enviando) { Text("Voltar", color = c.texto2) }
        }
        Text(parada.endereco, color = c.texto2)

        if (ex.nome) {
            OutlinedTextField(nome, { nome = it }, label = { Text(if (coleta) "Nome de quem entregou" else "Nome de quem recebeu") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        if (ex.codigo) {
            OutlinedTextField(
                codigo, { codigo = it.filter(Char::isDigit).take(4) }, label = { Text(if (coleta) "Código da coleta (4 dígitos)" else "Código de entrega (4 dígitos)") },
                singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (ex.foto) {
            PwCartao {
                Titulo(PwIcones.Camera, "Foto")
                foto?.let { bytes ->
                    Fotos.miniatura(bytes)?.let {
                        Image(it.asImageBitmap(), "Foto do comprovante", contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)))
                        Spacer(Modifier.height(8.dp))
                    }
                }
                PwBotaoVidro(if (foto == null) "Tirar foto" else "Tirar outra", onClick = {
                    val (arq, uri) = Fotos.novoDestino(ctx)
                    arquivoFoto = arq
                    camera.launch(uri)
                }, icone = PwIcones.Camera, modifier = Modifier.fillMaxWidth())
            }
        }
        if (ex.assinatura) {
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { Titulo(PwIcones.PenLine, "Assinatura") }
                    TextButton(onClick = { assinatura.limpar() }) { Text("Limpar", color = c.ouro) }
                }
                PainelAssinatura(assinatura)
            }
        }

        erro?.let { Text(it, color = c.vermelho) }
        PwBotaoOuro(if (enviando) etapa else if (coleta) "Confirmar coleta" else "Confirmar entrega", onClick = { finalizar() }, habilitado = !enviando, icone = PwIcones.CircleCheck, altura = 64.dp)
        Spacer(Modifier.size(12.dp))
    }
}

@Composable
private fun Titulo(icone: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(icone, null, tint = Pw.cores.ouro, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        PwRotulo(texto)
    }
}

// ------------------------------------------------------------------ navegação externa

private fun abrirMaps(ctx: Context, p: Parada) {
    val app = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${p.lat},${p.lng}"))
        .setPackage("com.google.android.apps.maps")
    val generico = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${p.lat},${p.lng}?q=${p.lat},${p.lng}"))
    runCatching { ctx.startActivity(app) }.recoverCatching { ctx.startActivity(generico) }
}

private fun abrirWaze(ctx: Context, p: Parada) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=${p.lat},${p.lng}&navigate=yes"))) }
}

private fun ligar(ctx: Context, telefone: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${telefone.filter { it.isDigit() || it == '+' }}"))) }
}

@Suppress("unused")
private val transparente = Color.Transparent
