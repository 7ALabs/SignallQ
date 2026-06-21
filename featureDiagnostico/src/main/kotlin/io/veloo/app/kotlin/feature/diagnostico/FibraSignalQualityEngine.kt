package io.veloo.app.feature.diagnostico

import io.veloo.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.veloo.app.core.network.contracts.fibra.GponSaudeStatus

private const val CAT = "fibra"

object FibraSignalQualityEngine {

    fun avaliar(input: FibraDiagnosticInput?): List<DiagnosticResult> {
        if (input == null) return emptyList()

        if (!input.isUp) {
            return listOf(
                DiagnosticResult(
                    id = "FIB-01",
                    titulo = "Fibra Desconectada",
                    status = DiagnosticStatus.critical,
                    evidencia = "gpon=down",
                    mensagemUsuario = "A conexão de fibra óptica está inativa. Sem sinal da OLT da operadora.",
                    recomendacao = "Verifique se o cabo de fibra está bem conectado na ONT. Se o problema persistir, contate o provedor.",
                    categoria = CAT,
                    podeConcluir = true,
                ),
            )
        }

        val resultados = mutableListOf<DiagnosticResult>()

        // RX Power — ITU-T G.984: boa ≥ -23 dBm | regular [-27, -23) | ruim < -27 dBm
        val rx = input.rxPowerDbm
        if (rx != 0.0) {
            when (ClassificadorSaudeGpon.classificarRx(rx)) {
                GponSaudeStatus.ruim -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-02",
                        titulo = "Sinal de Recepção Ruim",
                        status = DiagnosticStatus.critical,
                        evidencia = "rx=${"%.2f".format(rx)} dBm",
                        mensagemUsuario = "O sinal de recepção da fibra está muito fraco (${"%.2f".format(rx)} dBm). Abaixo de -27 dBm o link pode cair.",
                        recomendacao = "Verifique se o cabo de fibra está dobrado ou danificado. Contate o provedor para verificar a potência na OLT.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                GponSaudeStatus.regular -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-02b",
                        titulo = "Sinal de Recepção Fraco",
                        status = DiagnosticStatus.attention,
                        evidencia = "rx=${"%.2f".format(rx)} dBm",
                        mensagemUsuario = "O sinal de recepção da fibra está abaixo do ideal (${"%.2f".format(rx)} dBm). Pode indicar desgaste ou dobra no cabo.",
                        recomendacao = "Informe o valor ao provedor em caso de instabilidade recorrente.",
                        categoria = CAT,
                    ),
                )
                GponSaudeStatus.boa -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-02-OK",
                        titulo = "Sinal de Recepção Bom",
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
        if (tx != 0.0) {
            when (ClassificadorSaudeGpon.classificarTx(tx)) {
                GponSaudeStatus.ruim -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-03",
                        titulo = "Potência de Transmissão Ruim",
                        status = DiagnosticStatus.critical,
                        evidencia = "tx=${"%.2f".format(tx)} dBm",
                        mensagemUsuario = "A potência de transmissão do laser da ONT está muito baixa (${"%.2f".format(tx)} dBm). O equipamento pode estar com defeito.",
                        recomendacao = "Reinicie a ONT. Se o problema persistir, o equipamento pode precisar de substituição pelo provedor.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                GponSaudeStatus.regular -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-03b",
                        titulo = "Potência de Transmissão Baixa",
                        status = DiagnosticStatus.attention,
                        evidencia = "tx=${"%.2f".format(tx)} dBm",
                        mensagemUsuario = "A potência de transmissão está abaixo do ideal (${"%.2f".format(tx)} dBm). Pode indicar desgaste do laser.",
                        recomendacao = "Monitore. Se houver quedas frequentes, solicite visita técnica ao provedor.",
                        categoria = CAT,
                    ),
                )
                GponSaudeStatus.boa -> {
                    // Mantemos o comportamento anterior: caso esteja muito acima do esperado, vira atencao.
                    if (tx > 5.0) {
                        resultados.add(
                            DiagnosticResult(
                                id = "FIB-03-ALTO",
                                titulo = "Potência de Transmissão Alta",
                                status = DiagnosticStatus.attention,
                                evidencia = "tx=${"%.2f".format(tx)} dBm",
                                mensagemUsuario = "A potência de transmissão está acima do esperado (${"%.2f".format(tx)} dBm). Pode causar saturação no receptor da OLT.",
                                recomendacao = "Informe o provedor para verificação da configuração da OLT.",
                                categoria = CAT,
                            ),
                        )
                    } else {
                        resultados.add(
                            DiagnosticResult(
                                id = "FIB-03-OK",
                                titulo = "Potência de Transmissão Boa",
                                status = DiagnosticStatus.ok,
                                evidencia = "tx=${"%.2f".format(tx)} dBm",
                                mensagemUsuario = "A potência de transmissão da ONT está na faixa ideal (${"%.2f".format(tx)} dBm).",
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
        if (temp != 0.0) {
            when (ClassificadorSaudeGpon.classificarTemp(temp)) {
                GponSaudeStatus.ruim -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-04",
                        titulo = "Temperatura Crítica da ONT",
                        status = DiagnosticStatus.critical,
                        evidencia = "temp=${"%.1f".format(temp)} °C",
                        mensagemUsuario = "A ONT está com temperatura muito alta (${"%.1f".format(temp)} °C). Risco de desligamento por proteção térmica.",
                        recomendacao = "Melhore a ventilação ao redor da ONT. Certifique-se de que não está coberta ou próxima a fontes de calor.",
                        categoria = CAT,
                        podeConcluir = true,
                    ),
                )
                GponSaudeStatus.regular -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-04b",
                        titulo = "Temperatura Elevada da ONT",
                        status = DiagnosticStatus.attention,
                        evidencia = "temp=${"%.1f".format(temp)} °C",
                        mensagemUsuario = "A ONT está aquecida (${"%.1f".format(temp)} °C). Temperaturas acima de 65 °C reduzem a vida útil do equipamento.",
                        recomendacao = "Verifique se há ventilação adequada ao redor da ONT.",
                        categoria = CAT,
                    ),
                )
                GponSaudeStatus.boa -> resultados.add(
                    DiagnosticResult(
                        id = "FIB-04-OK",
                        titulo = "Temperatura Normal da ONT",
                        status = DiagnosticStatus.ok,
                        evidencia = "temp=${"%.1f".format(temp)} °C",
                        mensagemUsuario = "A temperatura da ONT está normal (${"%.1f".format(temp)} °C).",
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
