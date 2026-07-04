import type { SpeedTestResult } from '@shared/contracts';

export type SpeedQuality = 'fast' | 'ok' | 'slow' | 'unknown';
export type StabilityQuality = 'stable' | 'attention' | 'unstable' | 'unknown';
export type OverallQuality = 'good' | 'attention' | 'bad' | 'unknown';
export type UsageVerdict = 'good' | 'acceptable' | 'poor' | 'unknown';
export type PrimaryBottleneck = 'none' | 'latency' | 'upload' | 'bufferbloat' | 'packetLoss' | 'unknown';

export interface SpeedTestQuality {
  bottleneck: PrimaryBottleneck;
  overall: OverallQuality;
  speed: SpeedQuality;
  stability: StabilityQuality;
  summary: string;
  usage: {
    gaming: UsageVerdict;
    streaming: UsageVerdict;
    videoCall: UsageVerdict;
  };
}

// Thresholds de download/latência/jitter alinhados a `shared/diagnosis.ts` e ao motor
// Android (InternetDiagnosticEngine + MetricClassifier — GH#438). Não diverja destes
// números sem atualizar os dois lados: essa duplicação foi exatamente o que causou a
// divergência original entre PWA e Android.
export function classifySpeed(downloadMbps: number | null): SpeedQuality {
  if (downloadMbps == null) return 'unknown';
  if (downloadMbps < 25) return 'slow';
  if (downloadMbps < 100) return 'ok';
  return 'fast';
}

export function classifyStability(latencyMs: number | null, jitterMs: number | null): StabilityQuality {
  if (latencyMs == null && jitterMs == null) return 'unknown';
  if ((latencyMs != null && latencyMs > 100) || (jitterMs != null && jitterMs > 20)) return 'unstable';
  if ((latencyMs != null && latencyMs > 60) || (jitterMs != null && jitterMs > 10)) return 'attention';
  return 'stable';
}

function verdictFromAndroidThresholds(
  downloadMbps: number | null,
  uploadMbps: number | null,
  latencyMs: number | null,
  jitterMs: number | null,
  packetLossPercent: number | null,
): SpeedTestQuality['usage'] {
  if (
    downloadMbps == null ||
    uploadMbps == null ||
    latencyMs == null ||
    jitterMs == null ||
    packetLossPercent == null
  ) {
    return { gaming: 'unknown', streaming: 'unknown', videoCall: 'unknown' };
  }

  const streaming: UsageVerdict =
    downloadMbps >= 25 && latencyMs <= 200 && jitterMs <= 50 && packetLossPercent <= 2
      ? 'good'
      : downloadMbps >= 15 && latencyMs <= 500 && jitterMs <= 100 && packetLossPercent <= 5
        ? 'acceptable'
        : 'poor';
  const gaming: UsageVerdict =
    downloadMbps >= 10 && uploadMbps >= 3 && latencyMs <= 50 && jitterMs <= 15 && packetLossPercent <= 0.5
      ? 'good'
      : downloadMbps >= 5 && uploadMbps >= 1 && latencyMs <= 100 && jitterMs <= 30 && packetLossPercent <= 1
        ? 'acceptable'
        : 'poor';
  const videoCall: UsageVerdict =
    downloadMbps >= 10 && uploadMbps >= 3 && latencyMs <= 80 && jitterMs <= 30 && packetLossPercent <= 1
      ? 'good'
      : downloadMbps >= 5 && uploadMbps >= 1 && latencyMs <= 150 && jitterMs <= 50 && packetLossPercent <= 3
        ? 'acceptable'
        : 'poor';

  return { gaming, streaming, videoCall };
}

function classifyBottleneck(
  uploadMbps: number | null,
  latencyMs: number | null,
  packetLossPercent: number | null,
): PrimaryBottleneck {
  if (packetLossPercent != null && packetLossPercent > 2) return 'packetLoss';
  if (latencyMs != null && latencyMs > 100) return 'latency';
  if (uploadMbps != null && uploadMbps < 5) return 'upload';
  if (uploadMbps == null && latencyMs == null && packetLossPercent == null) return 'unknown';
  return 'none';
}

export function classifySpeedTest(result: SpeedTestResult | null): SpeedTestQuality {
  if (!result) {
    return {
      bottleneck: 'unknown',
      overall: 'unknown',
      speed: 'unknown',
      stability: 'unknown',
      summary: 'Teste ainda não executado.',
      usage: { gaming: 'unknown', streaming: 'unknown', videoCall: 'unknown' },
    };
  }

  const speed = classifySpeed(result.download.mbps);
  const stability = classifyStability(result.latency.ms, result.jitter.ms);
  const usage = verdictFromAndroidThresholds(
    result.download.mbps,
    result.upload.mbps,
    result.latency.ms,
    result.jitter.ms,
    result.availability.perceivedLossPercent,
  );
  const bottleneck = classifyBottleneck(
    result.upload.mbps,
    result.latency.ms,
    result.availability.perceivedLossPercent,
  );

  const overall: OverallQuality =
    speed === 'unknown' && stability === 'unknown'
      ? 'unknown'
      : speed === 'slow' || stability === 'unstable' || usage.gaming === 'poor' || usage.videoCall === 'poor'
        ? 'bad'
        : speed === 'ok' ||
            stability === 'attention' ||
            result.upload.status !== 'measured' ||
            usage.gaming === 'acceptable' ||
            usage.videoCall === 'acceptable'
          ? 'attention'
          : 'good';

  const summary =
    overall === 'good'
      ? 'A conexão parece adequada no teste web.'
      : overall === 'bad'
        ? 'O teste encontrou lentidão ou instabilidade relevante.'
        : overall === 'attention'
          ? 'A conexão funciona, mas há pontos para acompanhar.'
          : 'Não há dados suficientes para classificar a conexão.';

  return { bottleneck, overall, speed, stability, summary, usage };
}
