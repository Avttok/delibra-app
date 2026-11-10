package com.pwexpress.core.mapa

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pwexpress.core.designsystem.PwFiltros
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Hoje / Ontem / Anteontem / Outro dia (abre o calendário). [dia] = quantos dias atrás. */
@Composable
fun SeletorDia(dia: Int, onDia: (Int) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val hoje = LocalDate.now(ZoneId.systemDefault())
    val outro = if (dia >= 3) hoje.minusDays(dia.toLong()).format(DateTimeFormatter.ofPattern("dd/MM/yy")) else "Outro dia"
    PwFiltros(listOf("Hoje" to 0, "Ontem" to 0, "Anteontem" to 0, outro to 0), minOf(dia, 3), { i ->
        if (i < 3) onDia(i)
        else {
            val base = hoje.minusDays(maxOf(dia, 3).toLong())
            android.app.DatePickerDialog(ctx, { _, a, m, d ->
                val escolhido = LocalDate.of(a, m + 1, d)
                onDia(java.time.temporal.ChronoUnit.DAYS.between(escolhido, hoje).toInt().coerceAtLeast(0))
            }, base.year, base.monthValue - 1, base.dayOfMonth).apply {
                datePicker.maxDate = System.currentTimeMillis()
            }.show()
        }
    }, modifier)
}
