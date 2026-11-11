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
import androidx.compose.foundation.layout.padding
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

private val MODELO = mapOf("percentual" to "Percentual do faturamento", "licenca" to "Licença", "isento" to "Isento")

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
    var nomeTopo by remember { mutableStateOf("") }
    var salvandoNome by remember { mutableStateOf(false) }

    LaunchedEffect(versao) {
        runCatching {
            val o = PwCore.rpc.chamar("minha_organizacao", JsonObject(emptyMap()))
            org = if (o.ok) o.json() as? JsonObject else null
            nomeApp = org?.t("nome_app").orEmpty()
            nomeTopo = org?.t("nome_topo").orEmpty()
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
        val atualF = com.pwexpress.core.designsystem.FonteApp.de(o.t("fonte_nome"))
        var fonte by remember(o) { mutableStateOf(atualF) }
        val topoTxt = nomeTopo.trim().ifEmpty { nomeApp.trim().ifEmpty { o.t("nome") ?: "Delibra" } }
        val appTxt = nomeApp.trim().ifEmpty { o.t("nome") ?: "Delibra" }

        // ------------------------------------------------ como fica (ao vivo, com o que está mudando)
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Eye, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); PwRotulo("Como fica")
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).background(c.fundo2)
                .border(1.dp, c.borda, androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                PwSeloLogo((26 * (o.t("logo_escala")?.toFloatOrNull() ?: 1.9f)).dp)
                Spacer(Modifier.width(8.dp))
                Text(topoTxt, color = c.texto, fontFamily = fonte.familia, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            Spacer(Modifier.height(10.dp))
            PwLinhaInfo(PwIcones.PanelTop, "No topo, ao lado da logo", topoTxt)
            PwLinhaInfo(PwIcones.Bell, "Atalho, avisos e títulos", appTxt)
        }

        // ------------------------------------------------ ícone
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Image, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); PwRotulo("Ícone")
            }
            Spacer(Modifier.height(10.dp))
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
            Text("PNG com fundo transparente fica melhor; se tiver fundo, o editor tira. Ajuste zoom, posição, margem, brilho e tamanho no topo antes de salvar.", color = c.texto3, fontSize = 12.sp)
        }

        // ------------------------------------------------ nomes e fonte
        PwCartao {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(PwIcones.Type, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); PwRotulo("Nomes e fonte")
            }
            Spacer(Modifier.height(10.dp))
            com.pwexpress.core.designsystem.PwCampo(nomeApp, { nomeApp = it.take(30) }, "Nome do app", Modifier.fillMaxWidth(), dica = "Ex.: PW Express · atalho, avisos e entrada. Vazio = Delibra")
            Spacer(Modifier.height(8.dp))
            com.pwexpress.core.designsystem.PwCampo(nomeTopo, { nomeTopo = it.take(30) }, "Texto ao lado da logo (topo)", Modifier.fillMaxWidth(),
                dica = "Se a logo já tem parte do nome (ex.: PW), escreva só o resto (Express). Vazio = o nome do app")
            Spacer(Modifier.height(12.dp))
            Text("Fonte do texto do topo", color = c.texto2, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            com.pwexpress.core.designsystem.FonteApp.entries.chunked(2).forEach { par ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    par.forEach { f ->
                        val ativo = f == fonte
                        val forma = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
                        Column(Modifier.weight(1f).clip(forma).background(if (ativo) c.ouro.copy(alpha = 0.16f) else c.vidro)
                            .border(if (ativo) 2.dp else 1.dp, if (ativo) c.ouro else c.borda, forma).clickable { fonte = f }.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(topoTxt, fontFamily = f.familia, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = c.texto, maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(f.rotulo, color = c.texto3, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                    if (par.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            val mudou = nomeApp.trim() != (o.t("nome_app") ?: "") || nomeTopo.trim() != (o.t("nome_topo") ?: "") || fonte != atualF
            PwBotaoOuro(if (salvandoNome) "Salvando…" else "Salvar", icone = PwIcones.Check, altura = 50.dp, habilitado = mudou && !salvandoNome, modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val v = nomeApp.trim()
                    if (v.isNotEmpty() && v.length < 2) { PwToast.mostrar("O nome do app precisa de pelo menos 2 letras.", TipoAviso.ERRO); return@PwBotaoOuro }
                    salvandoNome = true
                    escopo.launch {
                        val r = PwCore.rpc.chamar("salvar_minha_organizacao", buildJsonObject { put("p", buildJsonObject {
                            put("nome_app", v); put("nome_topo", nomeTopo.trim())
                            put("fonte", if (fonte == com.pwexpress.core.designsystem.FonteApp.ARCHIVO) "" else fonte.name)
                        }) })
                        salvandoNome = false
                        if (r.ok) { Perfis.carregar(); versao++; PwToast.mostrar("O app e o site já mostram assim.", titulo = "Salvo") }
                        else PwToast.mostrar(if (r.codigo == 403) "Só o dono da empresa muda isto." else "Não deu para salvar (${r.codigo}).", TipoAviso.ERRO)
                    }
                })
        }

        RecebimentosApp()

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
                    "licenca" -> when (p.t("periodo")) {
                        "anual" -> "${moedaBr(p.n("mensalidade"))} por ano."
                        "vitalicia" -> "Licença vitalícia (${moedaBr(p.n("mensalidade"))}, paga uma vez)."
                        else -> "${moedaBr(p.n("mensalidade"))} por mês, vencimento todo dia ${(p["vencimento_dia"] as? JsonPrimitive)?.intOrNull ?: 10}."
                    }
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


/** Receber dos clientes (PIX e cartão) pelo Mercado Pago: o dono liga a conta da empresa. */
@Composable
private fun RecebimentosApp() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var conta by remember { mutableStateOf<JsonObject?>(null) }
    var token by remember { mutableStateOf("") }
    var ocupado by remember { mutableStateOf(false) }
    var teste by remember { mutableStateOf<String?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    LaunchedEffect(versao) {
        val r = runCatching { PwCore.rpc.chamar("pag_minha_conta", JsonObject(emptyMap())) }.getOrNull()
        conta = if (r?.ok == true) r.json() as? JsonObject else null
    }
    val k = conta ?: return
    fun testar() {
        escopo.launch {
            ocupado = true
            teste = runCatching { PwCore.rpc.site("/api/pagamentos/testar", JsonObject(emptyMap())).json() as? JsonObject }.getOrNull()?.let { j ->
                if ((j["ok"] as? JsonPrimitive)?.content == "true") "Conectada: ${(j["nome"] as? JsonPrimitive)?.content}" else (j["erro"] as? JsonPrimitive)?.content ?: "Não deu para testar."
            } ?: "Sem resposta do site."
            ocupado = false; versao++
        }
    }
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.CreditCard, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); PwRotulo("Receber dos clientes (PIX e cartão)")
        }
        Spacer(Modifier.height(8.dp))
        Text(if (k.t("destino") == "plataforma") "Pela escolha da plataforma, os pagamentos dos seus clientes caem na conta da plataforma, que repassa a você."
             else "Os pagamentos dos seus clientes caem direto na sua conta do Mercado Pago. O pedido segue sozinho quando o pagamento é aprovado.",
            color = c.texto2, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        val ligada = k.t("conectada") == "true"
        PwLinhaInfo(if (ligada) PwIcones.CircleCheck else PwIcones.Unplug, "Conta Mercado Pago",
            if (ligada) "ligada · …${k.t("final")}${if (k.t("teste") == "true") " · teste" else ""}" else "não ligada", destaque = ligada)
        k.t("nome")?.let { Text(it, color = c.texto3, fontSize = 12.sp) }
        Spacer(Modifier.height(8.dp))
        com.pwexpress.core.designsystem.PwCampo(token, { token = it.trim() }, "Access Token de produção", Modifier.fillMaxWidth(), senha = true,
            dica = "Mercado Pago → Seu negócio → Configurações → Credenciais. Fica só no servidor.")
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwBotaoOuro(if (ocupado) "Salvando…" else "Ligar conta", icone = PwIcones.PlugZap, altura = 48.dp, habilitado = token.isNotBlank() && !ocupado, modifier = Modifier.weight(1f), onClick = {
                escopo.launch {
                    ocupado = true
                    val r = PwCore.rpc.chamar("pag_salvar_conta", buildJsonObject { put("p_token", token); put("p_public_key", JsonNull); put("p_nome", JsonNull) })
                    ocupado = false
                    if (r.ok) { token = ""; versao++; testar() }
                    else PwToast.mostrar(if (r.corpo.contains("token_invalido")) "Isso não parece um Access Token (começa com APP_USR- ou TEST-)." else "Não deu para salvar (${r.codigo}).", TipoAviso.ERRO)
                }
            })
            if (ligada) PwBotaoIcone(PwIcones.Link2, "Testar", onClick = { testar() })
            if (ligada) PwBotaoIcone(PwIcones.Unplug, "Desligar", onClick = {
                escopo.launch { if (PwCore.rpc.chamar("pag_salvar_conta", buildJsonObject { put("p_token", "") }).ok) { versao++; PwToast.mostrar("Conta desligada.") } }
            })
        }
        teste?.let { Spacer(Modifier.height(6.dp)); Text(it, color = c.texto2, fontSize = 12.sp) }
    }
}
