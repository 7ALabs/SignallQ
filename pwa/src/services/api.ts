import { DiagnosticPayload, SpeedtestResult } from '@/types/network';

interface ApiResponse<T> {
  ok: boolean;
  data?: T;
  error?: string;
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
    const data = (await response.json()) as unknown;
    if (!response.ok) {
      const error = data && typeof data === 'object' && 'error' in data ? String(data.error) : response.statusText;
      return { ok: false, error };
    }
    return { ok: true, data: data as T };
  } catch (error) {
    return { ok: false, error: error instanceof Error ? error.message : 'Falha de rede' };
  }
}

export async function runSpeedtestProbe(): Promise<ApiResponse<SpeedtestResult>> {
  const latencyStart = performance.now();
  const latency = await requestJson<{ ok: boolean; now: number }>('/api/speedtest/latency?cacheBust=' + Date.now(), {
    method: 'GET',
  });
  const latencyMs = latency.ok ? Math.round(performance.now() - latencyStart) : null;

  const downloadStart = performance.now();
  const downloadResponse = await fetch('/api/speedtest/download?bytes=524288&cacheBust=' + Date.now(), {
    cache: 'no-store',
  });
  const downloadBuffer = await downloadResponse.arrayBuffer();
  const downloadSeconds = Math.max((performance.now() - downloadStart) / 1000, 0.001);
  const downloadMbps = (downloadBuffer.byteLength * 8) / downloadSeconds / 1_000_000;

  const uploadPayload = new Uint8Array(256 * 1024);
  crypto.getRandomValues(uploadPayload);
  const uploadStart = performance.now();
  const upload = await fetch('/api/speedtest/upload?cacheBust=' + Date.now(), {
    method: 'POST',
    cache: 'no-store',
    body: uploadPayload,
  });
  if (!upload.ok) {
    return { ok: false, error: 'Falha no endpoint de upload' };
  }
  const uploadSeconds = Math.max((performance.now() - uploadStart) / 1000, 0.001);
  const uploadMbps = (uploadPayload.byteLength * 8) / uploadSeconds / 1_000_000;

  return {
    ok: true,
    data: {
      latencyMs,
      downloadMbps: Math.round(downloadMbps * 10) / 10,
      uploadMbps: Math.round(uploadMbps * 10) / 10,
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

export function sendAdminDiagnostic(payload: Record<string, unknown>): Promise<ApiResponse<{ ok: boolean; id: string }>> {
  return requestJson('/api/admin/ingest', {
    method: 'POST',
    body: JSON.stringify({ kind: 'diagnostic', payload }),
  });
}
