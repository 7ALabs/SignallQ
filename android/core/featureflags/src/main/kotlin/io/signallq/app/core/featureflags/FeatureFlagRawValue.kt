package io.signallq.app.core.featureflags

/**
 * Valor tipado de uma flag, sem contexto de origem/chave -- usado tanto para o
 * `defaultValue` do catalogo ([FeatureFlagDefinition]) quanto embutido em
 * [FeatureFlagValue] (valor efetivo em runtime, com [FeatureFlagSource]).
 */
sealed interface FeatureFlagRawValue {
    val type: FeatureFlagType

    data class BooleanValue(
        val value: Boolean,
    ) : FeatureFlagRawValue {
        override val type: FeatureFlagType = FeatureFlagType.BOOLEAN
    }

    data class StringValue(
        val value: String,
    ) : FeatureFlagRawValue {
        override val type: FeatureFlagType = FeatureFlagType.STRING
    }

    data class LongValue(
        val value: Long,
    ) : FeatureFlagRawValue {
        override val type: FeatureFlagType = FeatureFlagType.LONG
    }

    data class DoubleValue(
        val value: Double,
    ) : FeatureFlagRawValue {
        override val type: FeatureFlagType = FeatureFlagType.DOUBLE
    }

    fun asBooleanOrNull(): Boolean? = (this as? BooleanValue)?.value

    fun asStringOrNull(): String? = (this as? StringValue)?.value

    fun asLongOrNull(): Long? = (this as? LongValue)?.value

    fun asDoubleOrNull(): Double? = (this as? DoubleValue)?.value

    /** Valor nativo Kotlin/Java equivalente -- usado para alimentar `setDefaultsAsync`
     *  do Firebase Remote Config, que espera `Map<String, Any>`. */
    fun toNativeValue(): Any =
        when (this) {
            is BooleanValue -> value
            is StringValue -> value
            is LongValue -> value
            is DoubleValue -> value
        }
}
