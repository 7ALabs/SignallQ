import { describe, expect, it } from 'vitest';
import { createDiagnosisWithAiFallback } from '../src/features/diagnosis/aiClient';
import type { SpeedTestResult } from '../shared/contracts';

const speedTest: SpeedTestResult = {
  availability: {
    failedRequests: 0,
    perceivedLossPercent: 0,
    status: 'inferred',
    totalRequests: 12,
  },
  browser: {},
  connection: { effectiveType: '4g', source: 'unavailable' },
  download: {
    bytes: 1_000_000,
    durationMs: 1000,
    mbps: 20,
    samples: 1,
    status: 'measured',
  },
  id: 'speed_test_ai',
  jitter: {
    ms: 4,
    samples: 10,
    status: 'measured',
  },
  latency: {
    method: 'http_timing',
    ms: 30,
    samples: 10,
    status: 'measured',
  },
  limitations: ['http_latency_not_icmp_ping'],
  measuredAt: '2026-06-28T00:00:00.000Z',
  upload: {
    bytes: 500_000,
    durationMs: 1000,
    mbps: 5,
    samples: 1,
    status: 'measured',
  },
};

describe('AI diagnosis client', () => {
  it('returns AI diagnosis when the worker responds with the expected contract', async () => {
    const fetchFn: typeof fetch = async () =>
      Response.json({
        actions: [
          {
            category: 'retry',
            description: 'Use este resultado como referência.',
            priority: 1,
            title: 'Mantenha o teste salvo',
          },
        ],
        confidence: 'high',
        generatedAt: '2026-06-28T00:00:00.000Z',
        id: 'diag_ai',
        limitations: [],
        quality: 'good',
        source: 'ai',
        speed: 'fast',
        stability: 'stable',
        summary: 'Sua conexão está boa para uso comum.',
      });

    const result = await createDiagnosisWithAiFallback(speedTest, { fetchFn });

    expect(result.source).toBe('ai');
    expect(result.diagnosis).toMatchObject({
      quality: 'good',
      source: 'ai',
      speed: 'fast',
      stability: 'stable',
      summary: 'Sua conexão está boa para uso comum.',
    });
  });

  it('returns a fallback diagnosis when the worker is unavailable', async () => {
    const fetchFn: typeof fetch = async () => Response.json({ error: 'AI_WORKER_URL não configurada' }, { status: 503 });

    const result = await createDiagnosisWithAiFallback(speedTest, { fetchFn });

    expect(result.source).toBe('fallback');
    expect(result.diagnosis.source).toBe('fallback');
    expect(result.diagnosis.limitations.some((limitation) => limitation.code === 'ai_unavailable')).toBe(true);
  });
});
