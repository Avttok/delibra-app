package com.pwexpress.feature.gestor.ui

import com.pwexpress.core.tentar

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.str
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** As "peças do lego" do catálogo. */
enum class Catalogo(val tabela: String, val titulo: String, val ordem: String) {
    ZONAS("zonas", "Zonas de preço", "nome"), HORARIOS("faixas_horario", "Horários de pico", "inicio"),
    CATEGORIAS("categorias_produto", "Categorias de produto", "ordem"), TIPOS("tipos_pedido", "Tipos de pedido", "ordem"),
}

private fun num(s: String) = s.trim().replace(',', '.').toDoubleOrNull()
private fun txt(d: Double?) = d?.let { numeroBr(it) }.orEmpty()

/**
 * Lista e edição de um catálogo (igual ao site). Com [clienteId], as zonas/horários são só daquele cliente.
 */
@Composable
fun CatalogoG(cat: Catalogo, onVoltar: (() -> Unit)?, clienteId: String? = null) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var linhas by remember { mutableStateOf<List<JsonObject>?>(null) }
    var editando by remember { mutableStateOf<JsonObject?>(null) }
    var novo by remember { mutableStateOf(false) }
    var versao by remember { mutableStateOf(0) }
    LaunchedEffect(cat, versao, clienteId) {
        val filtro = if (clienteId != null) "&cliente_id=eq.$clienteId" else ""
        linhas = Gestao.tabela(cat.tabela, "select=*&order=${cat.ordem}$filtro")
    }
    val conteudo: @Composable () -> Unit = {
        if (cat == Catalogo.ZONAS) {
            val zs = linhas.orEmpty().filter { it.bool("ativa") }
            if (zs.isNotEmpty()) Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))) {
                PwMapa(paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(220.dp),
                    marcadores = zs.mapNotNull { z -> val la = z.dbl("lat"); val lo = z.dbl("lng"); if (la == null || lo == null) null else
                        com.pwexpress.core.mapa.MarcadorMapa(z.str("id")!!, la, lo, z.str("cor") ?: "#D4AF37", z.str("nome").orEmpty(), pino = false) },
                    enquadrar = zs.size)
            }
        }
        TextoDica(when (cat) {
            Catalogo.ZONAS -> if (clienteId != null) "Zonas que só valem para este cliente." else "Se algum ponto do pedido cair dentro do raio, a zona soma no orçamento."
            Catalogo.HORARIOS -> "Faixas de horário com acréscimo (pelo horário da coleta)."
            Catalogo.CATEGORIAS -> "Cada categoria escolhe o que o cliente precisa informar (peso, medidas, valor declarado…) e quanto soma."
            Catalogo.TIPOS -> "Os tipos que o cliente escolhe no pedido (e que podem ter regras próprias em Exceções)."
        })
        PwBotaoOuro("Novo", onClick = { novo = true }, icone = PwIcones.Plus)
        when {
            linhas == null -> PwCarregando()
            linhas!!.isEmpty() -> PwVazio("Nada cadastrado ainda.")
            else -> linhas!!.forEach { l -> LinhaCatalogo(cat, l, dados.empresa(l.str("cliente_id"))?.nome) { editando = l } }
        }
    }
    if (onVoltar != null) PwPagina { Cabecalho(cat.titulo, onVoltar); conteudo() } else conteudo()

    if (novo || editando != null) EditorCatalogo(cat, editando, clienteId, onFechar = { novo = false; editando = null }, onSalvo = { novo = false; editando = null; versao++ })
}

@Composable
private fun LinhaCatalogo(cat: Catalogo, l: JsonObject, cliente: String?, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    val ativo = if (cat == Catalogo.TIPOS) l.bool("ativo") else l.bool("ativa")
    Row(Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, c.borda, forma).clickable(onClick = onClick).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        val cor = l.str("cor")?.let { tentar { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: c.ouro
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(cor.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(if (l.bool("perigoso")) PwIcones.FlaskConical else iconeDoNome(l.str("icone")), null, tint = if (l.bool("perigoso")) c.vermelho else cor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(l.str("nome").orEmpty(), color = if (ativo) c.texto else c.texto3, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(resumo(cat, l) + (cliente?.let { " · só $it" } ?: ""), color = c.texto3, fontSize = 12.sp, maxLines = 2)
        }
        if (!ativo) PwSelo("Desligado", c.texto3)
        Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
    }
}

private fun resumo(cat: Catalogo, l: JsonObject): String = when (cat) {
    Catalogo.ZONAS -> listOfNotNull(l.dbl("raio_m")?.let { if (it < 1000) "${it.toInt()} m" else "${numeroBr(it / 1000)} km" },
        l.dbl("fator_pct")?.takeIf { it != 0.0 }?.let { "+${numeroBr(it)}%" }, l.dbl("adicional")?.takeIf { it != 0.0 }?.let { "+${moedaBr(it)}" }).joinToString(" · ")
    Catalogo.HORARIOS -> listOfNotNull("${l.str("inicio")?.take(5)}–${l.str("fim")?.take(5)}",
        (l["dias"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() }?.joinToString(" ") { DIAS_CURTOS[it] },
        l.dbl("fator_pct")?.takeIf { it != 0.0 }?.let { "+${numeroBr(it)}%" }, l.dbl("adicional")?.takeIf { it != 0.0 }?.let { "+${moedaBr(it)}" }).joinToString(" · ")
    Catalogo.CATEGORIAS -> listOfNotNull("perigoso".takeIf { l.bool("perigoso") }, "frágil".takeIf { l.bool("fragil") },
        l.dbl("adicional_item")?.takeIf { it != 0.0 }?.let { "+${moedaBr(it)}/item" }, l.dbl("fator_pct")?.takeIf { it != 0.0 }?.let { "+${numeroBr(it)}%" },
        "pede " + listOf("campo_peso" to "peso", "campo_medidas" to "medidas", "campo_valor" to "valor").filter { (k, _) -> l.str(k) != "oculto" }.joinToString(", ") { it.second }.ifEmpty { "só a quantidade" }).joinToString(" · ")
    Catalogo.TIPOS -> listOfNotNull(l.str("chave"), l.str("descricao")).joinToString(" · ")
}

private val DIAS_CURTOS = listOf("Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb")
private val CORES = listOf("#D4AF37", "#38BDF8", "#22C55E", "#F97316", "#A78BFA", "#EF4444")
private val CAMPOS = listOf("campo_quantidade" to "Quantidade", "campo_descricao" to "Descrição", "campo_peso" to "Peso", "campo_medidas" to "Medidas", "campo_valor" to "Valor declarado")
private val VALORES_CAMPO = listOf("oculto" to "Não pede", "opcional" to "Opcional", "obrigatorio" to "Obrigatório")

@Composable
private fun EditorCatalogo(cat: Catalogo, l: JsonObject?, clienteId: String?, onFechar: () -> Unit, onSalvo: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val escopo = rememberCoroutineScope()
    val id = l?.str("id")
    var nome by remember { mutableStateOf(l?.str("nome").orEmpty()) }
    var descricao by remember { mutableStateOf(l?.str("descricao").orEmpty()) }
    var icone by remember { mutableStateOf(l?.str("icone")) }
    var ativo by remember { mutableStateOf(l?.let { if (cat == Catalogo.TIPOS) it.bool("ativo") else it.bool("ativa") } ?: true) }
    var fator by remember { mutableStateOf(txt(l?.dbl("fator_pct"))) }
    var adicional by remember { mutableStateOf(txt(l?.dbl("adicional"))) }
    // zona
    var cliente by remember { mutableStateOf(l?.str("cliente_id") ?: clienteId) }
    var lat by remember { mutableStateOf(l?.dbl("lat")) }
    var lng by remember { mutableStateOf(l?.dbl("lng")) }
    var raioKm by remember { mutableStateOf(txt(l?.dbl("raio_m")?.div(1000.0) ?: 1.5)) }
    var cor by remember { mutableStateOf(l?.str("cor") ?: CORES.first()) }
    var marcando by remember { mutableStateOf(false) }
    // horário
    var inicio by remember { mutableStateOf(l?.str("inicio")?.take(5) ?: "17:00") }
    var fim by remember { mutableStateOf(l?.str("fim")?.take(5) ?: "19:00") }
    var dias by remember { mutableStateOf((l?.get("dias") as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content?.toIntOrNull() }?.toSet() ?: setOf(1, 2, 3, 4, 5)) }
    // categoria
    var perigoso by remember { mutableStateOf(l?.bool("perigoso") ?: false) }
    var classe by remember { mutableStateOf(l?.str("classe_risco").orEmpty()) }
    var fragil by remember { mutableStateOf(l?.bool("fragil") ?: false) }
    var porItem by remember { mutableStateOf(txt(l?.dbl("adicional_item"))) }
    var fixo by remember { mutableStateOf(txt(l?.dbl("adicional_fixo"))) }
    var campos by remember { mutableStateOf(CAMPOS.associate { (k, _) -> k to (l?.str(k) ?: if (k == "campo_valor") "oculto" else "opcional") }) }
    // tipo
    var chave by remember { mutableStateOf(l?.str("chave").orEmpty()) }
    var erro by remember { mutableStateOf<String?>(null) }
    var salvando by remember { mutableStateOf(false) }

    fun salvar() {
        erro = when {
            nome.isBlank() -> "Dê um nome."
            cat == Catalogo.ZONAS && (lat == null || lng == null) -> "Marque o centro da zona no mapa."
            cat == Catalogo.HORARIOS && (!Regex("^\\d{2}:\\d{2}$").matches(inicio) || !Regex("^\\d{2}:\\d{2}$").matches(fim)) -> "Horário no formato 00:00."
            cat == Catalogo.TIPOS && l == null && !Regex("^[a-z0-9_]{2,40}$").matches(chave) -> "Chave: letras minúsculas, números e _ (ex.: farmacia)."
            else -> null
        }
        if (erro != null) return
        val linha = buildJsonObject {
            put("nome", nome.trim())
            when (cat) {
                Catalogo.ZONAS -> {
                    put("cliente_id", cliente?.let { JsonPrimitive(it) } ?: JsonNull); put("lat", lat!!); put("lng", lng!!)
                    put("raio_m", ((num(raioKm) ?: 1.0) * 1000).toInt().coerceIn(50, 200000)); put("fator_pct", num(fator) ?: 0.0); put("adicional", num(adicional) ?: 0.0)
                    put("cor", cor); put("ativa", ativo); put("icone", icone?.let { JsonPrimitive(it) } ?: JsonNull)
                }
                Catalogo.HORARIOS -> {
                    put("cliente_id", cliente?.let { JsonPrimitive(it) } ?: JsonNull); put("inicio", inicio); put("fim", fim)
                    put("dias", buildJsonArray { dias.sorted().forEach { add(JsonPrimitive(it)) } }); put("fator_pct", num(fator) ?: 0.0); put("adicional", num(adicional) ?: 0.0)
                    put("ativa", ativo); put("icone", icone?.let { JsonPrimitive(it) } ?: JsonNull)
                }
                Catalogo.CATEGORIAS -> {
                    put("descricao", descricao.trim().ifEmpty { null }?.let { JsonPrimitive(it) } ?: JsonNull); put("perigoso", perigoso)
                    put("classe_risco", classe.trim().takeIf { perigoso && it.isNotEmpty() }?.let { JsonPrimitive(it) } ?: JsonNull); put("fragil", fragil)
                    put("adicional_item", num(porItem) ?: 0.0); put("adicional_fixo", num(fixo) ?: 0.0); put("fator_pct", num(fator) ?: 0.0)
                    campos.forEach { (k, v) -> put(k, v) }; put("ativa", ativo); put("icone", icone?.let { JsonPrimitive(it) } ?: JsonNull)
                }
                Catalogo.TIPOS -> {
                    if (l == null) put("chave", chave); put("descricao", descricao.trim().ifEmpty { null }?.let { JsonPrimitive(it) } ?: JsonNull)
                    put("ativo", ativo); put("icone", icone?.let { JsonPrimitive(it) } ?: JsonNull)
                }
            }
        }
        salvando = true
        escopo.launch {
            val r = if (cat == Catalogo.TIPOS && l != null) tentar {
                val resp = com.pwexpress.core.PwCore.rpc.rest("PATCH", "/rest/v1/tipos_pedido?chave=eq.${l.str("chave")}", linha.toString(), "return=representation")
                if (!resp.ok) error(Gestao.traduzir(resp.corpo) ?: "Não deu para salvar (${resp.codigo}).")
                linha
            } else Gestao.salvarLinha(cat.tabela, id, linha)
            r.onSuccess { PwToast.mostrar("Salvo.", TipoAviso.OK); onSalvo() }.onFailure { erro = it.message }
            salvando = false
        }
    }

    PwTelaCheia(if (l == null) "Novo · ${cat.titulo}" else nome.ifBlank { cat.titulo }, onFechar, rodape = {
        PwBotaoOuro(if (salvando) "Salvando…" else "Salvar", onClick = ::salvar, icone = PwIcones.Check, habilitado = !salvando)
        if (l != null && cat != Catalogo.TIPOS) PwBotaoPerigo("Apagar", onClick = {
            escopo.launch { Gestao.apagarLinha(cat.tabela, id!!).onSuccess { onSalvo() }.onFailure { erro = it.message } }
        }, modifier = Modifier.fillMaxWidth())
    }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwCampo(nome, { nome = it }, "Nome", modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            BotaoIcone(icone) { icone = it }
        }
        if (cat == Catalogo.TIPOS && l == null) PwCampo(chave, { chave = it.lowercase().replace(' ', '_') }, "Chave", dica = "ex.: farmacia (não muda depois)")
        if (cat == Catalogo.CATEGORIAS || cat == Catalogo.TIPOS) PwCampo(descricao, { descricao = it }, "Descrição")
        PwInterruptor("Ligado", ativo, { ativo = it })

        if (cat == Catalogo.ZONAS || cat == Catalogo.HORARIOS) {
            if (clienteId == null) PwSelecao("Vale para", listOf<Pair<String?, String>>(null to "Todos os clientes") + dados.empresas.map { it.id to it.nome }, cliente, { cliente = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(fator, { fator = it }, "Acréscimo (%)", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                PwCampo(adicional, { adicional = it }, "Adicional (R$)", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
        }
        if (cat == Catalogo.ZONAS) {
            Text("Raio", color = c.texto2, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            com.pwexpress.core.designsystem.PwPasso(num(raioKm) ?: 1.0, { raioKm = com.pwexpress.core.designsystem.numeroBr(it, 2) }, Modifier.fillMaxWidth(),
                passo = 0.1, min = 0.1, max = 100.0, unidade = "km")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CORES.forEach { h ->
                    val col = Color(android.graphics.Color.parseColor(h))
                    Box(Modifier.size(34.dp).clip(CircleShape).background(col).border(if (h == cor) 3.dp else 0.dp, c.texto, CircleShape).clickable { cor = h })
                }
            }
            val la = lat; val lo = lng
            if (la != null && lo != null) Box(Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))) {
                // toque no mapa muda o centro; na tela cheia, o raio e o centro ficam embaixo (tudo sem sair do mapa)
                PwMapa(paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(220.dp),
                    circulo = (la to lo) to ((num(raioKm) ?: 1.0) * 1000), inicio = la to lo, zoomInicio = 12.0, enquadrar = "$la$lo",
                    onToque = { nla, nlo -> lat = nla; lng = nlo },
                    tituloTelaCheia = "Zona",
                    rodapeTelaCheia = {
                        val forma = RoundedCornerShape(16.dp)
                        androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().clip(forma).background(c.vidroForte).border(1.dp, c.borda, forma).padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Toque no mapa para mudar o centro.", color = c.texto2, fontSize = 13.sp)
                            com.pwexpress.core.designsystem.PwPasso(num(raioKm) ?: 1.0, { raioKm = com.pwexpress.core.designsystem.numeroBr(it, 2) }, Modifier.fillMaxWidth(),
                                passo = 0.1, min = 0.1, max = 100.0, unidade = "km")
                        }
                    })
            }
            PwBotaoVidro(if (lat == null) "Marcar o centro no mapa" else "Mudar o centro", onClick = { marcando = true }, icone = PwIcones.MapPinned, modifier = Modifier.fillMaxWidth())
        }
        if (cat == Catalogo.HORARIOS) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(inicio, { inicio = it.take(5) }, "Começa", dica = "17:00", modifier = Modifier.weight(1f))
                PwCampo(fim, { fim = it.take(5) }, "Termina", dica = "19:30", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DIAS_CURTOS.forEachIndexed { i, d ->
                    val sel = i in dias
                    Text(d, color = if (sel) c.sobreOuro else c.texto2, fontWeight = FontWeight.Bold, fontSize = 12.sp,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (sel) c.ouro else c.vidroSutil).clickable { dias = if (sel) dias - i else dias + i }.padding(horizontal = 8.dp, vertical = 8.dp))
                }
            }
        }
        if (cat == Catalogo.CATEGORIAS) {
            PwInterruptor("Perigoso", perigoso, { perigoso = it }, descricao = "Químicos, inflamáveis… (podem ir em rota própria)", cor = c.vermelho)
            if (perigoso) PwCampo(classe, { classe = it }, "Classe de risco", dica = "Ex.: Inflamável")
            PwInterruptor("Frágil", fragil, { fragil = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(porItem, { porItem = it }, "R$ por item", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                PwCampo(fixo, { fixo = it }, "R$ fixo", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
                PwCampo(fator, { fator = it }, "%", teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            }
            PwRotulo("O que o cliente informa")
            CAMPOS.forEach { (k, rot) ->
                Text(rot, color = c.texto, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
                PwSegmentado(VALORES_CAMPO.map { it.second to null }, VALORES_CAMPO.indexOfFirst { it.first == campos[k] }.coerceAtLeast(0),
                    { i -> campos = campos + (k to VALORES_CAMPO[i].first) })
            }
        }
        PwErro(erro)
    }

    if (marcando) com.pwexpress.core.mapa.PwSeletorMapa(lat?.let { la -> lng?.let { lo -> la to lo } }, onFechar = { marcando = false }, titulo = "Centro da zona") { la, lo ->
        lat = la; lng = lo; marcando = false
    }
}

