package io.signallq.app.core.featureflags

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagCatalogParserTest {
    @Test
    fun `parseia catalogo valido com todos os campos`() {
        val json =
            """
            {
              "schemaVersion": "1.0",
              "flags": [
                {
                  "key": "consumer_speedtest_enabled",
                  "module": ":featureSpeedtest",
                  "type": "BOOLEAN",
                  "defaultValue": true,
                  "criticality": "HIGH",
                  "owner": "product-signallq",
                  "description": "Controla acesso e execucao do SpeedTest.",
                  "disabledBehavior": "HIDE_ENTRY_AND_BLOCK_ROUTE",
                  "disabledMessage": "Recurso temporariamente indisponivel.",
                  "dependencies": [],
                  "androidImplemented": false,
                  "adminManaged": true,
                  "analyticsEvent": "feature_blocked_remote"
                }
              ]
            }
            """.trimIndent()

        val definicoes = FeatureFlagCatalogParser.parse(json)

        assertEquals(1, definicoes.size)
        val definicao = definicoes.single()
        assertEquals(FeatureFlagKey("consumer_speedtest_enabled"), definicao.key)
        assertEquals(":featureSpeedtest", definicao.module)
        assertEquals(FeatureFlagType.BOOLEAN, definicao.type)
        assertEquals(FeatureFlagRawValue.BooleanValue(true), definicao.defaultValue)
        assertEquals(FeatureFlagCriticality.HIGH, definicao.criticality)
        assertEquals("product-signallq", definicao.owner)
        assertEquals(FeatureFlagDisabledBehavior.HIDE_ENTRY_AND_BLOCK_ROUTE, definicao.disabledBehavior)
        assertEquals("Recurso temporariamente indisponivel.", definicao.disabledMessage)
        assertTrue(definicao.dependencies.isEmpty())
        assertEquals(false, definicao.androidImplemented)
        assertEquals(true, definicao.adminManaged)
        assertEquals("feature_blocked_remote", definicao.analyticsEvent)
    }

    @Test
    fun `campos opcionais nulos viram null, nao string vazia`() {
        val json =
            """
            {
              "schemaVersion": "1.0",
              "flags": [
                {
                  "key": "consumer_speedtest_cloudflare_engine_enabled",
                  "module": ":featureSpeedtest",
                  "type": "BOOLEAN",
                  "defaultValue": true,
                  "criticality": "MEDIUM",
                  "owner": "product-signallq",
                  "description": "desc",
                  "disabledBehavior": "SILENT_NO_OP",
                  "disabledMessage": null,
                  "dependencies": ["consumer_speedtest_enabled"],
                  "androidImplemented": false,
                  "adminManaged": true,
                  "analyticsEvent": null
                }
              ]
            }
            """.trimIndent()

        val definicao = FeatureFlagCatalogParser.parse(json).single()

        assertNull(definicao.disabledMessage)
        assertNull(definicao.analyticsEvent)
        assertEquals(listOf(FeatureFlagKey("consumer_speedtest_enabled")), definicao.dependencies)
    }

    @Test
    fun `catalogo sem schemaVersion lanca excecao`() {
        val json = """{"flags": []}"""

        assertThrows(FeatureFlagCatalogParseException::class.java) {
            FeatureFlagCatalogParser.parse(json)
        }
    }

    @Test
    fun `catalogo sem array flags lanca excecao`() {
        val json = """{"schemaVersion": "1.0"}"""

        assertThrows(FeatureFlagCatalogParseException::class.java) {
            FeatureFlagCatalogParser.parse(json)
        }
    }

    @Test
    fun `entrada sem campo obrigatorio lanca excecao`() {
        val json =
            """
            {
              "schemaVersion": "1.0",
              "flags": [
                { "key": "consumer_speedtest_enabled" }
              ]
            }
            """.trimIndent()

        assertThrows(FeatureFlagCatalogParseException::class.java) {
            FeatureFlagCatalogParser.parse(json)
        }
    }

    @Test
    fun `defaultValue com tipo divergente do campo type lanca excecao`() {
        val json =
            """
            {
              "schemaVersion": "1.0",
              "flags": [
                {
                  "key": "consumer_speedtest_enabled",
                  "module": ":featureSpeedtest",
                  "type": "BOOLEAN",
                  "defaultValue": "nao-e-booleano",
                  "criticality": "HIGH",
                  "owner": "product-signallq",
                  "description": "desc",
                  "disabledBehavior": "HIDE_ENTRY_AND_BLOCK_ROUTE",
                  "dependencies": [],
                  "androidImplemented": false,
                  "adminManaged": true
                }
              ]
            }
            """.trimIndent()

        assertThrows(FeatureFlagCatalogParseException::class.java) {
            FeatureFlagCatalogParser.parse(json)
        }
    }

    @Test
    fun `chaves duplicadas no catalogo lanca excecao`() {
        val entrada = { key: String ->
            """
            {
              "key": "$key",
              "module": ":featureSpeedtest",
              "type": "BOOLEAN",
              "defaultValue": true,
              "criticality": "HIGH",
              "owner": "product-signallq",
              "description": "desc",
              "disabledBehavior": "HIDE_ENTRY_AND_BLOCK_ROUTE",
              "dependencies": [],
              "androidImplemented": false,
              "adminManaged": true
            }
            """.trimIndent()
        }
        val json =
            """
            {
              "schemaVersion": "1.0",
              "flags": [${entrada("consumer_speedtest_enabled")}, ${entrada("consumer_speedtest_enabled")}]
            }
            """.trimIndent()

        assertThrows(FeatureFlagCatalogParseException::class.java) {
            FeatureFlagCatalogParser.parse(json)
        }
    }

    @Test
    fun `tipos LONG e DOUBLE sao parseados corretamente`() {
        val json =
            """
            {
              "schemaVersion": "1.0",
              "flags": [
                {
                  "key": "app.minimum_supported_version",
                  "module": ":app",
                  "type": "LONG",
                  "defaultValue": 70,
                  "criticality": "CRITICAL",
                  "owner": "product-signallq",
                  "description": "desc",
                  "disabledBehavior": "SILENT_NO_OP",
                  "dependencies": [],
                  "androidImplemented": false,
                  "adminManaged": true
                },
                {
                  "key": "shared.recommendation.min_score",
                  "module": ":coreRecommendation",
                  "type": "DOUBLE",
                  "defaultValue": 0.5,
                  "criticality": "LOW",
                  "owner": "product-signallq",
                  "description": "desc",
                  "disabledBehavior": "SILENT_NO_OP",
                  "dependencies": [],
                  "androidImplemented": false,
                  "adminManaged": true
                }
              ]
            }
            """.trimIndent()

        val definicoes = FeatureFlagCatalogParser.parse(json)

        assertEquals(FeatureFlagRawValue.LongValue(70L), definicoes[0].defaultValue)
        assertEquals(FeatureFlagRawValue.DoubleValue(0.5), definicoes[1].defaultValue)
    }
}
