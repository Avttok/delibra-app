package com.pwexpress.motoboy.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.PreferenciaTema
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.alvoTour
import com.pwexpress.core.designsystem.PwAvatarConta
import com.pwexpress.core.designsystem.PwSelosCargo
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloat
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.core.update.Atualizador
import kotlinx.coroutines.launch

/** Qual lado do app está aberto (para quem é gestor e motoboy). */
enum class Painel { MOTOBOY, GESTOR }

/** Ações do topo: troca motoboy/gestor (quando tem os dois) e a foto, único caminho para o menu da conta. */
@Composable
fun RowScope.AcoesTopo(painel: Painel, podeTrocar: Boolean, onTrocar: (Painel) -> Unit, onConta: () -> Unit) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    if (podeTrocar) {
        val forma = RoundedCornerShape(99.dp)
        Row(Modifier.clip(forma).background(c.vidroForte).border(1.dp, c.borda, forma).padding(3.dp)) {
            listOf(Painel.MOTOBOY to PwIcones.Bike, Painel.GESTOR to PwIcones.ShieldCheck).forEach { (p, i) ->
                val ativo = p == painel
                Box(
                    Modifier.clip(forma).then(if (ativo) Modifier.background(c.gradOuro) else Modifier).clickable { onTrocar(p) }.padding(horizontal = 11.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) { Icon(i, if (p == Painel.MOTOBOY) "MotoApp" else "Painel do gestor", tint = if (ativo) c.sobreOuro else c.texto3, modifier = Modifier.size(17.dp)) }
            }
        }
    }
    if (perfil?.logico("chat_ligado", true) != false && perfil?.modulo("chat") != false) {
        com.pwexpress.core.chat.BotaoChatTopo { com.pwexpress.core.chat.Chat.abrir.value = true }
    }
    PwAvatarConta(perfil?.nome ?: "", perfil?.fotoUrl, teste, modifier = Modifier.alvoTour("conta"), tamanho = 42.dp, onClick = onConta, dev = perfil?.dev == true, gestor = perfil?.gestor == true, motoboy = perfil?.motoboy == true)
}

/**
 * Menu da conta, aberto pela foto (como no site): perfil, tema, modo teste,
 * configurações (gestor), trocar de lado, atualização e sair.
 */
@Composable
fun JanelaConta(
    email: String,
    painel: Painel,
    podeTrocar: Boolean,
    onFechar: () -> Unit,
    onPerfil: () -> Unit,
    onConfiguracoes: () -> Unit,
    onTrocar: (Painel) -> Unit,
    onSair: () -> Unit,
    onDev: () -> Unit = {},
) {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var erro by remember { mutableStateOf<String?>(null) }
    var trocandoTeste by remember { mutableStateOf(false) }
    val escopo = rememberCoroutineScope()

    var conquistas by remember { mutableStateOf(false) }
    var raio by remember { mutableStateOf(false) }
    var empresa by remember { mutableStateOf(false) }
    val ehMotoboy = painel == Painel.MOTOBOY && perfil?.motoboy == true
    com.pwexpress.core.designsystem.PwTelaCheia("Sua conta", onFechar) {
        // cabeçalho: foto grande, nome, e-mail, empresa e o cargo principal (toque edita o perfil)
        val forma = RoundedCornerShape(24.dp)
        Column(
            Modifier.fillMaxWidth().clip(forma).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.ouro.copy(alpha = 0.16f), c.vidro)))
                .border(1.dp, c.ouro.copy(alpha = 0.35f), forma).clickable(onClick = onPerfil).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwAvatarConta(perfil?.nome ?: email, perfil?.fotoUrl, teste, tamanho = 64.dp, dev = perfil?.dev == true, gestor = perfil?.gestor == true, motoboy = perfil?.motoboy == true)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(perfil?.nome ?: email.substringBefore("@"), color = c.texto, fontWeight = FontWeight.Black, fontFamily = Archivo, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(email, color = c.texto3, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    perfil?.organizacao?.let { Text(it, color = c.ouro, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    Spacer(Modifier.height(6.dp))
                    PwSelosCargo(perfil?.dev == true, perfil?.gestor == true && perfil?.dev != true, perfil?.motoboy == true)
                }
                Icon(PwIcones.Pencil, "Editar perfil", tint = c.texto3, modifier = Modifier.size(18.dp))
            }
        }

        // tudo em blocos de 2 colunas: atalhos, liga/desliga e ajuda
        val escuro = c.escuro
        val estilo by TemaApp.estilo.collectAsState()
        val mono = estilo == com.pwexpress.core.designsystem.EstiloTema.PRETO_NO_BRANCO || estilo == com.pwexpress.core.designsystem.EstiloTema.BRANCO_NO_PRETO
        val vermelho by TemaApp.vermelho.collectAsState()
        val info by com.pwexpress.feature.tracking.Tracking.info.collectAsState()
        var oculto by remember { mutableStateOf(perfil?.devOculto == true) }
        val ctxAjuda = androidx.compose.ui.platform.LocalContext.current
        fun alternarTeste(lig: Boolean) {
            if (trocandoTeste) return
            trocandoTeste = true
            escopo.launch {
                Perfis.definirModoTeste(lig).onFailure { erro = it.message }.onSuccess {
                    erro = null
                    PwToast.mostrar(if (lig) "Tudo violeta: só corridas e ganhos de teste." else "De volta ao normal: tudo vale de verdade.", TipoAviso.INFO, if (lig) "Modo teste ligado" else "Modo teste desligado")
                }
                trocandoTeste = false
            }
        }
        val blocos = buildList<Bloco> {
            if (perfil?.motoboy == true) add(Bloco(PwIcones.Trophy, "Conquistas", onClick = { conquistas = true }))
            add(Bloco(PwIcones.SlidersHorizontal, "Ajustes", onClick = onConfiguracoes))
            if (ehMotoboy) add(Bloco(PwIcones.Radar, "Raio", onClick = { raio = true }))
            if (perfil?.dev == true || (perfil?.gestor == true && perfil?.supervisor != true)) add(Bloco(PwIcones.Landmark, "Minha empresa", onClick = { empresa = true }))
            if (perfil?.dev == true) add(Bloco(PwIcones.CodeXml, "Console Dev", c.dev, onClick = onDev))
            if (perfil?.dev == true) add(Bloco(PwIcones.Layers, "Modo Dev", c.dev, sub = "Trocar de empresa", onClick = { onFechar(); ModoDevApp.abrir() }))
            if (!mono) add(Bloco(if (escuro) PwIcones.Moon else PwIcones.Sun, if (escuro) "Tema noturno" else "Tema claro", ligado = escuro,
                onClick = { TemaApp.definir(if (escuro) PreferenciaTema.CLARO else PreferenciaTema.ESCURO) }))
            if (perfil?.dev == true) add(Bloco(PwIcones.Palette, "Tema vermelho", c.dev, ligado = vermelho, onClick = {
                val lig = !vermelho
                TemaApp.definirVermelho(lig)
                escopo.launch { Perfis.definirCorTema(if (lig) "vermelho" else "ouro").onFailure { TemaApp.definirVermelho(!lig); PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) } }
            }))
            if (perfil?.gestor == true && perfil?.modulo("modo_teste") != false) add(Bloco(PwIcones.FlaskConical, "Modo teste", c.violeta, ligado = teste, onClick = { alternarTeste(!teste) }))
            if (ehMotoboy) add(Bloco(PwIcones.BatteryLow, "Economia", sub = if (info.economia) "GPS mais espaçado" else "GPS normal", ligado = info.economia,
                onClick = { com.pwexpress.feature.tracking.Tracking.definirEconomia(!info.economia) }))
            if (perfil?.dev == true) add(Bloco(if (oculto) PwIcones.EyeOff else PwIcones.Eye, "Ficar oculto", c.dev, ligado = oculto, onClick = {
                val lig = !oculto
                oculto = lig
                escopo.launch { Perfis.definirOculto(lig).onFailure { oculto = !lig; PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) } }
            }))
            add(Bloco(PwIcones.Info, "Ajuda", sub = "Passo a passo", onClick = {
                runCatching { ctxAjuda.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(com.pwexpress.core.config.PwConfig.siteUrl + "/ajuda?p=" + if (painel == Painel.GESTOR) "gestor" else "motoboy"))) }
            }))
            add(Bloco(PwIcones.Navigation, "Ver o tour", onClick = {
                onFechar()
                if (painel == Painel.GESTOR) com.pwexpress.core.designsystem.PwTour.abrir("gestor-app", ToursApp.gestor)
                else com.pwexpress.core.designsystem.PwTour.abrir("motoboy", ToursApp.motoboy)
            }))
            if (perfil?.modulo("atualizacao_app") != false) add(Bloco(PwIcones.Download, "Atualização", sub = "Versão ${com.pwexpress.core.update.Atualizador.versaoAtual}",
                onClick = { onFechar(); Atualizador.abrirJanela(); Atualizador.verificar(automatico = false) }))
            add(Bloco(PwIcones.LogOut, "Sair", c.vermelho, onClick = onSair))
        }
        blocos.chunked(2).forEach { linha ->
            Row(Modifier.fillMaxWidth().height(androidx.compose.foundation.layout.IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                linha.forEach { b -> BlocoAtalho(b, Modifier.weight(1f).fillMaxHeight()) }
                if (linha.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        PwErro(erro)
        GrupoMenu("Cores do app") { EscolherCores(estilo) }
        // easter egg, igual ao rodapé do site
        Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Text("Um projeto de irmão para irmão!", color = c.texto3, fontSize = 11.5.sp, fontFamily = Archivo)
            Spacer(Modifier.width(5.dp))
            CoracaoBatendo()
        }
    }
    if (conquistas) com.pwexpress.core.perfil.JanelaConquistas(onFechar = { conquistas = false })
    if (raio) JanelaRaio(onFechar = { raio = false })
    if (empresa) JanelaMinhaEmpresa(onFechar = { empresa = false })
}

/** Um bloco do menu: atalho, ou liga/desliga quando `ligado` não é null. */
private data class Bloco(val icone: ImageVector, val texto: String, val cor: Color? = null, val sub: String? = null, val ligado: Boolean? = null, val onClick: () -> Unit)

/** Bloco grande (ícone em cima, nome embaixo). Os de liga/desliga mostram o estado no canto. */
@Composable
private fun BlocoAtalho(b: Bloco, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val cor = b.cor ?: c.ouro
    val forma = RoundedCornerShape(18.dp)
    val ligado = b.ligado == true
    Column(
        modifier.clip(forma).background(if (ligado) cor.copy(alpha = 0.12f) else c.vidro).border(1.dp, if (ligado) cor.copy(alpha = 0.55f) else c.borda, forma)
            .clickable(onClick = b.onClick).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(cor.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(b.icone, null, tint = cor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            if (b.ligado != null) Box(Modifier.size(width = 34.dp, height = 20.dp).clip(RoundedCornerShape(99.dp)).background(if (ligado) cor else c.bordaForte).padding(3.dp),
                contentAlignment = if (ligado) Alignment.CenterEnd else Alignment.CenterStart) {
                Box(Modifier.size(14.dp).clip(androidx.compose.foundation.shape.CircleShape).background(if (ligado) c.sobreOuro else c.texto3))
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(b.texto, color = if (b.cor == c.vermelho) c.vermelho else c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val sub = b.sub ?: b.ligado?.let { if (it) "Ligado" else "Desligado" }
            if (sub != null) Text(sub, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Grupo do menu: título pequeno e os itens num cartão só (tudo alinhado). */
@Composable
private fun GrupoMenu(titulo: String, conteudo: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    val c = Pw.cores
    Column(Modifier.fillMaxWidth()) {
        Text(titulo, color = c.texto3, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(18.dp)).padding(4.dp), content = conteudo)
    }
}

@Composable
private fun ItemMenu(
    icone: ImageVector,
    texto: String,
    subtitulo: String? = null,
    cor: Color? = null,
    corTexto: Color? = null,
    onClick: () -> Unit,
    direita: @Composable RowScope.() -> Unit = {},
) {
    val c = Pw.cores
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, null, tint = cor ?: c.ouro, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(texto, color = corTexto ?: c.texto, fontWeight = FontWeight.SemiBold, fontFamily = Archivo, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitulo != null) Text(subtitulo, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = direita)
    }
}

@Composable
private fun Interruptor(ligado: Boolean, cor: Color? = null, onMudar: (Boolean) -> Unit) {
    val c = Pw.cores
    Switch(
        checked = ligado, onCheckedChange = onMudar,
        colors = SwitchDefaults.colors(checkedThumbColor = c.sobreOuro, checkedTrackColor = cor ?: c.ouro, uncheckedTrackColor = c.bordaForte, uncheckedBorderColor = Color.Transparent),
    )
}

@Composable
private fun CoracaoBatendo() {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "coracao")
    val escala by t.animateFloat(
        1f, 1.18f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween<Float>(800), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "batida",
    )
    Icon(PwIcones.Heart, "com amor", tint = Pw.cores.ouro, modifier = Modifier.size(12.dp).scale(escala))
}


/** Bolinhas com as cores do app: dourado, azul, verde, preto no branco e branco no preto. */
@Composable
private fun EscolherCores(atual: com.pwexpress.core.designsystem.EstiloTema) {
    val c = Pw.cores
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Palette, null, tint = c.ouro, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Cores do app", color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(atual.rotulo, color = c.texto3, fontSize = 12.5.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            com.pwexpress.core.designsystem.EstiloTema.entries.forEach { e ->
                val ativo = e == atual
                val anel by androidx.compose.animation.core.animateDpAsState(if (ativo) 3.dp else 1.dp, label = "anel")
                Box(
                    Modifier.size(44.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .border(anel, if (ativo) c.ouro else c.bordaForte, androidx.compose.foundation.shape.CircleShape)
                        .clickable { com.pwexpress.core.designsystem.TemaApp.definirEstilo(e) }
                        .padding(5.dp).clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(e.fundoAmostra, e.fundoAmostra, e.amostra, e.amostra), start = androidx.compose.ui.geometry.Offset.Zero, end = androidx.compose.ui.geometry.Offset.Infinite)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (ativo) Icon(PwIcones.Check, e.rotulo, tint = if (e == com.pwexpress.core.designsystem.EstiloTema.PRETO_NO_BRANCO) Color.White else Color.Black, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
