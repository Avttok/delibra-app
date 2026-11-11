package com.pwexpress.core.notificacoes

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pwexpress.core.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Identidade das notificações do app: canais organizados por assunto, logo redonda,
 * dourado da marca (violeta no modo teste) e atalhos para abrir o app.
 */
object PwNotificacoes {
    const val GRUPO_CORRIDAS = "pwex_grupo_corridas"
    const val GRUPO_SISTEMA = "pwex_grupo_sistema"

    /** Corrida em andamento (notificação ao vivo, sem som). */
    const val CANAL_CORRIDA = "pwex_corrida_atual"
    /** Corridas planejadas (reserva, lembrete, mudanças). */
    const val CANAL_PLANEJADAS = "pwex_planejadas"
    /** Cancelamentos e avisos importantes. */
    const val CANAL_AVISOS = "pwex_avisos"
    /** Informativos que não tocam. */
    const val CANAL_SILENCIOSO = "pwex_silencioso"
    /** Download da atualização do app. */
    const val CANAL_ATUALIZACAO = "pwex_atualizacao"
    /** Mensagens do chat da equipe. */
    const val CANAL_CHAT = "pwex_chat"

    /** Degradês dos cartões (mesmos dos destaques do app). */
    val GRAD_OURO = intArrayOf(0xFF8A6A12.toInt(), 0xFFD4AF37.toInt(), 0xFFF5DE8C.toInt())
    val GRAD_VERMELHO = intArrayOf(0xFF5E0E1A.toInt(), 0xFFD23A4E.toInt(), 0xFFFF8A80.toInt())
    val GRAD_VERDE = intArrayOf(0xFF0A5C38.toInt(), 0xFF1FB86E.toInt(), 0xFF7BE3A8.toInt())
    val GRAD_AZUL = intArrayOf(0xFF142B6B.toInt(), 0xFF3B82F6.toInt(), 0xFF93C5FD.toInt())
    val GRAD_VIOLETA = intArrayOf(0xFF3B1577.toInt(), 0xFF7C3AED.toInt(), 0xFFC4B5FD.toInt())

    /** Ícones (Lucide, 24×24) que entram no cartão de cada tipo de aviso. */
    val ICONES: Map<String, List<String>> = mapOf(
        "rota" to listOf("M6 19a3 3 0 1 0 0-6 3 3 0 0 0 0 6Z", "M9 19h8.5a3.5 3.5 0 0 0 0-7h-11a3.5 3.5 0 0 1 0-7H15", "M18 5a3 3 0 1 0 0 6 3 3 0 0 0 0-6Z"),
        "calendario" to listOf("M8 2v4", "M16 2v4", "M3 10h18", "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"),
        "relogio" to listOf("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z", "M12 6v6l4 2"),
        "alerta" to listOf("m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3", "M12 9v4", "M12 17h.01"),
        "x" to listOf("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z", "m15 9-6 6", "m9 9 6 6"),
        "check" to listOf("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z", "m9 12 2 2 4-4"),
        "info" to listOf("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z", "M12 16v-4", "M12 8h.01"),
        "sino" to listOf("M10.27 21a2 2 0 0 0 3.46 0", "M3.26 15.33A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.67C19.41 13.96 18 12.5 18 8A6 6 0 0 0 6 8c0 4.5-1.41 5.96-2.74 7.33"),
        "pessoa" to listOf("M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2", "M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8Z"),
        "mais" to listOf("M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20Z", "M8 12h8", "M12 8v8"),
    )

    const val DOURADO = 0xFFD4AF37.toInt()
    const val VIOLETA = 0xFFA78BFA.toInt()

    /** O app avisa aqui quando o modo teste muda (cor das notificações). */
    @Volatile var modoTeste: Boolean = false
    val cor: Int get() = if (modoTeste) VIOLETA else DOURADO

    fun criarCanais(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannelGroup(NotificationChannelGroup(GRUPO_CORRIDAS, "Corridas"))
        nm.createNotificationChannelGroup(NotificationChannelGroup(GRUPO_SISTEMA, "Sistema"))
        fun canal(id: String, nome: String, imp: Int, grupo: String, desc: String, semSom: Boolean = false) =
            NotificationChannel(id, nome, imp).apply {
                description = desc
                this.group = grupo
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                if (semSom) { setSound(null, null); enableVibration(false) }
            }
        nm.createNotificationChannels(listOf(
            canal(CANAL_CORRIDA, "Corrida em andamento", NotificationManager.IMPORTANCE_DEFAULT, GRUPO_CORRIDAS,
                "Progresso da corrida atual (etapas, endereço e distância).", semSom = true),
            canal(CANAL_PLANEJADAS, "Corridas planejadas", NotificationManager.IMPORTANCE_DEFAULT, GRUPO_CORRIDAS,
                "Novas corridas com hora marcada, reservas e lembretes."),
            canal(CANAL_AVISOS, "Avisos de corrida", NotificationManager.IMPORTANCE_HIGH, GRUPO_CORRIDAS,
                "Cancelamentos e mudanças importantes."),
            canal(CANAL_SILENCIOSO, "Avisos silenciosos", NotificationManager.IMPORTANCE_LOW, GRUPO_CORRIDAS,
                "Informativos que não tocam nem acendem a tela.", semSom = true),
            canal(CANAL_ATUALIZACAO, "Atualização do app", NotificationManager.IMPORTANCE_LOW, GRUPO_SISTEMA,
                "Progresso do download de uma versão nova.", semSom = true),
            canal(CANAL_CHAT, "Conversas", NotificationManager.IMPORTANCE_HIGH, GRUPO_SISTEMA,
                "Mensagens do chat da equipe (geral, diretas e grupos)."),
        ))
    }

    private val logos = mutableMapOf<Boolean, Bitmap>()

    /** Logo (PW + entregador) num círculo escuro: ícone grande das notificações. Violeta no modo teste. */
    fun logoRedonda(ctx: Context): Bitmap? {
        val teste = modoTeste
        // empresa com ícone próprio: as notificações usam o dela (no modo teste continua o violeta)
        if (!teste) com.pwexpress.core.marca.MarcaEmpresa.iconeRedondo()?.let { return it }
        logos[teste]?.let { return it }
        val res = if (teste) R.drawable.pwex_marca_violeta else R.drawable.pwex_marca_ouro
        val fonte = runCatching { BitmapFactory.decodeResource(ctx.resources, res) }.getOrNull() ?: return null
        val lado = 192
        val saida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val tela = Canvas(saida)
        tela.drawCircle(lado / 2f, lado / 2f, lado / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0B0B0D.toInt() })
        val larg = lado * 0.74f
        val alt = larg * fonte.height / fonte.width
        val destino = android.graphics.RectF((lado - larg) / 2f, (lado - alt) / 2f, (lado + larg) / 2f, (lado + alt) / 2f)
        tela.drawBitmap(fonte, null, destino, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        logos[teste] = saida
        return saida
    }

    fun iconeLogo(ctx: Context): Icon? = logoRedonda(ctx)?.let { Icon.createWithBitmap(it) }

    /** Abre o app (tela principal). `extra` vai junto no intent (ex.: abrir a aba de planejadas). */
    fun abrirApp(ctx: Context, codigo: Int, extra: String? = null): PendingIntent? =
        ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (extra != null) it.putExtra("pwex_abrir", extra)
            PendingIntent.getActivity(ctx, codigo, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

    /** Base com a identidade do app. */
    fun base(ctx: Context, canal: String): NotificationCompat.Builder {
        criarCanais(ctx)
        return NotificationCompat.Builder(ctx, canal)
            .setSmallIcon(R.drawable.ic_pwex_notif)
            .setLargeIcon(logoRedonda(ctx))
            .setColor(cor)
            .setSubText(if (modoTeste) "Modo teste" else null)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    /** Cartão expandido simples: título, texto curto e texto longo ao expandir. */
    fun aviso(
        ctx: Context, id: Int, canal: String, titulo: String, texto: String,
        longo: String? = null, abrir: PendingIntent? = abrirApp(ctx, id), acoes: List<NotificationCompat.Action> = emptyList(),
        silencioso: Boolean = false, icone: String? = null, cores: IntArray? = null,
    ) {
        // todo aviso ganha o cartão colorido com ícone do tipo (a cor e o ícone vêm do canal se não vierem)
        val cor = cores ?: when (canal) { CANAL_PLANEJADAS -> GRAD_AZUL; CANAL_SILENCIOSO -> GRAD_VIOLETA; else -> GRAD_OURO }
        val ic = icone ?: when (canal) { CANAL_PLANEJADAS -> "calendario"; CANAL_SILENCIOSO -> "info"; else -> "sino" }
        val img = runCatching { banner(ctx, titulo, texto, cor, ic) }.getOrNull()
        val b = base(ctx, canal)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setColor(cor[1])
            .setStyle(if (img != null) NotificationCompat.BigPictureStyle().bigPicture(img).bigLargeIcon(null as Bitmap?).setSummaryText(longo ?: texto)
                else NotificationCompat.BigTextStyle().bigText(longo ?: texto))
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .setSilent(silencioso)
        acoes.forEach { b.addAction(it) }
        ctx.getSystemService(NotificationManager::class.java)?.notify(id, b.build())
    }

    /**
     * Faixa com degradê, a logo e o título (a imagem que aparece ao expandir a notificação).
     * Fica no lugar dos cartões cinza de antes: cada assunto tem sua cor.
     */
    fun banner(ctx: Context, titulo: String, texto: String?, cores: IntArray, icone: String? = null): Bitmap {
        val w = 1080; val h = 420
        val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val tela = Canvas(b)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.shader = android.graphics.LinearGradient(0f, h.toFloat(), w.toFloat(), 0f, cores, null, Shader.TileMode.CLAMP)
        tela.drawRoundRect(android.graphics.RectF(0f, 0f, w.toFloat(), h.toFloat()), 48f, 48f, p)
        // brilho e círculos decorativos
        p.shader = android.graphics.RadialGradient(w * 0.85f, h * 0.1f, h * 0.9f, intArrayOf(0x55FFFFFF, 0x00FFFFFF), null, Shader.TileMode.CLAMP)
        tela.drawCircle(w * 0.85f, h * 0.1f, h * 0.9f, p)
        p.shader = null; p.style = Paint.Style.STROKE; p.strokeWidth = 6f; p.color = 0x33FFFFFF
        tela.drawCircle(w * 0.92f, h * 0.95f, 170f, p); tela.drawCircle(w * 0.92f, h * 0.95f, 250f, p)
        p.style = Paint.Style.FILL
        // selo do tipo (rota, calendário, alerta…) no canto, como no aviso verde do app
        ICONES[icone]?.let { caminhos ->
            val cx = w - 110f; val cy = 110f; val raio = 64f
            p.color = 0x33FFFFFF; tela.drawCircle(cx, cy, raio, p)
            val tr = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 2.2f; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                color = if (android.graphics.Color.luminance(cores[1]) > 0.45f) 0xFF0A0A0A.toInt() else 0xFFFFFFFF.toInt() }
            tela.save(); tela.translate(cx - 36f, cy - 36f); tela.scale(3f, 3f)
            caminhos.forEach { d -> runCatching { androidx.core.graphics.PathParser.createPathFromPathData(d) }.getOrNull()?.let { tela.drawPath(it, tr) } }
            tela.restore()
        }
        // logo num círculo escuro
        logoRedonda(ctx)?.let { tela.drawBitmap(it, null, android.graphics.RectF(60f, (h - 180) / 2f, 240f, (h + 180) / 2f), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)) }
        val claro = android.graphics.Color.luminance(cores[1]) > 0.45f
        val tinta = if (claro) 0xFF0A0A0A.toInt() else 0xFFFFFFFF.toInt()
        val tp = android.text.TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = tinta; textSize = 62f; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD) }
        val larg = w - 300 - (if (icone != null) 200 else 60)
        val l1 = android.text.StaticLayout.Builder.obtain(titulo, 0, titulo.length, tp, larg).setMaxLines(2).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        val sub = texto.orEmpty()
        val tp2 = android.text.TextPaint(tp).apply { textSize = 40f; typeface = android.graphics.Typeface.DEFAULT; alpha = 215 }
        val l2 = android.text.StaticLayout.Builder.obtain(sub, 0, sub.length, tp2, larg).setMaxLines(3).setEllipsize(android.text.TextUtils.TruncateAt.END).build()
        val total = l1.height + (if (sub.isNotEmpty()) 18 + l2.height else 0)
        tela.save(); tela.translate(290f, (h - total) / 2f); l1.draw(tela)
        if (sub.isNotEmpty()) { tela.translate(0f, l1.height + 18f); l2.draw(tela) }
        tela.restore()
        return b
    }

    /** Notificação com a faixa em degradê (cancelada, rota estendida, planejada aberta…). */
    fun cartao(
        ctx: Context, id: Int, canal: String, titulo: String, texto: String, cores: IntArray,
        abrir: PendingIntent? = abrirApp(ctx, id), acoes: List<NotificationCompat.Action> = emptyList(), silencioso: Boolean = false,
        icone: String? = null,
    ) {
        val img = runCatching { banner(ctx, titulo, texto, cores, icone) }.getOrNull()
        val b = base(ctx, canal)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setColor(cores[1]).setColorized(false)
            .setAutoCancel(true)
            .setContentIntent(abrir)
            .setSilent(silencioso)
        if (img != null) b.setStyle(NotificationCompat.BigPictureStyle().bigPicture(img).bigLargeIcon(null as Bitmap?).setSummaryText(texto))
        else b.setStyle(NotificationCompat.BigTextStyle().bigText(texto))
        acoes.forEach { b.addAction(it) }
        ctx.getSystemService(NotificationManager::class.java)?.notify(id, b.build())
    }

    /** Mensagem do chat: estilo de conversa (remetente e prévia), tocando no canal de Conversas. */
    /** Quando cada conversa avisou pela última vez (push e checagem não avisam em dobro). */
    val chatAvisadoEm = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun mensagemChat(ctx: Context, conversa: String, titulo: String, texto: String) {
        chatAvisadoEm[conversa] = System.currentTimeMillis()
        val id = 5000 + (conversa.hashCode() and 0x3FF)
        val abrir = abrirApp(ctx, id, "chat:$conversa")
        val eu = androidx.core.app.Person.Builder().setName("Você").build()
        val de = androidx.core.app.Person.Builder().setName(titulo).setIcon(logoRedonda(ctx)?.let { androidx.core.graphics.drawable.IconCompat.createWithBitmap(it) }).build()
        val estilo = NotificationCompat.MessagingStyle(eu).addMessage(texto, System.currentTimeMillis(), de)
        val b = base(ctx, CANAL_CHAT).setStyle(estilo).setContentTitle(titulo).setContentText(texto)
            .setColor(0xFF7C3AED.toInt()).setAutoCancel(true).setContentIntent(abrir).setCategory(NotificationCompat.CATEGORY_MESSAGE)
        ctx.getSystemService(NotificationManager::class.java)?.notify(id, b.build())
    }

    fun cancelar(ctx: Context, id: Int) { ctx.getSystemService(NotificationManager::class.java)?.cancel(id) }

    val suportaAoVivo: Boolean get() = Build.VERSION.SDK_INT >= 36
}

/** Resumo da corrida atual para a notificação ao vivo (quem tem a corrida publica; o serviço de GPS mostra). */
data class ResumoCorrida(
    val numero: String,
    val titulo: String,          // "A caminho da coleta"
    val endereco: String,
    val etapaAtual: Int,         // 0-based: quantas etapas já foram concluídas
    val etapas: List<Boolean>,   // true = coleta, false = entrega (na ordem)
    val distanciaM: Int?,
    val valor: String?,
    val teste: Boolean,
)

object CorridaEmCurso {
    private val _resumo = MutableStateFlow<ResumoCorrida?>(null)
    val resumo: StateFlow<ResumoCorrida?> = _resumo.asStateFlow()
    fun publicar(r: ResumoCorrida?) { _resumo.value = r }

    /** Próxima curva da navegação no app ("Em 200 m, vire à direita na Rua X"), para a notificação. */
    private val _instrucao = MutableStateFlow<String?>(null)
    val instrucao: StateFlow<String?> = _instrucao.asStateFlow()
    fun instruir(t: String?) { if (_instrucao.value != t) _instrucao.value = t }
}
