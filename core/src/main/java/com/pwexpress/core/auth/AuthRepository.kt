package com.pwexpress.core.auth

import com.pwexpress.core.network.HttpResposta
import com.pwexpress.core.network.SupabaseHttp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.IOException

class AuthErro(mensagem: String) : Exception(mensagem)

/** Login por e-mail/senha no Supabase Auth, com renovação automática do token. */
class AuthRepository(
    private val http: SupabaseHttp,
    private val store: SessaoStore,
    val contas: ContasSalvas,
) {

    private val mutex = Mutex()
    private val _sessao = MutableStateFlow(store.ler())
    val sessao: StateFlow<Sessao?> = _sessao.asStateFlow()

    /** Marcado no login: mantém a sessão entre aberturas do app e guarda o acesso de um toque. */
    var permanecerConectado: Boolean
        get() = store.permanecer
        set(v) { store.permanecer = v }

    suspend fun entrar(email: String, senha: String, permanecer: Boolean = true): Result<Unit> = try {
        val corpo = buildJsonObject {
            put("email", email.trim())
            put("password", senha)
        }.toString()
        val r = http.post("/auth/v1/token?grant_type=password", corpo)
        if (r.ok) {
            val sessao = parseSessao(r.corpo)
            store.permanecer = permanecer
            store.entrouEm = System.currentTimeMillis()
            guardar(sessao)
            contas.lembrar(sessao.email.ifBlank { email }, if (permanecer) sessao.refreshToken else null)
            Result.success(Unit)
        } else {
            Result.failure(AuthErro(mensagemErro(r)))
        }
    } catch (e: IOException) {
        Result.failure(AuthErro("Sem conexão com o servidor. Verifique a internet."))
    } catch (e: Exception) {
        Result.failure(AuthErro("Erro inesperado: ${e.message}"))
    }

    /** Entra com um toque usando o acesso salvo de uma conta ("Logar com"). */
    suspend fun entrarComContaSalva(conta: ContaSalva): Result<Unit> = try {
        val token = conta.refreshToken ?: throw AuthErro("Digite a senha desta conta.")
        val r = http.post("/auth/v1/token?grant_type=refresh_token", buildJsonObject { put("refresh_token", token) }.toString())
        if (r.ok) {
            val sessao = parseSessao(r.corpo)
            store.permanecer = true
            store.entrouEm = System.currentTimeMillis()
            guardar(sessao)
            contas.lembrar(sessao.email.ifBlank { conta.email }, sessao.refreshToken)
            Result.success(Unit)
        } else {
            contas.esquecerToken(conta.email) // acesso expirou: pede a senha
            Result.failure(AuthErro("O acesso salvo expirou. Digite a senha."))
        }
    } catch (e: IOException) {
        Result.failure(AuthErro("Sem conexão com o servidor. Verifique a internet."))
    }

    /**
     * Sem "Permanecer conectado", a sessão expira 12 h depois do login (um turno).
     * Não desloga a cada reinício do app porque o Android reinicia o rastreamento em segundo plano.
     * Chamado no início do processo (Application.onCreate).
     */
    fun aplicarPermanencia() {
        val expirou = System.currentTimeMillis() - store.entrouEm > 12 * 60 * 60 * 1000L
        if (!store.permanecer && expirou && _sessao.value != null) {
            store.limpar()
            _sessao.value = null
        }
    }

    /**
     * Devolve um access token válido (renovando se faltar menos de 1 min),
     * ou null se a sessão não existe mais. Lança IOException sem internet.
     */
    suspend fun tokenValido(forcarRenovacao: Boolean = false): String? = mutex.withLock {
        val atual = _sessao.value ?: return@withLock null
        val expirando = atual.expiraEm - System.currentTimeMillis() < 60_000
        if (!forcarRenovacao && !expirando) return@withLock atual.accessToken

        val corpo = buildJsonObject { put("refresh_token", atual.refreshToken) }.toString()
        val r = http.post("/auth/v1/token?grant_type=refresh_token", corpo)
        when {
            r.ok -> parseSessao(r.corpo).also {
                guardar(it)
                contas.atualizarToken(it.email.ifBlank { atual.email }, it.refreshToken)
            }.accessToken
            r.codigo in 400..499 -> { sair(); null } // sessão revogada
            else -> atual.accessToken                // servidor instável: tenta com o atual
        }
    }

    fun sair() {
        store.limpar()
        _sessao.value = null
    }

    private fun guardar(s: Sessao) {
        store.salvar(s)
        _sessao.value = s
    }

    private fun parseSessao(corpo: String): Sessao {
        val o = Json.parseToJsonElement(corpo).jsonObject
        val user = o["user"]?.jsonObject
        val expiraEmSeg = o["expires_in"]?.jsonPrimitive?.longOrNull ?: 3600L
        return Sessao(
            accessToken = o.getValue("access_token").jsonPrimitive.content,
            refreshToken = o.getValue("refresh_token").jsonPrimitive.content,
            expiraEm = System.currentTimeMillis() + expiraEmSeg * 1000,
            userId = user?.get("id")?.jsonPrimitive?.content.orEmpty(),
            email = user?.get("email")?.jsonPrimitive?.content.orEmpty(),
        )
    }

    private fun mensagemErro(r: HttpResposta): String = when {
        r.corpo.contains("banned", ignoreCase = true) -> "Seu acesso foi bloqueado pelo gestor. Fale com ele para liberar."
        else -> mensagemPorCodigo(r)
    }

    private fun mensagemPorCodigo(r: HttpResposta): String = when (r.codigo) {
        400 -> "E-mail ou senha incorretos."
        422 -> "E-mail ou senha em formato inválido."
        429 -> "Muitas tentativas. Aguarde um minuto e tente de novo."
        else -> "Erro no servidor (${r.codigo})."
    }
}
