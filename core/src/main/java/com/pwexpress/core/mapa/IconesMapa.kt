package com.pwexpress.core.mapa

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.PathParser

/**
 * Pinos das paradas desenhados como imagem: um círculo com o ícone do que vai ali (documento, pacote,
 * sacola, remédio…), o número da parada e um selo "+N" quando há mais de um item no mesmo ponto.
 * Os ícones são os mesmos do Lucide usados no app e no site (traço 2 sobre 24×24).
 */
object IconesMapa {
    private val CAMINHOS: Map<String, List<String>> = mapOf(
        "file-text" to listOf("M6 22a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h8a2.4 2.4 0 0 1 1.704.706l3.588 3.588A2.4 2.4 0 0 1 20 8v12a2 2 0 0 1-2 2z", "M14 2v5a1 1 0 0 0 1 1h5", "M10 9H8", "M16 13H8", "M16 17H8"),
        "package" to listOf("M11 21.73a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73z", "M12 22V12", "M3.29 7 12 12l8.71-5", "m7.5 4.27 9 5.15"),
        "shopping-bag" to listOf("M16 10a4 4 0 0 1-8 0", "M3.103 6.034h17.794", "M3.4 5.467a2 2 0 0 0-.4 1.2V20a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6.667a2 2 0 0 0-.4-1.2l-2-2.667A2 2 0 0 0 17 2H7a2 2 0 0 0-1.6.8z"),
        "pill" to listOf("m10.5 20.5 10-10a4.95 4.95 0 1 0-7-7l-10 10a4.95 4.95 0 1 0 7 7Z", "m8.5 8.5 7 7"),
        "smartphone" to listOf("M7 2h10a2 2 0 0 1 2 2v16a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2z", "M12 18h.01"),
        "utensils" to listOf("M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2", "M7 2v20", "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3Zm0 0v7"),
        "flame" to listOf("M12 3q1 4 4 6.5t3 5.5a1 1 0 0 1-14 0 5 5 0 0 1 1-3 1 1 0 0 0 5 0c0-2-1.5-3-1.5-5q0-2 2.5-4"),
        "store" to listOf("M15 21v-5a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v5", "M17.774 10.31a1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.451 0 1.12 1.12 0 0 0-1.548 0 2.5 2.5 0 0 1-3.452 0 1.12 1.12 0 0 0-1.549 0 2.5 2.5 0 0 1-3.77-3.248l2.889-4.184A2 2 0 0 1 7 2h10a2 2 0 0 1 1.653.873l2.895 4.192a2.5 2.5 0 0 1-3.774 3.244", "M4 10.95V19a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8.05"),
        "house" to listOf("M15 21v-8a1 1 0 0 0-1-1h-4a1 1 0 0 0-1 1v8", "M3 10a2 2 0 0 1 .709-1.528l7-6a2 2 0 0 1 2.582 0l7 6A2 2 0 0 1 21 10v9a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"),
        "building-2" to listOf("M10 12h4", "M10 8h4", "M14 21v-3a2 2 0 0 0-4 0v3", "M6 10H4a2 2 0 0 0-2 2v7a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2h-2", "M6 21V5a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v16"),
        "key" to listOf("m15.5 7.5 2.3 2.3a1 1 0 0 0 1.4 0l2.1-2.1a1 1 0 0 0 0-1.4L19 4", "m21 2-9.6 9.6", "M2 15.5a5.5 5.5 0 1 0 11 0a5.5 5.5 0 1 0 -11 0"),
        "flag" to listOf("M4 22V4a1 1 0 0 1 .4-.8A6 6 0 0 1 8 2c3 0 5 2 7.333 2q2 0 3.067-.8A1 1 0 0 1 20 4v10a1 1 0 0 1-.4.8A6 6 0 0 1 16 16c-3 0-5-2-8-2a6 6 0 0 0-4 1.528"),
        "map-pin" to listOf("M20 10c0 4.993-5.539 10.193-7.399 11.799a1 1 0 0 1-1.202 0C9.539 20.193 4 14.993 4 10a8 8 0 0 1 16 0", "M9 10a3 3 0 1 0 6 0a3 3 0 1 0 -6 0"),
        "boxes" to listOf("M2.97 12.92A2 2 0 0 0 2 14.63v3.24a2 2 0 0 0 .97 1.71l3 1.8a2 2 0 0 0 2.06 0L12 19v-5.5l-5-3-4.03 2.42Z", "M12 13.5V19l3.97 2.38a2 2 0 0 0 2.06 0l3-1.8a2 2 0 0 0 .97-1.71v-3.24a2 2 0 0 0-.97-1.71L17 10.5l-5 3Z", "M7.97 4.42A2 2 0 0 0 7 6.13v4.37l5 3 5-3V6.13a2 2 0 0 0-.97-1.71l-3-1.8a2 2 0 0 0-2.06 0l-3 1.8Z"),
        "check" to listOf("M20 6 9 17l-5-5"),
    )

    /** Nome de ícone guardado (Lucide) → um dos desenhos acima (o resto vira pacote). */
    fun normalizar(nome: String?): String = when (nome) {
        null, "" -> "package"
        "file-text", "file-box", "newspaper", "mail", "folder", "documento" -> "file-text"
        "shopping-bag", "shopping-cart", "sacola" -> "shopping-bag"
        "pill", "heart", "syringe", "stethoscope", "briefcase-medical", "remedio" -> "pill"
        "smartphone", "tablet", "laptop", "tv", "cpu", "headphones", "watch", "camera" -> "smartphone"
        "utensils", "pizza", "coffee", "cake", "sandwich", "apple" -> "utensils"
        "flame", "flask-conical", "droplet", "fuel" -> "flame"
        "store", "warehouse", "factory" -> "store"
        "house", "home" -> "house"
        "building-2", "building", "hotel", "school", "hospital" -> "building-2"
        "key", "key-round" -> "key"
        "boxes", "package-2", "gift", "box" -> "boxes"
        else -> if (CAMINHOS.containsKey(nome)) nome else "package"
    }

    private val cacheCaminhos = HashMap<String, List<Path>>()
    private fun caminhos(nome: String): List<Path> = cacheCaminhos.getOrPut(nome) {
        CAMINHOS[nome].orEmpty().mapNotNull { runCatching { PathParser.createPathFromPathData(it) }.getOrNull() }
    }

    /** Chave da imagem (o mapa guarda cada combinação uma vez só). */
    fun chave(coleta: Boolean, atual: Boolean, concluida: Boolean, icone: String?, numero: Int?, mais: Int, escuro: Boolean, ouro: Int) =
        "parada_${if (coleta) "c" else "e"}${if (atual) "a" else ""}${if (concluida) "f" else ""}_${normalizar(icone)}_${numero ?: 0}_${mais}_${if (escuro) "n" else "d"}_${ouro}"

    fun desenhar(ctx: Context, coleta: Boolean, atual: Boolean, concluida: Boolean, icone: String?, numero: Int?, mais: Int, escuro: Boolean, ouro: Int): Bitmap {
        val d = ctx.resources.displayMetrics.density
        val diam = (if (atual) 38 else 32) * d
        val margem = 8 * d
        val w = (diam + margem * 2).toInt()
        val h = (diam + margem * 2 + 6 * d).toInt()
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val cx = w / 2f; val cy = margem + diam / 2f
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val escuroFundo = 0xFF111114.toInt()
        // contraste sempre certo: destaque escuro (tema preto no branco) ou claro (branco no preto)
        val lum = android.graphics.Color.luminance(ouro)
        val visivel = if (lum < 0.3f) 0xFFFFFFFF.toInt() else ouro // destaque que aparece sobre o escuro
        val fundo = if (coleta) ouro else escuroFundo
        val traco = if (coleta) (if (lum > 0.45f) 0xFF111114.toInt() else 0xFFFFFFFF.toInt()) else (if (lum < 0.3f) 0xFFFFFFFF.toInt() else ouro)
        val alfa = if (concluida) 120 else 255
        // sombra + ponta (o ponto exato fica embaixo)
        p.color = 0x55000000; cv.drawCircle(cx, cy + 2 * d, diam / 2f, p)
        p.color = fundo; p.alpha = alfa
        cv.drawPath(Path().apply { moveTo(cx - 6 * d, cy + diam / 2f - 5 * d); lineTo(cx, cy + diam / 2f + 6 * d); lineTo(cx + 6 * d, cy + diam / 2f - 5 * d); close() }, p)
        // anel branco + círculo
        p.color = 0xFFFFFFFF.toInt(); p.alpha = alfa; cv.drawCircle(cx, cy, diam / 2f, p)
        p.color = fundo; p.alpha = alfa; cv.drawCircle(cx, cy, diam / 2f - 2.6f * d, p)
        if (atual) { p.style = Paint.Style.STROKE; p.strokeWidth = 2.4f * d; p.color = ouro; cv.drawCircle(cx, cy, diam / 2f + 3.5f * d, p); p.style = Paint.Style.FILL }
        // ícone (feita = check)
        val nome = if (concluida) "check" else normalizar(icone)
        val lado = diam * 0.52f
        val m = Matrix().apply { setScale(lado / 24f, lado / 24f); postTranslate(cx - lado / 2f, cy - lado / 2f) }
        val pi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeWidth = 2.1f * (lado / 24f) * 1.05f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
            color = traco; alpha = alfa
        }
        caminhos(nome).forEach { base -> cv.drawPath(Path(base).apply { transform(m) }, pi) }
        // número da parada (canto de cima, à esquerda)
        if (numero != null && numero > 0) selo(cv, cx - diam / 2f + 3 * d, cy - diam / 2f + 3 * d, numero.toString(), if (coleta) escuroFundo else visivel, if (coleta) visivel else escuroFundo, d)
        // + itens (canto de cima, à direita)
        if (mais > 0) selo(cv, cx + diam / 2f - 3 * d, cy - diam / 2f + 3 * d, "+$mais", 0xFFFFFFFF.toInt(), escuroFundo, d)
        return bmp
    }

    private fun selo(cv: Canvas, x: Float, y: Float, texto: String, fundo: Int, letra: Int, d: Float) {
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 9.5f * d; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER }
        val larg = maxOf(15 * d, p.measureText(texto) + 8 * d)
        val r = RectF(x - larg / 2f, y - 7.5f * d, x + larg / 2f, y + 7.5f * d)
        p.color = 0xFF111114.toInt(); cv.drawRoundRect(RectF(r.left - 1.5f * d, r.top - 1.5f * d, r.right + 1.5f * d, r.bottom + 1.5f * d), 9 * d, 9 * d, p)
        p.color = fundo; cv.drawRoundRect(r, 8 * d, 8 * d, p)
        p.color = letra; cv.drawText(texto, x, y - (p.descent() + p.ascent()) / 2f, p)
    }
}
