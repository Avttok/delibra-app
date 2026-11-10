package com.pwexpress.feature.perfil

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.perfil.Perfis
import java.text.NumberFormat
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")
private fun moeda(v: Double) = NumberFormat.getCurrencyInstance(br).format(v)

/** Avatar redondo com anel dourado (foto ou inicial). Toque abre o perfil. */
@Composable
fun AvatarPerfil(email: String, onClick: () -> Unit, tamanho: Int = 46) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    Box(
        Modifier.size(tamanho.dp).clip(CircleShape).background(c.gradOuro).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size((tamanho - 5).dp).clip(CircleShape).background(c.fundo2), contentAlignment = Alignment.Center) {
            val foto = perfil?.fotoUrl
            if (foto != null) {
                AsyncImage(model = foto, contentDescription = "Meu perfil", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    (perfil?.nome?.ifBlank { null } ?: email).trim().take(1).uppercase(),
                    style = TextStyle(brush = c.gradOuro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = (tamanho * 0.42f).sp),
                )
            }
        }
    }
}

/** Nome de exibição (ou o e-mail enquanto carrega). */
@Composable
fun NomePerfil(email: String): String {
    val perfil by Perfis.perfil.collectAsState()
    return perfil?.nome?.ifBlank { null } ?: email.substringBefore("@")
}

/** Seus ganhos (Início): hoje em destaque, corridas e km do dia, semana e mês. Toque abre a Carteira. */
@Composable
fun CartaoGanhos(onCarteira: (() -> Unit)? = null) {
    val c = Pw.cores
    val ganhos by Perfis.ganhos.collectAsState()
    val perfil by Perfis.perfil.collectAsState()
    LaunchedEffect(Unit) { Perfis.carregar() }
    val g = ganhos

    val periodo = g?.periodoInicio?.let { ini -> g.periodoFim?.let { fim -> "${ini.substring(8, 10)}/${ini.substring(5, 7)} – ${fim.substring(8, 10)}/${fim.substring(5, 7)}" } }
    PwCartao(destaque = true, modifier = if (onCarteira != null) Modifier.clickable(onClick = onCarteira) else Modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwRotulo(if (perfil?.modoTeste == true) "Seu dia (teste)" else "Seu dia", Modifier.weight(1f))
            if (onCarteira != null) {
                Text("Holerite", color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, fontFamily = Archivo)
                androidx.compose.material3.Icon(PwIcones.ChevronRight, null, tint = c.ouro, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        if (g?.valoresVisiveis != false) {
            PwValor(moeda(g?.hoje ?: 0.0), Modifier.fillMaxWidth(), tamanhoMax = 40)
            Text(
                "hoje · ${g?.entregasHoje ?: 0} entrega${if ((g?.entregasHoje ?: 0) == 1) "" else "s"} · ${"%.1f".format(br, g?.kmHoje ?: 0.0)} km",
                color = c.texto2, fontFamily = Archivo, fontSize = 13.sp,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${g.entregasHoje}", style = androidx.compose.ui.text.TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 40.sp), color = c.ouro)
                Spacer(Modifier.width(8.dp))
                Text("entrega${if (g.entregasHoje == 1) "" else "s"} hoje", color = c.texto2, fontFamily = Archivo, fontSize = 15.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
            Text("${g.corridasHoje} rota${if (g.corridasHoje == 1) "" else "s"} · ${"%.1f".format(br, g.kmHoje)} km rodados", color = c.texto2, fontFamily = Archivo, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Mini("Período${periodo?.let { " · $it" } ?: ""}", if (g?.valoresVisiveis != false) moeda(g?.periodoTotal ?: 0.0) else "${g.entregasPeriodo} entregas", Modifier.weight(1f))
            Mini("Rotas no período", "${g?.corridasPeriodo ?: 0} · ${"%.0f".format(br, g?.kmPeriodo ?: 0.0)} km", Modifier.weight(1f))
        }
    }
}

@Composable
private fun Mini(rotulo: String, valor: String, modifier: Modifier) {
    val c = Pw.cores
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
    Column(modifier.clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(12.dp)) {
        PwRotulo(rotulo, linhas = 1)
        Spacer(Modifier.width(2.dp))
        com.pwexpress.core.designsystem.TextoAjustado(valor, androidx.compose.ui.text.TextStyle(color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold), 17, minimo = 11)
    }
}

