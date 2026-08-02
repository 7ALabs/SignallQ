package io.signallq.app.core.diagnostico

import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus

private const val CAT = "fibra"

object FibraSignalQualityEngine {

    fun avaliar(input: FibraDiagnosticInput?): List<DiagnosticResult> {
        if (input == null) return emptyList()

        if (!input.isUp) {
            return listOf(
                DiagnosticResult(
                    id = "FIB-01",
                    titulo = "A fibra está sem sinal",
                    status = DiagnosticStatus.critical,
                    evidencia = "gpon=down",
                    mensagemUsuario = "A conexão de fibra óptica está inativa. Sem sinal do equipamento da operadora.",
                    recomendacao = "Verifique se o cabo de fibra está bem conectado no modem de fibra. Se o problema persistir, contate a operadora.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
            )
        }

        val resultados = mutableListOf<DiagnosticResult>()

        // RX Power — ITU-T G.984: boa ≥ -23 dBm | regular [-27, -23) | ruim < -27 dBm
        val rx = input.rxPowerDbm
        if (rx != null && rx != 0.0) {
            when (ClassificadorSaudeGpon.classificarRx(rx)) {
                GponSaudeStatus.ruim -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-02",
                        titulo = "O sinal recebido da fibra está muito fraco",
                        status = DiagnosticStatus.critical,
                        evidencia = "rx=${"%.2f".format(rx)} dBm",
                        mensagemUsuario = "O sinal de recepção da fibra está muito fraco (${"%.2f".format(rx)} dBm). Abaixo de -27 dBm o link pode cair.",
                        recomendacao = "Verifique se o cabo de fibra está dobrado ou danificado. Contate a operadora para verificar a potência no equipamento da operadora.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                GponSaudeStatus.regular -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-02b",
                        titulo = "O sinal recebido da fibra está fraco",
                        status = DiagnosticStatus.attention,
                        evidencia = "rx=${"%.2f".format(rx)} dBm",
                        mensagemUsuario = "O sinal de recepção da fibra está abaixo do ideal (${"%.2f".format(rx)} dBm). Pode indicar desgaste ou dobra no cabo.",
                        recomendacao = "Informe o valor à operadora em caso de instabilidade recorrente.",
                        categoria = CAT,
                    ),
                )
                GponSaudeStatus.boa -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-02-OK",
                        titulo = "O sinal recebido da fibra está bom",
                        status = DiagnosticStatus.ok,
                        evidencia = "rx=${"%.2f".format(rx)} dBm",
                        mensagemUsuario = "O sinal de recepção da fibra está dentro da faixa ideal (${"%.2f".format(rx)} dBm).",
                        recomendacao = null,
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
            }
        }

        // TX Power — ITU-T G.984: boa [+0.5, +5] dBm | regular [-1, +0.5) | ruim < -1 dBm
        val tx = input.txPowerDbm
        if (tx != null && tx != 0.0) {
            when (ClassificadorSaudeGpon.classificarTx(tx)) {
                GponSaudeStatus.ruim -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-03",
                        titulo = "O sinal enviado pela fibra está muito fraco",
                        status = DiagnosticStatus.critical,
                        evidencia = "tx=${"%.2f".format(tx)} dBm",
                        mensagemUsuario = "A potência de transmissão do laser do modem de fibra está muito baixa (${"%.2f".format(tx)} dBm). O equipamento pode estar com defeito.",
                        recomendacao = "Reinicie o modem de fibra. Se o problema persistir, o equipamento pode precisar de substituição pela operadora.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                GponSaudeStatus.regular -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-03b",
                        titulo = "O sinal enviado pela fibra está fraco",
                        status = DiagnosticStatus.attention,
                        evidencia = "tx=${"%.2f".format(tx)} dBm",
                        mensagemUsuario = "A potência de transmissão está abaixo do ideal (${"%.2f".format(tx)} dBm). Pode indicar desgaste do laser.",
                        recomendacao = "Monitore. Se houver quedas frequentes, solicite visita técnica à operadora.",
                        categoria = CAT,
                    ),
                )
                GponSaudeStatus.boa -> {
                    // Mantemos o comportamento anterior: caso esteja muito acima do esperado, vira atencao.
                    if (tx > 5.0) {
                        resultados.add(
                            DiagnosticResult(
                                id = "FIB-03-ALTO",
                                titulo = "O sinal enviado pela fibra está acima do esperado",
                                status = DiagnosticStatus.attention,
                                evidencia = "tx=${"%.2f".format(tx)} dBm",
                                mensagemUsuario = "A potência de transmissão está acima do esperado (${"%.2f".format(tx)} dBm). Pode causar saturação no receptor do equipamento da operadora.",
                                recomendacao = "Informe a operadora para verificação da configuração do equipamento da operadora.",
                                categoria = CAT,
                            ),
                        )
                    } else {
                        resultados.add(
                            DiagnosticResult(
                                id = "FIB-03-OK",
                                titulo = "O sinal enviado pela fibra está bom",
                                status = DiagnosticStatus.ok,
                                evidencia = "tx=${"%.2f".format(tx)} dBm",
                                mensagemUsuario = "A potência de transmissão do modem de fibra está na faixa ideal (${"%.2f".format(tx)} dBm).",
                                recomendacao = null,
                                categoria = CAT,
                                podeConcluir = true,
                            ),
                        )
                    }
                }
            }
        }

        // Temperatura — ITU-T G.984: boa < 65 °C | regular [65, 75] | ruim > 75 °C
        val temp = input.temperatureCelsius
        if (temp != null && temp != 0.0) {
            when (ClassificadorSaudeGpon.classificarTemp(temp)) {
                GponSaudeStatus.ruim -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-04",
                        titulo = "O modem de fibra está muito quente",
                        status = DiagnosticStatus.critical,
                        evidencia = "temp=${"%.1f".format(temp)} °C",
                        mensagemUsuario = "O modem de fibra está com temperatura muito alta (${"%.1f".format(temp)} °C). Risco de desligamento por proteção térmica.",
                        recomendacao = "Melhore a ventilação ao redor do modem de fibra. Certifique-se de que ele não está coberto ou próximo a fontes de calor.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                GponSaudeStatus.regular -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-04b",
                        titulo = "O modem de fibra está quente",
                        status = DiagnosticStatus.attention,
                        evidencia = "temp=${"%.1f".format(temp)} °C",
                        mensagemUsuario = "O modem de fibra está aquecido (${"%.1f".format(temp)} °C). Temperaturas acima de 65 °C reduzem a vida útil do equipamento.",
                        recomendacao = "Verifique se há ventilação adequada ao redor do modem de fibra.",
                        categoria = CAT,
                    ),
                )
                GponSaudeStatus.boa -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-04-OK",
                        titulo = "A temperatura do modem de fibra está normal",
                        status = DiagnosticStatus.ok,
                        evidencia = "temp=${"%.1f".format(temp)} °C",
                        mensagemUsuario = "A temperatura do modem de fibra está normal (${"%.1f".format(temp)} °C).",
                        recomendacao = null,
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
            }
        }

        return resultados
    }
}
