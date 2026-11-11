package com.pwexpress.core.marca

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Marca da empresa (organização) da conta. Quando o dono ou o Dev envia um ícone pelo site,
 * ele entra no lugar do Delibra no app inteiro (topo, telas de entrada, notificações) e pode
 * virar o ícone do atalho na tela inicial do celular. Fica guardada no aparelho: o app já abre
 * com ela, mesmo sem internet.
 */
data class Marca(val nome: String, val logoUrl: String?, val cor: String?, val nomeApp: String? = null) {
    /** Nome no lugar de "Delibra": o escolhido pela empresa, ou o da empresa quando ela tem ícone. */
    val nomeExibido: String? get() = nomeApp ?: nome.takeIf { logoUrl != null }
    val propria: Boolean get() = logoUrl != null || nomeApp != null
}

object MarcaEmpresa {
    private const val PREFS = "delibra_marca"
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var app: Context? = null

    private val _marca = MutableStateFlow<Marca?>(null)
    val marca: StateFlow<Marca?> = _marca.asStateFlow()

    /** Ícone já baixado (quadrado), para notificações e atalho. */
    @Volatile var icone: Bitmap? = null
        private set
    private val _versaoIcone = MutableStateFlow(0)
    val versaoIcone: StateFlow<Int> = _versaoIcone.asStateFlow()

    private fun arquivo(ctx: Context) = File(ctx.filesDir, "marca_empresa.png")

    fun iniciar(ctx: Context) {
        if (app != null) return
        app = ctx.applicationContext
        val p = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val nome = p.getString("nome", null)
        if (nome != null) _marca.value = Marca(nome, p.getString("logo", null), p.getString("cor", null), p.getString("nome_app", null))
        val f = arquivo(ctx)
        if (f.exists() && _marca.value?.logoUrl != null) icone = runCatching { BitmapFactory.decodeFile(f.path) }.getOrNull()
    }

    /** O perfil chegou do servidor: guarda a marca e baixa o ícone se mudou. `null` = sem empresa. */
    fun atualizar(m: Marca?) {
        val ctx = app ?: return
        val antes = _marca.value
        if (antes == m) return
        _marca.value = m
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().apply {
            if (m == null) clear() else { putString("nome", m.nome); putString("logo", m.logoUrl); putString("cor", m.cor); putString("nome_app", m.nomeApp) }
        }.apply()
        if (m?.logoUrl == null) {
            icone = null; arquivo(ctx).delete(); _versaoIcone.value++
            atualizarAtalho(ctx)
            return
        }
        if (antes?.logoUrl != m.logoUrl || icone == null) escopo.launch {
            val bmp = runCatching { URL(m.logoUrl).openStream().use { BitmapFactory.decodeStream(it) } }.getOrNull() ?: return@launch
            runCatching { arquivo(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) } }
            icone = bmp
            _versaoIcone.value++
            atualizarAtalho(ctx)
        }
    }

    private fun corMarca(): Int = runCatching { android.graphics.Color.parseColor(_marca.value?.cor ?: "#D4AF37") }.getOrDefault(0xFFD4AF37.toInt())

    /** Fundo escuro com o brilho da cor da empresa e a logo inteira por cima (sem cortar). */
    private fun comBrilho(b: Bitmap, lado: Int, dentro: Float, redondo: Boolean): Bitmap {
        val saida = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
        val tela = Canvas(saida)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val meio = lado / 2f
        p.color = 0xFF0B0B0D.toInt()
        if (redondo) tela.drawCircle(meio, meio, meio, p) else tela.drawRect(0f, 0f, lado.toFloat(), lado.toFloat(), p)
        val cor = corMarca()
        p.shader = android.graphics.RadialGradient(meio, meio, lado * 0.48f,
            intArrayOf((cor and 0x00FFFFFF) or 0x88000000.toInt(), (cor and 0x00FFFFFF) or 0x2A000000, cor and 0x00FFFFFF), floatArrayOf(0f, 0.55f, 1f), Shader.TileMode.CLAMP)
        tela.drawCircle(meio, meio, lado * 0.48f, p)
        p.shader = null
        val area = lado * dentro
        val esc = minOf(area / b.width, area / b.height)
        val w = b.width * esc; val h = b.height * esc
        tela.drawBitmap(b, null, RectF(meio - w / 2, meio - h / 2, meio + w / 2, meio + h / 2), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        return saida
    }

    /** Ícone redondo para o ícone grande das notificações. */
    fun iconeRedondo(lado: Int = 192): Bitmap? = icone?.let { comBrilho(it, lado, 0.72f, redondo = true) }

    /** Ícone "adaptável" (com margem de segurança) para o atalho da tela inicial. */
    private fun iconeAtalho(): Bitmap? {
        val b = icone ?: app?.let { runCatching { BitmapFactory.decodeResource(it.resources, com.pwexpress.core.R.drawable.pwex_marca_ouro) }.getOrNull() } ?: return null
        return comBrilho(b, 432, 0.6f, redondo = false)
    }

    fun podeFixar(ctx: Context) = (icone != null || _marca.value?.logoUrl == null) && _marca.value?.propria == true && ShortcutManagerCompat.isRequestPinShortcutSupported(ctx)

    /**
     * Pede ao celular para colocar na tela inicial um atalho com o ícone e o nome da empresa.
     * (O Android não deixa um app trocar o próprio ícone por uma imagem qualquer; o atalho é o
     * jeito oficial. O ícone do Delibra continua na gaveta de apps.)
     */
    suspend fun fixarNaTelaInicial(ctx: Context): Boolean = withContext(Dispatchers.Main) {
        val bmp = iconeAtalho() ?: return@withContext false
        val m = _marca.value ?: return@withContext false
        val abrir = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply { action = Intent.ACTION_MAIN } ?: return@withContext false
        val atalho = ShortcutInfoCompat.Builder(ctx, "marca_empresa")
            .setShortLabel((m.nomeExibido ?: "Delibra").take(24))
            .setLongLabel(m.nomeExibido ?: "Delibra")
            .setIcon(IconCompat.createWithAdaptiveBitmap(bmp))
            .setIntent(abrir)
            .build()
        runCatching {
            if (ShortcutManagerCompat.getShortcuts(ctx, ShortcutManagerCompat.FLAG_MATCH_PINNED).any { it.id == "marca_empresa" })
                ShortcutManagerCompat.updateShortcuts(ctx, listOf(atalho))
            ShortcutManagerCompat.requestPinShortcut(ctx, atalho, null)
        }.getOrDefault(false)
    }

    /** Ícone mudou e o atalho já está na tela: atualiza a imagem dele. */
    fun atualizarAtalho(ctx: Context) {
        val bmp = iconeAtalho() ?: return
        val m = _marca.value ?: return
        runCatching {
            if (ShortcutManagerCompat.getShortcuts(ctx, ShortcutManagerCompat.FLAG_MATCH_PINNED).none { it.id == "marca_empresa" }) return
            val abrir = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.apply { action = Intent.ACTION_MAIN } ?: return
            ShortcutManagerCompat.updateShortcuts(ctx, listOf(ShortcutInfoCompat.Builder(ctx, "marca_empresa")
                .setShortLabel((m.nomeExibido ?: "Delibra").take(24)).setLongLabel(m.nomeExibido ?: "Delibra").setIcon(IconCompat.createWithAdaptiveBitmap(bmp)).setIntent(abrir).build()))
        }
    }
}
