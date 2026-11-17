package com.pwexpress.core

import com.pwexpress.core.update.NotasPorArea
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotasPorAreaTest {
    private val notas = """
        # Delibra · novidades
        ## Todos
        - Avisos no topo
        ## Motoboy
        - Ficha da rota
        ## Gestor
        - Matriz
        ## Dev
        - Fantasma
    """.trimIndent()

    @Test fun motoboyVeSoODele() {
        val r = NotasPorArea.filtrar(notas, NotasPorArea.areasDe(listOf("motoboy"), false))
        assertTrue("Ficha da rota" in r); assertTrue("Avisos no topo" in r)
        assertFalse("Matriz" in r); assertFalse("Fantasma" in r)
    }

    @Test fun gestorEMotoboyVeAsDuas() {
        val r = NotasPorArea.filtrar(notas, NotasPorArea.areasDe(listOf("gestor", "motoboy"), false))
        assertTrue("Ficha da rota" in r && "Matriz" in r); assertFalse("Fantasma" in r)
    }

    @Test fun devVeTudo() {
        val r = NotasPorArea.filtrar(notas, NotasPorArea.areasDe(listOf("dev", "gestor"), true))
        assertTrue("Fantasma" in r && "Ficha da rota" in r && "Matriz" in r)
    }

    @Test fun notasAntigasSemAreaValemParaTodos() {
        val r = NotasPorArea.filtrar("**Para o motoboy**\n- algo", setOf("todos"))
        assertTrue("algo" in r)
    }

    @Test fun tituloComAcentoEPrefixo() {
        assertTrue(NotasPorArea.areaDoTitulo("## Para o Gestor") == "gestor")
        assertTrue(NotasPorArea.areaDoTitulo("## Clientes") == "cliente")
        assertTrue(NotasPorArea.areaDoTitulo("## No mapa") == null)
    }
}
