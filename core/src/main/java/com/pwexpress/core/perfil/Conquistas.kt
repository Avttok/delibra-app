package com.pwexpress.core.perfil

import com.pwexpress.core.tentar

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.PwCore
import com.pwexpress.core.designsystem.Archivo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.TipoAviso
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.launch

/** Uma conquista: alcança quando a estatística [stat] chega em [meta]. */
data class Conquista(
    val chave: String,
    val nome: String,
    val descricao: String,
    val stat: String,
    val meta: Double,
    val icone: () -> ImageVector,
    val cor: Color,
    val unidade: String = "",
)

private fun c(hex: Long) = Color(hex)
private val BRONZE = c(0xFFCD7F32); private val PRATA = c(0xFFC0C7D0); private val OURO = c(0xFFFACC15)
private val ROXO = c(0xFFA855F7); private val CIANO = c(0xFF22D3EE); private val VERDE = c(0xFF34D399)
private val AZUL = c(0xFF60A5FA); private val LARANJA = c(0xFFFB923C); private val VERMELHO = c(0xFFF87171)
private val ROSA = c(0xFFF472B6); private val LIMA = c(0xFFA3E635); private val INDIGO = c(0xFF818CF8)
private val AMBAR = c(0xFFF59E0B); private val TURQUESA = c(0xFF2DD4BF)

object Conquistas {
    val catalogo: List<Conquista> = listOf(
        // corridas concluídas
        Conquista("corrida_1", "Primeira corrida", "Concluiu a primeira corrida. O começo de tudo!", "corridas", 1.0, { PwIcones.Flag }, VERDE, "corrida"),
        Conquista("corrida_10", "Pegando o ritmo", "Concluiu 10 corridas.", "corridas", 10.0, { PwIcones.Bike }, TURQUESA, "corridas"),
        Conquista("corrida_50", "Veterano", "Concluiu 50 corridas.", "corridas", 50.0, { PwIcones.Medal }, BRONZE, "corridas"),
        Conquista("corrida_100", "Centurião", "Concluiu 100 corridas.", "corridas", 100.0, { PwIcones.Award }, PRATA, "corridas"),
        Conquista("corrida_250", "Máquina", "Concluiu 250 corridas.", "corridas", 250.0, { PwIcones.Trophy }, OURO, "corridas"),
        Conquista("corrida_500", "Lenda das ruas", "Concluiu 500 corridas.", "corridas", 500.0, { PwIcones.Crown }, ROXO, "corridas"),
        Conquista("corrida_1000", "Mil e uma", "Concluiu 1.000 corridas. Poucos chegam aqui.", "corridas", 1000.0, { PwIcones.Gem }, CIANO, "corridas"),
        // entregas
        Conquista("entrega_25", "Entregador", "Fez 25 entregas.", "entregas", 25.0, { PwIcones.PackageCheck }, LIMA, "entregas"),
        Conquista("entrega_100", "Caixa cheia", "Fez 100 entregas.", "entregas", 100.0, { PwIcones.Package2 }, AMBAR, "entregas"),
        Conquista("entrega_500", "Carga pesada", "Fez 500 entregas.", "entregas", 500.0, { PwIcones.Package }, LARANJA, "entregas"),
        Conquista("entrega_1000", "Foguete postal", "Fez 1.000 entregas.", "entregas", 1000.0, { PwIcones.Rocket }, VERMELHO, "entregas"),
        Conquista("entrega_5000", "Infinito", "Fez 5.000 entregas.", "entregas", 5000.0, { PwIcones.Infinito }, ROXO, "entregas"),
        // quilômetros
        Conquista("km_10", "Primeiros passos", "Rodou 10 km em corridas.", "km", 10.0, { PwIcones.Footprints }, VERDE, "km"),
        Conquista("km_100", "Marco 100", "Rodou 100 km em corridas.", "km", 100.0, { PwIcones.Milestone }, AZUL, "km"),
        Conquista("km_500", "Serra acima", "Rodou 500 km em corridas.", "km", 500.0, { PwIcones.Mountain }, TURQUESA, "km"),
        Conquista("km_1000", "Mil quilômetros", "Rodou 1.000 km em corridas.", "km", 1000.0, { PwIcones.Gauge }, OURO, "km"),
        Conquista("km_5000", "Estradeiro", "Rodou 5.000 km em corridas.", "km", 5000.0, { PwIcones.Route }, LARANJA, "km"),
        Conquista("km_10000", "Volta ao Brasil", "Rodou 10.000 km em corridas: quase a costa do Brasil inteira.", "km", 10000.0, { PwIcones.Globe }, CIANO, "km"),
        // tempo online
        Conquista("online_1", "Ligado", "Ficou 1 hora online.", "horas_online", 1.0, { PwIcones.Power }, VERDE, "h"),
        Conquista("online_10", "De plantão", "Ficou 10 horas online.", "horas_online", 10.0, { PwIcones.Timer }, AZUL, "h"),
        Conquista("online_50", "Sempre a postos", "Ficou 50 horas online.", "horas_online", 50.0, { PwIcones.Clock }, INDIGO, "h"),
        Conquista("online_100", "Radar ligado", "Ficou 100 horas online.", "horas_online", 100.0, { PwIcones.Radar }, TURQUESA, "h"),
        Conquista("online_500", "Incansável", "Ficou 500 horas online.", "horas_online", 500.0, { PwIcones.Hourglass }, AMBAR, "h"),
        Conquista("online_1000", "Energia infinita", "Ficou 1.000 horas online.", "horas_online", 1000.0, { PwIcones.Zap }, OURO, "h"),
        // tempo de conta
        Conquista("conta_7", "Bem-vindo", "Uma semana com a gente.", "dias_conta", 7.0, { PwIcones.CalendarCheck }, VERDE, "dias"),
        Conquista("conta_30", "Um mês de casa", "Um mês com a gente.", "dias_conta", 30.0, { PwIcones.CalendarDays }, AZUL, "dias"),
        Conquista("conta_90", "Da família", "Três meses com a gente.", "dias_conta", 90.0, { PwIcones.Heart }, ROSA, "dias"),
        Conquista("conta_180", "Meio ano", "Seis meses com a gente.", "dias_conta", 180.0, { PwIcones.BadgeCheck }, INDIGO, "dias"),
        Conquista("conta_365", "Aniversário", "Um ano com a gente. Parabéns!", "dias_conta", 365.0, { PwIcones.Cake }, ROSA, "dias"),
        Conquista("conta_730", "Raiz", "Dois anos com a gente.", "dias_conta", 730.0, { PwIcones.Crown }, OURO, "dias"),
        // dias e sequências
        Conquista("ativos_30", "Presença", "Trabalhou em 30 dias diferentes.", "dias_ativos", 30.0, { PwIcones.CalendarClock }, TURQUESA, "dias"),
        Conquista("ativos_100", "Cem dias de rua", "Trabalhou em 100 dias diferentes.", "dias_ativos", 100.0, { PwIcones.Star }, OURO, "dias"),
        Conquista("seq_3", "Esquentando", "Corridas em 3 dias seguidos.", "sequencia", 3.0, { PwIcones.Flame }, LARANJA, "dias seguidos"),
        Conquista("seq_7", "Semana cheia", "Corridas em 7 dias seguidos.", "sequencia", 7.0, { PwIcones.Flame }, VERMELHO, "dias seguidos"),
        Conquista("seq_15", "Pegando fogo", "Corridas em 15 dias seguidos.", "sequencia", 15.0, { PwIcones.Flame }, ROSA, "dias seguidos"),
        Conquista("seq_30", "Chama eterna", "Corridas em 30 dias seguidos.", "sequencia", 30.0, { PwIcones.Flame }, ROXO, "dias seguidos"),
        // ritmo
        Conquista("dia_5", "Coelho", "5 corridas num dia só.", "melhor_dia", 5.0, { PwIcones.Rabbit }, LIMA, "no melhor dia"),
        Conquista("dia_10", "Dia de foguete", "10 corridas num dia só.", "melhor_dia", 10.0, { PwIcones.Rocket }, AZUL, "no melhor dia"),
        Conquista("dia_20", "Imparável", "20 corridas num dia só.", "melhor_dia", 20.0, { PwIcones.Swords }, VERMELHO, "no melhor dia"),
        Conquista("rota_5", "Multitarefa", "5 entregas numa rota só.", "maior_rota", 5.0, { PwIcones.ListChecks }, INDIGO, "entregas numa rota"),
        Conquista("rota_10", "Malabarista", "10 entregas numa rota só.", "maior_rota", 10.0, { PwIcones.Layers }, ROXO, "entregas numa rota"),
        Conquista("longe_20", "Bate e volta", "Uma corrida de 20 km ou mais.", "maior_km", 20.0, { PwIcones.Navigation }, AZUL, "km na maior"),
        Conquista("longe_50", "Viajante", "Uma corrida de 50 km ou mais.", "maior_km", 50.0, { PwIcones.Mapa }, CIANO, "km na maior"),
        // horários
        Conquista("noite_1", "Turno da noite", "Concluiu uma corrida depois das 22h.", "noturnas", 1.0, { PwIcones.Moon }, INDIGO, "noturnas"),
        Conquista("noite_25", "Noturno", "25 corridas depois das 22h.", "noturnas", 25.0, { PwIcones.MoonStar }, ROXO, "noturnas"),
        Conquista("noite_100", "Coruja", "100 corridas depois das 22h.", "noturnas", 100.0, { PwIcones.Bird }, AMBAR, "noturnas"),
        Conquista("madrugada_10", "Vigia da madrugada", "10 corridas entre meia-noite e 5h.", "madrugada", 10.0, { PwIcones.SunMoon }, INDIGO, "de madrugada"),
        Conquista("cedo_10", "Madrugador", "10 corridas entre 5h e 8h.", "cedinho", 10.0, { PwIcones.Sunrise }, LARANJA, "cedinho"),
        Conquista("fds_10", "Fim de semana na rua", "10 corridas em sábados ou domingos.", "fim_de_semana", 10.0, { PwIcones.PartyPopper }, ROSA, "no fim de semana"),
        Conquista("fds_50", "Sem folga", "50 corridas em sábados ou domingos.", "fim_de_semana", 50.0, { PwIcones.Sun }, AMBAR, "no fim de semana"),
        // tipos de corrida
        Conquista("urgente_1", "Sirene", "Concluiu a primeira corrida urgente.", "urgentes", 1.0, { PwIcones.Siren }, VERMELHO, "urgentes"),
        Conquista("urgente_10", "Bombeiro", "10 corridas urgentes.", "urgentes", 10.0, { PwIcones.Flame }, LARANJA, "urgentes"),
        Conquista("urgente_50", "Relâmpago", "50 corridas urgentes.", "urgentes", 50.0, { PwIcones.Zap }, AMBAR, "urgentes"),
        Conquista("planejada_10", "Agenda em dia", "10 corridas planejadas concluídas.", "planejadas", 10.0, { PwIcones.CalendarClock }, AZUL, "planejadas"),
        Conquista("planejada_50", "Pontual", "50 corridas planejadas concluídas.", "planejadas", 50.0, { PwIcones.Target }, VERDE, "planejadas"),
        Conquista("perigo_1", "Mão firme", "Levou uma carga perigosa.", "perigosas", 1.0, { PwIcones.ShieldAlert }, AMBAR, "com carga perigosa"),
        Conquista("perigo_10", "Escudo", "10 corridas com carga perigosa.", "perigosas", 10.0, { PwIcones.Shield }, VERMELHO, "com carga perigosa"),
        Conquista("clientes_5", "Bem relacionado", "Atendeu 5 clientes diferentes.", "clientes", 5.0, { PwIcones.Handshake }, TURQUESA, "clientes"),
        Conquista("clientes_20", "Conhecido na cidade", "Atendeu 20 clientes diferentes.", "clientes", 20.0, { PwIcones.Users }, INDIGO, "clientes"),
        Conquista("prova_50", "Fotógrafo", "50 entregas com foto ou assinatura.", "comprovantes", 50.0, { PwIcones.Camera }, CIANO, "comprovantes"),
        Conquista("prova_200", "Tudo assinado", "200 entregas com foto ou assinatura.", "comprovantes", 200.0, { PwIcones.Signature }, ROXO, "comprovantes"),
        // ganhos
        Conquista("ganho_100", "Primeiro cem", "Ganhou R$ 100 em corridas.", "ganhos", 100.0, { PwIcones.Coins }, LIMA, "reais"),
        Conquista("ganho_1000", "Mil no bolso", "Ganhou R$ 1.000 em corridas.", "ganhos", 1000.0, { PwIcones.Wallet }, VERDE, "reais"),
        Conquista("ganho_10000", "Cofre cheio", "Ganhou R$ 10.000 em corridas.", "ganhos", 10000.0, { PwIcones.Gem }, OURO, "reais"),
    )

    // as últimas estatísticas ficam guardadas: o menu abre já com as medalhas e atualiza por trás
    private val prefs get() = PwCore.app.getSharedPreferences("pwex_conquistas", android.content.Context.MODE_PRIVATE)
    private val _minhas = MutableStateFlow<JsonObject?>(tentar { prefs.getString("minhas", null)?.let { Json.parseToJsonElement(it).jsonObject } }.getOrNull())
    @Volatile private var ultimaCarga = 0L
    private val carregando = java.util.concurrent.atomic.AtomicBoolean(false)
    /** Atualiza as minhas se passou um tempo (sem segurar a tela). */
    suspend fun atualizarMinhas(maxIdadeMs: Long = 120_000) {
        if (System.currentTimeMillis() - ultimaCarga < maxIdadeMs || !carregando.compareAndSet(false, true)) return
        try { carregar() } finally { carregando.set(false) }
    }
    /** Estatísticas do próprio usuário (para o menu da conta e o perfil). */
    val minhas: StateFlow<JsonObject?> = _minhas.asStateFlow()

    /** Busca as estatísticas (null = as minhas). */
    suspend fun carregar(motoboyId: String? = null): Result<JsonObject> = tentar {
        val r = PwCore.rpc.chamar("conquistas", buildJsonObject { if (motoboyId != null) put("p_motoboy", motoboyId) })
        if (!r.ok) error(if (r.corpo.contains("conquistas")) "Rode a migração das conquistas no banco." else "Erro (${r.codigo}).")
        Json.parseToJsonElement(r.corpo).jsonObject
    }.onSuccess {
        if (motoboyId == null) {
            _minhas.value = it; ultimaCarga = System.currentTimeMillis()
            tentar { prefs.edit().putString("minhas", it.toString()).apply() }
        }
    }

    fun valor(stats: JsonObject?, stat: String): Double = (stats?.get(stat) as? JsonPrimitive)?.doubleOrNull ?: 0.0
    /** Ganha/bloqueada à mão pelo Dev (null = pela estatística). */
    fun manual(stats: JsonObject?, chave: String): Boolean? = ((stats?.get("manuais") as? JsonObject)?.get(chave) as? JsonPrimitive)?.content?.toBooleanStrictOrNull()
    fun ganha(stats: JsonObject?, cq: Conquista): Boolean = manual(stats, cq.chave) ?: (valor(stats, cq.stat) >= cq.meta)
    fun ganhas(stats: JsonObject?) = catalogo.filter { ganha(stats, it) }

    /** Dev: "ganha", "bloqueada" ou "auto" (volta a valer a estatística). */
    suspend fun definir(perfilId: String, chave: String, estado: String): Result<Unit> = tentar {
        val r = PwCore.rpc.chamar("definir_conquista", buildJsonObject { put("p_perfil", perfilId); put("p_chave", chave); put("p_estado", estado) })
        if (!r.ok) error(if (r.corpo.contains("apenas_dev")) "Só o Dev mexe nas conquistas." else if (r.corpo.contains("definir_conquista")) "Rode a migração 20261110000007 no banco." else "Erro (${r.codigo}).")
    }
}

private fun numeroCurto(v: Double): String =
    if (v % 1.0 == 0.0) "%,d".format(java.util.Locale.forLanguageTag("pt-BR"), v.toLong()) else String.format(java.util.Locale.forLanguageTag("pt-BR"), "%,.1f", v)

/** Mostra o que a conquista significa (fica escondido até tocar). */
fun explicarConquista(cq: Conquista, stats: JsonObject?) {
    val v = Conquistas.valor(stats, cq.stat)
    val ok = Conquistas.ganha(stats, cq)
    PwToast.mostrar(
        if (ok) cq.descricao else "${cq.descricao} Falta pouco: ${numeroCurto(v)} de ${numeroCurto(cq.meta)} ${cq.unidade}.".replace("  ", " "),
        if (ok) TipoAviso.OK else TipoAviso.INFO,
        if (ok) "🏆 ${cq.nome}" else "🔒 ${cq.nome}",
    )
}

/** Medalha redonda: colorida quando ganha, apagada (com cadeado) quando não. */
@Composable
fun MedalhaConquista(cq: Conquista, ganha: Boolean, stats: JsonObject?, tamanho: Dp = 56.dp, comNome: Boolean = true, onToque: (() -> Unit)? = null) {
    val c = Pw.cores
    var tocou by remember { mutableStateOf(false) }
    val escala by animateFloatAsState(if (tocou) 1.15f else 1f, spring(0.4f, 500f), finishedListener = { tocou = false }, label = "medalha")
    Column(
        Modifier.width(tamanho + 18.dp).clip(RoundedCornerShape(14.dp)).clickable { tocou = true; if (onToque != null) onToque() else explicarConquista(cq, stats) }.padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(tamanho).scale(escala), contentAlignment = Alignment.Center) {
            val cor = if (ganha) cq.cor else c.texto3
            Box(
                Modifier.size(tamanho).clip(CircleShape)
                    .background(if (ganha) Brush.radialGradient(listOf(cor.copy(alpha = 0.55f), cor.copy(alpha = 0.18f))) else Brush.linearGradient(listOf(c.vidroSutil, c.vidroSutil)))
                    .border(2.dp, if (ganha) Brush.linearGradient(listOf(Color.White.copy(alpha = 0.7f), cor, cor.copy(alpha = 0.5f))) else Brush.linearGradient(listOf(c.borda, c.borda)), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(cq.icone(), cq.nome, tint = if (ganha) Color.White else c.texto3.copy(alpha = 0.6f), modifier = Modifier.size(tamanho * 0.46f))
            }
            if (!ganha) Box(
                Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp).size(tamanho * 0.36f).clip(CircleShape).background(c.fundo2).border(1.dp, c.borda, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(PwIcones.Lock, null, tint = c.texto3, modifier = Modifier.size(tamanho * 0.2f)) }
        }
        if (comNome) {
            Spacer(Modifier.height(4.dp))
            Text(cq.nome, color = if (ganha) c.texto else c.texto3, fontSize = 11.sp, lineHeight = 13.sp, fontFamily = Archivo, fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Todas as conquistas: ganhas primeiro, depois as que faltam (com progresso no toque). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GradeConquistas(stats: JsonObject?, modifier: Modifier = Modifier, perfilDev: String? = null, onMudou: () -> Unit = {}) {
    val c = Pw.cores
    val ganhas = Conquistas.ganhas(stats).toSet()
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Trophy, null, tint = c.ouro, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Conquistas", color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text("${ganhas.size} de ${Conquistas.catalogo.size}", color = c.ouro, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(if (perfilDev != null) "Dev: toque numa medalha para dar, bloquear ou voltar ao automático." else "Toque numa medalha para ver o que ela significa.", color = if (perfilDev != null) c.dev else c.texto3, fontSize = 12.sp)
        Spacer(Modifier.height(10.dp))
        if (stats == null) { Text("Carregando…", color = c.texto3, fontSize = 13.sp); return@Column }
        var editando by remember { mutableStateOf<Conquista?>(null) }
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (Conquistas.catalogo.filter { it in ganhas } + Conquistas.catalogo.filter { it !in ganhas }).forEach { cq ->
                MedalhaConquista(cq, cq in ganhas, stats, onToque = if (perfilDev != null) ({ editando = cq }) else null)
            }
        }
        val cq = editando
        if (cq != null && perfilDev != null) {
            val escopo = androidx.compose.runtime.rememberCoroutineScope()
            val manual = Conquistas.manual(stats, cq.chave)
            com.pwexpress.core.designsystem.PwJanela(cq.nome, { editando = null }) {
                Text(cq.descricao, color = c.texto2, fontSize = 14.sp)
                Text("Pela estatística: ${if (Conquistas.valor(stats, cq.stat) >= cq.meta) "ganha" else "ainda não"} · agora: ${when (manual) { true -> "dada pelo Dev"; false -> "bloqueada pelo Dev"; null -> "automática" }}",
                    color = c.texto3, fontSize = 12.5.sp)
                Spacer(Modifier.height(10.dp))
                fun aplicar(estado: String, msg: String) {
                    escopo.launch {
                        Conquistas.definir(perfilDev, cq.chave, estado).onSuccess { PwToast.mostrar(msg, TipoAviso.OK, cq.nome); editando = null; onMudou() }
                            .onFailure { PwToast.mostrar(it.message ?: "Erro", TipoAviso.ERRO) }
                    }
                }
                com.pwexpress.core.designsystem.PwBotaoOuro("Dar a medalha", onClick = { aplicar("ganha", "Medalha liberada.") }, icone = PwIcones.Trophy)
                Spacer(Modifier.height(8.dp))
                com.pwexpress.core.designsystem.PwBotaoVidro("Bloquear", onClick = { aplicar("bloqueada", "Medalha bloqueada.") }, icone = PwIcones.Lock, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                com.pwexpress.core.designsystem.PwBotaoVidro("Automático (pela estatística)", onClick = { aplicar("auto", "Volta a valer a estatística.") }, icone = PwIcones.RefreshCw, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Faixa curta com as conquistas ganhas (menu da conta). */
@Composable
fun FaixaConquistas(onVerTodas: () -> Unit, modifier: Modifier = Modifier) {
    val c = Pw.cores
    val stats by Conquistas.minhas.collectAsState()
    LaunchedEffect(Unit) { Conquistas.atualizarMinhas() }
    val ganhas = Conquistas.ganhas(stats)
    Column(modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onVerTodas).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(PwIcones.Trophy, null, tint = c.ouro, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Conquistas", color = c.texto, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (stats == null) "…" else "${ganhas.size} de ${Conquistas.catalogo.size}", color = c.texto3, fontSize = 12.5.sp)
            Spacer(Modifier.width(4.dp))
            Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(16.dp))
        }
        if (stats != null) {
            Spacer(Modifier.height(6.dp))
            if (ganhas.isEmpty()) Text("Conclua a primeira corrida para ganhar a primeira medalha.", color = c.texto3, fontSize = 12.5.sp)
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                ganhas.reversed().forEach { MedalhaConquista(it, true, stats, tamanho = 40.dp, comNome = false) }
            }
        }
    }
}

private val GRUPO_STAT = mapOf("corridas" to "Rotas", "entregas" to "Entregas", "km" to "Quilômetros", "dias" to "Dias rodando", "seguidos" to "Sequências")

/**
 * Janela das conquistas (aberta pelo menu da foto): anel com o total, filtros, as que estão
 * "quase lá" e cada medalha com a barra do quanto falta.
 */
@Composable
fun JanelaConquistas(onFechar: () -> Unit) {
    val c = Pw.cores
    val stats by Conquistas.minhas.collectAsState()
    LaunchedEffect(Unit) { Conquistas.atualizarMinhas(0) }
    var filtro by remember { mutableStateOf(0) }
    val ganhas = Conquistas.ganhas(stats).toSet()
    val total = Conquistas.catalogo.size
    fun progresso(cq: Conquista) = if (cq in ganhas) 1f else (Conquistas.valor(stats, cq.stat) / cq.meta).toFloat().coerceIn(0f, 1f)
    com.pwexpress.core.designsystem.PwTelaCheia("Conquistas", onFechar, subtitulo = "${ganhas.size} de $total medalhas") {
        // anel do total
        val alvo = if (total == 0) 0f else ganhas.size.toFloat() / total
        val p by animateFloatAsState(if (stats == null) 0f else alvo, spring(0.8f, 60f), label = "anel")
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.size(150.dp).drawBehind {
                val esp = 12.dp.toPx()
                drawCircle(c.borda, size.minDimension / 2 - esp / 2, style = androidx.compose.ui.graphics.drawscope.Stroke(esp))
                drawArc(Brush.sweepGradient(listOf(c.ouroClaro, c.ouro, c.ouroEscuro, c.ouroClaro)), -90f, 360f * p, false,
                    topLeft = androidx.compose.ui.geometry.Offset(esp / 2, esp / 2), size = androidx.compose.ui.geometry.Size(size.width - esp, size.height - esp),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(esp, cap = androidx.compose.ui.graphics.StrokeCap.Round))
            }, contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(PwIcones.Trophy, null, tint = c.ouro, modifier = Modifier.size(26.dp))
                    Text("${ganhas.size}", color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Black, fontSize = 32.sp)
                    Text("de $total", color = c.texto3, fontSize = 12.sp)
                }
            }
        }
        if (stats == null) { Text("Carregando…", color = c.texto3, fontSize = 13.sp); return@PwTelaCheia }
        // estatísticas principais
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("corridas" to "rotas", "entregas" to "entregas", "km" to "km").forEach { (k, r) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.vidro).border(1.dp, c.borda, RoundedCornerShape(14.dp)).padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(com.pwexpress.core.designsystem.numeroBr(Conquistas.valor(stats, k), 0), color = c.texto, fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(r, color = c.texto3, fontSize = 11.5.sp)
                }
            }
        }
        // quase lá: as 3 que faltam menos
        val quase = Conquistas.catalogo.filter { it !in ganhas }.sortedByDescending { progresso(it) }.take(3).filter { progresso(it) > 0f }
        if (quase.isNotEmpty()) {
            com.pwexpress.core.designsystem.PwRotulo("Quase lá")
            quase.forEach { LinhaConquista(it, false, progresso(it), stats) }
        }
        com.pwexpress.core.designsystem.PwSegmentado(listOf("Todas" to null, "Ganhas" to PwIcones.CircleCheck, "Faltam" to PwIcones.Lock), filtro, { filtro = it })
        val lista = Conquistas.catalogo.filter { when (filtro) { 1 -> it in ganhas; 2 -> it !in ganhas; else -> true } }
        lista.groupBy { it.stat }.forEach { (stat, itens) ->
            com.pwexpress.core.designsystem.PwRotulo(GRUPO_STAT[stat] ?: stat.replaceFirstChar { it.uppercase() })
            itens.forEach { LinhaConquista(it, it in ganhas, progresso(it), stats) }
        }
        if (lista.isEmpty()) Text(if (filtro == 1) "Nenhuma medalha ainda. Conclua a primeira rota!" else "Você já tem todas. Lenda!", color = c.texto3, fontSize = 13.sp)
    }
}

@Composable
private fun LinhaConquista(cq: Conquista, ganha: Boolean, progresso: Float, stats: JsonObject?) {
    val c = Pw.cores
    val forma = RoundedCornerShape(16.dp)
    Row(Modifier.fillMaxWidth().clip(forma).background(if (ganha) cq.cor.copy(alpha = 0.10f) else c.vidro).border(1.dp, if (ganha) cq.cor.copy(alpha = 0.45f) else c.borda, forma)
        .clickable { explicarConquista(cq, stats) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        MedalhaConquista(cq, ganha, stats, tamanho = 46.dp, comNome = false)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cq.nome, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (ganha) Icon(PwIcones.CircleCheck, "Ganha", tint = cq.cor, modifier = Modifier.size(16.dp))
            }
            Text(cq.descricao, color = c.texto3, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!ganha) {
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)).background(c.borda)) {
                    Box(Modifier.fillMaxWidth(progresso).height(6.dp).clip(RoundedCornerShape(99.dp)).background(cq.cor))
                }
                Text("${com.pwexpress.core.designsystem.numeroBr(Conquistas.valor(stats, cq.stat), 0)} de ${com.pwexpress.core.designsystem.numeroBr(cq.meta, 0)}", color = c.texto3, fontSize = 11.sp)
            }
        }
    }
}
