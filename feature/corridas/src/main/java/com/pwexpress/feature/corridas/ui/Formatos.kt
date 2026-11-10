package com.pwexpress.feature.corridas.ui

import java.text.NumberFormat
import java.util.Locale

internal val localeBr: Locale = Locale.forLanguageTag("pt-BR")

internal fun moeda(v: Double?): String =
    if (v == null) "—" else NumberFormat.getCurrencyInstance(localeBr).format(v)

internal fun km(m: Int?): String =
    if (m == null) "—" else String.format(localeBr, "%.1f km", m / 1000.0)

internal fun minutos(s: Int?): String {
    if (s == null) return "—"
    val min = (s + 30) / 60
    return if (min < 60) "$min min" else "${min / 60} h ${min % 60} min"
}
