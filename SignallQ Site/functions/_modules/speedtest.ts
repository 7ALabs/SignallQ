import { jsonResponse } from '../_shared/http';
import { calculateJitterMs, calculateMbps, MIN_JITTER_SAMPLES } from '../../shared/speedtest-metrics';

export const MIN_DOWNLOAD_BYTES = 64 * 1024;
export const DEFAULT_DOWNLOAD_BYTES = 512 * 1024;
export const MAX_DOWNLOAD_BYTES = 2 * 1024 * 1024;
export const MAX_UPLOAD_BYTES = 4 * 1024 * 1024;
export { calculateJitterMs, calculateMbps, MIN_JITTER_SAMPLES };

export function clampByteLength(value: number, minBytes: number, maxBytes: number): number {
  if (!Number.isFinite(value)) return minBytes;
  return Math.min(Math.max(Math.trunc(value), minBytes), maxBytes);
}

export function parseDownloadBytes(request: Request): number {
  const url = new URL(request.url);
  const requestedBytes = Number(url.searchParams.get('bytes') ?? DEFAULT_DOWNLOAD_BYTES);
  return clampByteLength(requestedBytes, MIN_DOWNLOAD_BYTES, MAX_DOWNLOAD_BYTES);
}

export function createLatencyPayload(now = Date.now()) {
  return {
    ok: true,
    now,
    method: 'http_timing' as const,
    limitations: ['http_latency_not_icmp_ping'],
  };
}

export function createDownloadResponse(byteLength: number): Response {
  const payload = new Uint8Array(byteLength);

  return new Response(payload, {
    headers: {
      'Content-Type': 'application/octet-stream',
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      'Content-Length': String(byteLength),
      'X-SignallQ-Speedtest-Bytes': String(byteLength),
    },
  });
}

export async function createUploadResult(request: Request): Promise<Response> {
  const bytes = await request.arrayBuffer();
  if (bytes.byteLength > MAX_UPLOAD_BYTES) {
    return jsonResponse({ error: 'Payload acima do limite do speedtest PWA.' }, { status: 413 });
  }

  return jsonResponse({
    ok: true,
    receivedBytes: bytes.byteLength,
    receivedAt: Date.now(),
    limitations: ['browser_measurement_may_vary'],
  });
}
