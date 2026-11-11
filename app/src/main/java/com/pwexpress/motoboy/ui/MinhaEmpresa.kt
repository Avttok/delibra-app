package com.pwexpress.motoboy.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSeloLogo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.perfil.Perfis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream

private fun com.pwexpress.core.network.HttpResposta.json(): kotlinx.serialization.json.JsonElement? =
    corpo.takeIf { it.isNotBlank() && it != "null" }?.let { runCatching { kotlinx.serialization.json.Json.parseToJsonElement(it) }.getOrNull() }?.takeIf { it !is JsonNull }

private fun JsonObject.t(k: String) = (this[k] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content
private fun JsonObject.n(k: String) = (this[k] as? JsonPrimitive)?.doubleOrNull ?: 0.0

private val MODELO = mapOf("percentual" to "Percentual do faturamento", "licenca" to "Licença mensal", "isento" to "Isento")

/**
 * Minha empresa (dono ou Dev): ícone que entra no lugar do Delibra no app, no site e no atalho do
 * celular, e o plano da empresa com a plataforma (o mesmo do site em Minha empresa).
 */
@Composable
fun JanelaMinhaEmpresa(onFechar: () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    val marca by com.pwexpress.core.marca.MarcaEmpresa.marca.collectAsState()
    var org by remember { mutableStateOf<JsonObject?>(null) }
    var plano by remember { mutableStateOf<JsonObject?>(null) }
    var carregou by remember { mutableStateOf(false) }
    var enviando by remember { mutableStateOf(false) }
    var versao by remember { mutableIntStateOf(0) }
    var nomeApp by remember { mutableStateOf("") }
    var salvandoNome by remember { mutableStateOf(false) }

    LaunchedEffect(versao) {
        runCatching {
            val o = PwCore.rpc.chamar("minha_organizacao", JsonObject(emptyMap()))
            org = if (o.ok) o.json() as? JsonObject else null
            nomeApp = org?.t("nome_app").orEmpty()
            val p = PwCore.rpc.chamar("meu_plano", JsonObject(emptyMap()))
            plano = if (p.ok) p.json() as? JsonObject else null
        }
        carregou = true
    }

    suspend fun salvarLogo(url: String?, escala: Float? = null, extra: (kotlinx.serialization.json.JsonObjectBuilder.() -> Unit)? = null): Boolean {
        val r = PwCore.rpc.chamar("salvar_minha_organizacao", buildJsonObject { put("p", buildJsonObject {
            put("logo_url", url ?: ""); escala?.let { put("logo_escala", it) }
            if (url == null) put("logo_original_url", "")
            extra?.invoke(this)
        }) })
        if (r.ok) { Perfis.carregar(); versao++ }
        return r.ok
    }

    var editando by remember { mutableStateOf<Bitmap?>(null) }
    // imagem nova escolhida (vai também como "original", para reajustar depois sem reenviar)
    var originalNovo by remember { mutableStateOf<ByteArray?>(null) }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        escopo.launch {
            val bytes = withContext(Dispatchers.IO) { runCatching { ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull() }
            originalNovo = bytes
            editando = bytes?.let { runCatching { BitmapFactory.decodeByteArray(it, 0, it.size) }.getOrNull() }
            if (editando == null) PwToast.mostrar("Não deu para abrir esta imagem.", TipoAviso.ERRO)
        }
    }
    /** Reajustar sem enviar outra: abre a original guardada (ou a logo atual). */
    fun ajustar() {
        val url = org?.t("logo_original_url") ?: org?.t("logo_url") ?: return
        escopo.launch {
            originalNovo = null
            editando = withContext(Dispatchers.IO) { runCatching { java.net.URL(url).openStream().use { BitmapFactory.decodeStream(it) } }.getOrNull() }
            if (editando == null) PwToast.mostrar("Não deu para abrir a imagem atual.", TipoAviso.ERRO)
        }
    }
    fun enviar(png: ByteArray, escala: Float, brilhoCor: String?, forca: Float) {
        val id = org?.t("id") ?: return
        editando = null
        enviando = true
        escopo.launch {
            val ok = runCatching {
                val agora = System.currentTimeMillis()
                val caminho = "$id/icone-$agora.png"
                val up = PwCore.rpc.enviarArquivo("marcas", caminho, png, "image/png")
                if (!up.ok) error("envio ${up.codigo}")
                val orig = originalNovo?.let { b ->
                    val c2 = "$id/original-$agora.png"
                    if (PwCore.rpc.enviarArquivo("marcas", c2, b, "image/png").ok) "${PwConfig.supabaseUrl}/storage/v1/object/public/marcas/$c2" else null
                }
                salvarLogo("${PwConfig.supabaseUrl}/storage/v1/object/public/marcas/$caminho", escala) {
                    orig?.let { put("logo_original_url", it) }
                    put("brilho_cor", brilhoCor ?: ""); put("brilho", forca)
                }
            }.getOrDefault(false)
            enviando = false
            if (ok) PwToast.mostrar("O app e o site já usam o ícone novo.", titulo = "Ícone da empresa")
            else PwToast.mostrar("Não deu para salvar o ícone. Tente outra imagem.", TipoAviso.ERRO)
        }
    }
    editando?.let { b -> EditorIconeApp(b, marca?.cor ?: org?.t("cor"), marca?.nomeExibido ?: org?.t("nome_app") ?: org?.t("nome") ?: "Delibra",
        org?.t("logo_escala")?.toFloatOrNull() ?: 1.9f, onCancelar = { editando = null }, onPronto = ::enviar,
        brilhoCorInicial = org?.t("logo_brilho_cor"), brilhoInicial = org?.t("logo_brilho")?.toFloatOrNull() ?: 0.55f, fonteNome = org?.t("fonte_nome")); return }

    PwJanela("Minha empresa", onFechar) {
        if (!carregou) { PwCarregando(); return@PwJanela }
        val o = org
        if (o == null) { PwVazio("Esta conta não está em nenhuma empresa.", icone = PwIcones.Landmark); return@PwJanela }
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Image, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                PwRotulo("Ícone e nome")
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwSeloLogo(84.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(o.t("nome") ?: "", color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Black, fontSize = 19.sp)
                    Text(if (marca?.logoUrl != null) "Aparece no lugar do Delibra no app, no site e nas notificações." else "Sem ícone próprio: o app mostra o Delibra.", color = c.texto2, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                PwBotaoOuro(if (enviando) "Enviando…" else if (o.t("logo_url") != null) "Trocar ícone" else "Enviar ícone", icone = PwIcones.ImagePlus, altura = 50.dp,
                    habilitado = !enviando, modifier = Modifier.weight(1f),
                    onClick = { galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                if (o.t("logo_url") != null) PwBotaoIcone(PwIcones.SlidersHorizontal, "Ajustar sem enviar outra", onClick = { ajustar() })
                if (o.t("logo_url") != null) PwBotaoIcone(PwIcones.Trash2, "Voltar ao ícone do Delibra", onClick = {
                    escopo.launch { if (salvarLogo(null)) PwToast.mostrar("Voltou para o ícone do Delibra.", TipoAviso.INFO) }
                })
            }
            Spacer(Modifier.height(6.dp))
            Text("PNG com fundo transparente fica melhor; se a imagem tiver fundo, o editor tira. Ajuste zoom, posição e margem antes de salvar.", color = c.texto3, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Text("Fonte do nome no topo", color = c.texto2, fontSize = 13.sp)
            @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
            androidx.compose.foundation.layout.FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val atualF = com.pwexpress.core.designsystem.FonteApp.de(o.t("fonte_nome"))
                com.pwexpress.core.designsystem.FonteApp.entries.forEach { f ->
                    val ativo = f == atualF
                    Text(f.rotulo.substringBefore(" ("), fontFamily = f.familia, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (ativo) c.sobreOuro else c.texto,
                        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(99.dp)).background(if (ativo) c.ouro else c.vidro)
                            .border(1.dp, if (ativo) c.ouro else c.borda, androidx.compose.foundation.shape.RoundedCornerShape(99.dp))
                            .clickable { escopo.launch {
                                val r = PwCore.rpc.chamar("salvar_minha_organizacao", buildJsonObject { put("p", buildJsonObject { put("fonte", if (f == com.pwexpress.core.designsystem.FonteApp.ARCHIVO) "" else f.name) }) })
                                if (r.ok) { Perfis.carregar(); versao++ } else PwToast.mostrar("Não deu para salvar (${r.codigo}).", TipoAviso.ERRO)
                            } }.padding(horizontal = 12.dp, vertical = 7.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            com.pwexpress.core.designsystem.PwCampo(nomeApp, { nomeApp = it.take(30) }, "Nome do app (no lugar de Delibra)", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            val mudou = nomeApp.trim() != (o.t("nome_app") ?: "")
            PwBotaoOuro(if (salvandoNome) "Salvando…" else "Salvar nome do app", icone = PwIcones.Check, altura = 48.dp, habilitado = mudou && !salvandoNome,
                onClick = {
                    val v = nomeApp.trim()
                    if (v.isNotEmpty() && v.length < 2) { PwToast.mostrar("Use pelo menos 2 letras.", TipoAviso.ERRO); return@PwBotaoOuro }
                    salvandoNome = true
                    escopo.launch {
                        val r = PwCore.rpc.chamar("salvar_minha_organizacao", buildJsonObject { put("p", buildJsonObject { put("nome_app", v) }) })
                        salvandoNome = false
                        if (r.ok) { Perfis.carregar(); versao++; PwToast.mostrar(if (v.isEmpty()) "Voltou a se chamar Delibra." else "O app agora se chama $v.", titulo = "Nome do app") }
                        else PwToast.mostrar("Não deu para salvar (${r.codigo}).", TipoAviso.ERRO)
                    }
                })
            Text("Aparece no topo do app e do site, no atalho do celular e nas telas de entrar. Em branco = Delibra.", color = c.texto3, fontSize = 12.sp)
        }

        val p = plano
        if (p != null) {
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(PwIcones.BadgeCheck, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    PwRotulo("Seu plano")
                }
                Spacer(Modifier.height(6.dp))
                val modelo = p.t("modelo") ?: "isento"
                Text(MODELO[modelo] ?: modelo, color = c.ouro, fontFamily = Archivo, fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text(when (modelo) {
                    "percentual" -> "${numeroBr(p.n("pct"))}% sobre o valor das rotas concluídas no mês."
                    "licenca" -> "${moedaBr(p.n("mensalidade"))} por mês, vencimento todo dia ${(p["vencimento_dia"] as? JsonPrimitive)?.intOrNull ?: 10}."
                    else -> "Sua empresa não paga nada pela plataforma."
                }, color = c.texto2, fontSize = 13.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwNumero("Este mês", moedaBr(p.n("faturamento")), Modifier.weight(1f), icone = PwIcones.Receipt)
                PwNumero("A pagar", moedaBr(p.n("devido")), Modifier.weight(1f), icone = PwIcones.Landmark, destaque = true)
            }
            if ((p.t("modelo") ?: "isento") != "isento") PwLinhaInfo(PwIcones.CalendarClock, "Próximo vencimento",
                p.t("proximo_vencimento")?.let { java.time.LocalDate.parse(it).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) } ?: "—")
            val meses = (p["meses"] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
            if (meses.isNotEmpty()) PwCartao {
                PwRotulo("Últimos meses")
                meses.forEach { m ->
                    val mes = m.t("mes")?.let { runCatching { java.time.LocalDate.parse(it).format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.forLanguageTag("pt-BR"))) }.getOrNull() } ?: ""
                    PwLinhaInfo(null, mes.replaceFirstChar { it.uppercase() }, "${moedaBr(m.n("faturamento"))} · ${moedaBr(m.n("devido"))}")
                }
            }
        }
    }
}
