package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private fun dataBr(iso: String?): String = iso?.take(10)?.let { runCatching { LocalDate.parse(it).format(DATA_BR) }.getOrNull() } ?: "—"
private fun lista(e: JsonElement?): List<JsonObject> = (e as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()

private val COR_FATURA = mapOf("rascunho" to Color(0xFFF59E0B), "emitida" to Color(0xFF60A5FA), "paga" to Color(0xFF2ECC71), "cancelada" to Color(0xFF8A8A8A))
private val ROT_FATURA = mapOf("rascunho" to "Rascunho", "emitida" to "Emitida", "paga" to "Paga", "cancelada" to "Cancelada")

/** Faturas dos clientes no crédito: geradas por ciclo (ou à mão), revisadas, emitidas e baixadas. Igual ao site. */
@Composable
internal fun FaturasG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var faturas by remember { mutableStateOf<List<JsonObject>?>(null) }
    var abertos by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var versao by remember { mutableIntStateOf(0) }
    var aberta by remember { mutableStateOf<JsonObject?>(null) }
    var gerar by remember { mutableStateOf(false) }
    var filtro by remember { mutableIntStateOf(0) }
    LaunchedEffect(teste, versao) {
        faturas = Gestao.tabela("faturas", "select=*&teste=eq.$teste&order=criado_em.desc&limit=300")
        abertos = Gestao.tabela("pedidos", "select=id,numero,concluido_em,valor,empresa_id&teste=eq.$teste&cobranca=eq.credito&status=eq.concluido&fatura_id=is.null&limit=2000")
    }
    PwPagina {
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = "Faturas dos clientes no crédito") else TituloTela("Carteira", "Faturas dos clientes no crédito", PwIcones.Wallet)
        abas()
        val emAberto = abertos.filter { it.str("empresa_id") != null }.groupBy { it.str("empresa_id")!! }
        if (emAberto.isNotEmpty()) PwCartao {
            PwRotulo("Ainda sem fatura")
            emAberto.entries.sortedByDescending { e -> e.value.sumOf { it.dbl("valor") ?: 0.0 } }.forEach { (id, ps) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(dados.empresa(id)?.nome ?: "—", color = c.texto, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${ps.size} · ${moedaBr(ps.sumOf { it.dbl("valor") ?: 0.0 })}", color = c.texto2, fontSize = 13.sp)
                }
            }
        }
        PwBotaoVidro("Gerar fatura agora", onClick = { gerar = true }, icone = PwIcones.Plus, modifier = Modifier.fillMaxWidth())
        val status = listOf(null, "rascunho", "emitida", "paga")
        val todas = faturas
        com.pwexpress.core.designsystem.PwFiltros(listOf("Todas" to 0, "Rascunho" to (todas?.count { it.str("status") == "rascunho" } ?: 0), "Emitidas" to 0, "Pagas" to 0), filtro, { filtro = it })
        when {
            todas == null -> PwCarregando()
            else -> {
                val l = todas.filter { status[filtro] == null || it.str("status") == status[filtro] }
                if (l.isEmpty()) PwVazio("Nenhuma fatura aqui.", icone = PwIcones.ReceiptText)
                l.forEach { f ->
                    val st = f.str("status") ?: "rascunho"
                    val forma = RoundedCornerShape(16.dp)
                    Row(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).clickable { aberta = f }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Fatura ${f.int("numero") ?: ""} · ${dados.empresa(f.str("empresa_id"))?.nome ?: "—"}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${dataBr(f.str("inicio"))} a ${dataBr(f.str("fim"))}" + (f.str("vencimento")?.let { " · vence ${dataBr(it)}" } ?: ""), color = c.texto3, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(moedaBr(f.dbl("total")), color = c.texto, fontWeight = FontWeight.Bold)
                            PwSelo(ROT_FATURA[st] ?: st, COR_FATURA[st] ?: c.texto3)
                        }
                    }
                }
            }
        }
    }
    aberta?.let { f -> DetalheFatura(f, dados.empresa(f.str("empresa_id"))?.nome ?: "—", onFechar = { aberta = null }, onMudou = { versao++; aberta = null }) }
    if (gerar) GerarFatura(teste, onFechar = { gerar = false }, onGerada = { gerar = false; versao++ })
}

@Composable
private fun DetalheFatura(f: JsonObject, cliente: String, onFechar: () -> Unit, onMudou: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var pedidos by remember { mutableStateOf<List<JsonObject>?>(null) }
    var ajuste by remember { mutableStateOf(f.dbl("ajuste")?.takeIf { it != 0.0 }?.let { numeroBr(it) }.orEmpty()) }
    var motivo by remember { mutableStateOf(f.str("ajuste_motivo").orEmpty()) }
    var vencimento by remember { mutableStateOf(dataBr(f.str("vencimento")).takeIf { it != "—" }.orEmpty()) }
    var metodo by remember { mutableStateOf("pix") }
    var erro by remember { mutableStateOf<String?>(null) }
    val id = f.str("id")!!
    val st = f.str("status") ?: "rascunho"
    LaunchedEffect(id) { pedidos = Gestao.tabela("pedidos", "select=id,numero,concluido_em,valor&fatura_id=eq.$id&order=concluido_em") }
    fun acao(a: String, msg: String, extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit = {}) {
        val corpo = buildJsonObject { put("p_fatura", id); put("p_acao", a); extra() }
        escopo.launch {
            Gestao.rpcJson("editar_fatura", corpo)
                .onSuccess { PwToast.mostrar(msg, TipoAviso.OK); onMudou() }.onFailure { erro = it.message }
        }
    }
    PwTelaCheia("Fatura ${f.int("numero") ?: ""}", onFechar, subtitulo = "$cliente · ${dataBr(f.str("inicio"))} a ${dataBr(f.str("fim"))}") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwNumero("Total", moedaBr(f.dbl("total")), Modifier.weight(1f), icone = PwIcones.ReceiptText, destaque = true)
            PwNumero("Subtotal", moedaBr(f.dbl("subtotal")), Modifier.weight(1f))
        }
        PwSelo(ROT_FATURA[st] ?: st, COR_FATURA[st] ?: c.texto3)
        PwRotulo("Pedidos")
        when (val ps = pedidos) {
            null -> PwCarregando()
            else -> ps.forEach { p ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("#${p.int("numero")} · ${dataBr(p.str("concluido_em"))}", color = c.texto, modifier = Modifier.weight(1f))
                    Text(moedaBr(p.dbl("valor")), color = c.texto, fontWeight = FontWeight.SemiBold)
                    if (st == "rascunho") PwBotaoIcone(PwIcones.X, "Tirar da fatura", { acao("tirar_pedido", "Pedido tirado da fatura.") { put("p_pedido", p.str("id")!!) } }, tamanho = 32.dp)
                }
            }
        }
        if (st == "rascunho") {
            PwCartao {
                PwRotulo("Ajuste")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwCampo(ajuste, { ajuste = it }, "R$ (+/−)", teclado = KeyboardType.Text, modifier = Modifier.weight(1f))
                    PwCampo(motivo, { motivo = it }, "Motivo", modifier = Modifier.weight(2f))
                }
                PwBotaoVidro("Salvar ajuste", onClick = {
                    acao("ajustar", "Ajuste salvo.") { put("p_ajuste", ajuste.replace(',', '.').toDoubleOrNull() ?: 0.0); put("p_motivo", motivo.ifBlank { null }?.let { JsonPrimitive(it) } ?: JsonNull) }
                }, icone = PwIcones.Check, modifier = Modifier.fillMaxWidth())
            }
            PwCampo(vencimento, { vencimento = it }, "Vencimento", dica = "dd/mm/aaaa (vazio = pelo prazo padrão)")
            PwBotaoOuro("Emitir para o cliente", onClick = {
                val iso = Regex("""(\d{2})/(\d{2})/(\d{4})""").matchEntire(vencimento.trim())?.destructured?.let { (d, m, a) -> "$a-$m-$d" }
                acao("emitir", "Fatura emitida: o cliente já vê no site.") { put("p_vencimento", iso?.let { JsonPrimitive(it) } ?: JsonNull) }
            }, icone = PwIcones.Send)
        }
        if (st == "emitida") {
            PwSegmentado(listOf("PIX" to null, "Cartão" to null, "Boleto" to null, "Dinheiro" to null), listOf("pix", "cartao", "boleto", "dinheiro").indexOf(metodo).coerceAtLeast(0),
                { metodo = listOf("pix", "cartao", "boleto", "dinheiro")[it] })
            PwBotaoOuro("Marcar como paga", onClick = { acao("paga", "Fatura baixada como paga.") { put("p_metodo", metodo) } }, icone = PwIcones.CircleCheck)
        }
        if (st == "paga") TextoDica("Paga em ${dataBr(f.str("paga_em"))}" + (f.str("pagamento_metodo")?.let { " · $it" } ?: ""))
        if (st == "rascunho" || st == "emitida") PwBotaoPerigo("Cancelar fatura", onClick = { acao("cancelar", "Fatura cancelada; os pedidos voltaram para em aberto.") }, modifier = Modifier.fillMaxWidth())
        PwErro(erro)
    }
}

@Composable
private fun GerarFatura(teste: Boolean, onFechar: () -> Unit, onGerada: () -> Unit) {
    val dados by Gestao.dados.collectAsState()
    val escopo = rememberCoroutineScope()
    var cliente by remember { mutableStateOf<String?>(null) }
    val hoje = LocalDate.now()
    var inicio by remember { mutableStateOf(hoje.withDayOfMonth(1).format(DATA_BR)) }
    var fim by remember { mutableStateOf(hoje.format(DATA_BR)) }
    var erro by remember { mutableStateOf<String?>(null) }
    fun iso(s: String) = Regex("""(\d{2})/(\d{2})/(\d{4})""").matchEntire(s.trim())?.destructured?.let { (d, m, a) -> "$a-$m-$d" }
    PwTelaCheia("Gerar fatura", onFechar) {
        PwSelecao("Cliente", dados.empresas.map { it.id to it.nome }, cliente, { cliente = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(inicio, { inicio = it }, "De", dica = "dd/mm/aaaa", modifier = Modifier.weight(1f))
            PwCampo(fim, { fim = it }, "Até", dica = "dd/mm/aaaa", modifier = Modifier.weight(1f))
        }
        TextoDica("Junta os pedidos concluídos no crédito desse período que ainda não estão em fatura. Nasce como rascunho para você revisar.")
        PwErro(erro)
        PwBotaoOuro("Gerar", onClick = {
            val a = iso(inicio); val b = iso(fim)
            if (cliente == null || a == null || b == null) { erro = "Escolha o cliente e as datas (dd/mm/aaaa)."; return@PwBotaoOuro }
            escopo.launch {
                Gestao.rpcJson("gerar_fatura", buildJsonObject { put("p_cliente", cliente!!); put("p_inicio", a); put("p_fim", b); put("p_teste", teste) })
                    .onSuccess { PwToast.mostrar("Fatura gerada como rascunho.", TipoAviso.OK); onGerada() }.onFailure { erro = it.message }
            }
        }, icone = PwIcones.Check)
    }
}

// =====================================================================
// Folha
// =====================================================================

/** Período (mesma regra do banco) que contém hoje, deslocado [passo] períodos. */
private fun periodo(tipo: String, inicioSemana: Int, passo: Int): Pair<LocalDate, LocalDate> {
    val hoje = LocalDate.now()
    return when (tipo) {
        "diario" -> hoje.plusDays(passo.toLong()).let { it to it }
        "mensal" -> hoje.withDayOfMonth(1).plusMonths(passo.toLong()).let { it to it.with(TemporalAdjusters.lastDayOfMonth()) }
        "quinzenal" -> {
            var q = (hoje.year * 12 + hoje.monthValue - 1) * 2 + (if (hoje.dayOfMonth > 15) 1 else 0) + passo
            val mes = Math.floorDiv(q, 2); q = Math.floorMod(q, 2)
            val i = LocalDate.of(Math.floorDiv(mes, 12), Math.floorMod(mes, 12) + 1, if (q == 1) 16 else 1)
            i to (if (q == 1) i.with(TemporalAdjusters.lastDayOfMonth()) else i.withDayOfMonth(15))
        }
        else -> {
            val ini = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.of(inicioSemana.coerceIn(1, 7)))).plusWeeks(passo.toLong())
            ini to ini.plusDays(6)
        }
    }
}

private val COR_HOL = mapOf("aberto" to Color(0xFFF59E0B), "fechado" to Color(0xFF60A5FA), "pago" to Color(0xFF2ECC71))

/** Folha de pagamento dos motoboys: período, rotas, cálculo, ajustes, fechar e pagar. Igual ao site. */
@Composable
internal fun FolhaG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    val tipos = listOf("diario" to "Dia", "semanal" to "Semana", "quinzenal" to "Quinzena", "mensal" to "Mês")
    val padrao = (dados.config["holerite_periodo"] as? JsonPrimitive)?.content ?: "semanal"
    var tipo by remember(padrao) { mutableStateOf(padrao) }
    var passo by remember { mutableIntStateOf(0) }
    var linhas by remember { mutableStateOf<List<JsonObject>?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    var aberto by remember { mutableStateOf<JsonObject?>(null) }
    val (ini, fim) = periodo(tipo, dados.cfgNum("holerite_inicio_semana", 1.0).toInt(), passo)
    LaunchedEffect(ini, fim, teste, versao) {
        linhas = null
        linhas = lista(Gestao.rpcJson("folha", buildJsonObject { put("p_inicio", ini.toString()); put("p_fim", fim.toString()); put("p_teste", teste) }).getOrNull())
    }
    PwPagina {
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = "Folha dos motoboys") else TituloTela("Carteira", "Folha dos motoboys", PwIcones.Wallet)
        abas()
        PwSegmentado(tipos.map { it.second to null }, tipos.indexOfFirst { it.first == tipo }.coerceAtLeast(0), { tipo = tipos[it].first; passo = 0 })
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwBotaoIcone(PwIcones.ChevronLeft, "Anterior", { passo-- })
            Text("${ini.format(DATA_BR)} a ${fim.format(DATA_BR)}", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            PwBotaoIcone(PwIcones.ChevronRight, "Próximo", { passo++ }, habilitado = passo < 0)
        }
        val l = linhas ?: run { PwCarregando(); return@PwPagina }
        val total = l.sumOf { it.obj("holerite")?.dbl("total") ?: ((it.dbl("calculado") ?: 0.0) + (it.dbl("ajustes_rotas") ?: 0.0) + (it.dbl("ajustes") ?: 0.0)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwNumero("Folha", moedaBr(total), Modifier.weight(1f), icone = PwIcones.Users, destaque = true)
            PwNumero("Faturado", moedaBr(l.sumOf { it.dbl("faturado") ?: 0.0 }), Modifier.weight(1f), icone = PwIcones.TrendingUp)
        }
        if (l.isEmpty()) PwVazio("Ninguém rodou neste período.", icone = PwIcones.Users)
        l.forEach { m ->
            val h = m.obj("holerite")
            val st = h?.str("status") ?: "aberto"
            val forma = RoundedCornerShape(16.dp)
            Row(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).clickable { aberto = m }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(m.str("nome").orEmpty(), color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${m.int("rotas") ?: 0} rotas · ${m.int("entregas") ?: 0} entregas · ${numeroBr(m.dbl("km") ?: 0.0)} km", color = c.texto3, fontSize = 12.sp)
                    if (m.obj("chave_pix") == null) Text("sem chave PIX", color = c.vermelho, fontSize = 11.5.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(moedaBr(h?.dbl("total") ?: ((m.dbl("calculado") ?: 0.0) + (m.dbl("ajustes_rotas") ?: 0.0) + (m.dbl("ajustes") ?: 0.0))), color = c.texto, fontWeight = FontWeight.Bold)
                    PwSelo(mapOf("aberto" to "Em aberto", "fechado" to "Fechado", "pago" to "Pago")[st] ?: st, COR_HOL[st] ?: c.texto3)
                }
            }
        }
    }
    aberto?.let { m -> HoleriteG(m, ini.toString(), fim.toString(), teste, onFechar = { aberto = null }, onMudou = { versao++ }) }
}

@Composable
private fun HoleriteG(linha: JsonObject, ini: String, fim: String, teste: Boolean, onFechar: () -> Unit, onMudou: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    val mid = linha.str("motoboy_id")!!
    var h by remember { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    var desc by remember { mutableStateOf("") }
    var valor by remember { mutableStateOf("") }
    var vis by remember { mutableStateOf("tudo") }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(mid, ini, fim, versao) {
        h = Gestao.rpcJson("holerite", buildJsonObject { put("p_motoboy", mid); put("p_inicio", ini); put("p_fim", fim); put("p_teste", teste) }).getOrNull() as? JsonObject
    }
    fun rodar(msg: String, f: String, args: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit) {
        val corpo = buildJsonObject(args)   // monta já: os campos podem ser limpos logo depois
        escopo.launch {
            Gestao.rpcJson(f, corpo).onSuccess { PwToast.mostrar(msg, TipoAviso.OK); erro = null; versao++; onMudou() }.onFailure { erro = it.message }
        }
    }
    val st = h?.str("status") ?: "aberto"
    PwTelaCheia("Holerite · ${linha.str("nome")}", onFechar, subtitulo = "${dataBr(ini)} a ${dataBr(fim)}") {
        val x = h ?: run { PwCarregando(); return@PwTelaCheia }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwNumero("A pagar", moedaBr(x.dbl("total")), Modifier.weight(1f), icone = PwIcones.Wallet, destaque = true)
            PwNumero("Rotas", "${x.int("rotas") ?: 0} · ${x.int("entregas") ?: 0} entr.", Modifier.weight(1f), icone = PwIcones.Route)
        }
        PwSelo(mapOf("aberto" to "Em aberto", "fechado" to "Fechado", "pago" to "Pago")[st] ?: st, COR_HOL[st] ?: c.texto3)
        linha.obj("chave_pix")?.let { k -> TextoDica("PIX: ${k.str("chave")} (${k.str("titular")})") } ?: Text("Sem chave PIX cadastrada.", color = c.vermelho, fontSize = 13.sp)
        PwRotulo("Rotas")
        x.arr("itens").forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Rota #${r.int("numero")} · ${r.int("entregas") ?: 0} entr. · ${numeroBr(r.dbl("km") ?: 0.0)} km", color = c.texto, fontSize = 13.5.sp)
                    Text(listOfNotNull(dataBr(r.str("concluida_em")), r.str("clientes"), r.dbl("ajuste")?.takeIf { it != 0.0 }?.let { "ajuste ${moedaBr(it)}" + (r.str("ajuste_motivo")?.let { m -> " ($m)" } ?: "") }).joinToString(" · "),
                        color = c.texto3, fontSize = 12.sp, maxLines = 2)
                }
                Text(moedaBr(r.dbl("valor")), color = c.texto, fontWeight = FontWeight.SemiBold)
            }
        }
        // salário, diárias, horas e mínimo do período (modelo de pagamento do motoboy)
        x.arr("fixos").takeIf { it.isNotEmpty() }?.let { fx ->
            PwRotulo("Salário, diárias e horas")
            fx.forEach { f ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(f.str("descricao").orEmpty(), color = c.texto, fontSize = 13.5.sp)
                        f.str("detalhe")?.let { Text(it, color = c.texto3, fontSize = 12.sp) }
                    }
                    Text(moedaBr(f.dbl("valor")), color = c.texto, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwRotulo("Ajustes do período"); Spacer(Modifier.width(8.dp))
            com.pwexpress.core.designsystem.PwDica("Ajustes", "Bônus (valor positivo) ou desconto (com −). Em cada ajuste você escolhe o que o motoboy vê: tudo, só o valor (aparece como \"Ajuste\") ou nada (não aparece para ele nem no total dele).")
        }
        x.arr("ajustes").forEach { a ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(a.str("descricao").orEmpty(), color = c.texto)
                    val vAt = a.str("visibilidade") ?: "tudo"
                    val (rot, ic) = VIS_AJUSTE[vAt] ?: VIS_AJUSTE.getValue("tudo")
                    Row(Modifier.padding(top = 2.dp).clip(RoundedCornerShape(99.dp)).background(c.vidro)
                        .clickable(enabled = st == "aberto") { rodar("O motoboy agora vê: ${VIS_AJUSTE[proximaVis(vAt)]?.first?.lowercase()}.", "ajuste_holerite_visibilidade") { put("p_id", a.str("id")!!); put("p_visibilidade", proximaVis(vAt)) } }
                        .padding(horizontal = 8.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ic, null, tint = c.texto3, modifier = Modifier.size(12.dp)); Spacer(Modifier.width(4.dp))
                        Text("Motoboy vê: ${rot.lowercase()}", color = c.texto3, fontSize = 11.5.sp)
                    }
                }
                Text(moedaBr(a.dbl("valor")), color = if ((a.dbl("valor") ?: 0.0) >= 0) c.verde else c.vermelho, fontWeight = FontWeight.SemiBold)
                if (st == "aberto") PwBotaoIcone(PwIcones.Trash2, "Remover", { rodar("Ajuste removido.", "remover_ajuste_holerite") { put("p_ajuste", a.str("id")!!) } }, tamanho = 32.dp, cor = c.vermelho)
            }
        }
        if (st == "aberto") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(desc, { desc = it }, "Descrição", dica = "Ex.: bônus, vale", modifier = Modifier.weight(2f))
                PwCampo(valor, { valor = it }, "R$ (+/−)", modifier = Modifier.weight(1f))
            }
            Row(Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                VIS_AJUSTE.forEach { (k, v) -> com.pwexpress.core.designsystem.PwPilula(v.first, v.second, { vis = k }, ligada = vis == k) }
            }
            TextoDica(when (vis) { "valor" -> "O motoboy vê só o valor, como \"Ajuste\"."; "oculto" -> "O motoboy não vê este ajuste (nem no total dele)."; else -> "O motoboy vê a descrição e o valor." })
            PwBotaoVidro("Adicionar ajuste", onClick = {
                val v = valor.replace('−', '-').replace('–', '-').replace("R$", "").replace(" ", "").replace(',', '.').toDoubleOrNull()?.takeIf { it != 0.0 }
                if (desc.isBlank() || v == null) { erro = "Descreva e informe um valor diferente de zero (use − para desconto)."; return@PwBotaoVidro }
                rodar("Ajuste adicionado.", "ajuste_holerite") { put("p_motoboy", mid); put("p_inicio", ini); put("p_fim", fim); put("p_descricao", desc.trim()); put("p_valor", v); put("p_teste", teste); put("p_visibilidade", vis) }
                desc = ""; valor = ""
            }, icone = PwIcones.Plus, modifier = Modifier.fillMaxWidth())
        }
        PwErro(erro)
        when (st) {
            "aberto" -> PwBotaoOuro("Fechar holerite", onClick = { rodar("Holerite fechado.", "situacao_holerite") { put("p_motoboy", mid); put("p_inicio", ini); put("p_fim", fim); put("p_acao", "fechar"); put("p_teste", teste) } }, icone = PwIcones.Lock)
            "fechado" -> {
                PwBotaoOuro("Marcar como pago", onClick = { rodar("Holerite pago.", "situacao_holerite") { put("p_motoboy", mid); put("p_inicio", ini); put("p_fim", fim); put("p_acao", "pago"); put("p_teste", teste) } }, icone = PwIcones.CircleCheck)
                PwBotaoVidro("Reabrir", onClick = { rodar("Holerite reaberto.", "situacao_holerite") { put("p_motoboy", mid); put("p_inicio", ini); put("p_fim", fim); put("p_acao", "reabrir"); put("p_teste", teste) } }, icone = PwIcones.LockOpen, modifier = Modifier.fillMaxWidth())
            }
            else -> x.str("pago_em")?.let { TextoDica("Pago em ${dataBr(it)}") }
        }
        Spacer(Modifier.width(1.dp))
    }
}

@Suppress("unused") private val semUso: JsonElement = JsonNull

private val VIS_AJUSTE = linkedMapOf("tudo" to ("Tudo" to PwIcones.Eye), "valor" to ("Só o valor" to PwIcones.EyeOff), "oculto" to ("Nada" to PwIcones.Lock))
private fun proximaVis(v: String) = when (v) { "tudo" -> "valor"; "valor" -> "oculto"; else -> "tudo" }
