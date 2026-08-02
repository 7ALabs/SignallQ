package io.signallq.app.core.featureflags

/**
 * Identificador tipado de uma flag do catalogo (issue #1477, Epico #1347).
 *
 * Nunca construir a partir de uma string solta em codigo de feature -- usar sempre
 * uma constante de [FeatureFlagKeys]. O valor de [id] segue o modelo obrigatorio
 * `consumer.{modulo}.{funcionalidade}.{controle}` (ou `shared.*`/`app.*` para nucleos
 * compartilhados), documentado no corpo do Epico #1347.
 */
@JvmInline
value class FeatureFlagKey(
    val id: String,
) {
    override fun toString(): String = id
}
