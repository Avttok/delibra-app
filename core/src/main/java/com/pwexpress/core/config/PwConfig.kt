package com.pwexpress.core.config

import com.pwexpress.core.BuildConfig

/** Configuração do servidor, definida em gradle.properties (ou variáveis de ambiente no CI). */
object PwConfig {
    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trimEnd('/')
    val supabaseKey: String = BuildConfig.SUPABASE_KEY

    /** Site do gestor: rotas do servidor que precisam da chave secreta (criar conta, CNPJ…). */
    private val siteUrlPadrao: String = BuildConfig.SITE_URL.ifBlank { "https://delibra-web.vercel.app" }.trimEnd('/')
    /** Domínio do site: o que o Dev escolheu em Dev → Sistema (dev_site_url) ou o padrão do build. */
    @Volatile var siteUrlEscolhido: String? = null
    val siteUrl: String get() = siteUrlEscolhido?.takeIf { it.startsWith("https://") }?.trimEnd('/') ?: siteUrlPadrao

    /** "dono/repositorio" no GitHub, de onde vêm as atualizações do app. */
    val githubRepo: String = BuildConfig.GITHUB_REPO.ifBlank { "Avttok/PWEx_App" }

    val configurado: Boolean
        get() = supabaseUrl.startsWith("https://") && supabaseKey.isNotBlank()
}
