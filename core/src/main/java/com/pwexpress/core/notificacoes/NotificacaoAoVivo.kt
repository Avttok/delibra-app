package com.pwexpress.core.notificacoes

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.pwexpress.core.R

/**
 * Notificação da corrida em andamento. No Android 16+ usa o estilo de progresso
 * (barra por etapa: coleta em dourado, entregas em branco) e pede para ser
 * "promovida" (tela de bloqueio / Now Bar). Antes disso, barra de progresso comum.
 */
object NotificacaoAoVivo {

    fun montar(ctx: Context, r: ResumoCorrida, abrir: PendingIntent?, extra: String? = null): Notification {
        PwNotificacoes.modoTeste = r.teste
        if (Build.VERSION.SDK_INT >= 36) return montarProgresso(ctx, r, abrir, extra)
        val total = r.etapas.size.coerceAtLeast(1)
        val nav = CorridaEmCurso.instrucao.value
        val texto = listOfNotNull(r.endereco, r.distanciaM?.let { distancia(it) }).joinToString(" · ")
        return PwNotificacoes.base(ctx, PwNotificacoes.CANAL_CORRIDA)
            .setContentTitle(nav ?: "Corrida ${r.numero} · ${r.titulo}")
            .setContentText(texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(texto, r.valor?.let { "Você recebe $it" }, extra).joinToString("\n")))
            .setProgress(total, r.etapaAtual.coerceAtMost(total), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setContentIntent(abrir)
            .build()
    }

    @RequiresApi(36)
    private fun montarProgresso(ctx: Context, r: ResumoCorrida, abrir: PendingIntent?, extra: String?): Notification {
        val dourado = PwNotificacoes.cor
        val branco = 0xFFE9E4D8.toInt()
        val segmentos = r.etapas.map { coleta -> Notification.ProgressStyle.Segment(100).setColor(if (coleta) dourado else branco) }
        val pontos = r.etapas.indices.drop(1).map { i -> Notification.ProgressStyle.Point(i * 100).setColor(0xFF0A0A0A.toInt()) }
        val estilo = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressSegments(segmentos)
            .setProgressPoints(pontos)
            .setProgress((r.etapaAtual * 100 + 50).coerceAtMost(r.etapas.size * 100))
            .setProgressTrackerIcon(Icon.createWithResource(ctx, R.drawable.ic_pwex_notif))
        val texto = listOfNotNull(r.endereco, r.distanciaM?.let { distancia(it) }).joinToString(" · ")
        PwNotificacoes.criarCanais(ctx)
        val b = Notification.Builder(ctx, PwNotificacoes.CANAL_CORRIDA)
            .setSmallIcon(R.drawable.ic_pwex_notif)
            .setColor(dourado)
            .setContentTitle(CorridaEmCurso.instrucao.value ?: "Corrida ${r.numero} · ${r.titulo}")
            .setContentText(listOfNotNull(texto, extra).joinToString(" · "))
            .setStyle(estilo)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setContentIntent(abrir)
        PwNotificacoes.iconeLogo(ctx)?.let { b.setLargeIcon(it) }
        if (r.valor != null) b.setSubText(r.valor)
        // pede o destaque "ao vivo" (Android 16): tela de bloqueio, barra de status e Now Bar
        b.extras.putBoolean("android.requestPromotedOngoing", true)
        r.distanciaM?.let { b.setShortCriticalText(distanciaCurta(it)) }
        return b.build()
    }

    private fun distancia(m: Int) = if (m < 1000) "$m m" else String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f km", m / 1000.0)
    private fun distanciaCurta(m: Int) = if (m < 1000) "${m}m" else String.format(java.util.Locale.US, "%.0fkm", m / 1000.0)
}
