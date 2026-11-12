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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwPasso
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.PARAMETROS_VISIVEIS
import com.pwexpress.feature.gestor.data.PedidoG
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull

/** As regras de "juntar rotas" que valem por pedido: só aqui no painel (não repetem em "Regras deste pedido"). */
internal val CHAVES_JUNTAR = listOf(
    "roteirizar_agrupar", "rota_raio_coleta_km", "rota_raio_entrega_km", "rota_max_entregas", "rota_max_coletas", "rota_max_km",
    "rota_exclusiva_cliente", "rota_revisao_gestor",
)

private data class Bloco(val chave: String, val icone: ImageVector, val titulo: String, val texto: (Double) -> String)

private val BLOCOS = listOf(
    Bloco("rota_raio_coleta_km", PwIcones.ArrowUpFromLine, "Coletas") { v -> if (v <= 0) "só a mesma coleta" else "a até ${numeroBr(v, 1)} km entre si" },
    Bloco("rota_raio_entrega_km", PwIcones.ArrowDownToLine, "Entregas") { v -> if (v <= 0) "só no mesmo endereço" else "a até ${numeroBr(v, 1)} km uma da outra" },
    Bloco("rota_max_entregas", PwIcones.Package2, "Máximo") { v -> "${v.toInt()} entrega${if (v.toInt() == 1) "" else "s"} por rota" },
    Bloco("rota_max_coletas", PwIcones.Store, "Coletas por rota") { v -> "até ${v.toInt()}" },
    Bloco("rota_max_km", PwIcones.Ruler, "Tamanho") { v -> if (v <= 0) "sem limite de km" else "até ${numeroBr(v, 0)} km da coleta" },
)

/**
 * Revisão do pedido: como ele se junta com outros numa rota. Mostra o que vale agora (e de onde vem:
 * padrão, cliente ou só este pedido); tocar num bloco muda só para este pedido. A prévia das rotas logo
 * abaixo se refaz sozinha.
 */
@Composable
internal fun PainelJuntarG(p: PedidoG, onAbrir: (TelaG) -> Unit = {}) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    val dados by Gestao.dados.collectAsState()
    var doCliente by remember(p.empresaId) { mutableStateOf<Map<String, JsonElement>>(emptyMap()) }
    LaunchedEffect(p.empresaId) { p.empresaId?.let { doCliente = Gestao.politicas("cliente", it) } }
    val doPedido = remember(p.id) { mutableStateMapOf<String, JsonElement>().apply { putAll(p.regras) } }
    var aberto by remember(p.id) { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    val params = PARAMETROS_VISIVEIS.associateBy { it.chave }

    fun origem(k: String) = when { k in doPedido -> "pedido"; k in doCliente -> "cliente"; else -> "padrao" }
    fun valor(k: String): JsonElement? = doPedido[k] ?: doCliente[k] ?: dados.config[k]
    fun num(k: String) = (valor(k) as? JsonPrimitive)?.doubleOrNull ?: params[k]?.padraoNum ?: 0.0
    fun bool(k: String) = (valor(k) as? JsonPrimitive)?.let { it.booleanOrNull ?: it.content.toBooleanStrictOrNull() } ?: params[k]?.padraoBool ?: false
    fun salvar(k: String, v: JsonElement?) {
        val novo = doPedido.toMutableMap().apply { if (v == null) remove(k) else put(k, v) }
        escopo.launch {
            Gestao.definirRegrasPedido(p.id, JsonObject(novo)).onSuccess {
                if (v == null) doPedido.remove(k) else doPedido[k] = v
                erro = null; Gestao.atualizar()
            }.onFailure { erro = it.message }
        }
    }

    @Composable
    fun SeloOrigem(k: String) {
        when (origem(k)) {
            "pedido" -> PwSelo("só este pedido", c.ouro, icone = PwIcones.Pin)
            "cliente" -> PwSelo("do cliente", c.azul, icone = PwIcones.Building2)
            else -> PwSelo("padrão", c.texto3)
        }
    }

    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Combine, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Montagem da rota", color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 16.sp)
                Text("O que vale para este pedido. Toque para mudar só aqui.", color = c.texto3, fontSize = 12.5.sp)
            }
            SeloOrigem("roteirizar_agrupar")
        }
        Espaco(8)
        val junta = bool("roteirizar_agrupar")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) {
                PwInterruptor(if (junta) "Junta com outros pedidos" else "Rota só com este pedido", junta, { salvar("roteirizar_agrupar", JsonPrimitive(it)) },
                    descricao = if (junta) "Entra numa rota com pedidos perto e no mesmo horário." else "Cada entrega dele vira uma rota própria.", icone = PwIcones.Combine)
            }
        }
        AnimatedVisibility(junta) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                BLOCOS.forEach { b ->
                    val ativo = aberto == b.chave
                    val forma = RoundedCornerShape(16.dp)
                    val o = origem(b.chave)
                    val corBorda = when { ativo -> c.ouro; o == "pedido" -> c.ouro.copy(alpha = 0.6f); o == "cliente" -> c.azul.copy(alpha = 0.5f); else -> c.borda }
                    Column(Modifier.fillMaxWidth().clip(forma).background(if (ativo) c.ouro.copy(alpha = 0.08f) else c.vidroSutil).border(1.dp, corBorda, forma)
                        .clickable { aberto = if (ativo) null else b.chave }.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(c.ouro.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                Icon(b.icone, null, tint = c.ouro, modifier = Modifier.size(17.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(b.titulo, color = c.texto3, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                                Text(b.texto(num(b.chave)), color = c.texto, fontSize = 14.5.sp, fontWeight = FontWeight.Bold, fontFamily = Archivo)
                            }
                            SeloOrigem(b.chave)
                        }
                        if (ativo) {
                            val pr = params[b.chave]
                            Espaco(10)
                            PwPasso(num(b.chave), { v -> salvar(b.chave, JsonPrimitive(v)); aberto = null }, Modifier.fillMaxWidth(),
                                passo = pr?.passo ?: 1.0, min = pr?.min ?: 0.0, max = pr?.max ?: 100.0, unidade = pr?.unidade, confirmar = true)
                            if (o == "pedido") {
                                Espaco(6)
                                PwPilula(if (b.chave in doCliente) "Voltar ao do cliente" else "Voltar ao padrão", PwIcones.RotateCcw, { salvar(b.chave, null); aberto = null })
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val so = bool("rota_exclusiva_cliente")
                    PwPilula(if (so) "Só pedidos deste cliente" else "Mistura clientes", if (so) PwIcones.Lock else PwIcones.Users, { salvar("rota_exclusiva_cliente", JsonPrimitive(!so)) }, ligada = so)
                    val segura = bool("rota_revisao_gestor")
                    PwPilula(if (segura) "Espera eu soltar" else "Sai sozinha", if (segura) PwIcones.Hand else PwIcones.Send, { salvar("rota_revisao_gestor", JsonPrimitive(!segura)) }, ligada = segura)
                }
            }
        }
        PwErro(erro)
        Espaco(8)
        // mudar para sempre: no cliente ou no padrão (sem repetir as mesmas opções aqui)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (p.empresaId != null) PwPilula("Sempre para este cliente", PwIcones.Building2, { onAbrir(TelaG.Empresa(p.empresaId)) })
            PwPilula("Mudar o padrão", PwIcones.Settings2, { onAbrir(TelaG.Configuracoes) })
        }
    }
}

@Suppress("unused") private val semUso = Color.Unspecified
