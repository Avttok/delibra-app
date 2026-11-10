package com.pwexpress.feature.gestor.ui

import kotlinx.coroutines.launch

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.mapa.Estabelecimentos
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.mapa.TrilhaMapa
import com.pwexpress.core.mapa.coordenadaValida
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PosicaoG
import com.pwexpress.feature.gestor.data.Status
import kotlinx.coroutines.delay

/** Altura da barra de abas flutuante (a gaveta fica logo acima dela). */
private val BARRA = 92.dp

/**
 * Tela de mapa do gestor (igual ao site no celular): mapa inteiro, botões no canto
 * e a lista numa gaveta com só dois estados: mostrando ou escondida.
 */
@Composable
private fun TelaComMapa(
    chave: String,
    titulo: String,
    subtitulo: String,
    mapa: @Composable (margemBaixo: Int, enquadrar: Int) -> Unit,
    acoesGaveta: @Composable RowScope.() -> Unit = {},
    lista: LazyListScope.() -> Unit,
) {
    val c = Pw.cores
    var mostrando by com.pwexpress.core.designsystem.lembrarLigado("gaveta_$chave", true)
    var enquadrar by remember { mutableIntStateOf(0) }
    val locaisPref by Estabelecimentos.visiveis.collectAsState()
    val comLocais = modulo("estabelecimentos")
    val locais = locaisPref && comLocais
    LaunchedEffect(comLocais) { if (!comLocais) Estabelecimentos.definir(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val alturaGaveta = maxHeight * 0.55f
        val margem = (if (mostrando) alturaGaveta + BARRA else BARRA + 64.dp).value.toInt()
        mapa(margem, enquadrar)

        // controles do mapa (canto superior direito)
        Column(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (comLocais) PwBotaoIcone(PwIcones.Store, if (locais) "Ocultar estabelecimentos" else "Mostrar estabelecimentos", { Estabelecimentos.definir(!locais) }, ativo = locais, tamanho = 44.dp)
            PwBotaoIcone(PwIcones.Maximize2, "Enquadrar tudo", { enquadrar++ }, tamanho = 44.dp)
        }

        // gaveta
        val forma = RoundedCornerShape(26.dp)
        Column(
            Modifier.align(Alignment.BottomCenter).padding(start = 10.dp, end = 10.dp, bottom = BARRA).fillMaxWidth()
                .clip(forma).background(c.fundo2.copy(alpha = 0.96f)).border(1.dp, c.bordaForte, forma).animateContentSize(),
        ) {
            Row(
                Modifier.fillMaxWidth().clickable { mostrando = !mostrando }.padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1)
                    Text(subtitulo, color = c.texto3, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), content = acoesGaveta)
                Spacer(Modifier.width(6.dp))
                PwBotaoIcone(if (mostrando) PwIcones.ChevronDown else PwIcones.ChevronUp, if (mostrando) "Esconder lista" else "Mostrar lista", { mostrando = !mostrando }, tamanho = 38.dp)
            }
            if (mostrando) {
                LazyColumn(
                    Modifier.fillMaxWidth().height(alturaGaveta - 64.dp),
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    content = lista,
                )
            }
        }
    }
}

internal fun corHex(cor: androidx.compose.ui.graphics.Color) = String.format("#%06X", 0xFFFFFF and cor.toArgb())

/** Trilhas "em corrida" das últimas 12 h, separadas por sessão (como no site). */
private fun trilhasDe(dados: Gestao.Dados, ids: Set<String>): List<TrilhaMapa> =
    dados.trilhas.filterKeys { it in ids }.values.flatMap { pts ->
        pts.filter { it.status == "em_corrida" }.groupBy { it.sessao ?: "sem" }.values.map { s -> TrilhaMapa(s.map { it.lat to it.lng }) }
    }

private fun semSinal(p: PosicaoG?, agora: Long): Boolean {
    if (p == null || p.status == "offline") return false
    val limite = when (p.status) { "em_corrida" -> (if (p.economia) 90 else 45) * 3 * 1000L; else -> 3 * 60 * 3 * 1000L }
    return p.posicaoEm == null || agora - p.posicaoEm > limite
}

// =====================================================================
// Mapa ao vivo
// =====================================================================

/** Registros (procurando / corrida) de um motoboy a partir dos pings das últimas 12 h. */
internal fun registrosDe(dados: Gestao.Dados, id: String): List<com.pwexpress.core.mapa.RegistroPercurso> =
    com.pwexpress.core.mapa.aplicarAjustes(
        com.pwexpress.core.mapa.separarRegistros(dados.trilhas[id].orEmpty().map { com.pwexpress.core.mapa.PingMapa(it.lat, it.lng, it.status, it.corridaId, it.quando) }),
        id, dados.ajustesTrilha,
    )

private enum class Calor(val rotulo: String) { DESLIGADO("Sem calor"), PEDIDOS("Pedidos"), PINGS("Pings") }

@Composable
fun MapaG(onCorrida: (String) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val erro by Gestao.erro.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var agora by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { agora = System.currentTimeMillis(); delay(1000) } }
    var seguindo by rememberSaveable { mutableStateOf<String?>(null) }
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var busca by rememberSaveable { mutableStateOf("") }
    var calor by rememberSaveable { mutableStateOf(Calor.DESLIGADO) }
    var comPings by rememberSaveable { mutableStateOf(true) }
    var destaque by remember(seguindo) { mutableStateOf<Int?>(null) }
    val modoRuas by com.pwexpress.core.mapa.Estabelecimentos.ruas.collectAsState()
    val iconesLigados by com.pwexpress.core.mapa.Estabelecimentos.visiveis.collectAsState()
    val comIcones = modulo("estabelecimentos")
    val rotulos = com.pwexpress.core.mapa.Estabelecimentos.rotulos(modoRuas, iconesLigados && comIcones)
    val comTrilhas = modulo("trilhas_mapa")

    val ordem = mapOf("em_corrida" to 0, "disponivel" to 1)
    val todos = dados.equipe.filter { it.motoboy }
        .sortedWith(compareBy({ ordem[dados.posicao(it.id)?.status] ?: 2 }, { it.nome.lowercase() }))
    fun st(id: String) = dados.posicao(id)?.status ?: "offline"
    val filtros = listOf<Pair<String, (com.pwexpress.feature.gestor.data.MembroG) -> Boolean>>(
        "Todos" to { _ -> true }, "Em corrida" to { m -> st(m.id) == "em_corrida" }, "Disponíveis" to { m -> st(m.id) == "disponivel" },
        "Offline" to { m -> st(m.id) == "offline" }, "Sem sinal" to { m -> semSinal(dados.posicao(m.id), agora) }, "Teste" to { m -> m.modoTeste },
    )
    val termo = java.text.Normalizer.normalize(busca.trim(), java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
    fun casa(m: com.pwexpress.feature.gestor.data.MembroG) = termo.isEmpty() ||
        java.text.Normalizer.normalize("${m.nome} ${m.email}", java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase().contains(termo)
    val motoboys = todos.filter { filtros[filtro].second(it) && casa(it) }
    val emServico = todos.count { st(it.id) != "offline" }
    val someH = dados.cfgNum("mapa_offline_some_h", 24.0)
    val esmaecer = dados.cfgBool("mapa_offline_esmaecer", true)
    val marcadores = motoboys.mapNotNull { m ->
        val p = dados.posicao(m.id) ?: return@mapNotNull null
        if (!coordenadaValida(p.lat, p.lng)) return@mapNotNull null
        val op = opacidadeNoMapa(p.status, p.posicaoEm, someH, esmaecer, agora) ?: return@mapNotNull null
        MarcadorMapa(m.id, p.lat!!, p.lng!!, corHex(corMotoboyMapa(p.status, p.modoTeste || m.modoTeste, c)), m.nome.substringBefore(" "), foto = m.foto,
            prioridade = when (p.status) { "offline" -> 0; "disponivel", "em_corrida" -> 2; else -> 1 },
            opacidade = op, teste = p.modoTeste || m.modoTeste)
    }
    // selecionado: todos os registros do dia; os outros: só o registro atual (se estiver em serviço)
    val regSel = seguindo?.let { registrosDe(dados, it) }.orEmpty()
    val percursos = if (!comTrilhas) emptyList() else motoboys.flatMap { m ->
        if (m.id == seguindo) regSel.mapIndexed { i, r -> com.pwexpress.core.mapa.PercursoMapa.de(r, destaque = destaque == i, apagado = destaque != null && destaque != i) }
        else if (st(m.id) == "offline") emptyList()
        else registrosDe(dados, m.id).lastOrNull()?.let { r -> listOf(com.pwexpress.core.mapa.PercursoMapa.de(r, apagado = seguindo != null)) }.orEmpty()
    }
    val pontosCalor = when (calor) {
        Calor.DESLIGADO -> null
        Calor.PEDIDOS -> dados.corridas.filter { it.teste == teste }.flatMap { it.paradas }.map { it.lat to it.lng }
        Calor.PINGS -> dados.trilhas.values.flatten().map { it.lat to it.lng }
    }
    fun alternar(id: String) { seguindo = if (seguindo == id) null else id }

    // simulador (só motoboy em modo teste): toque no mapa e ele "anda" até lá mandando pings pelo servidor
    var simulando by remember { mutableStateOf<String?>(null) }
    var andando by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val escopoSim = androidx.compose.runtime.rememberCoroutineScope()
    val simKmh = dados.cfgNum("simulador_velocidade_kmh", 35.0)
    fun simularAte(mid: String, destino: List<Pair<Double, Double>>) {
        andando?.cancel()
        andando = escopoSim.launch {
            val ini = dados.posicao(mid)?.let { p -> p.lat?.let { la -> p.lng?.let { lo -> la to lo } } } ?: destino.first()
            val pts = listOf(ini) + destino
            val passoM = simKmh / 3.6 * 3 // a cada 3 s
            for (k in 0 until pts.size - 1) {
                val a = pts[k]; val b = pts[k + 1]
                val dist = com.pwexpress.feature.gestor.data.Geo.metros(a.first, a.second, b.first, b.second).toDouble()
                val n = maxOf(1, (dist / passoM).toInt())
                for (i in 1..n) {
                    val t = i.toDouble() / n
                    Gestao.simularPosicao(mid, a.first + (b.first - a.first) * t, a.second + (b.second - a.second) * t, simKmh / 3.6)
                        .onFailure { erroSim -> com.pwexpress.core.designsystem.PwToast.mostrar(erroSim.message ?: "Erro", com.pwexpress.core.designsystem.TipoAviso.ERRO); return@launch }
                    kotlinx.coroutines.delay(3000)
                }
            }
            Gestao.atualizar()
        }
    }

    TelaComMapa(
        chave = "mapa",
        titulo = "Mapa",
        subtitulo = "$emServico em serviço · ${todos.size} motoboy${if (todos.size == 1) "" else "s"}" + (seguindo?.let { id -> " · seguindo ${dados.membro(id)?.nome?.substringBefore(" ") ?: ""}" } ?: ""),
        mapa = { margem, enquadrar ->
            PwMapa(
                paradas = emptyList(), rota = null, minhaPosicao = null, marcadores = marcadores, modifier = Modifier.fillMaxSize(),
                enquadrar = enquadrar, onMarcador = ::alternar, seguir = seguindo.takeIf { destaque == null },
                percursos = percursos, comPings = comPings, percursoTeste = teste, calor = pontosCalor, margemBaixo = margem,
                onToque = simulando?.let { mid -> { la: Double, lo: Double -> simularAte(mid, listOf(la to lo)) } },
            )
        },
    ) {
        item {
            PwErro(erro)
            if (todos.size > 4) {
                com.pwexpress.core.designsystem.PwCampo(busca, { busca = it }, "Buscar motoboy", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            PwFiltros(filtros.map { (r, f) -> r to (if (r == "Todos") 0 else todos.count(f)) }, filtro, { filtro = it })
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PwPilula(calor.rotulo, PwIcones.Flame, { calor = Calor.entries[(calor.ordinal + 1) % Calor.entries.size] }, ligada = calor != Calor.DESLIGADO)
                if (comTrilhas) PwPilula("Pings", PwIcones.Route, { comPings = !comPings }, ligada = comPings)
                if (comIcones) PwPilula(rotulos.rotulo, PwIcones.MapPin, {
                    val r = com.pwexpress.core.mapa.Rotulos.entries
                    com.pwexpress.core.mapa.Estabelecimentos.definirRotulos(r[(rotulos.ordinal + 1) % r.size])
                }, ligada = rotulos != com.pwexpress.core.mapa.Rotulos.NADA)
                else PwPilula("Ruas: ${modoRuas.rotulo}", PwIcones.MapPin, {
                    com.pwexpress.core.mapa.Estabelecimentos.definirRuas(if (modoRuas == com.pwexpress.core.mapa.ModoRuas.RUA) com.pwexpress.core.mapa.ModoRuas.OCULTA else com.pwexpress.core.mapa.ModoRuas.RUA)
                }, ligada = modoRuas == com.pwexpress.core.mapa.ModoRuas.RUA)
            }
            if (comTrilhas) { Spacer(Modifier.height(8.dp)); LegendaPercurso(teste) }
            if (!dados.carregado) PwCarregando()
            else if (todos.isEmpty()) PwVazio("Nenhum motoboy ainda. Crie uma conta em Equipe.", icone = PwIcones.Bike)
            else if (motoboys.isEmpty()) PwVazio("Ninguém neste filtro.", icone = PwIcones.Search)
        }
        items(motoboys, key = { it.id }) { m ->
            val p = dados.posicao(m.id)
            val corrida = dados.corridas.firstOrNull { it.motoboyId == m.id && it.ativa }
            val sel = seguindo == m.id
            val forma = RoundedCornerShape(18.dp)
            Column(
                Modifier.fillMaxWidth().clip(forma).background(if (sel) c.ouro.copy(alpha = 0.10f) else c.vidroSutil)
                    .border(1.dp, if (sel) c.ouro else c.borda, forma).clickable { alternar(m.id) }.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PwAvatar(m.nome, m.foto, tamanho = 36.dp)
                    Spacer(Modifier.width(10.dp))
                    Text(m.nome, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (m.modoTeste) { Icon(PwIcones.FlaskConical, "Modo teste", tint = c.violeta, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)) }
                    PwSelo(rotuloMotoboy[p?.status] ?: "Offline", corMotoboy(p?.status, c))
                    if (corrida != null) { Spacer(Modifier.width(6.dp)); PwBotaoIcone(PwIcones.Route, "Abrir corrida ${corrida.numeroExibicao}", { onCorrida(corrida.id) }, tamanho = 34.dp, cor = c.ouro) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Detalhe(PwIcones.Timer, haQuanto(p?.posicaoEm, agora))
                    p?.precisaoM?.let { Detalhe(PwIcones.Crosshair, "± ${it.toInt()} m") }
                    p?.bateria?.let { Detalhe(PwIcones.BatteryMedium, "$it%") }
                }
                corrida?.let { Detalhe(PwIcones.Package, "Corrida ${it.numeroExibicao} · ${Status.rotulo[it.status]}") }
                if (semSinal(p, agora)) Detalhe(PwIcones.Alerta, "Sem sinal recente", c.vermelho)
                if (sel && m.modoTeste && dados.cfgBool("simulador_ligado", true)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        PwPilula(if (simulando == m.id) "Simulador ligado · toque no mapa" else "Simular posição", PwIcones.FlaskConical,
                            { simulando = if (simulando == m.id) null else m.id; if (simulando == null) andando?.cancel() }, ligada = simulando == m.id)
                        val rotaSim = corrida?.rota?.let { com.pwexpress.core.mapa.decodificarPolyline(it) }.orEmpty()
                        if (simulando == m.id && rotaSim.size >= 2) PwPilula("Fazer a rota", PwIcones.Route, { simularAte(m.id, rotaSim) })
                        if (andando?.isActive == true) PwPilula("Parar", PwIcones.X, { andando?.cancel() })
                    }
                }
                if (sel) {
                    Detalhe(PwIcones.LocateFixed, if (coordenadaValida(p?.lat, p?.lng)) "Seguindo no mapa · toque para soltar" else "Sem posição para seguir", c.ouro)
                    if (comTrilhas) ListaRegistros(regSel, destaque, { destaque = if (destaque == it) null else it }, dados)
                }
            }
        }
    }
}

/** Degradê do percurso: do início (esquerda) ao mais recente (direita). */
@Composable
internal fun LegendaPercurso(teste: Boolean) {
    val c = Pw.cores
    val cores = com.pwexpress.core.mapa.coresPercurso(teste).map { androidx.compose.ui.graphics.Color(it) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Início", color = c.texto3, fontSize = 11.5.sp)
        Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(99.dp)).background(androidx.compose.ui.graphics.Brush.horizontalGradient(cores)))
        Text("Agora", color = c.texto3, fontSize = 11.5.sp)
    }
}

/** Registros do dia (procurando corrida / em corrida); tocar destaca no mapa. */
@Composable
internal fun ListaRegistros(regs: List<com.pwexpress.core.mapa.RegistroPercurso>, destaque: Int?, onEscolher: (Int) -> Unit, dados: Gestao.Dados, vazio: String = "Nenhum percurso nas últimas 12 h.") {
    val c = Pw.cores
    if (regs.isEmpty()) { if (vazio.isNotEmpty()) TextoDica(vazio); return }
    val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    fun h(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).format(fmt)
    regs.withIndex().reversed().forEach { (i, r) ->
        val forma = RoundedCornerShape(12.dp)
        val sel = destaque == i
        Row(
            Modifier.fillMaxWidth().clip(forma).background(if (sel) c.ouro.copy(alpha = 0.16f) else c.vidro).border(1.dp, if (sel) c.ouro else c.borda, forma)
                .clickable { onEscolher(i) }.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(if (r.corrida) PwIcones.Package else PwIcones.Search, null, tint = if (r.corrida) c.ouro else c.texto2, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            val num = r.corridaId?.let { id -> dados.corridas.firstOrNull { it.id == id }?.numeroExibicao }
            Text(if (r.corrida) "Corrida ${num ?: ""}".trim() else "Procurando corrida", color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
            Text("${h(r.inicio)}–${h(r.fim)} · ${String.format(java.util.Locale("pt", "BR"), "%.1f km", r.metros / 1000)}", color = c.texto3, fontSize = 12.sp)
        }
    }
}

// =====================================================================
// Corridas
// =====================================================================

@Composable
fun CorridasG(onAbrir: (String) -> Unit, onNova: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val erro by Gestao.erro.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var selecionando by remember { mutableStateOf(false) }
    val marcadas = remember { mutableStateListOf<String>() }
    var excluindo by remember { mutableStateOf(false) }
    var limpando by remember { mutableStateOf(false) }
    val podeApagar = modulo("excluir_corridas")

    var busca by rememberSaveable { mutableStateOf("") }
    val todas = dados.corridas.filter { it.teste == teste }.filter { cor ->
        busca.isBlank() || combinaBusca(busca, cor.numeroExibicao, cor.numero.toString(), cor.motoboyId?.let { dados.membro(it)?.nome }, cor.motoboyNomeArquivado,
            cor.clienteNome, dados.empresa(cor.empresaId)?.nome, cor.observacoes, cor.paradas.joinToString(" ") { "${it.endereco} ${it.contatoNome ?: ""} ${it.recebedorNome ?: ""}" })
    }
    val planejadas = todas.filter { it.planejada }.sortedBy { it.agendadaPara ?: Long.MAX_VALUE }
    val esperando = todas.filter { it.status == Status.MONTADA }
    val listas = listOf(
        todas.filter { it.ativa } + esperando, planejadas, todas.filter { it.status == Status.CONCLUIDA }, todas.filter { it.status == Status.CANCELADA }, todas,
    )
    val lista = listas[filtro]
    val ativas = listas[0]
    val pinos = ativas.flatMap { cor -> cor.paradas.map { PinoMapa(it.lat, it.lng, it.coleta, false, it.status == "concluida") } }
    val online = dados.equipe.filter { it.motoboy && it.modoTeste == teste }.mapNotNull { m ->
        val p = dados.posicao(m.id)?.takeIf { it.status != "offline" && coordenadaValida(it.lat, it.lng) } ?: return@mapNotNull null
        MarcadorMapa(m.id, p.lat!!, p.lng!!, corHex(corMotoboyMapa(p.status, m.modoTeste, c)), m.nome.substringBefore(" "), foto = m.foto, teste = m.modoTeste,
            prioridade = when (p.status) { "offline" -> 0; "disponivel", "em_corrida" -> 2; else -> 1 })
    }

    TelaComMapa(
        chave = "rotas",
        titulo = if (selecionando) "${marcadas.size} selecionada${if (marcadas.size == 1) "" else "s"}" else "Rotas",
        subtitulo = "${if (ativas.isEmpty()) "Nenhuma ativa" else if (ativas.size == 1) "1 ativa" else "${ativas.size} ativas"} agora${if (teste) " · modo teste" else ""}",
        mapa = { margem, enquadrar ->
            // caminho de cada rota ativa (a pronta ou a calculada pelas paradas)
            val caminhos by androidx.compose.runtime.produceState(emptyList<String>(), ativas.map { it.id + (it.rota?.length ?: 0) }) {
                value = ativas.mapNotNull { cor -> cor.rota ?: com.pwexpress.core.mapa.Caminho.entre(cor.paradas.map { it.lat to it.lng }) }
            }
            PwMapa(paradas = pinos, rota = null, rotas = caminhos, caminhoAuto = false, minhaPosicao = null, marcadores = online, modifier = Modifier.fillMaxSize(), enquadrar = "${pinos.size}-$enquadrar", margemBaixo = margem)
        },
        acoesGaveta = {
            if (selecionando) {
                PwBotaoIcone(PwIcones.Trash2, "Apagar selecionadas", { if (marcadas.isNotEmpty()) excluindo = true }, tamanho = 38.dp, habilitado = marcadas.isNotEmpty(), cor = c.vermelho)
                PwBotaoIcone(PwIcones.X, "Cancelar seleção", { selecionando = false; marcadas.clear() }, tamanho = 38.dp)
            } else {
                if (podeApagar) PwBotaoIcone(PwIcones.History, "Apagar histórico", { limpando = true }, tamanho = 38.dp)
                if (podeApagar) PwBotaoIcone(PwIcones.ListChecks, "Selecionar para apagar", { selecionando = true; marcadas.clear() }, tamanho = 38.dp)
                PwBotaoIcone(PwIcones.Plus, "Nova corrida", onNova, ativo = true, tamanho = 38.dp)
            }
        },
    ) {
        item {
            CampoBusca(busca, { busca = it }, "Buscar nº, motoboy, cliente, endereço…")
            Spacer(Modifier.height(6.dp))
            PwFiltros(listOf("Ativas" to listas[0].size, "Planejadas" to planejadas.size, "Concluídas" to 0, "Canceladas" to 0, "Todas" to 0), filtro, { filtro = it })
            Spacer(Modifier.height(4.dp))
            if (esperando.isNotEmpty() && filtro == 0) TextoDica("${esperando.size} rota${if (esperando.size == 1) "" else "s"} montada${if (esperando.size == 1) "" else "s"} esperando você soltar para os motoboys.")
            TextoDica("${lista.size} rota${if (lista.size == 1) "" else "s"} · últimos 30 dias")
            PwErro(erro)
            if (!dados.carregado) PwCarregando()
            else if (lista.isEmpty()) PwVazio(
                if (filtro == 1) "Nenhuma corrida planejada. Toque em + e escolha “Planejar”." else "Nenhuma corrida aqui.",
                icone = if (filtro == 1) PwIcones.CalendarClock else PwIcones.Route,
            )
        }
        items(lista, key = { it.id }) { cor ->
            CartaoCorridaG(cor, dados, marcado = if (selecionando) cor.id in marcadas else null, onClick = {
                if (selecionando) { if (cor.id in marcadas) marcadas.remove(cor.id) else marcadas.add(cor.id) } else onAbrir(cor.id)
            })
        }
        if (selecionando && marcadas.isNotEmpty()) item {
            PwBotaoPerigo("Apagar ${marcadas.size}", onClick = { excluindo = true }, cheio = true, modifier = Modifier.fillMaxWidth())
        }
    }
    if (limpando) DialogoApagarHistorico(teste, onFechar = { limpando = false })
    if (excluindo) {
        DialogoExcluirCorridas(dados.corridas.filter { it.id in marcadas }, onFechar = { excluindo = false }, onExcluidas = { excluindo = false; selecionando = false; marcadas.clear() })
    }
}
