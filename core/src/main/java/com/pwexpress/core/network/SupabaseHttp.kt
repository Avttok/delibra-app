package com.pwexpress.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class HttpResposta(val codigo: Int, val corpo: String) {
    val ok: Boolean get() = codigo in 200..299
}

/** Cliente HTTP mínimo para a API REST do Supabase (Auth + PostgREST + Storage) e para o site. */
class SupabaseHttp(private val baseUrl: String, private val apiKey: String) {

    companion object {
        /** Build instalado: vai em todo pedido ao Supabase (o servidor recusa versões que o Dev não escolheu). */
        @Volatile var versaoApp: Int = 0
        /** Chamado quando o servidor responde "versao_nao_autorizada". */
        @Volatile var aoVersaoBloqueada: (() -> Unit)? = null
    }

    private val hostSupabase = runCatching { java.net.URI(baseUrl).host }.getOrNull()

    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { cadeia ->
            val req = cadeia.request()
            val doSupabase = req.url.host == hostSupabase
            val r = cadeia.proceed(if (doSupabase && versaoApp > 0) req.newBuilder().header("x-pwex-versao", versaoApp.toString()).build() else req)
            if (doSupabase && r.code in 400..499 && runCatching { r.peekBody(4096).string() }.getOrNull()?.contains("versao_nao_autorizada") == true) {
                aoVersaoBloqueada?.invoke()
            }
            r
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    private val tipoJson = "application/json; charset=utf-8".toMediaType()

    /** Lança IOException quando não há conexão. */
    suspend fun post(caminho: String, corpo: String, bearer: String? = null): HttpResposta =
        requisitar("POST", caminho, corpo, bearer)

    /**
     * Requisição genérica. `caminho` relativo vai para o Supabase; URL completa (https://…) vai direto
     * (o site do gestor, por exemplo). Lança IOException sem conexão.
     */
    suspend fun requisitar(
        metodo: String,
        caminho: String,
        corpo: String? = null,
        bearer: String? = null,
        cabecalhos: Map<String, String> = emptyMap(),
    ): HttpResposta = withContext(Dispatchers.IO) {
        val url = if (caminho.startsWith("http")) caminho else baseUrl + caminho
        val body = corpo?.toRequestBody(tipoJson)
        val b = Request.Builder().url(url).method(metodo, if (metodo == "GET") null else body ?: "".toRequestBody(tipoJson))
        if (!caminho.startsWith("http")) b.header("apikey", apiKey)
        if (bearer != null) b.header("Authorization", "Bearer $bearer")
        cabecalhos.forEach { (k, v) -> b.header(k, v) }
        client.newCall(b.build()).execute().use { r -> HttpResposta(r.code, r.body?.string().orEmpty()) }
    }

    /** Envia bytes (ex.: foto) para o Storage. Lança IOException sem conexão. */
    suspend fun postBytes(caminho: String, bytes: ByteArray, tipo: String, bearer: String): HttpResposta =
        withContext(Dispatchers.IO) {
            val req = Request.Builder()
                .url(baseUrl + caminho)
                .post(bytes.toRequestBody(tipo.toMediaType()))
                .header("apikey", apiKey)
                .header("Authorization", "Bearer $bearer")
                .header("x-upsert", "true")
                .build()
            client.newCall(req).execute().use { r -> HttpResposta(r.code, r.body?.string().orEmpty()) }
        }
}
