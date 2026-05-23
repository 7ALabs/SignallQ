package io.linka.app.kotlin.feature.history

import kotlinx.coroutines.flow.StateFlow

interface ObservadorHistorico {
    val resumoFlow: StateFlow<ResumoHistorico>
}

