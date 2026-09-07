package io.signallq.app.core.diagnostico

/**
 * Evidência de velocidade que pode sustentar um veredito do Modo gamer.
 *
 * A camada que lê o speedtest/Room preenche esta estrutura com a identidade de rede capturada
 * pela medição. O motor gamer recebe somente [internet] depois que esta evidência for aceita;
 * não deve tentar deduzir idade ou rede a partir de [DiagnosticInput].
 */
data class MedicaoBaseModoGamer(
    val internet: InternetDiagnosticInput,
    val medidoEmEpochMs: Long,
    val networkId: String?,
    val integridade: IntegridadeMedicaoModoGamer,
)

/** Espelho tipado do estado de integridade persistido pelo speedtest, sem acoplar o domínio à feature. */
enum class IntegridadeMedicaoModoGamer {
    COMPLETA,
    PARCIAL,
    INCONCLUSIVA,
    CONTAMINADA,
    CANCELADA,
}

enum class MotivoMedicaoBaseInvalidaModoGamer {
    AUSENTE,
    INTEGRIDADE_INSUFICIENTE,
    REDE_ATUAL_DESCONHECIDA,
    REDE_DIFERENTE,
    EXPIRADA_OU_RELOGIO_INVALIDO,
}

sealed interface ElegibilidadeMedicaoBaseModoGamer {
    data class Elegivel(
        val medicao: MedicaoBaseModoGamer,
    ) : ElegibilidadeMedicaoBaseModoGamer

    data class RequerNovoTeste(
        val motivo: MotivoMedicaoBaseInvalidaModoGamer,
    ) : ElegibilidadeMedicaoBaseModoGamer
}

private const val VALIDADE_MEDICAO_BASE_MODO_GAMER_MS = 15 * 60 * 1_000L

/**
 * Tempo até a próxima revalidação de uma base gamer. O chamador só deve usar este valor depois
 * de [avaliarElegibilidadeMedicaoBaseModoGamer] retornar [ElegibilidadeMedicaoBaseModoGamer.Elegivel].
 */
fun atrasoAteRevalidarMedicaoBaseModoGamer(
    medicao: MedicaoBaseModoGamer,
    agoraEpochMs: Long,
): Long =
    (VALIDADE_MEDICAO_BASE_MODO_GAMER_MS - (agoraEpochMs - medicao.medidoEmEpochMs)).coerceAtLeast(0L) + 1L

/**
 * Fonte única da regra de reaproveitamento da velocidade no Modo gamer.
 *
 * Medição histórica é aceita somente de modo explícito, quando ainda é recente, completa e foi
 * coletada na mesma rede identificada agora. Uma identidade ausente nunca é tratada como igual.
 */
fun avaliarElegibilidadeMedicaoBaseModoGamer(
    medicao: MedicaoBaseModoGamer?,
    networkIdAtual: String?,
    agoraEpochMs: Long,
): ElegibilidadeMedicaoBaseModoGamer {
    if (medicao == null) {
        return ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste(MotivoMedicaoBaseInvalidaModoGamer.AUSENTE)
    }
    if (medicao.integridade != IntegridadeMedicaoModoGamer.COMPLETA) {
        return ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste(MotivoMedicaoBaseInvalidaModoGamer.INTEGRIDADE_INSUFICIENTE)
    }
    if (networkIdAtual.isNullOrBlank() || medicao.networkId.isNullOrBlank()) {
        return ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste(MotivoMedicaoBaseInvalidaModoGamer.REDE_ATUAL_DESCONHECIDA)
    }
    if (medicao.networkId != networkIdAtual) {
        return ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste(MotivoMedicaoBaseInvalidaModoGamer.REDE_DIFERENTE)
    }
    val idadeMs = agoraEpochMs - medicao.medidoEmEpochMs
    if (idadeMs !in 0..VALIDADE_MEDICAO_BASE_MODO_GAMER_MS) {
        return ElegibilidadeMedicaoBaseModoGamer.RequerNovoTeste(MotivoMedicaoBaseInvalidaModoGamer.EXPIRADA_OU_RELOGIO_INVALIDO)
    }
    return ElegibilidadeMedicaoBaseModoGamer.Elegivel(medicao)
}

/** Ping da rota do jogo, válido apenas para a tentativa gamer que o produziu. */
data class MedicaoPingEspecificoModoGamer(
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val medidoEmEpochMs: Long,
    val networkId: String?,
    val tentativaId: String,
)

enum class MotivoPingEspecificoInvalidoModoGamer {
    AUSENTE,
    TENTATIVA_DIFERENTE,
    REDE_ATUAL_DESCONHECIDA,
    REDE_DIFERENTE,
    EXPIRADO_OU_RELOGIO_INVALIDO,
}

sealed interface ElegibilidadePingEspecificoModoGamer {
    data class Elegivel(
        val medicao: MedicaoPingEspecificoModoGamer,
    ) : ElegibilidadePingEspecificoModoGamer

    data class NaoElegivel(
        val motivo: MotivoPingEspecificoInvalidoModoGamer,
    ) : ElegibilidadePingEspecificoModoGamer
}

private const val VALIDADE_PING_ESPECIFICO_MODO_GAMER_MS = 2 * 60 * 1_000L

/**
 * Impede o reuso de um ping de rota fora da tentativa atual, após dois minutos ou após mudança
 * de rede. O chamador deve medir novamente quando o retorno não for [ElegibilidadePingEspecificoModoGamer.Elegivel].
 */
fun avaliarElegibilidadePingEspecificoModoGamer(
    medicao: MedicaoPingEspecificoModoGamer?,
    tentativaAtualId: String,
    networkIdAtual: String?,
    agoraEpochMs: Long,
): ElegibilidadePingEspecificoModoGamer {
    if (medicao == null) {
        return ElegibilidadePingEspecificoModoGamer.NaoElegivel(MotivoPingEspecificoInvalidoModoGamer.AUSENTE)
    }
    if (medicao.tentativaId != tentativaAtualId) {
        return ElegibilidadePingEspecificoModoGamer.NaoElegivel(MotivoPingEspecificoInvalidoModoGamer.TENTATIVA_DIFERENTE)
    }
    if (networkIdAtual.isNullOrBlank() || medicao.networkId.isNullOrBlank()) {
        return ElegibilidadePingEspecificoModoGamer.NaoElegivel(MotivoPingEspecificoInvalidoModoGamer.REDE_ATUAL_DESCONHECIDA)
    }
    if (medicao.networkId != networkIdAtual) {
        return ElegibilidadePingEspecificoModoGamer.NaoElegivel(MotivoPingEspecificoInvalidoModoGamer.REDE_DIFERENTE)
    }
    val idadeMs = agoraEpochMs - medicao.medidoEmEpochMs
    if (idadeMs !in 0..VALIDADE_PING_ESPECIFICO_MODO_GAMER_MS) {
        return ElegibilidadePingEspecificoModoGamer.NaoElegivel(MotivoPingEspecificoInvalidoModoGamer.EXPIRADO_OU_RELOGIO_INVALIDO)
    }
    return ElegibilidadePingEspecificoModoGamer.Elegivel(medicao)
}
