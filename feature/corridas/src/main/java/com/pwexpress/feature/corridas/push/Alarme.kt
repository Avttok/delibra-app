package com.pwexpress.feature.corridas.push

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.pwexpress.core.notificacoes.PwNotificacoes
import com.pwexpress.feature.corridas.data.Oferta
import com.pwexpress.feature.corridas.data.TipoParada
import com.pwexpress.feature.corridas.data.instante
import com.pwexpress.feature.corridas.R
import com.pwexpress.feature.corridas.ui.OfertaActivity

/** Notificações de corrida: a oferta (alta prioridade, som em loop, tela cheia) e avisos. */
object Alarme {
    // Mudou o som ou a vibração? Troque o id do canal: o Android não atualiza canais existentes.
    private const val CANAL_OFERTA = "pwex_ofertas_v1"
    private const val NOTIF_VOLTOU = 2003
    const val NOTIF_OFERTA = 2001
    private const val NOTIF_AVISO = 2002

    private fun criarCanais(ctx: Context) {
        PwNotificacoes.criarCanais(ctx)
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        val som = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${ctx.packageName}/${R.raw.pwex_oferta}")
        val audio = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        nm.createNotificationChannel(
            NotificationChannel(CANAL_OFERTA, "Corridas novas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Toca alto e abre em tela cheia quando chega uma corrida"
                group = PwNotificacoes.GRUPO_CORRIDAS
                setSound(som, audio)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600, 300, 900)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
        )
    }

    /** Aviso discreto: sem som, sem vibrar, sem acender a tela. */
    fun avisarSilencioso(ctx: Context, texto: String, titulo: String = com.pwexpress.core.marca.MarcaEmpresa.marca.value?.nomeExibido ?: "Delibra") {
        PwNotificacoes.aviso(ctx, NOTIF_VOLTOU, PwNotificacoes.CANAL_SILENCIOSO, titulo, texto, silencioso = true)
    }

    private fun km(m: Int?) = when {
        m == null -> null
        m < 1000 -> "$m m"
        else -> String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f km", m / 1000.0)
    }

    /**
     * Oferta de corrida no estilo "chamada recebida": valor, distância, contagem regressiva e
     * os botões Aceitar / Recusar (funcionam direto da notificação). Toca até responder.
     */
    fun mostrarOferta(ctx: Context, oferta: Oferta? = null, ofertaId: String? = null) {
        criarCanais(ctx)
        val id = oferta?.id ?: ofertaId
        val abrir = Intent(ctx, OfertaActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pi = PendingIntent.getActivity(ctx, 10, abrir, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val aceitar = PendingIntent.getActivity(
            ctx, 13,
            Intent(ctx, OfertaActivity::class.java).setAction(OfertaActivity.ACAO_ACEITAR).putExtra("oferta_id", id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val recusar = PendingIntent.getBroadcast(
            ctx, 14,
            Intent(ctx, OfertaReceiver::class.java).setAction(OfertaReceiver.ACAO_RECUSAR).putExtra("oferta_id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val c = oferta?.corrida
        PwNotificacoes.modoTeste = c?.teste ?: PwNotificacoes.modoTeste
        val valor = c?.valorMotoboy?.let { java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("pt-BR")).format(it) }
        val titulo = if (c != null) "Corrida ${c.numeroExibicao}${valor?.let { " · $it" } ?: ""}" else "Nova corrida!"
        val coleta = c?.paradas?.firstOrNull()
        val entregas = c?.paradas?.count { it.tipo == TipoParada.ENTREGA } ?: 0
        val texto = if (c != null) listOfNotNull(km(oferta.distanciaM)?.let { "$it até a coleta" }, km(c.distanciaM)?.let { "percurso $it" }).joinToString(" · ")
            else "Toque para ver e aceitar."
        val detalhes = if (c != null) listOfNotNull(
            coleta?.let { "Coleta: ${it.endereco}" },
            c.paradas.lastOrNull { it.tipo == TipoParada.ENTREGA }?.let { "Entrega: ${it.endereco}" + if (entregas > 1) " (+${entregas - 1})" else "" },
            c.empresaNome?.let { "Cliente: $it" },
        ).joinToString("\n") else null

        val pessoa = Person.Builder()
            .setName(titulo)
            .setIcon(PwNotificacoes.logoRedonda(ctx)?.let { IconCompat.createWithBitmap(it) })
            .setImportant(true)
            .build()
        val b = PwNotificacoes.base(ctx, CANAL_OFERTA)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pi, true)
            .setContentIntent(pi)
            .setOngoing(true)
            .setColorized(true)
            .setTimeoutAfter(((oferta?.prazoMillis ?: (System.currentTimeMillis() + 30_000)) - System.currentTimeMillis() + 2_000).coerceAtLeast(5_000))
        if (oferta != null) b.setWhen(oferta.prazoMillis).setShowWhen(true).setUsesChronometer(true).setChronometerCountDown(true)
        if (id != null) {
            b.setStyle(
                NotificationCompat.CallStyle.forIncomingCall(pessoa, recusar, aceitar)
                    .setAnswerButtonColorHint(0xFF22C55E.toInt())
                    .setDeclineButtonColorHint(0xFFEF4444.toInt())
                    .setVerificationText(detalhes ?: texto)
            )
        } else {
            b.setStyle(NotificationCompat.BigTextStyle().bigText(detalhes ?: texto))
        }
        val n = b.build()
        n.flags = n.flags or Notification.FLAG_INSISTENT // repete o som até responder
        ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_OFERTA, n)
    }

    fun pararOferta(ctx: Context) {
        ctx.getSystemService(NotificationManager::class.java)?.cancel(NOTIF_OFERTA)
    }

    fun avisar(ctx: Context, titulo: String, texto: String, longo: String? = null) {
        PwNotificacoes.aviso(ctx, NOTIF_AVISO, PwNotificacoes.CANAL_AVISOS, titulo, texto, longo)
    }

    /** Notificações das corridas planejadas (vêm do servidor com o tipo e o horário). */
    /** Rota nova no quadro: quem pegar primeiro leva (express) ou reserva (planejada). */
    fun rotaNova(ctx: Context, corridaId: String?, planejada: Boolean, entregas: Int, sugerida: Boolean, agendadaPara: String?) {
        val quando = agendadaPara?.takeIf { it.isNotBlank() }?.let { instante(it) }?.let { formatarQuando(it) }
        val abrir = PwNotificacoes.abrirApp(ctx, 3600 + (corridaId?.hashCode() ?: 0) % 300, "rotas")
        val ent = if (entregas == 1) "1 entrega" else "$entregas entregas"
        val titulo = when {
            sugerida -> if (planejada) "Rota planejada perto de você" else "Rota nova: você é o mais perto"
            planejada -> "Nova rota planejada"
            else -> "Nova rota disponível"
        }
        val texto = if (planejada) "${quando?.let { "Para $it · " } ?: ""}$ent · reserve antes dos outros" else "$ent · pegue antes dos outros"
        val ver = NotificationCompat.Action.Builder(null, if (planejada) "Ver e reservar" else "Ver e pegar", abrir).build()
        PwNotificacoes.aviso(ctx, 3600 + ((corridaId?.hashCode() ?: 0) and 0x1FF),
            if (planejada) PwNotificacoes.CANAL_PLANEJADAS else PwNotificacoes.CANAL_AVISOS, titulo, texto, null, abrir, listOf(ver))
    }

    fun planejada(ctx: Context, tipo: String, corridaId: String?, agendadaPara: String?) {
        val quando = agendadaPara?.let { instante(it) }?.let { formatarQuando(it) }
        val abrir = PwNotificacoes.abrirApp(ctx, 3100 + (corridaId?.hashCode() ?: 0) % 500, "planejadas")
        val (titulo, texto, longo) = when (tipo) {
            "planejada_nova" -> Triple("Nova corrida planejada", quando?.let { "Para $it · reserve antes dos outros" } ?: "Reserve antes dos outros",
                "Uma corrida com hora marcada está livre${quando?.let { " para $it" } ?: ""}. Abra o app para ver e reservar.")
            "planejada_voltou" -> Triple("Corrida planejada livre de novo", quando?.let { "Para $it · alguém desistiu" } ?: "Alguém desistiu",
                "Quem tinha reservado desistiu. Ela está livre de novo${quando?.let { " para $it" } ?: ""}.")
            "planejada_reservada" -> Triple("Corrida reservada para você", quando?.let { "Começa $it" } ?: "Veja os detalhes no app",
                "O gestor reservou uma corrida planejada para você${quando?.let { " ($it)" } ?: ""}. Você pode iniciar pouco antes do horário.")
            "planejada_liberada" -> Triple("Reserva removida", "O gestor tirou uma corrida planejada de você", null)
            "planejada_reagendada" -> Triple("Horário mudou", quando?.let { "Sua corrida planejada agora é $it" } ?: "Sua corrida planejada mudou de horário", null)
            "planejada_lembrete" -> Triple("Sua corrida começa em breve", quando?.let { "Às ${it.substringAfter(" às ")} · prepare-se" } ?: "Prepare-se",
                "Lembrete da sua corrida planejada${quando?.let { " ($it)" } ?: ""}. Toque em Iniciar quando estiver pronto.")
            "planejada_atrasada" -> Triple("Corrida planejada atrasada", "Já passou do horário e ela ainda não foi iniciada", "Inicie agora ou fale com o gestor.")
            else -> return
        }
        val ver = NotificationCompat.Action.Builder(null, if (tipo == "planejada_lembrete" || tipo == "planejada_atrasada") "Abrir e iniciar" else "Ver planejadas", abrir).build()
        PwNotificacoes.aviso(
            ctx, 3000 + ((corridaId?.hashCode() ?: tipo.hashCode()) and 0x3FF),
            if (tipo == "planejada_atrasada") PwNotificacoes.CANAL_AVISOS else PwNotificacoes.CANAL_PLANEJADAS,
            titulo, texto, longo, abrir, listOf(ver),
        )
    }

    private fun formatarQuando(ms: Long): String {
        val z = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault())
        val hoje = java.time.LocalDate.now()
        val dia = when (z.toLocalDate()) {
            hoje -> "hoje"
            hoje.plusDays(1) -> "amanhã"
            else -> z.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd/MM", java.util.Locale.forLanguageTag("pt-BR")))
        }
        return "$dia às ${z.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}"
    }

    /** Toca o som de oferta uma vez, sem abrir nada. */
    fun testarSom(ctx: Context) {
        criarCanais(ctx)
        val n = NotificationCompat.Builder(ctx, CANAL_OFERTA)
            .setSmallIcon(R.drawable.ic_notif_corrida)
            .setColor(PwNotificacoes.cor)
            .setLargeIcon(PwNotificacoes.logoRedonda(ctx))
            .setContentTitle("Teste de som")
            .setContentText("É assim que uma nova corrida vai tocar.")
            .setAutoCancel(true)
            .setTimeoutAfter(6_000)
            .build()
        ctx.getSystemService(NotificationManager::class.java)?.notify(NOTIF_AVISO + 1, n)
    }

    /** Android 14+: o usuário precisa permitir notificações em tela cheia. */
    fun podeTelaCheia(ctx: Context): Boolean =
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            ctx.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
        } else true
}
