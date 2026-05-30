package io.veloo.app.feature.diagnostico.pulse

enum class OrbitState {
    Idle,
    Collecting,
    Thinking,
    Analyzing,
    AwaitingInput,
    Success,
    Warning,
    Critical,
}

@Deprecated("Use OrbitState", ReplaceWith("OrbitState"))
typealias PulseState = OrbitState
