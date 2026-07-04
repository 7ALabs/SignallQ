import type { AdminDiagnosticPayload } from '@shared/contracts';
import { createLocalDiagnosis } from '@shared/diagnosis';
import { calculateJitterMs, calculateMbps } from '@shared/speedtest-metrics';
import { DiagnosticPayload, SpeedtestResult } from '@/types/network';
import type { DiagnosisResult } from '@/types/network';

export interface ApiResponse<T> {
  ok: boolean;
  data?: T;
  error?: string;
}

interface LatencyResponse {
  ok: boolean;
  now: number;
  method: 'http_timing';
  limitations: string[];
}

interface UploadResponse {
  ok: boolean;
  receivedBytes: number;
  receivedAt: number;
}

async function requestJson<T>(input: RequestInfo | URL, init?: RequestInit): Promise<ApiResponse<T>> {
  try {
    const response = await fetch(input, {
      ...init,
      headers: {
        'Content-Type': 'application/json',
        ...init?.headers,
      },
    });
    const contentType = response.headers.get('content-type') ?? '';
    const data = contentType.includes('application/json') ? ((await response.json()) as unknown) : null;
    if (!response.ok) {
      const error = data && typeof data === 'object' && 'error' in data ? String(data.error) : response.statusText;
      return { ok: false, error };
    }
    return { ok: true, data: data as T };
  } catch (error) {
    return { ok: false, error: error instanceof Error ? error.message : 'Falha de rede' };
  }
}

function fillRandomValues(payload: Uint8Array): void {
  const maxChunkBytes = 65_536;

  for (let offset = 0; offset < payload.byteLength; offset += maxChunkBytes) {
    crypto.getRandomValues(payload.subarray(offset, Math.min(offset + maxChunkBytes, payload.byteLength)));
  }
}

export async function runSpeedtestProbe(): Promise<ApiResponse<SpeedtestResult>> {
  const latencySamples: number[] = [];
  for (let sample = 0; sample < 3; sample += 1) {
    const latencyStart = performance.now();
    const latency = await requestJson<LatencyResponse>('/api/speedtest/latency?cacheBust=' + Date.now() + '_' + sample, {
      method: 'GET',
    });
    if (latency.ok) {
      latencySamples.push(Math.round(performance.now() - latencyStart));
    }
  }

  const latencyMs = latencySamples.length > 0
    ? Math.round(latencySamples.reduce((sum, sample) => sum + sample, 0) / latencySamples.length)
    : null;
  const jitter = calculateJitterMs(latencySamples);

  const downloadStart = performance.now();
  const downloadResponse = await fetch('/api/speedtest/download?bytes=524288&cacheBust=' + Date.now(), {
    cache: 'no-store',
  });
  if (!downloadResponse.ok) {
    return { ok: false, error: 'Falha no endpoint de download' };
  }
  const downloadBuffer = await downloadResponse.arrayBuffer();
  const downloadDurationMs = Math.max(performance.now() - downloadStart, 1);
  const downloadMbps = calculateMbps(downloadBuffer.byteLength, downloadDurationMs);

  const uploadPayload = new Uint8Array(256 * 1024);
  fillRandomValues(uploadPayload);
  const uploadStart = performance.now();
  const upload = await fetch('/api/speedtest/upload?cacheBust=' + Date.now(), {
    method: 'POST',
    cache: 'no-store',
    body: uploadPayload,
  });
  if (!upload.ok) {
    return { ok: false, error: 'Falha no endpoint de upload' };
  }
  const uploadBody = (await upload.json().catch(() => null)) as UploadResponse | null;
  const uploadDurationMs = Math.max(performance.now() - uploadStart, 1);
  const uploadMbps = calculateMbps(uploadBody?.receivedBytes ?? uploadPayload.byteLength, uploadDurationMs);

  return {
    ok: true,
    data: {
      latencyMs,
      downloadMbps,
      uploadMbps,
      jitterMs: jitter.ms,
      measuredAt: new Date().toISOString(),
    },
  };
}

export function requestAiDiagnosis(payload: DiagnosticPayload): Promise<ApiResponse<unknown>> {
  return requestJson('/api/ai/diagnostico-conexao', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export async function requestDiagnosisWithFallback(
  payload: DiagnosticPayload,
  speedtest: SpeedtestResult | null,
): Promise<ApiResponse<DiagnosisResult>> {
  const aiResult = await requestAiDiagnosis(payload);
  if (aiResult.ok && aiResult.data && typeof aiResult.data === 'object') {
    return { ok: true, data: aiResult.data as DiagnosisResult };
  }

  const fallback = createLocalDiagnosis({ speedTest: speedtest });
  const response: ApiResponse<DiagnosisResult> = {
    ok: true,
    data: {
      ...fallback,
      source: 'fallback',
      limitations: [
        ...fallback.limitations,
        {
          code: 'ai_unavailable',
          message: aiResult.error ?? 'Analise avancada indisponivel no momento.',
        },
      ],
    },
  };
  if (aiResult.error) {
    response.error = aiResult.error;
  }

  return response;
}

export function sendAdminDiagnostic(payload: AdminDiagnosticPayload): Promise<ApiResponse<{ ok: boolean; id: string }>> {
  return requestJson('/api/admin/ingest', {
    method: 'POST',
    body: JSON.stringify({ kind: 'diagnostic', payload }),
  });
}
