package com.pwexpress.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.animation.core.animateFloat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.text.NumberFormat
import java.util.Locale

// =====================================================================
// Kit de componentes do app (mesma linguagem do site).
// =====================================================================

private val br = Locale.forLanguageTag("pt-BR")
fun moedaBr(v: Double?): String = if (v == null || v.isNaN()) "—" else NumberFormat.getCurrencyInstance(br).format(v)
fun numeroBr(v: Double, casas: Int = 2): String = NumberFormat.getNumberInstance(br).apply { maximumFractionDigits = casas }.format(v)

// ------------------------------------------------------------------ topo e abas

/** Topo das telas principais: logo + "Delibra" e ações à direita. */
@Composable
fun PwTopo(modifier: Modifier = Modifier, acoes: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        // a logo cede espaço (nunca a foto nem os botões)
        Box(Modifier.weight(1f).clipToBounds(), contentAlignment = Alignment.CenterStart) { PwLogo(tamanho = 22) }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = acoes)
    }
}

data class PwAba(val chave: String, val rotulo: String, val icone: ImageVector, val contador: Int = 0)

/** Barra de abas flutuante (vidro), como uma pílula no rodapé. */
@Composable
fun PwBarraAbas(abas: List<PwAba>, selecionada: String, onSelecionar: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val forma = RoundedCornerShape(26.dp)
    Row(
        modifier
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(forma)
            .background(c.vidroForte)
            .border(1.dp, c.bordaForte, forma)
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        abas.forEach { aba ->
            val ativa = aba.chave == selecionada
            val cor by animateColorAsState(if (ativa) c.ouro else c.texto3, label = "cor-aba")
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .then(if (ativa) Modifier.background(c.gradSuave) else Modifier)
                    .clickable { onSelecionar(aba.chave) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(22.dp)) {
                    Icon(aba.icone, null, tint = cor, modifier = Modifier.size(22.dp))
                    if (aba.contador > 0) PwContador(aba.contador, Modifier.align(Alignment.TopEnd).offset(x = 10.dp, y = (-7).dp))
                }
                Spacer(Modifier.height(3.dp))
                Text(aba.rotulo, color = cor, fontSize = 11.sp, fontWeight = if (ativa) FontWeight.Bold else FontWeight.Medium, fontFamily = Archivo, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/**
 * Bolinha de contador (abas, botões). O número fica no centro exato: sem o espaço extra da fonte
 * e com a linha centralizada, senão ele "cai" para baixo do círculo.
 */
@Composable
fun PwContador(n: Int, modifier: Modifier = Modifier) {
    val c = Pw.cores
    Box(
        modifier.height(18.dp).widthIn(min = 18.dp).clip(CircleShape).background(c.gradOuro)
            .border(1.5.dp, c.fundo2, CircleShape).padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (n > 99) "99+" else n.toString(),
            style = androidx.compose.ui.text.TextStyle(
                color = c.sobreOuro, fontSize = 10.sp, fontWeight = FontWeight.Black, fontFamily = Archivo, lineHeight = 10.sp,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(
                    androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.Both),
            ),
            maxLines = 1,
        )
    }
}

// ------------------------------------------------------------------ seleção

/** Botões lado a lado (um selecionado). */
@Composable
fun PwSegmentado(opcoes: List<Pair<String, ImageVector?>>, selecionado: Int, onSelecionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    Row(modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        // abas só de ícone: a que tem nome ganha espaço para o texto caber inteiro
        val misto = opcoes.any { it.first.isEmpty() } && opcoes.any { it.first.isNotEmpty() }
        opcoes.forEachIndexed { i, (rotulo, icone) ->
            val ativo = i == selecionado
            val peso by androidx.compose.animation.core.animateFloatAsState(if (misto && rotulo.isNotEmpty()) 1f + rotulo.length * 0.32f else 1f, label = "peso-aba")
            Row(
                Modifier.weight(peso).clip(RoundedCornerShape(11.dp)).then(if (ativo) Modifier.background(c.vidroForte) else Modifier)
                    .clickable { onSelecionar(i) }.padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                if (icone != null) {
                    Icon(icone, rotulo.ifEmpty { null }, tint = if (ativo) c.ouro else c.texto2, modifier = Modifier.size(16.dp))
                    if (rotulo.isNotEmpty()) Spacer(Modifier.width(6.dp))
                }
                if (rotulo.isNotEmpty()) Text(rotulo, color = if (ativo) c.ouro else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, fontFamily = Archivo, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** Filtros em fichas que rolam para o lado (com contador opcional). */
@Composable
fun PwFiltros(opcoes: List<Pair<String, Int>>, selecionado: Int, onSelecionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    Row(modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        opcoes.forEachIndexed { i, (rotulo, n) ->
            val ativo = i == selecionado
            val forma = RoundedCornerShape(99.dp)
            Row(
                Modifier.clip(forma)
                    .background(if (ativo) c.gradOuro else Brush.linearGradient(listOf(c.vidroForte, c.vidroForte)))
                    .border(1.dp, if (ativo) Color.Transparent else c.borda, forma)
                    .clickable { onSelecionar(i) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(rotulo, color = if (ativo) c.sobreOuro else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = Archivo, maxLines = 1, softWrap = false)
                if (n > 0) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(CircleShape).background(if (ativo) c.sobreOuro.copy(alpha = 0.2f) else c.ouro).padding(horizontal = 6.dp, vertical = 1.dp)) {
                        Text(n.toString(), color = if (ativo) c.sobreOuro else c.sobreOuro, fontSize = 10.5.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

/** Campo de escolha (abre uma lista). */
@Composable
fun <T> PwSelecao(rotulo: String, opcoes: List<Pair<T, String>>, valor: T?, onMudar: (T) -> Unit, modifier: Modifier = Modifier, vazio: String = "Escolha…") {
    val c = Pw.cores
    var aberto by remember { mutableStateOf(false) }
    Column(modifier) {
        if (rotulo.isNotEmpty()) { PwRotulo(rotulo); Spacer(Modifier.height(6.dp)) }
        Box {
            val forma = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(forma).background(c.campo).border(1.dp, c.bordaForte, forma).clickable { aberto = true }.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(opcoes.firstOrNull { it.first == valor }?.second ?: vazio, color = if (valor == null) c.texto3 else c.texto, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(PwIcones.ChevronDown, null, tint = c.texto3, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }, modifier = Modifier.background(c.fundo2)) {
                opcoes.forEach { (v, texto) ->
                    DropdownMenuItem(
                        text = { Text(texto, color = if (v == valor) c.ouro else c.texto, fontWeight = if (v == valor) FontWeight.Bold else FontWeight.Normal) },
                        onClick = { onMudar(v); aberto = false },
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ campos

@Composable
fun PwCampo(
    valor: String,
    onMudar: (String) -> Unit,
    rotulo: String,
    modifier: Modifier = Modifier,
    dica: String? = null,
    teclado: KeyboardType = KeyboardType.Text,
    senha: Boolean = false,
    linhas: Int = 1,
    prefixo: String? = null,
    sufixo: String? = null,
    habilitado: Boolean = true,
) {
    val c = Pw.cores
    // senha: o olho no canto mostra/esconde o que foi digitado
    var mostrar by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    OutlinedTextField(
        value = valor,
        onValueChange = onMudar,
        trailingIcon = if (senha) { {
            androidx.compose.material3.IconButton(onClick = { mostrar = !mostrar }) {
                Icon(if (mostrar) PwIcones.EyeOff else PwIcones.Eye, if (mostrar) "Esconder a senha" else "Ver a senha", tint = c.texto3)
            }
        } } else null,
        label = { Text(rotulo, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        placeholder = dica?.let { { Text(it, color = c.texto3, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        textStyle = TextStyle(fontFamily = Archivo, fontSize = 15.sp, color = c.texto),
        singleLine = linhas == 1,
        minLines = linhas,
        enabled = habilitado,
        prefix = prefixo?.let { { Text(it, color = c.texto3) } },
        suffix = sufixo?.let { { Text(it, color = c.texto3) } },
        visualTransformation = if (senha && !mostrar) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.ouro, unfocusedBorderColor = c.bordaForte, focusedLabelColor = c.ouro, cursorColor = c.ouro,
            focusedContainerColor = c.campo, unfocusedContainerColor = c.campo, disabledContainerColor = c.campo,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Linha com interruptor (o toque na linha inteira liga/desliga). */
@Composable
fun PwInterruptor(titulo: String, ligado: Boolean, onMudar: (Boolean) -> Unit, modifier: Modifier = Modifier, descricao: String? = null, icone: ImageVector? = null, cor: Color? = null, habilitado: Boolean = true) {
    val c = Pw.cores
    Row(
        modifier.fillMaxWidth().alpha(if (habilitado) 1f else 0.5f).clip(RoundedCornerShape(14.dp)).clickable(enabled = habilitado) { onMudar(!ligado) }.padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, null, tint = cor ?: c.ouro, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, fontSize = 14.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (descricao != null) Text(descricao, color = c.texto3, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = ligado, onCheckedChange = onMudar, enabled = habilitado,
            colors = SwitchDefaults.colors(checkedThumbColor = c.sobreOuro, checkedTrackColor = cor ?: c.ouro, uncheckedTrackColor = c.bordaForte, uncheckedBorderColor = Color.Transparent),
        )
    }
}

/** Número com botões − e + (ajustes de configuração). */
@Composable
fun PwPasso(valor: Double, onMudar: (Double) -> Unit, modifier: Modifier = Modifier, passo: Double = 1.0, min: Double = 0.0, max: Double = 1e9, unidade: String? = null,
            /** true = − / + e o número digitado mudam um rascunho; salva só no ✓. */
            confirmar: Boolean = false) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    var rascunho by androidx.compose.runtime.remember(valor) { androidx.compose.runtime.mutableStateOf<Double?>(null) }
    var texto by androidx.compose.runtime.remember(valor) { androidx.compose.runtime.mutableStateOf(numeroBr(valor, 2)) }
    var focado by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val atual = rascunho ?: valor
    fun limpar(n: Double) = (n * 1000).let { Math.round(it) / 1000.0 }.coerceIn(min, max)
    fun definir(n: Double) {
        val v = limpar(n)
        texto = numeroBr(v, 2)
        if (confirmar) rascunho = v else onMudar(v)
    }
    fun ler(t: String) = t.replace(".", "").replace(",", ".").replace(" ", "").toDoubleOrNull()
    val mudou = confirmar && rascunho != null && rascunho != valor
    // barra só faz sentido com limites razoáveis (e até ~400 passos)
    val cabeBarra = max < 1e8 && max > min && (max - min) / passo.coerceAtLeast(1e-6) <= 400
    val barraPref by TemaApp.numeroBarra.collectAsState()
    val barra = barraPref && cabeBarra
    Column(modifier) {
        if (barra) {
            Row(Modifier.fillMaxWidth().clip(forma).background(c.campo).border(1.dp, if (mudou) c.ouro else c.bordaForte, forma).padding(start = 14.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text((if (unidade == "R$") "R$ " else "") + numeroBr(atual, 2) + (if (unidade != null && unidade != "R$") " $unidade" else ""),
                        color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Archivo)
                    com.pwexpress.core.mapa.PwBarraBolinha(
                        atual.toFloat().coerceIn(min.toFloat(), max.toFloat()),
                        { v -> val n = limpar(v.toDouble()); texto = numeroBr(n, 2); rascunho = n },
                        min.toFloat()..max.toFloat(), passo.toFloat(), Modifier.fillMaxWidth(),
                        aoSoltar = { if (!confirmar) rascunho?.let { onMudar(it); rascunho = null } },
                    )
                }
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).clickable { TemaApp.definirNumeroBarra(false) }, contentAlignment = Alignment.Center) {
                    Icon(PwIcones.Pencil, "Digitar o número", tint = c.texto3, modifier = Modifier.size(16.dp))
                }
            }
        } else
        Row(Modifier.fillMaxWidth().clip(forma).background(c.campo).border(1.dp, if (focado || mudou) c.ouro else c.bordaForte, forma), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clickable(enabled = atual > min) { definir(atual - passo) }, contentAlignment = Alignment.Center) {
                Icon(PwIcones.Minus, "Diminuir", tint = if (atual > min) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                if (unidade == "R$") Text("R$ ", color = c.texto3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                // dá para digitar o número direto
                androidx.compose.foundation.text.BasicTextField(
                    texto, { t ->
                        texto = t.filter { it.isDigit() || it == ',' || it == '.' || it == '-' }.take(12)
                        ler(texto)?.let { n -> if (confirmar) rascunho = n.coerceIn(min, max) }
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(color = c.texto, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = Archivo, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                    singleLine = true, cursorBrush = androidx.compose.ui.graphics.SolidColor(c.ouro),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                        val n = ler(texto)
                        if (n == null) texto = numeroBr(atual, 2)
                        else if (confirmar) { onMudar(limpar(n)); rascunho = null } else definir(n)
                    }),
                    modifier = Modifier.widthIn(min = 48.dp, max = 130.dp).onFocusChanged { f ->
                        focado = f.isFocused
                        // saiu do campo sem confirmar: no modo direto aplica, no modo ✓ fica como rascunho
                        if (!f.isFocused && !confirmar) ler(texto)?.let { if (limpar(it) != valor) definir(it) else texto = numeroBr(valor, 2) }
                    },
                )
                if (unidade != null && unidade != "R$") Text(" $unidade", color = c.texto3, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Box(Modifier.size(48.dp).clickable(enabled = atual < max) { definir(atual + passo) }, contentAlignment = Alignment.Center) {
                Icon(PwIcones.Plus, "Aumentar", tint = if (atual < max) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
            }
            if (cabeBarra) Box(Modifier.size(width = 40.dp, height = 48.dp).clickable { TemaApp.definirNumeroBarra(true) }, contentAlignment = Alignment.Center) {
                Icon(PwIcones.SlidersHorizontal, "Usar a barra", tint = c.texto3, modifier = Modifier.size(16.dp))
            }
        }
        if (mudou) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PwBotaoOuro("Salvar ${if (unidade == "R$") "R$ " else ""}${numeroBr(rascunho!!, 2)}${if (unidade != null && unidade != "R$") " $unidade" else ""}",
                    onClick = { onMudar(limpar(rascunho!!)); rascunho = null }, icone = PwIcones.Check, altura = 44.dp, modifier = Modifier.weight(1f))
                PwBotaoIcone(PwIcones.X, "Desfazer", { rascunho = null; texto = numeroBr(valor, 2) }, tamanho = 44.dp)
            }
        }
    }
}

// ------------------------------------------------------------------ listas e estados

/** Linha tocável de lista (ícone, título, subtítulo e algo à direita). */
@Composable
fun PwLinha(titulo: String, modifier: Modifier = Modifier, subtitulo: String? = null, icone: ImageVector? = null, corIcone: Color? = null, onClick: (() -> Unit)? = null, direita: @Composable RowScope.() -> Unit = {}) {
    val c = Pw.cores
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            PwAzulejo(icone, tamanho = 38.dp, cor = corIcone)
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(titulo, color = c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, color = c.texto3, fontSize = 12.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = direita)
        if (onClick != null) Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun PwVazio(texto: String, modifier: Modifier = Modifier, icone: ImageVector = PwIcones.Info) {
    val c = Pw.cores
    Column(modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PwAzulejo(icone, tamanho = 52.dp)
        Spacer(Modifier.height(12.dp))
        Text(texto, color = c.texto2, textAlign = TextAlign.Center, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
fun PwCarregando(modifier: Modifier = Modifier, texto: String? = null) {
    val c = Pw.cores
    Column(modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = c.ouro, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        if (texto != null) { Spacer(Modifier.height(10.dp)); Text(texto, color = c.texto3, fontSize = 13.sp) }
    }
}

/** Mensagem de erro em caixa vermelha suave. */
@Composable
fun PwErro(texto: String?, modifier: Modifier = Modifier) {
    if (texto.isNullOrBlank()) return
    if (com.pwexpress.core.ehMensagemDeCancelamento(texto)) return // a tela saiu no meio: não é erro
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(forma).background(c.vermelho.copy(alpha = 0.10f)).border(1.dp, c.vermelho.copy(alpha = 0.35f), forma).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PwIcones.Alerta, null, tint = c.vermelho, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(10.dp))
        Text(texto, color = c.texto, fontSize = 13.5.sp, lineHeight = 19.sp)
    }
}

/** Título de seção com ação opcional à direita. */
@Composable
fun PwSecao(titulo: String, modifier: Modifier = Modifier, acao: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        PwRotulo(titulo, Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, content = acao)
    }
}

/** Botão de texto vermelho (ações perigosas). */
@Composable
fun PwBotaoPerigo(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, icone: ImageVector? = PwIcones.Trash2, habilitado: Boolean = true, cheio: Boolean = false) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier.heightIn(min = 50.dp).clip(forma)
            .background(if (cheio) Color(0xFFDC2626) else c.vermelho.copy(alpha = 0.08f))
            .border(1.dp, c.vermelho.copy(alpha = if (cheio) 0f else 0.5f), forma)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val cor = if (cheio) Color.White else c.vermelho
        if (icone != null) { Icon(icone, null, tint = cor.copy(alpha = if (habilitado) 1f else 0.5f), modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)) }
        Text(texto, color = cor.copy(alpha = if (habilitado) 1f else 0.5f), fontWeight = FontWeight.Bold, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ------------------------------------------------------------------ avatar

/** Foto (ou inicial) com anel dourado; `teste` troca o anel pelo violeta. */
@Composable
fun PwAvatar(nome: String, foto: String?, modifier: Modifier = Modifier, tamanho: Dp = 44.dp, onClick: (() -> Unit)? = null, oculto: Boolean = false) {
    val c = Pw.cores
    val vermelho = androidx.compose.ui.graphics.Color(0xFFEF4444)
    Box(
        modifier.size(tamanho).clip(CircleShape).background(if (oculto) androidx.compose.ui.graphics.SolidColor(vermelho) else c.gradOuro).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(tamanho - 4.dp).clip(CircleShape).background(c.fundo2), contentAlignment = Alignment.Center) {
            // Dev oculto: o ícone </> no lugar da foto
            if (oculto) { Icon(PwIcones.CodeXml, "Dev", tint = vermelho, modifier = Modifier.size(tamanho * 0.5f)); return@Box }
            // a inicial fica sempre por baixo: se a foto não carregar, ela aparece (nada de círculo vazio)
            Text(
                nome.trim().take(1).uppercase().ifEmpty { "?" },
                style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = (tamanho.value * 0.42f).sp),
            )
            if (!foto.isNullOrBlank()) {
                AsyncImage(model = foto, contentDescription = nome, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
    }
}

// ------------------------------------------------------------------ janelas

/**
 * Janela cheia: ocupa a tela inteira, com barra de voltar e título no topo.
 * Todo menu e aviso do app abre assim (nada de caixinha flutuando por cima).
 */
@Composable
fun PwTelaCheia(
    titulo: String,
    onFechar: () -> Unit,
    modifier: Modifier = Modifier,
    subtitulo: String? = null,
    fecharVoltar: Boolean = true,
    rolar: Boolean = true,
    centralizar: Boolean = false,
    rodape: (@Composable ColumnScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pw.cores
    Dialog(onDismissRequest = { if (fecharVoltar) onFechar() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false, dismissOnBackPress = fecharVoltar)) {
        Column(Modifier.fillMaxSize().background(c.fundo).statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (fecharVoltar) PwBotaoIcone(PwIcones.ArrowLeft, "Voltar", onFechar) else Spacer(Modifier.size(42.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(titulo, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    subtitulo?.let { Text(it, color = c.texto3, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.borda))
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(
                    modifier.widthIn(max = 560.dp).fillMaxWidth().then(if (rolar) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(horizontal = 18.dp, vertical = 16.dp)
                        .then(if (centralizar) Modifier.fillMaxSize() else Modifier),
                    verticalArrangement = if (centralizar) Arrangement.spacedBy(12.dp, Alignment.CenterVertically) else Arrangement.spacedBy(12.dp),
                    horizontalAlignment = if (centralizar) Alignment.CenterHorizontally else Alignment.Start,
                    content = content,
                )
            }
            if (rodape != null) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = rodape)
            }
        }
    }
}

/** Janela de diálogo: agora abre como janela cheia (mesma API de antes). */
@Composable
fun PwJanela(titulo: String, onFechar: () -> Unit, modifier: Modifier = Modifier, fecharFora: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    PwTelaCheia(titulo, onFechar, modifier, fecharVoltar = fecharFora, content = content)
}

/** Botão flutuante redondo dourado (ex.: nova corrida). */
@Composable
fun PwBotaoFlutuante(icone: ImageVector, texto: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val forma = RoundedCornerShape(20.dp)
    Row(
        modifier.clip(forma).background(c.gradOuro).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = c.sobreOuro, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = c.sobreOuro, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp)
    }
}

/** Cartão-número (painéis de resumo). */
@Composable
fun PwNumero(rotulo: String, valor: String, modifier: Modifier = Modifier, icone: ImageVector? = null, destaque: Boolean = false) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier.clip(forma)
            .background(if (destaque) Brush.linearGradient(listOf(c.ouro.copy(alpha = 0.16f), Color.Transparent)) else Brush.linearGradient(listOf(c.vidroSutil, c.vidroSutil)))
            .border(1.dp, if (destaque) c.ouro.copy(alpha = 0.35f) else c.borda, forma)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icone != null) { Icon(icone, null, tint = c.texto3, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp)) }
            PwRotulo(rotulo, linhas = 1)
        }
        Spacer(Modifier.height(4.dp))
        TextoAjustado(
            valor,
            if (destaque) TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black)
            else TextStyle(color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold),
            if (destaque) 24 else 19,
        )
    }
}

/** Some/aparece com fade (atalho). */
@Composable
fun PwSumir(visivel: Boolean, content: @Composable () -> Unit) {
    AnimatedVisibility(visivel, enter = fadeIn(), exit = fadeOut()) { content() }
}

/** Escala suave ao pressionar não é trivial sem interação; usamos para destacar seleção. */
@Composable
fun Modifier.pwDestaque(ativo: Boolean): Modifier {
    val s by animateFloatAsState(if (ativo) 1f else 0.98f, label = "destaque")
    return this.scale(s)
}

/** Conteúdo rolável com espaçamento padrão das telas. */
@Composable
fun PwPagina(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(padding), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

/** Texto de uma linha que diminui sozinho até caber (valores, números grandes). */
@Composable
fun TextoAjustado(texto: String, estilo: TextStyle, maximo: Int, modifier: Modifier = Modifier, minimo: Int = 10) {
    androidx.compose.foundation.text.BasicText(
        texto, modifier, style = estilo, maxLines = 1, softWrap = false,
        autoSize = androidx.compose.foundation.text.TextAutoSize.StepBased(minFontSize = minimo.sp, maxFontSize = maximo.sp, stepSize = 0.5.sp),
    )
}

/**
 * Foto da conta (topo e menu). O espaço do anel existe sempre, então trocar de tema ou ligar o
 * modo teste não muda nenhum tamanho. No teste: anel + frasco; Dev: selo </>.
 */
@Composable
fun PwAvatarConta(nome: String, foto: String?, teste: Boolean, modifier: Modifier = Modifier, tamanho: Dp = 42.dp, onClick: (() -> Unit)? = null, dev: Boolean = false,
                  gestor: Boolean = false, motoboy: Boolean = false) {
    val c = Pw.cores
    Box(modifier.requiredSize(tamanho)) {
        PwAvatar(
            nome, foto,
            Modifier.border(2.dp, if (teste) c.ouro else Color.Transparent, CircleShape).padding(3.dp),
            tamanho = tamanho, onClick = onClick,
        )
        // selo do cargo mais alto: teste > Dev > gestor > motoboy
        val selo = when { teste -> PwIcones.FlaskConical; dev -> PwIcones.CodeXml; gestor -> PwIcones.ShieldCheck; motoboy -> PwIcones.Bike; else -> null }
        if (selo != null) {
            Box(
                Modifier.align(Alignment.BottomEnd).size((tamanho.value * 0.42f).coerceAtLeast(16f).dp).clip(CircleShape).background(c.fundo)
                    .padding(2.dp).clip(CircleShape).background(c.gradOuro),
                contentAlignment = Alignment.Center,
            ) { Icon(selo, when { teste -> "Modo teste"; dev -> "Dev"; gestor -> "Gestor"; else -> "Motoboy" }, tint = c.sobreOuro, modifier = Modifier.size((tamanho.value * 0.22f).coerceAtLeast(9f).dp)) }
        }
    }
}

/** Ícone e cor de cada cargo (iguais ao site): Dev </>, Gestor escudo, Motoboy moto. */
@Composable
fun PwSelosCargo(dev: Boolean, gestor: Boolean, motoboy: Boolean, teste: Boolean = false, modifier: Modifier = Modifier, supervisor: Boolean = false) {
    val c = Pw.cores
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        if (dev) PwSelo("Dev", c.dev, icone = PwIcones.CodeXml)
        if (gestor) PwSelo("Gestor", c.ouro, icone = PwIcones.ShieldCheck)
        if (supervisor && !gestor) PwSelo("Supervisor", c.azul, icone = PwIcones.UserCog)
        if (motoboy) PwSelo("Motoboy", c.texto2, icone = PwIcones.Bike)
        if (teste) PwSelo("Teste", c.violeta, icone = PwIcones.FlaskConical)
    }
}

/** Cabeçalho das telas abertas por cima (voltar + título de uma linha + ações em ícone). */
@Composable
fun PwCabecalho(titulo: String, onVoltar: () -> Unit, modifier: Modifier = Modifier, subtitulo: String? = null, acoes: @Composable RowScope.() -> Unit = {}) {
    val c = Pw.cores
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PwBotaoIcone(PwIcones.ArrowLeft, "Voltar", onVoltar)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = c.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, color = c.texto2, fontSize = 13.sp, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = acoes)
    }
}

/** Pílula de vidro sobre o mapa (lista, estabelecimentos…). `ligada` pinta de dourado. */
@Composable
fun PwPilula(texto: String, icone: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, ligada: Boolean = false) {
    val c = Pw.cores
    val forma = RoundedCornerShape(99.dp)
    Row(
        modifier.clip(forma).then(if (ligada) Modifier.background(c.gradOuro) else Modifier.background(c.vidroForte))
            .border(1.dp, if (ligada) Color.Transparent else c.bordaForte, forma).clickable(onClick = onClick)
            .padding(horizontal = if (texto.isEmpty()) 11.dp else 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, texto.ifEmpty { null }, tint = if (ligada) c.sobreOuro else c.texto, modifier = Modifier.size(17.dp))
        if (texto.isNotEmpty()) {
            Spacer(Modifier.width(7.dp))
            Text(texto, color = if (ligada) c.sobreOuro else c.texto, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, fontFamily = Archivo, maxLines = 1, softWrap = false)
        }
    }
}

// ------------------------------------------------------------------ avisos rápidos (toasts)

enum class TipoAviso { OK, ERRO, INFO, AVISO }
data class Aviso(val id: Long, val texto: String, val tipo: TipoAviso, val titulo: String?, val duracaoMs: Long)

/** Avisos rápidos no rodapé (mesmo sistema do site). Pode chamar de qualquer lugar: PwToast.mostrar("Salvo"). */
object PwToast {
    private var seq = 0L
    private val _avisos = kotlinx.coroutines.flow.MutableStateFlow<List<Aviso>>(emptyList())
    val avisos: kotlinx.coroutines.flow.StateFlow<List<Aviso>> = _avisos
    fun mostrar(texto: String, tipo: TipoAviso = TipoAviso.OK, titulo: String? = null) {
        if (tipo == TipoAviso.ERRO && com.pwexpress.core.ehMensagemDeCancelamento(texto)) return
        // texto longo (explicações, medalhas) fica mais tempo na tela
        val base = if (tipo == TipoAviso.ERRO || tipo == TipoAviso.AVISO) 6000L else 3500L
        val a = Aviso(++seq, texto, tipo, titulo, maxOf(base, minOf(11_000L, (texto.length + (titulo?.length ?: 0)) * 60L)))
        _avisos.value = (_avisos.value + a).takeLast(3)
    }
    fun fechar(id: Long) { _avisos.value = _avisos.value.filterNot { it.id == id } }
}

/** Onde os avisos aparecem (uma vez, por cima de tudo). Cada tipo tem seu degradê. */
@Composable
fun PwToasts(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val avisos by PwToast.avisos.collectAsState()
    Box(Modifier.fillMaxSize()) {
        PwDestaques(Modifier.align(Alignment.TopCenter))
        Column(modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 100.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            avisos.forEach { a ->
                androidx.compose.runtime.key(a.id) {
                    androidx.compose.runtime.LaunchedEffect(a.id) { kotlinx.coroutines.delay(a.duracaoMs); PwToast.fechar(a.id) }
                    var visivel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                    androidx.compose.runtime.LaunchedEffect(Unit) { visivel = true }
                    val forma = RoundedCornerShape(20.dp)
                    val cor = when (a.tipo) { TipoAviso.OK -> c.verde; TipoAviso.ERRO, TipoAviso.AVISO -> c.vermelho; TipoAviso.INFO -> c.ouro }
                    androidx.compose.animation.AnimatedVisibility(visivel, enter = androidx.compose.animation.slideInVertically { it / 2 } + androidx.compose.animation.fadeIn()) {
                        Row(
                            Modifier.fillMaxWidth()
                                .shadow(10.dp, forma, ambientColor = cor.copy(alpha = 0.4f), spotColor = cor.copy(alpha = 0.4f))
                                .clip(forma)
                                .background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(cor.copy(alpha = 0.22f).compositeOver(c.fundo2), c.fundo2, c.fundo2)))
                                .border(1.dp, androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(cor.copy(alpha = 0.8f), c.bordaForte)), forma)
                                .clickable { PwToast.fechar(a.id) }.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(cor, cor.copy(alpha = 0.55f)))), contentAlignment = Alignment.Center) {
                                Icon(if (a.tipo == TipoAviso.OK) PwIcones.Check else if (a.tipo == TipoAviso.INFO) PwIcones.Info else PwIcones.Alerta, null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(19.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                a.titulo?.let { Text(it, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 14.sp) }
                                Text(a.texto, color = if (a.titulo != null) c.texto2 else c.texto, fontSize = 13.5.sp, lineHeight = 18.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Estilo dos destaques grandes (topo da tela). */
enum class EstiloDestaque { ONLINE, OFFLINE, CANCELADA, EXTENSAO, OFERTA, PLANEJADA, CHAT, OURO }
data class Destaque(val id: Long, val titulo: String, val texto: String?, val estilo: EstiloDestaque, val icone: ImageVector?, val duracaoMs: Long, val onClick: (() -> Unit)?)

/** Avisos grandes com degradê para os momentos importantes: ficou online, o gestor cancelou, a rota cresceu… */
object PwDestaque {
    private var seq = 0L
    private val _lista = kotlinx.coroutines.flow.MutableStateFlow<List<Destaque>>(emptyList())
    val lista: kotlinx.coroutines.flow.StateFlow<List<Destaque>> = _lista
    fun mostrar(titulo: String, texto: String? = null, estilo: EstiloDestaque = EstiloDestaque.OURO, icone: ImageVector? = null, duracaoMs: Long = 4_500, onClick: (() -> Unit)? = null) {
        _lista.value = (_lista.value + Destaque(++seq, titulo, texto, estilo, icone, duracaoMs, onClick)).takeLast(2)
    }
    fun fechar(id: Long) { _lista.value = _lista.value.filterNot { it.id == id } }
}

private fun coresDestaque(e: EstiloDestaque, c: PwPaleta): List<Color> = when (e) {
    EstiloDestaque.ONLINE -> listOf(Color(0xFF0E7A4A), Color(0xFF1FB86E), Color(0xFF7BE3A8))
    EstiloDestaque.OFFLINE -> listOf(Color(0xFF2A2A2E), Color(0xFF4A4A52), Color(0xFF6E6E78))
    EstiloDestaque.CANCELADA -> listOf(Color(0xFF7A1222), Color(0xFFD23A4E), Color(0xFFFF8A80))
    EstiloDestaque.EXTENSAO, EstiloDestaque.OURO -> listOf(c.ouroEscuro, c.ouro, c.ouroClaro)
    EstiloDestaque.OFERTA -> listOf(Color(0xFF8A6A12), c.ouro, Color(0xFFFFE9A8))
    EstiloDestaque.PLANEJADA -> listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF93C5FD))
    EstiloDestaque.CHAT -> listOf(Color(0xFF4C1D95), Color(0xFF7C3AED), Color(0xFFC4B5FD))
}

@Composable
private fun PwDestaques(modifier: Modifier) {
    val c = Pw.cores
    val lista by PwDestaque.lista.collectAsState()
    Column(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lista.forEach { d ->
            androidx.compose.runtime.key(d.id) {
                androidx.compose.runtime.LaunchedEffect(d.id) { kotlinx.coroutines.delay(d.duracaoMs); PwDestaque.fechar(d.id) }
                var visivel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                androidx.compose.runtime.LaunchedEffect(Unit) { visivel = true }
                val cores = coresDestaque(d.estilo, c)
                val claro = d.estilo == EstiloDestaque.EXTENSAO || d.estilo == EstiloDestaque.OURO || d.estilo == EstiloDestaque.OFERTA
                val tinta = if (claro) Color(0xFF0A0A0A) else Color.White
                val forma = RoundedCornerShape(24.dp)
                // brilho que passa uma vez pelo cartão
                val brilho = androidx.compose.animation.core.rememberInfiniteTransition(label = "brilho")
                val x by brilho.animateFloat(-0.4f, 1.4f, androidx.compose.animation.core.infiniteRepeatable<Float>(androidx.compose.animation.core.tween<Float>(2600), androidx.compose.animation.core.RepeatMode.Restart), label = "x")
                androidx.compose.animation.AnimatedVisibility(visivel, enter = androidx.compose.animation.slideInVertically { -it } + androidx.compose.animation.fadeIn()) {
                    Box(
                        Modifier.fillMaxWidth().shadow(16.dp, forma, ambientColor = cores[1], spotColor = cores[1]).clip(forma)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(cores))
                            .clickable { d.onClick?.invoke(); PwDestaque.fechar(d.id) },
                    ) {
                        Box(Modifier.matchParentSize().background(androidx.compose.ui.graphics.Brush.linearGradient(
                            0f to Color.Transparent, (x - 0.15f).coerceIn(0f, 1f) to Color.Transparent, x.coerceIn(0f, 1f) to Color.White.copy(alpha = 0.22f),
                            (x + 0.15f).coerceIn(0f, 1f) to Color.Transparent, 1f to Color.Transparent,
                        )))
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).clip(CircleShape).background(tinta.copy(alpha = 0.16f)).border(1.5.dp, tinta.copy(alpha = 0.4f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(d.icone ?: when (d.estilo) {
                                    EstiloDestaque.ONLINE -> PwIcones.Radio; EstiloDestaque.OFFLINE -> PwIcones.Power; EstiloDestaque.CANCELADA -> PwIcones.Ban
                                    EstiloDestaque.EXTENSAO -> PwIcones.Route; EstiloDestaque.OFERTA -> PwIcones.Zap; EstiloDestaque.PLANEJADA -> PwIcones.CalendarClock
                                    EstiloDestaque.CHAT -> PwIcones.MessagesSquare; EstiloDestaque.OURO -> PwIcones.Sparkles
                                }, null, tint = tinta, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(d.titulo, color = tinta, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 17.sp, maxLines = 2)
                                d.texto?.let { Text(it, color = tinta.copy(alpha = 0.85f), fontSize = 13.5.sp, lineHeight = 18.sp, maxLines = 3) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Estado liga/desliga que o app lembra entre aberturas (ex.: gaveta minimizada ou expandida). */
@Composable
fun lembrarLigado(chave: String, padrao: Boolean): androidx.compose.runtime.MutableState<Boolean> {
    val ctx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val prefs = androidx.compose.runtime.remember { ctx.getSharedPreferences("pwex_ui", android.content.Context.MODE_PRIVATE) }
    val estado = androidx.compose.runtime.remember(chave) { androidx.compose.runtime.mutableStateOf(prefs.getBoolean(chave, padrao)) }
    return androidx.compose.runtime.remember(chave) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = estado.value
                set(v) { estado.value = v; prefs.edit().putBoolean(chave, v).apply() }
            override fun component1() = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
}


/** As chaves PIX da própria conta (a tela vem do módulo do motoboy; o painel do gestor mostra na Carteira). */
val LocalMinhasChavesPix = androidx.compose.runtime.staticCompositionLocalOf<@Composable () -> Unit> { {} }

/** Pergunta antes de algo que não dá para desfazer (apagar, sair…). */
@Composable
fun PwConfirmar(titulo: String, texto: String, acao: String, onConfirmar: () -> Unit, onCancelar: () -> Unit, perigo: Boolean = true) {
    PwJanela(titulo, onCancelar) {
        Text(texto, color = Pw.cores.texto2, fontSize = 14.sp)
        Spacer(Modifier.height(14.dp))
        if (perigo) PwBotaoPerigo(acao, onClick = onConfirmar, cheio = true, modifier = Modifier.fillMaxWidth())
        else PwBotaoOuro(acao, onClick = onConfirmar, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PwBotaoVidro("Cancelar", onClick = onCancelar, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * Página que o app lembra ao abrir de novo (aba, sub-aba…). Segue o ajuste "lembrar_pagina"
 * (ligado: volta onde a pessoa estava; desligado: sempre começa no padrão).
 */
@Composable
fun lembrarPagina(chave: String, padrao: String): androidx.compose.runtime.MutableState<String> {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val prefs = androidx.compose.runtime.remember { ctx.getSharedPreferences("pwex_paginas", android.content.Context.MODE_PRIVATE) }
    val ligado = com.pwexpress.core.perfil.Perfis.perfil.value?.logico("lembrar_pagina", true) != false
    val estado = androidx.compose.runtime.saveable.rememberSaveable(chave) {
        androidx.compose.runtime.mutableStateOf(if (ligado) prefs.getString(chave, padrao) ?: padrao else padrao)
    }
    androidx.compose.runtime.LaunchedEffect(estado.value) { prefs.edit().putString(chave, estado.value).apply() }
    return estado
}

/** Igual a [lembrarPagina], para números (ex.: sub-aba 0/1). */
@Composable
fun lembrarPaginaNumero(chave: String, padrao: Int): androidx.compose.runtime.MutableState<Int> {
    val s = lembrarPagina(chave, padrao.toString())
    return androidx.compose.runtime.remember(s) {
        object : androidx.compose.runtime.MutableState<Int> {
            override var value: Int
                get() = s.value.toIntOrNull() ?: padrao
                set(v) { s.value = v.toString() }
            override fun component1(): Int = value
            override fun component2(): (Int) -> Unit = { value = it }
        }
    }
}
