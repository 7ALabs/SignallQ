package io.signallq.app.feature.fibra

/**
 * GH#1213 (dispatch final) — perfil técnico versionado do Nokia G-1425G-B, GPON classe B+
 * (ITU-T G.984.2 Amendment 1). Faixas retiradas literalmente da issue #1213 (que por sua vez cita
 * o Product Guide do fabricante e a recomendação ITU-T vigente) — não inventar nem "arredondar"
 * estes valores sem atualizar a referência.
 *
 * As margens de atenção ([rxAtencaoInferiorDbm]/[rxAtencaoSuperiorDbm]) são heurística de produto
 * do SignallQ (2 dB para dentro da borda normativa), **não um limite novo da ITU-T** — mantidas
 * como campos separados do [source] justamente para não se confundirem com a faixa normativa.
 *
 * Vinculado à iniciativa guarda-chuva #1228 (Motor Canônico): este perfil é o candidato a ser
 * consumido pelo motor central quando #1228 avançar — por ora vive em `feature/fibra` (mesmo
 * módulo do driver Nokia) porque ainda não foi wireado ao `ClassificadorSaudeGpon` genérico de
 * `core/network` (mudança de maior risco, fora do escopo desta rodada — ver comentário da issue).
 */
data class NokiaG1425GBProfile(
    val technology: String = "GPON",
    val opticalClass: String = "B_PLUS",
    val rxMinOperationalDbm: Double = -27.0,
    val rxMaxOperationalDbm: Double = -8.0,
    val rxAtencaoInferiorDbm: Double = -25.0,
    val rxAtencaoSuperiorDbm: Double = -10.0,
    val txMinOperationalDbm: Double = 0.5,
    val txMaxOperationalDbm: Double = 5.0,
    val source: String = "ITU_T_G984_2_AMD1_CLASS_B_PLUS",
) {
    companion object {
        val PADRAO = NokiaG1425GBProfile()
    }
}

/** RX (downstream) — 4 estados, conforme tabela da issue #1213. */
enum class NivelSinalOpticoRx {
    /** `< rxMinOperationalDbm` ou `> rxMaxOperationalDbm` — fora da faixa da classe B+. */
    FORA_DE_ESPECIFICACAO,

    /** Dentro da faixa normativa, mas na margem de atenção (heurística de produto, não ITU-T). */
    PROXIMO_AO_LIMITE,

    /** Dentro da faixa normativa, com folga da heurística de margem. */
    DENTRO_DA_FAIXA_COM_MARGEM,

    /** `rxPowerDbm == null` — nunca classificar ausência de dado como falha. */
    NAO_INFORMADO,
}

/** TX (upstream) — 3 estados; a issue não define tier intermediário pra TX (sem evidência do transceptor). */
enum class NivelSinalOpticoTx {
    FORA_DE_ESPECIFICACAO,
    DENTRO_DA_FAIXA_NORMATIVA,
    NAO_INFORMADO,
}

/** Função pura, sem dependência de Android/HTTP — testável isoladamente. */
object ClassificadorOpticoNokiaG1425GB {

    fun classificarRx(
        rxPowerDbm: Double?,
        profile: NokiaG1425GBProfile = NokiaG1425GBProfile.PADRAO,
    ): NivelSinalOpticoRx {
        if (rxPowerDbm == null) return NivelSinalOpticoRx.NAO_INFORMADO
        return when {
            rxPowerDbm < profile.rxMinOperationalDbm || rxPowerDbm > profile.rxMaxOperationalDbm ->
                NivelSinalOpticoRx.FORA_DE_ESPECIFICACAO
            rxPowerDbm < profile.rxAtencaoInferiorDbm || rxPowerDbm > profile.rxAtencaoSuperiorDbm ->
                NivelSinalOpticoRx.PROXIMO_AO_LIMITE
            else -> NivelSinalOpticoRx.DENTRO_DA_FAIXA_COM_MARGEM
        }
    }

    fun classificarTx(
        txPowerDbm: Double?,
        profile: NokiaG1425GBProfile = NokiaG1425GBProfile.PADRAO,
    ): NivelSinalOpticoTx {
        if (txPowerDbm == null) return NivelSinalOpticoTx.NAO_INFORMADO
        return if (txPowerDbm < profile.txMinOperationalDbm || txPowerDbm > profile.txMaxOperationalDbm) {
            NivelSinalOpticoTx.FORA_DE_ESPECIFICACAO
        } else {
            NivelSinalOpticoTx.DENTRO_DA_FAIXA_NORMATIVA
        }
    }
}
