package io.signallq.app.feature.fibra

data class PppStatus(
    val isConnected: Boolean,
    val connectionStatus: String,
    val connectionType: String,
    val name: String,
    val lastError: String,
)
