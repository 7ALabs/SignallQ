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
  it('returns AI diagnosis when the worker responds with the real ai-diagnosis-worker schema', async () => {
    // Schema real do Worker (compartilhado com o Android) — não o contrato
    // de UI `DiagnosisResult`. Ver aiResponseMapper.ts.
    const fetchFn: typeof fetch = async () =>
      Response.json({
        acoesRecomendadas: [
          {
            descricao: 'Use este resultado como referência.',
            prioridade: 'alta',
            tipo: 'reteste',
            titulo: 'Mantenha o teste salvo',
          },
        ],
        classificacaoTecnica: {
          estabilidade: { avaliacao: 'boa' },
          velocidade: { avaliacao: 'boa' },
        },
        generatedAt: 1751068800000,
        limitesDaAnalise: [],
        problemaPrincipal: { confianca: 0.9, tipo: 'sem_problema' },
        resumo: 'Sua conexão está boa para uso comum.',
        schemaVersion: '2',
        source: 'cloudflare_ai',
        status: 'bom',
        textoLaudo: 'Sua conexão está boa para uso comum.',
        titulo: 'Conexão boa',
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
