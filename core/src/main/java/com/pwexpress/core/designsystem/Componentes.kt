package com.pwexpress.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.pwexpress.core.R
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ------------------------------------------------------------------ fundo com brilhos dourados

/** Fundo da tela: base do tema + três brilhos dourados difusos (igual ao site). */
@Composable
fun PwFundo(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = Pw.cores
    Box(
        modifier
            .fillMaxSize()
            .background(c.fundo)
            .drawBehind {
                drawRect(Brush.radialGradient(listOf(c.brilho1, Color.Transparent), center = Offset(-size.width * 0.1f, -size.height * 0.05f), radius = size.width * 1.1f))
                drawRect(Brush.radialGradient(listOf(c.brilho2, Color.Transparent), center = Offset(size.width * 1.1f, size.height * 0.05f), radius = size.width * 0.9f))
                drawRect(Brush.radialGradient(listOf(c.brilho2, Color.Transparent), center = Offset(size.width * 0.5f, size.height * 1.08f), radius = size.width))
            },
        content = content,
    )
}

// ------------------------------------------------------------------ vidro

/** Cartão de vidro: translúcido, borda sutil e reflexo no topo. */
@Composable
fun PwCartao(
    modifier: Modifier = Modifier,
    destaque: Boolean = false,
    cor: Color? = null,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Pw.cores
    val forma = RoundedCornerShape(22.dp)
    val borda = cor ?: if (destaque) c.ouro.copy(alpha = 0.7f) else c.borda
    Column(
        modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Brush.verticalGradient(listOf(c.vidroForte, c.vidro)))
            .background(Brush.linearGradient(listOf(c.vidroSutil, Color.Transparent)))
            .drawBehind {
                // reflexo de 1 px no topo
                drawLine(c.reflexo, Offset(size.width * 0.08f, 0.5f), Offset(size.width * 0.92f, 0.5f), strokeWidth = 1.5f)
            }
            .border(1.dp, borda, forma)
            .padding(padding),
        content = content,
    )
}

/** Bloco interno (dentro de um cartão). */
@Composable
fun PwBloco(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Column(
        modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

// ------------------------------------------------------------------ botões

@Composable
fun PwBotaoOuro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    habilitado: Boolean = true,
    altura: Dp = 58.dp,
    tamanhoTexto: Int = 17,
) {
    val c = Pw.cores
    val forma = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = altura)
            .alpha(if (habilitado) 1f else 0.5f)
            .clip(forma)
            .background(c.gradOuro)
            .drawBehind { drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, 0.5f), Offset(size.width, 0.5f), 1.5f) }
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, null, tint = c.sobreOuro, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(texto, color = c.sobreOuro, fontWeight = FontWeight.ExtraBold, fontSize = tamanhoTexto.sp, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun PwBotaoVidro(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icone: ImageVector? = null,
    habilitado: Boolean = true,
    cor: Color? = null,
) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    val tinta = cor ?: c.texto
    Row(
        modifier
            .heightIn(min = 46.dp)
            .alpha(if (habilitado) 1f else 0.5f)
            .clip(forma)
            .background(c.vidroSutil)
            .border(1.dp, (cor ?: c.bordaForte).copy(alpha = if (cor != null) 0.6f else 1f), forma)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, texto.ifEmpty { null }, tint = tinta, modifier = Modifier.size(18.dp))
            if (texto.isNotEmpty()) Spacer(Modifier.width(8.dp))
        }
        if (texto.isNotEmpty()) Text(texto, color = tinta, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, fontFamily = Archivo, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Botão só de ícone (quadrado de vidro). */
@Composable
fun PwBotaoIcone(icone: ImageVector, descricao: String, onClick: () -> Unit, modifier: Modifier = Modifier, ativo: Boolean = false, tamanho: Dp = 42.dp, habilitado: Boolean = true, cor: Color? = null) {
    val c = Pw.cores
    val forma = RoundedCornerShape(tamanho * 0.31f)
    Box(
        modifier.size(tamanho).alpha(if (habilitado) 1f else 0.4f).clip(forma)
            .then(if (ativo) Modifier.background(c.gradOuro) else Modifier.background(c.vidroForte))
            .border(1.dp, if (ativo) Color.Transparent else c.borda, forma).clickable(enabled = habilitado, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icone, descricao, tint = if (ativo) c.sobreOuro else cor ?: c.texto2, modifier = Modifier.size(tamanho * 0.45f)) }
}

/** Alterna claro ↔ noturno. */
@Composable
fun BotaoTema(modifier: Modifier = Modifier) {
    val escuro = Pw.cores.escuro
    PwBotaoIcone(if (escuro) PwIcones.Moon else PwIcones.Sun, if (escuro) "Tema noturno" else "Tema claro",
        onClick = { TemaApp.definir(if (escuro) PreferenciaTema.CLARO else PreferenciaTema.ESCURO) }, modifier = modifier)
}

// ------------------------------------------------------------------ marca e detalhes

/** Logo da Delibra (o D com as ondas de radar). A cor segue o tema: dourado, violeta no teste, vermelho do Dev. */
@Composable
fun PwSeloLogo(tamanho: Dp, modifier: Modifier = Modifier, descricao: String? = "Delibra", delibra: Boolean = false) {
    // ícone próprio da empresa (enviado pelo dono no site) no lugar do Delibra
    val empresa by com.pwexpress.core.marca.MarcaEmpresa.marca.collectAsState()
    val url = empresa?.logoUrl
    if (!delibra && url != null) {
        coil.compose.AsyncImage(url, empresa?.nome ?: descricao, modifier.size(tamanho).clip(RoundedCornerShape(tamanho * 0.22f)),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            error = painterResource(R.drawable.pwex_marca_ouro))
        return
    }
    val marca = Pw.cores.marca
    val res = when (marca) { Marca.VIOLETA -> R.drawable.pwex_marca_violeta; Marca.VERMELHO -> R.drawable.pwex_marca_vermelho; Marca.AZUL -> R.drawable.pwex_marca_azul; Marca.VERDE -> R.drawable.pwex_marca_verde; Marca.BRANCO -> R.drawable.pwex_marca_branco; Marca.PRETO -> R.drawable.pwex_marca_preto; Marca.OURO -> R.drawable.pwex_marca_ouro }
    Image(painterResource(res), descricao, modifier.size(tamanho), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
}

/** Logo + o nome "delibra". Com `soSelo`, só o desenho. */
@Composable
fun PwLogo(modifier: Modifier = Modifier, tamanho: Int = 26, soSelo: Boolean = false) {
    val c = Pw.cores
    if (soSelo) {
        PwSeloLogo(tamanho.dp, modifier)
        return
    }
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        val empresa by com.pwexpress.core.marca.MarcaEmpresa.marca.collectAsState()
        val propria = empresa?.nomeExibido
        PwSeloLogo((tamanho * 1.35f).dp, descricao = propria ?: "Delibra")
        Spacer(Modifier.width((tamanho * 0.3f).dp))
        Text(
            propria ?: "delibra",
            color = c.texto,
            fontFamily = Archivo,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (tamanho * 0.95f).sp,
            letterSpacing = (-tamanho * 0.03f).sp,
            maxLines = 1,
        )
    }
}

/** Logo grande das telas de entrada: desenho + "EXPRESS" espaçado embaixo. */
@Composable
fun PwLogoGrande(tamanho: Dp, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PwSeloLogo(tamanho)
        val empresa by com.pwexpress.core.marca.MarcaEmpresa.marca.collectAsState()
        Text(
            empresa?.nomeExibido?.uppercase() ?: "DELIBRA", color = Pw.cores.texto2, fontFamily = Archivo, fontWeight = FontWeight.Light,
            fontSize = (tamanho.value * 0.09f).coerceAtLeast(12f).sp, letterSpacing = (tamanho.value * 0.055f).sp, maxLines = 1,
        )
    }
}

/** Azulejo de ícone com degradê suave (como no site). */
@Composable
fun PwAzulejo(icone: ImageVector, modifier: Modifier = Modifier, tamanho: Dp = 44.dp, cor: Color? = null) {
    val c = Pw.cores
    val tinta = cor ?: c.ouro
    Box(
        modifier.size(tamanho).clip(RoundedCornerShape(tamanho * 0.32f))
            .background(Brush.linearGradient(listOf(tinta.copy(alpha = 0.26f), tinta.copy(alpha = 0.08f)))),
        contentAlignment = Alignment.Center,
    ) { Icon(icone, null, tint = tinta, modifier = Modifier.size(tamanho * 0.5f)) }
}

/** Cabeçalho de seção: azulejo + título + subtítulo. */
@Composable
fun PwCabeca(icone: ImageVector, titulo: String, subtitulo: String? = null, modifier: Modifier = Modifier, acoes: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PwAzulejo(icone)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineSmall, color = Pw.cores.texto, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyMedium, color = Pw.cores.texto2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp), content = acoes)
    }
}

@Composable
fun PwPonto(cor: Color, tamanho: Dp = 12.dp) {
    Box(Modifier.size(tamanho).clip(CircleShape).background(cor))
}

@Composable
fun PwRotulo(texto: String, modifier: Modifier = Modifier, linhas: Int = 2) {
    Text(texto.uppercase(), modifier = modifier, style = MaterialTheme.typography.labelSmall, color = Pw.cores.texto3, maxLines = linhas, overflow = TextOverflow.Ellipsis)
}

/** Selo arredondado colorido (status, "TESTE"…). */
@Composable
fun PwSelo(texto: String, cor: Color, modifier: Modifier = Modifier, icone: ImageVector? = null) {
    Row(
        modifier.clip(RoundedCornerShape(99.dp)).background(cor.copy(alpha = 0.15f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icone != null) {
            Icon(icone, null, tint = cor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(texto, color = cor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1, softWrap = false)
    }
}

/** Linha "rótulo ........ valor". */
@Composable
fun PwLinhaInfo(icone: ImageVector?, rotulo: String, valor: String, destaque: Boolean = false) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        if (icone != null) {
            Icon(icone, null, tint = c.texto3, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(rotulo, Modifier.weight(1f), color = c.texto2, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        Text(valor, color = if (destaque) c.ouro else c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1)
    }
}

@Composable
fun PwEspaco(altura: Dp) = Spacer(Modifier.height(altura))

/**
 * Valor em dinheiro com degradê, que diminui sozinho para nunca quebrar a linha
 * (ex.: R$ 10.500,00 cabe onde cabia R$ 15,00).
 */
@Composable
fun PwValor(texto: String, modifier: Modifier = Modifier, tamanhoMax: Int = 44, cor: Color? = null) {
    val c = Pw.cores
    TextoAjustado(
        texto,
        if (cor != null) TextStyle(color = cor, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
        else TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp),
        tamanhoMax, modifier, minimo = 14,
    )
}

/** Faixa no topo avisando que o modo teste está ligado. */
@Composable
fun PwFaixaTeste(modifier: Modifier = Modifier) {
    val c = Pw.cores
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.gradOuro).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(PwIcones.FlaskConical, null, tint = c.sobreOuro, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text("MODO TESTE · só corridas e ganhos de teste", color = c.sobreOuro, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = Archivo, letterSpacing = 0.5.sp)
    }
}
