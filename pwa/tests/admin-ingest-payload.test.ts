import { afterEach, describe, expect, it, vi } from 'vitest';
import { buildAdminDiagnosticPayload } from '../src/features/diagnosis/adminIngestPayload';
import type { DiagnosisResult, SpeedTestResult } from '../shared/contracts';

const speedTest: SpeedTestResult = {
  availability: {
    failedRequests: 0,
    perceivedLossPercent: 1.5,
    status: 'inferred',
    totalRequests: 12,
  },
  browser: {},
  connection: { source: 'unavailable' },
  download: {
    bytes: 1_000_000,
    durationMs: 1000,
    mbps: 42,
    samples: 1,
    status: 'measured',
  },
  id: 'speed_test_web_1',
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
  limitations: [],
  measuredAt: '2026-07-04T12:00:00.000Z',
  upload: {
    bytes: 500_000,
    durationMs: 1000,
    mbps: 8,
    samples: 1,
    status: 'measured',
  },
};

const localDiagnosis: DiagnosisResult = {
  actions: [],
  confidence: 'medium',
  generatedAt: '2026-07-04T12:00:01.000Z',
  id: 'diag_1',
  limitations: [],
  quality: 'good',
  source: 'local',
  speed: 'fast',
  stability: 'stable',
  summary: 'Conexao boa.',
};

const aiDiagnosis: DiagnosisResult = { ...localDiagnosis, source: 'ai', summary: 'Laudo gerado pela IA.' };

describe('buildAdminDiagnosticPayload', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('marks the payload origin as web and maps measured metrics as-is', () => {
    const payload = buildAdminDiagnosticPayload(speedTest, localDiagnosis);

    expect(payload).toMatchObject({
      id: 'speed_test_web_1',
      created_at: Math.floor(new Date(speedTest.measuredAt).getTime() / 1000),
      download_mbps: 42,
      upload_mbps: 8,
      latency_ms: 20,
      jitter_ms: 4,
      packet_loss: 1.5,
      platform: 'web',
      status: 'completed',
    });
  });

  it('never invents network_type, operator or device fields the browser cannot measure', () => {
    const payload = buildAdminDiagnosticPayload(speedTest, localDiagnosis);

    expect(payload.network_type).toBe('unknown');
    expect(payload).not.toHaveProperty('operator');
    expect(payload).not.toHaveProperty('device_model');
  });

  it('only includes ai_summary_report when the diagnosis actually came from the AI', () => {
    expect(buildAdminDiagnosticPayload(speedTest, localDiagnosis).ai_summary_report).toBeUndefined();
    expect(buildAdminDiagnosticPayload(speedTest, aiDiagnosis).ai_summary_report).toBe('Laudo gerado pela IA.');
  });

  it('reports staging when running on localhost, production otherwise', () => {
    vi.stubGlobal('window', { location: { hostname: 'localhost' } });
    expect(buildAdminDiagnosticPayload(speedTest, localDiagnosis).environment).toBe('staging');

    vi.stubGlobal('window', { location: { hostname: 'signallq-pwa.pages.dev' } });
    expect(buildAdminDiagnosticPayload(speedTest, localDiagnosis).environment).toBe('production');
  });
});
