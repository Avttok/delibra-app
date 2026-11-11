package com.pwexpress.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** **negrito**, *itálico* e `código` dentro de uma linha. */
fun textoRico(linha: String, corForte: androidx.compose.ui.graphics.Color? = null): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < linha.length) {
        when {
            linha.startsWith("**", i) -> {
                val fim = linha.indexOf("**", i + 2)
                if (fim < 0) { append(linha.substring(i)); i = linha.length }
                else { pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = corForte ?: androidx.compose.ui.graphics.Color.Unspecified)); append(linha.substring(i + 2, fim)); pop(); i = fim + 2 }
            }
            linha[i] == '*' && linha.indexOf('*', i + 1) > i + 1 -> {
                val fim = linha.indexOf('*', i + 1)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(linha.substring(i + 1, fim)); pop(); i = fim + 1
            }
            linha[i] == '`' && linha.indexOf('`', i + 1) > i -> {
                val fim = linha.indexOf('`', i + 1)
                pushStyle(SpanStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)); append(linha.substring(i + 1, fim)); pop(); i = fim + 1
            }
            else -> { append(linha[i]); i++ }
        }
    }
}

private fun iconeSecao(t: String): ImageVector {
    val s = t.lowercase()
    return when {
        "motoboy" in s -> PwIcones.Bike
        "gestor" in s -> PwIcones.ShieldCheck
        "cliente" in s -> PwIcones.Store
        "dev" in s -> PwIcones.CodeXml
        "app" in s || "todo" in s -> PwIcones.Smartphone
        "corrig" in s || "arrum" in s -> PwIcones.Wrench
        else -> PwIcones.Sparkles
    }
}

/**
 * Markdown simples das notas da versão: "## seção" e "**seção**" sozinho viram cabeçalho com ícone,
 * "- item" vira item com marcador dourado, e o resto vira texto (com negrito, itálico e código).
 */
@Composable
fun PwMarkdown(texto: String, modifier: Modifier = Modifier) {
    val c = Pw.cores
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        texto.lines().map { it.trimEnd() }.forEach { bruta ->
            val l = bruta.trim()
            when {
                l.isEmpty() -> Spacer(Modifier.size(2.dp))
                l.startsWith("#") -> {
                    val t = l.trimStart('#').trim().removeSurrounding("**")
                    Titulo(t, l.takeWhile { it == '#' }.length <= 2)
                }
                l.startsWith("**") && l.endsWith("**") && l.count { it == '*' } == 4 -> Titulo(l.removeSurrounding("**"), false)
                l.startsWith("- ") || l.startsWith("* ") || l.startsWith("• ") -> {
                    val recuo = bruta.length - bruta.trimStart().length
                    Row(Modifier.padding(start = (recuo * 6).dp), verticalAlignment = Alignment.Top) {
                        Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(if (recuo > 0) c.texto3 else c.ouro))
                        Spacer(Modifier.width(10.dp))
                        Text(textoRico(l.drop(2), c.texto), color = c.texto2, fontSize = 14.5.sp, lineHeight = 21.sp, fontFamily = Archivo)
                    }
                }
                else -> Text(textoRico(l, c.texto), color = c.texto2, fontSize = 14.5.sp, lineHeight = 21.sp, fontFamily = Archivo)
            }
        }
    }
}

@Composable
private fun Titulo(t: String, grande: Boolean) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(top = if (grande) 10.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(if (grande) 30.dp else 26.dp).clip(RoundedCornerShape(9.dp)).background(c.ouro.copy(alpha = 0.16f))
            .border(1.dp, c.ouro.copy(alpha = 0.35f), RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
            Icon(iconeSecao(t), null, tint = c.ouro, modifier = Modifier.size(if (grande) 16.dp else 14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(textoRico(t), color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = if (grande) 18.sp else 15.5.sp)
    }
}
