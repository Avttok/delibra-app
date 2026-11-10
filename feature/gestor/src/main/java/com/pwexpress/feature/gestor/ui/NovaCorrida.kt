package com.pwexpress.feature.gestor.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwPasso
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.EnderecoG
import com.pwexpress.feature.gestor.data.Geo
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Precos
import com.pwexpress.feature.gestor.data.RefG
import com.pwexpress.feature.gestor.data.RotaG
import com.pwexpress.feature.gestor.data.logico
import com.pwexpress.feature.gestor.data.numero
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Calendar

private var seq = 0
internal class ParadaForm(coleta: Boolean) {
    val chave = "p${++seq}"
    var coleta by mutableStateOf(coleta)
    var endereco by mutableStateOf("")
    var lat by mutableStateOf<Double?>(null)
    var lng by mutableStateOf<Double?>(null)
    var contato by mutableStateOf("")
    var telefone by mutableStateOf("")
    var instrucoes by mutableStateOf("")
    val refs = mutableStateListOf<RefG>()
    var aberta by mutableStateOf(false)
    // confirmações deste ponto (false = segue o padrão da rota) e horário em que atende
    var exigeFoto by mutableStateOf(false)
    var exigeNome by mutableStateOf(false)
    var exigeAssinatura by mutableStateOf(false)
    var exigeCodigo by mutableStateOf(false)
    var janelaDe by mutableStateOf("")
    var janelaAte by mutableStateOf("")
}

/** "9", "930", "09:30" → "09:30"; inválido → null. */
internal fun horaValida(t: String): String? {
    val d = t.filter(Char::isDigit)
    if (d.isEmpty()) return null
    val (h, m) = when (d.length) { 1, 2 -> d.toInt() to 0; 3 -> d.take(1).toInt() to d.drop(1).toInt(); else -> d.take(2).toInt() to d.drop(2).take(2).toInt() }
    return if (h in 0..23 && m in 0..59) String.format(java.util.Locale.US, "%02d:%02d", h, m) else null
}

private val DIAS = listOf("D", "S", "T", "Q", "Q", "S", "S")

/** Datas da série: só os dias marcados no modo semanal (0 = domingo). */
private fun gerarDatas(inicio: Long, repetir: Int, dias: List<Int>, vezes: Int): List<Long> {
    if (repetir == 0 || vezes <= 1) return listOf(inicio)
    val cal = Calendar.getInstance().apply { timeInMillis = inicio }
    val usar = if (repetir == 1) (0..6).toList() else dias.ifEmpty { listOf(cal.get(Calendar.DAY_OF_WEEK) - 1) }
    val out = mutableListOf<Long>()
    var i = 0
    while (out.size < vezes && i < 400) {
        if ((cal.get(Calendar.DAY_OF_WEEK) - 1) in usar) out += cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1); i++
    }
    return out
}

@Composable
fun NovaCorridaG(onVoltar: () -> Unit, onCriada: (String) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    val cfg = dados.config
    val escopo = rememberCoroutineScope()

    var planejar by remember { mutableStateOf(false) }
    val antecedencia = cfg.numero("agendada_antecedencia_min", 30.0).toLong()
    var inicio by remember { mutableLongStateOf(Calendar.getInstance().apply { add(Calendar.MINUTE, (antecedencia + 60).toInt()); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis) }
    var escolhendoHora by remember { mutableStateOf(false) }
    var repetir by remember { mutableStateOf(0) }
    val dias = remember { mutableStateListOf<Int>() }
    var vezes by remember { mutableStateOf(4.0) }

    var cadastrado by remember { mutableStateOf(false) }
    var empresaId by remember { mutableStateOf<String?>(null) }
    var nomeAvulso by remember { mutableStateOf("") }
    var conhecidos by remember { mutableStateOf<List<String>>(emptyList()) }
    var modalidade by remember { mutableStateOf("por_km") }
    var kmAvulso by remember { mutableStateOf(cfg.numero("valor_km_padrao", 2.0).toString().replace('.', ',')) }
    var fixoAvulso by remember { mutableStateOf("") }

    val paradas = remember { mutableStateListOf(ParadaForm(true), ParadaForm(false)) }
    var marcando by remember { mutableStateOf<ParadaForm?>(null) }
    var rota by remember { mutableStateOf<RotaG?>(null) }
    var calculando by remember { mutableStateOf(false) }
    var valorManual by remember { mutableStateOf<String?>(null) }
    var cobrarAcrescimo by remember { mutableStateOf(true) }
    var noLocal by remember { mutableStateOf(cfg.logico("exigir_no_local", true)) }
    var raio by remember { mutableStateOf(cfg.numero("raio_chegada_m", 150.0)) }
    var exNome by remember { mutableStateOf(cfg.logico("padrao_exige_nome", true)) }
    var exFoto by remember { mutableStateOf(cfg.logico("padrao_exige_foto", false)) }
    var exAss by remember { mutableStateOf(cfg.logico("padrao_exige_assinatura", false)) }
    var exCod by remember { mutableStateOf(cfg.logico("padrao_exige_codigo", false)) }
    var direto by remember { mutableStateOf(false) }
    var urgente by remember { mutableStateOf(false) }
    var divisaoPropria by remember { mutableStateOf(false) }
    var repasseCorrida by remember { mutableStateOf("") }
    var devCorrida by remember { mutableStateOf("") }
    var motoboyId by remember { mutableStateOf<String?>(null) }
    var obs by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { conhecidos = Gestao.clientesAvulsos() }

    val empresa = if (cadastrado) dados.empresa(empresaId) else null
    // rota: recalcula quando as coordenadas mudam
    val chave = paradas.joinToString("|") { "${it.lat},${it.lng}" }
    LaunchedEffect(chave) {
        val pts = paradas.mapNotNull { p -> p.lat?.let { la -> p.lng?.let { lo -> la to lo } } }
        if (pts.size < 2) { rota = null; return@LaunchedEffect }
        calculando = true
        delay(400)
        rota = Geo.calcularRota(pts)
        calculando = false
    }

    fun num(s: String) = s.trim().replace(',', '.').toDoubleOrNull()
    val orc = if (empresa != null) Precos.orcar(empresa.modalidade, empresa.valorKm, empresa.valorFixo, rota?.distanciaM, cfg)
              else Precos.orcar(modalidade, num(kmAvulso), num(fixoAvulso), rota?.distanciaM, cfg)
    val valorFinal = valorManual?.let { num(it) } ?: orc.valor
    val acrescimoLigado = cfg.logico("acrescimo_distancia", true) && cobrarAcrescimo
    val fe = if (acrescimoLigado) Precos.fatorEntrega(rota?.distanciaM, cfg) else 0.0
    val valorComAcrescimo = valorFinal?.let { Precos.arredondar(it * (1 + fe), cfg) }
    val datas = if (planejar) gerarDatas(inicio, repetir, dias.toList(), vezes.toInt()) else emptyList()
    val compativeis = dados.equipe.filter { it.motoboy && it.modoTeste == teste }
    val favoritos = dados.favoritos[empresaId].orEmpty()

    fun escolherEmpresa(id: String) {
        empresaId = id; valorManual = null
        val e = dados.empresa(id) ?: return
        val p0 = paradas.firstOrNull()
        if (p0 != null && p0.coleta && e.endereco != null && e.lat != null && e.lng != null) {
            p0.endereco = e.endereco; p0.lat = e.lat; p0.lng = e.lng
            p0.contato = e.nomeFantasia ?: e.nome; p0.telefone = e.telefone.orEmpty(); p0.instrucoes = e.instrucoesColeta.orEmpty()
        }
    }

    fun enviar() {
        erro = null
        when {
            paradas.any { it.lat == null } -> { erro = "Defina a localização de todas as paradas (busque o endereço ou marque no mapa)."; return }
            !paradas.first().coleta -> { erro = "A primeira parada precisa ser uma coleta."; return }
            paradas.none { !it.coleta } -> { erro = "Inclua pelo menos uma entrega."; return }
            valorFinal == null -> { erro = "Informe o valor da corrida."; return }
            direto && motoboyId == null -> { erro = "Escolha o motoboy."; return }
            cadastrado && empresa == null -> { erro = "Escolha o cliente (ou mude para avulso)."; return }
            planejar && (datas.firstOrNull() ?: 0) < System.currentTimeMillis() + antecedencia * 60_000 -> { erro = "Planeje com pelo menos $antecedencia min de antecedência."; return }
        }
        val corpo = buildJsonObject {
            put("empresa_id", empresa?.id?.let { JsonPrimitive(it) } ?: JsonNull)
            put("cliente_nome", if (empresa == null && nomeAvulso.isNotBlank()) JsonPrimitive(nomeAvulso.trim()) else JsonNull)
            put("aplicar_acrescimo", acrescimoLigado)
            put("teste", teste)
            put("modo_despacho", if (direto) "direto" else "cascata")
            put("urgente", !direto && !planejar && urgente)
            if (divisaoPropria) {
                repasseCorrida.replace(',', '.').toDoubleOrNull()?.let { put("repasse_pct", it) }
                devCorrida.replace(',', '.').toDoubleOrNull()?.let { put("dev_pct", it) }
            }
            put("motoboy_id", if (direto) JsonPrimitive(motoboyId) else JsonNull)
            put("exige_foto", exFoto); put("exige_assinatura", exAss); put("exige_nome", exNome); put("exige_codigo", exCod)
            put("exigir_no_local", noLocal); put("raio_chegada_m", raio.toInt())
            rota?.let { put("distancia_planejada_m", it.distanciaM); put("duracao_planejada_s", it.duracaoS); put("rota_planejada", it.polyline) }
            put("valor", valorFinal)
            put("observacoes", obs)
            put("paradas", JsonArray(paradas.map { p ->
                buildJsonObject {
                    put("tipo", if (p.coleta) "coleta" else "entrega"); put("endereco", p.endereco); put("lat", p.lat); put("lng", p.lng)
                    put("contato_nome", p.contato); put("contato_telefone", p.telefone); put("instrucoes", p.instrucoes)
                    put("referencias", JsonArray(p.refs.map { r -> buildJsonObject { put("nome", r.nome); r.distanciaM?.let { put("distancia_m", it) }; r.tipo?.let { put("tipo", it) }; r.lat?.let { put("lat", it) }; r.lng?.let { put("lng", it) }; r.relacao?.let { put("relacao", it) } } }))
                }
            }))
        }
        enviando = true
        escopo.launch {
            Gestao.criarCorrida(corpo, datas).onSuccess { id -> Gestao.atualizar(); onCriada(id) }.onFailure { erro = it.message }
            enviando = false
        }
    }

    PwPagina {
        Cabecalho("Nova corrida", onVoltar, subtitulo = if (teste) "Será uma corrida de teste" else "Paradas, preço e despacho")

        // ------------------------------------------------ quando
        if (modulo("corridas_planejadas")) PwCartao(cor = if (planejar) c.azul.copy(alpha = 0.6f) else null) {
            PwRotulo("Quando")
            Espaco(8)
            PwSegmentado(listOf("Express" to PwIcones.Zap, "Planejar" to PwIcones.CalendarClock), if (planejar) 1 else 0, { planejar = it == 1 })
            if (planejar) {
                Espaco(10)
                PwBotaoVidro("Começa ${quando(inicio)}", onClick = { escolhendoHora = true }, icone = PwIcones.CalendarClock, modifier = Modifier.fillMaxWidth())
                Espaco(10)
                PwSelecao("Repetir", listOf(0 to "Não repetir", 1 to "Todo dia", 2 to "Em dias da semana"), repetir, { repetir = it })
                if (repetir == 2) {
                    Espaco(8)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DIAS.forEachIndexed { i, d ->
                            val ativo = i in dias
                            Box(
                                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp))
                                    .background(if (ativo) c.gradOuro else androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.campo, c.campo)))
                                    .border(1.dp, if (ativo) androidx.compose.ui.graphics.Color.Transparent else c.bordaForte, RoundedCornerShape(12.dp))
                                    .clickable { if (ativo) dias.remove(i) else dias.add(i) },
                                contentAlignment = Alignment.Center,
                            ) { Text(d, color = if (ativo) c.sobreOuro else c.texto2, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
                if (repetir != 0) {
                    Espaco(8)
                    PwRotulo("Quantas corridas no total")
                    Espaco(4)
                    PwPasso(vezes, { vezes = it }, min = 2.0, max = 60.0)
                }
                Espaco(8)
                datas.take(6).forEach { TextoDica("• ${quando(it)}") }
                if (datas.size > 6) TextoDica("+ ${datas.size - 6} datas")
                Espaco(4)
                TextoDica("Aparece no app para todos os motoboys${if (teste) " em modo teste" else ""}, mesmo offline. Quem reservar pode desistir até ${cfg.numero("agendada_cancelar_ate_min", 60.0).toInt()} min antes.")
            }
        }

        // ------------------------------------------------ cliente
        PwCartao {
            PwRotulo("Cliente")
            Espaco(8)
            PwSegmentado(listOf("Avulso" to PwIcones.UserRound, "Cadastrado" to PwIcones.Building2), if (cadastrado) 1 else 0, { cadastrado = it == 1; valorManual = null })
            Espaco(10)
            if (cadastrado) {
                PwSelecao("Cliente", dados.empresas.map { it.id to (it.nome + if (it.situacao != null && it.situacao != "ATIVA") " (${it.situacao})" else "") }, empresaId, { escolherEmpresa(it) }, vazio = "Escolha o cliente…")
                empresa?.let { e ->
                    Espaco(6)
                    TextoDica("Preço do cliente: ${Precos.rotulo(e.modalidade)}" + when (e.modalidade) { "por_km" -> " · ${moedaBr(e.valorKm)}/km"; "fixo" -> " · ${moedaBr(e.valorFixo)}"; else -> "" })
                }
            } else {
                PwCampo(nomeAvulso, { nomeAvulso = it }, "Nome do cliente (opcional)", dica = "Ex.: Dona Maria · Padaria do Centro")
                val sugestoes = conhecidos.filter { nomeAvulso.length >= 1 && it.contains(nomeAvulso, true) && !it.equals(nomeAvulso, true) }.take(4)
                if (sugestoes.isNotEmpty()) Row(Modifier.horizontalScroll(rememberScrollState()).padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { sugestoes.forEach { s -> Chip(s) { nomeAvulso = s } } }
                Espaco(8)
                PwSelecao("Modalidade desta corrida", Precos.modalidades, modalidade, { modalidade = it; valorManual = null })
                Espaco(8)
                when (modalidade) {
                    "por_km" -> PwCampo(kmAvulso, { kmAvulso = it; valorManual = null }, "Valor do km", teclado = KeyboardType.Decimal, prefixo = "R$ ")
                    "fixo" -> PwCampo(fixoAvulso, { fixoAvulso = it; valorManual = null }, "Preço fixo", teclado = KeyboardType.Decimal, prefixo = "R$ ")
                    else -> TextoDica("Informe o valor da corrida no resumo abaixo.")
                }
            }
        }

        // ------------------------------------------------ paradas
        PwCartao {
            PwRotulo("Paradas")
            paradas.forEachIndexed { i, p ->
                Espaco(10)
                CartaoParadaForm(
                    p, i, paradas.size,
                    onMover = { d -> val j = i + d; if (j in paradas.indices) { val x = paradas[i]; paradas[i] = paradas[j]; paradas[j] = x } },
                    onRemover = { paradas.remove(p) },
                    onMarcar = { marcando = p },
                )
            }
            Espaco(10)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwBotaoVidro("Coleta", onClick = { paradas.add(ParadaForm(true)) }, icone = PwIcones.Plus, modifier = Modifier.weight(1f))
                PwBotaoVidro("Entrega", onClick = { paradas.add(ParadaForm(false)) }, icone = PwIcones.Plus, modifier = Modifier.weight(1f))
            }
        }

        val pinos = paradas.mapNotNull { p -> p.lat?.let { la -> p.lng?.let { lo -> PinoMapa(la, lo, p.coleta, false, false) } } }
        if (pinos.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(22.dp)).border(1.dp, c.borda, RoundedCornerShape(22.dp))) {
                PwMapa(paradas = pinos, rota = rota?.polyline, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(210.dp), enquadrar = chave)
            }
        }

        // ------------------------------------------------ resumo e preço
        PwCartao {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.pwexpress.core.designsystem.PwNumero("Distância", if (calculando) "…" else km(rota?.distanciaM), Modifier.weight(1f), icone = PwIcones.Ruler)
                com.pwexpress.core.designsystem.PwNumero("Tempo", if (calculando) "…" else duracao(rota?.duracaoS), Modifier.weight(1f), icone = PwIcones.Clock)
            }
            Espaco(10)
            PwCampo(valorManual ?: (orc.valor?.let { "%.2f".format(it).replace('.', ',') } ?: ""), { valorManual = it }, "Valor", teclado = KeyboardType.Decimal, prefixo = "R$ ")
            Espaco(4)
            TextoDica("${Precos.rotulo(empresa?.modalidade ?: modalidade)} · ${orc.explicacao}")
            if (valorManual != null) Text("Voltar ao calculado (${moedaBr(orc.valor)})", color = c.ouro, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable { valorManual = null }.padding(vertical = 6.dp))
            if (cfg.logico("acrescimo_distancia", true)) {
                Espaco(4)
                PwInterruptor("Acréscimo por distância", cobrarAcrescimo, { cobrarAcrescimo = it }, icone = PwIcones.TrendingUp,
                    descricao = if (cobrarAcrescimo) "Cobra a mais em percursos longos e para motoboys longe da coleta." else "Esta corrida fica com o valor acima, sem acréscimo.")
                if (acrescimoLigado) TextoDica(
                    (if (fe > 0) "Percurso longo: +${numeroBr(fe)}× → ${moedaBr(valorComAcrescimo)}. " else "Sem acréscimo de percurso. ") +
                        "Se o motoboy estiver a mais de ${cfg.numero("acrescimo_coleta_cada_km", 100.0).toInt()} km da coleta, soma +${numeroBr(cfg.numero("acrescimo_coleta_fator", 0.5))}× a cada ${cfg.numero("acrescimo_coleta_cada_km", 100.0).toInt()} km no valor que ele recebe.",
                )
            }
        }

        // ------------------------------------------------ local, comprovantes e despacho
        PwCartao {
            PwRotulo("Localização")
            PwInterruptor("Exigir estar no local", noLocal, { noLocal = it }, descricao = "O motoboy só avança as etapas perto do endereço.", icone = PwIcones.MapPinned)
            if (noLocal) PwPasso(raio, { raio = it }, passo = 10.0, min = 20.0, max = 2000.0, unidade = "m")
            Espaco(8)
            PwRotulo("Comprovantes na entrega")
            PwInterruptor("Nome de quem recebeu", exNome, { exNome = it })
            PwInterruptor("Foto", exFoto, { exFoto = it })
            PwInterruptor("Assinatura", exAss, { exAss = it })
            PwInterruptor("Código de 4 dígitos", exCod, { exCod = it })
        }
        PwCartao {
            PwRotulo("Despacho")
            Espaco(8)
            PwSegmentado(
                listOf((if (planejar) "Aberta" else "Mais próximo") to PwIcones.Radar, (if (planejar) "Reservar" else "Escolher") to PwIcones.UserRound),
                if (direto) 1 else 0, { direto = it == 1 },
            )
            Espaco(6)
            TextoDica(when {
                planejar && !direto -> "O primeiro motoboy que reservar no app fica com ela."
                planejar -> "Já fica reservada para o motoboy escolhido."
                !direto -> "Vai para o mais próximo da coleta" + if (favoritos.isNotEmpty() && cfg.logico("favoritos_primeiro", true)) ", favoritos do cliente primeiro." else "."
                else -> "Só esse motoboy recebe a corrida."
            })
            if (!direto && !planejar) {
                Espaco(4)
                PwInterruptor("Urgente", urgente, { urgente = it }, icone = PwIcones.Zap, cor = c.vermelho,
                    descricao = if (urgente) "Vai para o mais próximo mesmo fora do raio que ele escolheu." else "Respeita o raio de disponibilidade de cada motoboy.")
            }
            if (direto) {
                Espaco(8)
                PwSelecao("Motoboy", compativeis.sortedByDescending { it.id in favoritos }.map { m -> m.id to ((if (m.id in favoritos) "★ " else "") + m.nome + " · " + (rotuloMotoboy[dados.posicao(m.id)?.status] ?: "Offline")) }, motoboyId, { motoboyId = it })
                if (compativeis.isEmpty()) TextoDica(if (teste) "Nenhum motoboy com o modo teste ligado." else "Nenhum motoboy fora do modo teste.")
            }
            Espaco(8)
            PwInterruptor("Divisão própria nesta corrida", divisaoPropria, { divisaoPropria = it }, icone = PwIcones.Percent,
                descricao = if (divisaoPropria) "Vale só para esta corrida. Em branco = regra do cliente/padrão." else "Usa a divisão do cliente (ou o padrão das configurações).")
            if (divisaoPropria) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwCampo(repasseCorrida, { repasseCorrida = it }, "Motoboy %", Modifier.weight(1f), dica = numeroBr(empresa?.repassePct ?: cfg.numero("repasse_motoboy_pct", 67.5)), teclado = androidx.compose.ui.text.input.KeyboardType.Decimal)
                    PwCampo(devCorrida, { devCorrida = it }, "Dev %", Modifier.weight(1f), dica = numeroBr(empresa?.devPct ?: cfg.numero("dev_pct", 5.0)), teclado = androidx.compose.ui.text.input.KeyboardType.Decimal)
                }
            }
            Espaco(8)
            PwCampo(obs, { obs = it }, "Observações para o motoboy", linhas = 2)
        }

        PwErro(erro)
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PwRotulo(if (planejar && datas.size > 1) "Valor de cada corrida" else "Valor da corrida", linhas = 1)
                if (planejar && datas.size > 1) TextoDica("${datas.size} corridas na repetição")
            }
            com.pwexpress.core.designsystem.PwValor(moedaBr(valorComAcrescimo), Modifier.weight(1f), tamanhoMax = 30)
        }
        PwBotaoOuro(
            when {
                enviando -> "Criando…"
                planejar -> if (datas.size > 1) "Planejar ${datas.size} corridas" else "Planejar corrida"
                else -> if (teste) "Criar teste" else "Criar e despachar"
            },
            onClick = ::enviar, icone = if (planejar) PwIcones.CalendarCheck else PwIcones.Send, habilitado = !enviando, altura = 60.dp,
        )
    }

    if (escolhendoHora) SeletorDataHora("Começa em", inicio, onFechar = { escolhendoHora = false }, onEscolher = { inicio = it; escolhendoHora = false })
    marcando?.let { p ->
        com.pwexpress.core.mapa.PwSeletorMapa(p.lat?.let { la -> p.lng?.let { lo -> la to lo } } ?: paradas.firstNotNullOfOrNull { q -> q.lat?.let { la -> q.lng?.let { lo -> la to lo } } }, onFechar = { marcando = null }, titulo = if (p.coleta) "Local da coleta" else "Local da entrega") { la, lo ->
            marcando = null
            p.lat = la; p.lng = lo; p.endereco = "Buscando endereço…"
            escopo.launch { p.endereco = Geo.enderecoDoPonto(la, lo) }
        }
    }
}

@Composable
private fun Chip(texto: String, onClick: () -> Unit) {
    val c = Pw.cores
    Text(texto, color = c.texto, fontSize = 12.5.sp, maxLines = 1, softWrap = false, modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(c.vidroForte).border(1.dp, c.bordaForte, RoundedCornerShape(99.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp))
}

@Composable
internal fun CartaoParadaForm(p: ParadaForm, i: Int, total: Int, onMover: (Int) -> Unit, onRemover: () -> Unit, onMarcar: () -> Unit, comOpcoes: Boolean = false) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var sugestoes by remember { mutableStateOf<List<EnderecoG>>(emptyList()) }
    var digitado by remember { mutableStateOf(false) }
    var proximos by remember { mutableStateOf<List<RefG>?>(null) }
    var novaRef by remember { mutableStateOf("") }
    LaunchedEffect(p.endereco) {
        if (!digitado) return@LaunchedEffect
        delay(600)
        sugestoes = Geo.buscarEnderecos(p.endereco)
    }
    val forma = RoundedCornerShape(16.dp)
    val comRefs = modulo("pontos_referencia")
    Column(
        Modifier.fillMaxWidth().clip(forma).background(c.vidroSutil).border(1.dp, if (p.coleta) c.ouro.copy(alpha = 0.5f) else c.borda, forma).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(if (p.coleta) c.gradOuro else androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.texto, c.texto))), contentAlignment = Alignment.Center) {
                Text("${i + 1}", color = if (p.coleta) c.sobreOuro else c.fundo, fontWeight = FontWeight.Black, fontSize = 13.sp)
            }
            Text(if (p.coleta) "Coleta" else "Entrega", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f).padding(start = 4.dp), maxLines = 1)
            PwBotaoIcone(PwIcones.ArrowUp, "Subir", { onMover(-1) }, tamanho = 34.dp, habilitado = i > 0)
            PwBotaoIcone(PwIcones.ArrowDown, "Descer", { onMover(1) }, tamanho = 34.dp, habilitado = i < total - 1)
            if (total > 2) PwBotaoIcone(PwIcones.Trash2, "Remover parada", onRemover, tamanho = 34.dp, cor = c.vermelho)
        }
        PwSegmentado(listOf("Coleta" to PwIcones.ArrowUpFromLine, "Entrega" to PwIcones.ArrowDownToLine), if (p.coleta) 0 else 1, { p.coleta = it == 0 })
        PwCampo(p.endereco, { digitado = true; p.endereco = it; p.lat = null; p.lng = null }, "Endereço", dica = "Rua, número, cidade")
        sugestoes.forEach { s ->
            Text(s.descricao, color = c.texto, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable {
                digitado = false; p.endereco = s.descricao; p.lat = s.lat; p.lng = s.lng; sugestoes = emptyList()
            }.padding(8.dp))
        }
        if (p.lat != null) Detalhe(PwIcones.CircleCheck, "Localizado no mapa", c.verde)
        else Detalhe(PwIcones.MapPin, "Sem localização: busque o endereço ou marque no mapa")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwBotaoVidro("No mapa", onClick = onMarcar, icone = PwIcones.MapPinned, modifier = Modifier.weight(1f))
            PwBotaoVidro(
                if (p.aberta) "Menos" else "Detalhes" + if (p.refs.isNotEmpty()) " (${p.refs.size})" else "",
                onClick = { p.aberta = !p.aberta }, icone = if (p.aberta) PwIcones.ChevronUp else PwIcones.SlidersHorizontal, modifier = Modifier.weight(1f),
            )
        }
        if (p.aberta) {
            PwCampo(p.contato, { p.contato = it }, "Nome do contato")
            PwCampo(p.telefone, { p.telefone = it }, "Telefone", teclado = KeyboardType.Phone)
            PwCampo(p.instrucoes, { p.instrucoes = it }, "Instruções (portaria, bloco…)")
            if (comOpcoes) {
                PwRotulo(if (p.coleta) "Na coleta, confirmar com" else "Na entrega, confirmar com")
                TextoDica("Desligado = segue o padrão da rota.")
                PwInterruptor("Foto", p.exigeFoto, { p.exigeFoto = it }, icone = PwIcones.Camera)
                PwInterruptor(if (p.coleta) "Nome de quem entregou" else "Nome de quem recebeu", p.exigeNome, { p.exigeNome = it }, icone = PwIcones.UserRound)
                PwInterruptor("Assinatura", p.exigeAssinatura, { p.exigeAssinatura = it }, icone = PwIcones.PenLine)
                PwInterruptor("Código de 4 dígitos", p.exigeCodigo, { p.exigeCodigo = it }, icone = PwIcones.KeyRound)
                PwRotulo("Atende das … às …")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PwCampo(p.janelaDe, { p.janelaDe = it.take(5) }, "Das", dica = "09:00", teclado = KeyboardType.Number, modifier = Modifier.weight(1f))
                    PwCampo(p.janelaAte, { p.janelaAte = it.take(5) }, "Às", dica = "12:00", teclado = KeyboardType.Number, modifier = Modifier.weight(1f))
                }
            }
            if (comRefs) PwRotulo("Pontos de referência ${p.refs.size}/3")
            if (comRefs) p.refs.toList().forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.Landmark, null, tint = c.ouro, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(r.nome + listOfNotNull(r.tipo, r.distanciaM?.let { "$it m" }).joinToString(" · ").let { if (it.isNotEmpty()) "  ·  $it" else "" }, color = c.texto, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    PwBotaoIcone(PwIcones.X, "Remover", { p.refs.remove(r) }, tamanho = 32.dp)
                }
            }
            if (comRefs && p.refs.size < 3) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    PwCampo(novaRef, { novaRef = it }, "Nova referência", dica = "Ex.: em frente ao posto", modifier = Modifier.weight(1f))
                    PwBotaoIcone(PwIcones.Plus, "Adicionar", { if (novaRef.isNotBlank()) { p.refs.add(RefG(novaRef.trim(), null, null)); novaRef = "" } }, ativo = novaRef.isNotBlank())
                }
                PwBotaoVidro(if (proximos == null) "Sugerir próximos" else "Buscar de novo", onClick = {
                    val la = p.lat; val lo = p.lng
                    if (la != null && lo != null) escopo.launch { proximos = Geo.pontosProximos(la, lo) }
                }, icone = PwIcones.Sparkles, habilitado = p.lat != null, modifier = Modifier.fillMaxWidth())
                proximos?.let { lista ->
                    if (lista.isEmpty()) TextoDica("Nenhum estabelecimento com nome por perto.")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        lista.filter { s -> p.refs.none { it.nome.equals(s.nome, true) } }.take(8).forEach { s ->
                            Chip("+ ${s.nome}" + (s.distanciaM?.let { "  $it m" } ?: "")) { if (p.refs.size < 3) p.refs.add(s) }
                        }
                    }
                }
            }
        }
    }
}

/** Data e hora pelos seletores do Android. */
@Composable
fun SeletorDataHora(titulo: String, inicial: Long, onFechar: () -> Unit, onEscolher: (Long) -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = inicial }
        DatePickerDialog(ctx, { _, a, m, d ->
            cal.set(a, m, d)
            TimePickerDialog(ctx, { _, h, min ->
                cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                onEscolher(cal.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).apply { setOnCancelListener { onFechar() } }.show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle(titulo)
            datePicker.minDate = System.currentTimeMillis() - 1000
            setOnCancelListener { onFechar() }
        }.show()
    }
}
