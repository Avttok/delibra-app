package com.pwexpress.feature.corridas.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.pwexpress.core.designsystem.EstiloDestaque
import com.pwexpress.core.designsystem.PwDestaque
import com.pwexpress.core.notificacoes.PwNotificacoes
import com.pwexpress.feature.corridas.data.Corridas
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/** Recebe mensagens do servidor (via Firebase), mesmo com o app fechado. */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Corridas.registrarToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        when (message.data["tipo"]) {
            "oferta" -> {
                // toca primeiro (não depende de rede), depois busca os detalhes e enriquece a notificação
                val id = message.data["oferta_id"]
                Alarme.mostrarOferta(applicationContext, ofertaId = id)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
                Corridas.oferta.value?.takeIf { id == null || it.id == id }?.let { Alarme.mostrarOferta(applicationContext, it) }
            }
            "planejada_nova", "planejada_voltou", "planejada_reservada", "planejada_liberada",
            "planejada_reagendada", "planejada_lembrete", "planejada_atrasada" -> {
                Alarme.planejada(applicationContext, message.data["tipo"]!!, message.data["corrida_id"], message.data["agendada_para"])
                runBlocking { withTimeoutOrNull(6_000) { Corridas.carregarPlanejadas() } }
            }
            "rota_nova" -> {
                Alarme.rotaNova(applicationContext, message.data["corrida_id"], message.data["planejada"] == "true",
                    message.data["entregas"]?.toIntOrNull() ?: 1, message.data["sugerida"] == "true", message.data["agendada_para"])
                runBlocking { withTimeoutOrNull(6_000) { Corridas.carregarRotas() } }
            }
            "corrida_voltou" -> Alarme.avisarSilencioso(applicationContext, "Uma corrida que você recusou voltou ao lançamento")
            "cancelada" -> {
                Alarme.pararOferta(applicationContext)
                PwNotificacoes.cartao(applicationContext, 4100, PwNotificacoes.CANAL_AVISOS, "Corrida cancelada",
                    "O gestor cancelou a corrida. Se você estava a caminho, pode parar.", PwNotificacoes.GRAD_VERMELHO)
                PwDestaque.mostrar("Corrida cancelada", "O gestor cancelou. Se estava a caminho, pode parar.", EstiloDestaque.CANCELADA, duracaoMs = 7_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "rota_estendida" -> {
                PwNotificacoes.cartao(applicationContext, 4200, PwNotificacoes.CANAL_AVISOS, "Sua rota cresceu",
                    "Entraram mais paradas no fim da sua rota. O que você já ia ganhar fica garantido.", PwNotificacoes.GRAD_OURO)
                PwDestaque.mostrar("Sua rota cresceu", "Mais paradas no fim da rota. Seu valor não diminui.", EstiloDestaque.EXTENSAO, duracaoMs = 7_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "planejada_aberta" -> {
                PwNotificacoes.cartao(applicationContext, 4300 + ((message.data["corrida_id"]?.hashCode() ?: 0) and 0xFF), PwNotificacoes.CANAL_PLANEJADAS,
                    "Rota planejada sem motoboy", "Ninguém reservou e o horário está chegando. Pegue antes dos outros.", PwNotificacoes.GRAD_AZUL,
                    abrir = PwNotificacoes.abrirApp(applicationContext, 4300, "rotas"))
                runBlocking { withTimeoutOrNull(6_000) { Corridas.carregarPlanejadas(); Corridas.carregarRotas() } }
            }
            "chat" -> {
                val conversa = message.data["conversa_id"] ?: return
                PwNotificacoes.mensagemChat(applicationContext, conversa, message.data["titulo"] ?: "Conversa", message.data["texto"] ?: "Nova mensagem")
                runBlocking { withTimeoutOrNull(5_000) { com.pwexpress.core.chat.Chat.carregar() } }
            }
        }
    }
}
