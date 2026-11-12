package com.pwexpress.feature.gestor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PARAMETROS
import kotlinx.coroutines.delay
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Os cálculos que podem pagar uma rota, na ordem em que aparecem. */
private val CALCULOS = listOf(
    Triple("zonas", "Zonas", "pag_zonas"), Triple("rota", "Por km", "pag_km"), Triple("percentual", "Percentual", "percentual"),
    Triple("por_entrega", "Por entrega", "pag_entrega"), Triple("fixo", "Fixo", "pag_fixo"),
)
private val COMBINAR = listOf("soma" to "Soma", "media" to "Média", "maior" to "Maior", "menor" to "Menor")

/** Valor de cada cálculo num resultado (combinado tem as partes; um só, é o próprio). */
private fun valorDe(calc: JsonObject?, modo: String): Double? {
    calc ?: return null
    val partes = calc.arr("partes")
    if (partes.isNotEmpty()) return partes.firstOrNull { it.str("parte") == modo }?.let { it.dbl("calculado") ?: it.dbl("total") }
    return if (calc.str("modo") == modo) calc.dbl("calculado") ?: calc.dbl("total") else null
}
private fun modosDe(calc: JsonObject?): List<String> {
    calc ?: return emptyList()
    val partes = calc.arr("partes").mapNotNull { it.str("parte") }
    return partes.ifEmpty { listOfNotNull(calc.str("modo")?.takeIf { it != "combinado" && it != "periodo" }) }
}

/**
 * Pagamento do motoboy na rota, num lugar só: quanto ele recebe, quais cálculos entram (liga e desliga),
 * como juntar, os valores de cada cálculo só para esta rota, o ajuste e se o motoboy vê o motivo.
 * Vale até o holerite do período fechar. Mostra a prévia antes de salvar.
 */
@Composable
internal fun PagamentoMotoboyG(corrida: CorridaG, ocupado: Boolean, acao: (suspend () -> Result<Unit>) -> Unit) {
    val c = Pw.cores
    val calc = corrida.pagCalculo ?: return
    if (corrida.status == "cancelada") return
    val salvas = remember(corrida.id) { mutableStateOf<JsonObject?>(null) }
    val regras = remember(corrida.id) { mutableStateMapOf<String, JsonElement>() }
    var previa by remember(corrida.id) { mutableStateOf<JsonObject?>(null) }
    var aberto by remember(corrida.id) { mutableStateOf<String?>(null) }
    var mais by remember(corrida.id) { mutableStateOf(false) }
    var ajuste by remember(corrida.id) { mutableStateOf(if (corrida.pagAjuste == 0.0) "" else numeroBr(corrida.pagAjuste)) }
    var motivo by remember(corrida.id) { mutableStateOf(corrida.pagAjusteMotivo.orEmpty()) }
    var mostrar by remember(corrida.id) { mutableStateOf(corrida.pagAjusteMostrar) }

    LaunchedEffect(corrida.id) {
        val r = Gestao.rpcJson("previa_pagamento_rota", buildJsonObject { put("p_corrida", corrida.id) }).getOrNull() as? JsonObject
        val atuais = (r?.get("regras_rota") as? JsonObject) ?: JsonObject(emptyMap())
        salvas.value = atuais; regras.clear(); regras.putAll(atuais)
    }
    val atualRegras = JsonObject(regras.toMap())
    val mudou = salvas.value != null && atualRegras != salvas.value
    LaunchedEffect(atualRegras.toString()) {
        if (!mudou) { previa = null; return@LaunchedEffect }
        delay(350)
        previa = Gestao.rpcJson("previa_pagamento_rota", buildJsonObject { put("p_corrida", corrida.id); put("p_regras", atualRegras) }).getOrNull() as? JsonObject
    }
    val ligados = (regras["pag_modos"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }?.takeIf { it.isNotEmpty() } ?: modosDe(calc)
    fun definirModos(l: List<String>) { regras["pag_modos"] = JsonArray(l.map { JsonPrimitive(it) }); regras.remove("pag_modo") }
    val base = previa ?: calc
    val totalCalc = base.dbl("total") ?: 0.0
    val ajusteNum = ajuste.replace('−', '-').replace("R$", "").replace(" ", "").replace(',', '.').toDoubleOrNull() ?: 0.0

    PwCartao(destaque = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.HandCoins, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Text("Pagamento do motoboy", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            if (corrida.motoboyId == null) com.pwexpress.core.designsystem.PwSelo("PRÉVIA", c.texto3)
            if ((salvas.value?.size ?: 0) > 0) com.pwexpress.core.designsystem.PwSelo("SÓ DESTA ROTA", c.ouro, icone = PwIcones.SlidersHorizontal)
        }
        // o número que importa
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
            Text(moedaBr(totalCalc + ajusteNum), color = c.ouro, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 32.sp)
            Spacer(Modifier.width(8.dp))
            if (previa != null) Text("antes ${moedaBr(corrida.valorMotoboy ?: calc.dbl("total"))}", color = c.texto3, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        TextoDica(when {
            ligados.size > 1 -> "${ligados.size} cálculos juntos · ${COMBINAR.firstOrNull { it.first == (regras["pag_combinar"] as? JsonPrimitive)?.content ?: calc.str("combinar") }?.second?.lowercase() ?: "soma"}" +
                (if (base.dbl("calculado") != null && base.dbl("calculado") != base.dbl("total")) " · limitado ao valor da rota" else "")
            ligados.size == 1 -> CALCULOS.firstOrNull { it.first == ligados[0] }?.second ?: ligados[0]
            else -> "Pago no holerite (salário, diária ou hora)"
        })

        val podeMudar = com.pwexpress.feature.gestor.data.Permissoes.estado.value.pode("editar_pagamento_motoboy")
        if (!podeMudar) { TextoDica("A plataforma não liberou mudar o pagamento do motoboy."); return@PwCartao }
        Spacer(Modifier.height(10.dp))
        EditorCalculosG(regras, base, calc)
        if (mudou || (salvas.value?.size ?: 0) > 0) Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PwBotaoOuro("Salvar cálculo", onClick = {
                acao {
                    val r = Gestao.rpcJson("definir_regras_rota", buildJsonObject { put("p_corrida", corrida.id); put("p_regras", atualRegras) })
                    if (r.isSuccess) { salvas.value = atualRegras; previa = null; Gestao.atualizar() }
                    r.map { }
                }
            }, icone = PwIcones.Check, habilitado = mudou && !ocupado, modifier = Modifier.weight(1f))
            if ((salvas.value?.size ?: 0) > 0) PwBotaoVidro("Padrão", onClick = {
                acao {
                    val r = Gestao.rpcJson("definir_regras_rota", buildJsonObject { put("p_corrida", corrida.id); put("p_regras", JsonObject(emptyMap())) })
                    if (r.isSuccess) { salvas.value = JsonObject(emptyMap()); regras.clear(); Gestao.atualizar() }
                    r.map { }
                }
            }, icone = PwIcones.RotateCcw)
        }
        TextoDica("Mudar aqui vale só para esta rota. O padrão fica em Ajustes → Motoboys.")

        // ------------------------------------------------ ajuste e o que o motoboy vê
        if (corrida.motoboyId != null) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.borda))
            Spacer(Modifier.height(10.dp))
            PwRotuloG("Ajuste nesta rota")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(ajuste, { ajuste = it }, "R$ (+/−)", teclado = KeyboardType.Text, modifier = Modifier.weight(1f))
                PwCampo(motivo, { motivo = it }, "Motivo", dica = "Ex.: chuva, espera", modifier = Modifier.weight(2f))
            }
            PwInterruptor("O motoboy vê o motivo", mostrar, { mostrar = it }, descricao = if (mostrar) "Aparece no holerite dele." else "Ele vê só o valor ajustado.", icone = if (mostrar) PwIcones.Eye else PwIcones.EyeOff)
            PwBotaoVidro("Salvar ajuste", onClick = {
                acao {
                    val r = Gestao.rpcJson("ajustar_pagamento_rota", buildJsonObject {
                        put("p_corrida", corrida.id); put("p_ajuste", ajusteNum); put("p_motivo", motivo.trim().ifBlank { null }); put("p_mostrar", mostrar)
                    })
                    if (r.isSuccess) Gestao.atualizar()
                    r.map { }
                }
            }, icone = PwIcones.Check, habilitado = !ocupado, modifier = Modifier.fillMaxWidth())
        }
    }
}

/** Liga e desliga os cálculos, abre os valores de cada um, escolhe como juntar e os acréscimos. Muda só o mapa [regras]. */
@Composable
internal fun EditorCalculosG(regras: androidx.compose.runtime.snapshots.SnapshotStateMap<String, JsonElement>, base: JsonObject?, calc: JsonObject?) {
    val c = Pw.cores
    var aberto by remember { mutableStateOf<String?>(null) }
    var mais by remember { mutableStateOf(false) }
    val ligados = (regras["pag_modos"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }?.takeIf { it.isNotEmpty() } ?: modosDe(calc)
    fun definirModos(l: List<String>) { regras["pag_modos"] = JsonArray(l.map { JsonPrimitive(it) }); regras.remove("pag_modo") }
        // ------------------------------------------------ cálculos: liga, desliga e abre os valores de cada um
        PwRotuloG("Como calcular")
        CALCULOS.forEach { (modo, nome, grupo) ->
            val ligado = modo in ligados
            val forma = RoundedCornerShape(14.dp)
            Column(Modifier.fillMaxWidth().padding(top = 6.dp).clip(forma).background(if (ligado) c.ouro.copy(alpha = 0.08f) else c.vidro)
                .border(1.dp, if (ligado) c.ouro.copy(alpha = 0.45f) else c.borda, forma)) {
                Row(Modifier.fillMaxWidth().clickable { aberto = if (aberto == modo) null else modo }.padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(ICONE_MODO[modo] ?: PwIcones.Wallet, null, tint = if (ligado) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(nome, color = if (ligado) c.texto else c.texto2, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
                        Text(valorDe(base, modo)?.let { moedaBr(it) } ?: if (ligado) "calculando…" else "fora do cálculo", color = c.texto3, fontSize = 12.sp)
                    }
                    Icon(if (aberto == modo) PwIcones.ChevronUp else PwIcones.ChevronDown, "Valores", tint = c.texto3, modifier = Modifier.size(18.dp))
                    Switch(ligado, { on -> definirModos(if (on) (ligados + modo).distinct() else (ligados - modo).ifEmpty { ligados }) },
                        colors = SwitchDefaults.colors(checkedTrackColor = c.ouro, checkedThumbColor = c.fundo))
                }
                AnimatedVisibility(aberto == modo) {
                    Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp)) {
                        val ps = PARAMETROS.filter { p -> !p.soDev && !p.escondido && if (grupo == "percentual") p.chave == "repasse_motoboy_pct" else p.grupo == grupo }
                        EditorRegrasG(ps, regras, { k, v -> if (v == null) regras.remove(k) else regras[k] = v })
                    }
                }
            }
        }
        if (ligados.size > 1) {
            Spacer(Modifier.height(10.dp))
            PwRotuloG("Juntar os cálculos")
            val atualC = (regras["pag_combinar"] as? JsonPrimitive)?.content ?: calc?.str("combinar") ?: "soma"
            PwSegmentado(COMBINAR.map { it.second to null }, COMBINAR.indexOfFirst { it.first == atualC }.coerceAtLeast(0), { regras["pag_combinar"] = JsonPrimitive(COMBINAR[it].first) },
                Modifier.fillMaxWidth().padding(top = 6.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { mais = !mais }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.SlidersHorizontal, null, tint = c.texto3, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
            Text("Acréscimos e volta", color = c.texto2, fontSize = 13.5.sp, modifier = Modifier.weight(1f))
            Icon(if (mais) PwIcones.ChevronUp else PwIcones.ChevronDown, null, tint = c.texto3, modifier = Modifier.size(16.dp))
        }
        AnimatedVisibility(mais) {
            val ps = PARAMETROS.filter { p -> !p.soDev && !p.escondido && p.chave in setOf("pag_acrescimo_pct", "pag_acrescimo_rota", "pag_volta", "pag_volta_valor_km", "pag_volta_pct", "pag_volta_teto") }
            Column { EditorRegrasG(ps, regras, { k, v -> if (v == null) regras.remove(k) else regras[k] = v }) }
        }
}

@Composable
private fun PwRotuloG(t: String) = Text(t, color = Pw.cores.texto, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
