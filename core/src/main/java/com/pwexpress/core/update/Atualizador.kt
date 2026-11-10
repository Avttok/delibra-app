package com.pwexpress.core.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.edit
import com.pwexpress.core.PwCore
import com.pwexpress.core.config.PwConfig
import com.pwexpress.core.json.arr
import com.pwexpress.core.json.long
import com.pwexpress.core.json.str
import com.pwexpress.core.notificacoes.PwNotificacoes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import okhttp3.Request
import java.io.File

/** Uma versão publicada no GitHub (Releases "build-N" com o APK anexado). */
data class Versao(val build: Int, val nome: String, val notas: String, val urlApk: String, val tamanho: Long, val publicadaEm: String?)

/**
 * O Dev escolheu a versão que deve rodar e esta não é ela: o app fica travado até atualizar.
 * [maisNova] = a instalada é mais nova que a escolhida (Android não volta versão sem reinstalar).
 */
data class BloqueioVersao(val exigida: Int, val versao: Versao, val maisNova: Boolean)

sealed interface EstadoAtualizacao {
    data object Ocioso : EstadoAtualizacao
    data object Verificando : EstadoAtualizacao
    data object EmDia : EstadoAtualizacao
    data class Disponivel(val versao: Versao) : EstadoAtualizacao
    data class Baixando(val versao: Versao, val baixado: Long, val total: Long) : EstadoAtualizacao {
        val fracao: Float get() = if (total > 0) (baixado.toFloat() / total).coerceIn(0f, 1f) else 0f
    }
    data class Instalando(val versao: Versao) : EstadoAtualizacao
    data class Erro(val mensagem: String, val versao: Versao?) : EstadoAtualizacao
}

/**
 * Atualização do app sem loja: procura no GitHub a versão mais nova, baixa mostrando o progresso
 * e entrega ao instalador do Android (o mesmo certificado garante que é uma atualização).
 */
object Atualizador {
    private var app: Context? = null
    private var buildAtual = 0
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var download: Job? = null

    private val _estado = MutableStateFlow<EstadoAtualizacao>(EstadoAtualizacao.Ocioso)
    val estado: StateFlow<EstadoAtualizacao> = _estado.asStateFlow()

    /** Janela aberta (o app mostra o diálogo quando true). */
    private val _janela = MutableStateFlow(false)
    val janela: StateFlow<Boolean> = _janela.asStateFlow()

    val versaoInstalada: Int get() = buildAtual

    /** Versão obrigatória (escolhida pelo Dev) diferente da instalada: o app inteiro fica atrás da janela. */
    private val _bloqueio = MutableStateFlow<BloqueioVersao?>(null)
    val bloqueio: StateFlow<BloqueioVersao?> = _bloqueio.asStateFlow()

    fun init(context: Context, build: Int) {
        app = context.applicationContext
        buildAtual = build
        com.pwexpress.core.network.SupabaseHttp.versaoApp = build
        com.pwexpress.core.network.SupabaseHttp.aoVersaoBloqueada = { escopo.launch { verificarObrigatoria() } }
        // o último bloqueio vale mesmo sem internet até o app confirmar de novo
        prefs().getInt("exigida", 0).takeIf { it > 0 && it != build }?.let { e ->
            prefs().getString("exigida_url", null)?.let { url -> _bloqueio.value = BloqueioVersao(e, Versao(e, "build $e", "", url, 0, null), build > e) }
        }
        // apaga APKs de atualizações já instaladas
        runCatching { pasta(context).listFiles()?.forEach { it.delete() } }
    }

    private fun pasta(ctx: Context) = File(ctx.cacheDir, "atualizacao").apply { mkdirs() }
    private fun prefs() = app!!.getSharedPreferences("pwex_atualizacao", Context.MODE_PRIVATE)

    fun abrirJanela() { _janela.value = true }
    fun fecharJanela() {
        if (_bloqueio.value != null) return // versão obrigatória: não dá para fechar
        _janela.value = false
        val e = _estado.value
        if (e is EstadoAtualizacao.Disponivel) prefs().edit { putInt("adiada", e.versao.build) }
    }

    /** `automatico`: na abertura do app. Só abre a janela se for uma versão nova que ainda não foi adiada. */
    fun verificar(automatico: Boolean = false) {
        if (_estado.value is EstadoAtualizacao.Baixando || _estado.value is EstadoAtualizacao.Verificando) return
        if (_bloqueio.value != null) { escopo.launch { verificarObrigatoria() }; return }
        escopo.launch {
            _estado.value = EstadoAtualizacao.Verificando
            val v = runCatching { buscarUltima() }.getOrElse { e ->
                val motivo = when (e) {
                    is java.net.UnknownHostException, is java.net.ConnectException -> "Sem conexão com a internet."
                    is java.net.SocketTimeoutException, is java.io.InterruptedIOException -> "A conexão demorou demais. Tente de novo."
                    else -> "Não consegui consultar as atualizações (${e.message ?: e.javaClass.simpleName})."
                }
                _estado.value = if (automatico) EstadoAtualizacao.Ocioso else EstadoAtualizacao.Erro(motivo, null)
                return@launch
            }
            if (v == null || v.build <= buildAtual) {
                _estado.value = if (automatico) EstadoAtualizacao.Ocioso else EstadoAtualizacao.EmDia
                return@launch
            }
            _estado.value = EstadoAtualizacao.Disponivel(v)
            if (!automatico || prefs().getInt("adiada", 0) < v.build) _janela.value = true
        }
    }

    private var consultando = false

    /**
     * Pergunta ao servidor qual versão deve rodar. Diferente da instalada (ou, com "sempre a mais nova",
     * há uma mais nova publicada) e o Dev mandou forçar: trava o app na janela de atualização.
     */
    suspend fun verificarObrigatoria() {
        if (consultando) return
        consultando = true
        try {
            val r = runCatching { PwCore.rpc.chamar("versao_app", JsonObject(emptyMap())) }.getOrNull() ?: return
            if (!r.ok) return
            val o = runCatching { Json.parseToJsonElement(r.corpo) as JsonObject }.getOrNull() ?: return
            val exigida = o.long("exigida")?.toInt() ?: 0
            val forcar = (o["forcar"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
            val devLivre = (o["dev_livre"] as? kotlinx.serialization.json.JsonPrimitive)?.content == "true"
            val novo: BloqueioVersao? = when {
                !forcar || devLivre -> null
                exigida > 0 && exigida == buildAtual -> null
                exigida > 0 -> {
                    val url = o.str("url") ?: return
                    // as notas vêm do versao.json quando a escolhida é a mais nova
                    val notas = runCatching { withContext(Dispatchers.IO) { buscarArquivo() } }.getOrNull()?.takeIf { it.build == exigida }
                    BloqueioVersao(exigida, notas ?: Versao(exigida, "build $exigida", "", url, 0, null), buildAtual > exigida)
                }
                else -> runCatching { buscarUltima() }.getOrNull()?.takeIf { it.build > buildAtual }?.let { BloqueioVersao(it.build, it, false) }
            }
            prefs().edit {
                if (novo != null && exigida > 0) { putInt("exigida", exigida); putString("exigida_url", novo.versao.urlApk) } else { remove("exigida"); remove("exigida_url") }
            }
            _bloqueio.value = novo
            if (novo != null) {
                val e = _estado.value
                if (e !is EstadoAtualizacao.Baixando && e !is EstadoAtualizacao.Instalando) _estado.value = EstadoAtualizacao.Disponivel(novo.versao)
                _janela.value = true
            }
        } finally { consultando = false }
    }

    /** Falha ao consultar, com o motivo real (aparece na janela). */
    private class FalhaConsulta(msg: String) : Exception(msg)

    /**
     * Versão mais nova publicada. Primeiro o arquivo `versao.json` da release fixa "atualizacao"
     * (download comum do GitHub, sem limite de consultas); se não existir, a API de releases
     * (limitada a 60 consultas por hora por rede sem login).
     */
    private suspend fun buscarUltima(): Versao? = withContext(Dispatchers.IO) {
        val motivoArquivo = runCatching { return@withContext buscarArquivo() }.exceptionOrNull()
        runCatching { buscarApi() }.getOrElse { e ->
            throw FalhaConsulta(listOfNotNull(e.message, motivoArquivo?.message?.let { "arquivo: $it" }).joinToString(" · "))
        }
    }

    private fun buscarArquivo(): Versao {
        val req = Request.Builder()
            .url("https://github.com/${PwConfig.githubRepo}/releases/download/atualizacao/versao.json?t=${System.currentTimeMillis() / 60_000}")
            .header("User-Agent", "PWExpress-app")
            .header("Cache-Control", "no-cache")
            .build()
        PwCore.http.client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) error("HTTP ${r.code}")
            val o = Json.parseToJsonElement(r.body!!.string()) as JsonObject
            return Versao(
                build = o.long("build")?.toInt() ?: error("sem build"),
                nome = o.str("nome") ?: "build ${o.long("build")}",
                notas = o.str("notas").orEmpty().trim(),
                urlApk = o.str("url") ?: error("sem url"),
                tamanho = o.long("tamanho") ?: 0L,
                publicadaEm = o.str("publicada_em"),
            )
        }
    }

    private fun buscarApi(): Versao? {
        val req = Request.Builder()
            .url("https://api.github.com/repos/${PwConfig.githubRepo}/releases?per_page=30")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "PWExpress-app")
            .build()
        PwCore.http.client.newCall(req).execute().use { r ->
            if (!r.isSuccessful) {
                val limite = r.header("X-RateLimit-Remaining") == "0"
                error(if (limite) "GitHub: limite de consultas desta rede atingido, tente em 1 h" else "GitHub respondeu ${r.code}")
            }
            return Json.parseToJsonElement(r.body!!.string()).jsonArray
                .mapNotNull { it as? JsonObject }
                .mapNotNull { rel ->
                    val build = rel.str("tag_name")?.substringAfter("build-")?.toIntOrNull() ?: return@mapNotNull null
                    val apk = rel.arr("assets").firstOrNull { it.str("name")?.endsWith(".apk") == true } ?: return@mapNotNull null
                    Versao(
                        build = build,
                        nome = rel.str("name") ?: "build $build",
                        notas = rel.str("body").orEmpty().lines().filterNot { it.startsWith("Commit:") }.joinToString("\n").trim(),
                        urlApk = apk.str("browser_download_url") ?: return@mapNotNull null,
                        tamanho = apk.long("size") ?: 0L,
                        publicadaEm = rel.str("published_at"),
                    )
                }
                .maxByOrNull { it.build }
        }
    }

    fun baixar() {
        val v = when (val e = _estado.value) {
            is EstadoAtualizacao.Disponivel -> e.versao
            is EstadoAtualizacao.Erro -> e.versao ?: return
            else -> return
        }
        val ctx = app ?: return
        download?.cancel()
        download = escopo.launch {
            _estado.value = EstadoAtualizacao.Baixando(v, 0, v.tamanho)
            val arquivo = File(pasta(ctx), "PWExpress-build${v.build}.apk")
            val ok = runCatching {
                withContext(Dispatchers.IO) {
                    val req = Request.Builder().url(v.urlApk).header("User-Agent", "PWExpress-app").build()
                    PwCore.http.client.newBuilder().callTimeout(java.time.Duration.ofMinutes(10)).readTimeout(java.time.Duration.ofSeconds(60)).build()
                        .newCall(req).execute().use { r ->
                            if (!r.isSuccessful) error("download ${r.code}")
                            val corpo = r.body ?: error("vazio")
                            val total = corpo.contentLength().takeIf { it > 0 } ?: v.tamanho
                            var lido = 0L
                            var ultimo = 0L
                            corpo.byteStream().use { entrada ->
                                arquivo.outputStream().use { saida ->
                                    val buf = ByteArray(64 * 1024)
                                    while (isActive) {
                                        val n = entrada.read(buf)
                                        if (n < 0) break
                                        saida.write(buf, 0, n)
                                        lido += n
                                        if (lido - ultimo > 128 * 1024 || lido == total) {
                                            ultimo = lido
                                            _estado.value = EstadoAtualizacao.Baixando(v, lido, total)
                                            notificarProgresso(ctx, v, lido, total)
                                        }
                                    }
                                }
                            }
                            if (total > 0 && lido < total) error("incompleto")
                        }
                }
            }.isSuccess
            PwNotificacoes.cancelar(ctx, NOTIF)
            if (!ok) {
                arquivo.delete()
                _estado.value = EstadoAtualizacao.Erro("O download falhou. Tente de novo.", v)
                return@launch
            }
            instalar(ctx, arquivo, v)
        }
    }

    private const val NOTIF = 4001
    private var ultimoPct = -1

    /** Progresso na barra de notificações (para quando o app vai para o fundo durante o download). */
    private fun notificarProgresso(ctx: Context, v: Versao, lido: Long, total: Long) {
        val pct = if (total > 0) (lido * 100 / total).toInt() else 0
        if (pct == ultimoPct) return
        ultimoPct = pct
        val n = PwNotificacoes.base(ctx, PwNotificacoes.CANAL_ATUALIZACAO)
            .setContentTitle("Baixando a atualização do Delibra")
            .setContentText("${lido / 1_048_576} de ${total / 1_048_576} MB · $pct%")
            .setProgress(100, pct, total <= 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(PwNotificacoes.abrirApp(ctx, NOTIF))
            .build()
        runCatching { ctx.getSystemService(android.app.NotificationManager::class.java)?.notify(NOTIF, n) }
    }

    fun cancelarDownload() {
        app?.let { PwNotificacoes.cancelar(it, NOTIF) }
        download?.cancel()
        val e = _estado.value
        if (e is EstadoAtualizacao.Baixando) _estado.value = EstadoAtualizacao.Disponivel(e.versao)
    }

    /** O Android exige que o usuário libere "instalar apps desconhecidos" para este app. */
    fun podeInstalar(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || ctx.packageManager.canRequestPackageInstalls()

    fun abrirPermissaoInstalar(ctx: Context) {
        val i = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { ctx.startActivity(i) }
    }

    private suspend fun instalar(ctx: Context, arquivo: File, v: Versao) {
        _estado.value = EstadoAtualizacao.Instalando(v)
        val ok = runCatching {
            withContext(Dispatchers.IO) {
                val pi = ctx.packageManager.packageInstaller
                val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                    setAppPackageName(ctx.packageName)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
                }
                val id = pi.createSession(params)
                pi.openSession(id).use { s ->
                    s.openWrite("pwexpress.apk", 0, arquivo.length()).use { out -> arquivo.inputStream().use { it.copyTo(out) }; s.fsync(out) }
                    val intent = Intent(ctx, InstaladorReceiver::class.java).setAction(InstaladorReceiver.ACAO)
                    val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
                    s.commit(PendingIntent.getBroadcast(ctx, id, intent, flags).intentSender)
                }
            }
        }
        if (ok.isFailure) _estado.value = EstadoAtualizacao.Erro("Não consegui abrir o instalador (${ok.exceptionOrNull()?.message}).", v)
    }

    /** Chamado pelo receiver com o resultado do instalador. */
    internal fun resultado(sucesso: Boolean, mensagem: String?) {
        val v = (_estado.value as? EstadoAtualizacao.Instalando)?.versao
        if (!sucesso) _estado.value = EstadoAtualizacao.Erro(mensagem ?: "A instalação foi cancelada.", v)
    }
}
