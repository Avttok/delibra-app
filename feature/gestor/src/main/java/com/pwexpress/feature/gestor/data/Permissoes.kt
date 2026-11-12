package com.pwexpress.feature.gestor.data

import com.pwexpress.core.PwCore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/** O que o Dev deixou este gestor ver e fazer (igual ao site). O banco barra de qualquer jeito. */
object Permissoes {
    data class Estado(val dev: Boolean = false, val ajustes: Map<String, String> = emptyMap(), val acoes: Map<String, Boolean> = emptyMap(), val carregou: Boolean = false) {
        fun nivel(chave: String) = if (dev) "editar" else ajustes[chave] ?: "editar"
        fun pode(acao: String) = dev || acoes[acao] != false
    }
    private val _estado = MutableStateFlow(Estado())
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    suspend fun carregar() {
        runCatching {
            val r = PwCore.rpc.chamar("minhas_permissoes", JsonObject(emptyMap()))
            if (!r.ok) return
            val o = kotlinx.serialization.json.Json.parseToJsonElement(r.corpo) as? JsonObject ?: return
            _estado.value = Estado(
                dev = (o["dev"] as? JsonPrimitive)?.booleanOrNull == true,
                ajustes = (o["ajustes"] as? JsonObject)?.mapValues { (it.value as? JsonPrimitive)?.content ?: "editar" }.orEmpty(),
                acoes = (o["acoes"] as? JsonObject)?.mapValues { (it.value as? JsonPrimitive)?.booleanOrNull ?: true }.orEmpty(),
                carregou = true,
            )
        }
    }
    fun limpar() { _estado.value = Estado() }
}

/** Ações que o Dev liga e desliga (mesma lista do banco). */
val ACOES_GESTOR = listOf(
    "criar_contas" to "Criar contas", "excluir_contas" to "Excluir contas", "editar_cargos" to "Mudar cargos",
    "apagar_historico" to "Apagar histórico", "exportar_dados" to "Relatórios e planilhas", "editar_valor_pedido" to "Mudar o valor do pedido",
    "editar_pagamento_motoboy" to "Mudar o pagamento do motoboy", "ajuste_holerite" to "Lançar ajustes no holerite", "fechar_holerite" to "Fechar e pagar holerites",
    "emitir_fatura" to "Emitir faturas", "cancelar_rota" to "Cancelar rotas", "voltar_revisao" to "Voltar pedidos à revisão",
    "forcar_inicio" to "Forçar início de planejada", "chat_apagar" to "Apagar mensagens de outros", "mudar_marca" to "Mudar a marca da empresa",
    "modo_teste" to "Usar o modo teste", "ver_carteira" to "Ver a carteira", "regras_por_cliente" to "Regras por cliente",
    "regras_por_motoboy" to "Regras por motoboy", "penalidades" to "Penalidades",
)
