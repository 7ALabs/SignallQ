import type { DiagnosisInput, DiagnosisResult, SpeedTestResult } from '@shared/contracts';
import { buildDiagnosticPayload } from './aiPayload';
import { isAiWorkerResponse, mapAiWorkerResponseToDiagnosis } from './aiResponseMapper';
import type { AiDiagnosisClientOptions, AiDiagnosisOutcome } from './aiTypes';
import { createLocalDiagnosis } from './localDiagnosis';

const DEFAULT_AI_ENDPOINT = '/api/ai/diagnostico-conexao';
const DEFAULT_TIMEOUT_MS = 10_000;

function createFallbackDiagnosis(input: DiagnosisInput, errorMessage: string): DiagnosisResult {
  const fallback = createLocalDiagnosis(input);

  return {
    ...fallback,
    source: 'fallback',
    limitations: [
      ...fallback.limitations,
      {
        code: 'ai_unavailable',
        message: errorMessage,
      },
    ],
  };
}

async function postAiDiagnosis(
  payload: ReturnType<typeof buildDiagnosticPayload>,
  options: Required<Pick<AiDiagnosisClientOptions, 'endpoint' | 'fetchFn' | 'timeoutMs'>>,
): Promise<DiagnosisResult> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), options.timeoutMs);

  try {
    const response = await options.fetchFn(options.endpoint, {
      body: JSON.stringify(payload),
      headers: {
        'Content-Type': 'application/json',
      },
      method: 'POST',
      signal: controller.signal,
    });

    const data = (await response.json().catch(() => null)) as unknown;
    if (!response.ok) {
      const error = data && typeof data === 'object' && 'error' in data ? String(data.error) : response.statusText;
      throw new Error(error || 'Diagnóstico IA indisponível.');
    }

    // Contrato real do ai-diagnosis-worker (compartilhado com o Android) — não
    // o contrato de UI `DiagnosisResult`. Ver aiResponseMapper.ts.
    if (!isAiWorkerResponse(data)) {
      throw new Error('Resposta IA fora do contrato esperado.');
    }

    return mapAiWorkerResponseToDiagnosis(data);
  } finally {
    clearTimeout(timeoutId);
  }
}

export async function createDiagnosisWithAiFallback(
  speedTest: SpeedTestResult,
  options: AiDiagnosisClientOptions = {},
): Promise<AiDiagnosisOutcome> {
  const connectionType = speedTest.connection.effectiveType ?? speedTest.connection.source;
  const payload = buildDiagnosticPayload(speedTest, connectionType);
  const fetchFn = options.fetchFn ?? ((input, init) => fetch(input, init));
  const endpoint = options.endpoint ?? DEFAULT_AI_ENDPOINT;
  const timeoutMs = options.timeoutMs ?? DEFAULT_TIMEOUT_MS;

  try {
    const diagnosis = await postAiDiagnosis(payload, { endpoint, fetchFn, timeoutMs });
    return { diagnosis, payload, source: 'ai' };
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : 'Análise avançada indisponível no momento.';
    return {
      diagnosis: createFallbackDiagnosis({ speedTest }, errorMessage),
      errorMessage,
      payload,
      source: 'fallback',
    };
  }
}
