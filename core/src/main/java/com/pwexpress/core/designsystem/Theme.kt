package com.pwexpress.core.designsystem

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.pwexpress.core.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ------------------------------------------------------------------ cores

/** Paleta de um tema (claro ou noturno). Os nomes seguem o design system do site. */
@Immutable
data class PwPaleta(
    val escuro: Boolean,
    val fundo: Color,
    val fundo2: Color,
    val vidro: Color,
    val vidroForte: Color,
    val vidroSutil: Color,
    val campo: Color,
    val borda: Color,
    val bordaForte: Color,
    val reflexo: Color,
    val texto: Color,
    val texto2: Color,
    val texto3: Color,
    val ouro: Color,
    val ouroClaro: Color,
    val ouroEscuro: Color,
    val sobreOuro: Color,
    val verde: Color,
    val vermelho: Color,
    val violeta: Color,
    /** Corridas planejadas (hora marcada). */
    val azul: Color = Color(0xFF60A5FA),
    val brilho1: Color,
    val brilho2: Color,
    /** Qual logo usar: o tema troca só cores, nunca tamanhos. */
    val marca: Marca = Marca.OURO,
    /** Cor do cargo Dev (selo </>). */
    val dev: Color = Color(0xFFF87171),
) {
    /** Degradê principal (botões, selos): claro → ouro → escuro, 135°. */
    val gradOuro: Brush get() = Brush.linearGradient(listOf(ouroClaro, ouro, ouroEscuro))
    /** Degradê suave (azulejos de ícone, item ativo). */
    val gradSuave: Brush get() = Brush.linearGradient(listOf(ouro.copy(alpha = 0.26f), ouro.copy(alpha = 0.08f)))
}

private val Noturno = PwPaleta(
    escuro = true,
    fundo = Color(0xFF08080A), fundo2 = Color(0xFF0F0F12),
    vidro = Color(0xD6121216), vidroForte = Color(0xED18181D), vidroSutil = Color(0x0AFFFFFF),
    campo = Color(0x59000000), borda = Color(0x14FFFFFF), bordaForte = Color(0x29FFFFFF), reflexo = Color(0x1AFFFFFF),
    texto = Color(0xFFF5F2EA), texto2 = Color(0xFFA9A497), texto3 = Color(0xFF6F6B62),
    ouro = Color(0xFFD4AF37), ouroClaro = Color(0xFFF3D77E), ouroEscuro = Color(0xFF8F7019), sobreOuro = Color(0xFF120F06),
    verde = Color(0xFF34D399), vermelho = Color(0xFFF87171), violeta = Color(0xFFA78BFA),
    brilho1 = Color(0x29D4AF37), brilho2 = Color(0x12D4AF37),
)

private val Claro = PwPaleta(
    escuro = false,
    fundo = Color(0xFFF6F2E9), fundo2 = Color(0xFFEFE9DC),
    vidro = Color(0xDBFFFFFF), vidroForte = Color(0xF2FFFFFF), vidroSutil = Color(0x08140F04),
    campo = Color(0xBFFFFFFF), borda = Color(0x1A28200A), bordaForte = Color(0x3328200A), reflexo = Color(0xE6FFFFFF),
    texto = Color(0xFF16130C), texto2 = Color(0xFF6B6455), texto3 = Color(0xFF9A9384),
    ouro = Color(0xFFB8901F), ouroClaro = Color(0xFFE2BD52), ouroEscuro = Color(0xFF7D6110), sobreOuro = Color(0xFFFFFFFF),
    verde = Color(0xFF059669), vermelho = Color(0xFFDC2626), violeta = Color(0xFF7C3AED),
    brilho1 = Color(0x47D4AF37), brilho2 = Color(0x24D4AF37),
)

val LocalPw = staticCompositionLocalOf { Noturno }

/** Atalho: `Pw.cores.ouro` dentro de qualquer composable. */
object Pw {
    val cores: PwPaleta @Composable get() = LocalPw.current
}

/** Compatibilidade com o código existente (cores fixas da marca). */
object PwCores {
    val Ouro = Color(0xFFD4AF37)
    val OuroClaro = Color(0xFFF3D77E)
    val OuroEscuro = Color(0xFF8F7019)
    val Preto = Color(0xFF0A0A0A)
    val Grafite = Color(0xFF151515)
    val Grafite2 = Color(0xFF1F1F1F)
    val Borda = Color(0xFF2C2C2C)
    val Branco = Color(0xFFFFFFFF)
    val Cinza = Color(0xFF9A9A9A)
    val Verde = Color(0xFF2ECC71)
    val Vermelho = Color(0xFFE5484D)
    val Violeta = Color(0xFFA78BFA)
}

// ------------------------------------------------------------------ preferência de tema

enum class PreferenciaTema { SISTEMA, CLARO, ESCURO }

/** Cor principal: dourado (todos), vermelho (Dev) ou violeta (modo teste, sempre vence). */
enum class Marca { OURO, VERMELHO, VIOLETA, AZUL, VERDE, BRANCO, PRETO, TINTA }

/** Cores que qualquer usuário escolhe no menu da conta. Os dois "preto/branco" fixam claro ou noturno. */
enum class EstiloTema(
    val rotulo: String, val amostra: Color, val fundoAmostra: Color,
    /** Cores novas (claro, principal, escuro) no noturno e no claro; o degradê dos botões vai de uma à outra. */
    val noturno: List<Long>? = null, val diurno: List<Long>? = null,
) {
    OURO("Dourado", Color(0xFFD4AF37), Color(0xFF0F0F12)),
    AZUL("Azul", Color(0xFF3B82F6), Color(0xFF0F0F12)),
    VERDE("Verde", Color(0xFF10B981), Color(0xFF0F0F12)),
    VERMELHO("Vermelho", Color(0xFFEF4444), Color(0xFF0F0F12), listOf(0xFFFCA5A5, 0xFFEF4444, 0xFF991B1B), listOf(0xFFF87171, 0xFFDC2626, 0xFF7F1D1D)),
    ROSA("Rosa", Color(0xFFF472B6), Color(0xFF0F0F12), listOf(0xFFFBCFE8, 0xFFF472B6, 0xFFBE185D), listOf(0xFFF472B6, 0xFFDB2777, 0xFF831843)),
    ROXO("Roxo", Color(0xFF8B5CF6), Color(0xFF0F0F12), listOf(0xFFDDD6FE, 0xFF8B5CF6, 0xFF5B21B6), listOf(0xFFA78BFA, 0xFF7C3AED, 0xFF4C1D95)),
    LARANJA("Laranja", Color(0xFFFB923C), Color(0xFF0F0F12), listOf(0xFFFED7AA, 0xFFFB923C, 0xFFC2410C), listOf(0xFFFB923C, 0xFFEA580C, 0xFF7C2D12)),
    CIANO("Ciano", Color(0xFF22D3EE), Color(0xFF0F0F12), listOf(0xFFA5F3FC, 0xFF22D3EE, 0xFF0E7490), listOf(0xFF22D3EE, 0xFF0891B2, 0xFF164E63)),
    // degradês: as duas pontas são cores diferentes
    POR_DO_SOL("Pôr do sol", Color(0xFFFB923C), Color(0xFFDB2777), listOf(0xFFFDE047, 0xFFFB923C, 0xFFDB2777), listOf(0xFFF59E0B, 0xFFEA580C, 0xFFBE185D)),
    OCEANO("Oceano", Color(0xFF22D3EE), Color(0xFF4F46E5), listOf(0xFF67E8F9, 0xFF3B82F6, 0xFF4F46E5), listOf(0xFF06B6D4, 0xFF2563EB, 0xFF3730A3)),
    AURORA("Aurora", Color(0xFF34D399), Color(0xFF22D3EE), listOf(0xFFBEF264, 0xFF34D399, 0xFF0891B2), listOf(0xFF65A30D, 0xFF059669, 0xFF0E7490)),
    NEON("Neon", Color(0xFFE879F9), Color(0xFF6366F1), listOf(0xFFF0ABFC, 0xFFC026D3, 0xFF6366F1), listOf(0xFFD946EF, 0xFFA21CAF, 0xFF4338CA)),
    FOGO("Fogo", Color(0xFFFACC15), Color(0xFFDC2626), listOf(0xFFFDE047, 0xFFF97316, 0xFFDC2626), listOf(0xFFEAB308, 0xFFEA580C, 0xFFB91C1C)),
    PRATA("Prata", Color(0xFFD4D4D8), Color(0xFF52525B), listOf(0xFFF4F4F5, 0xFFA1A1AA, 0xFF52525B), listOf(0xFF71717A, 0xFF52525B, 0xFF27272A)),
    PRETO_NO_BRANCO("Preto no branco", Color(0xFF111111), Color(0xFFFFFFFF)),
    BRANCO_NO_PRETO("Branco no preto", Color(0xFFFFFFFF), Color(0xFF000000)),
}

/** Guarda a escolha do usuário (sistema / claro / noturno). */
object TemaApp {
    private var ctx: Context? = null
    private val _pref = MutableStateFlow(PreferenciaTema.SISTEMA)
    val preferencia: StateFlow<PreferenciaTema> = _pref.asStateFlow()

    /** Dev com o tema vermelho: troca o dourado por vermelho (o modo teste ainda vence). */
    private val _vermelho = MutableStateFlow(false)
    val vermelho: StateFlow<Boolean> = _vermelho.asStateFlow()
    fun definirVermelho(ligado: Boolean) {
        _vermelho.value = ligado
        ctx?.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE)?.edit { putBoolean("vermelho", ligado) }
    }

    /** Com o modo teste ligado, o app inteiro troca o dourado por violeta (fica claro que é teste). */
    private val _modoTeste = MutableStateFlow(false)
    val modoTeste: StateFlow<Boolean> = _modoTeste.asStateFlow()
    fun definirModoTeste(ligado: Boolean) { _modoTeste.value = ligado }

    /** Cores escolhidas pelo usuário (guardadas no aparelho). */
    private val _estilo = MutableStateFlow(EstiloTema.OURO)
    val estilo: StateFlow<EstiloTema> = _estilo.asStateFlow()
    fun definirEstilo(e: EstiloTema) {
        _estilo.value = e
        ctx?.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE)?.edit { putString("estilo", e.name) }
    }

    /** Como a pessoa prefere mudar números: barra (deslizar) ou digitando (com − e +). Vale em todo o app. */
    private val _numeroBarra = MutableStateFlow(false)
    val numeroBarra: StateFlow<Boolean> = _numeroBarra.asStateFlow()
    fun definirNumeroBarra(barra: Boolean) {
        _numeroBarra.value = barra
        ctx?.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE)?.edit { putBoolean("numero_barra", barra) }
    }

    fun init(context: Context) {
        ctx = context.applicationContext
        Fontes.init(context)
        _numeroBarra.value = context.applicationContext.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE).getBoolean("numero_barra", false)
        val salvo = ctx!!.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE).getString("tema", null)
        _pref.value = PreferenciaTema.entries.firstOrNull { it.name == salvo } ?: PreferenciaTema.SISTEMA
        _vermelho.value = ctx!!.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE).getBoolean("vermelho", false)
        val est = ctx!!.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE).getString("estilo", null)
        _estilo.value = EstiloTema.entries.firstOrNull { it.name == est } ?: EstiloTema.OURO
    }

    fun definir(p: PreferenciaTema) {
        _pref.value = p
        ctx?.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE)?.edit { putString("tema", p.name) }
    }

    /** sistema → claro → noturno → sistema */
    fun alternar() = definir(
        when (_pref.value) {
            PreferenciaTema.SISTEMA -> PreferenciaTema.CLARO
            PreferenciaTema.CLARO -> PreferenciaTema.ESCURO
            PreferenciaTema.ESCURO -> PreferenciaTema.SISTEMA
        },
    )
}

// ------------------------------------------------------------------ tipografia (Archivo variável)

@OptIn(ExperimentalTextApi::class)
private fun archivo(peso: Int, largura: Float = 100f) = Font(
    R.font.archivo,
    weight = FontWeight(peso),
    variationSettings = FontVariation.Settings(FontVariation.weight(peso), FontVariation.width(largura)),
)

val ArchivoBase = FontFamily(
    archivo(300), archivo(400), archivo(500), archivo(600), archivo(700), archivo(800), archivo(900),
)
private val ArchivoLargoBase = FontFamily(archivo(780, 122f), archivo(900, 125f), archivo(700, 112f))

private fun tres(r400: Int, r700: Int, r800: Int?) = FontFamily(listOfNotNull(
    Font(r400, FontWeight.Normal), Font(r400, FontWeight.Medium), Font(r700, FontWeight.SemiBold), Font(r700, FontWeight.Bold),
    Font(r800 ?: r700, FontWeight.ExtraBold), Font(r800 ?: r700, FontWeight.Black),
))

/** Fontes que o usuário escolhe para o app (e o gestor para o nome da empresa no topo). */
enum class FonteApp(val rotulo: String, familia: () -> FontFamily) {
    ARCHIVO("Archivo (padrão)", { ArchivoBase }),
    INTER("Inter", { tres(R.font.f_inter_400, R.font.f_inter_700, R.font.f_inter_800) }),
    POPPINS("Poppins", { tres(R.font.f_poppins_400, R.font.f_poppins_700, R.font.f_poppins_800) }),
    MONTSERRAT("Montserrat", { tres(R.font.f_montserrat_400, R.font.f_montserrat_700, R.font.f_montserrat_800) }),
    NUNITO("Nunito", { tres(R.font.f_nunito_400, R.font.f_nunito_700, R.font.f_nunito_800) }),
    SPACE_GROTESK("Space Grotesk", { tres(R.font.f_space_grotesk_400, R.font.f_space_grotesk_700, null) }),
    PLAYFAIR("Playfair Display", { tres(R.font.f_playfair_400, R.font.f_playfair_700, R.font.f_playfair_800) }),
    OSWALD("Oswald", { tres(R.font.f_oswald_400, R.font.f_oswald_700, null) });

    val familia: FontFamily by lazy(familia)

    companion object {
        /** Nome guardado (no aparelho ou na empresa) → fonte; desconhecido = Archivo. */
        fun de(nome: String?): FonteApp = entries.firstOrNull { it.name.equals(nome, ignoreCase = true) } ?: ARCHIVO
    }
}

/** A fonte escolhida por este usuário (guardada no aparelho). Ler aqui recompõe a tela quando muda. */
object Fontes {
    private var ctx: Context? = null
    val atual = androidx.compose.runtime.mutableStateOf(FonteApp.ARCHIVO)
    fun init(context: Context) {
        ctx = context.applicationContext
        atual.value = FonteApp.de(ctx!!.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE).getString("fonte", null))
    }
    fun definir(f: FonteApp) {
        atual.value = f
        ctx?.getSharedPreferences("pwex_tema", Context.MODE_PRIVATE)?.edit { putString("fonte", f.name) }
    }
}

/** Fonte do app inteiro: a escolhida pelo usuário (padrão Archivo). */
val Archivo: FontFamily get() = Fontes.atual.value.familia

/** Títulos "de letreiro": Archivo expandida e pesada, como no site (ou a fonte escolhida). */
val ArchivoLargo: FontFamily get() = if (Fontes.atual.value == FonteApp.ARCHIVO) ArchivoLargoBase else Fontes.atual.value.familia

private fun tipografia() = Typography(
    displaySmall = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight(780), fontSize = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight(780), fontSize = 26.sp, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontFamily = ArchivoLargo, fontWeight = FontWeight(780), fontSize = 22.sp),
    titleLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = Archivo, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Archivo, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 0.3.sp),
    labelSmall = TextStyle(fontFamily = Archivo, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 1.5.sp),
)

@Composable
fun PwTheme(content: @Composable () -> Unit) {
    val pref by TemaApp.preferencia.collectAsState()
    val estilo by TemaApp.estilo.collectAsState()
    val escuro = when {
        estilo == EstiloTema.BRANCO_NO_PRETO -> true
        estilo == EstiloTema.PRETO_NO_BRANCO -> false
        pref == PreferenciaTema.SISTEMA -> isSystemInDarkTheme()
        pref == PreferenciaTema.CLARO -> false
        else -> true
    }
    val teste by TemaApp.modoTeste.collectAsState()
    val vermelho by TemaApp.vermelho.collectAsState()
    val base = (if (escuro) Noturno else Claro).copy(dev = if (escuro) Color(0xFFF87171) else Color(0xFFDC2626))
    // mesmas cores do site; só os tons mudam, tamanhos e espaços ficam iguais em todos os temas
    val p = when {
        teste -> base.copy(
            ouro = if (escuro) Color(0xFFA78BFA) else Color(0xFF7C3AED),
            ouroClaro = if (escuro) Color(0xFFD4C6FF) else Color(0xFFA78BFA),
            ouroEscuro = if (escuro) Color(0xFF6D28D9) else Color(0xFF5B21B6),
            sobreOuro = Color(0xFFFFFFFF),
            brilho1 = Color(if (escuro) 0x33A78BFA else 0x407C3AED),
            brilho2 = Color(if (escuro) 0x14A78BFA else 0x1F7C3AED),
            marca = Marca.VIOLETA,
        )
        vermelho -> base.copy(
            ouro = if (escuro) Color(0xFFEF4444) else Color(0xFFDC2626),
            ouroClaro = if (escuro) Color(0xFFFCA5A5) else Color(0xFFF87171),
            ouroEscuro = Color(0xFF991B1B),
            sobreOuro = Color(0xFFFFFFFF),
            brilho1 = Color(if (escuro) 0x2EEF4444 else 0x38DC2626),
            brilho2 = Color(if (escuro) 0x12EF4444 else 0x1ADC2626),
            marca = Marca.VERMELHO,
        )
        estilo == EstiloTema.AZUL -> base.copy(
            ouro = if (escuro) Color(0xFF60A5FA) else Color(0xFF2563EB),
            ouroClaro = if (escuro) Color(0xFFBFDBFE) else Color(0xFF60A5FA),
            ouroEscuro = if (escuro) Color(0xFF1D4ED8) else Color(0xFF1E3A8A),
            sobreOuro = if (escuro) Color(0xFF06101F) else Color(0xFFFFFFFF),
            brilho1 = Color(if (escuro) 0x2960A5FA else 0x382563EB),
            brilho2 = Color(if (escuro) 0x1260A5FA else 0x1A2563EB),
            marca = Marca.AZUL,
        )
        estilo == EstiloTema.VERDE -> base.copy(
            ouro = if (escuro) Color(0xFF34D399) else Color(0xFF059669),
            ouroClaro = if (escuro) Color(0xFFA7F3D0) else Color(0xFF34D399),
            ouroEscuro = if (escuro) Color(0xFF047857) else Color(0xFF065F46),
            sobreOuro = if (escuro) Color(0xFF03140D) else Color(0xFFFFFFFF),
            brilho1 = Color(if (escuro) 0x2934D399 else 0x38059669),
            brilho2 = Color(if (escuro) 0x1234D399 else 0x1A059669),
            marca = Marca.VERDE,
        )
        estilo.noturno != null -> {
            val t = (if (escuro) estilo.noturno!! else (estilo.diurno ?: estilo.noturno!!)).map { Color(it) }
            val principal = t[1]
            base.copy(
                ouro = principal, ouroClaro = t[0], ouroEscuro = t[2],
                sobreOuro = if (escuro && principal.luminance() > 0.45f) Color(0xFF0A0A0A) else Color(0xFFFFFFFF),
                brilho1 = principal.copy(alpha = if (escuro) 0.17f else 0.22f), brilho2 = principal.copy(alpha = if (escuro) 0.07f else 0.10f),
                marca = Marca.TINTA,
            )
        }
        estilo == EstiloTema.BRANCO_NO_PRETO -> base.copy(
            fundo = Color(0xFF000000), fundo2 = Color(0xFF0A0A0A),
            vidro = Color(0xE00C0C0C), vidroForte = Color(0xF2141414),
            texto = Color(0xFFFFFFFF), texto2 = Color(0xFFB8B8B8), texto3 = Color(0xFF7C7C7C),
            ouro = Color(0xFFF2F2F2), ouroClaro = Color(0xFFFFFFFF), ouroEscuro = Color(0xFF9A9A9A), sobreOuro = Color(0xFF000000),
            brilho1 = Color(0x1FFFFFFF), brilho2 = Color(0x0AFFFFFF),
            marca = Marca.BRANCO,
        )
        estilo == EstiloTema.PRETO_NO_BRANCO -> base.copy(
            fundo = Color(0xFFFFFFFF), fundo2 = Color(0xFFF4F4F5),
            vidro = Color(0xE6FFFFFF), vidroForte = Color(0xF7FFFFFF), vidroSutil = Color(0x08000000),
            borda = Color(0x1A000000), bordaForte = Color(0x33000000),
            texto = Color(0xFF0A0A0A), texto2 = Color(0xFF52525B), texto3 = Color(0xFF8A8A8E),
            ouro = Color(0xFF111111), ouroClaro = Color(0xFF52525B), ouroEscuro = Color(0xFF000000), sobreOuro = Color(0xFFFFFFFF),
            brilho1 = Color(0x14000000), brilho2 = Color(0x08000000),
            marca = Marca.PRETO,
        )
        else -> base
    }
    val esquema = if (escuro) {
        darkColorScheme(
            primary = p.ouro, onPrimary = p.sobreOuro, secondary = p.ouroClaro, onSecondary = p.sobreOuro,
            background = p.fundo, onBackground = p.texto, surface = p.fundo2, onSurface = p.texto,
            surfaceVariant = p.vidroForte, onSurfaceVariant = p.texto2, outline = p.borda, error = p.vermelho,
        )
    } else {
        lightColorScheme(
            primary = p.ouro, onPrimary = p.sobreOuro, secondary = p.ouroClaro, onSecondary = p.sobreOuro,
            background = p.fundo, onBackground = p.texto, surface = p.fundo2, onSurface = p.texto,
            surfaceVariant = p.vidroForte, onSurfaceVariant = p.texto2, outline = p.borda, error = p.vermelho,
        )
    }
    // fonte do sistema muito grande quebrava os layouts: acompanha o ajuste do celular só até 1,15×
    val densidade = androidx.compose.ui.platform.LocalDensity.current
    val limitada = androidx.compose.ui.unit.Density(densidade.density, densidade.fontScale.coerceAtMost(1.15f))
    CompositionLocalProvider(LocalPw provides p, androidx.compose.ui.platform.LocalDensity provides limitada) {
        val fonte = Fontes.atual.value
        val tipo = androidx.compose.runtime.remember(fonte) { tipografia() }
        MaterialTheme(colorScheme = esquema, typography = tipo, content = content)
    }
}
