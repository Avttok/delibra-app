package com.pwexpress.feature.gestor.ui

import com.pwexpress.core.tentar

import com.pwexpress.core.json.str

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import com.pwexpress.core.designsystem.PwPilula
import com.pwexpress.core.designsystem.PwPonto
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pwexpress.core.designsystem.ArchivoLargo
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwAvatar
import com.pwexpress.core.designsystem.PwBotaoFlutuante
import com.pwexpress.core.designsystem.PwBotaoIcone
import com.pwexpress.core.designsystem.PwBotaoOuro
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwCarregando
import com.pwexpress.core.designsystem.PwCartao
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwLinha
import com.pwexpress.core.designsystem.PwNumero
import com.pwexpress.core.designsystem.PwPagina
import com.pwexpress.core.designsystem.PwPasso
import com.pwexpress.core.designsystem.PwRotulo
import com.pwexpress.core.designsystem.PwSegmentado
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwSelo
import com.pwexpress.core.designsystem.PwVazio
import com.pwexpress.core.designsystem.TemaApp
import com.pwexpress.core.designsystem.moedaBr
import com.pwexpress.core.json.dbl
import com.pwexpress.core.json.int
import com.pwexpress.core.mapa.MarcadorMapa
import com.pwexpress.core.mapa.PinoMapa
import com.pwexpress.core.mapa.coordenadaValida
import androidx.compose.ui.graphics.toArgb
import com.pwexpress.core.mapa.PwMapa
import com.pwexpress.feature.gestor.data.CorridaG
import com.pwexpress.feature.gestor.data.GRUPOS_CONFIG
import com.pwexpress.feature.gestor.data.AREAS_CONFIG
import com.pwexpress.feature.gestor.data.Geo
import com.pwexpress.feature.gestor.data.Gestao
import com.pwexpress.feature.gestor.data.ItemRegistro
import com.pwexpress.feature.gestor.data.PARAMETROS
import com.pwexpress.feature.gestor.data.PARAMETROS_VISIVEIS
import com.pwexpress.feature.gestor.data.Param
import com.pwexpress.feature.gestor.data.Precos
import com.pwexpress.feature.gestor.data.logico
import com.pwexpress.feature.gestor.data.numero
import com.pwexpress.feature.gestor.data.textoCfg
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// =====================================================================
// Empresas
// =====================================================================

@Composable
fun EmpresasG(onAbrir: (String?) -> Unit) {
    val dados by Gestao.dados.collectAsState()
    var busca by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var tipo by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    val filtros: List<Pair<String, (com.pwexpress.feature.gestor.data.EmpresaG) -> Boolean>> = listOf(
        "Todos" to { _ -> true }, "Empresas" to { e -> e.tipo != "pf" }, "Pessoas" to { e -> e.tipo == "pf" },
        "Express" to { e -> e.podeExpress }, "Só agendados" to { e -> !e.podeExpress }, "Teste" to { e -> e.modoTeste },
    )
    val lista = dados.empresas.filter { e ->
        filtros[tipo].second(e) && combinaBusca(busca, e.nome, e.razaoSocial, e.nomeFantasia, e.documento, e.documento?.filter { it.isDigit() }, e.email, e.telefone, e.endereco)
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TituloTela("Clientes", if (dados.empresas.size == 1) "1 cliente cadastrado" else "${dados.empresas.size} clientes cadastrados", PwIcones.Building2) }
            item { CadastrosPendentesG() }
            if (dados.empresas.size > 0) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoBusca(busca, { busca = it }, "Buscar por nome, CNPJ/CPF, e-mail, telefone…")
                    PwFiltros(filtros.map { (r, f) -> r to (if (r == "Todos") 0 else dados.empresas.count(f)) }, tipo, { tipo = it })
                }
            }
            if (dados.carregado && dados.empresas.isEmpty()) item { PwVazio("Nenhum cliente cadastrado ainda.", icone = PwIcones.Building2) }
            else if (dados.carregado && lista.isEmpty()) item { PwVazio("Nenhum cliente encontrado.", icone = PwIcones.Search) }
            items(lista, key = { it.id }) { e ->
                PwCartao(padding = PaddingValues(6.dp)) {
                    PwLinha(
                        e.nome,
                        subtitulo = listOfNotNull(
                            when (e.modalidade) { "por_km" -> "${moedaBr(e.valorKm)}/km"; "fixo" -> moedaBr(e.valorFixo); else -> "Por zona" },
                            e.documento?.let { if (e.tipo == "pf") formatarCpf(it) else formatarCnpj(it) }, if (e.podeExpress) "Express" else "só agendados",
                        ).joinToString(" · "),
                        icone = if (e.tipo == "pf") PwIcones.User else PwIcones.Building2, onClick = { onAbrir(e.id) },
                    )
                }
            }
        }
        PwBotaoFlutuante(PwIcones.Plus, "Novo cliente", { onAbrir(null) }, Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 108.dp))
    }
}

private fun formatarCpf(s: String) = s.filter { it.isDigit() }.let { d -> if (d.length == 11) "${d.substring(0, 3)}.${d.substring(3, 6)}.${d.substring(6, 9)}-${d.substring(9)}" else s }

/** Dígitos verificadores do CPF. */
private fun cpfValido(s: String): Boolean {
    val d = s.filter { it.isDigit() }.map { it - '0' }
    if (d.size != 11 || d.distinct().size == 1) return false
    fun dv(n: Int): Int { val soma = (0 until n).sumOf { d[it] * (n + 1 - it) }; val r = soma * 10 % 11; return if (r == 10) 0 else r }
    return dv(9) == d[9] && dv(10) == d[10]
}

private fun formatarCnpj(s: String) = s.filter { it.isDigit() }.let { d -> if (d.length == 14) "${d.substring(0, 2)}.${d.substring(2, 5)}.${d.substring(5, 8)}/${d.substring(8, 12)}-${d.substring(12)}" else s }

@Composable
fun EmpresaG(id: String?, onVoltar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val e = id?.let { dados.empresa(it) }
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf(e?.nome.orEmpty()) }
    var cnpj by remember { mutableStateOf(e?.cnpj?.let(::formatarCnpj).orEmpty()) }
    var razao by remember { mutableStateOf(e?.razaoSocial.orEmpty()) }
    var fantasia by remember { mutableStateOf(e?.nomeFantasia.orEmpty()) }
    var situacao by remember { mutableStateOf(e?.situacao.orEmpty()) }
    var telefone by remember { mutableStateOf(e?.telefone.orEmpty()) }
    var email by remember { mutableStateOf(e?.email.orEmpty()) }
    var endereco by remember { mutableStateOf(e?.endereco.orEmpty()) }
    var lat by remember { mutableStateOf(e?.lat) }
    var lng by remember { mutableStateOf(e?.lng) }
    var instrucoes by remember { mutableStateOf(e?.instrucoesColeta.orEmpty()) }
    var modalidade by remember { mutableStateOf(e?.modalidade ?: "por_km") }
    var valorKm by remember { mutableStateOf(e?.valorKm?.toString()?.replace('.', ',') ?: dados.config.numero("valor_km_padrao", 2.0).toString().replace('.', ',')) }
    var valorFixo by remember { mutableStateOf(e?.valorFixo?.toString()?.replace('.', ',').orEmpty()) }
    var aoVivo by remember { mutableStateOf(e?.clienteVeAoVivo ?: false) }
    var observacoes by remember { mutableStateOf(e?.observacoes.orEmpty()) }
    var tipo by remember { mutableStateOf(e?.tipo ?: "pj") }
    var cpf by remember { mutableStateOf(e?.cpf?.let(::formatarCpf).orEmpty()) }
    var podeExpress by remember { mutableStateOf(e?.podeExpress ?: false) }
    val metodos = remember { mutableStateListOf<String>().apply { addAll(e?.pagamentoMetodos ?: listOf("pix", "cartao")) } }
    var repasse by remember { mutableStateOf(e?.repassePct?.let { numeroBr(it) }.orEmpty()) }
    var devPct by remember { mutableStateOf(e?.devPct?.let { numeroBr(it) }.orEmpty()) }
    var criandoAcesso by remember { mutableStateOf(false) }
    val favoritos = remember { mutableStateListOf<String>().apply { addAll(dados.favoritos[id].orEmpty()) } }
    var msg by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var excluir by remember { mutableStateOf(false) }
    var marcarMapa by remember { mutableStateOf(false) }
    fun num(s: String) = s.trim().replace(',', '.').toDoubleOrNull()

    fun localizar() = escopo.launch {
        val r = Geo.buscarEnderecos(endereco).firstOrNull()
        if (r != null) { lat = r.lat; lng = r.lng; erro = null } else erro = "Não encontrei esse endereço no mapa. Ajuste o texto ou toque no mapa."
    }

    var aba by remember { mutableIntStateOf(0) }
    PwPagina {
        Cabecalho(if (e == null) "Novo cliente" else e.nome, onVoltar, subtitulo = "Dados, preço, pagamento e coleta padrão") {
            if (e != null) com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Share2, "Mandar no chat", { com.pwexpress.core.chat.Chat.compartilhar(com.pwexpress.core.chat.Anexo("cliente", e.id, e.nome)) })
        }
        if (e != null) {
            val ctxCli = LocalContext.current
            PwBotaoVidro("Usar o site do cliente como ${e.nome}", onClick = {
                escopo.launch {
                    Gestao.rpcJson("entrar_como_cliente", kotlinx.serialization.json.buildJsonObject { put("p_empresa", kotlinx.serialization.json.JsonPrimitive(e.id)) })
                        .onSuccess {
                            tentar { ctxCli.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(com.pwexpress.core.config.PwConfig.siteUrl + "/cliente"))) }
                            com.pwexpress.core.designsystem.PwToast.mostrar("No site do cliente você está como ${e.nome}${if (e.modoTeste) " (modo teste)" else ""}.")
                        }.onFailure { erro = it.message }
                }
            }, icone = PwIcones.Store, modifier = Modifier.fillMaxWidth())
            PwSegmentado(listOf("Dados" to PwIcones.Building2, "Regras" to PwIcones.Layers, "Zonas" to PwIcones.MapPin, "" to PwIcones.Gavel), aba, { aba = it })
            when (aba) {
                1 -> { RegrasDoAlvoG("cliente", e.id, e.nome); return@PwPagina }
                3 -> { PenalidadesG("cliente", e.id); return@PwPagina }
                2 -> {
                    CatalogoG(Catalogo.ZONAS, null, clienteId = e.id)
                    PwRotulo("Horários de pico só deste cliente")
                    CatalogoG(Catalogo.HORARIOS, null, clienteId = e.id)
                    return@PwPagina
                }
            }
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        PwInterruptor("Cliente em modo teste", e.modoTeste, { lig ->
                            escopo.launch {
                                Gestao.salvarLinha("empresas", e.id, kotlinx.serialization.json.buildJsonObject { put("modo_teste", lig) })
                                    .onSuccess { Gestao.atualizar() }.onFailure { erro = it.message }
                            }
                        }, descricao = "Pedidos dele viram pedidos de teste (só para motoboys em teste, fora das faturas reais).", cor = c.violeta)
                    }
                }
                Espaco(6)
                BotaoIcone(e.icone) { nov -> escopo.launch { Gestao.definirIcone("empresa", e.id, nov).onSuccess { Gestao.atualizar() }.onFailure { erro = it.message } } }
            }
        }
        PwSegmentado(listOf("Empresa (CNPJ)" to PwIcones.Building2, "Pessoa (CPF)" to PwIcones.User), if (tipo == "pf") 1 else 0, { tipo = if (it == 1) "pf" else "pj" })
        if (tipo == "pf") PwCartao {
            PwRotulo("Pessoa física")
            Espaco(8)
            PwCampo(cpf, { cpf = it }, "CPF", teclado = KeyboardType.Number)
            if (cpf.count { it.isDigit() } == 11 && !cpfValido(cpf)) { Espaco(4); PwErro("CPF inválido — confira os números.") }
        }
        if (tipo == "pj") PwCartao {
            PwRotulo("Receita Federal")
            Espaco(8)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(cnpj, { cnpj = it }, "CNPJ", teclado = KeyboardType.Number, modifier = Modifier.weight(1f))
                PwBotaoIcone(PwIcones.Search, "Consultar na Receita", habilitado = !ocupado, tamanho = 52.dp, ativo = cnpj.count { it.isDigit() } == 14, onClick = {
                    val d = cnpj.filter { it.isDigit() }
                    if (d.length != 14) { erro = "Digite os 14 dígitos do CNPJ."; return@PwBotaoIcone }
                    ocupado = true
                    escopo.launch {
                        Gestao.consultarCnpj(d).onSuccess { r ->
                            if (nome.isBlank()) nome = r.nomeFantasia ?: r.razaoSocial.orEmpty()
                            razao = r.razaoSocial.orEmpty(); fantasia = r.nomeFantasia.orEmpty(); situacao = r.situacao.orEmpty()
                            if (telefone.isBlank()) telefone = r.telefone.orEmpty()
                            if (email.isBlank()) email = r.email.orEmpty()
                            if (endereco.isBlank() && r.endereco != null) { endereco = r.endereco; localizar() }
                            msg = "Dados da Receita carregados."; erro = null
                        }.onFailure { erro = it.message }
                        ocupado = false
                    }
                })
            }
            if (razao.isNotBlank()) { Espaco(6); TextoDica("$razao${if (situacao.isNotBlank()) " · $situacao" else ""}") }
            if (situacao.isNotBlank() && situacao != "ATIVA") PwErro("Situação cadastral: $situacao")
        }
        PwCartao {
            PwCampo(nome, { nome = it }, "Nome (como aparece nas corridas)")
            Espaco(8); PwCampo(telefone, { telefone = it }, "Telefone", teclado = KeyboardType.Phone)
            Espaco(8); PwCampo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
        }
        PwCartao {
            PwRotulo("Preço")
            Espaco(8)
            PwSelecao("", Precos.modalidades, modalidade, { modalidade = it })
            Espaco(8)
            when (modalidade) {
                "por_km" -> PwCampo(valorKm, { valorKm = it }, "Valor do km", teclado = KeyboardType.Decimal, prefixo = "R$ ")
                "fixo" -> PwCampo(valorFixo, { valorFixo = it }, "Preço fixo", teclado = KeyboardType.Decimal, prefixo = "R$ ")
                else -> TextoDica("O valor é informado em cada corrida.")
            }
        }
        PwCartao {
            PwRotulo("Pedidos e pagamento")
            PwInterruptor("Pode pedir Express", podeExpress, { podeExpress = it }, descricao = if (podeExpress) "Pede corridas na hora, além das agendadas." else "Cliente novo: só pedidos agendados até você liberar.", icone = PwIcones.Zap)
            Espaco(4)
            TextoDica("Formas de pagamento aceitas (o pedido só sai depois de pago):")
            PwInterruptor("PIX", "pix" in metodos, { if (it) metodos.add("pix") else if (metodos.size > 1) metodos.remove("pix") })
            PwInterruptor("Cartão", "cartao" in metodos, { if (it) metodos.add("cartao") else if (metodos.size > 1) metodos.remove("cartao") })
            Espaco(6)
            TextoDica("Divisão própria deste cliente (em branco = padrão das configurações):")
            Espaco(6)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(repasse, { repasse = it }, "Motoboy %", Modifier.weight(1f), dica = numeroBr(dados.config.numero("repasse_motoboy_pct", 67.5)), teclado = KeyboardType.Decimal)
            }
        }
        PwCartao {
            PwRotulo("Coleta padrão")
            Espaco(8)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PwCampo(endereco, { endereco = it; lat = null; lng = null }, "Endereço de coleta", modifier = Modifier.weight(1f))
                PwBotaoIcone(PwIcones.Search, "Localizar endereço", { localizar() }, tamanho = 52.dp)
            }
            Espaco(8)
            val formaMapa = RoundedCornerShape(18.dp)
            Box(Modifier.fillMaxWidth().height(180.dp).clip(formaMapa).border(1.dp, c.borda, formaMapa)) {
                PwMapa(
                    paradas = listOfNotNull(lat?.let { la -> lng?.let { lo -> PinoMapa(la, lo, true, true, false) } }), rota = null, minhaPosicao = null,
                    modifier = Modifier.fillMaxWidth().height(180.dp), enquadrar = "$lat,$lng",
                )
                // a prévia não se mexe: o toque abre o mapa em tela cheia, que é fácil de arrastar
                Box(Modifier.fillMaxSize().clickable { marcarMapa = true })
                PwPilula(if (lat != null) "Ajustar no mapa" else "Marcar no mapa", PwIcones.MapPinned, { marcarMapa = true }, Modifier.align(Alignment.BottomEnd).padding(10.dp))
            }
            Detalhe(if (lat != null) PwIcones.CircleCheck else PwIcones.MapPin, if (lat != null) "Coleta marcada no mapa" else "Sem local: busque o endereço ou marque no mapa", if (lat != null) c.verde else null)
            Espaco(8)
            PwCampo(instrucoes, { instrucoes = it }, "Instruções de coleta", linhas = 2)
        }
        PwCartao {
            PwRotulo("Motoboys favoritos")
            TextoDica("Recebem as corridas deste cliente antes dos outros, na ordem marcada.")
            dados.equipe.filter { it.motoboy }.forEach { m ->
                PwInterruptor(m.nome + (if (m.id in favoritos) "  · ${favoritos.indexOf(m.id) + 1}º" else ""), m.id in favoritos, { lig -> if (lig) favoritos.add(m.id) else favoritos.remove(m.id) })
            }
            Espaco(6)
            PwInterruptor("Cliente vê a corrida ao vivo", aoVivo, { aoVivo = it }, descricao = "No painel do cliente, mostra o motoboy no mapa durante a corrida.")
            PwCampo(observacoes, { observacoes = it }, "Observações internas", linhas = 2)
        }
        PwErro(erro)
        msg?.let { Text(it, color = c.verde, fontWeight = FontWeight.SemiBold) }
        PwBotaoOuro(if (ocupado) "Salvando…" else "Salvar", habilitado = !ocupado, icone = PwIcones.Check, onClick = {
            if (nome.isBlank()) { erro = "Informe o nome."; return@PwBotaoOuro }
            val d = if (tipo == "pj") cnpj.filter { it.isDigit() } else ""
            val dCpf = if (tipo == "pf") cpf.filter { it.isDigit() } else ""
            if (d.isNotEmpty() && d.length != 14) { erro = "CNPJ deve ter 14 dígitos."; return@PwBotaoOuro }
            if (dCpf.isNotEmpty() && !cpfValido(dCpf)) { erro = "CPF inválido."; return@PwBotaoOuro }
            val pr = num(repasse); val pd = num(devPct)
            if ((pr ?: 0.0) + (pd ?: 0.0) > 100 || (pr ?: 0.0) < 0 || (pd ?: 0.0) < 0) { erro = "A parte do motoboy fica entre 0 e 100%."; return@PwBotaoOuro }
            val linha = buildJsonObject {
                put("tipo", tipo); put("cpf", dCpf.ifEmpty { null }); put("pode_express", podeExpress)
                put("pagamento_metodos", kotlinx.serialization.json.buildJsonArray { metodos.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                put("repasse_pct", pr); put("dev_pct", pd)
                put("nome", nome.trim()); put("cnpj", d.ifEmpty { null }); put("razao_social", razao.ifBlank { null }); put("nome_fantasia", fantasia.ifBlank { null })
                put("situacao", situacao.ifBlank { null }); put("telefone", telefone.ifBlank { null }); put("email", email.ifBlank { null })
                put("endereco", endereco.ifBlank { null }); put("lat", lat); put("lng", lng); put("instrucoes_coleta", instrucoes.ifBlank { null })
                put("modalidade_preco", modalidade); put("valor_km", num(valorKm) ?: 2.0); put("valor_fixo", num(valorFixo))
                put("cliente_ve_ao_vivo", aoVivo); put("observacoes", observacoes.ifBlank { null })
            }
            ocupado = true
            escopo.launch {
                Gestao.salvarEmpresa(id, linha, favoritos.toList()).onSuccess { Gestao.atualizar(); msg = "Salvo."; erro = null; if (id == null) onVoltar() }.onFailure { erro = it.message }
                ocupado = false
            }
        })
        if (e != null) {
            PwCartao {
                PwRotulo("Acesso do cliente")
                val acessos = dados.acessosCliente[e.id].orEmpty()
                TextoDica(if (acessos.isEmpty()) "Crie um login para o cliente pedir, pagar e acompanhar pelo site." else "Logins que pedem pelo site em nome deste cliente:")
                acessos.forEach { a -> Detalhe(PwIcones.User, "${a.nome} · ${a.email}") }
                Espaco(8)
                PwBotaoVidro("Criar acesso do cliente", onClick = { criandoAcesso = true }, icone = PwIcones.UserPlus, modifier = Modifier.fillMaxWidth())
            }
            PwBotaoPerigo("Excluir cliente", onClick = { excluir = true }, modifier = Modifier.fillMaxWidth())
        }
    }
    if (criandoAcesso && e != null) NovaContaG(0.0, onFechar = { criandoAcesso = false }, onCriada = { criandoAcesso = false }, cliente = e)
    if (marcarMapa) {
        com.pwexpress.core.mapa.PwSeletorMapa(lat?.let { la -> lng?.let { lo -> la to lo } }, onFechar = { marcarMapa = false }, titulo = "Coleta padrão") { la, lo ->
            marcarMapa = false
            lat = la; lng = lo
            escopo.launch { if (endereco.isBlank()) endereco = Geo.enderecoDoPonto(la, lo) }
        }
    }
    if (excluir && e != null) {
        PwJanela("Excluir ${e.nome}?", onFechar = { excluir = false }) {
            Text("As corridas antigas ficam, mas sem vínculo com o cliente.", color = c.texto2)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Voltar", onClick = { excluir = false }, modifier = Modifier.weight(1f))
                PwBotaoPerigo("Excluir", cheio = true, modifier = Modifier.weight(1f), onClick = {
                    excluir = false
                    escopo.launch { Gestao.excluirEmpresa(e.id).onSuccess { Gestao.atualizar(); onVoltar() }.onFailure { erro = it.message } }
                })
            }
        }
    }
}

// =====================================================================
// Equipe
// =====================================================================

@Composable
fun EquipeG(onAbrir: (String) -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var criando by remember { mutableStateOf(false) }
    var busca by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var tipo by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    fun online(m: com.pwexpress.feature.gestor.data.MembroG) = m.motoboy && (dados.posicao(m.id)?.status ?: "offline") != "offline"
    val filtros: List<Pair<String, (com.pwexpress.feature.gestor.data.MembroG) -> Boolean>> = listOf(
        "Todos" to { _ -> true }, "Motoboys" to { m -> m.motoboy }, "Online" to { m -> online(m) }, "Offline" to { m -> m.motoboy && !online(m) },
        "Gestores" to { m -> m.gestor || m.dev }, "Teste" to { m -> m.modoTeste }, "Bloqueados" to { m -> m.bloqueado },
    )
    // em corrida e disponíveis primeiro, offline por último (a ordem de antes vale dentro de cada grupo)
    val lista = dados.equipe.filter { m -> filtros[tipo].second(m) && combinaBusca(busca, m.nome, m.email) }
        .sortedBy { m -> when (dados.posicao(m.id)?.status) { "em_corrida" -> 0; "disponivel" -> 1; else -> 2 } }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 190.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TituloTela("Equipe", if (dados.equipe.size == 1) "1 pessoa" else "${dados.equipe.size} pessoas", PwIcones.Users) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CampoBusca(busca, { busca = it }, "Buscar por nome ou e-mail…")
                    PwFiltros(filtros.map { (r, f) -> r to (if (r == "Todos") 0 else dados.equipe.count(f)) }, tipo, { tipo = it })
                }
            }
            if (dados.carregado && lista.isEmpty()) item { PwVazio("Ninguém encontrado.", icone = PwIcones.Search) }
            items(lista, key = { it.id }) { m ->
                val p = dados.posicao(m.id)
                val forma = RoundedCornerShape(20.dp)
                Row(
                    Modifier.fillMaxWidth().clip(forma).background(c.vidro).border(1.dp, c.borda, forma).clickable { onAbrir(m.id) }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PwAvatar(m.nome, m.foto, tamanho = 46.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(m.nome, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 15.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                            if (m.motoboy) { Spacer(Modifier.width(6.dp)); PwPonto(corMotoboy(p?.status, c), 9.dp) }
                        }
                        Text(m.email, color = c.texto3, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Selos(m, p?.status)
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (modulo("criar_contas")) PwBotaoFlutuante(PwIcones.UserPlus, "Nova conta", { criando = true }, Modifier.align(Alignment.BottomEnd).padding(end = 18.dp, bottom = 108.dp))
    }
    if (criando) NovaContaG(dados.config.numero("repasse_motoboy_pct", 67.5), onFechar = { criando = false }, onCriada = { criando = false; it?.let(onAbrir) })
}

/** Papéis e situação da pessoa, em fichas que quebram de linha sem cortar o texto. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun Selos(m: com.pwexpress.feature.gestor.data.MembroG, status: String?) {
    val c = Pw.cores
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (m.dev) PwSelo("Dev", c.dev, icone = PwIcones.CodeXml)
        if (m.gestor && !m.dev) PwSelo("Gestor", c.ouro, icone = PwIcones.ShieldCheck)
        if (m.supervisor && !m.gestor && !m.dev) PwSelo("Supervisor", c.azul, icone = PwIcones.UserCog)
        if (m.motoboy) PwSelo(rotuloMotoboy[status] ?: "Offline", corMotoboy(status, c), icone = PwIcones.Bike)
        if (m.modoTeste) PwSelo("Teste", c.violeta, icone = PwIcones.FlaskConical)
        if (m.bloqueado) PwSelo("Bloqueado", c.vermelho, icone = PwIcones.Lock)
    }
}

private fun gerarSenha(): String {
    val l = "abcdefghjkmnpqrstuvwxyz"; val n = "23456789"; val r = java.security.SecureRandom()
    fun p() = (1..4).map { l[r.nextInt(l.length)] }.joinToString("")
    return "${p()}-${p()}-${n[r.nextInt(n.length)]}${n[r.nextInt(n.length)]}"
}

private fun copiar(ctx: Context, texto: String) {
    ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("Acesso", texto))
}

@Composable
private fun NovaContaG(repassePadrao: Double, onFechar: () -> Unit, onCriada: (String?) -> Unit, cliente: com.pwexpress.feature.gestor.data.EmpresaG? = null) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    var nome by remember { mutableStateOf(cliente?.nome.orEmpty()) }
    var email by remember { mutableStateOf(cliente?.email.orEmpty()) }
    var senha by remember { mutableStateOf(gerarSenha()) }
    var motoboy by remember { mutableStateOf(true) }
    var gestor by remember { mutableStateOf(false) }
    var supervisor by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var criada by remember { mutableStateOf<String?>(null) }
    val acesso = "${com.pwexpress.core.marca.MarcaEmpresa.marca.value?.nomePara("documentos") ?: "Delibra"}\nE-mail: ${email.trim().lowercase()}\nSenha: $senha\nApp: ${com.pwexpress.core.config.PwConfig.siteUrl}/app"
    if (criada != null) {
        PwJanela("Conta criada", onFechar = { onCriada(criada) }) {
            Text("Passe estes dados para ${nome.trim().substringBefore(" ")}. A senha não aparece de novo.", color = c.texto2)
            Text(acesso, fontFamily = FontFamily.Monospace, color = c.texto, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.campo).border(1.dp, c.bordaForte, RoundedCornerShape(14.dp)).padding(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PwBotaoVidro("Copiar acesso", onClick = { copiar(ctx, acesso) }, icone = PwIcones.Copy, modifier = Modifier.weight(1f))
                PwBotaoOuro("Pronto", onClick = { onCriada(criada) }, modifier = Modifier.weight(1f))
            }
        }
        return
    }
    PwJanela(if (cliente != null) "Acesso de ${cliente.nome}" else "Nova conta", onFechar = onFechar, fecharFora = !enviando) {
        TextoDica(if (cliente != null) "O cliente entra no site para pedir, pagar e acompanhar." else "A conta já nasce confirmada e pode entrar no app ou no painel na hora.")
        PwCampo(nome, { nome = it }, "Nome")
        PwCampo(email, { email = it }, "E-mail", teclado = KeyboardType.Email)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PwCampo(senha, { senha = it }, "Senha inicial", modifier = Modifier.weight(1f))
            PwBotaoIcone(PwIcones.RefreshCw, "Gerar outra", { senha = gerarSenha() })
        }
        if (cliente == null) PwInterruptor("Motoboy", motoboy, { motoboy = it }, descricao = "Usa o app e recebe corridas", icone = PwIcones.Bike)
        if (cliente == null) PwInterruptor("Gestor", gestor, { gestor = it; if (it) supervisor = false }, descricao = "Acessa o painel", icone = PwIcones.ShieldCheck)
        // líder de equipe: faz só o que o gestor liberar (Equipe → pessoa → Permissões)
        if (cliente == null) PwInterruptor("Supervisor", supervisor, { supervisor = it; if (it) gestor = false }, descricao = "Líder de equipe, com o que o gestor liberar", icone = PwIcones.UserCog, cor = Pw.cores.azul)
        PwErro(erro)
        PwBotaoOuro(if (enviando) "Criando…" else "Criar conta", icone = PwIcones.UserPlus, habilitado = !enviando, onClick = {
            enviando = true
            escopo.launch {
                Gestao.criarConta(
                    nome.trim(), email.trim(), senha,
                    if (cliente != null) listOf("cliente") else listOfNotNull("motoboy".takeIf { motoboy }, "gestor".takeIf { gestor }, "supervisor".takeIf { supervisor }),
                    null, cliente?.id,
                )
                    .onSuccess { criada = it; Gestao.atualizar() }.onFailure { erro = it.message }
                enviando = false
            }
        })
    }
}

@Composable
fun MembroG(id: String, meuId: String?, onVoltar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val m = dados.membro(id)
    var aba by remember { mutableIntStateOf(0) }
    PwPagina {
        Cabecalho(m?.nome ?: "Conta", onVoltar, subtitulo = m?.email) {
            if (m != null) com.pwexpress.core.designsystem.PwBotaoIcone(PwIcones.Share2, "Mandar no chat", { com.pwexpress.core.chat.Chat.compartilhar(com.pwexpress.core.chat.Anexo("pessoa", m.id, m.nome)) })
        }
        if (m == null) { if (!dados.carregado) PwCarregando() else PwErro("Essa conta não existe mais."); return@PwPagina }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PwAvatar(m.nome, m.foto, tamanho = 60.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Selos(m, dados.posicao(m.id)?.status)
                m.ultimoAcesso?.let { TextoDica("Último acesso: ${dataHora(it)}") }
            }
        }
        val comPercursos = m.motoboy && modulo("trilhas_mapa")
        val abas = listOfNotNull(
            Triple("registro", "Registro", PwIcones.History),
            Triple("percursos", "Percursos", PwIcones.Route).takeIf { comPercursos },
            Triple("veiculo", "Veículo", PwIcones.Bike).takeIf { m.motoboy },
            Triple("medalhas", "Medalhas", PwIcones.Trophy).takeIf { m.motoboy },
            Triple("regras", "Regras", PwIcones.Layers).takeIf { m.motoboy },
            Triple("penalidades", "Penalidades", PwIcones.Gavel).takeIf { m.motoboy },
            Triple("editar", "Editar", PwIcones.Pencil),
        )
        // muitas abas: só a escolhida mostra o nome (as outras ficam no ícone, sem texto cortado)
        val abaAtual = aba.coerceIn(0, abas.lastIndex)
        PwSegmentado(abas.mapIndexed { i, t -> (if (i == abaAtual) t.second else "") to t.third }, abaAtual, { aba = it })
        val souDev = com.pwexpress.core.perfil.Perfis.perfil.collectAsState().value?.dev == true
        when (abas.getOrNull(aba)?.first) {
            "registro" -> RegistroG(m.id)
            "percursos" -> PercursosMembroG(m.id, m.modoTeste)
            "veiculo" -> VeiculoG(m.id)
            "medalhas" -> {
                var stats by remember(m.id) { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
                var erroC by remember(m.id) { mutableStateOf<String?>(null) }
                var versaoC by remember(m.id) { androidx.compose.runtime.mutableIntStateOf(0) }
                androidx.compose.runtime.LaunchedEffect(m.id, versaoC) { com.pwexpress.core.perfil.Conquistas.carregar(m.id).onSuccess { stats = it }.onFailure { erroC = it.message } }
                PwErro(erroC)
                PwCartao { com.pwexpress.core.perfil.GradeConquistas(stats, perfilDev = m.id.takeIf { souDev }, onMudou = { versaoC++ }) }
            }
            "regras" -> {
                val escopoI = rememberCoroutineScope()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextoDica("Ícone no mapa (quando não há foto):")
                    Spacer(Modifier.width(8.dp))
                    BotaoIcone(dados.membroIcone(m.id)) { nov -> escopoI.launch { Gestao.definirIcone("perfil", m.id, nov).onSuccess { Gestao.atualizar() } } }
                }
                RegrasDoAlvoG("motoboy", m.id, m.nome)
                CompartilharMotoboyG(m.id, m.nome)
            }
            "penalidades" -> PenalidadesG("motoboy", m.id)
            "editar" -> if (m.dev && !souDev) TextoDica("Conta Dev: só o Dev altera esta conta.") else EditarMembroG(m, eu = m.id == meuId, onExcluida = onVoltar, souDev = souDev)
        }
    }
}

/** Percursos do motoboy por dia, separados em registros (procurando / corrida), traçados pelas ruas. */
@Composable
private fun PercursosMembroG(id: String, teste: Boolean) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var dia by remember { mutableIntStateOf(0) }
    var doServidor by remember(id) { mutableStateOf<List<com.pwexpress.core.mapa.RegistroPercurso>?>(null) }
    var carregando by remember { mutableStateOf(true) }
    LaunchedEffect(id, dia, teste) {
        carregando = true; doServidor = null
        val zona = java.time.ZoneId.systemDefault()
        val ini = java.time.LocalDate.now(zona).minusDays(dia.toLong()).atStartOfDay(zona).toInstant()
        var tentativas = 0
        while (true) {
            val r = com.pwexpress.core.mapa.PercursosServidor.buscar(ini, ini.plusSeconds(86_400), id, teste) ?: break
            doServidor = r.registros(id); carregando = false
            if (!r.parcial || tentativas++ >= 3) break
            kotlinx.coroutines.delay(4000)
        }
        carregando = false
    }
    // site fora do ar: as últimas 12 h que o app já tem (linha reta entre os pings)
    val regs = doServidor ?: if (dia == 0 && !carregando) registrosDe(dados, id) else emptyList()
    var destaque by remember(dia) { mutableStateOf<Int?>(null) }
    var chave by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(id) { chave = Gestao.chavesPix()[id] }
    val forma = RoundedCornerShape(20.dp)
    Box(Modifier.fillMaxWidth().height(300.dp).clip(forma).border(1.dp, c.borda, forma)) {
        PwMapa(
            paradas = emptyList(), rota = null, minhaPosicao = null, modifier = Modifier.fillMaxWidth().height(300.dp),
            percursos = regs.mapIndexed { i, r -> com.pwexpress.core.mapa.PercursoMapa.de(r, destaque = destaque == i, apagado = destaque != null && destaque != i) },
            percursoTeste = teste, enquadrar = "$id-$dia-${regs.size}-$destaque",
            tituloTelaCheia = "Percursos de ${dados.membro(id)?.nome?.substringBefore(" ").orEmpty()}",
            // tela cheia: troca o dia sem sair do mapa
            rodapeTelaCheia = { com.pwexpress.core.mapa.SeletorDia(dia, { dia = it }) },
            marcadores = dados.posicao(id)?.takeIf { coordenadaValida(it.lat, it.lng) }?.let { p -> listOf(MarcadorMapa(id, p.lat!!, p.lng!!, String.format("#%06X", 0xFFFFFF and corMotoboy(p.status, c).toArgb()), dados.membro(id)?.nome?.substringBefore(" ").orEmpty(), foto = dados.membro(id)?.foto)) }.orEmpty(),
        )
    }
    com.pwexpress.core.mapa.SeletorDia(dia, { dia = it })
    LegendaPercurso(teste)
    PwCartao {
        PwRotulo("Registros do dia")
        Espaco(6)
        if (carregando && regs.isEmpty()) PwCarregando(texto = "Traçando os percursos pelas ruas…")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { ListaRegistros(regs, destaque, { destaque = if (destaque == it) null else it }, dados, vazio = if (carregando) "" else "Nenhum percurso neste dia.") }
        Espaco(8)
        TextoDica("Total: ${String.format(java.util.Locale("pt", "BR"), "%.1f km", regs.sumOf { it.metros } / 1000)} · ${regs.count { it.corrida }} em corrida · ${regs.count { !it.corrida }} procurando")
    }
    PwCartao { PwRotulo("Chave PIX"); Espaco(4); TextoDica(chave ?: "Ainda não cadastrou a chave PIX no MotoApp.") }
}

@Composable
private fun RegistroG(id: String) {
    val c = Pw.cores
    val teste by TemaApp.modoTeste.collectAsState()
    var dados by remember { mutableStateOf<Pair<JsonObject, List<ItemRegistro>>?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var filtro by remember { mutableIntStateOf(0) }
    LaunchedEffect(id) { tentar { Gestao.registro(id) }.onSuccess { dados = it }.onFailure { erro = it.message } }
    PwErro(erro)
    val d = dados ?: run { if (erro == null) PwCarregando(); return }
    val r = d.first
    fun n(k: String) = (r.int(k) ?: 0).toString()
    PwNumero(if (teste) "Ganhos de teste" else "Ganhos", moedaBr(r.dbl(if (teste) "ganhos_teste" else "ganhos") ?: 0.0), Modifier.fillMaxWidth(), icone = PwIcones.Wallet, destaque = true)
    val numeros = listOf(
        Triple("Km rodados", "%.1f".format(BR, r.dbl("km_rodados") ?: 0.0), PwIcones.Ruler),
        Triple("Concluídas", n(if (teste) "concluidas_teste" else "concluidas"), PwIcones.CircleCheck),
        Triple("Criadas", n(if (teste) "criadas_teste" else "criadas"), PwIcones.Plus),
        Triple("Aceitas", n("aceitas"), PwIcones.Check),
        Triple("Recusadas", n("recusadas"), PwIcones.Ban),
        Triple("Expiradas", n("expiradas"), PwIcones.Timer),
        Triple("Canceladas", ((r.int("cancelou") ?: 0) + (r.int("canceladas_com_ele") ?: 0)).toString(), PwIcones.CircleX),
        Triple("Apagadas", n("excluiu"), PwIcones.Trash2),
    )
    numeros.chunked(2).forEach { par ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            par.forEach { (rot, v, ic) -> PwNumero(rot, v, Modifier.weight(1f), icone = ic) }
        }
    }
    val grupos = mapOf(
        "criada" to 1, "reagendada" to 1, "concluida" to 2, "parada_concluida" to 2, "chegou" to 2, "cancelada" to 3,
        "oferta_enviada" to 4, "oferta_recusada" to 4, "oferta_expirada" to 4, "aceita" to 4, "redespachada" to 4, "reservada" to 4, "reserva_cancelada" to 4, "planejada_atrasada" to 4,
        "excluida" to 5,
    )
    PwFiltros(listOf("Tudo" to 0, "Criadas" to 0, "Concluídas" to 0, "Canceladas" to 0, "Recebidas" to 0, "Apagadas" to 0), filtro, { filtro = it })
    val itens = d.second.filter { (teste || !it.teste) && (filtro == 0 || grupos[it.tipo] == filtro) }
    if (itens.isEmpty()) PwVazio("Nada por aqui ainda.", icone = PwIcones.History)
    var diaAnterior = ""
    itens.forEach { i ->
        val dia = dataDia(i.quando)
        if (dia != diaAnterior) { diaAnterior = dia; Espaco(4); PwRotulo(dia) }
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.padding(vertical = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(descricaoRegistro(i), color = c.texto, fontSize = 14.sp)
                extraRegistro(i)?.let { TextoDica(it) }
            }
            Spacer(Modifier.width(8.dp))
            Text(hora(i.quando), color = c.texto3, fontSize = 12.sp)
        }
    }
}

private fun dataDia(ms: Long): String {
    val d = java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val hoje = java.time.LocalDate.now()
    return when (d) { hoje -> "Hoje"; hoje.minusDays(1) -> "Ontem"; else -> d.format(java.time.format.DateTimeFormatter.ofPattern("EEE, dd 'de' MMM", BR)) }
}

private fun descricaoRegistro(i: ItemRegistro): String {
    val n = i.numero?.let { if (i.teste) "T$it" else "#$it" }.orEmpty()
    val pelo = (i.dados["pelo_gestor"] as? JsonPrimitive)?.content == "true"
    return when (i.tipo) {
        "criada" -> "Criou a corrida $n"
        "cancelada" -> if (i.fez) "Cancelou a corrida $n" else "A corrida $n foi cancelada pelo gestor"
        "redespachada" -> if (i.fez) "Relançou a corrida $n" else "Recebeu a corrida $n direto do gestor"
        "oferta_enviada" -> "Recebeu a oferta da corrida $n"
        "oferta_recusada" -> "Recusou a corrida $n"
        "oferta_expirada" -> "Deixou a oferta da corrida $n expirar"
        "aceita" -> "Aceitou a corrida $n"
        "chegou" -> "Chegou no local (corrida $n)"
        "parada_concluida" -> "Concluiu uma parada (corrida $n)"
        "concluida" -> if (i.fez) "Concluiu a corrida $n" else "Corrida $n concluída"
        "excluida" -> "Apagou a corrida $n"
        "reservada" -> if (i.fez) (if (pelo) "Reservou a corrida planejada $n para um motoboy" else "Reservou a corrida planejada $n") else "Recebeu a reserva da corrida $n (pelo gestor)"
        "reserva_cancelada" -> if (i.fez && !pelo) "Desistiu da corrida planejada $n" else if (i.fez) "Liberou a reserva da corrida $n" else "A reserva da corrida $n foi liberada pelo gestor"
        "reagendada" -> "Mudou o horário da corrida $n"
        "planejada_atrasada" -> "Não iniciou a corrida planejada $n no horário"
        else -> "${i.tipo.replace('_', ' ')} $n"
    }
}

private fun extraRegistro(i: ItemRegistro): String? {
    val motivo = (i.dados["motivo"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() && it != "null" }
    val dist = (i.dados["distancia_m"] as? JsonPrimitive)?.content?.toDoubleOrNull()
    return listOfNotNull(
        i.cliente, motivo?.let { "Motivo: $it" }, dist?.let { "${km(it.toInt())} até a coleta" },
        i.ganho?.takeIf { i.tipo == "concluida" }?.let { "Ganhou ${moedaBr(it)}" },
        i.valor?.takeIf { i.tipo == "criada" }?.let { moedaBr(it) },
        "teste".takeIf { i.teste },
    ).joinToString(" · ").ifEmpty { null }
}

@Composable
private fun EditarMembroG(m: com.pwexpress.feature.gestor.data.MembroG, eu: Boolean, onExcluida: () -> Unit, souDev: Boolean = false) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val escopo = rememberCoroutineScope()
    val dados by Gestao.dados.collectAsState()
    var nome by remember(m.id) { mutableStateOf(m.nome) }
    var gestor by remember(m.id) { mutableStateOf(m.gestor) }
    var supervisor by remember(m.id) { mutableStateOf(m.supervisor) }
    var motoboy by remember(m.id) { mutableStateOf(m.motoboy) }
    var devPapel by remember(m.id) { mutableStateOf(m.dev) }
    var msg by remember { mutableStateOf<String?>(null) }
    var erro by remember { mutableStateOf<String?>(null) }
    var senhaNova by remember { mutableStateOf<String?>(null) }
    var senhaFeita by remember { mutableStateOf(false) }
    var excluindo by remember { mutableStateOf(false) }

    PwCartao {
        PwCampo(nome, { nome = it }, "Nome de exibição")
        Espaco(8)
        PwRotulo("Papéis")
        if (souDev) PwInterruptor("Dev", devPapel, { devPapel = it }, descricao = "Administra o sistema inteiro", icone = PwIcones.CodeXml, cor = c.dev)
        PwInterruptor("Gestor", gestor, { gestor = it; if (it) supervisor = false }, descricao = "Acessa o painel (site e app)", icone = PwIcones.ShieldCheck)
        PwInterruptor("Supervisor", supervisor, { supervisor = it; if (it) gestor = false }, descricao = "Líder de equipe, com o que o gestor liberar", icone = PwIcones.UserCog, cor = Pw.cores.azul)
        PwInterruptor("Motoboy", motoboy, { motoboy = it }, descricao = "Usa o app e recebe corridas", icone = PwIcones.Bike)
        if (gestor || devPapel) VincularClienteG(m.id, m.nome)
        PwErro(erro)
        msg?.let { Text(it, color = c.verde, fontWeight = FontWeight.SemiBold) }
        Espaco(6)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwBotaoVidro("Nova senha", onClick = { senhaNova = gerarSenha(); senhaFeita = false }, icone = PwIcones.KeyRound, modifier = Modifier.weight(1f))
            PwBotaoOuro("Salvar", icone = PwIcones.Check, modifier = Modifier.weight(1f), onClick = {
                escopo.launch {
                    Gestao.editarMembro(m.id, nome, listOfNotNull("dev".takeIf { devPapel }, "gestor".takeIf { gestor }, "supervisor".takeIf { supervisor }, "motoboy".takeIf { motoboy }), null)
                        .onSuccess { msg = null; erro = null; com.pwexpress.core.designsystem.PwToast.mostrar("${nome.ifBlank { m.email }} atualizado.", titulo = "Salvo"); Gestao.atualizar() }.onFailure { erro = it.message; msg = null }
                }
            })
        }
    }

    if (eu) { TextoDica("Esta é a sua conta: nome e foto você muda no seu perfil."); return }
    PwCartao(cor = c.vermelho.copy(alpha = 0.45f)) {
        PwRotulo("Zona de perigo")
        Espaco(6)
        PwLinha(if (m.bloqueado) "Acesso bloqueado" else "Bloquear acesso", subtitulo = if (m.bloqueado) "Não entra no app nem no painel. O histórico continua." else "Impede de entrar, sem apagar nada. Dá para liberar depois.",
            icone = if (m.bloqueado) PwIcones.LockOpen else PwIcones.Lock, corIcone = c.vermelho)
        PwBotaoVidro(if (m.bloqueado) "Liberar acesso" else "Bloquear", icone = if (m.bloqueado) PwIcones.LockOpen else PwIcones.Lock, modifier = Modifier.fillMaxWidth(), onClick = {
            escopo.launch { Gestao.bloquear(m.id, !m.bloqueado).onSuccess { Gestao.atualizar(); erro = null }.onFailure { erro = it.message } }
        })
        Espaco(10)
        PwLinha("Excluir conta", subtitulo = "Apaga de vez. As corridas continuam no histórico com o nome da pessoa.", icone = PwIcones.Trash2, corIcone = c.vermelho)
        PwBotaoPerigo("Excluir", onClick = { excluindo = true }, modifier = Modifier.fillMaxWidth())
    }

    senhaNova?.let { s ->
        val acesso = "${com.pwexpress.core.marca.MarcaEmpresa.marca.value?.nomePara("documentos") ?: "Delibra"}\nE-mail: ${m.email}\nSenha: $s"
        PwJanela(if (senhaFeita) "Senha trocada" else "Nova senha", onFechar = { senhaNova = null }) {
            if (!senhaFeita) {
                TextoDica("Para quando ${m.nome} esquecer a senha. Quem estiver conectado continua até sair.")
                Text(s, fontFamily = FontFamily.Monospace, color = c.texto, fontSize = 18.sp)
                PwBotaoOuro("Trocar senha", icone = PwIcones.KeyRound, onClick = {
                    escopo.launch { Gestao.redefinirSenha(m.id, s).onSuccess { senhaFeita = true }.onFailure { erro = it.message; senhaNova = null } }
                })
            } else {
                Text(acesso, fontFamily = FontFamily.Monospace, color = c.texto)
                PwBotaoVidro("Copiar acesso", onClick = { copiar(ctx, acesso) }, icone = PwIcones.Copy, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    if (excluindo) {
        var senha by remember { mutableStateOf("") }
        var motivo by remember { mutableStateOf("") }
        var entendi by remember { mutableStateOf(false) }
        var errExc by remember { mutableStateOf<String?>(null) }
        var enviando by remember { mutableStateOf(false) }
        PwJanela("Excluir ${m.nome}?", onFechar = { excluindo = false }, fecharFora = !enviando) {
            PwErro("${m.email} deixa de existir: não entra mais no app nem no painel. Não dá para desfazer. As corridas continuam no histórico com o nome guardado.")
            PwCampo(motivo, { motivo = it }, "Motivo (opcional)")
            PwInterruptor("Entendo que é definitivo", entendi, { entendi = it }, cor = c.vermelho)
            PwCampo(senha, { senha = it }, "Sua senha", senha = true, teclado = KeyboardType.Password)
            PwErro(errExc)
            PwBotaoPerigo(if (enviando) "Excluindo…" else "Excluir de vez", cheio = true, habilitado = entendi && senha.isNotEmpty() && !enviando, modifier = Modifier.fillMaxWidth(), onClick = {
                enviando = true
                escopo.launch {
                    Gestao.excluirConta(m.id, senha, motivo.trim().ifEmpty { null }).onSuccess { excluindo = false; Gestao.atualizar(); onExcluida() }.onFailure { errExc = it.message; senha = "" }
                    enviando = false
                }
            })
        }
    }
}

// =====================================================================
// Carteira
// =====================================================================

@Composable
fun FinanceiroG(onVoltar: (() -> Unit)?) {
    var aba by remember { mutableIntStateOf(0) }
    val abas: @Composable () -> Unit = {
        PwFiltros(listOf("Resumo" to 0, "Faturas" to 0, "Folha" to 0, "Clientes" to 0, "Extrato" to 0, "PIX" to 0), aba, { aba = it })
    }
    when (aba) {
        0 -> ResumoCarteiraG(onVoltar, abas)
        1 -> FaturasG(onVoltar, abas)
        2 -> FolhaG(onVoltar, abas)
        3 -> ClientesCarteiraG(onVoltar, abas)
        4 -> ExtratoCarteiraG(onVoltar, abas)
        else -> PixCarteiraG(onVoltar, abas)
    }
}

/** Período da carteira (Hoje · 7 dias · Este mês · 30 dias) e as corridas concluídas nele. */
@Composable
private fun corridasDoPeriodo(periodo: Int, teste: Boolean): List<CorridaG>? {
    var linhas by remember { mutableStateOf<List<CorridaG>?>(null) }
    LaunchedEffect(periodo, teste) {
        linhas = null
        val cal = java.util.Calendar.getInstance()
        when (periodo) {
            0 -> { cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0) }
            1 -> cal.add(java.util.Calendar.DAY_OF_MONTH, -7)
            2 -> { cal.set(java.util.Calendar.DAY_OF_MONTH, 1); cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0) }
            else -> cal.add(java.util.Calendar.DAY_OF_MONTH, -30)
        }
        linhas = Gestao.concluidas(cal.timeInMillis, teste)
    }
    return linhas
}

private fun clienteDaCorrida(cor: CorridaG, dados: Gestao.Dados) = dados.empresa(cor.empresaId)?.nome ?: cor.clienteNome?.let { "$it (avulso)" } ?: "Avulso"

/** Carteira · Clientes: quanto cada cliente rendeu no período. */
@Composable
private fun ClientesCarteiraG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var periodo by remember { mutableIntStateOf(2) }
    val l = corridasDoPeriodo(periodo, teste)
    PwPagina {
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = "Por cliente") else TituloTela("Carteira", "Por cliente", PwIcones.Wallet)
        abas()
        PwFiltros(listOf("Hoje" to 0, "7 dias" to 0, "Este mês" to 0, "30 dias" to 0), periodo, { periodo = it })
        val x = l ?: run { PwCarregando(); return@PwPagina }
        if (x.isEmpty()) { PwVazio("Nenhuma rota concluída neste período.", icone = PwIcones.Building2); return@PwPagina }
        x.groupBy { clienteDaCorrida(it, dados) }.entries.sortedByDescending { e -> e.value.sumOf { it.valor ?: 0.0 } }.forEach { (nome, cs) ->
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(nome, color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica("${cs.size} rota${if (cs.size == 1) "" else "s"} · gestão ${moedaBr(cs.sumOf { (it.valor ?: 0.0) - (it.valorMotoboy ?: 0.0) })}")
                    }
                    Text(moedaBr(cs.sumOf { it.valor ?: 0.0 }), color = c.texto, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Carteira · Extrato: cada rota com a divisão (motoboy e gestão), com busca e planilha. */
@Composable
private fun ExtratoCarteiraG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var periodo by remember { mutableIntStateOf(2) }
    var busca by remember { mutableStateOf("") }
    val l = corridasDoPeriodo(periodo, teste)
    PwPagina {
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = "Extrato") else TituloTela("Carteira", "Extrato", PwIcones.Wallet)
        abas()
        PwFiltros(listOf("Hoje" to 0, "7 dias" to 0, "Este mês" to 0, "30 dias" to 0), periodo, { periodo = it })
        CampoBusca(busca, { busca = it }, "Buscar por número, motoboy ou cliente")
        val x = (l ?: run { PwCarregando(); return@PwPagina }).filter { cor ->
            combinaBusca(busca, cor.numeroExibicao, cor.numero.toString(), cor.motoboyId?.let { dados.membro(it)?.nome } ?: cor.motoboyNomeArquivado, clienteDaCorrida(cor, dados))
        }
        if (x.isEmpty()) { PwVazio("Nenhuma rota aqui.", icone = PwIcones.ReceiptText); return@PwPagina }
        PwBotaoVidro("Compartilhar planilha (CSV)", onClick = {
            val csv = buildString {
                appendLine("Corrida;Quando;Cliente;Motoboy;Valor;Motoboy (R$);Gestão")
                x.forEach { cor ->
                    appendLine(listOf(cor.numeroExibicao, java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale("pt", "BR")).format(java.util.Date(cor.concluidaEm ?: cor.criadaEm)),
                        clienteDaCorrida(cor, dados), cor.motoboyId?.let { dados.membro(it)?.nome } ?: cor.motoboyNomeArquivado ?: "",
                        cor.valor ?: 0.0, cor.valorMotoboy ?: 0.0, (cor.valor ?: 0.0) - (cor.valorMotoboy ?: 0.0)).joinToString(";") { it.toString().replace(';', ',') })
                }
            }
            tentar {
                ctx.startActivity(android.content.Intent.createChooser(android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/csv")
                    .putExtra(android.content.Intent.EXTRA_SUBJECT, "Extrato " + (com.pwexpress.core.marca.MarcaEmpresa.marca.value?.nomePara("documentos") ?: "Delibra")).putExtra(android.content.Intent.EXTRA_TEXT, csv), "Compartilhar extrato"))
            }
        }, icone = PwIcones.Download, modifier = Modifier.fillMaxWidth())
        x.forEach { cor ->
            PwCartao {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${cor.numeroExibicao} · ${clienteDaCorrida(cor, dados)}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica("${dataHora(cor.concluidaEm ?: cor.criadaEm)} · ${cor.motoboyId?.let { dados.membro(it)?.nome } ?: cor.motoboyNomeArquivado ?: "—"}")
                        TextoDica("Motoboy ${moedaBr(cor.valorMotoboy)} · gestão ${moedaBr((cor.valor ?: 0.0) - (cor.valorMotoboy ?: 0.0))}")
                    }
                    Text(moedaBr(cor.valor), color = c.texto, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Carteira · PIX: as minhas chaves (vem do app) e a principal de cada pessoa da equipe. */
@Composable
private fun PixCarteiraG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val ctx = LocalContext.current
    val dados by Gestao.dados.collectAsState()
    var chaves by remember { mutableStateOf<Map<String, String>?>(null) }
    LaunchedEffect(Unit) { chaves = Gestao.chavesPix() }
    PwPagina {
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = "Chaves PIX") else TituloTela("Carteira", "Chaves PIX", PwIcones.Wallet)
        abas()
        com.pwexpress.core.designsystem.LocalMinhasChavesPix.current()
        PwRotulo("Chaves da equipe")
        TextoDica("A principal de cada pessoa: é para ela que vai o repasse. Toque para copiar.")
        val ch = chaves ?: run { PwCarregando(); return@PwPagina }
        dados.equipe.filter { it.motoboy }.sortedBy { it.nome }.forEach { m ->
            val k = ch[m.id]
            PwCartao(modifier = Modifier.clickable(enabled = k != null) { k?.let { copiar(ctx, it.substringBefore(" ·")); com.pwexpress.core.designsystem.PwToast.mostrar("Chave copiada.") } }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(m.nome, color = c.texto, fontWeight = FontWeight.SemiBold)
                        TextoDica(k ?: "Sem chave PIX cadastrada")
                    }
                    if (k != null) Icon(PwIcones.Copy, null, tint = c.ouro, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/**
 * "Para onde vai esse valor": uma barra e três linhas que sempre somam o total.
 * O que fica com o gestor é o total menos os motoboys (o mesmo número do site).
 */
@Composable
internal fun ParaOndeVai(total: Double, motoboys: Double, dev: Double) {
    val c = Pw.cores
    val gestor = (total - motoboys).coerceAtLeast(0.0)
    val partes = listOf(Triple("Motoboys", motoboys, c.verde), Triple("Gestor", gestor, c.ouro))
    PwCartao {
        PwRotulo("Para onde vai esse valor")
        Espaco(8)
        val forma = RoundedCornerShape(99.dp)
        Row(Modifier.fillMaxWidth().height(14.dp).clip(forma).background(c.vidro)) {
            partes.forEach { (_, v, cor) -> if (total > 0 && v > 0) Box(Modifier.weight((v / total).toFloat().coerceAtLeast(0.01f)).fillMaxHeight().background(cor)) }
        }
        Espaco(10)
        partes.forEach { (r, v, cor) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(cor)); Spacer(Modifier.width(10.dp))
                Text(r, color = c.texto, modifier = Modifier.weight(1f))
                Text(if (total > 0) "${Math.round(v / total * 100)}%" else "—", color = c.texto3, fontSize = 12.5.sp); Spacer(Modifier.width(10.dp))
                Text(moedaBr(v), color = c.texto, fontWeight = FontWeight.Bold)
            }
        }
        androidx.compose.material3.HorizontalDivider(color = c.borda, modifier = Modifier.padding(vertical = 4.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Total", color = c.texto2, modifier = Modifier.weight(1f)); Text(moedaBr(total), color = c.texto, fontWeight = FontWeight.Black)
        }
        TextoDica("Motoboys + gestor = o valor das rotas. O que é \"a repassar\" a cada motoboy está logo abaixo.")
    }
}

@Composable
private fun ResumoCarteiraG(onVoltar: (() -> Unit)?, abas: @Composable () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val teste by TemaApp.modoTeste.collectAsState()
    var periodo by remember { mutableIntStateOf(2) }
    var linhas by remember { mutableStateOf<List<CorridaG>?>(null) }
    LaunchedEffect(periodo, teste) {
        linhas = null
        val cal = java.util.Calendar.getInstance()
        when (periodo) {
            0 -> { cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0); cal.set(java.util.Calendar.SECOND, 0) }
            1 -> cal.add(java.util.Calendar.DAY_OF_MONTH, -7)
            2 -> { cal.set(java.util.Calendar.DAY_OF_MONTH, 1); cal.set(java.util.Calendar.HOUR_OF_DAY, 0); cal.set(java.util.Calendar.MINUTE, 0) }
            else -> cal.add(java.util.Calendar.DAY_OF_MONTH, -30)
        }
        linhas = Gestao.concluidas(cal.timeInMillis, teste)
    }
    PwPagina {
        val sub = if (teste) "Ganhos das corridas de teste" else "Corridas concluídas"
        if (onVoltar != null) Cabecalho("Carteira", onVoltar, subtitulo = sub) else TituloTela("Carteira", sub, PwIcones.Wallet)
        abas()
        PwFiltros(listOf("Hoje" to 0, "7 dias" to 0, "Este mês" to 0, "30 dias" to 0), periodo, { periodo = it })
        val l = linhas ?: run { PwCarregando(); return@PwPagina }
        val total = l.sumOf { it.valor ?: 0.0 }; val moto = l.sumOf { it.valorMotoboy ?: 0.0 }; val gest = l.sumOf { it.valorGestao ?: 0.0 }; val dev = l.sumOf { it.valorDev ?: 0.0 }
        var chaves by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        LaunchedEffect(Unit) { chaves = Gestao.chavesPix() }
        val ctxC = LocalContext.current
        PwNumero("Valor das rotas concluídas · ${l.size} rota${if (l.size == 1) "" else "s"}", moedaBr(total), Modifier.fillMaxWidth(), icone = PwIcones.TrendingUp, destaque = true)
        ParaOndeVai(total, moto, dev)
        @Suppress("UNUSED_VARIABLE") val semUso = gest
        if (l.isEmpty()) { PwVazio("Nenhuma corrida concluída neste período.", icone = PwIcones.Wallet); return@PwPagina }
        PwCartao {
            PwRotulo("Por motoboy")
            l.groupBy { it.motoboyId }.entries.sortedByDescending { e -> e.value.sumOf { it.valor ?: 0.0 } }.forEach { (mid, cs) ->
                Espaco(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(dados.membro(mid)?.nome ?: cs.first().motoboyNomeArquivado ?: "—", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica("${cs.size} corrida${if (cs.size == 1) "" else "s"} · faturou ${moedaBr(cs.sumOf { it.valor ?: 0.0 })} · gestão ${moedaBr(cs.sumOf { (it.valor ?: 0.0) - (it.valorMotoboy ?: 0.0) })}")
                        TextoDica(chaves[mid]?.let { "PIX: $it" } ?: "Sem chave PIX cadastrada")
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(moedaBr(cs.sumOf { it.valorMotoboy ?: 0.0 }), color = c.texto, fontWeight = FontWeight.Bold)
                        TextoDica("a repassar")
                    }
                    chaves[mid]?.let { ch -> PwBotaoIcone(PwIcones.Copy, "Copiar chave PIX", { copiar(ctxC, ch.substringBefore(" ·")); com.pwexpress.core.designsystem.PwToast.mostrar("Chave PIX copiada.") }, tamanho = 38.dp) }
                }
            }
        }
        PwCartao {
            PwRotulo("Por cliente")
            l.groupBy { it.empresaId ?: it.clienteNome?.let { n -> "avulso:$n" } ?: "—" }.entries.sortedByDescending { e -> e.value.sumOf { it.valor ?: 0.0 } }.forEach { (k, cs) ->
                Espaco(8)
                val nome = when { k == "—" -> "Avulso (sem nome)"; k.startsWith("avulso:") -> "${k.removePrefix("avulso:")} (avulso)"; else -> dados.empresa(k)?.nome ?: "—" }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(nome, color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); TextoDica("${cs.size} corrida${if (cs.size == 1) "" else "s"} · gestão ${moedaBr(cs.sumOf { (it.valor ?: 0.0) - (it.valorMotoboy ?: 0.0) })}") }
                    Spacer(Modifier.width(8.dp))
                    Text(moedaBr(cs.sumOf { it.valor ?: 0.0 }), color = c.texto, fontWeight = FontWeight.Bold)
                }
            }
        }
        PwCartao {
            PwRotulo("Extrato")
            l.take(150).forEach { k ->
                Espaco(8)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${k.numeroExibicao} · ${dados.cliente(k)}", color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextoDica("${k.concluidaEm?.let { dataHora(it) } ?: ""} · ${dados.nomeMotoboy(k) ?: "—"}")
                        TextoDica("Motoboy ${moedaBr(k.valorMotoboy)} · gestão ${moedaBr((k.valor ?: 0.0) - (k.valorMotoboy ?: 0.0))}")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(moedaBr(k.valor), color = c.texto, fontWeight = FontWeight.Bold)
                }
            }
            if (l.size > 150) { Espaco(6); TextoDica("Mostrando as 150 mais recentes. O extrato completo (com planilha) está no site.") }
        }
    }
}

// =====================================================================
// Configurações
// =====================================================================

@Composable
fun ConfiguracoesG(onVoltar: (() -> Unit)?, onAbrir: (TelaG) -> Unit = {}) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    var area by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }
    var secao by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf<String?>(null) }
    var soMudados by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    var busca by remember { mutableStateOf("") }
    var estado by remember { mutableStateOf<Pair<String, String>?>(null) }
    val escopo = rememberCoroutineScope()
    fun sem(t: String) = java.text.Normalizer.normalize(t, java.text.Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase()
    val grupoArea = GRUPOS_CONFIG.associate { it.chave to it.area }
    fun mudado(p: Param): Boolean {
        if (p.tipo == "multi") {
            val l = (dados.config[p.chave] as? kotlinx.serialization.json.JsonArray)?.mapNotNull { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }?.sorted() ?: return false
            return l != p.padraoTxt.orEmpty().split(",").filter { it.isNotBlank() }.sorted()
        }
        val v = (dados.config[p.chave] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return false
        return when (p.tipo) {
            "booleano" -> v.toBooleanStrictOrNull()?.let { it != p.padraoBool } ?: false
            "escolha", "texto" -> v != (p.padraoTxt ?: "")
            else -> v.toDoubleOrNull()?.let { kotlin.math.abs(it - p.padraoNum) > 1e-9 } ?: false
        }
    }
    fun padraoJson(p: Param): kotlinx.serialization.json.JsonElement = when (p.tipo) {
        "booleano" -> kotlinx.serialization.json.JsonPrimitive(p.padraoBool)
        "escolha", "texto" -> kotlinx.serialization.json.JsonPrimitive(p.padraoTxt ?: "")
        "multi" -> kotlinx.serialization.json.JsonArray(p.padraoTxt.orEmpty().split(",").filter { it.isNotBlank() }.map { kotlinx.serialization.json.JsonPrimitive(it) })
        else -> kotlinx.serialization.json.JsonPrimitive(p.padraoNum)
    }
    fun salvar(p: Param, v: kotlinx.serialization.json.JsonElement) {
        estado = p.chave to "salvando"
        escopo.launch { Gestao.salvarConfig(p.chave, v).onSuccess { estado = p.chave to "salvo" }.onFailure { estado = p.chave to (it.message ?: "erro") } }
    }

    PwPagina {
        if (onVoltar != null) Cabecalho("Ajustes", onVoltar, subtitulo = "Valem para todo o sistema · salvam sozinhos")
        else TituloTela("Ajustes", "Valem para todo o sistema · salvam sozinhos", PwIcones.Settings2)
        PwCampo(busca, { busca = it }, "Buscar em todos os ajustes…")
        val termo = sem(busca.trim())
        val nMudados = PARAMETROS_VISIVEIS.count(::mudado)
        PwInterruptor("Só o que mudei do padrão", soMudados, { soMudados = it }, descricao = if (nMudados == 0) "Tudo está no padrão." else "$nMudados ajuste${if (nMudados == 1) "" else "s"} diferente${if (nMudados == 1) "" else "s"} do padrão.")
        if (termo.isEmpty() && !soMudados) {
            // áreas (como no site): cada uma junta seções parecidas
            PwFiltros(AREAS_CONFIG.map { a -> a.titulo to PARAMETROS_VISIVEIS.count { grupoArea[it.grupo] == a.chave && mudado(it) } }, area, { area = it; secao = null })
            val a = AREAS_CONFIG.getOrNull(area)
            if (a != null) TextoDica(a.descricao)
            val secoes = GRUPOS_CONFIG.filter { it.area == a?.chave && PARAMETROS_VISIVEIS.any { p -> p.grupo == it.chave } }
            if (secoes.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.pwexpress.core.designsystem.PwPilula("Todas", PwIcones.Lista, { secao = null }, ligada = secao == null)
                secoes.forEach { g -> com.pwexpress.core.designsystem.PwPilula(g.titulo, PwIcones.SlidersHorizontal, { secao = if (secao == g.chave) null else g.chave }, ligada = secao == g.chave) }
            }
            // cadastros que ficam perto dos ajustes da área
            val extras = when (a?.chave) {
                "preco" -> listOf(Triple("Exceções", PwIcones.Layers, TelaG.Excecoes), Triple("Zonas de preço", PwIcones.MapPin, TelaG.CatalogoT(Catalogo.ZONAS)), Triple("Horários de pico", PwIcones.Clock, TelaG.CatalogoT(Catalogo.HORARIOS)))
                "clientes" -> listOf(Triple("Exceções", PwIcones.Layers, TelaG.Excecoes), Triple("Categorias", PwIcones.Package2, TelaG.CatalogoT(Catalogo.CATEGORIAS)), Triple("Tipos de pedido", PwIcones.ClipboardList, TelaG.CatalogoT(Catalogo.TIPOS)))
                "operacao" -> listOf(Triple("Exceções", PwIcones.Layers, TelaG.Excecoes), Triple("Tipos de pedido", PwIcones.ClipboardList, TelaG.CatalogoT(Catalogo.TIPOS)))
                else -> listOf(Triple("Exceções", PwIcones.Layers, TelaG.Excecoes))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                PwRotulo("Peças do lego"); Spacer(Modifier.width(8.dp))
                com.pwexpress.core.designsystem.PwDica("Peças do lego", "Cada regra tem um valor geral e pode ter exceção por tipo de pedido, cliente, motoboy ou num pedido só. Vale a mais específica: pedido, motoboy, cliente, tipo e, por último, a geral.")
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                extras.forEach { (t, i, tela) -> com.pwexpress.core.designsystem.PwPilula(t, i, { onAbrir(tela) }) }
            }
        }
        val a = AREAS_CONFIG.getOrNull(area)
        val lista = when {
            termo.isNotEmpty() -> PARAMETROS_VISIVEIS.filter { com.pwexpress.core.busca.combina(termo, it.rotulo, it.descricao, it.descricaoDesligado, it.chave) }.filter { !soMudados || mudado(it) }
            soMudados -> PARAMETROS_VISIVEIS.filter(::mudado)
            else -> PARAMETROS_VISIVEIS.filter { grupoArea[it.grupo] == a?.chave && (secao == null || it.grupo == secao) }
        }
        val souDev = com.pwexpress.core.perfil.Perfis.perfil.collectAsState().value?.dev == true
        GRUPOS_CONFIG.forEach { g ->
            val doGrupo = lista.filter { it.grupo == g.chave }
            if (doGrupo.isEmpty()) return@forEach
            Espaco(4)
            Text(g.titulo, color = c.texto, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            TextoDica(if (termo.isNotEmpty() || soMudados) "${AREAS_CONFIG.find { it.chave == g.area }?.titulo ?: ""} · ${g.descricao}" else g.descricao)
            if (termo.isEmpty() && !soMudados) ExemploConfig(g.chave, dados.config)
            doGrupo.forEach { p ->
                val st = estado?.takeIf { it.first == p.chave }?.second
                val dif = mudado(p)
                PwCartao(padding = PaddingValues(14.dp), cor = if (dif) c.ouro.copy(alpha = 0.45f) else null) {
                    CampoParametro(p, dados.config[p.chave], { salvar(p, it) }, bloqueado = p.soDev && !souDev) {
                        if (p.escopos.isNotEmpty()) TextoDica("Pode ter exceção por " + p.escopos.joinToString(", ") { mapOf("tipo" to "tipo de pedido", "cliente" to "cliente", "motoboy" to "motoboy")[it] ?: it } + ".")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (st) {
                            null, "salvando" -> {}
                            "salvo" -> Text("✓ salvo", color = c.verde, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            else -> PwErro(st)
                        }
                        Spacer(Modifier.weight(1f))
                        if (dif && !(p.soDev && !souDev)) Row(Modifier.clip(RoundedCornerShape(99.dp)).clickable { salvar(p, padraoJson(p)) }.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(PwIcones.RotateCcw, null, tint = c.ouro, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                            Text("Voltar ao padrão", color = c.ouro, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (lista.isEmpty()) PwVazio(if (soMudados) "Tudo no padrão." else "Nenhum ajuste encontrado.", icone = PwIcones.Search)
    }
}

/** Contas de exemplo no topo de algumas seções (o mesmo cálculo do banco). */
@Composable
private fun ExemploConfig(grupo: String, cfg: Map<String, kotlinx.serialization.json.JsonElement>) {
    val c = Pw.cores
    fun r2(v: Double) = Math.round(v * 100) / 100.0
    when (grupo) {
        "despacho" -> CompartilharEmpresaG()
        "distancia" -> {
            fun faixa(kmv: Double, cada: Double, f: Double) = if (cada > 0) Math.floor(kmv / cada) * f else 0.0
            val fc = faixa(250.0, cfg.numero("acrescimo_coleta_cada_km", 100.0), cfg.numero("acrescimo_coleta_fator", 0.5))
            val fe = faixa(450.0, cfg.numero("acrescimo_entrega_cada_km", 200.0), cfg.numero("acrescimo_entrega_fator", 0.25))
            PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
                PwRotulo("Exemplo")
                TextoDica("Corrida de R$ 100,00, motoboy a 250 km da coleta e 450 km de percurso:")
                Text(if (cfg.logico("acrescimo_distancia", true)) "100 × (1 + ${com.pwexpress.core.designsystem.numeroBr(fc)} + ${com.pwexpress.core.designsystem.numeroBr(fe)}) = ${moedaBr(100 * (1 + fc + fe))}" else "Acréscimo desligado: fica R$ 100,00.", color = c.texto, fontWeight = FontWeight.Bold)
            }
        }
        "pagamento" -> {
            val pct = cfg.numero("repasse_motoboy_pct", 67.5)
            val teto = 100.0
            val pMoto = minOf(teto, r2(pct))
            PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
                PwRotulo("Exemplo: rota de R$ 100,00")
                if (cfg.textoCfg("pag_modo", "zonas") == "percentual") Text("Motoboy ${moedaBr(pMoto)} · Gestor ${moedaBr(r2(100 - pMoto))}", color = c.texto, fontWeight = FontWeight.Bold)
                TextoDica("O motoboy nunca leva mais que a rota paga: no máximo ${moedaBr(teto)}, por km, zona ou percentual, com ajuste ou sem.")
            }
        }
        "pag_zonas" -> {
            val raio = cfg.numero("zona_entrega_raio_m", 1500.0); val km = cfg.numero("zona_valor_km", 2.0); val extra = cfg.numero("zona_entrega_extra", 10.0)
            val fixo = cfg.numero("zona_fixo_rota", 0.0); val fora = cfg.numero("zona_fora_pct", 65.0)
            PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
                PwRotulo("Exemplo: rota de R$ 18,00 com 2 entregas")
                TextoDica("Cada entrega tem um raio de ${com.pwexpress.core.designsystem.numeroBr(raio)} m. Se os raios se encontram, as entregas estão na mesma zona.")
                Text("Mesma zona (a mais perto a 1,45 km): 1,45 × ${moedaBr(km)} + ${moedaBr(extra)} por entrega a mais${if (fixo > 0) " + ${moedaBr(fixo)}" else ""} = ${moedaBr(r2(1.45 * km + extra + fixo))}", color = c.texto, fontWeight = FontWeight.Bold)
                Text("Zonas separadas: ${com.pwexpress.core.designsystem.numeroBr(fora)}% da rota = ${moedaBr(r2(18 * fora / 100))}", color = c.texto, fontWeight = FontWeight.Bold)
            }
        }
        "precocliente" -> if (cfg.textoCfg("preco_distancia_modo", "soma_entregas") == "soma_entregas") PwCartao(cor = c.ouro.copy(alpha = 0.5f)) {
            PwRotulo("Exemplo: 1 coleta, 3 entregas")
            val vkm = cfg.numero("preco_km", 2.5)
            TextoDica("O cliente paga o valor/km × a distância da coleta até cada entrega, somadas.")
            Text("(2 + 3,5 + 5) km × ${moedaBr(vkm)} = ${moedaBr(r2(10.5 * vkm))} + taxas e extras", color = c.texto, fontWeight = FontWeight.Bold)
        }
    }
}

@Suppress("unused") private val usado = JsonNull


/** Veículo e CNH do motoboy (o gestor edita; o motoboy só vê, a não ser que a regra deixe). */
@Composable
internal fun VeiculoG(id: String) {
    val c = Pw.cores
    val escopo = androidx.compose.runtime.rememberCoroutineScope()
    var v by remember { androidx.compose.runtime.mutableStateOf<com.pwexpress.feature.gestor.data.VeiculoG?>(null) }
    var erro by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var salvando by remember { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(id) { v = Gestao.veiculo(id) ?: com.pwexpress.feature.gestor.data.VeiculoG() }
    val x = v ?: run { PwCarregando(); return }
    fun mudar(f: (com.pwexpress.feature.gestor.data.VeiculoG) -> com.pwexpress.feature.gestor.data.VeiculoG) { v = f(x) }
    PwCartao {
        PwRotulo("Veículo")
        PwSegmentado(listOf("Moto" to PwIcones.Bike, "Carro" to PwIcones.Route, "Bicicleta" to PwIcones.Bike, "Van" to PwIcones.Package),
            listOf("moto", "carro", "bicicleta", "van").indexOf(x.tipo).coerceAtLeast(0), { i -> mudar { it.copy(tipo = listOf("moto", "carro", "bicicleta", "van")[i]) } })
        Espaco(10)
        com.pwexpress.core.designsystem.PwPlaca(x.placa, largura = 200.dp)
        Espaco(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(x.placa, { t -> mudar { it.copy(placa = t.uppercase()) } }, "Placa", dica = "AAA0A00", modifier = Modifier.weight(1f))
            PwCampo(x.cor, { t -> mudar { it.copy(cor = t) } }, "Cor", modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(x.modelo, { t -> mudar { it.copy(modelo = t) } }, "Modelo", dica = "Ex.: CG 160", modifier = Modifier.weight(2f))
            PwCampo(x.ano, { t -> mudar { it.copy(ano = t.filter(Char::isDigit).take(4)) } }, "Ano", teclado = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1f))
        }
    }
    PwCartao {
        PwRotulo("CNH")
        PwCampo(x.cnhNumero, { t -> mudar { it.copy(cnhNumero = t.filter(Char::isDigit).take(11)) } }, "Número", teclado = androidx.compose.ui.text.input.KeyboardType.Number)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PwCampo(x.cnhCategoria, { t -> mudar { it.copy(cnhCategoria = t.uppercase().take(3)) } }, "Categoria", dica = "A, AB…", modifier = Modifier.weight(1f))
            PwCampo(x.cnhValidade, { t -> mudar { it.copy(cnhValidade = t) } }, "Validade", dica = "dd/mm/aaaa", teclado = androidx.compose.ui.text.input.KeyboardType.Number, modifier = Modifier.weight(1.4f))
        }
        x.venceEmDias?.let { d ->
            if (d < 0) Text("CNH vencida há ${-d} dia${if (d == -1L) "" else "s"}.", color = c.vermelho, fontWeight = FontWeight.SemiBold)
            else if (d <= 30) Text("CNH vence em $d dia${if (d == 1L) "" else "s"}.", color = c.ouro, fontWeight = FontWeight.SemiBold)
        }
    }
    PwErro(erro)
    PwBotaoOuro(if (salvando) "Salvando…" else "Salvar veículo", onClick = {
        salvando = true
        escopo.launch {
            Gestao.salvarVeiculo(id, x).onSuccess { v = it; erro = null; com.pwexpress.core.designsystem.PwToast.mostrar("Veículo salvo.", com.pwexpress.core.designsystem.TipoAviso.OK) }
                .onFailure { erro = it.message }
            salvando = false
        }
    }, icone = PwIcones.Check, habilitado = !salvando)
}


/** Empresas que se cadastraram pelo site e esperam aprovação. */
@Composable
private fun CadastrosPendentesG() {
    val c = Pw.cores
    val escopo = rememberCoroutineScope()
    var lista by remember { mutableStateOf<List<kotlinx.serialization.json.JsonObject>>(emptyList()) }
    var versao by remember { mutableIntStateOf(0) }
    var aberto by remember { mutableStateOf<kotlinx.serialization.json.JsonObject?>(null) }
    LaunchedEffect(versao) { lista = Gestao.tabela("cadastros_cliente", "select=*&status=eq.pendente&order=criado_em") }
    if (lista.isEmpty()) return
    PwCartao(cor = c.ouro.copy(alpha = 0.6f)) {
        PwRotulo("Cadastros esperando aprovação · ${lista.size}")
        lista.forEach { cd ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { aberto = cd }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cd.str("nome").orEmpty(), color = c.texto, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    TextoDica(listOfNotNull(cd.str("documento")?.let { if (it.length == 11) formatarCpf(it) else formatarCnpj(it) }, cd.str("email"), cd.str("telefone")).joinToString(" · "))
                }
                Icon(PwIcones.ChevronRight, null, tint = c.texto3, modifier = Modifier.size(18.dp))
            }
        }
    }
    aberto?.let { cd ->
        val expressPadrao = dados_cfgBool("cadastro_express_liberado")
        var express by remember(cd) { mutableStateOf(expressPadrao) }
        var credito by remember(cd) { mutableStateOf(false) }
        var motivo by remember(cd) { mutableStateOf("") }
        var erro by remember(cd) { mutableStateOf<String?>(null) }
        fun revisar(aprovar: Boolean) = escopo.launch {
            Gestao.rpcJson("revisar_cadastro", kotlinx.serialization.json.buildJsonObject {
                put("p_id", cd.str("id")!!); put("p_aprovar", aprovar); put("p_motivo", motivo.ifBlank { null })
                put("p_opcoes", kotlinx.serialization.json.buildJsonObject { put("pode_express", express); put("cobranca", if (credito) "credito" else "pre_pago") })
            }).onSuccess {
                com.pwexpress.core.designsystem.PwToast.mostrar(if (aprovar) "Cliente aprovado: já pode pedir pelo site." else "Cadastro recusado.", com.pwexpress.core.designsystem.TipoAviso.OK)
                aberto = null; versao++; Gestao.atualizar()
            }.onFailure { erro = it.message }
        }
        com.pwexpress.core.designsystem.PwTelaCheia(cd.str("nome").orEmpty(), { aberto = null }, subtitulo = "Cadastro pelo site") {
            val la = cd.dbl("lat"); val lo = cd.dbl("lng")
            if (la != null && lo != null) {
                val formaM = RoundedCornerShape(20.dp)
                Box(Modifier.fillMaxWidth().height(180.dp).clip(formaM).border(1.dp, c.borda, formaM)) {
                    PwMapa(paradas = listOf(com.pwexpress.core.mapa.PinoMapa(la, lo, true, true, false)), rota = null, minhaPosicao = null,
                        modifier = Modifier.fillMaxSize(), enquadrar = cd.str("id"))
                }
            }
            PwCartao {
                listOf("Responsável" to cd.str("responsavel"), "Documento" to cd.str("documento")?.let { if (it.length == 11) formatarCpf(it) else formatarCnpj(it) },
                    "E-mail" to cd.str("email"), "Telefone" to cd.str("telefone"), "Endereço" to cd.str("endereco"), "Observações" to cd.str("observacoes"))
                    .filter { it.second != null }.forEach { (r, v) -> TextoDica(r); Text(v!!, color = c.texto); Espaco(4) }
            }
            PwInterruptor("Já pode pedir Express", express, { express = it })
            PwInterruptor("Cobrança no crédito (fatura)", credito, { credito = it }, descricao = if (credito) "Paga por fatura no ciclo." else "Paga antes de sair (pré-pago).")
            PwCampo(motivo, { motivo = it }, "Motivo (se recusar)")
            PwErro(erro)
            PwBotaoOuro("Aprovar", onClick = { revisar(true) }, icone = PwIcones.Check)
            PwBotaoPerigo("Recusar", onClick = { revisar(false) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun dados_cfgBool(k: String): Boolean = Gestao.dados.collectAsState().value.cfgBool(k, false)


/** Gestor/Dev que também é cliente: entra no site do cliente como este cadastro. */
@Composable
private fun VincularClienteG(id: String, nome: String) {
    val dados by Gestao.dados.collectAsState()
    val escopo = rememberCoroutineScope()
    var atual by remember(id) { mutableStateOf<String?>(null) }
    var carregado by remember(id) { mutableStateOf(false) }
    LaunchedEffect(id) {
        atual = tentar { Gestao.tabela("perfis", "id=eq.$id&select=empresa_id").firstOrNull()?.str("empresa_id") }.getOrNull()
        carregado = true
    }
    if (!carregado) return
    com.pwexpress.core.designsystem.PwSelecao("Também é cliente (entra no site do cliente como)", listOf("" to "Não") + dados.empresas.map { it.id to it.nome }, atual ?: "", { v ->
        escopo.launch {
            Gestao.rpcJson("vincular_cliente", kotlinx.serialization.json.buildJsonObject {
                put("p_usuario", kotlinx.serialization.json.JsonPrimitive(id)); put("p_empresa", if (v.isEmpty()) kotlinx.serialization.json.JsonNull else kotlinx.serialization.json.JsonPrimitive(v))
            }).onSuccess {
                atual = v.ifEmpty { null }
                com.pwexpress.core.designsystem.PwToast.mostrar(if (v.isEmpty()) "Não é mais cliente." else "Agora $nome também é cliente de ${dados.empresa(v)?.nome ?: ""}.")
            }.onFailure { com.pwexpress.core.designsystem.PwToast.mostrar(it.message ?: "Erro", com.pwexpress.core.designsystem.TipoAviso.ERRO) }
        }
    })
}
