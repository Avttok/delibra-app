package com.pwexpress.core

import com.pwexpress.core.mapa.PingMapa
import com.pwexpress.core.mapa.separarRegistros
import com.pwexpress.core.mapa.trilhasDoFeito
import com.pwexpress.core.perfil.Modalidades
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Testes da lógica que não precisa de aparelho: percurso, trilha da corrida e nomes/campos próprios. */
class LogicaTest {
    private val min = 60_000L
    private fun ping(lat: Double, lng: Double, t: Long, status: String = "em_corrida", corrida: String? = "c1") = PingMapa(lat, lng, status, corrida, t)

    @Test fun percursoContinuoFicaInteiro() {
        val regs = separarRegistros((0 until 10).map { ping(-26.9 + it * 0.0005, -49.1, it * 30_000L) })
        assertEquals(1, regs.size)
        assertEquals(10, regs[0].pontos.size)
    }

    @Test fun semSinalLongeSeparaEmDoisPedacos() {
        // 10 min sem ping e reaparece a ~1,1 km: não pode ligar com reta
        val antes = (0 until 5).map { ping(-26.9, -49.1 + it * 0.0002, it * 30_000L) }
        val depois = (0 until 5).map { ping(-26.91, -49.1 + it * 0.0002, 12 * min + it * 30_000L) }
        assertEquals(2, separarRegistros(antes + depois).size)
    }

    @Test fun semSinalPertoContinua() {
        // parado 10 min no mesmo lugar (ex.: esperando na coleta): é o mesmo registro
        val a = listOf(ping(-26.9, -49.1, 0), ping(-26.9001, -49.1, 30_000L), ping(-26.9002, -49.1, 10 * min))
        assertEquals(1, separarRegistros(a).size)
    }

    @Test fun offlineQuebraOPercurso() {
        val a = listOf(ping(-26.9, -49.1, 0), ping(-26.9, -49.1, min, "offline"), ping(-26.9001, -49.1, 2 * min))
        assertEquals(2, separarRegistros(a).size)
    }

    @Test fun trilhaDaCorridaEmPedacos() {
        val p1 = listOf(-26.9 to -49.1, -26.901 to -49.1)
        val p2 = listOf(-26.92 to -49.1, -26.921 to -49.1)
        val t = trilhasDoFeito(listOf(p1, p2, listOf(-26.95 to -49.1)), -26.96 to -49.1)
        // o último pedaço (1 ponto) ganha a posição de agora e vira linha; nenhum pedaço se liga ao outro
        assertEquals(3, t.size)
        assertEquals(p1, t[0].pontos)
        assertEquals(-26.96 to -49.1, t[2].pontos.last())
        assertTrue(trilhasDoFeito(listOf(listOf(-26.9 to -49.1)), null).isEmpty())
    }

    @Test fun nomesECamposProprios() {
        Modalidades.atualizar(Json.parseToJsonElement("""
            {"rotulos": {"|parada:coleta": "Retirada", "farmacia|parada:entrega": "Dispensação", "|pedido:recebido": "Aguardando"},
             "campos": [
               {"chave": "lacre", "rotulo": "Lacre", "tipo": "texto", "entidade": "ponto", "ponto_tipo": "coleta", "quem_preenche": "motoboy", "etapa": "chegou", "visivel_motoboy": true},
               {"chave": "troco", "rotulo": "Troco", "tipo": "dinheiro", "entidade": "ponto", "ponto_tipo": "entrega", "quem_preenche": "motoboy", "visivel_motoboy": true},
               {"chave": "receita", "rotulo": "Receita", "tipo": "foto", "entidade": "ponto", "quem_preenche": "motoboy", "tipos_pedido": ["farmacia"], "visivel_motoboy": true},
               {"chave": "nota", "rotulo": "Nota", "tipo": "texto", "entidade": "pedido", "quem_preenche": "cliente"}
             ]}
        """.trimIndent()).jsonObject)
        assertEquals("Retirada", Modalidades.nomeParada(true))
        assertEquals("Entrega", Modalidades.nomeParada(false))
        assertEquals("Dispensação", Modalidades.nomeParada(false, "farmacia"))
        assertEquals("Retirada", Modalidades.nomeParada(true, "farmacia"))
        assertEquals("Aguardando", Modalidades.nomeStatus("pedido", "recebido", "Recebido"))
        assertEquals(listOf("lacre"), Modalidades.camposDaParada(true, null, "chegou").map { it.chave })
        assertEquals(emptyList<String>(), Modalidades.camposDaParada(true, "padrao").map { it.chave })
        assertEquals(listOf("troco"), Modalidades.camposDaParada(false, "padrao").map { it.chave })
        assertEquals(setOf("troco", "receita"), Modalidades.camposDaParada(false, "farmacia").map { it.chave }.toSet())
    }
}
