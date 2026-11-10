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

    com.pwexpress.core.designsystem.PwTelaCheia("Sua conta", onFechar) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // cabeça: foto, nome e e-mail (toque edita o perfil)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable(onClick = onPerfil).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PwAvatarConta(perfil?.nome ?: email, perfil?.fotoUrl, teste, tamanho = 48.dp, dev = perfil?.dev == true, gestor = perfil?.gestor == true, motoboy = perfil?.motoboy == true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(perfil?.nome ?: email.substringBefore("@"), color = c.texto, fontWeight = FontWeight.Bold, fontFamily = Archivo, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(email, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        PwSelosCargo(perfil?.dev == true, perfil?.gestor == true && perfil?.dev != true, perfil?.motoboy == true)
                    }
                }
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(c.borda))
                if (perfil?.motoboy == true) {
                    com.pwexpress.core.perfil.FaixaConquistas(onVerTodas = onPerfil)
                    Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(c.borda))
                }

                ItemMenu(PwIcones.Settings2, "Ajustes", subtitulo = if (painel == Painel.GESTOR) "Do sistema inteiro" else "Raio, aparelho e GPS", onClick = onConfiguracoes)
                val escuro = c.escuro
                val estilo by TemaApp.estilo.collectAsState()
                val mono = estilo == com.pwexpress.core.designsystem.EstiloTema.PRETO_NO_BRANCO || estilo == com.pwexpress.core.designsystem.EstiloTema.BRANCO_NO_PRETO
                if (!mono) ItemMenu(if (escuro) PwIcones.Moon else PwIcones.Sun, if (escuro) "Tema noturno" else "Tema claro",
                    onClick = { TemaApp.definir(if (escuro) PreferenciaTema.CLARO else PreferenciaTema.ESCURO) }) {
                    Interruptor(escuro) { TemaApp.definir(if (it) PreferenciaTema.ESCURO else PreferenciaTema.CLARO) }
                }
                EscolherCores(estilo)
                if (perfil?.dev == true) {
                    val vermelho by TemaApp.vermelho.collectAsState()
                    ItemMenu(PwIcones.Palette, "Tema vermelho", subtitulo = if (vermelho) "A cor do Dev" else "Dourado, como todos", cor = c.dev, onClick = {}) {
                        Interruptor(vermelho, c.dev) { lig ->
                            TemaApp.definirVermelho(lig)
                            escopo.launch { Perfis.definirCorTema(if (lig) "vermelho" else "ouro").onFailure { TemaApp.definirVermelho(!lig); PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) } }
                        }
                    }
                }
                if (perfil?.gestor == true && perfil?.modulo("modo_teste") != false) {
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
                    ItemMenu(PwIcones.FlaskConical, "Modo teste", subtitulo = if (teste) "Só corridas e ganhos de teste" else null, cor = c.violeta, onClick = { alternarTeste(!teste) }) {
                        Interruptor(teste, c.violeta) { alternarTeste(it) }
                    }
                }
                PwErro(erro)
                if (painel == Painel.MOTOBOY && perfil?.motoboy == true) {
                    val info by com.pwexpress.feature.tracking.Tracking.info.collectAsState()
                    ItemMenu(PwIcones.BatteryLow, "Economia de bateria", subtitulo = if (info.economia) "GPS a cada ${com.pwexpress.feature.tracking.Intervalos.corridaMs(true) / 1000} s em corrida" else null,
                        onClick = { com.pwexpress.feature.tracking.Tracking.definirEconomia(!info.economia) }) {
                        Interruptor(info.economia) { com.pwexpress.feature.tracking.Tracking.definirEconomia(it) }
                    }
                }
                val ctxAjuda = androidx.compose.ui.platform.LocalContext.current
                ItemMenu(PwIcones.Info, "Central de ajuda", onClick = {
                    runCatching { ctxAjuda.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse(com.pwexpress.core.config.PwConfig.siteUrl + "/ajuda?p=" + if (painel == Painel.GESTOR) "gestor" else "motoboy"))) }
                })
                ItemMenu(PwIcones.Navigation, "Ver o tour", onClick = {
                    onFechar()
                    if (painel == Painel.GESTOR) com.pwexpress.core.designsystem.PwTour.abrir("gestor-app", ToursApp.gestor)
                    else com.pwexpress.core.designsystem.PwTour.abrir("motoboy", ToursApp.motoboy)
                })
                if (perfil?.modulo("atualizacao_app") != false) ItemMenu(PwIcones.Download, "Verificar atualização", onClick = { onFechar(); Atualizador.abrirJanela(); Atualizador.verificar(automatico = false) })
                if (perfil?.dev == true) ItemMenu(PwIcones.CodeXml, "Console Dev", cor = c.dev, onClick = onDev)
                Box(Modifier.fillMaxWidth().padding(vertical = 4.dp).height(1.dp).background(c.borda))
                ItemMenu(PwIcones.LogOut, "Sair da conta", cor = c.vermelho, corTexto = c.vermelho, onClick = onSair)
                // easter egg, igual ao rodapé do site
                Row(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("Um projeto de irmão para irmão!", color = c.texto3, fontSize = 11.5.sp, fontFamily = Archivo)
                    Spacer(Modifier.width(5.dp))
                    CoracaoBatendo()
                }
            }
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp)) {
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
