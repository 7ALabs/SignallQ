import type { SpeedTestResult } from '@shared/contracts';
import { SpeedtestPhase } from '@/types/network';

export type SpeedTestRunStatus = 'idle' | 'running' | 'partial' | 'success' | 'error' | 'canceled';

export interface SpeedTestProgress {
  phase: SpeedtestPhase;
  status: SpeedTestRunStatus;
  message: string;
}

export interface SpeedTestRunResult {
  result: SpeedTestResult;
  errorMessage?: string;
  status: Extract<SpeedTestRunStatus, 'partial' | 'success' | 'error'>;
}

export interface SpeedTestCanceledResult {
  result: null;
  status: Extract<SpeedTestRunStatus, 'canceled'>;
}

export interface SpeedTestRunnerOptions {
  downloadChunkBytes?: number;
  downloadDurationMs?: number;
  downloadStreams?: number;
  downloadWarmupMs?: number;
  fetchFn?: typeof fetch;
  latencySampleCount?: number;
  now?: () => number;
  onProgress?: (progress: SpeedTestProgress) => void;
  signal?: AbortSignal;
  skipUpload?: boolean;
  timeoutMs?: number;
  uploadChunkBytes?: number;
  uploadDurationMs?: number;
  uploadStreams?: number;
  uploadWarmupMs?: number;
}

export type SpeedTestRunnerResult = SpeedTestRunResult | SpeedTestCanceledResult;
