package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCabeca
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.TextoAjustado
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.corridas.data.CorridaFeita
import com.pwexpress.feature.corridas.data.Holerite
import com.pwexpress.feature.corridas.data.PeriodoHolerite
import com.pwexpress.feature.corridas.data.RotaHolerite
import com.pwexpress.feature.corridas.data.Corridas
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val br = Locale.forLanguageTag("pt-BR")
private val zona = ZoneId.systemDefault()
private fun dia(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zona).toLocalDate()
internal fun kmTexto(m: Int?): String = when { m == null -> "—"; m < 1000 -> "$m m"; else -> "%.1f km".format(br, m / 1000.0) }
internal fun dataCurta(ms: Long?): String = ms?.let { Instant.ofEpochMilli(it).atZone(zona).format(DateTimeFormatter.ofPattern("dd/MM · HH:mm")) } ?: "—"

private fun dataBr(iso: String): String = if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}" else iso
private fun rotuloStatus(s: String) = when (s) { "fechado" -> "Fechado"; "pago" -> "Pago"; else -> "Em aberto" }

/**
 * Holerite do motoboy: a folha de cada período (dia, semana, quinzena ou mês, o gestor escolhe).
 * Mostra rotas, entregas e km; os valores só aparecem se o gestor permitir.
 */
@Composable
fun CarteiraScreen(modifier: Modifier = Modifier) {
    val c = Pw.cores
    val teste by TemaApp.modoTeste.collectAsState()
    var periodos by remember { mutableStateOf<List<PeriodoHolerite>?>(null) }
    var escolhido by rememberSaveable { mutableIntStateOf(0) }
    var holerite by remember { mutableStateOf<Holerite?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(teste) {
        Perfis.carregar()
        Corridas.meusHolerites().onSuccess { (l, _) -> periodos = l; erro = null }.onFailure { erro = it.message; periodos = periodos ?: emptyList() }
    }
    val atual = periodos?.getOrNull(escolhido)
    LaunchedEffect(atual?.inicio, atual?.fim, teste) {
        holerite = null
        atual?.let { p -> Corridas.meuHolerite(p.inicio, p.fim).onSuccess { holerite = it }.onFailure { erro = it.message } }
    }

    PwPagina(modifier) {
        PwCabeca(PwIcones.ReceiptText, "Holerite", if (teste) "Folha das rotas de teste" else "Sua folha de pagamento")
        PwErro(erro)

        when {
            periodos == null -> PwCarregando()
            periodos!!.isEmpty() -> PwVazio("Nenhum período ainda. As rotas concluídas aparecem aqui.", icone = PwIcones.ReceiptText)
            else -> {
                // ------------------------------------------------ períodos (mais recente primeiro)
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(periodos!!.size) { i ->
                        val p = periodos!![i]
                        val sel = i == escolhido
                        val forma = RoundedCornerShape(14.dp)
                        Column(
                            Modifier.clip(forma).background(if (sel) c.ouro.copy(alpha = 0.16f) else c.vidroSutil)
                                .border(1.dp, if (sel) c.ouro else c.borda, forma)
                                .clickable { escolhido = i }.padding(horizontal = 14.dp, vertical = 10.dp),
                        ) {
                            Text("${dataBr(p.inicio)} – ${dataBr(p.fim)}", color = if (sel) c.ouro else c.texto, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = Archivo)
                            Text(rotuloStatus(p.status) + " · ${p.entregas} entr.", color = c.texto3, fontSize = 11.5.sp)
                        }
                    }
                }

                // ------------------------------------------------ resumo do período
                atual?.let { p ->
                    PwCartao(destaque = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            PwRotulo("${dataBr(p.inicio)} a ${dataBr(p.fim)}", Modifier.weight(1f))
                            com.pwexpress.core.designsystem.PwSelo(rotuloStatus(p.status), when (p.status) { "pago" -> c.verde; "fechado" -> c.ouro; else -> c.texto3 })
                        }
                        Spacer(Modifier.height(6.dp))
                        val total = holerite?.total ?: p.total
                        if (total != null) {
                            PwValor(moedaBr(total), Modifier.fillMaxWidth(), tamanhoMax = 44)
                            Text(if (p.status == "pago") "pago" else if (p.status == "fechado") "a receber" else "até agora", color = c.texto2, fontFamily = Archivo, fontSize = 13.sp)
                        } else {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${p.entregas}", style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 44.sp), color = c.ouro)
                                Spacer(Modifier.width(8.dp))
                                Text("entrega${if (p.entregas == 1) "" else "s"}", color = c.texto2, fontFamily = Archivo, fontSize = 15.sp, modifier = Modifier.padding(bottom = 9.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PwNumero("Rotas", "${p.rotas}", Modifier.weight(1f), icone = PwIcones.Route)
                            PwNumero("Rodados", "%.1f km".format(br, p.km), Modifier.weight(1f), icone = PwIcones.Ruler)
                        }
                    }
                }

                // ------------------------------------------------ como fica o total: rotas + ajustes = a receber
                holerite?.let { h ->
                    val rotas = h.itens.mapNotNull { it.valor }.sum()
                    if (h.valoresVisiveis || h.ajustes.isNotEmpty()) PwCartao {
                        PwRotulo(if (h.valoresVisiveis) "Como fica o total" else "Ajustes")
                        Spacer(Modifier.height(6.dp))
                        @Composable fun linha(r: String, v: String?, cor: androidx.compose.ui.graphics.Color = c.texto, forte: Boolean = false) =
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(r, color = if (forte) c.texto else c.texto2, fontSize = 14.sp, fontWeight = if (forte) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
                                if (v != null) Text(v, color = cor, fontWeight = if (forte) FontWeight.Black else FontWeight.SemiBold, fontSize = if (forte) 16.sp else 14.sp)
                            }
                        if (h.valoresVisiveis) linha("${h.itens.size} rota${if (h.itens.size == 1) "" else "s"} concluída${if (h.itens.size == 1) "" else "s"}", moedaBr(rotas))
                        h.ajustes.forEach { (motivo, v) ->
                            linha(motivo.ifBlank { if (v >= 0) "Bônus" else "Desconto" }, if (h.valoresVisiveis) (if (v >= 0) "+ " else "− ") + moedaBr(kotlin.math.abs(v)) else null, if (v >= 0) c.verde else c.vermelho)
                        }
                        if (h.valoresVisiveis) {
                            androidx.compose.material3.HorizontalDivider(color = c.borda, modifier = Modifier.padding(vertical = 4.dp))
                            linha(if (atual?.status == "pago") "Pago" else "Você recebe", moedaBr(h.total ?: (rotas + h.ajustes.sumOf { it.second })), c.ouro, forte = true)
                            Text("Cada rota já vem com a sua parte (nunca passa do valor da rota). Os ajustes são bônus ou descontos do gestor.", color = c.texto3, fontSize = 12.sp)
                        }
                    }
                }

                // ------------------------------------------------ para onde o dinheiro vai
                CartaoChavesPix()

                // ------------------------------------------------ rotas do período
                PwRotulo("Rotas do período")
                when {
                    holerite == null -> PwCarregando()
                    holerite!!.itens.isEmpty() -> PwVazio("Nenhuma rota concluída neste período.", icone = PwIcones.Route)
                    else -> holerite!!.itens.forEach { LinhaExtrato(it) }
                }
            }
        }
    }
}

@Composable
private fun LinhaExtrato(f: RotaHolerite) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.ouro.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(PwIcones.Route, null, tint = c.ouro, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Rota #${f.numero}" + (f.clientes?.let { " · $it" } ?: ""), color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${dataCurta(f.concluidaEm)} · ${f.entregas} entrega${if (f.entregas == 1) "" else "s"} · ${"%.1f".format(br, f.km)} km", color = c.texto3, fontSize = 12.sp, maxLines = 1)
        }
        if (f.valor != null) {
            Spacer(Modifier.width(8.dp))
            TextoAjustado(moedaBr(f.valor), TextStyle(color = c.verde, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold), 15, Modifier.width(100.dp), minimo = 11)
        }
    }
}

/** Barras simples (valor por dia), a de [destaque] na cor da marca. */
@Composable
private fun GraficoBarras(valores: List<Double>, rotulos: List<String>, destaque: Int) {
    val c = Pw.cores
    val max = (valores.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val n = valores.size
            val passo = size.width / n
            val larg = passo * 0.56f
            valores.forEachIndexed { i, v ->
                val h = (v / max).toFloat() * (size.height - 6f)
                val x = i * passo + (passo - larg) / 2f
                drawRoundRect(c.borda, Offset(x, 0f), Size(larg, size.height), CornerRadius(10f, 10f))
                if (h > 0f) drawRoundRect(if (i == destaque) c.ouro else c.ouroEscuro.copy(alpha = 0.8f), Offset(x, size.height - h), Size(larg, h), CornerRadius(10f, 10f))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            rotulos.forEachIndexed { i, r ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(r, color = if (i == destaque) c.ouro else c.texto3, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(if (valores[i] > 0) "%.0f".format(br, valores[i]) else "—", color = c.texto2, fontSize = 10.5.sp, maxLines = 1)
                }
            }
        }
    }
}

/** Cartão de uma corrida do histórico (aba Corridas → Histórico). */
@Composable
internal fun CartaoFeita(f: CorridaFeita) {
    val c = Pw.cores
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Corrida ${f.numeroExibicao}", color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            com.pwexpress.core.designsystem.PwSelo(if (f.concluida) "Concluída" else "Cancelada", if (f.concluida) c.verde else c.texto3)
            Spacer(Modifier.weight(1f))
            if (f.concluida && f.valor != null) TextoAjustado(moedaBr(f.valor), TextStyle(color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold), 15, Modifier.width(110.dp), minimo = 11)
        }
        Spacer(Modifier.height(6.dp))
        f.coleta?.let { Endereco(PwIcones.ArrowUpFromLine, it, c.ouro) }
        f.entrega?.let { Endereco(PwIcones.ArrowDownToLine, it + if (f.paradas > 2) "  (+${f.paradas - 2})" else "", c.texto) }
        Spacer(Modifier.height(4.dp))
        Text(listOfNotNull(dataCurta(f.quando), kmTexto(f.distanciaM), f.empresa).joinToString(" · "), color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun Endereco(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String, cor: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = Pw.cores.texto, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
