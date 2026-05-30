package io.veloo.app.feature.history

import kotlinx.coroutines.flow.StateFlow

interface ObservadorHistorico {
    val resumoFlow: StateFlow<ResumoHistorico>
}

