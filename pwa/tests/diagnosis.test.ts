import { describe, expect, it } from 'vitest';
import {
  buildSummary,
  classifySpeed,
  classifyStability,
  createLocalDiagnosis,
  proxyAiDiagnosis,
} from '../functions/_modules/diagnosis';

describe('diagnosis module', () => {
  it('classifies speed using the PWA diagnosis contract thresholds', () => {
    expect(classifySpeed(null)).toBe('unknown');
    expect(classifySpeed(9.9)).toBe('slow');
    expect(classifySpeed(25)).toBe('ok');
    expect(classifySpeed(100)).toBe('fast');
  });

  it('classifies stability from HTTP latency and jitter', () => {
    expect(classifyStability(null, null)).toBe('unknown');
    expect(classifyStability(151, 10)).toBe('unstable');
    expect(classifyStability(30, 41)).toBe('unstable');
    expect(classifyStability(30, 10)).toBe('stable');
  });

  it('creates a local fallback diagnosis without inventing absent jitter', () => {
    const diagnosis = createLocalDiagnosis({
      speedTest: {
        latencyMs: 30,
        downloadMbps: 20,
        uploadMbps: 5,
        measuredAt: '2026-06-26T00:00:00.000Z',
      },
    });

    expect(diagnosis.source).toBe('local');
    expect(diagnosis.speed).toBe('ok');
    expect(diagnosis.confidence).toBe('medium');
    expect(diagnosis.limitations.some((limitation) => limitation.code === 'jitter_not_measured')).toBe(true);
  });

  it('keeps summaries short and actionable', () => {
    expect(buildSummary('unknown', 'unknown')).toContain('Não foi possível medir');
    expect(buildSummary('slow', 'stable')).toContain('velocidade medida está baixa');
    expect(buildSummary('fast', 'unstable')).toContain('instável');
  });

  it('proxies AI diagnosis through the configured worker with no-store response', async () => {
    let upstreamUrl = '';
    const fetcher: typeof fetch = async (input) => {
      upstreamUrl = String(input);
      return Response.json({ ok: true, source: 'ai' });
    };

    const response = await proxyAiDiagnosis(
      new Request('https://pwa.local/api/ai/diagnostico-conexao', {
        method: 'POST',
        body: JSON.stringify({ schemaVersion: 'pwa_foundation_v1' }),
      }),
      'https://ai.worker.dev',
      { fetcher },
    );

    expect(upstreamUrl).toBe('https://ai.worker.dev/api/ai/diagnostico-conexao');
    expect(response.headers.get('Cache-Control')).toBe('no-store');
    await expect(response.json()).resolves.toEqual({ ok: true, source: 'ai' });
  });
});
