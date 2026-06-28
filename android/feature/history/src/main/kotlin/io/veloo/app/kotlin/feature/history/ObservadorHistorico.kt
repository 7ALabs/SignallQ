package io.signallq.app.feature.history

import kotlinx.coroutines.flow.StateFlow

interface ObservadorHistorico {
    val resumoFlow: StateFlow<ResumoHistorico>
}

