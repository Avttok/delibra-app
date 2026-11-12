package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwValor
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.CategoriaG
import com.pwexpress.feature.gestor.data.Geo
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.RotaG
import com.pwexpress.feature.gestor.data.TipoPedidoG
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.Calendar

internal fun iconeDoNome(nome: String?): ImageVector = com.pwexpress.core.designsystem.iconePorNome(nome)

private var seqItem = 0
private class ItemForm(val categoria: CategoriaG?, val entrega: String? = null) {
    val chave = "i${++seqItem}"
    var descricao by mutableStateOf("")
    var quantidade by mutableStateOf("1")
    var peso by mutableStateOf("")
    var comp by mutableStateOf("")
    var larg by mutableStateOf("")
    var alt by mutableStateOf("")
    var valor by mutableStateOf("")
}

private fun num(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

/**
 * Novo pedido pelo app (gestor): cliente, quando, tipo, endereços em qualquer ordem, itens por categoria
 * (cada categoria pede só os campos dela) e o orçamento na hora. O roteirizador monta as rotas sozinho.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NovoPedidoG(onVoltar: () -> Unit, onCriado: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    val escopo = rememberCoroutineScope()
    var tipos by remember { mutableStateOf<List<TipoPedidoG>>(emptyList()) }
    var categorias by remember { mutableStateOf<List<CategoriaG>>(emptyList()) }
    LaunchedEffect(Unit) { Gestao.catalogoPedido().let { (t, k) -> tipos = t; categorias = k } }

    var empresaId by remember { mutableStateOf<String?>(null) }
    var avulso by remember { mutableStateOf("") }
    var planejar by remember { mutableStateOf(false) }
    var quando by remember { mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 2); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis) }
    var escolhendoHora by remember { mutableStateOf(false) }
    var tipoPedido by remember { mutableStateOf("padrao") }
    var urgente by remember { mutableStateOf(false) }
    val paradas = remember { mutableStateListOf(ParadaForm(true), ParadaForm(false)) }
    val itens = remember { mutableStateListOf<ItemForm>() }
    var obs by remember { mutableStateOf("") }
    var regras by remember { mutableStateOf<Map<String, kotlinx.serialization.json.JsonElement>>(emptyMap()) }
    var editandoRegras by remember { mutableStateOf(false) }
    var marcando by remember { mutableStateOf<ParadaForm?>(null) }
    var rota by remember { mutableStateOf<RotaG?>(null) }
    var orc by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    fun escolherCliente(id: String?) {
        empresaId = id
        val e = dados.empresa(id) ?: return
        val p0 = paradas.firstOrNull()
        if (p0 != null && p0.coleta && p0.lat == null && e.endereco != null && e.lat != null && e.lng != null) {
            p0.endereco = e.endereco; p0.lat = e.lat; p0.lng = e.lng; p0.instrucoes = e.instrucoesColeta.orEmpty()
        }
    }

    val chaveRota = paradas.joinToString("|") { "${it.lat},${it.lng}" }
    LaunchedEffect(chaveRota) {
        val pts = paradas.mapNotNull { p -> p.lat?.let { la -> p.lng?.let { lo -> la to lo } } }
        if (pts.size < 2 || pts.size != paradas.size) { rota = null; return@LaunchedEffect }
        delay(400)
        rota = Geo.calcularRota(pts)
    }
    // distância pelas ruas da coleta (a anterior na lista) até cada entrega: base do preço "soma das entregas"
    val distEntrega = remember { androidx.compose.runtime.mutableStateMapOf<Int, Double>() }
    LaunchedEffect(chaveRota) {
        distEntrega.clear()
        delay(400)
        var col: Pair<Double, Double>? = paradas.firstOrNull { it.coleta && it.lat != null }?.let { it.lat!! to it.lng!! }
        paradas.forEachIndexed { i, p ->
            val pt = p.lat?.let { la -> p.lng?.let { lo -> la to lo } } ?: return@forEachIndexed
            if (p.coleta) { col = pt; return@forEachIndexed }
            val de = col ?: return@forEachIndexed
            com.pwexpress.core.mapa.Navegacao.rota(de, pt)?.distanciaM?.takeIf { it > 0 }?.let { distEntrega[i] = it }
        }
    }

    fun pontosJson() = buildJsonArray {
        paradas.forEachIndexed { i, p ->
            add(buildJsonObject {
                put("tipo", if (p.coleta) "coleta" else "entrega"); put("endereco", p.endereco)
                p.lat?.let { put("lat", it) }; p.lng?.let { put("lng", it) }
                if (!p.coleta) distEntrega[i]?.let { put("dist_coleta_m", kotlin.math.round(it).toInt()) }
                put("contato_nome", p.contato); put("contato_telefone", p.telefone); put("instrucoes", p.instrucoes)
                if (p.exigeFoto) put("exige_foto", true); if (p.exigeNome) put("exige_nome", true)
                if (p.exigeAssinatura) put("exige_assinatura", true); if (p.exigeCodigo) put("exige_codigo", true)
                horaValida(p.janelaDe)?.let { put("janela_de", it) }; horaValida(p.janelaAte)?.let { put("janela_ate", it) }
                put("referencias", buildJsonArray { p.refs.forEach { r -> add(buildJsonObject { put("nome", r.nome); r.distanciaM?.let { put("distancia_m", it) }; r.tipo?.let { put("tipo", it) }; r.relacao?.let { put("relacao", it) } }) } })
            })
        }
    }
    fun itensJson() = buildJsonArray {
        itens.forEach { x ->
            add(buildJsonObject {
                put("descricao", x.descricao.trim()); x.categoria?.let { put("categoria_id", it.id) } ?: put("categoria_id", JsonNull)
                put("quantidade", (num(x.quantidade) ?: 1.0).toInt().coerceAtLeast(1))
                num(x.peso)?.let { put("peso_kg", it) }; num(x.comp)?.let { put("comprimento_cm", it) }
                num(x.larg)?.let { put("largura_cm", it) }; num(x.alt)?.let { put("altura_cm", it) }; num(x.valor)?.let { put("valor_declarado", it) }
                // pacote de uma entrega só: vai com o índice do ponto (igual ao site do cliente)
                paradas.indexOfFirst { it.chave == x.entrega }.takeIf { it >= 0 }?.let { put("entrega", it) }
            })
        }
    }
    val quandoIso = if (planejar) Instant.ofEpochMilli(quando).toString() else null

    // orçamento ao vivo
    val chaveOrc = listOf(regras.toString(), empresaId, tipoPedido, urgente, quandoIso, rota?.distanciaM, chaveRota, distEntrega.toMap(), itens.joinToString { "${it.categoria?.id}${it.quantidade}${it.peso}${it.comp}${it.larg}${it.alt}${it.entrega}" }).joinToString("§")
    LaunchedEffect(chaveOrc) {
        if (paradas.any { it.lat == null }) { orc = null; return@LaunchedEffect }
        delay(450)
        orc = Gestao.orcarPrevia(buildJsonObject {
            empresaId?.let { put("empresa_id", it) }; put("tipo", if (planejar) "planejado" else "express"); put("tipo_pedido", tipoPedido)
            quandoIso?.let { put("quando", it) }; put("urgente", urgente); rota?.let { put("distancia_m", it.distanciaM) }
            put("pontos", pontosJson()); put("itens", itensJson())
            if (regras.isNotEmpty()) put("regras", JsonObject(regras))
        })
    }

    fun enviar() {
        erro = when {
            paradas.any { it.lat == null } -> "Localize todos os endereços (busque ou marque no mapa)."
            paradas.none { it.coleta } || paradas.none { !it.coleta } -> "O pedido precisa de pelo menos uma coleta e uma entrega."
            else -> null
        }
        if (erro != null) return
        enviando = true
        escopo.launch {
            Gestao.criarPedido(buildJsonObject {
                empresaId?.let { put("empresa_id", it) } ?: put("cliente_nome", avulso.trim())
                put("teste", teste); quandoIso?.let { put("quando", it) }; put("urgente", urgente); put("tipo_pedido", tipoPedido)
                obs.trim().takeIf { it.isNotEmpty() }?.let { put("observacoes", it) }
                rota?.let { put("distancia_m", it.distanciaM) }
                put("pontos", pontosJson()); put("itens", itensJson())
                if (regras.isNotEmpty()) put("regras", JsonObject(regras))
            }).onSuccess {
                PwToast.mostrar("O roteirizador já está montando a rota.", TipoAviso.OK, "Pedido criado")
                Gestao.atualizar(); onCriado()
            }.onFailure { erro = it.message }
            enviando = false
        }
    }

    PwPagina {
        Cabecalho("Novo pedido", onVoltar, subtitulo = if (teste) "Pedido de teste" else "Vira rota sozinho")

        PwCartao {
            PwRotulo("Cliente")
            PwSelecao("", listOf<Pair<String?, String>>(null to "Avulso (sem cadastro)") + dados.empresas.map { it.id to it.nome }, empresaId, { escolherCliente(it) })
            if (empresaId == null) PwCampo(avulso, { avulso = it }, "Nome do cliente avulso")
        }

        PwCartao {
            PwRotulo("Quando")
            PwSegmentado(listOf("Agora" to PwIcones.Zap, "Planejado" to PwIcones.CalendarClock), if (planejar) 1 else 0, { planejar = it == 1 })
            if (planejar) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.vidroSutil).border(1.dp, c.borda, RoundedCornerShape(14.dp)).clickable { escolhendoHora = true }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.CalendarClock, null, tint = c.azul, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(dataHora(quando), color = c.texto, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("Mudar", color = c.ouro, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
            if (tipos.size > 1) {
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tipos.forEach { t -> Ficha(t.nome, iconeDoNome(t.icone), tipoPedido == t.chave) { tipoPedido = t.chave } }
                }
            }
            Spacer(Modifier.height(4.dp))
            PwInterruptor("Urgente", urgente, { urgente = it }, icone = PwIcones.Zap, cor = c.vermelho)
        }

        PwRotulo("Endereços")
        paradas.forEachIndexed { i, p ->
            CartaoParadaForm(p, i, paradas.size,
                onMover = { d -> val j = i + d; if (j in paradas.indices) { val x = paradas.removeAt(i); paradas.add(j, x) } },
                onRemover = { itens.removeAll { it.entrega == p.chave }; paradas.remove(p) }, onMarcar = { marcando = p }, comOpcoes = true)
            // o que vai para esta entrega, direto no ponto (como no site do cliente)
            if (!p.coleta) Column(Modifier.fillMaxWidth().padding(start = 14.dp, end = 4.dp, bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("O que vai para esta entrega", color = c.texto2, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categorias.forEach { k -> Ficha(k.nome, if (k.perigoso) PwIcones.FlaskConical else iconeDoNome(k.icone), false, if (k.perigoso) c.vermelho else null) { itens.add(ItemForm(k, p.chave)) } }
                    Ficha("Outro", PwIcones.Package, false) { itens.add(ItemForm(null, p.chave)) }
                }
                itens.filter { it.entrega == p.chave }.forEach { x -> CartaoItem(x, onRemover = { itens.remove(x) }) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            com.pwexpress.core.designsystem.PwBotaoVidro("Entrega", onClick = { paradas.add(ParadaForm(false)) }, icone = PwIcones.Plus, modifier = Modifier.weight(1f))
            com.pwexpress.core.designsystem.PwBotaoVidro("Coleta", onClick = { paradas.add(ParadaForm(true)) }, icone = PwIcones.Plus, modifier = Modifier.weight(1f))
        }
        if (paradas.any { it.lat != null }) Box(Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(20.dp))) {
            PwMapa(paradas = paradas.filter { it.lat != null }.map { PinoMapa(it.lat!!, it.lng!!, it.coleta, false, false) }, rota = rota?.polyline, minhaPosicao = null,
                modifier = Modifier.fillMaxWidth().height(200.dp), enquadrar = chaveRota)
        }

        PwCartao {
            var paraTodos by remember { mutableStateOf(false) }
            if (!paraTodos && itens.none { it.entrega == null }) {
                com.pwexpress.core.designsystem.PwBotaoVidro("Especificar pacote para todas as entregas", onClick = { paraTodos = true }, icone = PwIcones.Boxes, modifier = Modifier.fillMaxWidth())
            } else {
                PwRotulo("Para todas as entregas")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categorias.forEach { k -> Ficha(k.nome, if (k.perigoso) PwIcones.FlaskConical else iconeDoNome(k.icone), false, if (k.perigoso) c.vermelho else null) { itens.add(ItemForm(k)) } }
                    Ficha("Outro", PwIcones.Package, false) { itens.add(ItemForm(null)) }
                }
                itens.filter { it.entrega == null }.forEach { x -> CartaoItem(x, onRemover = { itens.remove(x) }) }
            }
            PwCampo(obs, { obs = it }, "Observações para o motoboy", linhas = 2)
        }

        PwCartao(destaque = true) {
            PwRotulo("Orçamento")
            val total = (orc?.get("total") as? JsonPrimitive)?.content?.toDoubleOrNull()
            if (total != null) {
                PwValor(moedaBr(total), tamanhoMax = 40)
                (orc?.get("linhas") as? JsonArray)?.forEach { l ->
                    val o = l as? JsonObject ?: return@forEach
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text((o["rotulo"] as? JsonPrimitive)?.content.orEmpty(), color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(moedaBr((o["valor"] as? JsonPrimitive)?.content?.toDoubleOrNull()), color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                (orc?.get("percentuais") as? JsonArray)?.forEach { l ->
                    val o = l as? JsonObject ?: return@forEach
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text((o["rotulo"] as? JsonPrimitive)?.content.orEmpty(), color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text("+${(o["pct"] as? JsonPrimitive)?.content}%", color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                rota?.let { TextoDica("${km(it.distanciaM)} · ${duracao(it.duracaoS)}") }
            } else TextoDica("Localize os endereços para calcular.")
        }
        com.pwexpress.core.designsystem.PwBotaoVidro("Regras deste pedido" + if (regras.isNotEmpty()) " (${regras.size})" else "", onClick = { editandoRegras = true },
            icone = PwIcones.Layers, modifier = Modifier.fillMaxWidth())
        PwErro(erro)
        PwBotaoOuro(if (enviando) "Criando…" else "Criar pedido", onClick = ::enviar, icone = PwIcones.Package2, habilitado = !enviando, altura = 58.dp)
    }

    marcando?.let { p ->
        com.pwexpress.core.mapa.PwSeletorMapa(p.lat?.let { la -> p.lng?.let { lo -> la to lo } } ?: paradas.firstNotNullOfOrNull { q -> q.lat?.let { la -> q.lng?.let { lo -> la to lo } } },
            onFechar = { marcando = null }, titulo = if (p.coleta) "Local da coleta" else "Local da entrega") { la, lo ->
            marcando = null
            p.lat = la; p.lng = lo
            escopo.launch { p.endereco = Geo.enderecoDoPonto(la, lo) }
        }
    }
    if (editandoRegras) RegrasPedidoG(null, regras, onFechar = { editandoRegras = false }, onMudar = { regras = it })
    if (escolhendoHora) SeletorDataHora("Quando", quando, onFechar = { escolhendoHora = false }) { quando = it; escolhendoHora = false }
}

@Composable
private fun Ficha(texto: String, icone: ImageVector, ativa: Boolean, cor: androidx.compose.ui.graphics.Color? = null, onClick: () -> Unit) {
    val c = Pw.cores
    val forma = RoundedCornerShape(14.dp)
    Row(
        Modifier.clip(forma).background(if (ativa) c.ouro.copy(alpha = 0.16f) else c.vidroSutil).border(1.dp, if (ativa) c.ouro else c.borda, forma)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = cor ?: if (ativa) c.ouro else c.texto2, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Text(texto, color = c.texto, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, maxLines = 1)
    }
}

@Composable
private fun CartaoItem(x: ItemForm, onRemover: () -> Unit) {
    val c = Pw.cores
    val k = x.categoria
    val forma = RoundedCornerShape(16.dp)
    fun rot(t: String, campo: String) = if (campo == "obrigatorio") "$t *" else t
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).clip(forma).background(c.vidroSutil).border(1.dp, if (k?.perigoso == true) c.vermelho.copy(alpha = 0.5f) else c.borda, forma).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (k?.perigoso == true) PwIcones.FlaskConical else iconeDoNome(k?.icone), null, tint = if (k?.perigoso == true) c.vermelho else c.ouro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(k?.nome ?: "Outro", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, modifier = Modifier.weight(1f))
            if (k?.fragil == true) { PwSelo("Frágil", c.ouro); Spacer(Modifier.width(6.dp)) }
            if (k?.campoQuantidade != "oculto") {
                PwBotaoIcone(PwIcones.Minus, "Menos", { x.quantidade = ((num(x.quantidade) ?: 1.0).toInt() - 1).coerceAtLeast(1).toString() }, tamanho = 32.dp)
                Text(x.quantidade, color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                PwBotaoIcone(PwIcones.Plus, "Mais", { x.quantidade = ((num(x.quantidade) ?: 0.0).toInt() + 1).toString() }, tamanho = 32.dp)
            }
            Spacer(Modifier.width(4.dp))
            PwBotaoIcone(PwIcones.Trash2, "Remover", onRemover, tamanho = 32.dp, cor = c.vermelho)
        }
        val cd = k?.campoDescricao ?: "opcional"
        if (cd != "oculto") PwCampo(x.descricao, { x.descricao = it }, rot(if (cd == "obrigatorio") "Descreva o item" else "Detalhe (opcional)", cd))
        val cp = k?.campoPeso ?: "opcional"; val cm = k?.campoMedidas ?: "opcional"; val cv = k?.campoValor ?: "opcional"
        if (cp != "oculto" || cv != "oculto") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (cp != "oculto") PwCampo(x.peso, { x.peso = it }, rot("Peso (kg)", cp), teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            if (cv != "oculto") PwCampo(x.valor, { x.valor = it }, rot("Valor (R$)", cv), teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
        if (cm != "oculto") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(x.comp, { x.comp = it }, rot("C (cm)", cm), teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            PwCampo(x.larg, { x.larg = it }, rot("L (cm)", cm), teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
            PwCampo(x.alt, { x.alt = it }, rot("A (cm)", cm), teclado = KeyboardType.Decimal, modifier = Modifier.weight(1f))
        }
    }
}
