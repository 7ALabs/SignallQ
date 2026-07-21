package io.signallq.app.feature.settings

/**
 * GH#1227 item 5/RF-D — os campos de velocidade contratada aceitavam qualquer número,
 * incluindo zero, valores absurdos e preenchimento parcial sem nenhuma validação. Esta função
 * pura decide se um valor é aceitável antes de persistir no [ConnectionProfile].
 *
 * Regras:
 * - `null` é sempre válido (campo não preenchido — estado explícito de "sem dado", não `0`).
 * - `0` ou negativo é inválido (planos residenciais reais nunca têm 0 Mbps contratado — um
 *   valor `0` salvo hoje significa "campo vazio", que é exatamente a ambiguidade que a issue
 *   pede pra eliminar).
 * - Acima de [LIMITE_SUPERIOR_MBPS] é tratado como absurdo (maior plano residencial/comercial
 *   real do mercado brasileiro em 2026 não chega perto disso — teto generoso de propósito,
 *   não uma tentativa de prever o plano mais rápido do mercado).
 */
object ValidadorVelocidadeContratada {
    const val LIMITE_SUPERIOR_MBPS = 10_000

    fun ehValida(mbps: Int?): Boolean {
        if (mbps == null) return true
        return mbps in 1..LIMITE_SUPERIOR_MBPS
    }
}
