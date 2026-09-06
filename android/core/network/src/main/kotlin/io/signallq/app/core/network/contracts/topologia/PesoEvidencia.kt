package io.signallq.app.core.network.contracts.topologia

/**
 * Força qualitativa de uma [Evidencia] — ex.: OUI de hardware costuma ser [FORTE], correlação só
 * por prefixo de fabricante costuma ser [FRACO]. A regra de negócio que atribui o peso pertence
 * ao motor (Fase 1/2A), não a este contrato.
 */
enum class PesoEvidencia {
    FORTE,
    MEDIO,
    FRACO,
}
