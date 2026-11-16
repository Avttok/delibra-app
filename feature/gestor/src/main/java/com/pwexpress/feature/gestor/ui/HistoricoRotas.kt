package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.long
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.feature.gestor.data.GRUPOS_CONFIG
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PARAMETROS_VISIVEIS
import com.pwexpress.feature.gestor.data.Param
import com.pwexpress.feature.gestor.data.Permissoes
import com.pwexpress.feature.gestor.data.Status
import com.pwexpress.feature.gestor.data.iso
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

// =================================================================== ajustes na hora
/**
 * Botão de ajustes dentro de qualquer tela: abre numa janela à parte só os ajustes daquela tela, já editáveis.
 * Mesma cascata das Configurações: o Dev decide o que o gestor vê e muda; o que é do Dev fica travado.
 */
@Composable
internal fun BotaoAjustesG(titulo: String, grupos: List<String>, chaves: List<String> = emptyList()) {
    var aberto by remember { mutableStateOf(false) }
    PwBotaoIcone(PwIcones.Settings2, titulo, { aberto = true }, tamanho = 38.dp)
    if (aberto) AjustesRapidosG(titulo, grupos, chaves) { aberto = false }
}

@Composable
internal fun AjustesRapidosG(titulo: String, grupos: List<String>, chaves: List<String>, onFechar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val escopo = rememberCoroutineScope()
    val souDev = com.pwexpress.core.perfil.Perfis.perfil.collectAsState().value?.dev == true
    var estado by remember { mutableStateOf<Pair<String, String>?>(null) }
    val lista = PARAMETROS_VISIVEIS.filter { it.grupo in grupos || it.chave in chaves }
    fun salvar(p: Param, v: JsonElement) {
        estado = p.chave to "salvando"
        escopo.launch { Gestao.salvarConfig(p.chave, v).onSuccess { estado = p.chave to "salvo" }.onFailure { estado = p.chave to (it.message ?: "erro") } }
    }
    PwJanela(titulo, onFechar) {
        TextoDica("Muda na hora e já vale aqui. Exceções por cliente, motoboy ou tipo ficam em Ajustes → Exceções.")
        if (lista.isEmpty()) PwVazio("Nada para ajustar aqui com a sua permissão.", icone = PwIcones.Settings2)
        lista.groupBy { it.grupo }.forEach { (g, ps) ->
            PwRotulo(GRUPOS_CONFIG.find { it.chave == g }?.titulo ?: g)
            ps.forEach { p ->
                PwCartao(padding = PaddingValues(14.dp)) {
                    CampoParametro(p, dados.config[p.chave], { salvar(p, it) }, bloqueado = (p.soDev && !souDev) || Permissoes.estado.value.nivel(p.chave) == "ver")
                    estado?.takeIf { it.first == p.chave }?.second?.let { st ->
                        if (st == "salvo") Text("✓ salvo", color = c.verde, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) else if (st != "salvando") PwErro(st)
                    }
                }
            }
        }
    }
}

// =================================================================== histórico de rotas
@Composable
internal fun HistoricoRotasG(onVoltar: () -> Unit, onAbrir: (String) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var periodo by remember { mutableStateOf(1) }
    var status by remember { mutableStateOf(0) }
    var motoboy by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableStateOf("") }
    var lista by remember { mutableStateOf<List<JsonObject>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var aberta by remember { mutableStateOf<String?>(null) }
    val dias = listOf(0, 7, 30, 90)[periodo]
    val statusFiltro = listOf("concluida,cancelada", "concluida", "cancelada", "")[status]
    LaunchedEffect(periodo, status, motoboy, busca, teste) {
        kotlinx.coroutines.delay(300)
        Gestao.rpcJson("historico_rotas", buildJsonObject {
            put("p", buildJsonObject {
                put("de", LocalDate.now().minusDays(dias.toLong()).toString()); put("ate", LocalDate.now().toString())
                if (statusFiltro.isNotEmpty()) put("status", statusFiltro)
                motoboy?.let { put("motoboy", it) }
                if (busca.isNotBlank()) put("busca", busca.trim())
                put("teste", teste)
            })
        }).onSuccess { lista = (it as? JsonArray)?.mapNotNull { e -> e as? JsonObject }.orEmpty(); erro = null }.onFailure { erro = it.message }
    }
    PwPagina {
        Cabecalho("Histórico de rotas", onVoltar, subtitulo = "Tudo de cada rota num lugar só") {
            BotaoAjustesG("Ajustes do percurso e do pagamento", listOf("percurso", "pagamento", "mb_ve", "avaliacoes"))
        }
        CampoBusca(busca, { busca = it }, "Nº da rota ou do pedido, endereço, quem recebeu…")
        PwFiltros(listOf("Hoje" to 0, "7 dias" to 0, "30 dias" to 0, "90 dias" to 0), periodo, { periodo = it })
        PwFiltros(listOf("Encerradas" to 0, "Concluídas" to 0, "Canceladas" to 0, "Todas" to 0), status, { status = it })
        val motoboys = dados.equipe.filter { it.motoboy && it.modoTeste == teste }
        if (motoboys.isNotEmpty()) androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ChipM("Todos", motoboy == null) { motoboy = null }
            motoboys.forEach { m -> ChipM(m.nome.substringBefore(" "), motoboy == m.id) { motoboy = if (motoboy == m.id) null else m.id } }
        }
        PwErro(erro)
        val l = lista ?: run { PwCarregando(); return@PwPagina }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Resumo("${l.size}", "rotas", Modifier.weight(1f))
            Resumo("${l.sumOf { it.int("feitas") ?: 0 }}", "paradas feitas", Modifier.weight(1f))
            Resumo("%.0f".format(l.sumOf { it.dbl("km") ?: 0.0 }), "km", Modifier.weight(1f))
            Resumo(moedaBr(l.sumOf { it.dbl("valor") ?: 0.0 }), "faturado", Modifier.weight(1.4f))
        }
        if (l.isEmpty()) PwVazio("Nenhuma rota nesse período e filtro.", icone = PwIcones.History)
        l.forEach { x ->
            val st = x.str("status").orEmpty()
            PwCartao(modifier = Modifier.clickable { aberta = x.str("id") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text((if (x.bool2("teste")) "T" else "#") + (x.long("numero") ?: 0), color = c.texto, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    PwSelo(com.pwexpress.core.perfil.Modalidades.nomeStatus("rota", st, Status.padroes[st] ?: st, x.str("tipo_pedido")), corStatus(st, c))
                    if ((x.int("problemas") ?: 0) > 0) { Spacer(Modifier.width(6.dp)); Icon(PwIcones.TriangleAlert, "Teve ocorrência", tint = c.vermelho, modifier = Modifier.size(14.dp)) }
                    Spacer(Modifier.weight(1f))
                    TextoDica(dataHora(iso(x.str("quando"))))
                }
                Text(listOfNotNull(x.str("motoboy"), x.str("cliente")).joinToString(" · ").ifEmpty { "Sem motoboy" }, color = c.texto2, fontSize = 13.sp)
                val pedidos = (x["pedidos"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()
                TextoDica("${x.int("feitas") ?: 0}/${x.int("entregas") ?: 0} entregas · ${x.dbl("km") ?: 0.0} km" +
                    (x.int("minutos")?.let { " · $it min" } ?: "") + (if (pedidos.isNotEmpty()) " · pedido ${pedidos.joinToString(", ") { "#$it" }}" else "") +
                    (x.dbl("valor")?.let { " · ${moedaBr(it)}" } ?: ""))
            }
        }
    }
    aberta?.let { id -> FichaRotaG(id, onFechar = { aberta = null }, onAbrir = { aberta = null; onAbrir(it) }, onTrocar = { aberta = it }) }
}

private fun JsonObject.bool2(k: String) = (this[k] as? JsonPrimitive)?.content == "true"

@Composable
private fun Resumo(valor: String, rotulo: String, modifier: Modifier) {
    val c = Pw.cores
    PwCartao(modifier = modifier, padding = PaddingValues(10.dp)) {
        Text(valor, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
        Text(rotulo, color = c.texto3, fontSize = 11.sp, maxLines = 1)
    }
}

private val EVENTO = mapOf(
    "criada" to "Rota criada", "oferta_expirada" to "Oferta venceu", "aceita" to "Aceita", "recusada" to "Recusada", "iniciada" to "Saiu para a rota",
    "chegou" to "Chegou", "concluida" to "Concluída", "cancelada" to "Cancelada", "parada_concluida" to "Parada feita", "pediu_pular" to "Pediu para liberar",
    "liberada" to "Parada liberada", "rota_atualizada" to "Rota mudou", "passada" to "Passada para outro", "confirmada_offline" to "Confirmou sem internet",
)

/** Tudo da rota numa janela: mapa com o percurso, paradas e comprovantes, pedidos, valores e linha do tempo. */
@Composable
internal fun FichaRotaG(id: String, onFechar: () -> Unit, onAbrir: (String) -> Unit, onTrocar: (String) -> Unit) {
    val c = Pw.cores
    var f by remember(id) { mutableStateOf<JsonObject?>(null) }
    var erro by remember(id) { mutableStateOf<String?>(null) }
    var foco by remember { mutableStateOf<com.pwexpress.core.mapa.FocoMapa?>(null) }
    LaunchedEffect(id) { Gestao.rpcJson("ficha_rota", buildJsonObject { put("p_corrida", id) }).onSuccess { f = it as? JsonObject }.onFailure { erro = it.message } }
    val feito = com.pwexpress.core.mapa.rememberTrilhaCorrida(id, false)
    val r = f?.obj("rota")
    PwJanela(r?.let { "Rota " + (if (it.bool2("teste")) "T" else "#") + (it.long("numero") ?: "") } ?: "Rota", onFechar) {
        PwErro(erro)
        val ficha = f ?: run { if (erro == null) PwCarregando(); return@PwJanela }
        val rota = ficha.obj("rota")!!
        val st = rota.str("status").orEmpty()
        val tipoPedido = rota.str("tipo_pedido")
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwSelo(com.pwexpress.core.perfil.Modalidades.nomeStatus("rota", st, Status.padroes[st] ?: st, tipoPedido), corStatus(st, c))
            Spacer(Modifier.weight(1f))
            PwBotaoVidro("Abrir para mexer", onClick = { onAbrir(id) }, icone = PwIcones.ExternalLink)
        }
        TextoDica(listOfNotNull(rota.str("motoboy")?.let { "Motoboy: $it" }, rota.str("cliente")?.let { "Cliente: $it" },
            ((rota.long("distancia_real_m") ?: rota.long("distancia_planejada_m"))?.let { "%.1f km".format(it / 1000.0) })).joinToString(" · "))
        TextoDica("Criada ${dataHora(iso(rota.str("criada_em")))}" + (iso(rota.str("aceita_em"))?.let { " · aceita ${dataHora(it)}" } ?: "") +
            (iso(rota.str("concluida_em"))?.let { " · concluída ${dataHora(it)}" } ?: "") + (iso(rota.str("cancelada_em"))?.let { " · cancelada ${dataHora(it)}" } ?: ""))
        val paradas = ficha.arr("paradas")
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(20.dp))) {
            PwMapa(paradas = paradas.mapNotNull { p -> p.dbl("lat")?.let { la -> p.dbl("lng")?.let { lo -> PinoMapa(la, lo, p.str("tipo") == "coleta", false, p.str("status") == "concluida") } } },
                rota = rota.str("rota_planejada"), minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(260.dp), focar = foco, enquadrar = id,
                trilhas = com.pwexpress.core.mapa.trilhasDoFeito(feito, null), caminhoAuto = false)
        }
        PwRotulo("Paradas")
        paradas.forEach { p ->
            val coleta = p.str("tipo") == "coleta"
            PwCartao(modifier = Modifier.clickable { val la = p.dbl("lat"); val lo = p.dbl("lng"); if (la != null && lo != null) foco = com.pwexpress.core.mapa.FocoMapa(la, lo) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${(p.int("ordem") ?: 0) + 1}. ${com.pwexpress.core.perfil.Modalidades.nomeParada(coleta, tipoPedido)}", color = c.texto, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    TextoDica(rotuloParada(p.str("status").orEmpty()) + (iso(p.str("concluida_em"))?.let { " às ${hora(it)}" } ?: ""))
                }
                Text(p.str("endereco").orEmpty(), color = c.texto2, fontSize = 13.sp)
                listOfNotNull(
                    p.long("pedido_numero")?.let { "Pedido #$it" + (p.str("cliente")?.let { cl -> " · $cl" } ?: "") },
                    p.arr("itens").takeIf { it.isNotEmpty() }?.joinToString(", ") { "${it.int("quantidade") ?: 1}× ${it.str("descricao")?.ifBlank { null } ?: it.str("categoria") ?: "item"}" },
                    p.str("recebedor_nome")?.let { (if (coleta) "Entregou: " else "Recebeu: ") + it },
                    "fora do raio".takeIf { p.bool2("local_ignorado") }, "feita sem internet".takeIf { p.bool2("feita_offline") },
                    p.obj("respostas")?.takeIf { it.isNotEmpty() }?.entries?.filter { (_, v) -> (v as? JsonPrimitive)?.content?.contains("/") != true }
                        ?.joinToString(" · ") { (k, v) -> "${k.replace('_', ' ')}: ${(v as? JsonPrimitive)?.content ?: v}" },
                ).forEach { TextoDica(it) }
                if (p.str("foto_path") != null || p.str("assinatura_path") != null) TextoDica("Comprovante: " + listOfNotNull("foto".takeIf { p.str("foto_path") != null }, "assinatura".takeIf { p.str("assinatura_path") != null }).joinToString(" e ") + " (abra a rota para ver)")
            }
        }
        val pedidos = ficha.arr("pedidos")
        if (pedidos.isNotEmpty()) {
            PwRotulo("Pedidos desta rota")
            pedidos.forEach { pd ->
                PwCartao {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Pedido #${pd.long("numero") ?: ""}", color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        pd.dbl("valor")?.let { Text(moedaBr(it), color = c.texto, fontWeight = FontWeight.SemiBold) }
                    }
                    TextoDica("${pd.str("cliente") ?: "Avulso"} · feito ${dataHora(iso(pd.str("criado_em")))} · pagamento ${pd.str("pagamento").orEmpty().replace('_', ' ')}")
                    val outras = pd.arr("outras_rotas")
                    if (outras.isNotEmpty()) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextoDica("Também nas rotas:")
                        outras.forEach { o -> ChipM("#${o.long("numero") ?: ""}", false) { o.str("id")?.let(onTrocar) } }
                    }
                }
            }
        }
        val pag = ficha.obj("pagamento")
        PwRotulo("Valores")
        PwCartao {
            LinhaValor("Cliente paga", pag?.dbl("valor"))
            pag?.obj("calculo")?.arr("linhas")?.forEach { l -> LinhaValor("  " + (l.str("rotulo") ?: ""), l.dbl("valor"), fraco = true) }
            pag?.dbl("ajuste")?.takeIf { it != 0.0 }?.let { LinhaValor("Ajuste" + (pag.str("ajuste_motivo")?.let { m -> " ($m)" } ?: ""), it, fraco = true) }
            LinhaValor("Motoboy recebe", pag?.dbl("motoboy"))
            LinhaValor("Fica com a empresa", pag?.dbl("gestao"))
        }
        ficha.arr("avaliacoes").takeIf { it.isNotEmpty() }?.let { av ->
            PwRotulo("Avaliações")
            av.forEach { a -> TextoDica("★".repeat(a.int("nota") ?: 0) + " · " + (if (a.str("autor") == "cliente") "cliente" else "quem recebeu") + (a.str("comentario")?.let { ": $it" } ?: "")) }
        }
        ficha.arr("chamados").takeIf { it.isNotEmpty() }?.let { ch ->
            PwRotulo("Pedidos do motoboy")
            ch.forEach { h -> TextoDica((if (h.str("tipo") == "pular") "Liberar parada" else "Cancelar") + " · ${h.str("status")} · “${h.str("motivo")}”" + (h.str("resposta")?.let { " → $it" } ?: "")) }
        }
        PwRotulo("O que aconteceu")
        PwCartao {
            ficha.arr("eventos").forEach { e ->
                Row { Text(dataHora(iso(e.str("quando"))), color = c.texto3, fontSize = 12.sp, modifier = Modifier.width(86.dp))
                    Text((EVENTO[e.str("tipo")] ?: e.str("tipo").orEmpty().replace('_', ' ')) + (e.str("autor")?.let { " · $it" } ?: "") +
                        (e.obj("dados")?.str("motivo")?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), color = c.texto2, fontSize = 13.sp) }
            }
            ficha.arr("ofertas").takeIf { it.isNotEmpty() }?.let { of -> TextoDica("Ofertas: " + of.joinToString(", ") { "${it.str("motoboy") ?: "?"} (${it.str("status")})" }) }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun LinhaValor(rotulo: String, valor: Double?, fraco: Boolean = false) {
    val c = Pw.cores
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(rotulo, color = if (fraco) c.texto3 else c.texto2, fontSize = if (fraco) 12.sp else 13.5.sp, modifier = Modifier.weight(1f))
        Text(valor?.let { moedaBr(it) } ?: "—", color = if (fraco) c.texto3 else c.texto, fontSize = if (fraco) 12.sp else 13.5.sp, fontWeight = if (fraco) FontWeight.Normal else FontWeight.SemiBold)
    }
}

/** As rotas de um cliente ou de um motoboy dentro do cadastro dele (90 dias); cada uma abre a ficha completa por cima. */
@Composable
internal fun RotasDeG(cliente: String? = null, motoboy: String? = null) {
    val c = Pw.cores
    val teste by TemaApp.modoTeste.collectAsState()
    var lista by remember { mutableStateOf<List<JsonObject>?>(null) }
    var aberta by remember { mutableStateOf<String?>(null) }
    var busca by remember { mutableStateOf("") }
    LaunchedEffect(cliente, motoboy, busca, teste) {
        kotlinx.coroutines.delay(250)
        Gestao.rpcJson("historico_rotas", buildJsonObject {
            put("p", buildJsonObject {
                put("de", LocalDate.now().minusDays(90).toString()); put("ate", LocalDate.now().toString())
                cliente?.let { put("cliente", it) }; motoboy?.let { put("motoboy", it) }
                if (busca.isNotBlank()) put("busca", busca.trim()); put("teste", teste); put("limite", 100)
            })
        }).onSuccess { lista = (it as? JsonArray)?.mapNotNull { e -> e as? JsonObject }.orEmpty() }.onFailure { lista = emptyList() }
    }
    val l = lista ?: run { PwCarregando(); return }
    val feitas = l.filter { it.str("status") == "concluida" }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        Resumo("${feitas.size}", "concluídas", Modifier.weight(1f))
        Resumo("%.0f".format(feitas.sumOf { it.dbl("km") ?: 0.0 }), "km", Modifier.weight(1f))
        Resumo(moedaBr(feitas.sumOf { it.dbl(if (motoboy != null) "valor_motoboy" else "valor") ?: 0.0 }), if (motoboy != null) "para ele" else "faturado", Modifier.weight(1.4f))
    }
    CampoBusca(busca, { busca = it }, "Nº da rota ou do pedido, endereço…")
    if (l.isEmpty()) PwVazio("Nenhuma rota nos últimos 90 dias.", icone = PwIcones.History)
    l.forEach { x ->
        val st = x.str("status").orEmpty()
        PwCartao(modifier = Modifier.clickable { aberta = x.str("id") }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text((if (x.bool2("teste")) "T" else "#") + (x.long("numero") ?: 0), color = c.texto, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                PwSelo(com.pwexpress.core.perfil.Modalidades.nomeStatus("rota", st, Status.padroes[st] ?: st, x.str("tipo_pedido")), corStatus(st, c))
                Spacer(Modifier.weight(1f))
                TextoDica(dataHora(iso(x.str("quando"))))
            }
            Text((if (motoboy != null) x.str("cliente") else x.str("motoboy")) ?: "—", color = c.texto2, fontSize = 13.sp)
            TextoDica("${x.int("feitas") ?: 0}/${x.int("entregas") ?: 0} entregas · ${x.dbl("km") ?: 0.0} km" +
                (x.dbl(if (motoboy != null) "valor_motoboy" else "valor")?.let { " · ${moedaBr(it)}" } ?: ""))
        }
    }
    aberta?.let { id -> FichaRotaG(id, onFechar = { aberta = null }, onAbrir = { aberta = null; GestorNav.abrir(TelaG.Corrida(it)) }, onTrocar = { aberta = it }) }
}
