package com.pwexpress.core.erros

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import com.pwexpress.core.PwCore
import com.pwexpress.core.network.NaoAutenticadoException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import java.io.File
import java.time.Instant

/**
 * Relatório de travamentos: quando o app fecha sozinho, guarda o erro num arquivo e manda na próxima abertura
 * (Console Dev → Saúde do sistema → Travamentos do app). Erros pegos que não derrubam o app também podem ser
 * mandados com [registrar]. Nada aqui pode fazer o app travar de novo: tudo em try/catch.
 */
object RelatorioErros {
    private const val ARQUIVO = "erros_pendentes.json"
    private const val MAXIMO = 20
    private var versao = "?"
    private var telaAtual: String? = null
    private lateinit var arquivo: File
    private val escopo = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(app: Application, versaoApp: String) {
        versao = versaoApp
        arquivo = File(app.filesDir, ARQUIVO)
        // a tela aberta entra no relatório (só o nome da Activity)
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(a: Activity) { telaAtual = a.javaClass.simpleName }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityStarted(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivityStopped(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
        val anterior = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching { guardar(item("travou", e, "thread ${t.name}")) }
            anterior?.uncaughtException(t, e)
        }
        enviarPendentes()
    }

    /** Marca a tela atual com um nome mais útil (ex.: "Corrida", "Mapa do gestor"). */
    fun tela(nome: String) { telaAtual = nome }

    /** Erro pego que não derrubou o app, mas vale saber. */
    fun registrar(e: Throwable, contexto: String? = null) {
        if (!::arquivo.isInitialized) return
        runCatching { guardar(item("erro", e, contexto)) }
        enviarPendentes()
    }

    private fun item(tipo: String, e: Throwable, contexto: String?) = buildJsonObject {
        put("tipo", tipo)
        put("versao", versao)
        put("aparelho", "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80))
        put("android", Build.VERSION.RELEASE ?: "")
        put("tela", telaAtual ?: "")
        put("mensagem", listOfNotNull(e.javaClass.simpleName + (e.message?.let { ": $it" } ?: ""), contexto).joinToString(" · ").take(500))
        put("pilha", e.stackTraceToString().take(8000))
        put("quando", Instant.now().toString())
    }

    @Synchronized
    private fun guardar(novo: JsonObject) {
        val lista = ler().toMutableList()
        lista += novo
        arquivo.writeText(JsonArray(lista.takeLast(MAXIMO)).toString())
    }

    @Synchronized
    private fun ler(): List<kotlinx.serialization.json.JsonElement> =
        runCatching { if (arquivo.exists()) Json.parseToJsonElement(arquivo.readText()).jsonArray.toList() else emptyList() }.getOrDefault(emptyList())

    /** Manda o que ficou guardado; só apaga o arquivo se o servidor recebeu. */
    fun enviarPendentes() {
        if (!::arquivo.isInitialized) return
        escopo.launch {
            runCatching {
                val itens = ler()
                if (itens.isEmpty()) return@runCatching
                val corpo = buildJsonObject { put("p_itens", JsonArray(itens)) }
                val r = try { PwCore.rpc.chamar("registrar_erros_app", corpo) }
                        catch (_: NaoAutenticadoException) { PwCore.http.post("/rest/v1/rpc/registrar_erros_app", corpo.toString(), null) }
                if (r.ok) synchronized(this@RelatorioErros) { arquivo.delete() }
            }
        }
    }

}
