package com.pwexpress.feature.gestor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pwexpress.core.designsystem.Pw
import com.pwexpress.core.designsystem.PwBotaoPerigo
import com.pwexpress.core.designsystem.PwBotaoVidro
import com.pwexpress.core.designsystem.PwCampo
import com.pwexpress.core.designsystem.PwErro
import com.pwexpress.core.designsystem.PwFiltros
import com.pwexpress.core.designsystem.PwIcones
import com.pwexpress.core.designsystem.PwInterruptor
import com.pwexpress.core.designsystem.PwJanela
import com.pwexpress.core.designsystem.PwSelecao
import com.pwexpress.core.designsystem.PwToast
import com.pwexpress.core.designsystem.numeroBr
import com.pwexpress.core.perfil.Perfis
import com.pwexpress.feature.gestor.data.Gestao
import kotlinx.coroutines.launch

private val PERIODOS = listOf("Tudo" to null, "+ de 7 dias" to 7L, "+ de 30 dias" to 30L)

/**
 * Limpeza do histórico de todos (gestor e Dev): corridas finalizadas e/ou percursos (pings),
 * só do modo atual (teste ou real), de todos ou de um motoboy, de tudo ou só do que é antigo.
 * Corridas ativas, agendadas e reservadas nunca entram. Pede a senha.
 */
@Composable
fun DialogoApagarHistorico(teste: Boolean, onFechar: () -> Unit) {
    val c = Pw.cores
    val dados by Gestao.dados.collectAsState()
    val souDev = Perfis.perfil.value?.dev == true
    var corridas by remember { mutableStateOf(true) }
    var percursos by remember { mutableStateOf(true) }
    var motoboy by remember { mutableStateOf<String?>(null) }
    var periodo by remember { mutableIntStateOf(0) }
    var senha by remember { mutableStateOf("") }
    var entendi by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }
    var enviando by remember { mutableStateOf(false) }
    var previa by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val escopo = rememberCoroutineScope()
    // gestor não mexe nas contas Dev
    val motoboys = dados.equipe.filter { it.motoboy && (souDev || !it.dev) }.sortedBy { it.nome }
    val ateMs = PERIODOS[periodo].second?.let { System.currentTimeMillis() - it * 86_400_000L }

    LaunchedEffect(motoboy, periodo, teste) { previa = null; previa = Gestao.previaHistorico(teste, motoboy, ateMs) }

    PwJanela(if (teste) "Apagar histórico de teste" else "Apagar histórico", onFechar = onFechar, fecharFora = !enviando) {
        Text(
            "Vale só para o modo ${if (teste) "teste" else "real"}. Corridas em andamento, agendadas ou reservadas não entram. Fica uma cópia de quem apagou.",
            color = c.texto2,
        )
        PwInterruptor("Corridas finalizadas", corridas, { corridas = it }, descricao = "Concluídas, canceladas e sem motoboy, com comprovantes e trajeto", icone = PwIcones.Route)
        PwInterruptor("Percursos (GPS)", percursos, { percursos = it }, descricao = "Pontos de quando estavam procurando corrida", icone = PwIcones.MapPin)
        PwSelecao("De quem", listOf<Pair<String?, String>>(null to "Todos") + motoboys.map { it.id to it.nome }, motoboy, { motoboy = it })
        PwFiltros(PERIODOS.map { it.first to 0 }, periodo, { periodo = it })
        previa?.let { (nc, np) ->
            val partes = buildList {
                if (corridas) add("$nc corrida${if (nc == 1) "" else "s"}")
                if (percursos) add("${numeroBr(np.toDouble(), 0)} ponto${if (np == 1) "" else "s"} de percurso")
            }
            if (partes.isNotEmpty()) Text("Vai apagar: ${partes.joinToString(" e ")}.", color = c.vermelho, fontWeight = FontWeight.SemiBold)
        }
        PwInterruptor("Entendo que é definitivo", entendi, { entendi = it }, cor = c.vermelho)
        PwCampo(senha, { senha = it }, "Sua senha", senha = true, teclado = KeyboardType.Password)
        PwErro(erro)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            PwBotaoVidro("Voltar", onClick = onFechar, habilitado = !enviando, modifier = Modifier.weight(1f))
            PwBotaoPerigo(
                if (enviando) "Apagando…" else "Apagar", cheio = true, modifier = Modifier.weight(1f),
                habilitado = entendi && senha.isNotEmpty() && (corridas || percursos) && !enviando,
                onClick = {
                    enviando = true; erro = null
                    escopo.launch {
                        Gestao.apagarHistorico(senha, teste, corridas, percursos, motoboy, ateMs)
                            .onSuccess { (nc, np) ->
                                Gestao.atualizar()
                                PwToast.mostrar("$nc corrida${if (nc == 1) "" else "s"} e ${numeroBr(np.toDouble(), 0)} pontos apagados.", titulo = "Histórico limpo")
                                onFechar()
                            }
                            .onFailure { erro = it.message; senha = "" }
                        enviando = false
                    }
                },
            )
        }
    }
}
