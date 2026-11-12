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

    /**
     * Troca a senha da própria conta: confere a senha atual (login de novo) e grava a nova.
     * A sessão continua (o token novo substitui o antigo).
     */
    suspend fun trocarSenha(atual: String, nova: String): Result<Unit> = try {
        val s = _sessao.value ?: throw AuthErro("Entre de novo para trocar a senha.")
        if (nova.length < 6) throw AuthErro("A senha nova precisa ter pelo menos 6 caracteres.")
        if (nova == atual) throw AuthErro("A senha nova é igual à atual.")
        val conf = http.post("/auth/v1/token?grant_type=password", buildJsonObject { put("email", s.email); put("password", atual) }.toString())
        if (!conf.ok) throw AuthErro(if (conf.codigo == 400) "A senha atual não confere." else mensagemPorCodigo(conf))
        val nova1 = parseSessao(conf.corpo)
        guardar(nova1)
        val r = http.requisitar("PUT", "/auth/v1/user", buildJsonObject { put("password", nova) }.toString(), bearer = nova1.accessToken)
        if (!r.ok) throw AuthErro(when {
            r.corpo.contains("weak", true) || r.corpo.contains("characters", true) -> "Senha fraca demais. Use letras e números."
            r.corpo.contains("same", true) -> "A senha nova é igual à atual."
            else -> mensagemPorCodigo(r)
        })
        // a senha mudou: o acesso de um toque salvo continua valendo com o token novo
        contas.atualizarToken(nova1.email, nova1.refreshToken)
        Result.success(Unit)
    } catch (e: AuthErro) {
        Result.failure(e)
    } catch (e: IOException) {
        Result.failure(AuthErro("Sem conexão com o servidor. Verifique a internet."))
    } catch (e: Exception) {
        Result.failure(AuthErro("Erro inesperado: ${e.message}"))
    }

    // ------------------------------------------------------------------ verificação em dois passos (TOTP)

    data class Fator(val id: String, val nome: String, val verificado: Boolean)
    data class NovoFator(val id: String, val segredo: String, val uri: String)

    private fun aal(token: String): String = runCatching {
        val p = token.split(".")[1]
        val json = String(android.util.Base64.decode(p, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP))
        Json.parseToJsonElement(json).jsonObject["aal"]?.jsonPrimitive?.content ?: "aal1"
    }.getOrDefault("aal1")

    /** Fatores (aplicativos autenticadores) da conta. */
    suspend fun fatores(): Result<List<Fator>> = try {
        val t = tokenValido() ?: throw AuthErro("Entre de novo.")
        val r = http.requisitar("GET", "/auth/v1/user", null, bearer = t)
        if (!r.ok) throw AuthErro(mensagemPorCodigo(r))
        val l = (Json.parseToJsonElement(r.corpo).jsonObject["factors"] as? kotlinx.serialization.json.JsonArray).orEmpty().mapNotNull { e ->
            val o = e as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
            if (o["factor_type"]?.jsonPrimitive?.content != "totp") return@mapNotNull null
            Fator(o.getValue("id").jsonPrimitive.content, o["friendly_name"]?.jsonPrimitive?.content ?: "Autenticador", o["status"]?.jsonPrimitive?.content == "verified")
        }
        Result.success(l)
    } catch (e: AuthErro) { Result.failure(e) } catch (e: IOException) { Result.failure(AuthErro("Sem conexão com o servidor.")) }

    /** Conta com autenticador e sessão ainda sem o código: o app pede o código antes de seguir. */
    suspend fun precisaCodigo(): String? {
        val t = runCatching { tokenValido() }.getOrNull() ?: return null
        if (aal(t) == "aal2") return null
        return fatores().getOrNull()?.firstOrNull { it.verificado }?.id
    }

    /** Começa a cadastrar um autenticador: devolve o segredo e o link otpauth:// para o app autenticador. */
    suspend fun cadastrarFator(): Result<NovoFator> = try {
        val t = tokenValido() ?: throw AuthErro("Entre de novo.")
        // tira tentativas antigas não confirmadas
        fatores().getOrNull()?.filter { !it.verificado }?.forEach { http.requisitar("DELETE", "/auth/v1/factors/${it.id}", null, bearer = t) }
        val nome = "Delibra app ${java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date())}"
        val r = http.post("/auth/v1/factors", buildJsonObject { put("factor_type", "totp"); put("friendly_name", nome) }.toString(), bearer = t)
        if (!r.ok) throw AuthErro(if (r.codigo == 422) "Esse recurso não está ligado no servidor." else mensagemPorCodigo(r))
        val o = Json.parseToJsonElement(r.corpo).jsonObject
        val totp = o["totp"]?.jsonObject
        Result.success(NovoFator(o.getValue("id").jsonPrimitive.content, totp?.get("secret")?.jsonPrimitive?.content.orEmpty(), totp?.get("uri")?.jsonPrimitive?.content.orEmpty()))
    } catch (e: AuthErro) { Result.failure(e) } catch (e: IOException) { Result.failure(AuthErro("Sem conexão com o servidor.")) }

    /** Confere o código de 6 dígitos (cadastro ou entrada) e troca a sessão pela verificada. */
    suspend fun verificarCodigo(fator: String, codigo: String): Result<Unit> = try {
        val t = tokenValido() ?: throw AuthErro("Entre de novo.")
        val ch = http.post("/auth/v1/factors/$fator/challenge", "{}", bearer = t)
        if (!ch.ok) throw AuthErro(mensagemPorCodigo(ch))
        val chId = Json.parseToJsonElement(ch.corpo).jsonObject.getValue("id").jsonPrimitive.content
        val v = http.post("/auth/v1/factors/$fator/verify", buildJsonObject { put("challenge_id", chId); put("code", codigo.filter { it.isDigit() }) }.toString(), bearer = t)
        if (!v.ok) throw AuthErro(if (v.codigo in 400..422) "Código errado ou vencido. Confira a hora do celular e tente o código novo." else mensagemPorCodigo(v))
        val nova = parseSessao(v.corpo)
        guardar(nova)
        contas.atualizarToken(nova.email.ifBlank { _sessao.value?.email.orEmpty() }, nova.refreshToken)
        Result.success(Unit)
    } catch (e: AuthErro) { Result.failure(e) } catch (e: IOException) { Result.failure(AuthErro("Sem conexão com o servidor.")) }

    /** Tira um autenticador da conta. */
    suspend fun removerFator(fator: String): Result<Unit> = try {
        val t = tokenValido() ?: throw AuthErro("Entre de novo.")
        val r = http.requisitar("DELETE", "/auth/v1/factors/$fator", null, bearer = t)
        if (!r.ok) throw AuthErro(if (r.codigo == 403) "Para remover, entre com o código do autenticador primeiro." else mensagemPorCodigo(r))
        Result.success(Unit)
    } catch (e: AuthErro) { Result.failure(e) } catch (e: IOException) { Result.failure(AuthErro("Sem conexão com o servidor.")) }

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
