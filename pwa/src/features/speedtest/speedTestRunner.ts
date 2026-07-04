import type {
  AvailabilityMetric,
  BrowserConnectionInfo,
  BrowserInfo,
  DownloadMetric,
  JitterMetric,
  LatencyMetric,
  SpeedTestResult,
  UploadMetric,
} from '@shared/contracts';
import { calculateJitterMs, calculateMbps } from '@shared/speedtest-metrics';
import { SpeedtestPhase } from '@/types/network';
import type { SpeedTestProgress, SpeedTestRunnerOptions, SpeedTestRunnerResult } from './speedTestTypes';

const DEFAULT_LATENCY_SAMPLE_COUNT = 15;
const DEFAULT_TIMEOUT_MS = 15_000;

// GH#436: download/upload eram uma unica requisicao pequena (1MB/512KB). Isso mede
// sobretudo o tempo de handshake/TLS/TCP slow-start da conexao, nao o throughput
// sustentado da rede, e por isso o resultado divergia (muitas vezes bem abaixo) do
// SpeedTest Android, que usa multiplas conexoes paralelas sustentadas por uma janela
// de tempo com aquecimento descartado (ExecutorSpeedtestCloudflare). A correcao aqui
// reproduz a mesma lógica (streams paralelos + janela + warmup) dentro do que o
// browser suporta via fetch, sem estimativa nativa por RSSI/ICMP.
const DEFAULT_DOWNLOAD_DURATION_MS = 8_000;
const DEFAULT_DOWNLOAD_WARMUP_MS = 1_000;
const DEFAULT_DOWNLOAD_STREAMS = 4;
const DEFAULT_DOWNLOAD_CHUNK_BYTES = 1_500_000;
const DEFAULT_UPLOAD_DURATION_MS = 8_000;
const DEFAULT_UPLOAD_WARMUP_MS = 1_000;
const DEFAULT_UPLOAD_STREAMS = 4;
const DEFAULT_UPLOAD_CHUNK_BYTES = 512 * 1024;
const MAX_WORKER_ITERATIONS = 500;

interface LatencyResponse {
  method?: 'http_timing';
  ok?: boolean;
}

interface UploadResponse {
  receivedBytes?: number;
}

function emit(onProgress: SpeedTestRunnerOptions['onProgress'], progress: SpeedTestProgress): void {
  onProgress?.(progress);
}

class SpeedTestCanceledError extends Error {
  constructor() {
    super('speedtest_canceled');
  }
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError';
}

function isCanceledError(error: unknown): boolean {
  return error instanceof SpeedTestCanceledError;
}

function assertNotCanceled(signal: AbortSignal | undefined): void {
  if (signal?.aborted) throw new SpeedTestCanceledError();
}

async function fetchWithTimeout(
  fetchFn: typeof fetch,
  input: RequestInfo | URL,
  init: RequestInit,
  timeoutMs: number,
  parentSignal?: AbortSignal,
): Promise<Response> {
  assertNotCanceled(parentSignal);

  const controller = new AbortController();
  let timedOut = false;
  const timeout = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, timeoutMs);
  const onAbort = () => controller.abort();
  parentSignal?.addEventListener('abort', onAbort, { once: true });

  try {
    return await fetchFn(input, { ...init, signal: controller.signal });
  } catch (error) {
    if (parentSignal?.aborted) throw new SpeedTestCanceledError();
    if (timedOut || isAbortError(error)) throw new Error('request_timeout');
    throw error;
  } finally {
    clearTimeout(timeout);
    parentSignal?.removeEventListener('abort', onAbort);
  }
}

function median(values: number[]): number | null {
  if (values.length === 0) return null;
  const sorted = [...values].sort((a, b) => a - b);
  const middle = Math.floor(sorted.length / 2);
  const value =
    sorted.length % 2 === 0
      ? (sorted[middle - 1]! + sorted[middle]!) / 2
      : sorted[middle]!;
  return Math.round(value);
}

function perceivedLoss(failedRequests: number, totalRequests: number): number | null {
  if (totalRequests <= 0) return null;
  return Math.round((failedRequests / totalRequests) * 1000) / 10;
}

function createRandomPayload(bytes: number): Uint8Array {
  const payload = new Uint8Array(bytes);
  const maxChunkBytes = 65_536;

  for (let offset = 0; offset < payload.byteLength; offset += maxChunkBytes) {
    crypto.getRandomValues(payload.subarray(offset, Math.min(offset + maxChunkBytes, payload.byteLength)));
  }

  return payload;
}

function readBrowserInfo(): BrowserInfo {
  if (typeof navigator === 'undefined') return {};

  const browserInfo: BrowserInfo = {
    language: navigator.language,
    platform: navigator.platform,
    userAgent: navigator.userAgent,
  };

  if (typeof window !== 'undefined') {
    browserInfo.viewport = {
      width: window.innerWidth,
      height: window.innerHeight,
    };
  }

  return browserInfo;
}

function readConnectionInfo(): BrowserConnectionInfo {
  if (typeof navigator === 'undefined' || !('connection' in navigator)) {
    return { source: 'unavailable' };
  }

  const connection = navigator.connection as {
    downlink?: number;
    effectiveType?: string;
    rtt?: number;
    saveData?: boolean;
  };

  const connectionInfo: BrowserConnectionInfo = { source: 'network_information_api' };

  if (connection.downlink !== undefined) connectionInfo.downlink = connection.downlink;
  if (connection.effectiveType !== undefined) connectionInfo.effectiveType = connection.effectiveType;
  if (connection.rtt !== undefined) connectionInfo.rtt = connection.rtt;
  if (connection.saveData !== undefined) connectionInfo.saveData = connection.saveData;

  return connectionInfo;
}

async function measureLatency(
  fetchFn: typeof fetch,
  now: () => number,
  options: {
    latencySampleCount: number;
    signal: AbortSignal | undefined;
    timeoutMs: number;
  },
): Promise<{
  failedRequests: number;
  metric: LatencyMetric;
  samples: number[];
  totalRequests: number;
}> {
  const samples: number[] = [];
  let failedRequests = 0;

  for (let index = 0; index < options.latencySampleCount; index += 1) {
    assertNotCanceled(options.signal);
    const startedAt = now();
    try {
      const response = await fetchWithTimeout(
        fetchFn,
        `/api/speedtest/latency?cacheBust=${Date.now()}_${index}`,
        {
          cache: 'no-store',
          method: 'GET',
        },
        options.timeoutMs,
        options.signal,
      );
      const data = (await response.json().catch(() => null)) as LatencyResponse | null;
      if (!response.ok || data?.method !== 'http_timing') {
        failedRequests += 1;
      } else {
        samples.push(Math.round(now() - startedAt));
      }
    } catch (error) {
      if (isCanceledError(error)) throw error;
      failedRequests += 1;
    }
  }

  return {
    failedRequests,
    metric: {
      method: 'http_timing',
      ms: median(samples),
      samples: samples.length,
      status: samples.length > 0 ? 'measured' : 'failed',
    },
    samples,
    totalRequests: options.latencySampleCount,
  };
}

/**
 * Resume os bytes transferidos durante a janela de medição, descartando o warmup
 * inicial quando há amostras suficientes depois dele (mesmo critério do Android:
 * throughput calculado sobre a janela estável, não sobre o teste inteiro).
 */
function summarizeThroughputWindow(
  bytesTotal: number,
  bytesAfterWarmup: number,
  elapsedTotalMs: number,
  warmupMs: number,
  successfulRequests: number,
): { bytes: number; durationMs: number; mbps: number | null } {
  const hasWarmupWindow = elapsedTotalMs > warmupMs && bytesAfterWarmup > 0;
  if (hasWarmupWindow) {
    const durationMs = Math.max(elapsedTotalMs - warmupMs, 1);
    return { bytes: bytesAfterWarmup, durationMs, mbps: calculateMbps(bytesAfterWarmup, durationMs) };
  }
  if (successfulRequests > 0) {
    const durationMs = Math.max(elapsedTotalMs, 1);
    return { bytes: bytesTotal, durationMs, mbps: calculateMbps(bytesTotal, durationMs) };
  }
  return { bytes: 0, durationMs: Math.max(elapsedTotalMs, 1), mbps: null };
}

async function measureDownload(
  fetchFn: typeof fetch,
  now: () => number,
  options: {
    chunkBytes: number;
    durationMs: number;
    signal: AbortSignal | undefined;
    streams: number;
    timeoutMs: number;
    warmupMs: number;
  },
): Promise<{
  failedRequests: number;
  metric: DownloadMetric;
  totalRequests: number;
}> {
  assertNotCanceled(options.signal);
  const startedAt = now();
  const deadline = startedAt + options.durationMs;
  let bytesTotal = 0;
  let bytesAfterWarmup = 0;
  let successfulRequests = 0;
  let failedRequests = 0;
  let requestSeq = 0;

  async function worker(): Promise<void> {
    let iterations = 0;
    while (now() < deadline && iterations < MAX_WORKER_ITERATIONS) {
      iterations += 1;
      requestSeq += 1;
      assertNotCanceled(options.signal);

      try {
        const response = await fetchWithTimeout(
          fetchFn,
          `/api/speedtest/download?bytes=${options.chunkBytes}&cacheBust=${Date.now()}_${requestSeq}`,
          {
            cache: 'no-store',
            method: 'GET',
          },
          options.timeoutMs,
          options.signal,
        );
        if (!response.ok) throw new Error('download_failed');
        const buffer = await response.arrayBuffer();
        const elapsed = now() - startedAt;
        bytesTotal += buffer.byteLength;
        successfulRequests += 1;
        if (elapsed >= options.warmupMs) bytesAfterWarmup += buffer.byteLength;
      } catch (error) {
        if (isCanceledError(error)) throw error;
        failedRequests += 1;
      }
    }
  }

  await Promise.all(Array.from({ length: options.streams }, () => worker()));

  const elapsedTotalMs = Math.max(now() - startedAt, 1);
  const window = summarizeThroughputWindow(bytesTotal, bytesAfterWarmup, elapsedTotalMs, options.warmupMs, successfulRequests);

  return {
    failedRequests,
    metric: {
      bytes: window.bytes,
      durationMs: window.durationMs,
      mbps: window.mbps,
      samples: successfulRequests,
      status: successfulRequests > 0 ? 'measured' : 'failed',
    },
    totalRequests: successfulRequests + failedRequests,
  };
}

async function measureUpload(
  fetchFn: typeof fetch,
  now: () => number,
  options: {
    chunkBytes: number;
    durationMs: number;
    signal: AbortSignal | undefined;
    skipUpload: boolean | undefined;
    streams: number;
    timeoutMs: number;
    warmupMs: number;
  },
): Promise<{
  failedRequests: number;
  metric: UploadMetric;
  totalRequests: number;
}> {
  if (options.skipUpload) {
    return {
      failedRequests: 0,
      metric: {
        bytes: 0,
        durationMs: 0,
        mbps: null,
        samples: 0,
        status: 'not_available',
      },
      totalRequests: 0,
    };
  }

  assertNotCanceled(options.signal);
  const payload = createRandomPayload(options.chunkBytes);
  const startedAt = now();
  const deadline = startedAt + options.durationMs;
  let bytesTotal = 0;
  let bytesAfterWarmup = 0;
  let successfulRequests = 0;
  let failedRequests = 0;
  let requestSeq = 0;

  async function worker(): Promise<void> {
    let iterations = 0;
    while (now() < deadline && iterations < MAX_WORKER_ITERATIONS) {
      iterations += 1;
      requestSeq += 1;
      assertNotCanceled(options.signal);

      try {
        const response = await fetchWithTimeout(
          fetchFn,
          `/api/speedtest/upload?cacheBust=${Date.now()}_${requestSeq}`,
          {
            body: payload,
            cache: 'no-store',
            method: 'POST',
          },
          options.timeoutMs,
          options.signal,
        );
        if (!response.ok) throw new Error('upload_failed');

        const data = (await response.json().catch(() => null)) as UploadResponse | null;
        const receivedBytes = data?.receivedBytes ?? payload.byteLength;
        if (receivedBytes <= 0) {
          failedRequests += 1;
          continue;
        }

        const elapsed = now() - startedAt;
        bytesTotal += receivedBytes;
        successfulRequests += 1;
        if (elapsed >= options.warmupMs) bytesAfterWarmup += receivedBytes;
      } catch (error) {
        if (isCanceledError(error)) throw error;
        failedRequests += 1;
      }
    }
  }

  await Promise.all(Array.from({ length: options.streams }, () => worker()));

  const elapsedTotalMs = Math.max(now() - startedAt, 1);
  const window = summarizeThroughputWindow(bytesTotal, bytesAfterWarmup, elapsedTotalMs, options.warmupMs, successfulRequests);

  return {
    failedRequests,
    metric: {
      bytes: window.bytes,
      durationMs: window.durationMs,
      mbps: window.mbps,
      samples: successfulRequests,
      status: successfulRequests > 0 ? 'measured' : 'failed',
    },
    totalRequests: successfulRequests + failedRequests,
  };
}

export async function runSpeedTestWeb(options: SpeedTestRunnerOptions = {}): Promise<SpeedTestRunnerResult> {
  const fetchFn = options.fetchFn ?? fetch;
  const now = options.now ?? (() => performance.now());
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;
  const latencySampleCount = options.latencySampleCount ?? DEFAULT_LATENCY_SAMPLE_COUNT;
  const downloadDurationMs = options.downloadDurationMs ?? DEFAULT_DOWNLOAD_DURATION_MS;
  const downloadWarmupMs = options.downloadWarmupMs ?? DEFAULT_DOWNLOAD_WARMUP_MS;
  const downloadStreams = options.downloadStreams ?? DEFAULT_DOWNLOAD_STREAMS;
  const downloadChunkBytes = options.downloadChunkBytes ?? DEFAULT_DOWNLOAD_CHUNK_BYTES;
  const uploadDurationMs = options.uploadDurationMs ?? DEFAULT_UPLOAD_DURATION_MS;
  const uploadWarmupMs = options.uploadWarmupMs ?? DEFAULT_UPLOAD_WARMUP_MS;
  const uploadStreams = options.uploadStreams ?? DEFAULT_UPLOAD_STREAMS;
  const uploadChunkBytes = options.uploadChunkBytes ?? DEFAULT_UPLOAD_CHUNK_BYTES;
  const limitations = [
    'http_latency_not_icmp_ping',
    'packet_loss_not_directly_measured',
    'browser_measurement_may_vary',
    'wifi_signal_not_available_on_web',
  ];

  if (readConnectionInfo().source === 'unavailable') {
    limitations.push('network_information_api_unavailable');
  }

  if (options.skipUpload) limitations.push('upload_endpoint_unavailable');

  try {
    emit(options.onProgress, {
      phase: SpeedtestPhase.Latency,
      status: 'running',
      message: 'Medindo latência HTTP...',
    });
    const latency = await measureLatency(fetchFn, now, {
      latencySampleCount,
      signal: options.signal,
      timeoutMs,
    });
    const jitterResult = calculateJitterMs(latency.samples);
    const jitter: JitterMetric = {
      ms: jitterResult.ms,
      samples: jitterResult.samples,
      status: jitterResult.status,
    };

    emit(options.onProgress, {
      phase: SpeedtestPhase.Download,
      status: 'running',
      message: 'Medindo download...',
    });
    const download = await measureDownload(fetchFn, now, {
      chunkBytes: downloadChunkBytes,
      durationMs: downloadDurationMs,
      signal: options.signal,
      streams: downloadStreams,
      timeoutMs,
      warmupMs: downloadWarmupMs,
    });

    emit(options.onProgress, {
      phase: SpeedtestPhase.Upload,
      status: 'running',
      message: options.skipUpload ? 'Upload indisponível neste ambiente.' : 'Medindo upload...',
    });
    const upload = await measureUpload(fetchFn, now, {
      chunkBytes: uploadChunkBytes,
      durationMs: uploadDurationMs,
      signal: options.signal,
      skipUpload: options.skipUpload,
      streams: uploadStreams,
      timeoutMs,
      warmupMs: uploadWarmupMs,
    });

    const failedRequests = latency.failedRequests + download.failedRequests + upload.failedRequests;
    const totalRequests = latency.totalRequests + download.totalRequests + upload.totalRequests;
    const availability: AvailabilityMetric = {
      failedRequests,
      perceivedLossPercent: perceivedLoss(failedRequests, totalRequests),
      status: totalRequests > 0 ? 'inferred' : 'not_measured',
      totalRequests,
    };
    const measuredAny =
      latency.metric.status === 'measured' || download.metric.status === 'measured' || upload.metric.status === 'measured';
    const status =
      latency.metric.status === 'measured' && download.metric.status === 'measured' && upload.metric.status === 'measured'
        ? 'success'
        : measuredAny
          ? 'partial'
          : 'error';

    const result: SpeedTestResult = {
      availability,
      browser: readBrowserInfo(),
      connection: readConnectionInfo(),
      download: download.metric,
      id: `speed_${Date.now().toString(36)}`,
      jitter,
      latency: latency.metric,
      limitations,
      measuredAt: new Date().toISOString(),
      upload: upload.metric,
    };

    emit(options.onProgress, {
      phase: status === 'success' ? SpeedtestPhase.Complete : status === 'partial' ? SpeedtestPhase.Partial : SpeedtestPhase.Error,
      status,
      message:
        status === 'success'
          ? 'Teste concluído.'
          : status === 'partial'
            ? 'Teste parcial concluído.'
            : 'Não foi possível medir a conexão.',
    });

    if (status === 'error') {
      return { errorMessage: 'speedtest_failed', result, status };
    }

    return { result, status };
  } catch (error) {
    if (isCanceledError(error)) {
      emit(options.onProgress, {
        phase: SpeedtestPhase.Canceled,
        status: 'canceled',
        message: 'Teste cancelado.',
      });
      return { result: null, status: 'canceled' };
    }

    throw error;
  }
}
