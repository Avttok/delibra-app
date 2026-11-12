package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.put
import java.time.LocalDate

private data class TipoRel(val v: String, val nome: String, val dica: String, val colunas: List<String>, val dinheiro: List<String>)
private val TIPOS_REL = listOf(
    TipoRel("rotas", "Rotas", "Cada rota concluída ou cancelada.", listOf("Rota", "Data", "Situação", "Cliente", "Motoboy", "Paradas", "Km", "Valor", "Motoboy recebe", "Empresa fica"), listOf("Valor", "Motoboy recebe", "Empresa fica")),
    TipoRel("pedidos", "Pedidos", "Todos os pedidos que chegaram.", listOf("Pedido", "Data", "Cliente", "Situação", "Origem", "Km estimado", "Valor", "Pagamento"), listOf("Valor")),
    TipoRel("motoboys", "Motoboys", "Quanto cada um fez e tem a receber.", listOf("Motoboy", "Rotas", "Entregas", "Km", "Valor das rotas", "A receber"), listOf("Valor das rotas", "A receber")),
    TipoRel("clientes", "Clientes", "Pedidos e faturamento de cada cliente.", listOf("Cliente", "Pedidos", "Entregues", "Cancelados", "Faturado"), listOf("Faturado")),
)
private val SIT_REL = mapOf("recebido" to "Para revisar", "aprovado" to "Aprovado", "roteirizado" to "Em rota", "em_rota" to "Em rota", "concluido" to "Entregue", "cancelado" to "Cancelado", "recusado" to "Recusado")

/** Relatórios da carteira (igual ao site): rotas, pedidos, motoboys e clientes no período, em planilha ou por e-mail. */
@Composable
internal fun RelatoriosG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var tipo by remember { mutableIntStateOf(0) }
    var periodo by remember { mutableIntStateOf(1) }
    var linhas by remember { mutableStateOf<List<JsonObject>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf(PwCore.auth.sessao.value?.email.orEmpty()) }
    var enviando by remember { mutableStateOf(false) }
    val hoje = LocalDate.now()
    val (de, ate) = when (periodo) { 0 -> hoje to hoje; 1 -> hoje.withDayOfMonth(1) to hoje; 2 -> hoje.minusMonths(1).withDayOfMonth(1) to hoje.withDayOfMonth(1).minusDays(1); else -> hoje.minusDays(89) to hoje }
    val t = TIPOS_REL[tipo]
    LaunchedEffect(tipo, periodo) {
        linhas = null
        Gestao.rpcJson("relatorio", buildJsonObject { put("p_tipo", t.v); put("p_de", de.toString()); put("p_ate", ate.toString()) })
            .onSuccess { j -> linhas = (j as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }; erro = null }.onFailure { erro = it.message; linhas = emptyList() }
    }
    fun valor(o: JsonObject, col: String): String {
        val p = o[col] as? JsonPrimitive ?: return ""
        return if (col == "Situação") SIT_REL[p.content] ?: p.content else p.content
    }
    PwPagina {
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = "Relatórios") else TituloTela("Carteira", "Relatórios", PwIcones.Wallet)
        abas()
        PwFiltros(TIPOS_REL.map { it.nome to 0 }, tipo, { tipo = it })
        TextoDica(t.dica)
        PwFiltros(listOf("Hoje" to 0, "Este mês" to 0, "Mês passado" to 0, "90 dias" to 0), periodo, { periodo = it })
        PwErro(erro)
        val l = linhas ?: run { PwCarregando(); return@PwPagina }
        if (l.isEmpty()) { PwVazio("Nada neste período.", icone = PwIcones.ReceiptText); return@PwPagina }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwNumero("Linhas", "${l.size}", Modifier.weight(1f))
            t.dinheiro.lastOrNull()?.let { col -> PwNumero(col, moedaBr(l.sumOf { (it[col] as? JsonPrimitive)?.doubleOrNull ?: 0.0 }), Modifier.weight(1f)) }
        }
        PwBotaoOuro("Compartilhar planilha (Excel)", icone = PwIcones.Download, onClick = {
            val csv = "﻿" + buildString {
                appendLine(t.colunas.joinToString(";"))
                l.forEach { o -> appendLine(t.colunas.joinToString(";") { col -> valor(o, col).let { v -> if (o[col] is JsonPrimitive && (o[col] as JsonPrimitive).doubleOrNull != null && col != "Data") v.replace('.', ',') else v }.replace(';', ',') }) }
            }
            runCatching {
                val arq = java.io.File(java.io.File(ctx.cacheDir, "fotos").apply { mkdirs() }, "relatorio-${t.v}-$de-a-$ate.csv").apply { writeText(csv) }
                val uri = androidx.core.content.FileProvider.getUriForFile(ctx, "${ctx.packageName}.comprovantes", arq)
                ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/csv")
                    .putExtra(android.content.Intent.EXTRA_STREAM, uri).putExtra(android.content.Intent.EXTRA_SUBJECT, "Relatório de ${t.nome.lowercase()}")
                    .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION), "Compartilhar relatório"))
            }.onFailure {
                // sem o compartilhamento de arquivo: manda como texto
                runCatching { ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, csv), "Compartilhar relatório")) }
            }
        })
        PwCartao {
            PwRotulo("Mandar por e-mail")
            PwCampo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
            PwBotaoVidro(if (enviando) "Enviando…" else "Enviar com a planilha anexada", icone = PwIcones.Mail, habilitado = !enviando && email.contains("@"), modifier = Modifier.fillMaxWidth(), onClick = {
                enviando = true
                escopo.launch {
                    runCatching { PwCore.rpc.site("/api/relatorios/email", buildJsonObject { put("tipo", t.v); put("de", de.toString()); put("ate", ate.toString()); put("para", email.trim()) }) }
                        .onSuccess { r -> when {
                            r.ok -> PwToast.mostrar("Relatório enviado para ${email.trim()}.")
                            r.corpo.contains("nao_conectado") -> PwToast.mostrar("O envio de e-mail ainda não foi conectado. Por enquanto, compartilhe a planilha.", TipoAviso.AVISO)
                            else -> PwToast.mostrar("Não deu para enviar agora.", TipoAviso.ERRO)
                        } }
                        .onFailure { PwToast.mostrar("Sem internet.", TipoAviso.ERRO) }
                    enviando = false
                }
            })
        }
        l.take(100).forEach { o ->
            PwCartao(padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
                Text(t.colunas.take(2).joinToString(" · ") { valor(o, it) }, color = c.texto, fontWeight = FontWeight.SemiBold)
                Column(Modifier.padding(top = 4.dp)) {
                    t.colunas.drop(2).chunked(2).forEach { par ->
                        Text(par.joinToString("   ") { col -> "$col: " + if (col in t.dinheiro) moedaBr((o[col] as? JsonPrimitive)?.doubleOrNull) else valor(o, col) }, color = c.texto2, fontSize = 12.5.sp)
                    }
                }
            }
        }
        if (l.size > 100) TextoDica("Mostrando 100 de ${l.size}. A planilha leva tudo.")
    }
}
