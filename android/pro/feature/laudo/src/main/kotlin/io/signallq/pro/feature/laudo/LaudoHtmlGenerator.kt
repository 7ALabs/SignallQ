package io.signallq.pro.feature.laudo

import io.signallq.pro.core.database.diagnostico.DiagnosticoAchadoProEntity
import io.signallq.pro.core.database.visita.TipoVisita
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Gera o HTML/CSS do laudo tecnico do Pro (tela 3.2). Funcao pura -- sem dependencia de
 * Android/Context, testavel em JVM -- delegada ao motor generico de paginacao
 * [io.signallq.app.core.relatorio.exportarHtmlComoPdf] (`:core:relatorio`) pelo
 * [LaudoViewModel]. Layout proprio do Pro (REWRITE esperado, doc 13 §3.4): nao herda o
 * template do consumidor (`ExportadorHistoricoPDF.gerarHtml`).
 */
internal object LaudoHtmlGenerator {
    private val formatadorData = SimpleDateFormat("dd/MM/yyyy 'as' HH:mm", Locale("pt", "BR"))

    /** CSS extraido de [gerarHtml] para nao estourar o limiar de LongMethod do detekt --
     *  string pura, sem interpolacao de dados da visita. */
    private const val ESTILOS_CSS = """
* { box-sizing: border-box; margin: 0; padding: 0; }
body { font-family: 'Arial', sans-serif; font-size: 12px; color: #1B1F27; margin: 24px; background: #fff; }

.header { margin-bottom: 20px; border-bottom: 2px solid #0B6CFF; padding-bottom: 12px; }
.header h1 { color: #0B6CFF; font-size: 22px; font-weight: bold; margin-bottom: 4px; }
.header .subtitle { color: #6B7280; font-size: 11px; }

.resumo { display: flex; gap: 16px; margin: 16px 0 24px; }
.resumo-card { background: #D8E7FF; border-radius: 6px; padding: 10px 16px; flex: 1; }
.resumo-card .label { font-size: 10px; color: #0B6CFF; text-transform: uppercase; font-weight: bold; }
.resumo-card .valor { font-size: 16px; font-weight: bold; color: #06305C; margin-top: 2px; }

.ambiente { margin-bottom: 20px; page-break-inside: avoid; }
.ambiente h2 { font-size: 14px; color: #06305C; border-left: 3px solid #0B6CFF; padding-left: 8px; margin-bottom: 8px; }

table { width: 100%; border-collapse: collapse; font-size: 11px; margin-bottom: 10px; }
thead tr { background: #0B6CFF; color: white; }
thead th { padding: 7px 8px; text-align: left; font-weight: bold; }
tbody td { padding: 6px 8px; border-bottom: 1px solid #E5E9F0; }

.achado { border: 1px solid #E5E9F0; border-radius: 6px; padding: 8px 10px; margin-bottom: 6px; }
.achado .prioridade { font-size: 9px; text-transform: uppercase; font-weight: bold; padding: 2px 8px; border-radius: 999px; display: inline-block; margin-bottom: 4px; }
.prioridade.critico { background: #FFDAD6; color: #D9363E; }
.prioridade.atencao { background: #FFE9B8; color: #8A5A00; }
.prioridade.info { background: #E7ECF3; color: #4B5563; }
.achado .titulo { font-weight: bold; margin-bottom: 2px; }
.achado .recomendacao { color: #0B6CFF; margin-top: 4px; }

.sem-dado { color: #6B7280; font-style: italic; }

.footer { margin-top: 24px; font-size: 9px; color: #9CA3AF; text-align: center; border-top: 1px solid #E5E9F0; padding-top: 8px; }

@media print {
  body { margin: 0; }
  thead { display: table-header-group; }
  .ambiente { page-break-inside: avoid; }
}
"""

    fun gerarHtml(dados: LaudoDados): String {
        val dataVisita = formatadorData.format(Date(dados.dataVisitaEpochMs))
        val secoesAmbientes = dados.ambientes.joinToString("\n") { ambiente -> secaoAmbiente(ambiente) }

        return """<!DOCTYPE html>
<html lang="pt-BR">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Laudo tecnico SignallQ Pro</title>
  <style>
$ESTILOS_CSS
  </style>
</head>
<body>
  <div class="header">
    <h1>Laudo tecnico -- ${escapeHtml(dados.clienteNome)}</h1>
    <p class="subtitle">${rotuloTipoVisita(dados.tipoVisita)} &nbsp;|&nbsp; Visita em: $dataVisita</p>
    <p class="subtitle">${localHtml(dados)}</p>
  </div>

  <div class="resumo">
    <div class="resumo-card">
      <div class="label">Ambientes avaliados</div>
      <div class="valor">${dados.ambientes.size}</div>
    </div>
    <div class="resumo-card">
      <div class="label">Evidencias registradas</div>
      <div class="valor">${dados.totalEvidencias}</div>
    </div>
    <div class="resumo-card">
      <div class="label">Recomendacoes</div>
      <div class="valor">${dados.totalAchados}</div>
    </div>
  </div>

$secoesAmbientes

  <div class="footer">
    Laudo gerado por ${escapeHtml(dados.profissionalNome)} via SignallQ Pro
    ${dados.clienteTelefone?.let { " &nbsp;|&nbsp; Cliente: " + escapeHtml(it) } ?: ""}
  </div>
</body>
</html>"""
    }

    private fun localHtml(dados: LaudoDados): String {
        val endereco = dados.localEndereco.takeIf { it.isNotBlank() } ?: "Endereco nao informado"
        return "Local: ${escapeHtml(dados.localNome)} &nbsp;|&nbsp; ${escapeHtml(endereco)}"
    }

    private fun secaoAmbiente(ambiente: LaudoAmbienteDados): String {
        val medicao = ambiente.medicao
        val diagnostico = ambiente.diagnostico
        val tabelaMedicao =
            if (medicao == null) {
                """    <p class="sem-dado">Nenhuma medicao registrada neste ambiente.</p>"""
            } else {
                """    <table>
      <thead>
        <tr><th>Download</th><th>Upload</th><th>Latencia</th><th>Jitter</th><th>Perda</th></tr>
      </thead>
      <tbody>
        <tr>
          <td>${"%.1f".format(Locale.US, medicao.downloadMbps)} Mbps</td>
          <td>${"%.1f".format(Locale.US, medicao.uploadMbps)} Mbps</td>
          <td>${"%.0f".format(Locale.US, medicao.latenciaMs)} ms</td>
          <td>${"%.0f".format(Locale.US, medicao.jitterMs)} ms</td>
          <td>${"%.1f".format(Locale.US, medicao.perdaPercentual)}%</td>
        </tr>
      </tbody>
    </table>"""
            }

        val vereditoHtml =
            if (diagnostico == null) {
                ""
            } else {
                """    <p><strong>Veredito:</strong> ${escapeHtml(diagnostico.veredito)} (${diagnostico.scoreConexao}/100)</p>"""
            }

        val achadosHtml =
            if (ambiente.achados.isEmpty()) {
                """    <p class="sem-dado">Nenhum problema relevante identificado neste ambiente.</p>"""
            } else {
                ambiente.achados.joinToString("\n") { achado -> achadoHtml(achado) }
            }

        return """  <div class="ambiente">
    <h2>${escapeHtml(ambiente.nome)}</h2>
$tabelaMedicao
$vereditoHtml
$achadosHtml
  </div>"""
    }

    private fun achadoHtml(achado: DiagnosticoAchadoProEntity): String {
        val (classePrioridade, rotuloPrioridade) = prioridade(achado.status)
        val recomendacaoHtml =
            achado.recomendacao
                ?.takeIf { it.isNotBlank() }
                ?.let { """<div class="recomendacao">${escapeHtml(it)}</div>""" }
                ?: ""
        return """    <div class="achado">
      <span class="prioridade $classePrioridade">$rotuloPrioridade</span>
      <div class="titulo">${escapeHtml(achado.titulo)}</div>
      <div>${escapeHtml(achado.mensagem)}</div>
      $recomendacaoHtml
    </div>"""
    }

    private fun prioridade(status: String): Pair<String, String> =
        when (status) {
            "critical" -> "critico" to "Critico"
            "attention" -> "atencao" to "Atencao"
            else -> "info" to "Informativo"
        }

    private fun rotuloTipoVisita(tipo: TipoVisita): String =
        when (tipo) {
            TipoVisita.INSTALACAO -> "Instalacao"
            TipoVisita.MANUTENCAO -> "Manutencao"
            TipoVisita.VISTORIA -> "Vistoria"
            TipoVisita.SUPORTE -> "Suporte"
        }

    private fun escapeHtml(texto: String): String =
        texto
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}
