package com.pwexpress.feature.gestor.ui

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwTelaCheia
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

private val MODELOS = listOf("percentual" to "Percentual", "licenca" to "Licença", "isento" to "Isento")
private val SITUACOES = listOf("teste" to "Teste", "ativa" to "Ativa", "suspensa" to "Suspensa", "cancelada" to "Cancelada")

private fun senhaDono(): String {
    val l = "abcdefghjkmnpqrstuvwxyz"; val n = "23456789"; val r = java.security.SecureRandom()
    fun p() = (1..4).map { l[r.nextInt(l.length)] }.joinToString("")
    return "${p()}-${p()}-${n[r.nextInt(n.length)]}${n[r.nextInt(n.length)]}"
}

/** Dev → Empresas (igual ao site): lista, plano de cada uma e criar empresa nova já com o gestor. */
@Composable
internal fun DevEmpresasG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<JsonObject>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var editando by remember { mutableStateOf<JsonObject?>(null) }
    var nova by remember { mutableStateOf(false) }
    var versao by remember { mutableIntStateOf(0) }
    LaunchedEffect(versao) {
        Gestao.rpcJson("dev_organizacoes").onSuccess { j ->
            lista = (j as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
        }.onFailure { erro = it.message }
    }
    PwErro(erro)
    PwBotaoOuro("Nova empresa", onClick = { nova = true }, icone = PwIcones.Plus)
    val l = lista ?: run { PwCarregando(); return }
    if (l.isEmpty()) PwVazio("Nenhuma empresa ainda. Crie a primeira com o gestor (dono).", icone = PwIcones.Building2)
    l.forEach { o ->
        val forma = RoundedCornerShape(18.dp)
        Row(Modifier.fillMaxWidth().clip(forma).background(c.vidro).border(1.dp, c.borda, forma).clickable { editando = o }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.fundo2), contentAlignment = Alignment.Center) {
                val logo = o.str("logo_url")
                if (logo != null) coil.compose.AsyncImage(logo, null, Modifier.size(38.dp), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
                else Icon(PwIcones.Building2, null, tint = c.ouro, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(o.str("nome_app") ?: o.str("nome") ?: "", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val modelo = o.str("cobranca_modelo")
                TextoDica(listOfNotNull(
                    o.str("nome").takeIf { o.str("nome_app") != null },
                    SITUACOES.firstOrNull { it.first == o.str("status") }?.second,
                    when (modelo) { "percentual" -> "${numeroBr(o.dbl("cobranca_pct") ?: 0.0)}%"; "licenca" -> moedaBr(o.dbl("mensalidade")) + when (o.str("cobranca_periodo")) { "anual" -> "/ano"; "vitalicia" -> " vitalícia"; else -> "/mês" }; else -> "isenta" },
                    "${o.int("motoboys") ?: 0} motoboys", "${o.int("rotas_30d") ?: 0} rotas em 30 dias",
                ).joinToString(" · "))
            }
            Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
        }
    }
    if (nova) NovaEmpresaG(onFechar = { nova = false }, onCriada = { nova = false; versao++ })
    editando?.let { o -> EditarEmpresaG(o, onFechar = { editando = null }, onSalvo = { editando = null; versao++ }) }
    @Suppress("UNUSED_VARIABLE") val semUso = escopo
}

private val PERIODOS = listOf("mensal" to "Mensal", "anual" to "Anual", "vitalicia" to "Vitalícia")
private val ALCANCES = listOf("nunca" to "Nunca", "sem_livre" to "Sem livre", "sempre" to "Sempre", "parceiros" to "Parceiras")
private val PAPEIS_ROT = mapOf("gestor" to "Gestor", "supervisor" to "Supervisor", "motoboy" to "Motoboy", "cliente" to "Cliente")

private fun quandoFoi(iso: String?): String {
    val ms = com.pwexpress.feature.gestor.data.iso(iso) ?: return "nunca"
    val min = (System.currentTimeMillis() - ms) / 60_000
    return when { min < 2 -> "agora"; min < 60 -> "há $min min"; min < 1440 -> "há ${min / 60} h"; else -> dataHora(ms).substringBefore(" ") }
}

/** Uma empresa no Dev (igual ao site): dados, plano, situação, limites, alcance, números, equipe, clientes e excluir. */
@Composable
private fun EditarEmpresaG(o: JsonObject, onFechar: () -> Unit, onSalvo: () -> Unit) {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    val id = o.str("id").orEmpty()
    var nome by remember { mutableStateOf(o.str("nome").orEmpty()) }
    var nomeApp by remember { mutableStateOf(o.str("nome_app").orEmpty()) }
    var documento by remember { mutableStateOf(o.str("documento").orEmpty()) }
    var status by remember { mutableIntStateOf(SITUACOES.indexOfFirst { it.first == o.str("status") }.coerceAtLeast(0)) }
    var motivo by remember { mutableStateOf(o.str("suspensa_motivo").orEmpty()) }
    var modelo by remember { mutableIntStateOf(MODELOS.indexOfFirst { it.first == o.str("cobranca_modelo") }.coerceAtLeast(0)) }
    var periodo by remember { mutableIntStateOf(PERIODOS.indexOfFirst { it.first == o.str("cobranca_periodo") }.coerceAtLeast(0)) }
    var pct by remember { mutableStateOf(numeroBr(o.dbl("cobranca_pct") ?: 5.0)) }
    var mensal by remember { mutableStateOf(numeroBr(o.dbl("mensalidade") ?: 0.0)) }
    var dia by remember { mutableStateOf((o.int("vencimento_dia") ?: 10).toString()) }
    val lim = o.obj("limites")
    var limMoto by remember { mutableStateOf(lim?.int("motoboys")?.toString().orEmpty()) }
    var limGest by remember { mutableStateOf(lim?.int("gestores")?.toString().orEmpty()) }
    var limCli by remember { mutableStateOf(lim?.int("clientes")?.toString().orEmpty()) }
    var alcance by remember { mutableIntStateOf(ALCANCES.indexOfFirst { it.first == o.str("compartilhar") }.coerceAtLeast(0)) }
    var aposMin by remember { mutableStateOf((o.int("compartilhar_apos_min") ?: 3).toString()) }
    var receber by remember { mutableStateOf(o.str("receber_de_outras") == "true") }
    var obs by remember { mutableStateOf(o.str("observacoes").orEmpty()) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var det by remember { mutableStateOf<JsonObject?>(null) }
    var todas by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var versao by remember { mutableIntStateOf(0) }
    var movendo by remember { mutableStateOf<JsonObject?>(null) }
    var excluir by remember { mutableStateOf(false) }
    LaunchedEffect(versao) {
        Gestao.rpcJson("dev_empresa_detalhe", buildJsonObject { put("p_org", id) }).onSuccess { det = it as? JsonObject }.onFailure { erro = it.message }
        Gestao.rpcJson("dev_organizacoes").onSuccess { j -> todas = (j as? kotlinx.serialization.json.JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty() }
    }
    fun num(s: String) = s.trim().replace(".", "").replace(",", ".").toDoubleOrNull()
    val ehLicenca = MODELOS[modelo].first == "licenca"
    val vitalicia = ehLicenca && PERIODOS[periodo].first == "vitalicia"
    val bloqueada = SITUACOES[status].first in setOf("suspensa", "cancelada")

    PwTelaCheia(o.str("nome_app") ?: o.str("nome") ?: "Empresa", onFechar, subtitulo = SITUACOES[status].second, rodape = {
        PwErro(erro)
        PwBotaoOuro(if (salvando) "Salvando…" else "Salvar", icone = PwIcones.Check, habilitado = !salvando && nome.trim().length >= 2, onClick = {
            salvando = true; erro = null
            escopo.launch {
                val r = Gestao.rpcJson("dev_salvar_organizacao", buildJsonObject {
                    put("p", buildJsonObject {
                        put("id", id); put("nome", nome.trim()); put("documento", documento.trim()); put("status", SITUACOES[status].first)
                        put("suspensa_motivo", motivo.trim()); put("cobranca_modelo", MODELOS[modelo].first); put("cobranca_periodo", PERIODOS[periodo].first)
                        num(pct)?.let { put("cobranca_pct", it) }; num(mensal)?.let { put("mensalidade", it) }
                        dia.toIntOrNull()?.coerceIn(1, 28)?.let { put("vencimento_dia", it) }
                        put("limites", buildJsonObject {
                            limMoto.toIntOrNull()?.takeIf { it > 0 }?.let { put("motoboys", it) }
                            limGest.toIntOrNull()?.takeIf { it > 0 }?.let { put("gestores", it) }
                            limCli.toIntOrNull()?.takeIf { it > 0 }?.let { put("clientes", it) }
                        })
                        put("compartilhar", ALCANCES[alcance].first); aposMin.toIntOrNull()?.coerceIn(0, 240)?.let { put("compartilhar_apos_min", it) }
                        put("receber_de_outras", receber); put("observacoes", obs)
                        o.str("dono_id")?.let { put("dono_id", it) }
                    })
                })
                val rn = Gestao.rpcJson("definir_marca_organizacao", buildJsonObject { put("p_org", id); put("p_nome_app", nomeApp.trim()); put("p_so_nome", true) })
                salvando = false
                if (r.isSuccess && rn.isSuccess) { PwToast.mostrar("Empresa atualizada."); onSalvo() }
                else erro = (r.exceptionOrNull() ?: rn.exceptionOrNull())?.message
            }
        })
    }) {
        // ------------------------------------------------ números
        det?.obj("numeros")?.let { n ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Pedidos 30 dias", "${n.int("pedidos_30d") ?: 0}", Modifier.weight(1f), icone = PwIcones.Package)
                PwNumero("Rotas 30 dias", "${n.int("rotas_30d") ?: 0}", Modifier.weight(1f), icone = PwIcones.Route)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Faturado 30 dias", moedaBr(n.dbl("faturado_30d")), Modifier.weight(1f), icone = PwIcones.Wallet, destaque = true)
                PwNumero("Faturado total", moedaBr(n.dbl("faturado_total")), Modifier.weight(1f), icone = PwIcones.Receipt)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Rotas agora", "${n.int("em_andamento") ?: 0}", Modifier.weight(1f), icone = PwIcones.Bike)
                PwNumero("Último pedido", quandoFoi(n.str("ultimo_pedido")), Modifier.weight(1f), icone = PwIcones.Clock)
            }
        } ?: PwCarregando()

        // ------------------------------------------------ dados
        PwRotulo("Dados")
        PwCampo(nome, { nome = it }, "Nome da empresa")
        PwCampo(nomeApp, { nomeApp = it.take(30) }, "Nome do app (vazio = Delibra)")
        PwCampo(documento, { documento = it }, "CNPJ", teclado = KeyboardType.Number)

        // ------------------------------------------------ situação
        PwRotulo("Situação")
        PwSegmentado(SITUACOES.map { it.second to null }, status, { status = it })
        if (bloqueada) {
            PwCampo(motivo, { motivo = it.take(160) }, "Motivo (a equipe vê ao entrar)", dica = "Ex.: mensalidade em atraso")
            TextoDica("Suspensa ou cancelada: gestores e motoboys desta empresa ficam sem acesso até você reativar.")
        }

        // ------------------------------------------------ plano
        PwRotulo("Cobrança da plataforma")
        PwSegmentado(MODELOS.map { it.second to null }, modelo, { modelo = it })
        if (ehLicenca) PwSegmentado(PERIODOS.map { it.second to null }, periodo, { periodo = it })
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            when (MODELOS[modelo].first) {
                "percentual" -> PwCampo(pct, { pct = it }, "Percentual", Modifier.weight(1f), teclado = KeyboardType.Decimal, sufixo = "%")
                "licenca" -> PwCampo(mensal, { mensal = it }, when (PERIODOS[periodo].first) { "anual" -> "Valor por ano"; "vitalicia" -> "Valor único"; else -> "Valor por mês" },
                    Modifier.weight(1f), teclado = KeyboardType.Decimal, prefixo = "R$ ")
                else -> {}
            }
            if (MODELOS[modelo].first != "isento" && !vitalicia) PwCampo(dia, { dia = it.filter(Char::isDigit).take(2) }, "Vence dia", Modifier.weight(1f), teclado = KeyboardType.Number)
        }
        TextoDica(when {
            MODELOS[modelo].first == "percentual" -> "A plataforma fica com esse percentual de cada rota concluída (sai da parte da empresa, não do motoboy)."
            vitalicia -> "A empresa paga uma vez e usa sem mensalidade."
            ehLicenca && PERIODOS[periodo].first == "anual" -> "A empresa paga uma vez por ano."
            ehLicenca -> "A empresa paga todo mês."
            else -> "Nada é cobrado desta empresa."
        })

        // ------------------------------------------------ limites
        PwRotulo("Limites do plano (vazio = sem limite)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(limMoto, { limMoto = it.filter(Char::isDigit).take(4) }, "Motoboys", Modifier.weight(1f), teclado = KeyboardType.Number)
            PwCampo(limGest, { limGest = it.filter(Char::isDigit).take(4) }, "Gestores", Modifier.weight(1f), teclado = KeyboardType.Number)
            PwCampo(limCli, { limCli = it.filter(Char::isDigit).take(5) }, "Clientes", Modifier.weight(1f), teclado = KeyboardType.Number)
        }

        // ------------------------------------------------ alcance entre empresas
        PwRotulo("Rotas desta empresa chegam a motoboys de outras?")
        PwSegmentado(ALCANCES.map { it.second to null }, alcance, { alcance = it })
        if (ALCANCES[alcance].first == "sem_livre") PwCampo(aposMin, { aposMin = it.filter(Char::isDigit).take(3) }, "Esperar (minutos) antes de abrir", teclado = KeyboardType.Number)
        if (ALCANCES[alcance].first == "parceiros") TextoDica("Escolha as parceiras no site, em Dev → Empresas.")
        com.pwexpress.core.designsystem.PwInterruptor("Motoboys desta empresa recebem rotas de outras", receber, { receber = it })
        PwCampo(obs, { obs = it }, "Anotações (só você vê)", linhas = 2)

        // ------------------------------------------------ equipe
        val equipe = det?.arr("equipe").orEmpty()
        PwRotulo("Equipe (${equipe.size})")
        if (det != null && equipe.isEmpty()) TextoDica("Ninguém nesta empresa ainda.")
        equipe.forEach { p ->
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text((p.str("nome") ?: "Sem nome") + if (p.str("dono") == "true") " · dono" else "", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val papeis = (p["papeis"] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }.orEmpty()
                        TextoDica(listOfNotNull(papeis.joinToString(", ") { PAPEIS_ROT[it] ?: it }.ifBlank { null }, p.str("email")).joinToString(" · "))
                        TextoDica(listOfNotNull("Entrou ${quandoFoi(p.str("ultimo_acesso"))}", p.int("versao_app")?.let { "build $it" },
                            p.str("status")?.takeIf { it != "offline" }?.let { if (it == "em_corrida") "em corrida" else "online" }).joinToString(" · "))
                    }
                    com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.ArrowLeftRight, "Mover", { movendo = p }, tamanho = 36.dp)
                }
            }
        }

        // ------------------------------------------------ clientes
        val clientes = det?.arr("clientes_lista").orEmpty()
        if (clientes.isNotEmpty()) {
            PwRotulo("Clientes (${clientes.size})")
            clientes.take(30).forEach { k -> PwLinhaInfo(PwIcones.Building2, k.str("nome").orEmpty(), "${k.int("pedidos") ?: 0} pedidos") }
            if (clientes.size > 30) TextoDica("E mais ${clientes.size - 30}.")
        }

        // ------------------------------------------------ histórico de cobrança
        val lic = det?.arr("licencas").orEmpty()
        if (lic.isNotEmpty()) {
            PwRotulo("Histórico de cobrança")
            lic.forEach { l ->
                val m = l.str("modelo")
                PwLinhaInfo(PwIcones.Receipt, (MODELOS.firstOrNull { it.first == m }?.second ?: m.orEmpty()) +
                    (if (m == "licenca") " " + (PERIODOS.firstOrNull { it.first == l.str("periodo") }?.second?.lowercase() ?: "") else ""),
                    (if (m == "percentual") "${numeroBr(l.dbl("pct") ?: 0.0)}%" else if (m == "licenca") moedaBr(l.dbl("valor")) else "—") +
                        " · desde ${l.str("inicio")?.let { dataBrCurta(it) } ?: "?"}${if (l.str("fim") == null) "" else " até ${dataBrCurta(l.str("fim")!!)}"}")
            }
        }

        // ------------------------------------------------ excluir
        PwBotaoVidro("Excluir empresa", onClick = { excluir = true }, icone = PwIcones.Trash2, modifier = Modifier.fillMaxWidth())
    }

    movendo?.let { p ->
        PwJanela("Mover ${p.str("nome") ?: ""}", { movendo = null }) {
            TextoDica("A pessoa passa para a outra empresa (sai desta).")
            (todas.filter { it.str("id") != id }.map { it.str("id") to (it.str("nome") ?: "") } + listOf(null to "Sem empresa")).forEach { (destino, rotulo) ->
                PwBotaoVidro(rotulo, onClick = {
                    escopo.launch {
                        Gestao.rpcJson("dev_mover_pessoa", buildJsonObject { put("p_usuario", p.str("id")); put("p_org", destino) })
                            .onSuccess { PwToast.mostrar("Pessoa movida."); movendo = null; versao++ }
                            .onFailure { erro = if (it.message?.contains("limite_") == true) "A empresa de destino chegou ao limite do plano." else it.message; movendo = null }
                    }
                }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (excluir) ExcluirEmpresaG(id, o.str("nome").orEmpty(), onFechar = { excluir = false }, onExcluida = { excluir = false; onSalvo() })
}

private fun dataBrCurta(iso: String) = if (iso.length >= 10) "${iso.substring(8, 10)}/${iso.substring(5, 7)}/${iso.substring(0, 4)}" else iso

@Composable
private fun ExcluirEmpresaG(id: String, nome: String, onFechar: () -> Unit, onExcluida: () -> Unit) {
    val escopo = rememberCoroutineScope()
    var conf by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    PwJanela("Excluir $nome", onFechar) {
        TextoDica("Apaga a empresa com clientes, pedidos, rotas, faturas, holerites, zonas e chat dela. As contas das pessoas continuam, sem empresa. Não dá para desfazer.")
        PwCampo(conf, { conf = it }, "Digite o nome: $nome")
        PwCampo(senha, { senha = it }, "Sua senha", senha = true)
        PwErro(erro)
        PwBotaoOuro(if (ocupado) "Excluindo…" else "Excluir de vez", icone = PwIcones.Trash2, habilitado = !ocupado && conf == nome && senha.isNotBlank(), onClick = {
            ocupado = true; erro = null
            escopo.launch {
                Gestao.rpcJson("dev_excluir_organizacao", buildJsonObject { put("p_org", id); put("p_senha", senha); put("p_confirmar", conf) })
                    .onSuccess { j ->
                        val r = j as? JsonObject
                        when (r?.str("erro")) {
                            "senha_incorreta" -> erro = "Senha incorreta (${r.int("restantes") ?: 0} tentativas)."
                            "muitas_tentativas" -> erro = "Muitas tentativas. Espere 15 minutos."
                            else -> { PwToast.mostrar("Empresa excluída. ${r?.int("contas_sem_empresa") ?: 0} conta(s) ficaram sem empresa."); onExcluida() }
                        }
                    }
                    .onFailure { erro = if (it.message?.contains("confirmacao") == true) "Digite o nome exatamente como está." else it.message }
                ocupado = false
            }
        })
    }
}

/** Empresa nova já com a conta do gestor (dono): no fim mostra o acesso para passar a ele. */
@Composable
private fun NovaEmpresaG(onFechar: () -> Unit, onCriada: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf("") }
    var gestor by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var modelo by remember { mutableIntStateOf(0) }
    var salvando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var acesso by remember { mutableStateOf<String?>(null) }
    acesso?.let { a ->
        PwJanela("Empresa criada", onFechar = onCriada) {
            TextoDica("Passe estes dados para o gestor. A senha não aparece de novo.")
            Text(a, fontFamily = FontFamily.Monospace, color = c.texto, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.campo).border(1.dp, c.bordaForte, RoundedCornerShape(14.dp)).padding(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Copiar acesso", onClick = {
                    ctx.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("Acesso", a))
                    PwToast.mostrar("Acesso copiado.")
                }, icone = PwIcones.Copy, modifier = Modifier.weight(1f))
                PwBotaoOuro("Pronto", onClick = onCriada, modifier = Modifier.weight(1f))
            }
        }
        return
    }
    PwJanela("Nova empresa", onFechar, fecharFora = !salvando) {
        PwCampo(nome, { nome = it }, "Nome da empresa")
        PwRotulo("Cobrança da plataforma")
        PwSegmentado(MODELOS.map { it.second to null }, modelo, { modelo = it })
        PwRotulo("Gestor (dono)")
        PwCampo(gestor, { gestor = it }, "Nome do gestor")
        PwCampo(email, { email = it }, "E-mail do gestor", teclado = KeyboardType.Email)
        TextoDica("A empresa começa em teste. A senha do gestor é gerada e aparece no fim.")
        PwErro(erro)
        PwBotaoOuro(if (salvando) "Criando…" else "Criar empresa e gestor", icone = PwIcones.Check,
            habilitado = !salvando && nome.trim().length >= 2 && gestor.trim().length >= 2 && Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim()),
            onClick = {
                salvando = true; erro = null
                escopo.launch {
                    val org = Gestao.rpcJson("dev_salvar_organizacao", buildJsonObject {
                        put("p", buildJsonObject { put("nome", nome.trim()); put("status", "teste"); put("cobranca_modelo", MODELOS[modelo].first) })
                    })
                    val idOrg = (org.getOrNull() as? kotlinx.serialization.json.JsonPrimitive)?.content
                    if (idOrg == null) { salvando = false; erro = org.exceptionOrNull()?.message ?: "Não deu para criar a empresa."; return@launch }
                    val senha = senhaDono()
                    Gestao.criarConta(gestor.trim(), email.trim().lowercase(), senha, listOf("gestor"), null, organizacaoId = idOrg)
                        .onSuccess { idDono ->
                            Gestao.rpcJson("dev_salvar_organizacao", buildJsonObject {
                                put("p", buildJsonObject { put("id", idOrg); put("nome", nome.trim()); put("status", "teste"); put("cobranca_modelo", MODELOS[modelo].first); put("dono_id", idDono) })
                            })
                            acesso = "${nome.trim()}\nGestor: ${gestor.trim()}\nE-mail: ${email.trim().lowercase()}\nSenha: $senha\nSite: ${com.pwexpress.core.config.PwConfig.siteUrl}/gestor"
                        }
                        .onFailure { erro = "A empresa foi criada, mas a conta do gestor não: ${it.message}" }
                    salvando = false
                }
            })
    }
}

/** Dev → Faturamento (igual ao site): quanto cada empresa faturou e quanto rende para a plataforma. */
@Composable
internal fun DevFaturamentoG() {
    val c = Pw.cores
    var periodo by remember { mutableIntStateOf(0) }
    var dados by remember { mutableStateOf<JsonObject?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(periodo) {
        dados = null
        val hoje = LocalDate.now()
        val (de, ate) = when (periodo) {
            0 -> hoje.withDayOfMonth(1) to hoje
            1 -> hoje.minusMonths(1).withDayOfMonth(1) to hoje.withDayOfMonth(1).minusDays(1)
            2 -> hoje.minusDays(89) to hoje
            else -> hoje.withDayOfYear(1) to hoje
        }
        Gestao.rpcJson("dev_faturamento", buildJsonObject { put("p_de", de.toString()); put("p_ate", ate.toString()) })
            .onSuccess { dados = it as? JsonObject }.onFailure { erro = it.message }
    }
    PwFiltros(listOf("Este mês" to 0, "Mês passado" to 0, "90 dias" to 0, "Este ano" to 0), periodo, { periodo = it })
    PwErro(erro)
    val d = dados ?: run { PwCarregando(); return }
    val t = d.obj("totais")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwNumero("Receita da plataforma", moedaBr(t?.dbl("receita_dev") ?: 0.0), Modifier.weight(1f), icone = PwIcones.Landmark, destaque = true)
        PwNumero("Faturamento das empresas", moedaBr(t?.dbl("faturamento") ?: 0.0), Modifier.weight(1f), icone = PwIcones.TrendingUp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PwNumero("Empresas ativas", "${t?.int("ativas") ?: 0} de ${t?.int("empresas") ?: 0}", Modifier.weight(1f), icone = PwIcones.Building2)
        PwNumero("Rotas", "${t?.int("rotas") ?: 0}", Modifier.weight(1f), icone = PwIcones.Route)
    }
    val empresas = d.arr("empresas")
    if (empresas.isEmpty()) PwVazio("Nenhuma empresa no período.", icone = PwIcones.Receipt)
    empresas.forEach { e ->
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(e.str("nome") ?: "", color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(moedaBr(e.dbl("receita_dev") ?: 0.0), color = c.ouro, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            TextoDica(when (e.str("modelo")) { "percentual" -> "Percentual · ${numeroBr(e.dbl("pct") ?: 0.0)}%"; "licenca" -> "Licença · ${moedaBr(e.dbl("mensalidade"))}/mês"; else -> "Isenta" })
            PwLinhaInfo(PwIcones.TrendingUp, "Faturamento", moedaBr(e.dbl("faturamento") ?: 0.0))
            PwLinhaInfo(PwIcones.Bike, "Repasse aos motoboys", moedaBr(e.dbl("repasse") ?: 0.0))
            PwLinhaInfo(PwIcones.Route, "Rotas · entregas", "${e.int("rotas") ?: 0} · ${e.int("entregas") ?: 0}")
            PwLinhaInfo(PwIcones.Users, "Motoboys ativos", "${e.int("motoboys_ativos") ?: 0} de ${e.int("motoboys") ?: 0}")
            PwLinhaInfo(PwIcones.Building2, "Clientes ativos", "${e.int("clientes_ativos") ?: 0} de ${e.int("clientes") ?: 0}")
        }
    }
    Spacer(Modifier.height(4.dp))
    @Suppress("UNUSED_VARIABLE") val semUso = Color.Transparent to Modifier.width(0.dp)
}


/** Dev → Sistema: manda um aviso de teste para este celular e mostra onde o push travou. */
@Composable
internal fun DevPushG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var diag by remember { mutableStateOf<JsonObject?>(null) }
    var enviando by remember { mutableStateOf(false) }
    suspend fun ler() { diag = Gestao.rpcJson("dev_push_diagnostico").getOrNull() as? JsonObject }
    LaunchedEffect(Unit) { ler() }
    PwCartao {
        PwRotulo("Avisos (push)")
        val d = diag
        if (d != null) {
            PwLinhaInfo(PwIcones.Smartphone, "Este celular registrado", if (d.str("tem_token") == "true") "sim" else "não — abra o app e entre de novo")
            PwLinhaInfo(PwIcones.Globe, "Servidor do push", d.str("url") ?: "não configurado")
            d.arr("respostas").take(3).forEach { r ->
                val st = r.str("status")
                PwLinhaInfo(PwIcones.Info, "Último envio", when {
                    st == "200" -> "ok"
                    st == "401" -> "recusado (segredo) — atualize o site"
                    st != null -> "erro $st"
                    else -> r.str("erro") ?: "—"
                })
            }
        }
        PwBotaoOuro(if (enviando) "Enviando…" else "Testar aviso neste celular", icone = PwIcones.Send, habilitado = !enviando, onClick = {
            enviando = true
            escopo.launch {
                val r = Gestao.rpcJson("dev_testar_push").getOrNull() as? JsonObject
                enviando = false
                if (r?.str("ok") == "true") PwToast.mostrar("Enviado. O aviso deve chegar em segundos.", titulo = "Teste")
                else PwToast.mostrar(if (r?.str("motivo") == "sem_token") "Este celular ainda não registrou o push. Saia e entre de novo." else "Não deu para enviar.", TipoAviso.ERRO)
                kotlinx.coroutines.delay(4000); ler()
            }
        })
        TextoDica("Para a tela acender com rota nova: Ajustes → Corridas novas → permitir tela cheia.")
    }
    @Suppress("UNUSED_VARIABLE") val semUso = c
}
