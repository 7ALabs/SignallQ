package io.signallq.app.ui.relatorio

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Gera o HTML/CSS do relatório de diagnóstico/velocidade — GH#1219.
 *
 * Fonte ÚNICA para os dois fluxos do app consumidor (pós-speedtest e laudo de diagnóstico),
 * substituindo os dois geradores `PdfDocument`/`Canvas` divergentes (`ResultadoPdfGenerator`,
 * gerador privado de `LaudoScreen`). Segue o mesmo padrão já estabelecido por
 * `ExportadorHistoricoPDF.gerarHtml` (`:featureHistory`) — função pura, sem dependência de
 * Android, testável em JVM — e delega a paginação real ao motor genérico
 * `io.signallq.app.core.relatorio.exportarHtmlComoPdf` (`:core:relatorio`, WebView).
 *
 * Não-negociáveis (GH#1219):
 * - SEM afirmação de mínimo garantido/obrigação regulatória (Resolução 574/2011 e
 *   variantes) — a medição é sempre apresentada como informativa, nunca oficial.
 * - Métrica ausente vira "Não medido", nunca `0`/`NaN`.
 * - "Perda" sempre rotulada como estimada quando [RelatorioDiagnosticoSnapshot.perdaEstimada].
 * - Horário de medição e de geração sempre exibidos separadamente.
 * - Identificação do documento (id único, execução, versão do app/motor) sempre no rodapé.
 */
object RelatorioDiagnosticoHtmlBuilder {
    private val formatadorDataHora = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

    fun gerarHtml(snapshot: RelatorioDiagnosticoSnapshot): String {
        val relatorioId = UUID.randomUUID().toString().take(8)
        val medidoEm = formatadorDataHora.format(Date(snapshot.medidoEmEpochMs))
        val geradoEm = formatadorDataHora.format(Date(snapshot.geradoEmEpochMs))

        val download = formatarMetrica(snapshot.downloadMbps, "Mbps", 1)
        val upload = if (snapshot.uploadNaoDetectado) "Não detectado" else formatarMetrica(snapshot.uploadMbps, "Mbps", 1)
        val latencia = formatarMetrica(snapshot.latenciaMs, "ms", 0)
        val jitter = formatarMetrica(snapshot.jitterMs, "ms", 0)
        val perdaValor = formatarMetrica(snapshot.perdaPercentual, "%", 1)
        val perda = if (snapshot.perdaEstimada && snapshot.perdaPercentual != null) "$perdaValor (estimada)" else perdaValor
        val bufferbloat = formatarMetrica(snapshot.bufferbloatMs, "ms", 0)

        val linhasRede =
            buildList {
                snapshot.ssidMascarado?.let { add("Wi-Fi (SSID)" to escapeHtml(it)) }
                snapshot.ipLocalMascarado?.let { add("IP local" to escapeHtml(it)) }
                snapshot.ipPublicoMascarado?.let { add("IP público" to escapeHtml(it)) }
                snapshot.operadora?.takeIf { it.isNotBlank() }?.let { add("Operadora" to escapeHtml(it)) }
                snapshot.servidorTeste?.takeIf { it.isNotBlank() }?.let { add("Servidor de teste" to escapeHtml(it)) }
            }

        val secaoDiagnostico =
            if (!snapshot.veredito.isNullOrBlank() || !snapshot.resumo.isNullOrBlank()) {
                """
                <section class="card">
                  <h2>Diagnóstico</h2>
                  ${if (!snapshot.veredito.isNullOrBlank()) "<p class=\"veredito\">${escapeHtml(snapshot.veredito)}</p>" else ""}
                  ${if (!snapshot.resumo.isNullOrBlank()) "<p>${escapeHtml(snapshot.resumo)}</p>" else ""}
                  ${if (!snapshot.recomendacao.isNullOrBlank()) "<p class=\"recomendacao\"><strong>Recomendação:</strong> ${escapeHtml(snapshot.recomendacao)}</p>" else ""}
                  ${diagnosticoOrigemLabel(snapshot.diagnosticoOrigem)}
                </section>
                """.trimIndent()
            } else {
                ""
            }

        val avisoIntegridade =
            snapshot.statusIntegridade?.takeIf { it != "Completo" }?.let {
                """<div class="aviso-integridade">Integridade da medição: <strong>${escapeHtml(it)}</strong> — os
                    números abaixo podem representar só parte do teste, não trate como conclusão definitiva.</div>"""
            } ?: ""

        val avisoOffline =
            if (snapshot.offline) {
                """<div class="aviso-integridade">Gerado sem conexão no momento — exibindo a última medição salva.</div>"""
            } else {
                ""
            }

        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <title>${escapeHtml(snapshot.nomeDocumento)}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Arial', sans-serif; font-size: 12px; color: #212121; margin: 24px; background: #fff; }
    .header { margin-bottom: 16px; border-bottom: 2px solid #5B21D6; padding-bottom: 12px; }
    .header h1 { color: #5B21D6; font-size: 20px; font-weight: bold; margin-bottom: 4px; }
    .header .subtitle { color: #757575; font-size: 11px; }
    .aviso-integridade { background: #FFF3CD; color: #7A5B00; border-radius: 6px; padding: 8px 12px; font-size: 11px; margin-bottom: 12px; }
    .card { background: #F5F5F5; border-radius: 8px; padding: 14px 16px; margin-bottom: 14px; }
    .card h2 { font-size: 13px; text-transform: uppercase; letter-spacing: 0.4px; color: #5B21D6; margin-bottom: 8px; }
    .metricas { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
    .metrica .label { font-size: 10px; color: #757575; text-transform: uppercase; }
    .metrica .valor { font-size: 16px; font-weight: bold; color: #212121; }
    .veredito { font-weight: bold; margin-bottom: 6px; }
    .recomendacao { margin-top: 6px; }
    table { width: 100%; border-collapse: collapse; font-size: 11px; }
    table td { padding: 5px 0; border-bottom: 1px solid #E0E0E0; }
    table td.label { color: #757575; width: 40%; }
    .disclaimer { font-size: 9px; color: #757575; margin-top: 16px; line-height: 1.5; border-top: 1px solid #E0E0E0; padding-top: 10px; }
    .footer { margin-top: 12px; font-size: 8px; color: #9E9E9E; }
    @media print { body { margin: 0; } }
  </style>
</head>
<body>
  <div class="header">
    <h1>${escapeHtml(snapshot.nomeDocumento)}</h1>
    <p class="subtitle">Medição realizada em: $medidoEm &nbsp;|&nbsp; Relatório gerado em: $geradoEm</p>
    <p class="subtitle">Tipo de rede: ${escapeHtml(snapshot.tipoRede)}</p>
  </div>

  $avisoOffline
  $avisoIntegridade

  <section class="card">
    <h2>Métricas</h2>
    <div class="metricas">
      <div class="metrica"><div class="label">Download</div><div class="valor">$download</div></div>
      <div class="metrica"><div class="label">Upload</div><div class="valor">$upload</div></div>
      <div class="metrica"><div class="label">Latência</div><div class="valor">$latencia</div></div>
      <div class="metrica"><div class="label">Oscilação (jitter)</div><div class="valor">$jitter</div></div>
      <div class="metrica"><div class="label">Perda</div><div class="valor">$perda</div></div>
      <div class="metrica"><div class="label">Atraso sob carga</div><div class="valor">$bufferbloat</div></div>
    </div>
  </section>

  $secaoDiagnostico

  ${if (linhasRede.isNotEmpty()) {
            """
            <section class="card">
              <h2>Rede</h2>
              <table>
                ${linhasRede.joinToString("\n      ") { (label, valor) -> "<tr><td class=\"label\">$label</td><td>$valor</td></tr>" }}
              </table>
            </section>
            """.trimIndent()
        } else {
            ""
        }}

  <div class="disclaimer">
    Esta medição é informativa: reflete o desempenho deste aparelho, nesta rede e neste
    momento — pode ser influenciada por Wi-Fi interno, aparelho, servidor de teste, rota e
    uso simultâneo da rede. Não é uma medição oficial da Anatel nem comprova, isoladamente,
    descumprimento contratual ou regulatório. Para reclamação formal, consulte os canais
    oficiais da sua operadora e da Anatel.
  </div>

  <div class="footer">
    SignallQ &nbsp;|&nbsp; Relatório $relatorioId &nbsp;|&nbsp;
    Execução ${snapshot.executionId.ifBlank { "n/d" }} &nbsp;|&nbsp;
    App ${escapeHtml(snapshot.versaoApp)} &nbsp;|&nbsp; Motor ${escapeHtml(snapshot.versaoMotor)}
  </div>
</body>
</html>"""
    }

    private fun diagnosticoOrigemLabel(origem: String?): String =
        when (origem) {
            "ia" -> "<p class=\"footer\">Diagnóstico gerado por IA — orientação automatizada, não substitui avaliação de um técnico.</p>"
            "local" -> "<p class=\"footer\">Diagnóstico gerado pelo motor local do app.</p>"
            else -> ""
        }

    private fun formatarMetrica(
        valor: Double?,
        unidade: String,
        casasDecimais: Int,
    ): String {
        // Nao concatenar `unidade` dentro do padrao de "%.${casasDecimais}f" antes do format —
        // a unidade "%" (perda) e interpretada como inicio de conversao pelo String.format,
        // lancando UnknownFormatConversionException. Formata so o numero, concatena depois.
        if (valor == null) return "Não medido"
        val numero = "%.${casasDecimais}f".format(Locale.US, valor)
        return "$numero $unidade"
    }

    private fun escapeHtml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
