import { describe, expect, it } from 'vitest';
import { buildDiagnosticPayload } from '../src/features/diagnosis/aiPayload';
import type { SpeedTestResult } from '../shared/contracts';

const speedtest: SpeedTestResult = {
  availability: {
    failedRequests: 0,
    perceivedLossPercent: 0,
    status: 'inferred',
    totalRequests: 12,
  },
  browser: {},
  connection: { source: 'unavailable' },
  download: {
    bytes: 1_000_000,
    durationMs: 1000,
    mbps: 8,
    samples: 1,
    status: 'measured',
  },
  id: 'speed_test',
  jitter: {
    ms: 4,
    samples: 10,
    status: 'measured',
  },
  latency: {
    method: 'http_timing',
    ms: 20,
    samples: 10,
    status: 'measured',
  },
  limitations: ['http_latency_not_icmp_ping'],
  measuredAt: '2026-06-27T00:00:00.000Z',
  upload: {
    bytes: 500_000,
    durationMs: 1000,
    mbps: 4,
    samples: 1,
    status: 'measured',
  },
};

describe('diagnosis AI payload', () => {
  it('maps the full PWA speedtest contract to the current worker payload without native-only fields', () => {
    expect(buildDiagnosticPayload(speedtest, '4g')).toEqual({
      browserContext: {
        browser: {},
        connection: { source: 'unavailable' },
        limitations: ['http_latency_not_icmp_ping'],
        unavailableNativeSignals: [
          'ssid',
          'bssid',
          'wifi_rssi',
          'wifi_channel',
          'nearby_networks',
          'cell_tower_id',
          'telephony_signal',
          'icmp_ping',
          'system_dns',
        ],
      },
      connectionType: '4g',
      metricasAtuais: {
        downloadMbps: 8,
        jitterMs: 4,
        latenciaMs: 20,
        perceivedLossPercent: 0,
        uploadMbps: 4,
      },
      schemaVersion: 'pwa_foundation_v1',
      source: 'pwa',
      speedTest: speedtest,
    });
  });

  it('keeps absent metrics as null instead of inventing native data', () => {
    expect(buildDiagnosticPayload(null, 'offline').metricasAtuais).toEqual({
      downloadMbps: null,
      jitterMs: null,
      latenciaMs: null,
      perceivedLossPercent: null,
      uploadMbps: null,
    });
  });
});
