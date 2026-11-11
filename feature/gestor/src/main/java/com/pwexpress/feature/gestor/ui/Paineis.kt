package com.pwexpress.feature.gestor.ui

import kotlinx.coroutines.launch

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
    /** Botões a mais no canto do mapa (ex.: camadas). */
    controles: @Composable ColumnScope.() -> Unit = {},
    /** Cartão por cima do mapa, no alto (ex.: motoboy selecionado). */
    topo: (@Composable () -> Unit)? = null,
    lista: LazyListScope.() -> Unit,
) {
    val c = Pw.cores
    var mostrando by com.pwexpress.core.designsystem.lembrarLigado("gaveta_$chave", true)
    var enquadrar by remember { mutableIntStateOf(0) }
    // tela cheia: some a gaveta, fica só o mapa (e os botões)
    var cheio by rememberSaveable { mutableStateOf(false) }
    val locaisPref by Estabelecimentos.visiveis.collectAsState()
    val comLocais = modulo("estabelecimentos")
    val locais = locaisPref && comLocais
    LaunchedEffect(comLocais) { if (!comLocais) Estabelecimentos.definir(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val alturaGaveta = maxHeight * 0.55f
        val margem = (if (cheio) BARRA else if (mostrando) alturaGaveta + BARRA else BARRA + 64.dp).value.toInt()
        mapa(margem, enquadrar)

        // controles do mapa (canto superior direito)
        Column(Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (comLocais) PwBotaoIcone(PwIcones.Store, if (locais) "Ocultar estabelecimentos" else "Mostrar estabelecimentos", { Estabelecimentos.definir(!locais) }, ativo = locais, tamanho = 44.dp)
            PwBotaoIcone(PwIcones.Crosshair, "Enquadrar tudo", { enquadrar++ }, tamanho = 44.dp)
            PwBotaoIcone(if (cheio) PwIcones.Lista else PwIcones.Maximize2, if (cheio) "Mostrar a lista" else "Tela cheia", { cheio = !cheio }, ativo = cheio, tamanho = 44.dp)
            controles()
        }
        topo?.let { t -> Box(Modifier.align(Alignment.TopStart).padding(top = 10.dp, start = 12.dp, end = 68.dp)) { t() } }
        if (!cheio) {

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
    // selecionado ≠ seguindo: tocar no motoboy só destaca; seguir é um botão e solta quando o dedo mexe no mapa
    var selecionado by rememberSaveable { mutableStateOf<String?>(null) }
    var seguir by rememberSaveable { mutableStateOf(false) }
    var irPara by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var filtro by rememberSaveable { mutableIntStateOf(0) }
    var busca by rememberSaveable { mutableStateOf("") }
    var calor by rememberSaveable { mutableStateOf(Calor.DESLIGADO) }
    var destaque by remember(selecionado) { mutableStateOf<Int?>(null) }
    var camadasAbertas by remember { mutableStateOf(false) }
    // camadas (guardadas no aparelho)
    var verPings by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_pings", true)
    var verPercursos by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_percursos", true)
    var verProcurando by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_procurando", true)
    var verRotas by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_rotas", true)
    var verParadas by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_paradas", true)
    var verOffline by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_offline", true)
    var verRaio by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_raio", true)
    var verNomes by com.pwexpress.core.designsystem.lembrarLigado("mapa_g_zonas", false)
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
        if (p.status == "offline" && !verOffline && m.id != selecionado) return@mapNotNull null
        val op = opacidadeNoMapa(p.status, p.posicaoEm, someH, esmaecer, agora) ?: return@mapNotNull null
        MarcadorMapa(m.id, p.lat!!, p.lng!!, corHex(corMotoboyMapa(p.status, p.modoTeste || m.modoTeste, c)), m.nome.substringBefore(" "), foto = m.foto,
            prioridade = when (p.status) { "offline" -> 0; "disponivel", "em_corrida" -> 2; else -> 1 },
            opacidade = op, teste = p.modoTeste || m.modoTeste)
    }
    // rotas planejadas das corridas ativas (tocar numa parada abre a corrida)
    val ativas = dados.corridas.filter { it.teste == teste && it.ativa && (selecionado == null || it.motoboyId == selecionado || it.motoboyId == null) }
    val paradasMapa = if (!verParadas) emptyList() else ativas.flatMap { cor ->
        cor.paradas.sortedBy { it.ordem }.mapIndexed { i, pa ->
            cor.id to com.pwexpress.core.mapa.PinoMapa(pa.lat, pa.lng, pa.coleta, false, pa.status == "concluida", numero = i + 1,
                raioM = if (verNomes && !pa.coleta) dados.cfgNum("zona_entrega_raio_m", 1000.0) else null)
        }
    }
    val rotasMapa = if (!verRotas) emptyList() else ativas.mapNotNull { it.rota }
    // percursos: o selecionado mostra o dia todo; os outros, só o trecho atual
    val regSel = selecionado?.let { registrosDe(dados, it) }.orEmpty()
    fun filtrar(regs: List<com.pwexpress.core.mapa.RegistroPercurso>) = if (verProcurando) regs else regs.filter { it.corrida }
    val percursos = if (!comTrilhas || !verPercursos) emptyList() else motoboys.flatMap { m ->
        if (m.id == selecionado) filtrar(regSel).let { lista -> lista.mapIndexed { i, r -> com.pwexpress.core.mapa.PercursoMapa.de(r, destaque = destaque == regSel.indexOf(r), apagado = destaque != null && destaque != regSel.indexOf(r)) } }
        else if (st(m.id) == "offline") emptyList()
        else filtrar(registrosDe(dados, m.id)).lastOrNull()?.let { r -> listOf(com.pwexpress.core.mapa.PercursoMapa.de(r, apagado = selecionado != null)) }.orEmpty()
    }
    val pontosCalor = when (calor) {
        Calor.DESLIGADO -> null
        Calor.PEDIDOS -> dados.corridas.filter { it.teste == teste }.flatMap { it.paradas }.map { it.lat to it.lng }
        Calor.PINGS -> dados.trilhas.values.flatten().map { it.lat to it.lng }
    }
    fun escolher(id: String) {
        if (selecionado == id) { selecionado = null; seguir = false; return }
        selecionado = id; seguir = false
        dados.posicao(id)?.let { p -> if (coordenadaValida(p.lat, p.lng)) irPara = p.lat!! to p.lng!! }
    }
    val membroSel = selecionado?.let { dados.membro(it) }
    val posSel = selecionado?.let { dados.posicao(it) }
    val raioSel = posSel?.raio?.takeIf { verRaio }

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
        subtitulo = "$emServico em serviço · ${todos.size} motoboy${if (todos.size == 1) "" else "s"} · ${ativas.size} rota${if (ativas.size == 1) "" else "s"} ativa${if (ativas.size == 1) "" else "s"}",
        mapa = { margem, enquadrar ->
            PwMapa(maximizavel = false, controles = true, controlesLocais = false,
                paradas = paradasMapa.map { it.second }, rota = null, rotas = rotasMapa, caminhoAuto = false, minhaPosicao = null, marcadores = marcadores, modifier = Modifier.fillMaxSize(),
                enquadrar = enquadrar, onMarcador = ::escolher, seguir = selecionado.takeIf { seguir && destaque == null }, destacar = selecionado,
                onArrastou = { if (seguir) seguir = false }, irPara = irPara,
                onParada = { i -> paradasMapa.getOrNull(i)?.first?.let(onCorrida) },
                circulo = raioSel?.let { it.first to it.second * 1000 },
                percursos = percursos, comPings = verPings, percursoTeste = teste, calor = pontosCalor, margemBaixo = margem,
                onToque = simulando?.let { mid -> { la: Double, lo: Double -> simularAte(mid, listOf(la to lo)) } },
            )
        },
        controles = {
            PwBotaoIcone(PwIcones.Layers, "Camadas do mapa", { camadasAbertas = true }, ativo = camadasAbertas, tamanho = 44.dp)
            PwBotaoIcone(PwIcones.Flame, "Calor: ${calor.rotulo}", { calor = Calor.entries[(calor.ordinal + 1) % Calor.entries.size] }, ativo = calor != Calor.DESLIGADO, tamanho = 44.dp)
        },
        topo = if (membroSel == null) null else ({
            CartaoSelecionado(membroSel, posSel, dados.corridas.firstOrNull { it.motoboyId == membroSel.id && it.ativa }, agora, seguir,
                onSeguir = { seguir = !seguir; if (seguir) destaque = null }, onFechar = { selecionado = null; seguir = false }, onCorrida = onCorrida,
                onCentralizar = { posSel?.let { p -> if (coordenadaValida(p.lat, p.lng)) irPara = p.lat!! to p.lng!! } })
        }),
    ) {
        item {
            PwErro(erro)
            if (todos.size > 4) {
                com.pwexpress.core.designsystem.PwCampo(busca, { busca = it }, "Buscar motoboy", Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }
            PwFiltros(filtros.map { (r, f) -> r to (if (r == "Todos") 0 else todos.count(f)) }, filtro, { filtro = it })
            if (comTrilhas && verPercursos) { Spacer(Modifier.height(8.dp)); LegendaPercurso(teste) }
            if (!dados.carregado) PwCarregando()
            else if (todos.isEmpty()) PwVazio("Nenhum motoboy ainda. Crie uma conta em Equipe.", icone = PwIcones.Bike)
            else if (motoboys.isEmpty()) PwVazio("Ninguém neste filtro.", icone = PwIcones.Search)
        }
        items(motoboys, key = { it.id }) { m ->
            val p = dados.posicao(m.id)
            val corrida = dados.corridas.firstOrNull { it.motoboyId == m.id && it.ativa }
            val sel = selecionado == m.id
            val forma = RoundedCornerShape(18.dp)
            Column(
                Modifier.fillMaxWidth().clip(forma).background(if (sel) c.ouro.copy(alpha = 0.10f) else c.vidroSutil)
                    .border(1.dp, if (sel) c.ouro else c.borda, forma).clickable { escolher(m.id) }.padding(12.dp),
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
                if (sel && comTrilhas) ListaRegistros(regSel, destaque, { destaque = if (destaque == it) null else it }, dados)
            }
        }
    }

    // a janela já rola sozinha: rolagem dentro de rolagem fechava o app
    if (camadasAbertas) com.pwexpress.core.designsystem.PwJanela("Camadas do mapa", { camadasAbertas = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextoDica("Escolha o que aparece no mapa. Fica guardado neste aparelho.")
            com.pwexpress.core.designsystem.PwInterruptor("Rotas planejadas", verRotas, { verRotas = it }, descricao = "O caminho das rotas ativas (toque numa parada para abrir a rota).", icone = PwIcones.Route)
            com.pwexpress.core.designsystem.PwInterruptor("Paradas das rotas", verParadas, { verParadas = it }, descricao = "Coletas e entregas numeradas, com ícone.", icone = PwIcones.MapPin)
            com.pwexpress.core.designsystem.PwInterruptor("Zona de cada entrega", verNomes, { verNomes = it }, descricao = "O raio da zona em volta de cada entrega.", icone = PwIcones.Radar)
            if (comTrilhas) {
                com.pwexpress.core.designsystem.PwInterruptor("Percursos", verPercursos, { verPercursos = it }, descricao = "O caminho feito pelos motoboys (o selecionado mostra o dia todo).", icone = PwIcones.Footprints)
                com.pwexpress.core.designsystem.PwInterruptor("Também fora de corrida", verProcurando, { verProcurando = it }, descricao = "Inclui o caminho enquanto procuravam corrida, não só em rota.", icone = PwIcones.Search)
                com.pwexpress.core.designsystem.PwInterruptor("Pings", verPings, { verPings = it }, descricao = "Os pontos de cada posição enviada pelo GPS.", icone = PwIcones.CircleDot)
            }
            com.pwexpress.core.designsystem.PwInterruptor("Motoboys offline", verOffline, { verOffline = it }, descricao = "Quem está offline aparece esmaecido.", icone = PwIcones.Power)
            com.pwexpress.core.designsystem.PwInterruptor("Raio do selecionado", verRaio, { verRaio = it }, descricao = "O raio de disponibilidade do motoboy escolhido.", icone = PwIcones.Target)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PwPilula(calor.rotulo, PwIcones.Flame, { calor = Calor.entries[(calor.ordinal + 1) % Calor.entries.size] }, ligada = calor != Calor.DESLIGADO)
                if (comIcones) PwPilula(rotulos.rotulo, PwIcones.MapPin, {
                    val r = com.pwexpress.core.mapa.Rotulos.entries
                    com.pwexpress.core.mapa.Estabelecimentos.definirRotulos(r[(rotulos.ordinal + 1) % r.size])
                }, ligada = rotulos != com.pwexpress.core.mapa.Rotulos.NADA)
                else PwPilula("Ruas: ${modoRuas.rotulo}", PwIcones.MapPin, {
                    com.pwexpress.core.mapa.Estabelecimentos.definirRuas(if (modoRuas == com.pwexpress.core.mapa.ModoRuas.RUA) com.pwexpress.core.mapa.ModoRuas.OCULTA else com.pwexpress.core.mapa.ModoRuas.RUA)
                }, ligada = modoRuas == com.pwexpress.core.mapa.ModoRuas.RUA)
            }
        }
    }
}

/** Cartão do motoboy escolhido, por cima do mapa: seguir (solta ao arrastar), centralizar, abrir a rota. */
@Composable
private fun CartaoSelecionado(
    m: com.pwexpress.feature.gestor.data.MembroG, p: PosicaoG?, corrida: com.pwexpress.feature.gestor.data.CorridaG?, agora: Long, seguindo: Boolean,
    onSeguir: () -> Unit, onFechar: () -> Unit, onCorrida: (String) -> Unit, onCentralizar: () -> Unit,
) {
    val c = Pw.cores
    val forma = RoundedCornerShape(20.dp)
    Column(
        Modifier.widthIn(max = 420.dp).clip(forma).background(c.fundo2.copy(alpha = 0.96f)).border(1.dp, c.ouro.copy(alpha = 0.5f), forma).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwAvatar(m.nome, m.foto, tamanho = 40.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(m.nome, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(listOfNotNull(rotuloMotoboy[p?.status] ?: "Offline", haQuanto(p?.posicaoEm, agora), p?.bateria?.let { "$it%" }).joinToString(" · "), color = c.texto3, fontSize = 12.sp, maxLines = 1)
            }
            PwBotaoIcone(PwIcones.X, "Fechar", onFechar, tamanho = 34.dp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PwPilula(if (seguindo) "Seguindo" else "Seguir", PwIcones.LocateFixed, onSeguir, ligada = seguindo)
            PwPilula("Centralizar", PwIcones.Crosshair, onCentralizar)
            corrida?.let { PwPilula("Rota ${it.numeroExibicao}", PwIcones.Route, { onCorrida(it.id) }) }
        }
        if (seguindo) Text("A câmera acompanha. Arraste o mapa para olhar livre.", color = c.texto3, fontSize = 11.5.sp)
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

/**
 * Registros do dia (procurando corrida / em corrida); tocar destaca no mapa.
 * Com muitos registros: resumo no topo, os "procurando" curtinhos ficam escondidos e a lista mostra os mais recentes.
 */
@Composable
internal fun ListaRegistros(regs: List<com.pwexpress.core.mapa.RegistroPercurso>, destaque: Int?, onEscolher: (Int) -> Unit, dados: Gestao.Dados, vazio: String = "Nenhum percurso nas últimas 12 h.") {
    val c = Pw.cores
    if (regs.isEmpty()) { if (vazio.isNotEmpty()) TextoDica(vazio); return }
    val fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    fun h(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).format(fmt)
    val br = java.util.Locale("pt", "BR")
    var todos by remember(regs.size) { mutableStateOf(false) }
    var soCorridas by remember { mutableStateOf(false) }
    val corridas = regs.count { it.corrida }
    val kmCorrida = regs.filter { it.corrida }.sumOf { it.metros } / 1000
    val kmTotal = regs.sumOf { it.metros } / 1000
    val tempo = regs.sumOf { (it.fim - it.inicio).coerceAtLeast(0) } / 60000
    // resumo
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("$corridas" to "rotas", String.format(br, "%.1f", kmCorrida) to "km em rota", String.format(br, "%.1f", kmTotal) to "km no dia",
            (if (tempo >= 60) "${tempo / 60}h${String.format(br, "%02d", tempo % 60)}" else "${tempo}min") to "em serviço").forEach { (v, r) ->
            Column(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(12.dp)).padding(vertical = 8.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text(v, color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1)
                Text(r, color = c.texto3, fontSize = 10.5.sp, maxLines = 1)
            }
        }
    }
    // procurando corrida parado (menos de 100 m) não ajuda a entender o dia: some, a não ser que peça tudo
    val uteis = regs.withIndex().filter { (_, r) -> r.corrida || r.metros >= 100 }.filter { (_, r) -> !soCorridas || r.corrida }
    val mostrar = (if (todos) uteis else uteis.takeLast(6)).reversed()
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        PwPilula("Só rotas", PwIcones.Package, { soCorridas = !soCorridas }, ligada = soCorridas)
        if (uteis.size > 6) PwPilula(if (todos) "Mostrar menos" else "Ver todos (${uteis.size})", PwIcones.Lista, { todos = !todos }, ligada = todos)
    }
    mostrar.forEach { (i, r) ->
        val forma = RoundedCornerShape(12.dp)
        val sel = destaque == i
        Row(
            Modifier.fillMaxWidth().clip(forma).background(if (sel) c.ouro.copy(alpha = 0.16f) else c.vidro).border(1.dp, if (sel) c.ouro else c.borda, forma)
                .clickable { onEscolher(i) }.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(99.dp)).background(if (r.corrida) c.ouro.copy(alpha = 0.18f) else c.vidroForte), contentAlignment = Alignment.Center) {
                Icon(if (r.corrida) PwIcones.Package else PwIcones.Search, null, tint = if (r.corrida) c.ouro else c.texto2, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            val num = r.corridaId?.let { id -> dados.corridas.firstOrNull { it.id == id }?.numeroExibicao }
            Column(Modifier.weight(1f)) {
                Text(if (r.corrida) "Rota ${num ?: ""}".trim() else "Procurando corrida", color = c.texto, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("${h(r.inicio)} – ${h(r.fim)} · ${(r.fim - r.inicio) / 60000} min", color = c.texto3, fontSize = 11.5.sp, maxLines = 1)
            }
            Text(String.format(br, "%.1f km", r.metros / 1000), color = if (r.corrida) c.ouro else c.texto2, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
    val escondidos = regs.size - regs.count { it.corrida || it.metros >= 100 }
    if (escondidos > 0 && !soCorridas) TextoDica("$escondidos período${if (escondidos == 1) "" else "s"} parado procurando corrida (menos de 100 m) não aparece${if (escondidos == 1) "" else "m"} na lista.")
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
            PwMapa(maximizavel = false, controles = true, controlesLocais = false, paradas = pinos, rota = null, rotas = caminhos, caminhoAuto = false, minhaPosicao = null, marcadores = online, modifier = Modifier.fillMaxSize(), enquadrar = "${pinos.size}-$enquadrar", margemBaixo = margem)
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
