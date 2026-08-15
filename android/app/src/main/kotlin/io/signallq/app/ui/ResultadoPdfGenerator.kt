package io.signallq.app.ui

import android.content.Context
import io.signallq.app.BuildConfig
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.feature.speedtest.labelPt
import io.signallq.app.ui.relatorio.RelatorioDiagnosticoExporter
import io.signallq.app.ui.relatorio.RelatorioDiagnosticoSnapshot
import io.signallq.app.ui.screen.AnalisadorState

/**
 * Exporta o resultado do teste (tela "Resultado do teste", GH#536) como PDF.
 *
 * GH#1219 — antes gerava o PDF direto via `PdfDocument`/`Canvas` manual (paginação, quebra
 * de linha e disclaimer próprios, divergentes do gerador de `LaudoScreen`). Agora só monta
 * o [RelatorioDiagnosticoSnapshot] a partir do estado já disponível na tela e delega pro
 * renderer único ([RelatorioDiagnosticoExporter]/`core/relatorio`).
 */
object ResultadoPdfGenerator {
    suspend fun gerarECompartilhar(
        context: Context,
        resultado: ResultadoSpeedtest,
        snapshotDiagnostico: SnapshotDiagnostico,
        analisadorState: AnalisadorState,
        ispInfo: IspInfo?,
        operadoraMovel: String?,
        localizacaoServidor: String?,
    ) {
        val decisao = snapshotDiagnostico.relatorio?.decisao
        val analiseEspecifica = analisadorState as? AnalisadorState.Resultado
        val recomendacao =
            analiseEspecifica?.acoes?.firstOrNull()?.let { "${it.titulo}: ${it.descricao}" }
                ?: decisao?.recomendacao

        val snapshot =
            RelatorioDiagnosticoSnapshot(
                executionId = resultado.executionId,
                medidoEmEpochMs = resultado.timestampEpochMs,
                tipoRede = tipoRedeLabel(resultado.connectionType, resultado.tecnologia),
                downloadMbps = resultado.downloadMbps,
                uploadMbps = resultado.uploadMbps,
                uploadNaoDetectado = resultado.uploadNaoDetectado,
                latenciaMs = resultado.latenciaMs,
                jitterMs = resultado.jitterMs,
                perdaPercentual = resultado.perdaPercentual,
                perdaEstimada = resultado.packetLossSource == "estimated",
                bufferbloatMs = resultado.bufferbloatMs,
                statusIntegridade = resultado.status.labelPt(),
                veredito = decisao?.titulo,
                resumo = decisao?.mensagemUsuario?.ifBlank { null } ?: analiseEspecifica?.resumo?.ifBlank { null },
                recomendacao = recomendacao,
                diagnosticoOrigem = analiseEspecifica?.origem,
                operadora = (ispInfo?.isp ?: operadoraMovel)?.takeIf { it.isNotBlank() },
                servidorTeste = localizacaoServidor,
                versaoApp = BuildConfig.VERSION_NAME,
                versaoMotor = resultado.specVersion,
            )

        RelatorioDiagnosticoExporter.gerarECompartilhar(
            context = context,
            snapshot = snapshot,
            nomeArquivoPrefixo = "resultado_signallq",
        )
    }

    private fun tipoRedeLabel(
        connectionType: String?,
        tecnologia: String?,
    ): String =
        when {
            connectionType.equals("wifi", ignoreCase = true) -> "Wi-Fi"
            connectionType.equals("movel", ignoreCase = true) -> "Rede móvel" + (tecnologia?.let { " ($it)" } ?: "")
            else -> "Não identificado"
        }
}
