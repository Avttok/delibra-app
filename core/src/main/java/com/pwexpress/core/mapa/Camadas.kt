package com.pwexpress.core.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.HeatmapLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.maps.Style
import org.maplibre.android.style.sources.GeoJsonOptions
import org.maplibre.android.style.sources.GeoJsonSource

// ====================================================================== percursos (registros)

/** Um ping do motoboy (tabela localizacoes). */
data class PingMapa(val lat: Double, val lng: Double, val status: String, val corridaId: String?, val quando: Long)

/**
 * Registro = um trecho contínuo: "procurando" (disponível até aceitar) ou "corrida" (a corrida inteira).
 * Começa de novo ao mudar de status, de corrida ou depois de 20 min sem ping (igual ao site).
 */
data class RegistroPercurso(
    val corrida: Boolean, val corridaId: String?, val pontos: List<PingMapa>,
    /** Caminho pelas ruas (lat, lng), quando o servidor conseguiu casar os pings com o mapa. */
    val caminho: List<Pair<Double, Double>>? = null,
) {
    val inicio get() = pontos.first().quando
    val fim get() = pontos.last().quando
    /** Linha desenhada: pelas ruas, ou os pings. */
    val linha: List<Pair<Double, Double>> get() = caminho ?: pontos.map { it.lat to it.lng }
    /** Distância percorrida (m), pelo caminho das ruas quando houver. */
    val metros: Double get() = linha.zipWithNext { a, b -> distanciaM(a.first, a.second, b.first, b.second) }.sum()
}

private const val LACUNA_MS = 20 * 60_000L

fun separarRegistros(pings: List<PingMapa>): List<RegistroPercurso> {
    val out = mutableListOf<RegistroPercurso>()
    var atual: MutableList<PingMapa>? = null
    var tipoAtual = false; var corridaAtual: String? = null; var anterior = 0L
    for (p in pings.sortedBy { it.quando }) {
        if (p.status == "offline" || !coordenadaValida(p.lat, p.lng)) { if (p.status == "offline") atual = null; continue }
        val corrida = p.status == "em_corrida"
        val cid = if (corrida) p.corridaId else null
        if (atual == null || tipoAtual != corrida || corridaAtual != cid || p.quando - anterior > LACUNA_MS) {
            atual?.let { out += RegistroPercurso(tipoAtual, corridaAtual, it.toList()) }
            atual = mutableListOf(); tipoAtual = corrida; corridaAtual = cid
        }
        atual!!.add(p); anterior = p.quando
    }
    atual?.let { out += RegistroPercurso(tipoAtual, corridaAtual, it.toList()) }
    return out.filter { it.pontos.isNotEmpty() }
}

fun distanciaM(la1: Double, lo1: Double, la2: Double, lo2: Double): Double {
    val r = 6_371_000.0
    val dLa = Math.toRadians(la2 - la1); val dLo = Math.toRadians(lo2 - lo1)
    val a = Math.sin(dLa / 2).let { it * it } + Math.cos(Math.toRadians(la1)) * Math.cos(Math.toRadians(la2)) * Math.sin(dLo / 2).let { it * it }
    return 2 * r * Math.asin(Math.sqrt(a))
}

/** Percurso desenhado: pontos (lat, lng) do mais antigo ao mais recente. */
data class PercursoMapa(
    val pontos: List<Pair<Double, Double>>, val destaque: Boolean = false, val apagado: Boolean = false,
    /** Linha pelas ruas (se nula, liga os pings). Os pings continuam nos [pontos]. */
    val linha: List<Pair<Double, Double>>? = null,
) {
    companion object {
        fun de(r: RegistroPercurso, destaque: Boolean = false, apagado: Boolean = false) =
            PercursoMapa(r.pontos.map { it.lat to it.lng }, destaque, apagado, r.caminho)
    }
}

/** Do início ao mais recente. Normal: vermelho → amarelo → verde. Teste: cinza → roxo → azul. */
fun coresPercurso(teste: Boolean): List<Int> =
    (if (teste) listOf("#8A8A93", "#9333EA", "#3B82F6") else listOf("#EF4444", "#FACC15", "#22C55E")).map { android.graphics.Color.parseColor(it) }

internal fun misturarCor(cores: List<Int>, t: Double): String {
    val n = cores.size - 1
    val i = minOf(n - 1, (t * n).toInt()).coerceAtLeast(0)
    val f = t * n - i
    val a = cores[i]; val b = cores[i + 1]
    fun canal(sh: Int) = ((a shr sh and 0xFF) + (((b shr sh and 0xFF) - (a shr sh and 0xFF)) * f)).toInt().coerceIn(0, 255)
    return String.format("#%02X%02X%02X", canal(16), canal(8), canal(0))
}

internal fun criarCamadasPercurso(s: Style, teste: Boolean) {
    val cores = coresPercurso(teste)
    s.addSource(GeoJsonSource("percursos", GeoJsonOptions().withLineMetrics(true)))
    s.addSource(GeoJsonSource("pings"))
    s.addLayer(LineLayer("percursos-contorno", "percursos").withProperties(
        PropertyFactory.lineColor("#0A0A0A"), PropertyFactory.lineOpacity(0.45f),
        PropertyFactory.lineWidth(Expression.switchCase(Expression.get("destaque"), Expression.literal(8f), Expression.literal(6f))),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    ))
    s.addLayer(LineLayer("percursos", "percursos").withProperties(
        PropertyFactory.lineWidth(Expression.switchCase(Expression.get("destaque"), Expression.literal(5f), Expression.literal(3.5f))),
        PropertyFactory.lineOpacity(Expression.switchCase(Expression.get("apagado"), Expression.literal(0.35f), Expression.literal(1f))),
        PropertyFactory.lineGradient(Expression.interpolate(Expression.linear(), Expression.lineProgress(),
            *cores.mapIndexed { i, cor -> Expression.stop(i.toFloat() / (cores.size - 1), Expression.color(cor)) }.toTypedArray())),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND), PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    ))
    s.addLayer(CircleLayer("pings", "pings").withProperties(
        PropertyFactory.circleRadius(Expression.switchCase(Expression.get("ponta"), Expression.literal(6f), Expression.literal(3.4f))),
        PropertyFactory.circleColor(Expression.toColor(Expression.get("cor"))),
        PropertyFactory.circleStrokeColor("#0A0A0A"), PropertyFactory.circleStrokeWidth(1f),
        PropertyFactory.circleOpacity(Expression.switchCase(Expression.get("apagado"), Expression.literal(0.35f), Expression.literal(1f))),
    ))
}

internal fun criarCamadaCalor(s: Style, cor: String) {
    s.addSource(GeoJsonSource("calor"))
    s.addLayer(HeatmapLayer("calor", "calor").apply {
        maxZoom = 17f
        setProperties(
            PropertyFactory.heatmapRadius(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(8, 12), Expression.stop(15, 28))),
            PropertyFactory.heatmapIntensity(Expression.interpolate(Expression.linear(), Expression.zoom(), Expression.stop(8, 0.8), Expression.stop(15, 1.6))),
            PropertyFactory.heatmapOpacity(0.85f),
            PropertyFactory.heatmapColor(Expression.interpolate(Expression.linear(), Expression.heatmapDensity(),
                Expression.stop(0, Expression.rgba(0, 0, 0, 0)), Expression.stop(0.2, Expression.color(0xFF3B82F6.toInt())),
                Expression.stop(0.45, Expression.color(0xFF22C55E.toInt())), Expression.stop(0.7, Expression.color(0xFFFACC15.toInt())),
                Expression.stop(1, Expression.color(android.graphics.Color.parseColor(cor))))),
        )
    })
}

// ====================================================================== estabelecimentos e rótulos

/** Categoria do estabelecimento (classe do OpenMapTiles) → ícone. O resto usa o padrão. (igual ao site) */
val ICONE_POI = mapOf(
    "grocery" to "mercado", "supermarket" to "mercado", "convenience" to "mercado", "alcohol_shop" to "mercado",
    "hospital" to "hospital", "doctor" to "hospital", "doctors" to "hospital", "dentist" to "hospital", "clinic" to "hospital",
    "pharmacy" to "farmacia", "chemist" to "farmacia",
    "park" to "praca", "garden" to "praca", "playground" to "praca", "pitch" to "praca",
    "school" to "escola", "college" to "escola", "kindergarten" to "escola", "university" to "escola",
    "restaurant" to "restaurante", "fast_food" to "restaurante", "food_court" to "restaurante", "bar" to "restaurante", "pub" to "restaurante",
    "cafe" to "cafe", "bakery" to "cafe", "ice_cream" to "cafe",
    "fuel" to "posto", "bank" to "banco", "atm" to "banco", "place_of_worship" to "igreja", "lodging" to "hotel",
    "police" to "policia", "post" to "correio", "car" to "carro", "car_repair" to "carro",
)

private fun carregarIcones(ctx: Context, s: Style) {
    val res = ctx.resources
    (ICONE_POI.values.toSet() + "padrao").forEach { n ->
        val id = res.getIdentifier("poi_$n", "drawable", ctx.packageName)
        if (id != 0) BitmapFactory.decodeResource(res, id)?.let { s.addImage("poi_$n", it, true) }
    }
}

/** Estabelecimentos com ícone por tipo (na cor do tema) + rótulos coloridos (cidade, bairro, vila, rua). */
/** Classes/subclasses do OpenMapTiles que não entram no mapa (transporte: só poluem com nomes de parada/rua). */
internal val POI_FORA = listOf("bus", "bus_stop", "bus_station", "railway", "platform", "stop_position", "tram_stop", "subway", "subway_entrance",
    "halt", "station", "aerialway", "ferry_terminal", "taxi", "bicycle_rental", "parking", "parking_entrance", "bicycle_parking", "motorcycle_parking")

internal fun aplicarEstiloPw(ctx: Context, s: Style, escuro: Boolean, corTema: String, modo: ModoRuas) {
    runCatching { carregarIcones(ctx, s) }
    val halo = if (escuro) "#0B0B0D" else "#FFFFFF"
    if (s.getSource("openmaptiles") != null) {
        val porClasse = ICONE_POI.entries.map { (k, v) -> Expression.stop(k, "poi_$v") }.toTypedArray()
        val icone = Expression.match(
            Expression.coalesce(Expression.get("subclass"), Expression.get("class")),
            Expression.match(Expression.get("class"), Expression.literal("poi_padrao"), *porClasse),
            *porClasse,
        )
        s.addLayer(SymbolLayer("pwex-poi", "openmaptiles").apply {
            sourceLayer = "poi"
            minZoom = 15f
            // pontos de ônibus/trem têm o nome da rua ou da parada: poluíam o mapa nas cidades grandes
            val fora = POI_FORA.map { Expression.stop(it, false) }.toTypedArray()
            setFilter(Expression.all(Expression.has("name"),
                Expression.match(Expression.coalesce(Expression.get("class"), Expression.literal("")), Expression.literal(true), *fora),
                Expression.match(Expression.coalesce(Expression.get("subclass"), Expression.literal("")), Expression.literal(true), *fora)))
            setProperties(
                PropertyFactory.iconImage(icone), PropertyFactory.iconSize(0.45f), PropertyFactory.iconAllowOverlap(false),
                PropertyFactory.iconColor(corTema), PropertyFactory.iconHaloColor(halo), PropertyFactory.iconHaloWidth(2f),
                PropertyFactory.textField(Expression.coalesce(Expression.get("name:pt"), Expression.get("name"))),
                PropertyFactory.textFont(arrayOf("Noto Sans Regular")), PropertyFactory.textSize(11.5f),
                PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP), PropertyFactory.textOffset(arrayOf(0f, 1.1f)),
                PropertyFactory.textMaxWidth(9f), PropertyFactory.textOptional(true),
                PropertyFactory.textColor(corTema), PropertyFactory.textHaloColor(halo), PropertyFactory.textHaloWidth(1.4f),
            )
        })
    }
    runCatching {
        val cidade = corTema
        val bairro = if (escuro) "#7DD3FC" else "#0369A1"
        val vila = if (escuro) "#C4B5FD" else "#6D28D9"
        val rua = if (escuro) "#E7E5E4" else "#44403C"
        s.layers.filterIsInstance<SymbolLayer>().filter { !it.id.startsWith("pwex") }.forEach { l ->
            when (l.sourceLayer) {
                "place" -> l.setProperties(
                    PropertyFactory.textColor(Expression.match(Expression.get("class"), Expression.color(android.graphics.Color.parseColor(rua)),
                        *listOf("city", "town", "state", "country").map { Expression.stop(it, Expression.color(android.graphics.Color.parseColor(cidade))) }.toTypedArray(),
                        *listOf("suburb", "neighbourhood", "quarter").map { Expression.stop(it, Expression.color(android.graphics.Color.parseColor(bairro))) }.toTypedArray(),
                        *listOf("village", "hamlet", "isolated_dwelling").map { Expression.stop(it, Expression.color(android.graphics.Color.parseColor(vila))) }.toTypedArray(),
                    )),
                    PropertyFactory.textHaloColor(halo),
                )
                "transportation_name" -> l.setProperties(PropertyFactory.textHaloColor(halo))
            }
        }
        aplicarModoRuas(s, modo, rua)
    }
}

/** Nomes de rua escritos na rua (como o estilo desenha) ou ocultos. Só mexe nas camadas de rua. */
internal fun aplicarModoRuas(s: Style, modo: ModoRuas, @Suppress("UNUSED_PARAMETER") corRua: String) {
    val v = if (modo == ModoRuas.OCULTA) Property.NONE else Property.VISIBLE
    s.layers.filterIsInstance<SymbolLayer>().filter { it.sourceLayer == "transportation_name" }.forEach { l ->
        runCatching { l.setProperties(PropertyFactory.visibility(v)) }
    }
}

// ====================================================================== pinos com foto

/**
 * Pino do motoboy: foto (ou inicial) num círculo com anel na cor do status, ponta embaixo e
 * selo de moto. Retorna um bitmap pronto para o estilo (âncora embaixo).
 */
internal fun desenharPino(ctx: Context, foto: Bitmap?, inicial: String, corAnel: Int, selecionado: Boolean, escuro: Boolean, teste: Boolean = false): Bitmap {
    val d = ctx.resources.displayMetrics.density
    val diam = (if (selecionado) 52 else 44) * d
    val anel = 3.2f * d
    val ponta = 9 * d
    val w = (diam + 8 * d).toInt(); val h = (diam + ponta + 6 * d).toInt()
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val cv = Canvas(bmp)
    val cx = w / 2f; val cy = diam / 2f + 3 * d
    val p = Paint(Paint.ANTI_ALIAS_FLAG)
    // sombra
    p.color = 0x55000000; cv.drawCircle(cx, cy + 2 * d, diam / 2f, p)
    // ponta
    p.color = corAnel
    cv.drawPath(Path().apply { moveTo(cx - 7 * d, cy + diam / 2f - 4 * d); lineTo(cx, cy + diam / 2f + ponta); lineTo(cx + 7 * d, cy + diam / 2f - 4 * d); close() }, p)
    // anel
    cv.drawCircle(cx, cy, diam / 2f, p)
    p.color = if (escuro) 0xFF0B0B0D.toInt() else 0xFFFFFFFF.toInt()
    cv.drawCircle(cx, cy, diam / 2f - anel, p)
    // foto ou inicial
    val r = diam / 2f - anel - 1.5f * d
    if (foto != null) {
        val camada = Bitmap.createBitmap((r * 2).toInt(), (r * 2).toInt(), Bitmap.Config.ARGB_8888)
        val c2 = Canvas(camada)
        val pp = Paint(Paint.ANTI_ALIAS_FLAG)
        c2.drawCircle(r, r, r, pp)
        pp.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        val lado = minOf(foto.width, foto.height)
        c2.drawBitmap(foto, Rect((foto.width - lado) / 2, (foto.height - lado) / 2, (foto.width + lado) / 2, (foto.height + lado) / 2), RectF(0f, 0f, r * 2, r * 2), pp)
        cv.drawBitmap(camada, cx - r, cy - r, null)
    } else {
        p.color = corAnel; p.alpha = 60; cv.drawCircle(cx, cy, r, p); p.alpha = 255
        p.color = if (escuro) 0xFFF5F2EA.toInt() else 0xFF16130C.toInt()
        p.textSize = r * 0.95f; p.typeface = Typeface.DEFAULT_BOLD; p.textAlign = Paint.Align.CENTER
        cv.drawText(inicial, cx, cy - (p.descent() + p.ascent()) / 2, p)
    }
    // selo de moto
    val sr = 9.5f * d
    val sx = cx + diam / 2f - sr * 0.7f; val sy = cy + diam / 2f - sr * 0.9f
    p.color = if (escuro) 0xFF0B0B0D.toInt() else 0xFFFFFFFF.toInt(); cv.drawCircle(sx, sy, sr + 1.5f * d, p)
    p.color = if (teste) 0xFFA78BFA.toInt() else corAnel; cv.drawCircle(sx, sy, sr, p)
    if (teste) {
        // frasco (modo teste) no lugar da moto
        val f = sr * 0.62f
        val pf = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0B0D.toInt(); style = Paint.Style.STROKE; strokeWidth = 1.6f * d; strokeJoin = Paint.Join.ROUND; strokeCap = Paint.Cap.ROUND }
        cv.drawPath(Path().apply {
            moveTo(sx - f * 0.32f, sy - f); lineTo(sx - f * 0.32f, sy - f * 0.25f); lineTo(sx - f * 0.9f, sy + f * 0.8f)
            lineTo(sx + f * 0.9f, sy + f * 0.8f); lineTo(sx + f * 0.32f, sy - f * 0.25f); lineTo(sx + f * 0.32f, sy - f)
        }, pf)
        cv.drawLine(sx - f * 0.5f, sy - f, sx + f * 0.5f, sy - f, pf)
        pf.style = Paint.Style.FILL
        cv.drawPath(Path().apply { moveTo(sx - f * 0.62f, sy + f * 0.3f); lineTo(sx - f * 0.9f, sy + f * 0.8f); lineTo(sx + f * 0.9f, sy + f * 0.8f); lineTo(sx + f * 0.62f, sy + f * 0.3f); close() }, pf)
        return bmp
    }
    val idMoto = ctx.resources.getIdentifier("poi_moto", "drawable", ctx.packageName)
    if (idMoto != 0) BitmapFactory.decodeResource(ctx.resources, idMoto)?.let { m ->
        val pm = Paint(Paint.ANTI_ALIAS_FLAG).apply { colorFilter = PorterDuffColorFilter(0xFF0B0B0D.toInt(), PorterDuff.Mode.SRC_IN) }
        val t = sr * 1.25f
        cv.drawBitmap(m, null, RectF(sx - t / 2, sy - t / 2, sx + t / 2, sy + t / 2), pm)
    }
    return bmp
}
