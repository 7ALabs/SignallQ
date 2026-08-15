package io.signallq.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Test

/** GH#1227 item 14/RF-I — valor não reconhecido sempre resolve pra uma opção válida (SYSTEM),
 *  nunca "nenhuma selecionada". */
class ThemePreferenceTest {

    @Test
    fun `parse reconhece as tres chaves persistidas`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.parse("sistema"))
        assertEquals(ThemePreference.LIGHT, ThemePreference.parse("claro"))
        assertEquals(ThemePreference.DARK, ThemePreference.parse("escuro"))
    }

    @Test
    fun `parse ignora case`() {
        assertEquals(ThemePreference.LIGHT, ThemePreference.parse("CLARO"))
    }

    @Test
    fun `valor nulo, vazio ou desconhecido sempre resolve para SYSTEM`() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.parse(null))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.parse(""))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.parse("valor-corrompido-xyz"))
    }

    @Test
    fun `chaveDataStore preserva os valores ja gravados em producao`() {
        assertEquals("sistema", ThemePreference.SYSTEM.chaveDataStore)
        assertEquals("claro", ThemePreference.LIGHT.chaveDataStore)
        assertEquals("escuro", ThemePreference.DARK.chaveDataStore)
    }
}
