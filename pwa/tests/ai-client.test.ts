import { describe, expect, it } from 'vitest';
import { requestAiDiagnosis } from '../src/features/diagnosis/aiClient';
import type { DiagnosisResult, DiagnosticPayload } from '../shared/contracts';

const localDiagnosis: DiagnosisResult = {
  actions: [
    {
      category: 'retry',
      description: 'Repita o teste.',
      priority: 1,
      title: 'Testar novamente',
    },
  ],
  confidence: 'medium',
  generatedAt: '2026-06-28T00:00:00.000Z',
  id: 'diag_local',
  limitations: [{ code: 'http_latency_not_icmp_ping', message: 'Latência HTTP.' }],
  quality: 'attention',
  source: 'local',
  speed: 'ok',
  stability: 'stable',
  summary: 'Diagnóstico local.',
};

const payload: DiagnosticPayload = {
  connectionType: '4g',
  metricasAtuais: {
    downloadMbps: 20,
    latenciaMs: 30,
    uploadMbps: 5,
  },
  schemaVersion: 'pwa_foundation_v1',
  source: 'pwa',
};

describe('AI diagnosis client', () => {
  it('normalizes the worker response to the PWA diagnosis contract', async () => {
    const fetchFn: typeof fetch = async () =>
      Response.json({
        status: 'bom',
        resumo: 'Sua conexão está boa para uso comum.',
        classificacaoTecnica: {
          velocidade: { avaliacao: 'boa' },
          estabilidade: { avaliacao: 'boa' },
        },
        problemaPrincipal: { confianca: 0.82 },
        acoesRecomendadas: [
          {
            titulo: 'Mantenha o teste salvo',
            descricao: 'Use este resultado como referência.',
            prioridade: 'media',
            tipo: 'observacao',
          },
        ],
        limitesDaAnalise: ['Sem RSSI no navegador.'],
        modeloIa: { textoRodape: 'Motor de análise: SignallQ IA' },
      });

    const result = await requestAiDiagnosis({ fetchFn, localDiagnosis, payload });

    expect(result.source).toBe('ai');
    expect(result.diagnosis).toMatchObject({
      quality: 'good',
      source: 'ai',
      speed: 'fast',
      stability: 'stable',
      summary: 'Sua conexão está boa para uso comum.',
    });
    expect(result.diagnosis.actions[0]).toMatchObject({ priority: 2, title: 'Mantenha o teste salvo' });
  });

  it('returns a fallback diagnosis when the worker is unavailable', async () => {
    const fetchFn: typeof fetch = async () => Response.json({ error: 'AI_WORKER_URL não configurada' }, { status: 503 });

    const result = await requestAiDiagnosis({ fetchFn, localDiagnosis, payload });

    expect(result.source).toBe('fallback');
    expect(result.diagnosis.source).toBe('fallback');
    expect(result.diagnosis.limitations.some((limitation) => limitation.code === 'ai_unavailable')).toBe(true);
  });
});
