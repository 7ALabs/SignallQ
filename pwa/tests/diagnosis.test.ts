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
        downloadMbps: 30,
        uploadMbps: 5,
        measuredAt: '2026-06-26T00:00:00.000Z',
      },
    });

    expect(diagnosis.source).toBe('local');
    expect(diagnosis.speed).toBe('ok');
    expect(diagnosis.confidence).toBe('medium');
    expect(diagnosis.limitations.some((limitation) => limitation.code === 'jitter_not_measured')).toBe(true);
  });

  it('treats 30 Mbps as a healthy connection like Android (old 10-50 Mbps PWA-only band no longer forces attention)', () => {
    const diagnosis = createLocalDiagnosis({
      speedTest: {
        latencyMs: 20,
        jitterMs: 4,
        downloadMbps: 30,
        uploadMbps: 10,
        measuredAt: '2026-06-26T00:00:00.000Z',
      },
    });

    expect(diagnosis.speed).toBe('ok');
    expect(diagnosis.quality).toBe('good');
  });

  it('flags quality as bad when upload is zero, matching Android IN-NORMAL-04Z', () => {
    const diagnosis = createLocalDiagnosis({
      speedTest: {
        latencyMs: 20,
        jitterMs: 4,
        downloadMbps: 50,
        uploadMbps: 0,
        measuredAt: '2026-06-26T00:00:00.000Z',
      },
    });

    expect(diagnosis.quality).toBe('bad');
    expect(diagnosis.summary).toContain('upload medido foi 0 Mbps');
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
