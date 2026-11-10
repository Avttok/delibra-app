package com.pwexpress.core.network

import com.pwexpress.core.auth.AuthRepository
import com.pwexpress.core.config.PwConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class NaoAutenticadoException : Exception("Sessão expirada. Entre novamente.")

/** Erro devolvido pelo servidor, já com a mensagem traduzida quando conhecida. */
class ErroServidor(val codigo: String, mensagem: String, val http: Int = 0) : Exception(mensagem)

/**
 * Acesso ao servidor com o token do usuário, renovando a sessão se necessário:
 * funções do banco (RPC), tabelas (REST), Storage e as rotas do site do gestor.
 */
class SupabaseRpc(private val http: SupabaseHttp, private val auth: AuthRepository) {

    private suspend fun comToken(chamada: suspend (String) -> HttpResposta): HttpResposta {
        val token = auth.tokenValido() ?: throw NaoAutenticadoException()
        val r = chamada(token)
        if (r.codigo != 401) return r
        val novo = auth.tokenValido(forcarRenovacao = true) ?: throw NaoAutenticadoException()
        return chamada(novo)
    }

    suspend fun chamar(funcao: String, args: JsonObject): HttpResposta =
        comToken { http.post("/rest/v1/rpc/$funcao", args.toString(), it) }

    /** Tabela direto (as regras de acesso do banco valem). Ex.: rest("GET", "/rest/v1/empresas?select=*"). */
    suspend fun rest(metodo: String, caminho: String, corpo: String? = null, prefer: String? = null): HttpResposta =
        comToken { http.requisitar(metodo, caminho, corpo, it, if (prefer != null) mapOf("Prefer" to prefer) else emptyMap()) }

    /** Rotas do site (Vercel) que exigem a chave secreta do Supabase no servidor. */
    suspend fun site(caminho: String, corpo: JsonObject? = null, metodo: String = "POST"): HttpResposta =
        comToken { http.requisitar(metodo, PwConfig.siteUrl + caminho, corpo?.toString(), it) }

    /** Envia arquivo para um bucket do Storage. Ex.: enviarArquivo("comprovantes", "corrida/foto.jpg", bytes, "image/jpeg"). */
    suspend fun enviarArquivo(bucket: String, caminho: String, bytes: ByteArray, tipo: String): HttpResposta {
        val token = auth.tokenValido() ?: throw NaoAutenticadoException()
        val url = "/storage/v1/object/$bucket/$caminho"
        val r = http.postBytes(url, bytes, tipo, token)
        if (r.codigo != 401 && r.codigo != 403) return r
        val novo = auth.tokenValido(forcarRenovacao = true) ?: throw NaoAutenticadoException()
        return http.postBytes(url, bytes, tipo, novo)
    }

    /** Link temporário para um arquivo privado (ex.: foto do comprovante). */
    suspend fun linkAssinado(bucket: String, caminho: String, segundos: Int = 3600): String? {
        val r = comToken { http.requisitar("POST", "/storage/v1/object/sign/$bucket/$caminho", buildJsonObject { put("expiresIn", segundos) }.toString(), it) }
        if (!r.ok) return null
        val rel = runCatching { Json.parseToJsonElement(r.corpo).jsonObject["signedURL"]?.jsonPrimitive?.content }.getOrNull() ?: return null
        return PwConfig.supabaseUrl + "/storage/v1" + rel
    }

    suspend fun removerArquivos(bucket: String, caminhos: List<String>): HttpResposta =
        comToken {
            http.requisitar("DELETE", "/storage/v1/object/$bucket",
                JsonObject(mapOf("prefixes" to JsonArray(caminhos.map { c -> JsonPrimitive(c) }))).toString(), it)
        }
}
