import type { DiagnosticPayload, SpeedTestResult } from '@shared/contracts';

const UNAVAILABLE_NATIVE_SIGNALS = [
  'ssid',
  'bssid',
  'wifi_rssi',
  'wifi_channel',
  'nearby_networks',
  'cell_tower_id',
  'telephony_signal',
  'icmp_ping',
  'system_dns',
];

export function buildDiagnosticPayload(
  speedtest: SpeedTestResult | null,
  connectionType: string,
): DiagnosticPayload {
  const payload: DiagnosticPayload = {
    connectionType,
    metricasAtuais: {
      downloadMbps: speedtest?.download.mbps ?? null,
      jitterMs: speedtest?.jitter.ms ?? null,
      latenciaMs: speedtest?.latency.ms ?? null,
      perdaPacotesPercentual: speedtest?.availability.perceivedLossPercent ?? null,
      uploadMbps: speedtest?.upload.mbps ?? null,
    },
    schemaVersion: 'pwa_foundation_v1',
    source: 'pwa',
  };

  if (speedtest) {
    payload.speedTest = speedtest;
    payload.browserContext = {
      browser: speedtest.browser,
      connection: speedtest.connection,
      limitations: speedtest.limitations,
      unavailableNativeSignals: UNAVAILABLE_NATIVE_SIGNALS,
    };
  }

  return payload;
}
