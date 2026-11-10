package com.pwexpress.motoboy.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCabecalho
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwLinhaInfo
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.core.mapa.PwSeletorMapa
import com.pwexpress.core.mapa.coordenadaValida
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.corridas.ui.CartaoOfertas
import com.pwexpress.feature.tracking.Tracking
import com.pwexpress.feature.tracking.ui.PainelAparelho
import com.pwexpress.feature.tracking.ui.PainelEnvio
import kotlinx.coroutines.launch

private const val RAIO_MAX = 100f

/**
 * Ajustes do MotoApp (menu da foto): saúde do aparelho num resumo, permissões, corridas novas,
 * bateria e GPS, envio de localização e as informações do aparelho. O raio tem janela própria
 * (toque em "Sem raio definido" / "Raio N km" no Início).
 */
@Composable
fun ConfigMotoApp(onVoltar: () -> Unit, modifier: Modifier = Modifier) {
    PwPagina(modifier, padding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)) {
        PwCabecalho("Ajustes", onVoltar, subtitulo = "Aparelho, GPS e corridas")
        ResumoAparelho()
        PainelAparelho()
        CartaoOfertas()
        CartaoBateriaGps()
        PainelEnvio()
        CartaoIconeEmpresa()
        CartaoInfoAparelho()
    }
}

/** A empresa tem ícone próprio: o motoboy põe o atalho com ele na tela inicial do celular. */
@Composable
private fun CartaoIconeEmpresa() {
    val c = Pw.cores
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val marca by com.pwexpress.core.marca.MarcaEmpresa.marca.collectAsState()
    val versao by com.pwexpress.core.marca.MarcaEmpresa.versaoIcone.collectAsState()
    val escopo = rememberCoroutineScope()
    val m = marca?.takeIf { it.logoUrl != null } ?: return
    val pode = remember(versao) { com.pwexpress.core.marca.MarcaEmpresa.podeFixar(ctx) }
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.House, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("Ícone na tela inicial")
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.pwexpress.core.designsystem.PwSeloLogo(52.dp)
            Spacer(Modifier.width(12.dp))
            Text("Coloque o ícone da ${m.nome} na tela do celular. Ele abre o app direto.", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        PwBotaoOuro(if (pode) "Adicionar à tela inicial" else "Baixando o ícone…", icone = PwIcones.ImagePlus, altura = 50.dp, habilitado = pode,
            onClick = {
                escopo.launch {
                    val ok = com.pwexpress.core.marca.MarcaEmpresa.fixarNaTelaInicial(ctx)
                    if (ok) PwToast.mostrar("Confirme no aviso do celular para colocar o ícone.", titulo = "Ícone da ${m.nome}")
                    else PwToast.mostrar("Este celular não deixa adicionar atalhos.", TipoAviso.ERRO)
                }
            })
    }
}

/** Janela do raio de disponibilidade (aberta pelo atalho do Início ou pelo menu da foto). */
@Composable
fun JanelaRaio(onFechar: () -> Unit) {
    com.pwexpress.core.designsystem.PwTelaCheia("Raio de disponibilidade", onFechar, subtitulo = "Até onde você aceita buscar uma coleta") {
        CartaoRaio()
    }
}

/** Resumo no topo: quantos ajustes faltam, GPS, envio e fila, num relance. */
@Composable
private fun ResumoAparelho() {
    val c = Pw.cores
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val info by Tracking.info.collectAsState()
    var perms by remember { mutableStateOf(com.pwexpress.feature.tracking.ui.Permissoes.ler(ctx)) }
    var telaCheia by remember { mutableStateOf(com.pwexpress.feature.corridas.push.Alarme.podeTelaCheia(ctx)) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        perms = com.pwexpress.feature.tracking.ui.Permissoes.ler(ctx); telaCheia = com.pwexpress.feature.corridas.push.Alarme.podeTelaCheia(ctx)
        onPauseOrDispose { }
    }
    val itens = listOfNotNull(perms.localizacao, perms.semRestricaoBateria, telaCheia,
        if (android.os.Build.VERSION.SDK_INT >= 33) perms.notificacoes else null, if (android.os.Build.VERSION.SDK_INT >= 29) perms.segundoPlano else null)
    val faltam = itens.count { !it }
    val ok = faltam == 0
    val cor = if (ok) c.verde else c.ouro
    val forma = RoundedCornerShape(22.dp)
    Column(Modifier.fillMaxWidth().clip(forma).background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(cor.copy(alpha = 0.18f), c.vidro)))
        .border(1.dp, cor.copy(alpha = 0.45f), forma).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(androidx.compose.foundation.shape.CircleShape).background(cor.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(if (ok) PwIcones.ShieldCheck else PwIcones.ShieldAlert, null, tint = cor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(if (ok) "Aparelho pronto" else "Falta${if (faltam == 1) "" else "m"} $faltam ajuste${if (faltam == 1) "" else "s"}", color = c.texto, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 19.sp)
                Text(if (ok) "O GPS e as chamadas de corrida funcionam com a tela apagada." else "Libere abaixo para o Android não pausar o GPS nem esconder as corridas.", color = c.texto2, fontSize = 12.5.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            fun ha(t: Long?) = t?.let { val s = (System.currentTimeMillis() - it) / 1000; if (s < 60) "${s}s" else if (s < 3600) "${s / 60} min" else "${s / 3600} h" } ?: "—"
            listOf(
                Triple(PwIcones.Crosshair, info.ultimaPrecisao?.let { "± ${it.toInt()} m" } ?: "—", "precisão"),
                Triple(PwIcones.Send, ha(info.ultimoEnvio), "último envio"),
                Triple(PwIcones.Clock, info.naFila.toString(), "na fila"),
            ).forEach { (ic, v, r) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.fundo2.copy(alpha = 0.6f)).padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(ic, null, tint = c.ouro, modifier = Modifier.size(16.dp))
                    Text(v, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    Text(r, color = c.texto3, fontSize = 10.5.sp, maxLines = 1)
                }
            }
        }
    }
}

/** Bateria e GPS: economia de bateria e de quanto em quanto o GPS manda a posição. */
@Composable
private fun CartaoBateriaGps() {
    val c = Pw.cores
    val info by Tracking.info.collectAsState()
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.BatteryMedium, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("Bateria e GPS")
        }
        Spacer(Modifier.height(6.dp))
        com.pwexpress.core.designsystem.PwInterruptor("Economia de bateria", info.economia, { Tracking.definirEconomia(it) }, icone = PwIcones.BatteryLow,
            descricao = if (info.economia) "GPS a cada ${com.pwexpress.feature.tracking.Intervalos.corridaMs(true) / 1000} s em corrida: gasta menos, o mapa do gestor atualiza mais devagar."
                else "GPS a cada ${com.pwexpress.feature.tracking.Intervalos.corridaMs(false) / 1000} s em corrida: posição mais certa para o gestor e o cliente.")
        PwLinhaInfo(PwIcones.Radio, "Agora", info.modo.rotulo)
    }
}

/** O aparelho e o app (o mesmo que o Dev vê no console). */
@Composable
private fun CartaoInfoAparelho() {
    val c = Pw.cores
    PwCartao {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Smartphone, null, tint = c.ouro, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
            PwRotulo("Este aparelho")
        }
        Spacer(Modifier.height(6.dp))
        PwLinhaInfo(PwIcones.Smartphone, "Modelo", "${android.os.Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${android.os.Build.MODEL}")
        PwLinhaInfo(PwIcones.Info, "Android", "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        PwLinhaInfo(PwIcones.Download, "Versão do app", com.pwexpress.core.update.Atualizador.versaoAtual)
    }
}

/** Até onde o motoboy aceita ir buscar uma corrida, desenhado no mapa. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CartaoRaio() {
    val c = Pw.cores
    val perfil by Perfis.perfil.collectAsState()
    val info by Tracking.info.collectAsState()
    val escopo = rememberCoroutineScope()
    var km by remember(perfil?.raioKm) { mutableFloatStateOf((perfil?.raioKm ?: 0.0).toFloat()) }
    var fixo by remember(perfil?.raioModo) { mutableStateOf(perfil?.raioModo == "fixo") }
    var ponto by remember(perfil?.raioLat, perfil?.raioLng) {
        mutableStateOf(perfil?.raioLat?.let { la -> perfil?.raioLng?.let { lo -> la to lo } })
    }
    var escolhendo by remember { mutableStateOf(false) }
    var salvando by remember { mutableStateOf(false) }
    val eu = info.ultimaPosicao?.takeIf { coordenadaValida(it.first, it.second) }
    val centro = if (fixo) ponto else eu
    val mudou = km.toDouble() != (perfil?.raioKm ?: 0.0) || fixo != (perfil?.raioModo == "fixo") ||
        (fixo && ponto != perfil?.raioLat?.let { la -> perfil?.raioLng?.let { lo -> la to lo } })

    PwCartao {
        PwRotulo("Raio de disponibilidade")
        Spacer(Modifier.height(4.dp))
        Text(
            if (km < 1f) "Sem raio definido" else "Até ${km.toInt()} km",
            style = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 26.sp), color = c.texto,
        )
        Text(
            if (km < 1f) "Você recebe corridas de qualquer distância." else "Corridas com coleta mais longe que isso não chegam para você (a não ser as urgentes).",
            color = c.texto2, fontSize = 13.sp, fontFamily = Archivo,
        )
        // bolinha grande para puxar e trilho grosso (a mesma barra de todos os raios do app)
        com.pwexpress.core.mapa.PwBarraBolinha(km, { km = it }, 0f..RAIO_MAX, 1f, Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
            Text("De onde contar o raio", color = c.texto2, fontSize = 13.sp, modifier = Modifier.weight(1f))
            com.pwexpress.core.designsystem.PwDica("De onde contar o raio", "De onde eu estiver: o raio acompanha você, a partir da sua posição. Ponto fixo: conta a partir de um lugar que você escolhe no mapa, como a sua casa ou a base. Corridas urgentes podem chegar mesmo fora do raio.")
        }
        PwSegmentado(listOf("De onde eu estiver" to PwIcones.LocateFixed, "Ponto fixo" to PwIcones.MapPin), if (fixo) 1 else 0, { fixo = it == 1 })
        Spacer(Modifier.height(10.dp))
        val forma = RoundedCornerShape(18.dp)
        Box(Modifier.fillMaxWidth().height(220.dp).clip(forma).border(1.dp, c.borda, forma)) {
            PwMapa(
                paradas = emptyList(), rota = null, minhaPosicao = eu, modifier = Modifier.fillMaxWidth().height(220.dp),
                marcadores = listOfNotNull(if (fixo) ponto?.let { MarcadorMapa("base", it.first, it.second, "#FFFFFF", "Base", pino = false) } else null),
                circulo = centro?.takeIf { km >= 1f }?.let { it to km * 1000.0 },
                enquadrar = "${centro?.first},${centro?.second},${km.toInt()}",
            )
        }
        if (centro == null) {
            Spacer(Modifier.height(6.dp))
            Text(if (fixo) "Escolha o ponto no mapa." else "Fique disponível para o mapa saber onde você está.", color = c.texto3, fontSize = 12.5.sp)
        }
        if (fixo) {
            Spacer(Modifier.height(8.dp))
            PwBotaoVidro(if (ponto == null) "Escolher o ponto no mapa" else "Mudar o ponto", onClick = { escolhendo = true }, icone = PwIcones.MapPinned, modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(10.dp))
        PwBotaoOuro(
            if (salvando) "Salvando…" else "Salvar raio", icone = PwIcones.Check, altura = 52.dp, habilitado = mudou && !salvando && !(fixo && ponto == null),
            onClick = {
                salvando = true
                escopo.launch {
                    Perfis.definirRaio(km.toDouble().takeIf { it >= 1 }, if (fixo) "fixo" else "posicao", ponto?.first, ponto?.second)
                        .onSuccess { PwToast.mostrar(if (km < 1f) "Você recebe corridas de qualquer distância." else "Até ${km.toInt()} km ${if (fixo) "do ponto fixo" else "de onde você estiver"}.", titulo = "Raio salvo") }
                        .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
                    salvando = false
                }
            },
        )
    }
    if (escolhendo) {
        PwSeletorMapa(ponto ?: eu, onFechar = { escolhendo = false }, titulo = "Ponto fixo do raio") { la, lo ->
            escolhendo = false
            ponto = la to lo
        }
    }
}

/** Texto curto do raio para o atalho do Início. */
fun textoRaio(km: Double?, modo: String): String = if (km == null || km < 1) "Sem raio definido" else "Raio ${km.toInt()} km" + if (modo == "fixo") " · base" else ""
