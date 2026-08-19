package io.signallq.app.ui.component

import io.signallq.app.core.diagnostico.DiagnosticStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Teste de caracterização — issue #1756 (NDS-02f), escrito ANTES de `LocalDeviceSection.kt` trocar
 * a chamada direta a `FibraSignalQualityEngine.avaliar()` (dentro de `resumoInterpretado()`) pelo
 * seam [classificarFibraLocal]. Cada cenário reproduz manualmente a lógica que existia embutida em
 * `resumoInterpretado()` antes desta fatia (link inativo -> resposta fixa; link ativo -> pior achado
 * do motor por severidade; nenhum achado -> resposta "conectada, sem outros dados") e confirma que
 * o seam produz exatamente o mesmo resultado — comportamento idêntico, só a fonte da chamada muda
 * de lugar (mesmo padrão de `ClassificacaoMetricaLocalTest`/`DecisaoDiagnosticoLocalTest`, ver KDoc
 * de `ResumoFibraLocal.kt` para o racional completo).
 */
class ResumoFibraLocalTest {
    @Test
    fun `link inativo devolve resumo critico fixo, sem consultar rx-tx-temperatura`() {
        val resumo =
            classificarFibraLocal(
                linkAtivo = false,
                // Valores que, se o motor fosse consultado, dariam "bom" — provam que o
                // early-return de link inativo nunca deixa o motor rodar.
                rxPowerDbm = -10.0,
                txPowerDbm = 2.0,
                temperatureCelsius = 40.0,
            )
        assertEquals("A fibra está sem sinal", resumo.titulo)
        assertEquals("A fibra está sem sinal da operadora.", resumo.mensagem)
        assertEquals(DiagnosticStatus.critical, resumo.status)
    }

    @Test
    fun `link ativo com todas as leituras boas devolve resumo ok do proprio motor`() {
        val resumo =
            classificarFibraLocal(
                linkAtivo = true,
                rxPowerDbm = -19.8,
                txPowerDbm = 2.1,
                temperatureCelsius = 45.0,
            )
        // rx/tx/temp todos "boa" -> três achados *-OK com severidade 0 cada; maxByOrNull
        // fica com o PRIMEIRO em caso de empate (ordem de inserção do motor: rx, tx, temp).
        assertEquals("O sinal recebido da fibra está bom", resumo.titulo)
        assertEquals(DiagnosticStatus.ok, resumo.status)
    }

    @Test
    fun `link ativo com rx critico entre leituras mistas devolve o pior achado por severidade`() {
        val resumo =
            classificarFibraLocal(
                linkAtivo = true,
                rxPowerDbm = -30.0, // ruim -> critical (severidade 3)
                txPowerDbm = 2.1, // boa -> ok (severidade 0)
                temperatureCelsius = 70.0, // regular -> attention (severidade 2)
            )
        assertEquals("O sinal recebido da fibra está muito fraco", resumo.titulo)
        assertEquals(DiagnosticStatus.critical, resumo.status)
    }

    @Test
    fun `tx acima da faixa boa cai em regular e pode vencer a comparacao de severidade`() {
        // ClassificadorSaudeGpon.classificarTx só marca "boa" no intervalo fechado [0.5, 5.0] —
        // 6.0 fica fora da faixa "boa" (regular, por ainda ser >= -1.0), então o ramo FIB-03-ALTO
        // (dentro do bloco "boa" do motor, para tx > 5.0) é hoje inalcançável por este call site;
        // caracteriza o comportamento real, não o que o comentário do motor sugeriria.
        val resumo =
            classificarFibraLocal(
                linkAtivo = true,
                rxPowerDbm = -19.8, // boa -> ok
                txPowerDbm = 6.0, // fora de [0.5, 5.0] -> regular -> attention (FIB-03b)
                temperatureCelsius = 45.0, // boa -> ok
            )
        assertEquals("O sinal enviado pela fibra está fraco", resumo.titulo)
        assertEquals(DiagnosticStatus.attention, resumo.status)
    }

    @Test
    fun `link ativo sem nenhuma leitura numerica devolve resumo generico de conectado`() {
        val resumo =
            classificarFibraLocal(
                linkAtivo = true,
                rxPowerDbm = null,
                txPowerDbm = null,
                temperatureCelsius = null,
            )
        assertEquals("A fibra está conectada", resumo.titulo)
        assertEquals("A fibra está conectada, mas não consegui ler outros dados agora.", resumo.mensagem)
        assertEquals(DiagnosticStatus.ok, resumo.status)
    }

    @Test
    fun `linkAtivo nulo e tratado como ativo, igual ao comportamento anterior da tela`() {
        val resumoComNull =
            classificarFibraLocal(
                linkAtivo = null,
                rxPowerDbm = -30.0,
                txPowerDbm = 2.1,
                temperatureCelsius = 45.0,
            )
        val resumoComTrue =
            classificarFibraLocal(
                linkAtivo = true,
                rxPowerDbm = -30.0,
                txPowerDbm = 2.1,
                temperatureCelsius = 45.0,
            )
        assertEquals(resumoComTrue, resumoComNull)
    }

    @Test
    fun `rx sentinela 0,0 e ignorado pelo motor, igual ao comportamento anterior`() {
        // Caracterizado antes em FibraSignalQualityEngineZeroSentinelCharacterizationTest
        // (core/diagnostico) — aqui só confirma que o seam não esconde nem corrige essa
        // divergência conhecida (P0-7, auditoria #1228), preserva tal qual.
        val resumo =
            classificarFibraLocal(
                linkAtivo = true,
                rxPowerDbm = 0.0,
                txPowerDbm = 2.1,
                temperatureCelsius = 45.0,
            )
        assertEquals(DiagnosticStatus.ok, resumo.status)
        assertEquals("O sinal enviado pela fibra está bom", resumo.titulo)
    }
}
