package io.signallq.app.core.featureflags

import org.json.JSONObject

/**
 * Parser puro do catalogo JSON canonico (Epico #1347) -- sem dependencia de
 * Android/Firebase, testavel em JUnit puro. Falha alto (excecao) em qualquer
 * campo obrigatorio ausente ou tipo divergente: catalogo invalido nunca deve
 * degradar silenciosamente para uma lista de flags incompleta.
 */
object FeatureFlagCatalogParser {
    fun parse(json: String): List<FeatureFlagDefinition> {
        val root =
            runCatching { JSONObject(json) }
                .getOrElse { throw FeatureFlagCatalogParseException("Catalogo de feature flags nao e um JSON valido", it) }

        if (root.optString("schemaVersion", "").isBlank()) {
            throw FeatureFlagCatalogParseException("Catalogo de feature flags sem \"schemaVersion\"")
        }

        val flagsArray =
            root.optJSONArray("flags")
                ?: throw FeatureFlagCatalogParseException("Catalogo de feature flags sem array \"flags\"")

        val definitions =
            (0 until flagsArray.length()).map { index ->
                val entry =
                    flagsArray.optJSONObject(index)
                        ?: throw FeatureFlagCatalogParseException("Entrada invalida em flags[$index] -- nao e um objeto JSON")
                parseDefinition(entry, index)
            }

        val duplicadas = definitions.groupBy { it.key }.filterValues { it.size > 1 }.keys
        if (duplicadas.isNotEmpty()) {
            throw FeatureFlagCatalogParseException("Chaves duplicadas no catalogo: ${duplicadas.joinToString { it.id }}")
        }

        return definitions
    }

    private fun parseDefinition(
        entry: JSONObject,
        index: Int,
    ): FeatureFlagDefinition {
        fun fail(campo: String): Nothing =
            throw FeatureFlagCatalogParseException(
                "Entrada flags[$index] invalida -- campo obrigatorio ausente/invalido: \"$campo\"",
            )

        val keyId = entry.optString("key", "").ifBlank { fail("key") }
        val module = entry.optString("module", "").ifBlank { fail("module") }
        val type = runCatching { FeatureFlagType.valueOf(entry.optString("type", "")) }.getOrElse { fail("type") }
        if (!entry.has("defaultValue") || entry.isNull("defaultValue")) fail("defaultValue")
        val defaultValue = parseRawValue(entry.get("defaultValue"), type, index)
        val criticality =
            runCatching { FeatureFlagCriticality.valueOf(entry.optString("criticality", "")) }.getOrElse { fail("criticality") }
        val owner = entry.optString("owner", "").ifBlank { fail("owner") }
        val description = entry.optString("description", "").ifBlank { fail("description") }
        val disabledBehavior =
            runCatching {
                FeatureFlagDisabledBehavior.valueOf(entry.optString("disabledBehavior", ""))
            }.getOrElse { fail("disabledBehavior") }
        val disabledMessage = optionalString(entry, "disabledMessage")
        if (!entry.has("dependencies")) fail("dependencies")
        val dependencies = parseDependencies(entry, index)
        if (!entry.has("androidImplemented")) fail("androidImplemented")
        if (!entry.has("adminManaged")) fail("adminManaged")

        return FeatureFlagDefinition(
            key = FeatureFlagKey(keyId),
            module = module,
            type = type,
            defaultValue = defaultValue,
            criticality = criticality,
            owner = owner,
            description = description,
            disabledBehavior = disabledBehavior,
            disabledMessage = disabledMessage,
            dependencies = dependencies,
            androidImplemented = entry.optBoolean("androidImplemented"),
            adminManaged = entry.optBoolean("adminManaged"),
            analyticsEvent = optionalString(entry, "analyticsEvent"),
        )
    }

    private fun parseDependencies(
        entry: JSONObject,
        index: Int,
    ): List<FeatureFlagKey> {
        val dependenciesArray =
            entry.optJSONArray("dependencies")
                ?: throw FeatureFlagCatalogParseException(
                    "Entrada flags[$index] invalida -- campo obrigatorio ausente/invalido: \"dependencies\"",
                )
        return (0 until dependenciesArray.length()).map { i ->
            val depId = dependenciesArray.optString(i, "")
            if (depId.isBlank()) {
                throw FeatureFlagCatalogParseException("Entrada flags[$index]: dependencies[$i] invalida")
            }
            FeatureFlagKey(depId)
        }
    }

    private fun optionalString(
        entry: JSONObject,
        campo: String,
    ): String? {
        if (!entry.has(campo) || entry.isNull(campo)) return null
        return entry.optString(campo).ifBlank { null }
    }

    private fun parseRawValue(
        raw: Any,
        type: FeatureFlagType,
        index: Int,
    ): FeatureFlagRawValue =
        try {
            when (type) {
                FeatureFlagType.BOOLEAN -> FeatureFlagRawValue.BooleanValue(raw as Boolean)
                FeatureFlagType.STRING -> FeatureFlagRawValue.StringValue(raw as String)
                FeatureFlagType.LONG -> FeatureFlagRawValue.LongValue((raw as Number).toLong())
                FeatureFlagType.DOUBLE -> FeatureFlagRawValue.DoubleValue((raw as Number).toDouble())
            }
        } catch (e: ClassCastException) {
            throw FeatureFlagCatalogParseException("Entrada flags[$index]: \"defaultValue\" nao corresponde ao \"type\"=$type", e)
        }
}
