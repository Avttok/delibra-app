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
                // a checagem em segundo plano pode já ter tocado esta oferta
                if (id == null || Alarme.ofertaTocada != id) Alarme.mostrarOferta(applicationContext, ofertaId = id)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
                Corridas.oferta.value?.takeIf { (id == null || it.id == id) && Alarme.ofertaCompleta != it.id }?.let { Alarme.mostrarOferta(applicationContext, it) }
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
            "teste" -> com.pwexpress.core.notificacoes.PwNotificacoes.cartao(applicationContext, 4900, com.pwexpress.core.notificacoes.PwNotificacoes.CANAL_AVISOS,
                message.data["titulo"] ?: "Teste de aviso", message.data["texto"] ?: "O push está funcionando.", com.pwexpress.core.notificacoes.PwNotificacoes.GRAD_VERDE, icone = "check")
            "corrida_atribuida" -> {
                // o gestor começou uma rota sua agora: o app entra em corrida (e fica online) sozinho
                PwNotificacoes.cartao(applicationContext, 4400, PwNotificacoes.CANAL_AVISOS, "Sua rota começou",
                    "O gestor iniciou a rota agora. Abra o app para ver o caminho.", PwNotificacoes.GRAD_OURO, icone = "rota")
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "corrida_voltou" -> Alarme.avisarSilencioso(applicationContext, "Uma corrida que você recusou voltou ao lançamento")
            "cancelada" -> {
                Alarme.pararOferta(applicationContext)
                PwNotificacoes.cartao(applicationContext, 4100, PwNotificacoes.CANAL_AVISOS, "Corrida cancelada",
                    "O gestor cancelou a corrida. Se você estava a caminho, pode parar.", PwNotificacoes.GRAD_VERMELHO, icone = "x")
                PwDestaque.mostrar("Corrida cancelada", "O gestor cancelou. Se estava a caminho, pode parar.", EstiloDestaque.CANCELADA, duracaoMs = 7_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "rota_estendida" -> {
                PwNotificacoes.cartao(applicationContext, 4200, PwNotificacoes.CANAL_AVISOS, "Sua rota cresceu",
                    "Entraram mais paradas no fim da sua rota. O que você já ia ganhar fica garantido.", PwNotificacoes.GRAD_OURO, icone = "mais")
                PwDestaque.mostrar("Sua rota cresceu", "Mais paradas no fim da rota. Seu valor não diminui.", EstiloDestaque.EXTENSAO, duracaoMs = 7_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "planejada_aberta" -> {
                PwNotificacoes.cartao(applicationContext, 4300 + ((message.data["corrida_id"]?.hashCode() ?: 0) and 0xFF), PwNotificacoes.CANAL_PLANEJADAS,
                    "Rota planejada sem motoboy", "Ninguém reservou e o horário está chegando. Pegue antes dos outros.", PwNotificacoes.GRAD_AZUL,
                    abrir = PwNotificacoes.abrirApp(applicationContext, 4300, "rotas"), icone = "calendario")
                runBlocking { withTimeoutOrNull(6_000) { Corridas.carregarPlanejadas(); Corridas.carregarRotas() } }
            }
            // a rota saiu do motoboy: passada para outro, ou o cancelamento que ele pediu foi aprovado
            "corrida_passada" -> {
                Alarme.pararOferta(applicationContext)
                val titulo = message.data["titulo"] ?: "Rota passada para outro motoboy"
                val texto = message.data["texto"] ?: "O gestor tirou esta rota de você."
                PwNotificacoes.cartao(applicationContext, 4110, PwNotificacoes.CANAL_AVISOS, titulo, texto, PwNotificacoes.GRAD_VERMELHO, icone = "x")
                PwDestaque.mostrar(titulo, texto, EstiloDestaque.CANCELADA, duracaoMs = 7_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "chamado_respondido" -> {
                if (message.data["pular"] == "true") {
                    val titulo = message.data["titulo"] ?: "Resposta do gestor"
                    val texto = message.data["texto"].orEmpty()
                    val liberou = titulo.contains("liberada", ignoreCase = true)
                    PwNotificacoes.cartao(applicationContext, 4121, PwNotificacoes.CANAL_AVISOS, titulo, texto, if (liberou) PwNotificacoes.GRAD_VERDE else PwNotificacoes.GRAD_OURO, icone = if (liberou) "check" else "alerta")
                    PwDestaque.mostrar(titulo, texto, if (liberou) EstiloDestaque.ONLINE else EstiloDestaque.EXTENSAO, duracaoMs = 6_000)
                    runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
                    return
                }
                val aprovado = message.data["aprovado"] == "true"
                val titulo = message.data["titulo"] ?: if (aprovado) "Cancelamento aprovado" else "Cancelamento negado"
                val texto = message.data["texto"] ?: if (aprovado) "A rota saiu de você. Pode parar." else "Siga com a rota como combinado."
                PwNotificacoes.cartao(applicationContext, 4120, PwNotificacoes.CANAL_AVISOS, titulo, texto,
                    if (aprovado) PwNotificacoes.GRAD_VERDE else PwNotificacoes.GRAD_OURO, icone = if (aprovado) "check" else "alerta")
                PwDestaque.mostrar(titulo, texto, if (aprovado) EstiloDestaque.CANCELADA else EstiloDestaque.EXTENSAO, duracaoMs = 7_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            "suspenso", "suspensao_fim" -> {
                val fim = message.data["tipo"] == "suspensao_fim"
                PwNotificacoes.cartao(applicationContext, 4130, PwNotificacoes.CANAL_AVISOS,
                    message.data["titulo"] ?: if (fim) "Suspensão retirada" else "Você está suspenso",
                    message.data["texto"] ?: if (fim) "Você já pode receber corridas de novo." else "Sem corridas novas por um tempo.",
                    if (fim) PwNotificacoes.GRAD_VERDE else PwNotificacoes.GRAD_VERMELHO, icone = if (fim) "check" else "alerta")
                runBlocking { withTimeoutOrNull(6_000) { com.pwexpress.core.perfil.Perfis.carregar() } }
            }
            "parada_liberada" -> {
                val titulo = message.data["titulo"] ?: "Parada liberada"
                val texto = message.data["texto"] ?: "Pode confirmar de onde está."
                PwNotificacoes.cartao(applicationContext, 4122, PwNotificacoes.CANAL_AVISOS, titulo, texto, PwNotificacoes.GRAD_VERDE, icone = "check")
                PwDestaque.mostrar(titulo, texto, EstiloDestaque.ONLINE, duracaoMs = 6_000)
                runBlocking { withTimeoutOrNull(8_000) { Corridas.sincronizarAgora() } }
            }
            // emergência de um motoboy: o aviso mais forte que o app tem
            "emergencia" -> {
                val idN = 4700 + ((message.data["emergencia_id"] ?: "").hashCode() and 0xFF)
                PwNotificacoes.cartao(applicationContext, idN, PwNotificacoes.CANAL_AVISOS,
                    message.data["titulo"] ?: "EMERGÊNCIA", message.data["texto"] ?: "Um motoboy apertou o botão de emergência.", PwNotificacoes.GRAD_VERMELHO,
                    abrir = PwNotificacoes.abrirApp(applicationContext, idN, "gestor:emergencia"), icone = "alerta")
                PwDestaque.mostrar(message.data["titulo"] ?: "EMERGÊNCIA", message.data["texto"], EstiloDestaque.CANCELADA, duracaoMs = 12_000)
            }
            // avisos do gestor: motoboy pedindo para cancelar ou liberar parada, motoboy suspenso, nota ruim, rota fechada sem sinal
            "chamado_cancelar", "motoboy_suspenso", "chamado_pular", "avaliacao_ruim", "corrida_fechada_auto" -> {
                val corrida = message.data["corrida_id"].orEmpty()
                val idN = 4500 + ((message.data["chamado_id"] ?: corrida).hashCode() and 0xFF)
                PwNotificacoes.cartao(applicationContext, idN, PwNotificacoes.CANAL_AVISOS,
                    message.data["titulo"] ?: "Pedido de cancelamento", message.data["texto"] ?: "", PwNotificacoes.GRAD_VERMELHO,
                    abrir = PwNotificacoes.abrirApp(applicationContext, idN, if (corrida.isNotBlank()) "gestor:corrida:$corrida" else "gestor:equipe"), icone = "alerta")
            }
            // avisos do gestor: pedido novo, rota sem motoboy, cadastro de cliente
            "pedido_novo", "rota_sem_motoboy", "cadastro_novo" -> {
                val t = message.data["tipo"]!!
                val alvo = when (t) {
                    "pedido_novo" -> "gestor:pedido:${message.data["pedido_id"].orEmpty()}"
                    "rota_sem_motoboy" -> "gestor:corrida:${message.data["corrida_id"].orEmpty()}"
                    else -> "gestor:clientes"
                }
                val idN = 4400 + ((message.data["pedido_id"] ?: message.data["corrida_id"] ?: message.data["cadastro_id"] ?: t).hashCode() and 0xFF)
                PwNotificacoes.cartao(applicationContext, idN, PwNotificacoes.CANAL_AVISOS,
                    message.data["titulo"] ?: "Aviso", message.data["texto"] ?: "", when (t) { "rota_sem_motoboy" -> PwNotificacoes.GRAD_VERMELHO; "cadastro_novo" -> PwNotificacoes.GRAD_AZUL; else -> PwNotificacoes.GRAD_OURO },
                    abrir = PwNotificacoes.abrirApp(applicationContext, idN, alvo),
                    icone = when (t) { "rota_sem_motoboy" -> "alerta"; "cadastro_novo" -> "pessoa"; else -> "rota" })
            }
            "chat" -> {
                val conversa = message.data["conversa_id"] ?: return
                PwNotificacoes.mensagemChat(applicationContext, conversa, message.data["titulo"] ?: "Conversa", message.data["texto"] ?: "Nova mensagem")
                runBlocking { withTimeoutOrNull(5_000) { com.pwexpress.core.chat.Chat.carregar() } }
            }
            // qualquer aviso novo do servidor que traga título e texto aparece, mesmo sem tratamento próprio
            else -> {
                val titulo = message.data["titulo"] ?: message.notification?.title ?: return
                val texto = message.data["texto"] ?: message.notification?.body ?: ""
                PwNotificacoes.cartao(applicationContext, 4600 + (titulo.hashCode() and 0xFF), PwNotificacoes.CANAL_AVISOS, titulo, texto, PwNotificacoes.GRAD_OURO, icone = "rota")
            }
        }
    }
}
