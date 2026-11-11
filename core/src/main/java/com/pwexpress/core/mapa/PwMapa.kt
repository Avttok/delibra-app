package com.pwexpress.core.mapa

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.maplibre.android.camera.CameraPosition
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import com.pwexpress.core.designsystem.PwIcones
import androidx.compose.foundation.layout.size
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pwexpress.core.designsystem.Pw
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/** Um ponto no mapa (parada da corrida). */
/** Pedido para o mapa centralizar num ponto (o [vez] muda a cada toque, para repetir no mesmo ponto). */
data class FocoMapa(val lat: Double, val lng: Double, val vez: Long = System.nanoTime())

data class PinoMapa(
    val lat: Double, val lng: Double, val coleta: Boolean, val atual: Boolean, val concluida: Boolean,
    /** Ícone do que vai no ponto (nome Lucide: file-text, package, pill…); null = loja na coleta, pacote na entrega. */
    val icone: String? = null,
    /** Número da parada na rota (aparece no canto do pino). */
    val numero: Int? = null,
    /** Itens a mais no mesmo ponto (selo "+N"). */
    val mais: Int = 0,
    /** Raio da zona deste ponto, em metros (desenha o círculo em volta). */
    val raioM: Double? = null,
)

/** Marcador livre (ex.: motoboy no mapa do gestor): cor em hex e rótulo curto. */
data class MarcadorMapa(
    val id: String, val lat: Double, val lng: Double, val cor: String, val rotulo: String,
    /** Foto do motoboy (vira um pino com a foto); null = pino com a inicial. */
    val foto: String? = null,
    /** false = marcador simples (ponto), sem pino. */
    val pino: Boolean = true,
    /** 0..1: motoboy offline vai esmaecendo conforme o tempo sem ser visto. */
    val opacidade: Float = 1f,
    /** Modo teste: selo de frasco e anel violeta no lugar da moto. */
    val teste: Boolean = false,
    /** Quem fica por cima quando se sobrepõem: 0 offline, 1 normal, 2 disponível/em corrida, 3 você. */
    val prioridade: Int = 1,
)

/** Linha de trajeto (trilha real do motoboy). */
data class TrilhaMapa(val pontos: List<Pair<Double, Double>>)

private fun Color.hex(): String = String.format("#%06X", 0xFFFFFF and toArgb())

private const val ESTILO_CLARO = "https://tiles.openfreemap.org/styles/positron"
private const val ESTILO_ESCURO = "https://tiles.openfreemap.org/styles/dark"

/** Centro padrão (Santa Catarina), igual ao site. */
val CENTRO_PADRAO = -27.2 to -49.5

/** Coordenada utilizável (descarta 0,0 e valores fora do mundo). */
fun coordenadaValida(lat: Double?, lng: Double?): Boolean =
    lat != null && lng != null && lat in -85.0..85.0 && lng in -180.0..180.0 && !(kotlin.math.abs(lat) < 0.001 && kotlin.math.abs(lng) < 0.001)

/** Nomes de rua: escritos na própria rua ou escondidos (separado dos estabelecimentos). */
enum class ModoRuas(val rotulo: String) { RUA("Na rua"), OCULTA("Ocultos") }

/** O que aparece de nome no mapa: combinação das ruas com os ícones de estabelecimento. */
enum class Rotulos(val rotulo: String) { TUDO("Ruas e ícones"), RUAS("Só ruas"), ICONES("Só ícones"), NADA("Sem nomes") }

/** Mostrar/ocultar estabelecimentos e o modo dos nomes de rua em todos os mapas (lembrado no aparelho, como no site). */
object Estabelecimentos {
    private val _visiveis = MutableStateFlow(true)
    val visiveis: StateFlow<Boolean> = _visiveis.asStateFlow()
    private val _ruas = MutableStateFlow(ModoRuas.RUA)
    val ruas: StateFlow<ModoRuas> = _ruas.asStateFlow()
    private var prefs: android.content.SharedPreferences? = null
    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("pwex_mapa", Context.MODE_PRIVATE)
        _visiveis.value = prefs!!.getBoolean("estabelecimentos", true)
        // "PLACA" (modo antigo, removido) volta para RUA
        _ruas.value = if (prefs!!.getString("ruas", "RUA") == "OCULTA") ModoRuas.OCULTA else ModoRuas.RUA
    }
    fun definir(v: Boolean) { _visiveis.value = v; prefs?.edit()?.putBoolean("estabelecimentos", v)?.apply() }
    fun definirRuas(m: ModoRuas) { _ruas.value = m; prefs?.edit()?.putString("ruas", m.name)?.apply() }

    fun rotulos(ruas: ModoRuas, icones: Boolean): Rotulos = when {
        ruas == ModoRuas.RUA && icones -> Rotulos.TUDO
        ruas == ModoRuas.RUA -> Rotulos.RUAS
        icones -> Rotulos.ICONES
        else -> Rotulos.NADA
    }

    fun definirRotulos(r: Rotulos) {
        definirRuas(if (r == Rotulos.TUDO || r == Rotulos.RUAS) ModoRuas.RUA else ModoRuas.OCULTA)
        definir(r == Rotulos.TUDO || r == Rotulos.ICONES)
    }
}

/**
 * Mapa do app (MapLibre + OpenStreetMap). Tema claro/noturno automático,
 * rota planejada em dourado, paradas, motoboys e a posição do motoboy.
 */
@Composable
private fun MapaBase(
    paradas: List<PinoMapa>,
    rota: String?,
    minhaPosicao: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
    marcadores: List<MarcadorMapa> = emptyList(),
    onToque: ((Double, Double) -> Unit)? = null,
    /** Mude este valor para o mapa enquadrar tudo de novo. */
    enquadrar: Any? = null,
    /** Toque num marcador (devolve o id). */
    onMarcador: ((String) -> Unit)? = null,
    /** Id do marcador que a câmera acompanha (fica destacado). */
    seguir: String? = null,
    trilhas: List<TrilhaMapa> = emptyList(),
    /** Centro da câmera quando ela para (para escolher um ponto arrastando o mapa). */
    onCentro: ((Double, Double) -> Unit)? = null,
    /** Onde a câmera começa quando não há nada para enquadrar. */
    inicio: Pair<Double, Double>? = null,
    zoomInicio: Double = 7.0,
    /** Espaço (dp) coberto por painéis em baixo: o enquadramento e o seguir desviam dele. */
    margemBaixo: Int = 0,
    /** Ao mudar, a câmera desliza até este ponto (mantendo o zoom). */
    irPara: Pair<Double, Double>? = null,
    /** Toque num endereço da lista: centraliza e aproxima nele (mude o [FocoMapa.vez] para repetir). */
    focar: FocoMapa? = null,
    /** Sem rota pronta, traça sozinho o caminho pelas ruas entre as paradas (na ordem). */
    caminhoAuto: Boolean = true,
    /** Vários caminhos ao mesmo tempo (ex.: todas as rotas ativas), além de [rota]. */
    rotas: List<String> = emptyList(),
    /** Círculo (centro, raio em metros): o raio de disponibilidade do motoboy. */
    circulo: Pair<Pair<Double, Double>, Double>? = null,
    /** Percursos (registros) em degradê, do início ao mais recente. */
    percursos: List<PercursoMapa> = emptyList(),
    /** Mostra os pings (pontos) dos percursos. */
    comPings: Boolean = true,
    /** Cores de teste (cinza → violeta) nos percursos. */
    percursoTeste: Boolean = false,
    /** Mapa de calor (null = desligado). */
    calor: List<Pair<Double, Double>>? = null,
    /** Posição do motoboy em corrida: o "a fazer" sai dele e passa pelas paradas que faltam. */
    restanteDe: Pair<Double, Double>? = null,
    /** Legenda "Feito / A fazer" no canto quando há trilha feita. */
    legenda: Boolean = true,
    /** A [minhaPosicao] aparece com a sua foto (pino), em vez do ponto. */
    euComFoto: Boolean = true,
    /** Botões por cima do mapa (só na tela cheia, ou também no pequeno quando [botoesSempre]). */
    botoes: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
    destacar: String? = null,
    onArrastou: (() -> Unit)? = null,
    onParada: ((Int) -> Unit)? = null,
    /** Aproximar, afastar, ir até mim e estabelecimentos à mão (tela cheia e mapas que ocupam a tela). */
    controles: Boolean = false,
    controlesLocais: Boolean = true,
) {
    val ctx = LocalContext.current
    val arrastou by rememberUpdatedState(onArrastou)
    val toqueParada by rememberUpdatedState(onParada)
    val escuro = Pw.cores.escuro
    val ouro = Pw.cores.ouro.hex()
    val ouroInt = Pw.cores.ouro.toArgb()
    // nomes dos estabelecimentos seguem o tema (dourado, violeta no teste, vermelho do Dev)
    val ouroTexto = (if (escuro) Pw.cores.ouroClaro else Pw.cores.ouroEscuro).hex()
    val toque by rememberUpdatedState(onToque)
    val toqueMarcador by rememberUpdatedState(onMarcador)
    val centro by rememberUpdatedState(onCentro)
    val ciclo = LocalLifecycleOwner.current
    remember { Estabelecimentos.init(ctx) }
    val comLocais by Estabelecimentos.visiveis.collectAsState()
    val modoRuas by Estabelecimentos.ruas.collectAsState()
    val corRua = if (escuro) "#E7E5E4" else "#44403C"
    val fotos = remember { androidx.compose.runtime.mutableStateMapOf<String, android.graphics.Bitmap>() }
    val carregador = remember { coil.ImageLoader(ctx.applicationContext) }
    val densidade = ctx.resources.displayMetrics.density
    val margemPx = margemBaixo * densidade

    val mapView = remember {
        MapLibre.getInstance(ctx.applicationContext)
        MapView(ctx).apply {
            onCreate(null)
            // dentro de telas que rolam: o dedo no mapa move o mapa, não a página
            setOnTouchListener { v, e ->
                if (e.actionMasked == android.view.MotionEvent.ACTION_DOWN) v.parent?.requestDisallowInterceptTouchEvent(true)
                false
            }
        }
    }
    var mapa by remember { mutableStateOf<MapLibreMap?>(null) }
    var estilo by remember { mutableStateOf<Style?>(null) }
    var enquadrou by remember { mutableStateOf(false) }

    // ciclo de vida do MapView
    DisposableEffect(ciclo) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        ciclo.lifecycle.addObserver(obs)
        onDispose {
            ciclo.lifecycle.removeObserver(obs)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    var ultimoEnquadrar by remember { mutableStateOf<Any?>(enquadrar) }

    // estilo (recarrega ao trocar o tema)
    LaunchedEffect(escuro, ouro, percursoTeste) {
        estilo = null
        enquadrou = false
        mapView.getMapAsync { m ->
            val primeira = mapa == null
            mapa = m
            if (primeira) {
                m.uiSettings.isCompassEnabled = false
                m.uiSettings.isRotateGesturesEnabled = false
                m.uiSettings.isTiltGesturesEnabled = false
                m.uiSettings.isLogoEnabled = false
                val (la, lo) = inicio ?: CENTRO_PADRAO
                m.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(la, lo), if (inicio != null) zoomInicio.coerceAtLeast(13.0) else zoomInicio))
                m.addOnMapClickListener { p ->
                    val cb = toqueMarcador
                    if (cb != null) {
                        val tela = m.projection.toScreenLocation(p)
                        val raio = 22 * densidade
                        val achados = m.queryRenderedFeatures(android.graphics.RectF(tela.x - raio, tela.y - raio, tela.x + raio, tela.y + raio), "marcadores", "marcadores-ponto")
                        achados.firstOrNull()?.getStringProperty("id")?.let { cb(it); return@addOnMapClickListener true }
                    }
                    toqueParada?.let { cb ->
                        val tela = m.projection.toScreenLocation(p)
                        val raio = 22 * densidade
                        m.queryRenderedFeatures(android.graphics.RectF(tela.x - raio, tela.y - raio * 2, tela.x + raio, tela.y + raio), "paradas")
                            .firstOrNull()?.getNumberProperty("idx")?.toInt()?.let { cb(it); return@addOnMapClickListener true }
                    }
                    toque?.let { it(p.latitude, p.longitude); true } ?: false
                }
                // o dedo mexeu no mapa: quem estava "seguindo" solta (o gestor fica livre para olhar a rota)
                m.addOnCameraMoveStartedListener { motivo -> if (motivo == org.maplibre.android.maps.MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) arrastou?.invoke() }
                m.addOnCameraIdleListener { centro?.let { cb -> m.cameraPosition.target?.let { t -> cb(t.latitude, t.longitude) } } }
            }
            m.setStyle(Style.Builder().fromUri(if (escuro) ESTILO_ESCURO else ESTILO_CLARO)) { s ->
                criarCamadas(ctx, s, escuro, ouro, ouroTexto, percursoTeste, Estabelecimentos.ruas.value)
                estilo = s
            }
        }
    }

    LaunchedEffect(mapa, irPara) {
        val m = mapa ?: return@LaunchedEffect
        val p = irPara ?: return@LaunchedEffect
        m.easeCamera(CameraUpdateFactory.newLatLng(LatLng(p.first, p.second)), 400)
    }
    LaunchedEffect(mapa, focar) {
        val m = mapa ?: return@LaunchedEffect
        val f = focar ?: return@LaunchedEffect
        val zoom = maxOf(m.cameraPosition.zoom, 15.5)
        m.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(f.lat, f.lng), zoom), 600)
    }

    // estabelecimentos: liga/desliga
    LaunchedEffect(estilo, comLocais) {
        val s = estilo ?: return@LaunchedEffect
        val v = if (comLocais) Property.VISIBLE else Property.NONE
        listOf("pwex-poi-ponto", "pwex-poi").forEach { s.getLayer(it)?.setProperties(PropertyFactory.visibility(v)) }
    }
    LaunchedEffect(estilo, modoRuas) { estilo?.let { runCatching { aplicarModoRuas(it, modoRuas, corRua) } } }

    // fotos dos pinos (baixadas uma vez)
    // você no mapa: pino com a sua foto (a bolinha só se não houver perfil)
    val meuPerfil by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    val euMarcador = minhaPosicao?.takeIf { euComFoto && coordenadaValida(it.first, it.second) }?.let {
        MarcadorMapa("eu", it.first, it.second, ouro, meuPerfil?.nome?.substringBefore(" ")?.takeIf { n -> n.isNotBlank() } ?: "Você", foto = meuPerfil?.fotoUrl, prioridade = 3)
    }
    val todosMarcadores = marcadores + listOfNotNull(euMarcador)
    LaunchedEffect(todosMarcadores.mapNotNull { it.foto }.toSet()) {
        todosMarcadores.mapNotNull { it.foto }.distinct().filter { it !in fotos }.forEach { url ->
            val r = carregador.execute(coil.request.ImageRequest.Builder(ctx).data(url).size(160).allowHardware(false).build())
            (r.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let { fotos[url] = it }
        }
    }

    LaunchedEffect(estilo, percursos, comPings, percursoTeste) {
        val s = estilo ?: return@LaunchedEffect
        val cores = coresPercurso(percursoTeste)
        val validos = percursos.map { it.copy(pontos = it.pontos.filter { p -> coordenadaValida(p.first, p.second) }, linha = it.linha?.filter { p -> coordenadaValida(p.first, p.second) }) }
        (s.getSource("percursos") as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(
            validos.filter { (it.linha ?: it.pontos).size >= 2 }.map { r ->
                Feature.fromGeometry(LineString.fromLngLats((r.linha ?: r.pontos).map { Point.fromLngLat(it.second, it.first) })).apply {
                    addBooleanProperty("destaque", r.destaque); addBooleanProperty("apagado", r.apagado)
                }
            },
        ))
        (s.getSource("pings") as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(
            if (!comPings) emptyList() else validos.flatMap { r ->
                r.pontos.mapIndexed { k, p ->
                    Feature.fromGeometry(Point.fromLngLat(p.second, p.first)).apply {
                        addStringProperty("cor", misturarCor(cores, if (r.pontos.size < 2) 1.0 else k.toDouble() / (r.pontos.size - 1)))
                        addBooleanProperty("ponta", k == 0 || k == r.pontos.size - 1)
                        addBooleanProperty("apagado", r.apagado)
                    }
                }
            },
        ))
    }
    LaunchedEffect(estilo, calor) {
        val s = estilo ?: return@LaunchedEffect
        (s.getSource("calor") as? GeoJsonSource)?.setGeoJson(FeatureCollection.fromFeatures(
            calor.orEmpty().filter { coordenadaValida(it.first, it.second) }.map { Feature.fromGeometry(Point.fromLngLat(it.second, it.first)) },
        ))
    }

    // dados
    LaunchedEffect(estilo, circulo) {
        val s = estilo ?: return@LaunchedEffect
        val anel = circulo?.let { (c, m) -> pontosDoCirculo(c.first, c.second, m) }
        (s.getSource("circulo") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                anel?.let { listOf(Feature.fromGeometry(org.maplibre.geojson.Polygon.fromLngLats(listOf(it.map { p -> Point.fromLngLat(p.second, p.first) })))) } ?: emptyList(),
            ),
        )
    }

    // rota que vem pronta ou, sem ela, o caminho calculado entre as paradas
    val chaveParadas = paradas.filter { coordenadaValida(it.lat, it.lng) }.joinToString("|") { "${it.lat},${it.lng}" }
    val caminho by androidx.compose.runtime.produceState<String?>(null, rota, chaveParadas, caminhoAuto) {
        value = if (rota != null || !caminhoAuto || rotas.isNotEmpty()) null else Caminho.entre(paradas.filter { coordenadaValida(it.lat, it.lng) }.map { it.lat to it.lng })
    }
    val rotaDesenhada = rota ?: caminho

    // "a fazer" de verdade: do motoboy até as paradas que faltam (recalcula ao andar ~80 m ou concluir uma parada)
    val pendentes = paradas.filter { !it.concluida && coordenadaValida(it.lat, it.lng) }
    val todasFeitas = paradas.isNotEmpty() && pendentes.isEmpty()
    val chavePend = pendentes.joinToString("|") { "${it.lat},${it.lng}" }
    var origemRest by remember { mutableStateOf<Pair<Pair<Double, Double>, String>?>(null) }
    var restante by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(restanteDe, chavePend) {
        val de = restanteDe?.takeIf { coordenadaValida(it.first, it.second) }
        if (de == null || pendentes.isEmpty()) { restante = null; origemRest = null; return@LaunchedEffect }
        val o = origemRest
        if (o != null && o.second == chavePend && distanciaM(o.first.first, o.first.second, de.first, de.second) < 80) return@LaunchedEffect
        origemRest = de to chavePend
        Caminho.entre(listOf(de) + pendentes.take(24).map { it.lat to it.lng })?.let { restante = it }
    }
    val aFazer = when {
        todasFeitas -> emptyList()
        restante != null -> listOf(restante!!)
        else -> listOfNotNull(rotaDesenhada) + rotas
    }
    val fundo = if (todasFeitas || restante != null) listOfNotNull(rotaDesenhada) + rotas else emptyList()
    LaunchedEffect(estilo, paradas, aFazer, fundo, minhaPosicao, todosMarcadores, seguir, destacar, trilhas, enquadrar, fotos.size) {
        val s = estilo ?: return@LaunchedEffect
        val m = mapa ?: return@LaunchedEffect
        if (enquadrar != ultimoEnquadrar) { ultimoEnquadrar = enquadrar; enquadrou = false }
        // offline por baixo, disponível/em corrida por cima, você no topo
        val validos = todosMarcadores.filter { coordenadaValida(it.lat, it.lng) }.sortedBy { it.prioridade }
        val pinos = paradas.filter { coordenadaValida(it.lat, it.lng) }
        val eu = minhaPosicao?.takeIf { coordenadaValida(it.first, it.second) }
        val euPonto = eu?.takeIf { euMarcador == null }

        fun feicoes(lista: List<String>) = FeatureCollection.fromFeatures(
            lista.map { decodificarPolyline(it) }.filter { it.size >= 2 }.map { l -> Feature.fromGeometry(LineString.fromLngLats(l.map { Point.fromLngLat(it.second, it.first) })) },
        )
        (s.getSource("rota") as? GeoJsonSource)?.setGeoJson(feicoes(aFazer))
        (s.getSource("plano") as? GeoJsonSource)?.setGeoJson(feicoes(fundo))
        (s.getSource("trilhas") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                trilhas.filter { it.pontos.size >= 2 }.map { t -> Feature.fromGeometry(LineString.fromLngLats(t.pontos.map { Point.fromLngLat(it.second, it.first) })) },
            ),
        )

        (s.getSource("paradas") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                pinos.map { p ->
                    val img = IconesMapa.chave(p.coleta, p.atual, p.concluida, p.icone, p.numero, p.mais, escuro, ouroInt)
                    if (s.getImage(img) == null) s.addImage(img, IconesMapa.desenhar(ctx, p.coleta, p.atual, p.concluida, p.icone, p.numero, p.mais, escuro, ouroInt))
                    Feature.fromGeometry(Point.fromLngLat(p.lng, p.lat)).apply {
                        addStringProperty("img", img)
                        addNumberProperty("idx", paradas.indexOf(p))
                        addNumberProperty("ordem", if (p.atual) 3 else if (p.concluida) 0 else if (p.coleta) 2 else 1)
                    }
                },
            ),
        )
        // zona de cada ponto (raio em volta)
        (s.getSource("zonas") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                pinos.filter { (it.raioM ?: 0.0) > 0 }.map { p ->
                    Feature.fromGeometry(org.maplibre.geojson.Polygon.fromLngLats(listOf(pontosDoCirculo(p.lat, p.lng, p.raioM!!).map { Point.fromLngLat(it.second, it.first) })))
                        .apply { addStringProperty("cor", if (p.coleta) ouro else "#38BDF8") }
                },
            ),
        )

        (s.getSource("eu") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(euPonto?.let { listOf(Feature.fromGeometry(Point.fromLngLat(it.second, it.first))) } ?: emptyList()),
        )

        (s.getSource("marcadores") as? GeoJsonSource)?.setGeoJson(
            FeatureCollection.fromFeatures(
                validos.map { mk ->
                    val sel = mk.id == seguir || mk.id == destacar
                    val foto = mk.foto?.let { fotos[it] }
                    val img = "pino-${mk.id}-${mk.cor}-$sel-${foto != null}-$escuro-${mk.teste}"
                    if (mk.pino && s.getImage(img) == null) runCatching {
                        s.addImage(img, desenharPino(ctx, foto, mk.rotulo.trim().take(1).uppercase(), android.graphics.Color.parseColor(mk.cor), sel, escuro, mk.teste))
                    }
                    Feature.fromGeometry(Point.fromLngLat(mk.lng, mk.lat)).apply {
                        addStringProperty("id", mk.id)
                        addStringProperty("icone", if (mk.pino) img else "")
                        addBooleanProperty("pino", mk.pino)
                        addStringProperty("cor", mk.cor)
                        addStringProperty("rotulo", mk.rotulo)
                        addStringProperty("inicial", mk.rotulo.trim().take(1).uppercase())
                        addNumberProperty("raio", if (sel) 17 else 13)
                        addNumberProperty("borda", if (sel) 4 else 3)
                        addNumberProperty("opacidade", mk.opacidade.coerceIn(0f, 1f))
                        addNumberProperty("ordem", mk.prioridade)
                    }
                },
            ),
        )

        // seguir: a câmera vai até o motoboy a cada nova posição
        val alvo = seguir?.let { id -> validos.firstOrNull { it.id == id } }
        if (alvo != null) {
            val zoom = maxOf(m.cameraPosition.zoom, 15.0)
            m.easeCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(alvo.lat, alvo.lng)).zoom(zoom).padding(0.0, 0.0, 0.0, margemPx.toDouble()).build(),
                ),
                800,
            )
            enquadrou = true
            return@LaunchedEffect
        }

        if (!enquadrou) {
            val pontos = (pinos.map { LatLng(it.lat, it.lng) } + listOfNotNull(eu?.let { LatLng(it.first, it.second) }) +
                validos.map { LatLng(it.lat, it.lng) } +
                (circulo?.let { (c, m) -> pontosDoCirculo(c.first, c.second, m).filterIndexed { i, _ -> i % 8 == 0 }.map { LatLng(it.first, it.second) } } ?: emptyList())).distinct()
            when {
                pontos.size >= 2 -> {
                    val p = (56 * densidade).toInt()
                    val cam = m.getCameraForLatLngBounds(
                        LatLngBounds.Builder().includes(pontos).build(),
                        intArrayOf(p, p, p, p + margemPx.toInt()),
                    )
                    if (cam != null) {
                        m.easeCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(cam).zoom(cam.zoom.coerceAtMost(15.5)).build()), 700)
                    }
                }
                pontos.size == 1 -> m.easeCamera(
                    CameraUpdateFactory.newCameraPosition(CameraPosition.Builder().target(pontos[0]).zoom(15.0).padding(0.0, 0.0, 0.0, margemPx.toDouble()).build()),
                    700,
                )
            }
            if (pontos.isNotEmpty()) enquadrou = true
        }
    }

    androidx.compose.foundation.layout.Box(modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.matchParentSize())
        if (legenda && trilhas.any { it.pontos.size >= 2 }) LegendaLinhas(aFazer.isNotEmpty(), Modifier.align(androidx.compose.ui.Alignment.BottomStart).padding(start = 10.dp, bottom = (margemBaixo + 10).dp))
        if (botoes != null || controles) androidx.compose.foundation.layout.Column(
            Modifier.align(androidx.compose.ui.Alignment.CenterEnd).padding(end = 12.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
        ) {
            botoes?.invoke(this)
            if (controles) {
                if (botoes != null) androidx.compose.foundation.layout.Spacer(Modifier.size(4.dp))
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Plus, "Aproximar", { mapa?.animateCamera(CameraUpdateFactory.zoomIn(), 250) }, tamanho = 44.dp)
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Minus, "Afastar", { mapa?.animateCamera(CameraUpdateFactory.zoomOut(), 250) }, tamanho = 44.dp)
                minhaPosicao?.let { eu ->
                    com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.LocateFixed, "Ir até mim", {
                        mapa?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(eu.first, eu.second), maxOf(mapa?.cameraPosition?.zoom ?: 15.0, 15.0)), 500)
                    }, tamanho = 44.dp)
                }
                if (controlesLocais) com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Store, if (comLocais) "Esconder estabelecimentos" else "Mostrar estabelecimentos",
                    { Estabelecimentos.definir(!comLocais) }, tamanho = 44.dp, ativo = comLocais)
            }
        }
    }
}

/**
 * Mapa do app (motoboy e gestor): rota planejada em dourado, paradas com ícones, motoboys e a sua posição.
 * Todo mapa tem o botão de **tela cheia** (canto de cima, à esquerda): abre o mesmo mapa ocupando a tela,
 * com tudo funcionando (toques, seguir, marcadores) e os [botoesTelaCheia] próprios de cada tela.
 */
@Composable
fun PwMapa(
    paradas: List<PinoMapa>,
    rota: String?,
    minhaPosicao: Pair<Double, Double>?,
    modifier: Modifier = Modifier,
    marcadores: List<MarcadorMapa> = emptyList(),
    onToque: ((Double, Double) -> Unit)? = null,
    enquadrar: Any? = null,
    onMarcador: ((String) -> Unit)? = null,
    seguir: String? = null,
    trilhas: List<TrilhaMapa> = emptyList(),
    onCentro: ((Double, Double) -> Unit)? = null,
    inicio: Pair<Double, Double>? = null,
    zoomInicio: Double = 7.0,
    margemBaixo: Int = 0,
    irPara: Pair<Double, Double>? = null,
    focar: FocoMapa? = null,
    caminhoAuto: Boolean = true,
    rotas: List<String> = emptyList(),
    circulo: Pair<Pair<Double, Double>, Double>? = null,
    percursos: List<PercursoMapa> = emptyList(),
    comPings: Boolean = true,
    percursoTeste: Boolean = false,
    calor: List<Pair<Double, Double>>? = null,
    restanteDe: Pair<Double, Double>? = null,
    legenda: Boolean = true,
    euComFoto: Boolean = true,
    /** false = sem o botão de tela cheia (mapas que já ocupam a tela). */
    maximizavel: Boolean = true,
    /** Título da tela cheia. */
    tituloTelaCheia: String = "Mapa",
    /** Botões próprios da tela cheia (à direita), ex.: enquadrar, mostrar pings, centralizar. */
    botoesTelaCheia: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
    /** Conteúdo embaixo na tela cheia (ex.: lista de paradas, legenda). */
    rodapeTelaCheia: (@Composable () -> Unit)? = null,
    /** Marcador destacado sem a câmera prender nele. */
    destacar: String? = null,
    /** O dedo mexeu no mapa (para soltar o "seguir"). */
    onArrastou: (() -> Unit)? = null,
    /** Toque numa parada (índice na lista [paradas]). */
    onParada: ((Int) -> Unit)? = null,
    /** Mostra aproximar/afastar/ir até mim também no mapa pequeno (mapas que já ocupam a tela). Na tela cheia, sempre. */
    controles: Boolean = false,
    /** Botão de estabelecimentos junto dos controles (desligue se a tela já tem o dela). */
    controlesLocais: Boolean = true,
    /** Onde fica o botão de tela cheia no mapa pequeno. */
    botaoTelaCheiaEm: androidx.compose.ui.Alignment = androidx.compose.ui.Alignment.TopStart,
) {
    var cheio by remember { mutableStateOf(false) }
    var enquadrarCheio by remember { mutableStateOf(0) }
    val base: @Composable (Modifier, Boolean) -> Unit = { mod, telaCheia ->
        MapaBase(paradas, rota, minhaPosicao, mod, marcadores, onToque, if (telaCheia) (enquadrar to enquadrarCheio) else enquadrar, onMarcador, seguir, trilhas,
            onCentro, inicio, zoomInicio, if (telaCheia) 0 else margemBaixo, irPara, focar, caminhoAuto, rotas, circulo, percursos, comPings, percursoTeste,
            calor, restanteDe, legenda, euComFoto, if (telaCheia) botoesTelaCheia else null, destacar, onArrastou, onParada, controles = telaCheia || controles, controlesLocais = controlesLocais)
    }
    androidx.compose.foundation.layout.Box(modifier) {
        if (!cheio) base(Modifier.matchParentSize(), false)
        else androidx.compose.foundation.layout.Box(Modifier.matchParentSize().background(Pw.cores.fundo))
        if (maximizavel) com.pwexpress.core.designsystem.PwBotaoIcone(
            PwIcones.Maximize2, "Tela cheia", { cheio = true },
            modifier = Modifier.align(botaoTelaCheiaEm).padding(10.dp), tamanho = 40.dp,
        )
    }
    if (cheio) androidx.compose.ui.window.Dialog(
        onDismissRequest = { cheio = false },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(Pw.cores.fundo)) {
            base(Modifier.fillMaxSize(), true)
            androidx.compose.foundation.layout.Row(
                Modifier.align(androidx.compose.ui.Alignment.TopStart).fillMaxWidth().statusBarsPadding().padding(12.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp),
            ) {
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.X, "Sair da tela cheia", { cheio = false }, tamanho = 44.dp)
                androidx.compose.material3.Text(
                    tituloTelaCheia, color = Pw.cores.texto, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 17.sp,
                    modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(99.dp)).background(Pw.cores.vidroForte).padding(horizontal = 14.dp, vertical = 8.dp),
                    maxLines = 1,
                )
                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Crosshair, "Enquadrar tudo", { enquadrarCheio++ }, tamanho = 44.dp)
            }
            rodapeTelaCheia?.let { r ->
                androidx.compose.foundation.layout.Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(12.dp)) { r() }
            }
        }
    }
}

/** Legenda das linhas: feito (dourado) e a fazer (tracejado). */
@Composable
private fun LegendaLinhas(comAFazer: Boolean, modifier: Modifier) {
    val c = Pw.cores
    val forma = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    androidx.compose.foundation.layout.Row(
        modifier.clip(forma).background(c.vidroForte).border(1.dp, c.borda, forma).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.foundation.layout.Box(Modifier.size(width = 20.dp, height = 4.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).background(c.ouro))
        androidx.compose.material3.Text("Feito", color = c.texto2, fontSize = 12.sp)
        if (comAFazer) {
            androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp)) {
                repeat(3) { androidx.compose.foundation.layout.Box(Modifier.size(width = 5.dp, height = 3.dp).background(c.texto)) }
            }
            androidx.compose.material3.Text("A fazer", color = c.texto2, fontSize = 12.sp)
        }
    }
}

/** Polígono de um círculo na superfície (64 pontos), para desenhar raios em metros. */
fun pontosDoCirculo(lat: Double, lng: Double, metros: Double, n: Int = 64): List<Pair<Double, Double>> {
    val r = metros / 6_371_000.0
    val la = Math.toRadians(lat); val lo = Math.toRadians(lng)
    return (0..n).map { i ->
        val b = 2 * Math.PI * i / n
        val la2 = Math.asin(Math.sin(la) * Math.cos(r) + Math.cos(la) * Math.sin(r) * Math.cos(b))
        val lo2 = lo + Math.atan2(Math.sin(b) * Math.sin(r) * Math.cos(la), Math.cos(r) - Math.sin(la) * Math.sin(la2))
        Math.toDegrees(la2) to Math.toDegrees(lo2)
    }
}

private fun criarCamadas(ctx: Context, s: Style, escuro: Boolean, ouro: String, ouroTexto: String, teste: Boolean, ruas: ModoRuas) {
    aplicarEstiloPw(ctx, s, escuro, ouroTexto, ruas)
    criarCamadaCalor(s, ouro)
    s.addSource(GeoJsonSource("circulo"))
    s.addLayer(org.maplibre.android.style.layers.FillLayer("circulo-fundo", "circulo").withProperties(
        PropertyFactory.fillColor(ouro), PropertyFactory.fillOpacity(0.12f),
    ))
    s.addLayer(LineLayer("circulo-borda", "circulo").withProperties(
        PropertyFactory.lineColor(ouro), PropertyFactory.lineWidth(2.5f), PropertyFactory.lineDasharray(arrayOf(2f, 1.5f)),
    ))
    s.addSource(GeoJsonSource("marcadores"))
    s.addSource(GeoJsonSource("trilhas"))
    s.addSource(GeoJsonSource("rota"))
    s.addSource(GeoJsonSource("plano"))
    s.addSource(GeoJsonSource("paradas"))
    s.addSource(GeoJsonSource("zonas"))
    s.addLayer(org.maplibre.android.style.layers.FillLayer("zonas-fundo", "zonas").withProperties(
        PropertyFactory.fillColor(Expression.toColor(Expression.get("cor"))), PropertyFactory.fillOpacity(0.10f),
    ))
    s.addLayer(LineLayer("zonas-borda", "zonas").withProperties(
        PropertyFactory.lineColor(Expression.toColor(Expression.get("cor"))), PropertyFactory.lineOpacity(0.7f),
        PropertyFactory.lineWidth(1.6f), PropertyFactory.lineDasharray(arrayOf(2f, 1.5f)),
    ))
    s.addSource(GeoJsonSource("eu"))

    s.addLayer(
        LineLayer("trilhas-contorno", "trilhas").withProperties(
            PropertyFactory.lineColor("#0A0A0A"), PropertyFactory.lineOpacity(0.5f), PropertyFactory.lineWidth(7f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    s.addLayer(
        LineLayer("trilhas", "trilhas").withProperties(
            PropertyFactory.lineColor(ouro), PropertyFactory.lineWidth(4f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    criarCamadasPercurso(s, teste)
    // rota planejada inteira, só de fundo (quando já há o "a fazer" a partir do motoboy, ou tudo foi feito)
    s.addLayer(
        LineLayer("plano", "plano").withProperties(
            PropertyFactory.lineColor(ouro), PropertyFactory.lineOpacity(0.35f), PropertyFactory.lineWidth(3f),
            PropertyFactory.lineDasharray(arrayOf(1.5f, 2f)), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    // a fazer: tracejado claro com contorno escuro (aparece no mapa claro e no noturno); o feito é a trilha dourada
    s.addLayer(
        LineLayer("rota-contorno", "rota").withProperties(
            PropertyFactory.lineColor("#0A0A0A"),
            PropertyFactory.lineOpacity(0.55f),
            PropertyFactory.lineWidth(8f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    s.addLayer(
        LineLayer("rota", "rota").withProperties(
            PropertyFactory.lineColor("#F5F5F4"),
            PropertyFactory.lineWidth(4f),
            PropertyFactory.lineDasharray(arrayOf(2f, 1.6f)),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    s.addLayer(
        SymbolLayer("paradas", "paradas").withProperties(
            PropertyFactory.iconImage(Expression.get("img")),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.symbolSortKey(Expression.get("ordem")),
        ),
    )
    s.addLayer(
        CircleLayer("eu-halo", "eu").withProperties(
            PropertyFactory.circleRadius(20f),
            PropertyFactory.circleColor(ouro),
            PropertyFactory.circleOpacity(0.22f),
        ),
    )
    s.addLayer(
        CircleLayer("eu", "eu").withProperties(
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleColor("#F3D77E"),
            PropertyFactory.circleStrokeColor("#0A0A0A"),
            PropertyFactory.circleStrokeWidth(3f),
        ),
    )
    s.addLayer(
        CircleLayer("marcadores-ponto", "marcadores").withProperties(
            PropertyFactory.circleRadius(Expression.get("raio")),
            PropertyFactory.circleColor(Expression.toColor(Expression.get("cor"))),
            PropertyFactory.circleStrokeColor(if (escuro) "#F5F2EA" else "#16130C"),
            PropertyFactory.circleStrokeWidth(Expression.get("borda")),
            PropertyFactory.circleOpacity(Expression.coalesce(Expression.get("opacidade"), Expression.literal(1f))),
            PropertyFactory.circleStrokeOpacity(Expression.coalesce(Expression.get("opacidade"), Expression.literal(1f))),
            PropertyFactory.circleSortKey(Expression.coalesce(Expression.get("ordem"), Expression.literal(1))),
        ).apply { setFilter(Expression.not(Expression.get("pino"))) },
    )
    s.addLayer(
        SymbolLayer("marcadores", "marcadores").withProperties(
            PropertyFactory.iconImage(Expression.get("icone")),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconOpacity(Expression.coalesce(Expression.get("opacidade"), Expression.literal(1f))),
            PropertyFactory.symbolSortKey(Expression.coalesce(Expression.get("ordem"), Expression.literal(1))),
        ),
    )
    s.addLayer(
        SymbolLayer("marcadores-rotulo", "marcadores").withProperties(
            PropertyFactory.textField(Expression.get("rotulo")),
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOffset(arrayOf(0f, 0.5f)),
            PropertyFactory.textColor(if (escuro) "#F5F2EA" else "#16130C"),
            PropertyFactory.textHaloColor(if (escuro) "#0B0B0D" else "#FFFFFF"),
            PropertyFactory.textHaloWidth(1.6f),
            // nomes que se encostam: um some em vez de virarem uma letra só em cima da outra
            PropertyFactory.textAllowOverlap(false),
            PropertyFactory.textOptional(true),
            PropertyFactory.textPadding(4f),
            PropertyFactory.textOpacity(Expression.coalesce(Expression.get("opacidade"), Expression.literal(1f))),
            PropertyFactory.symbolSortKey(Expression.coalesce(Expression.get("ordem"), Expression.literal(1))),
        ),
    )
}
