package io.signallq.app.ui.relatorio

/**
 * Snapshot IMUTAVEL de um relatório de diagnóstico/velocidade — GH#1219.
 *
 * Antes desta issue existiam dois geradores de PDF independentes (`ResultadoPdfGenerator`,
 * pós-speedtest, e o gerador privado de `LaudoScreen`), cada um lendo diretamente de fontes
 * ao vivo (`SnapshotDiagnostico`, `MedicaoEntity`, `AnalisadorState`) sem garantia de que
 * pertenciam ao mesmo ciclo de medição — podiam combinar diagnóstico atual com última
 * medição salva antiga, ou usar o horário da GERAÇÃO do PDF como se fosse o horário da
 * MEDIÇÃO (issue #4/#5 do corpo de #1219).
 *
 * Este data class é o ÚNICO ponto de entrada do renderer (ver [RelatorioDiagnosticoHtmlBuilder]):
 * cada caller (pós-speedtest, laudo de diagnóstico) monta o snapshot UMA VEZ a partir do seu
 * próprio estado consistente, depois só entrega objetos imutáveis — nenhuma leitura de
 * StateFlow "ao vivo" acontece dentro do builder.
 */
data class RelatorioDiagnosticoSnapshot(
    /** GH#1221/#1225 — id da execução de origem (vazio quando o dado vem de uma
     *  [io.signallq.app.core.database.MedicaoEntity] persistida antes desta issue existir,
     *  que não tinha esse campo). */
    val executionId: String,
    /** GH#1219 item 2 — nome do documento no app B2C. NUNCA "Laudo Técnico" (reservado ao
     *  Pro, quando houver responsável técnico/metodologia completa/assinatura). */
    val nomeDocumento: String = "Relatório de diagnóstico da conexão",
    /** Horário real em que a MEDIÇÃO foi feita — nunca o horário de geração do PDF. */
    val medidoEmEpochMs: Long,
    /** Horário em que este PDF está sendo gerado — GH#1219 item 5, exibido separado de
     *  [medidoEmEpochMs] (podem divergir horas/dias quando reaproveita a última medição
     *  salva offline). */
    val geradoEmEpochMs: Long = System.currentTimeMillis(),
    val tipoRede: String,
    /** `null` = não medido/indisponível (nunca renderizar como `0`, GH#1219 item 6). */
    val downloadMbps: Double?,
    val uploadMbps: Double?,
    val uploadNaoDetectado: Boolean = false,
    val latenciaMs: Double?,
    val jitterMs: Double?,
    val perdaPercentual: Double?,
    /** GH#1219 item 7 / GH#1221 RF-04 — quando `true`, a UI mostra "estimada" ao lado do
     *  valor de perda (método é falha/timeout de probe HTTP, não medição direta de perda
     *  de pacotes IP). */
    val perdaEstimada: Boolean = true,
    val bufferbloatMs: Double?,
    /** Label PT-BR já traduzido do [io.signallq.app.feature.speedtest.MeasurementStatus]
     *  (ex.: "Completo"/"Parcial"/"Inconclusivo"/"Contaminado") — `null` quando a origem
     *  não carrega essa informação (medições persistidas antes de GH#1221/#1225). */
    val statusIntegridade: String? = null,
    val veredito: String? = null,
    val resumo: String? = null,
    val recomendacao: String? = null,
    /** Origem do diagnóstico exibido: "ia", "local" ou `null` (não disponível) — GH#1219
     *  item 8, metadado mínimo de rastreabilidade do diagnóstico automatizado. */
    val diagnosticoOrigem: String? = null,
    /** GH#1219 item 11 — SSID/IPs já mascarados pelo CALLER antes de chegar aqui (o builder
     *  não sabe mascarar, só exibe o que recebe). Nenhum toggle de "mostrar sem máscara"
     *  nesta entrega — requer decisão de design/produto, ver pendências registradas. */
    val ssidMascarado: String? = null,
    val ipLocalMascarado: String? = null,
    val ipPublicoMascarado: String? = null,
    val operadora: String? = null,
    val servidorTeste: String? = null,
    val versaoApp: String,
    /** Versão do motor de speedtest/diagnóstico — `ResultadoSpeedtest.specVersion` quando
     *  disponível, ou "n/d". */
    val versaoMotor: String = "n/d",
    val offline: Boolean = false,
)
