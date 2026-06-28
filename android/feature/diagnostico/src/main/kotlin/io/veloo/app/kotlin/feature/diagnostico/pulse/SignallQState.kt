package io.signallq.app.feature.diagnostico.pulse

enum class SignallQState {
    Idle,
    Collecting,
    Thinking,
    Analyzing,
    AwaitingInput,
    Success,
    Warning,
    Critical,
}

@Deprecated("Use SignallQState", ReplaceWith("SignallQState"))
typealias PulseState = SignallQState
