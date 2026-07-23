export const MIN_JITTER_SAMPLES = 2;

export function calculateMbps(bytes: number, durationMs: number): number | null {
  if (bytes <= 0 || durationMs <= 0 || !Number.isFinite(durationMs)) return null;
  return Math.round(((bytes * 8) / (durationMs / 1000) / 1_000_000) * 10) / 10;
}

export function calculateJitterMs(latencySamplesMs: number[]): {
  ms: number | null;
  samples: number;
  status: 'measured' | 'insufficient_samples';
} {
  const validSamples = latencySamplesMs.filter((sample) => Number.isFinite(sample) && sample >= 0);
  if (validSamples.length < MIN_JITTER_SAMPLES) {
    return { ms: null, samples: validSamples.length, status: 'insufficient_samples' };
  }

  const deltas = validSamples.slice(1).map((sample, index) => Math.abs(sample - validSamples[index]!));
  const averageDelta = deltas.reduce((sum, value) => sum + value, 0) / deltas.length;
  return { ms: Math.round(averageDelta * 10) / 10, samples: validSamples.length, status: 'measured' };
}
