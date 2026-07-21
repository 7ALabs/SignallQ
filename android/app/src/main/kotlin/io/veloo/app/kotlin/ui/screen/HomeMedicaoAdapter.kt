package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.home.MetricasMedicaoHome
import io.signallq.app.feature.speedtest.MeasurementStatus
import io.signallq.app.feature.speedtest.ResultadoSpeedtest

/**
 * GH#1223 item 1/RF-02/RF-03 — adapta [ResultadoSpeedtest] (feature/speedtest) e
 * [MedicaoEntity] (core/database) pra [MetricasMedicaoHome] (feature/home), a struct genérica
 * que [io.signallq.app.feature.home.ResolvedorMedicaoHome] usa pra nunca misturar métricas de
 * execuções diferentes. Vive em `:app` (não em `feature/home`) porque a lei de dependência do
 * repo proíbe `feature/home` → `feature/speedtest` — só quem já depende dos dois pode adaptar.
 */
internal fun ResultadoSpeedtest.paraMetricasMedicaoHome(ssid: String?): MetricasMedicaoHome =
    MetricasMedicaoHome(
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        latenciaMs = latenciaMs,
        jitterMs = jitterMs,
        perdaPercentual = perdaPercentual,
        timestampEpochMs = timestampEpochMs,
        connectionType = connectionType,
        ssid = ssid,
        vereditoGamer = diagnosticoQualidade.vereditoGamer.name,
        gargaloPrimario = diagnosticoQualidade.gargaloPrimario.name,
        utilizavel = status == MeasurementStatus.COMPLETE,
    )

internal fun MedicaoEntity.paraMetricasMedicaoHome(): MetricasMedicaoHome =
    MetricasMedicaoHome(
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        latenciaMs = latencyMs,
        jitterMs = jitterMs,
        perdaPercentual = perdaPercentual,
        timestampEpochMs = timestampEpochMs,
        connectionType = connectionType,
        ssid = null,
        vereditoGamer = vereditoGamer,
        gargaloPrimario = gargaloPrimario,
        utilizavel = status == "completed",
    )
