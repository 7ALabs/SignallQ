import { describe, expect, it, vi } from 'vitest';
import { classifySpeedTest } from '../src/features/speedtest/qualityClassifier';
import { runSpeedTestWeb } from '../src/features/speedtest/speedTestRunner';

function jsonResponse(body: unknown, ok = true): Response {
  return new Response(JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    status: ok ? 200 : 500,
  });
}

function byteResponse(bytes: number, ok = true): Response {
  return new Response(new Uint8Array(bytes), {
    headers: { 'Content-Type': 'application/octet-stream' },
    status: ok ? 200 : 500,
  });
}

describe('speedtest runner', () => {
  it('returns a full measured result with HTTP latency, download, upload and inferred loss', async () => {
    let currentTime = 0;
    const fetchFn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      currentTime += url.includes('/latency') ? 20 : 1000;

      if (url.includes('/latency')) {
        return jsonResponse({ ok: true, method: 'http_timing' });
      }

      if (url.includes('/download')) {
        return byteResponse(1_000_000);
      }

      if (url.includes('/upload')) {
        const body = init?.body as Uint8Array;
        return jsonResponse({ ok: true, receivedBytes: body.byteLength });
      }

      return jsonResponse({ error: 'unexpected' }, false);
    });

    const progress: string[] = [];
    const result = await runSpeedTestWeb({
      fetchFn,
      now: () => currentTime,
      onProgress: (event) => progress.push(`${event.phase}:${event.status}`),
      uploadBytes: 100_000,
    });

    if (!result.result) throw new Error('expected result');

    expect(result.status).toBe('success');
    expect(result.result.latency).toMatchObject({ method: 'http_timing', samples: 15, status: 'measured' });
    expect(result.result.download).toMatchObject({ bytes: 1_000_000, mbps: 8, status: 'measured' });
    expect(result.result.upload).toMatchObject({ bytes: 100_000, mbps: 0.8, status: 'measured' });
    expect(result.result.availability).toMatchObject({
      failedRequests: 0,
      perceivedLossPercent: 0,
      status: 'inferred',
      totalRequests: 17,
    });
    expect(result.result.limitations).toContain('http_latency_not_icmp_ping');
    expect(progress).toContain('download:running');
    expect(progress).toContain('complete:success');
  });

  it('keeps a partial result when download fails instead of dropping latency evidence', async () => {
    let currentTime = 0;
    const fetchFn = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      currentTime += 25;

      if (url.includes('/latency')) {
        return jsonResponse({ ok: true, method: 'http_timing' });
      }

      if (url.includes('/download')) {
        return byteResponse(0, false);
      }

      return jsonResponse({ ok: true, receivedBytes: 64_000 });
    });

    const result = await runSpeedTestWeb({ fetchFn, now: () => currentTime, uploadBytes: 64_000 });

    if (!result.result) throw new Error('expected result');

    expect(result.status).toBe('partial');
    expect(result.result.latency.status).toBe('measured');
    expect(result.result.download.status).toBe('failed');
    expect(result.result.availability.failedRequests).toBe(1);
  });

  it('classifies slow or unstable speedtest results as bad', async () => {
    let currentTime = 0;
    const fetchFn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      currentTime += url.includes('/download') ? 3000 : 200;

      if (url.includes('/latency')) return jsonResponse({ ok: true, method: 'http_timing' });
      if (url.includes('/download')) return byteResponse(1_000_000);

      const body = init?.body as Uint8Array;
      return jsonResponse({ ok: true, receivedBytes: body.byteLength });
    });

    const result = await runSpeedTestWeb({ fetchFn, now: () => currentTime, uploadBytes: 32_000 });
    if (!result.result) throw new Error('expected result');

    const quality = classifySpeedTest(result.result);

    expect(quality.overall).toBe('bad');
    expect(quality.summary).toContain('lentidão');
  });

  it('returns canceled when the caller aborts before the first probe', async () => {
    const controller = new AbortController();
    controller.abort();

    const progress: string[] = [];
    const result = await runSpeedTestWeb({
      fetchFn: vi.fn(),
      onProgress: (event) => progress.push(`${event.phase}:${event.status}`),
      signal: controller.signal,
    });

    expect(result).toEqual({ result: null, status: 'canceled' });
    expect(progress).toContain('canceled:canceled');
  });

  it('returns an error result when network probes time out without inventing metrics', async () => {
    const fetchFn = vi.fn((_input: RequestInfo | URL, init?: RequestInit) => {
      return new Promise<Response>((_resolve, reject) => {
        init?.signal?.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), {
          once: true,
        });
      });
    });

    const result = await runSpeedTestWeb({
      fetchFn,
      latencySampleCount: 2,
      timeoutMs: 1,
      uploadRetryCount: 1,
    });

    if (!result.result) throw new Error('expected error result');

    expect(result.status).toBe('error');
    expect(result.errorMessage).toBe('speedtest_failed');
    expect(result.result.download.mbps).toBeNull();
    expect(result.result.upload.mbps).toBeNull();
    expect(result.result.latency.ms).toBeNull();
    expect(result.result.availability.failedRequests).toBe(4);
  });

  it('retries upload before returning a partial result', async () => {
    let currentTime = 0;
    let uploadAttempts = 0;
    const fetchFn = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input);
      currentTime += url.includes('/latency') ? 20 : 1000;

      if (url.includes('/latency')) return jsonResponse({ ok: true, method: 'http_timing' });
      if (url.includes('/download')) return byteResponse(1_000_000);

      uploadAttempts += 1;
      if (uploadAttempts < 3) return jsonResponse({ ok: false }, false);

      const body = init?.body as Uint8Array;
      return jsonResponse({ ok: true, receivedBytes: body.byteLength });
    });

    const result = await runSpeedTestWeb({
      fetchFn,
      now: () => currentTime,
      uploadBytes: 50_000,
      uploadRetryCount: 3,
    });

    if (!result.result) throw new Error('expected result');

    expect(result.status).toBe('success');
    expect(result.result.upload).toMatchObject({ bytes: 50_000, status: 'measured' });
    expect(result.result.availability.failedRequests).toBe(2);
    expect(uploadAttempts).toBe(3);
  });

  it('marks upload as not available only when explicitly skipped', async () => {
    let currentTime = 0;
    const fetchFn = vi.fn(async (input: RequestInfo | URL) => {
      const url = String(input);
      currentTime += 20;
      if (url.includes('/latency')) return jsonResponse({ ok: true, method: 'http_timing' });
      if (url.includes('/download')) return byteResponse(1_000_000);
      return jsonResponse({ error: 'unexpected' }, false);
    });

    const result = await runSpeedTestWeb({ fetchFn, now: () => currentTime, skipUpload: true });

    if (!result.result) throw new Error('expected result');

    expect(result.status).toBe('partial');
    expect(result.result.upload.status).toBe('not_available');
    expect(result.result.limitations).toContain('upload_endpoint_unavailable');
  });
});
