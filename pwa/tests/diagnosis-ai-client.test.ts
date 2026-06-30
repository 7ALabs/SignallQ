import { describe, expect, it, vi } from 'vitest';
import { createDiagnosisWithAiFallback } from '../src/features/diagnosis/aiClient';
import type { DiagnosisResult, SpeedTestResult } from '../shared/contracts';

const speedTest: SpeedTestResult = {
  availability: {
    failedRequests: 0,
    perceivedLossPercent: 0,
    status: 'inferred',
    totalRequests: 17,
  },
  browser: {
    language: 'pt-BR',
  },
  connection: {
    effectiveType: '4g',
    source: 'network_information_api',
  },
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
    samples: 15,
    status: 'measured',
  },
  latency: {
    method: 'http_timing',
    ms: 20,
    samples: 15,
    status: 'measured',
  },
  limitations: ['http_latency_not_icmp_ping'],
  measuredAt: '2026-06-29T00:00:00.000Z',
  upload: {
    bytes: 500_000,
    durationMs: 1000,
    mbps: 4,
    samples: 1,
    status: 'measured',
  },
};

const aiDiagnosis: DiagnosisResult = {
  actions: [],
  confidence: 'high',
  generatedAt: '2026-06-29T00:00:01.000Z',
  id: 'diag_ai',
  limitations: [],
  quality: 'attention',
  source: 'ai',
  speed: 'slow',
  stability: 'stable',
  summary: 'Resumo IA curto.',
};

describe('diagnosis AI client', () => {
  it('posts a structured PWA payload and returns AI diagnosis when the worker responds', async () => {
    let requestBody: unknown = null;
    const fetchFn = vi.fn(async (_input: RequestInfo | URL, init?: RequestInit) => {
      requestBody = JSON.parse(String(init?.body));
      return Response.json(aiDiagnosis);
    });

    const outcome = await createDiagnosisWithAiFallback(speedTest, {
      endpoint: '/mock-ai',
      fetchFn,
    });

    expect(outcome.source).toBe('ai');
    expect(outcome.diagnosis.source).toBe('ai');
    expect(requestBody).toMatchObject({
      browserContext: {
        unavailableNativeSignals: expect.arrayContaining(['wifi_rssi', 'icmp_ping', 'system_dns']),
      },
      metricasAtuais: {
        downloadMbps: 8,
        jitterMs: 4,
        latenciaMs: 20,
        uploadMbps: 4,
      },
      source: 'pwa',
    });
  });

  it('falls back to local diagnosis without blocking the result when AI is unavailable', async () => {
    const fetchFn = vi.fn(async () => Response.json({ error: 'AI_WORKER_URL não configurada' }, { status: 503 }));

    const outcome = await createDiagnosisWithAiFallback(speedTest, {
      endpoint: '/mock-ai',
      fetchFn,
    });

    expect(outcome.source).toBe('fallback');
    expect(outcome.diagnosis.source).toBe('fallback');
    expect(outcome.diagnosis.limitations.some((limitation) => limitation.code === 'ai_unavailable')).toBe(true);
  });

  it('falls back when the AI request times out', async () => {
    const fetchFn = vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
      return new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), {
          once: true,
        });
      });
    });

    const outcome = await createDiagnosisWithAiFallback(speedTest, {
      endpoint: '/mock-ai',
      fetchFn,
      timeoutMs: 1,
    });

    expect(outcome.source).toBe('fallback');
    expect(outcome.diagnosis.limitations).toEqual(
      expect.arrayContaining([
        expect.objectContaining({
          code: 'ai_unavailable',
        }),
      ]),
    );
  });
});
