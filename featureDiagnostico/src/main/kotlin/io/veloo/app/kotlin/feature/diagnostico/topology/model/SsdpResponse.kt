package io.veloo.app.feature.diagnostico.topology.model

data class SsdpResponse(
    val location: String,
    val usn: String?,
    val server: String?
)
