import type { DiagnosisResult, DiagnosticPayload } from '@shared/contracts';

export interface AiDiagnosisClientOptions {
  endpoint?: string;
  fetchFn?: typeof fetch;
  timeoutMs?: number;
}

export interface AiDiagnosisSuccess {
  diagnosis: DiagnosisResult;
  payload: DiagnosticPayload;
  source: 'ai';
}

export interface AiDiagnosisFallback {
  diagnosis: DiagnosisResult;
  errorMessage: string;
  payload: DiagnosticPayload;
  source: 'fallback';
}

export type AiDiagnosisOutcome = AiDiagnosisSuccess | AiDiagnosisFallback;
