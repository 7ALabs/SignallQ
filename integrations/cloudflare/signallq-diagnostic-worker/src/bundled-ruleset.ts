import type { DiagnosticRuleset } from "./contracts.ts";

export function getBundledRuleset(): DiagnosticRuleset {
  return {
    version: 7,
    schemaVersion: 6,
    engineVersion: 4,
    publishedAt: "2026-07-14T00:00:00.000Z",
    rules: [
      {
        ruleId: "internet_download_unavailable",
        ruleVersion: 1,
        enabled: true,
        priority: 200,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "speed.downloadMbps", operator: "NOT_EXISTS" },
            { field: "connection.hasInternet", operator: "EQ", value: false },
          ],
        },
        result: {
          findingCode: "INTERNET_UNAVAILABLE",
          category: "internet",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "VERIFY_LINK_AND_PROVIDER",
        },
      },
      {
        ruleId: "packet_loss_critical",
        ruleVersion: 1,
        enabled: true,
        priority: 190,
        minimumSchemaVersion: 6,
        conditions: [{ field: "quality.packetLossPercent", operator: "GTE", value: 3 }],
        result: {
          findingCode: "PACKET_LOSS_HIGH",
          category: "internet",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "CHECK_LINK_STABILITY",
        },
      },
      {
        ruleId: "upload_zero",
        ruleVersion: 1,
        enabled: true,
        priority: 185,
        minimumSchemaVersion: 6,
        conditions: [{ field: "speed.uploadMbps", operator: "EQ", value: 0 }],
        result: {
          findingCode: "UPLOAD_ZERO",
          category: "internet",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "VERIFY_UPLOAD_PATH",
        },
      },
      {
        // GH#955 — threshold alinhado a MetricClassifier.classificarBufferbloat
        // (Android): >100ms de bufferbloat e "ruim". Antes estava 130ms (~30ms
        // deslocado pra cima), gerando falso negativo no worker.
        ruleId: "bufferbloat_critical",
        ruleVersion: 2,
        enabled: true,
        priority: 180,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "quality.loadedLatencyMs", operator: "EXISTS" },
            { field: "quality.latencyMs", operator: "EXISTS" },
            { field: "quality.loadedLatencyMs", operator: "GT", value: 100 },
          ],
        },
        result: {
          findingCode: "BUFFERBLOAT_CRITICAL",
          category: "internet",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "ENABLE_QOS_OR_SQM",
        },
      },
      {
        ruleId: "fiber_rx_power_critical",
        ruleVersion: 1,
        enabled: true,
        priority: 175,
        minimumSchemaVersion: 6,
        conditions: [{ field: "fiber.rxPowerDbm", operator: "LT", value: -27 }],
        result: {
          findingCode: "FIBER_RX_POWER_LOW",
          category: "fibra",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "CHECK_FIBER_SIGNAL_WITH_PROVIDER",
        },
      },
      {
        ruleId: "mobile_signal_poor_5g",
        ruleVersion: 1,
        enabled: true,
        priority: 170,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ANY",
          conditions: [
            { field: "mobile.rsrpDbm", operator: "LTE", value: -110 },
            { field: "mobile.sinrDb", operator: "LT", value: 0 },
          ],
        },
        result: {
          findingCode: "MOBILE_SIGNAL_POOR_5G",
          category: "mobile",
          severity: "ERROR",
          confidence: "MEDIUM",
          recommendationId: "MOVE_TO_BETTER_5G_COVERAGE",
        },
      },
      {
        // GH#955 — latencia alinhada a MetricClassifier.classificarLatencia
        // (Android): 100-150ms e "bom", so acima de 150ms e problema. Antes
        // disparava ERROR combinado a partir de 100ms, mesmo intervalo que o
        // motor real considera saudavel.
        ruleId: "high_latency_and_jitter",
        ruleVersion: 3,
        enabled: true,
        priority: 160,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "quality.latencyMs", operator: "GT", value: 150 },
            { field: "quality.jitterMs", operator: "GT", value: 20 },
          ],
        },
        result: {
          findingCode: "HIGH_LATENCY_JITTER",
          category: "internet",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "CHECK_WIFI_AND_ROUTER_LOAD",
        },
      },
      {
        ruleId: "wifi_signal_critical_24ghz",
        ruleVersion: 1,
        enabled: true,
        priority: 155,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "wifi.band", operator: "EQ", value: "2_4_GHZ" },
            { field: "wifi.rssiDbm", operator: "LTE", value: -80 },
          ],
        },
        result: {
          findingCode: "WIFI_SIGNAL_CRITICAL",
          category: "wifi",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "MOVE_CLOSER_TO_ROUTER",
        },
      },
      {
        ruleId: "wifi_signal_critical_5ghz",
        ruleVersion: 1,
        enabled: true,
        priority: 155,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "wifi.band", operator: "IN", value: ["5_GHZ", "6_GHZ"] },
            { field: "wifi.rssiDbm", operator: "LTE", value: -82 },
          ],
        },
        result: {
          findingCode: "WIFI_SIGNAL_CRITICAL",
          category: "wifi",
          severity: "ERROR",
          confidence: "HIGH",
          recommendationId: "MOVE_CLOSER_TO_ROUTER",
        },
      },
      {
        ruleId: "packet_loss_moderate",
        ruleVersion: 1,
        enabled: true,
        priority: 140,
        minimumSchemaVersion: 6,
        conditions: [{ field: "quality.packetLossPercent", operator: "GTE", value: 1 }],
        result: {
          findingCode: "PACKET_LOSS_MODERATE",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "CHECK_WIFI_INTERFERENCE",
        },
      },
      {
        ruleId: "dns_latency_high",
        ruleVersion: 1,
        enabled: true,
        priority: 135,
        minimumSchemaVersion: 6,
        conditions: [{ field: "dns.latencyMs", operator: "GT", value: 300 }],
        result: {
          findingCode: "DNS_LATENCY_HIGH",
          category: "dns",
          severity: "WARNING",
          confidence: "MEDIUM",
          recommendationId: "COMPARE_WITH_FASTER_DNS",
        },
      },
      {
        // GH#955 — threshold alinhado a MetricClassifier.classificarBufferbloat
        // (Android): >30ms ja sai de "bom". Antes estava 60ms.
        ruleId: "bufferbloat_elevated",
        ruleVersion: 2,
        enabled: true,
        priority: 130,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "quality.loadedLatencyMs", operator: "EXISTS" },
            { field: "quality.latencyMs", operator: "EXISTS" },
            { field: "quality.loadedLatencyMs", operator: "GT", value: 30 },
          ],
        },
        result: {
          findingCode: "BUFFERBLOAT_ELEVATED",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "ENABLE_QOS_OR_SQM",
        },
      },
      {
        // GH#955 — latencia alinhada a MetricClassifier.classificarLatencia
        // (Android): <100ms excelente, 100-150ms bom, 150-200ms regular, >200ms
        // ruim. Ate 150ms nao e "problema" — antes disparava aviso a partir de
        // 100ms, avisando o usuario de algo que o motor real considera saudavel.
        ruleId: "latency_high",
        ruleVersion: 2,
        enabled: true,
        priority: 120,
        minimumSchemaVersion: 6,
        conditions: [{ field: "quality.latencyMs", operator: "GT", value: 150 }],
        result: {
          findingCode: "LATENCY_HIGH",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "RETEST_AND_CHECK_PROVIDER_CONGESTION",
        },
      },
      {
        ruleId: "jitter_elevated",
        ruleVersion: 1,
        enabled: true,
        priority: 115,
        minimumSchemaVersion: 6,
        conditions: [{ field: "quality.jitterMs", operator: "GT", value: 20 }],
        result: {
          findingCode: "JITTER_HIGH",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "REDUCE_BACKGROUND_TRAFFIC",
        },
      },
      {
        ruleId: "upload_low",
        ruleVersion: 1,
        enabled: true,
        priority: 110,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "speed.uploadMbps", operator: "GT", value: 0 },
            { field: "speed.uploadMbps", operator: "LT", value: 5 },
          ],
        },
        result: {
          findingCode: "UPLOAD_LOW",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "CHECK_BACKGROUND_UPLOADS",
        },
      },
      {
        ruleId: "download_low",
        ruleVersion: 1,
        enabled: true,
        priority: 105,
        minimumSchemaVersion: 6,
        conditions: [{ field: "speed.downloadMbps", operator: "LT", value: 25 }],
        result: {
          findingCode: "DOWNLOAD_LOW",
          category: "internet",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "VERIFY_PLAN_AND_ACTIVE_DEVICES",
        },
      },
      {
        ruleId: "wifi_signal_weak_24ghz",
        ruleVersion: 1,
        enabled: true,
        priority: 100,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "wifi.band", operator: "EQ", value: "2_4_GHZ" },
            { field: "wifi.rssiDbm", operator: "LTE", value: -70 },
            { field: "wifi.rssiDbm", operator: "GT", value: -80 },
          ],
        },
        result: {
          findingCode: "WIFI_SIGNAL_WEAK",
          category: "wifi",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "MOVE_CLOSER_TO_ROUTER",
        },
      },
      {
        ruleId: "wifi_signal_weak_5ghz",
        ruleVersion: 1,
        enabled: true,
        priority: 100,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "wifi.band", operator: "IN", value: ["5_GHZ", "6_GHZ"] },
            { field: "wifi.rssiDbm", operator: "LTE", value: -75 },
            { field: "wifi.rssiDbm", operator: "GT", value: -82 },
          ],
        },
        result: {
          findingCode: "WIFI_SIGNAL_WEAK",
          category: "wifi",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "MOVE_CLOSER_TO_ROUTER",
        },
      },
      {
        ruleId: "wifi_link_very_slow",
        ruleVersion: 1,
        enabled: true,
        priority: 95,
        minimumSchemaVersion: 6,
        conditions: [{ field: "wifi.linkSpeedMbps", operator: "LT", value: 54 }],
        result: {
          findingCode: "WIFI_LINK_VERY_SLOW",
          category: "wifi",
          severity: "WARNING",
          confidence: "MEDIUM",
          recommendationId: "CHECK_DISTANCE_AND_INTERFERENCE",
        },
      },
      {
        ruleId: "wifi_24ghz_slow_with_5ghz_available",
        ruleVersion: 2,
        enabled: true,
        priority: 90,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "wifi.band", operator: "EQ", value: "2_4_GHZ" },
            { field: "wifi.has5GhzAvailable", operator: "EQ", value: true },
            { field: "speed.downloadMbps", operator: "LT", value: 50 },
          ],
        },
        result: {
          findingCode: "CONNECTED_TO_24GHZ",
          category: "wifi",
          severity: "WARNING",
          confidence: "HIGH",
          recommendationId: "SWITCH_TO_5GHZ",
        },
      },
      {
        ruleId: "mobile_signal_acceptable_5g",
        ruleVersion: 1,
        enabled: true,
        priority: 80,
        minimumSchemaVersion: 6,
        conditionGroup: {
          operator: "ALL",
          conditions: [
            { field: "mobile.rsrpDbm", operator: "GT", value: -110 },
            { field: "mobile.rsrpDbm", operator: "LTE", value: -95 },
          ],
        },
        result: {
          findingCode: "MOBILE_SIGNAL_ACCEPTABLE_5G",
          category: "mobile",
          severity: "INFO",
          confidence: "MEDIUM",
          recommendationId: "MONITOR_5G_COVERAGE",
        },
      },
      {
        ruleId: "dns_latency_elevated",
        ruleVersion: 1,
        enabled: true,
        priority: 70,
        minimumSchemaVersion: 6,
        conditions: [{ field: "dns.latencyMs", operator: "GT", value: 150 }],
        result: {
          findingCode: "DNS_LATENCY_ELEVATED",
          category: "dns",
          severity: "INFO",
          confidence: "MEDIUM",
          recommendationId: "COMPARE_WITH_FASTER_DNS",
        },
      },
    ],
  };
}
