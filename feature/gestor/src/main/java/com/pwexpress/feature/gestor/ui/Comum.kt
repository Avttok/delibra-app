package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwPaleta
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.designsystem.TextoAjustado
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.text.TextStyle
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.Status
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val BR: Locale = Locale.forLanguageTag("pt-BR")

/** O módulo está ligado para esta conta? (o Dev liga/desliga no console; o Dev sempre vê) */
@Composable
internal fun modulo(chave: String): Boolean {
    val p by com.pwexpress.core.perfil.Perfis.perfil.collectAsState()
    return p?.modulo(chave) ?: true
}

internal fun corStatus(s: String, c: PwPaleta): Color = when (s) {
    Status.MONTADA -> c.ouro
    Status.AGENDADA, Status.RESERVADA -> c.azul
    Status.AGUARDANDO -> c.ouroClaro
    Status.AGUARDANDO_PAGAMENTO -> Color(0xFF38BDF8)
    Status.SEM_MOTOBOY -> c.vermelho
    Status.ACEITA, Status.EM_ANDAMENTO -> c.ouro
    Status.CONCLUIDA -> c.verde
    else -> c.texto3
}

internal fun corMotoboy(s: String?, c: PwPaleta): Color = when (s) {
    "disponivel" -> c.verde
    "em_corrida" -> c.ouro
    else -> c.texto3
}

/**
 * Quanto o motoboy aparece no mapa: em serviço 100%; offline vai esmaecendo conforme o tempo sem ser visto
 * e some depois de [someH] horas (null = não mostrar).
 */
internal fun opacidadeNoMapa(status: String?, posicaoEm: Long?, someH: Double, esmaecer: Boolean, agora: Long = System.currentTimeMillis()): Float? {
    if (status != null && status != "offline") return 1f
    val horas = posicaoEm?.let { (agora - it) / 3_600_000.0 } ?: return null
    if (someH > 0 && horas > someH) return null
    if (!esmaecer) return 0.6f
    val t = if (someH > 0) (horas / someH).coerceIn(0.0, 1.0) else 0.0
    return (0.85 - 0.6 * t).toFloat()
}

/** Cor do anel do motoboy no mapa (modo teste segue o violeta). */
internal fun corMotoboyMapa(s: String?, teste: Boolean, c: PwPaleta): Color =
    if (teste && s != null && s != "offline") (if (s == "em_corrida") c.violeta.copy(red = c.violeta.red * 0.8f) else c.violeta) else corMotoboy(s, c)

internal val rotuloMotoboy = mapOf("disponivel" to "Disponível", "em_corrida" to "Em corrida", "offline" to "Offline")

internal fun hora(ms: Long?): String = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm")) } ?: "—"
internal fun dataHora(ms: Long?): String = ms?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) } ?: "—"
internal fun km(m: Int?): String = when { m == null -> "—"; m < 1000 -> "$m m"; else -> "%.1f km".format(BR, m / 1000.0) }
internal fun duracao(s: Int?): String = when { s == null -> "—"; s < 3600 -> "${s / 60} min"; else -> "${s / 3600} h ${(s % 3600) / 60} min" }

internal fun quando(ms: Long): String {
    val z = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault())
    val hoje = LocalDate.now()
    val dia = when (z.toLocalDate()) { hoje -> "hoje"; hoje.plusDays(1) -> "amanhã"; else -> z.format(DateTimeFormatter.ofPattern("EEE, dd/MM", BR)) }
    return "$dia às ${z.format(DateTimeFormatter.ofPattern("HH:mm"))}"
}

internal fun relativo(ms: Long, agora: Long = System.currentTimeMillis()): String {
    val min = ((ms - agora) / 60_000).toInt()
    val a = kotlin.math.abs(min)
    val t = when { a < 60 -> "$a min"; a < 1440 -> "${a / 60} h${if (a % 60 > 0) " ${a % 60} min" else ""}"; else -> "${a / 1440} d${if ((a % 1440) / 60 > 0) " ${(a % 1440) / 60} h" else ""}" }
    return if (min >= 0) "em $t" else "há $t"
}

internal fun haQuanto(ms: Long?, agora: Long = System.currentTimeMillis()): String {
    if (ms == null) return "—"
    val s = ((agora - ms) / 1000).coerceAtLeast(0)
    return when { s < 60 -> "agora"; s < 3600 -> "há ${s / 60} min"; s < 86400 -> "há ${s / 3600} h"; else -> "há ${s / 86400} d" }
}

/** Cartão de corrida das listas (mesmo desenho do site). */
@Composable
internal fun CartaoCorridaG(corrida: CorridaG, dados: Gestao.Dados, onClick: () -> Unit, marcado: Boolean? = null) {
    val c = Pw.cores
    val forma = RoundedCornerShape(18.dp)
    val coleta = corrida.paradas.firstOrNull()
    val entregas = corrida.paradas.filter { !it.coleta }
    val oferta = dados.ofertas.firstOrNull { it.corridaId == corrida.id }
    Column(
        Modifier.fillMaxWidth().clip(forma).background(if (marcado == true) c.ouro.copy(alpha = 0.10f) else c.vidroSutil)
            .border(1.dp, if (marcado == true) c.ouro else c.borda, forma)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (marcado != null) {
                Icon(if (marcado) PwIcones.CircleCheck else PwIcones.Package, null, tint = if (marcado) c.ouro else c.texto3, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(corrida.numeroExibicao, fontFamily = ArchivoLargo, fontWeight = FontWeight.Black, fontSize = 16.sp, color = c.texto, maxLines = 1)
            if (corrida.urgente) { Spacer(Modifier.width(6.dp)); Icon(PwIcones.Zap, "Urgente", tint = c.vermelho, modifier = Modifier.size(16.dp)) }
            Spacer(Modifier.weight(1f))
            // o preço tem a linha só para ele: nunca fica cortado
            Text(moedaBr(corrida.valor), fontFamily = ArchivoLargo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = c.texto, maxLines = 1, softWrap = false)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PwSelo(Status.rotulo[corrida.status] ?: corrida.status, corStatus(corrida.status, c))
            if (corrida.pagamentoStatus == "pago") PwSelo("PAGO", c.verde, icone = PwIcones.BadgeCheck)
            if (corrida.urgente) PwSelo("URGENTE", c.vermelho, icone = PwIcones.Flame)
        }
        if (corrida.planejada && corrida.agendadaPara != null) {
            val f2 = RoundedCornerShape(10.dp)
            val cor = if (corrida.atrasada) c.vermelho else c.azul
            Row(
                Modifier.fillMaxWidth().clip(f2).background(cor.copy(alpha = 0.10f)).border(1.dp, cor.copy(alpha = 0.3f), f2).padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(if (corrida.atrasada) PwIcones.Alerta else PwIcones.CalendarClock, null, tint = cor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Column(Modifier.weight(1f)) {
                    Text(quando(corrida.agendadaPara).replaceFirstChar { it.uppercase() } + "  ·  " + (if (corrida.atrasada) "atrasada" else relativo(corrida.agendadaPara)),
                        color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(if (corrida.motoboyId != null) "Reservada · ${dados.membro(corrida.motoboyId)?.nome ?: "—"}" else "Livre para reservar",
                        color = if (corrida.motoboyId != null) c.texto2 else cor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        coleta?.let { LinhaEndereco(PwIcones.ArrowUpFromLine, it.endereco, c.ouro) }
        entregas.lastOrNull()?.let { LinhaEndereco(PwIcones.ArrowDownToLine, it.endereco + if (entregas.size > 1) " (+${entregas.size - 1})" else "", c.texto) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Detalhe(PwIcones.Building2, dados.cliente(corrida), modifier = Modifier.weight(1f, fill = false))
            Detalhe(PwIcones.Clock, hora(corrida.criadaEm))
            Detalhe(PwIcones.Ruler, km(corrida.distanciaPlanejadaM))
        }
        IdadeG(corrida.criadaEm, corrida.status in setOf("montada", "aguardando_motoboy", "sem_motoboy"), dados)
        if (!corrida.planejada) {
            dados.nomeMotoboy(corrida)?.let { Detalhe(PwIcones.Bike, it) }
            oferta?.let { Detalhe(PwIcones.BellRing, "Enviada para ${dados.membro(it.motoboyId)?.nome ?: "motoboy"}") }
        }
    }
}

@Composable
internal fun LinhaEndereco(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String, cor: Color) {
    val c = Pw.cores
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = cor, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, color = c.texto, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun Detalhe(icone: androidx.compose.ui.graphics.vector.ImageVector, texto: String, cor: Color? = null, modifier: Modifier = Modifier) {
    val c = Pw.cores
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icone, null, tint = cor ?: c.texto3, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(texto, color = cor ?: c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (cor != null) FontWeight.SemiBold else null)
    }
}

@Composable
internal fun TituloTela(titulo: String, subtitulo: String? = null, icone: androidx.compose.ui.graphics.vector.ImageVector) {
    com.pwexpress.core.designsystem.PwCabeca(icone, titulo, subtitulo)
}

/** Topo das telas abertas por cima: voltar, título de uma linha e ações em ícone. */
@Composable
internal fun Cabecalho(titulo: String, onVoltar: () -> Unit, subtitulo: String? = null, acoes: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {}) =
    com.pwexpress.core.designsystem.PwCabecalho(titulo, onVoltar, subtitulo = subtitulo, acoes = acoes)

@Composable
internal fun Espaco(h: Int = 4) = Spacer(Modifier.height(h.dp))

@Composable
internal fun TextoDica(texto: String) {
    Text(texto, color = Pw.cores.texto3, fontSize = 12.5.sp, lineHeight = 17.sp, fontFamily = Archivo)
}

/** Texto sem acento e minúsculo, para busca. */
internal fun normalBusca(t: String) = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()

/** Todas as palavras da busca aparecem em algum dos campos (sem acento, sem caixa). */
internal fun combinaBusca(termo: String, vararg campos: String?): Boolean {
    val palavras = normalBusca(termo).split(' ').filter { it.isNotBlank() }
    if (palavras.isEmpty()) return true
    val tudo = campos.filterNotNull().joinToString(" ") { normalBusca(it) }
    return palavras.all { it in tudo }
}

/** Campo de busca padrão das listas. */
@Composable
internal fun CampoBusca(valor: String, onMudar: (String) -> Unit, rotulo: String) =
    com.pwexpress.core.designsystem.PwCampo(valor, onMudar, rotulo, modifier = Modifier.fillMaxWidth())


/** Selos do que um ponto pede: confirmações e horário em que atende. */
@androidx.compose.runtime.Composable
internal fun SelosPontoG(foto: Boolean, nome: Boolean, assinatura: Boolean, codigo: Boolean, cod: String? = null, de: String? = null, ate: String? = null) {
    val c = com.pwexpress.core.designsystem.Pw.cores
    if (!(foto || nome || assinatura || codigo) && de == null && ate == null) return
    androidx.compose.foundation.layout.Row(
        Modifier.horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        if (foto) com.pwexpress.core.designsystem.PwSelo("Foto", c.texto2, icone = com.pwexpress.core.designsystem.PwIcones.Camera)
        if (nome) com.pwexpress.core.designsystem.PwSelo("Nome", c.texto2, icone = com.pwexpress.core.designsystem.PwIcones.UserRound)
        if (assinatura) com.pwexpress.core.designsystem.PwSelo("Assinatura", c.texto2, icone = com.pwexpress.core.designsystem.PwIcones.PenLine)
        if (codigo) com.pwexpress.core.designsystem.PwSelo("Código" + (cod?.let { " $it" } ?: ""), c.texto2, icone = com.pwexpress.core.designsystem.PwIcones.KeyRound)
        if (de != null || ate != null) com.pwexpress.core.designsystem.PwSelo("${de ?: "…"}–${ate ?: "…"}", c.texto2, icone = com.pwexpress.core.designsystem.PwIcones.Clock)
    }
}

/** "criado há 25 min", em destaque quando passou do tempo (Ajustes → Pedidos → Avisar se ficar parado). */
@Composable
internal fun IdadeG(desde: Long?, aberto: Boolean, dados: Gestao.Dados) {
    val c = Pw.cores
    if (desde == null) return
    var agora by androidx.compose.runtime.remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis()) }
    androidx.compose.runtime.LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(30_000); agora = System.currentTimeMillis() } }
    val min = ((agora - desde) / 60_000).coerceAtLeast(0)
    val txt = when { min < 1 -> "agora"; min < 60 -> "há $min min"; min < 1440 -> "há ${min / 60} h" + if (min % 60 > 0) " ${min % 60} min" else ""; else -> "há ${min / 1440} d" }
    val limite = (dados.config["alerta_parado_min"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toDoubleOrNull() ?: 30.0
    val alerta = aberto && limite > 0 && min >= limite
    Row(Modifier.clip(RoundedCornerShape(99.dp)).background(if (alerta) androidx.compose.ui.graphics.Color(0xFFF59E0B).copy(alpha = 0.15f) else androidx.compose.ui.graphics.Color.Transparent)
        .padding(horizontal = if (alerta) 8.dp else 0.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(PwIcones.Hourglass, null, tint = if (alerta) androidx.compose.ui.graphics.Color(0xFFFBBF24) else c.texto3, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text("Criado $txt", color = if (alerta) androidx.compose.ui.graphics.Color(0xFFFBBF24) else c.texto3, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
    }
}
