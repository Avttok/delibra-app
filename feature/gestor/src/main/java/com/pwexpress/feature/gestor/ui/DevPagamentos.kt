package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.bool
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.obj
import com.pwexpress.core.json.str
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Dev: para onde vai o dinheiro do PIX/cartão (conta da plataforma ou da empresa), no geral e por empresa. */
@Composable
internal fun DevPagamentosG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var d by remember { mutableStateOf<JsonObject?>(null) }
    var versao by remember { mutableIntStateOf(0) }
    var erro by remember { mutableStateOf<String?>(null) }
    var token by remember { mutableStateOf("") }
    LaunchedEffect(versao) { Gestao.rpcJson("dev_pag_contas").onSuccess { d = it as? JsonObject }.onFailure { erro = it.message } }
    fun destino(org: String?, v: String) = escopo.launch {
        Gestao.rpcJson("dev_pag_destino", buildJsonObject { if (org == null) put("p_org", JsonNull) else put("p_org", org); put("p_destino", v) })
            .onSuccess { PwToast.mostrar("Destino salvo."); versao++ }.onFailure { erro = it.message }
    }
    val x = d ?: return
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.CreditCard, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("Pagamentos (Mercado Pago)")
        }
        Espaco(8)
        val plat = x.obj("plataforma")
        Text(if (plat != null) "Conta da plataforma ligada (final ${plat.str("final")}${if (plat.bool("teste")) " · teste" else ""})" else "Conta da plataforma desligada",
            color = if (plat != null) c.verde else c.texto3, fontWeight = FontWeight.SemiBold)
        Espaco(6)
        PwCampo(token, { token = it.trim() }, "Access Token da plataforma", dica = "APP_USR-… (fica só no servidor)", senha = true)
        Espaco(6)
        PwBotaoOuro(if (plat != null) "Trocar a conta da plataforma" else "Ligar a conta da plataforma", onClick = {
            escopo.launch {
                Gestao.rpcJson("dev_pag_salvar_plataforma", buildJsonObject { put("p_token", token); put("p_nome", "Plataforma") })
                    .onSuccess { token = ""; PwToast.mostrar("Conta da plataforma salva."); versao++ }.onFailure { erro = it.message }
            }
        }, icone = PwIcones.PlugZap, habilitado = token.length > 20, altura = 46.dp)
        Espaco(12)
        TextoDica("Padrão para todas as empresas:")
        val padrao = x.str("padrao") ?: "empresa"
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwPilula("Conta da empresa", PwIcones.Building2, { destino(null, "empresa") }, ligada = padrao == "empresa")
            PwPilula("Conta da plataforma", PwIcones.CodeXml, { destino(null, "plataforma") }, ligada = padrao == "plataforma")
        }
        x.arr("empresas").forEach { e ->
            Espaco(10)
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.str("nome").orEmpty(), color = c.texto, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    PwSelo(if (e.bool("conectada")) "conta ligada" else "sem conta", if (e.bool("conectada")) c.verde else c.texto3)
                }
                TextoDica("Cai em: ${if (e.str("destino") == "plataforma") "plataforma" else "empresa"} · recebido 30 dias ${moedaBr(e.dbl("recebido_30d"))}")
                val esc = e.str("escolha") ?: "padrao"
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("padrao" to "Padrão", "empresa" to "Empresa", "plataforma" to "Plataforma").forEach { (k, r) ->
                        PwPilula(r, if (esc == k) PwIcones.Check else PwIcones.Plus, { destino(e.str("id"), k) }, ligada = esc == k)
                    }
                }
            }
        }
        PwErro(erro)
    }
}
