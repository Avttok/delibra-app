package com.pwexpress.feature.gestor.data

/** Ajustes que a conta vê: os "escondidos" (combinado entre o Dev e a empresa) só aparecem para o Dev. */
val PARAMETROS_VISIVEIS: List<Param>
    get() = if (com.pwexpress.core.perfil.Perfis.perfil.value?.dev == true) PARAMETROS else PARAMETROS.filter { !it.escondido }
