package com.pwexpress.feature.corridas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.feature.corridas.data.Referencia

/** Pontos de referência perto do endereço (até 3), em linhas discretas. */
@Composable
fun ReferenciasParada(referencias: List<Referencia>, modifier: Modifier = Modifier) {
    if (referencias.isEmpty()) return
    val c = Pw.cores
    Column(modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        referencias.forEach { r ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Landmark, null, tint = c.ouro, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                if (r.relacao != null) Text(r.relacao, color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo)
                else {
                    Text(r.nome, color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo)
                    val extra = listOfNotNull(r.tipo, r.distanciaM?.let { if (it < 1000) "$it m" else "${it / 1000} km" }).joinToString(" · ")
                    if (extra.isNotEmpty()) Text("  $extra", color = c.texto3, fontSize = 12.sp)
                }
            }
        }
    }
}

/** Rotas automáticas: de qual cliente/pedido é a parada e os itens (com selos de perigoso/frágil). */
/** Cliente, pedido e os pacotes desta parada: ícone de cada um; toque para ver o detalhe. */
@Composable
fun ItensParada(p: com.pwexpress.feature.corridas.data.Parada, modifier: Modifier = Modifier) {
    if (p.cliente == null && p.pedidoNumero == null && p.itens.isEmpty()) return
    val c = Pw.cores
    var aberto by androidx.compose.runtime.remember(p.id) { androidx.compose.runtime.mutableStateOf<Int?>(null) }
    Column(modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (p.cliente != null || p.pedidoNumero != null) {
            Text(
                listOfNotNull(p.cliente, p.pedidoNumero?.let { "pedido #$it" }).joinToString(" · "),
                color = c.texto2, fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp,
            )
        }
        if (p.itens.isNotEmpty()) {
            val presos = p.itens.count { !it.todas }
            Text(
                if (p.tipo == com.pwexpress.feature.corridas.data.TipoParada.COLETA) "Pegar aqui" else if (presos > 0) "Entregar aqui" else "Vai para todas as entregas",
                color = c.texto3, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
            )
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                p.itens.forEachIndexed { idx, i ->
                    val ativo = aberto == idx
                    val forma = androidx.compose.foundation.shape.RoundedCornerShape(99.dp)
                    Row(
                        Modifier.clip(forma)
                            .background(if (ativo) c.ouro.copy(alpha = 0.16f) else c.vidro)
                            .border(1.dp, if (ativo) c.ouro else if (i.perigoso) c.vermelho.copy(alpha = 0.6f) else c.borda, forma)
                            .clickable { aberto = if (ativo) null else idx }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(com.pwexpress.core.designsystem.iconePorNome(i.icone), null, tint = if (i.perigoso) c.vermelho else c.ouro, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text((if (i.quantidade > 1) "${i.quantidade}× " else "") + i.descricao.ifBlank { i.categoria ?: "Pacote" }, color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        if (i.todas && p.tipo == com.pwexpress.feature.corridas.data.TipoParada.ENTREGA && presos > 0) { Spacer(Modifier.width(4.dp)); Text("todas", color = c.texto3, fontSize = 10.5.sp) }
                    }
                }
            }
            aberto?.let { p.itens.getOrNull(it) }?.let { i ->
                val forma = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                Column(Modifier.fillMaxWidth().clip(forma).background(c.vidro).border(1.dp, c.borda, forma).padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    @Composable fun linha(r: String, v: String) = Row { Text(r, color = c.texto3, fontSize = 12.sp, modifier = Modifier.width(104.dp)); Text(v, color = c.texto, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold) }
                    linha("Item", i.descricao.ifBlank { "—" })
                    i.categoria?.let { linha("Tipo", it + (if (i.perigoso) " · perigoso" else "") + (if (i.fragil) " · frágil" else "")) }
                    linha("Quantidade", i.quantidade.toString())
                    i.pesoKg?.let { linha("Peso", "${com.pwexpress.core.designsystem.numeroBr(it)} kg cada") }
                    i.medidas?.let { linha("Medidas", it) }
                    i.valorDeclarado?.let { linha("Valor declarado", "R$ " + "%.2f".format(java.util.Locale("pt", "BR"), it)) }
                    i.observacoes?.let { linha("Obs.", it) }
                    if (i.todas) linha("Onde", if (p.tipo == com.pwexpress.feature.corridas.data.TipoParada.COLETA) "Qualquer coleta" else "Todas as entregas")
                    if (i.perigoso) Row(verticalAlignment = Alignment.CenterVertically) { Icon(PwIcones.Alerta, null, tint = c.vermelho, modifier = Modifier.size(13.dp)); Spacer(Modifier.width(4.dp)); Text("Produto perigoso: cuidado no transporte.", color = c.vermelho, fontSize = 12.sp) }
                }
            }
        }
    }
}
