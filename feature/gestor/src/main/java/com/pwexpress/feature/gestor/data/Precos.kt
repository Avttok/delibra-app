package com.pwexpress.feature.gestor.data

import kotlinx.serialization.json.JsonElement

/** Mesmas regras de preço do site (estratégia + mínimo + arredondamento) e o acréscimo do percurso. */
object Precos {
    val modalidades = listOf("por_km" to "Por km rodado", "fixo" to "Preço fixo", "por_zona" to "Por região/zona")
    fun rotulo(m: String) = modalidades.firstOrNull { it.first == m }?.second ?: m

    data class Orcamento(val valor: Double?, val explicacao: String)

    fun orcar(modalidade: String, valorKm: Double?, valorFixo: Double?, distanciaM: Int?, cfg: Map<String, JsonElement>): Orcamento {
        val porKmPadrao = cfg.numero("valor_km_padrao", 2.0)
        val base = when (modalidade) {
            "fixo" -> Orcamento(valorFixo, "Preço fixo")
            "por_zona" -> Orcamento(null, "Por zona: informe o valor")
            else -> {
                val porKm = valorKm ?: porKmPadrao
                if (distanciaM == null) Orcamento(null, "Aguardando cálculo da rota")
                else Orcamento(distanciaM / 1000.0 * porKm, "${"%.1f".format(distanciaM / 1000.0).replace('.', ',')} km × R$ ${"%.2f".format(porKm).replace('.', ',')}")
            }
        }
        var v = base.valor ?: return base
        val notas = mutableListOf(base.explicacao)
        val minimo = cfg.numero("valor_minimo", 0.0)
        if (minimo > 0 && v < minimo) { v = minimo; notas += "mínimo" }
        v = arredondar(v, cfg)
        return Orcamento(v, notas.joinToString(" · "))
    }

    fun arredondar(v: Double, cfg: Map<String, JsonElement>): Double {
        val a = cfg.numero("arredondamento", 0.0)
        return if (a > 0) Math.ceil(v / a - 1e-9) * a else Math.round(v * 100) / 100.0
    }

    /** Acréscimo do percurso (coleta → entrega): faixas × fator. */
    fun fatorEntrega(distanciaM: Int?, cfg: Map<String, JsonElement>): Double {
        if (!cfg.logico("acrescimo_distancia", true) || distanciaM == null) return 0.0
        val cada = cfg.numero("acrescimo_entrega_cada_km", 200.0)
        return if (cada > 0) Math.floor(distanciaM / 1000.0 / cada) * cfg.numero("acrescimo_entrega_fator", 0.25) else 0.0
    }
}
