import type { AdminIngestRequest, IngestKind } from '../../shared/contracts';
import { resolveAdminBaseUrl, resolveAdminToken, type AdminIngestEnv } from '../_shared/env';
import { errorResponse, jsonResponse, readJsonBody } from '../_shared/http';

const endpointByKind: Record<IngestKind, string> = {
  diagnostic: '/ingest/diagnostic',
  'ai-usage': '/ingest/ai-usage',
};

export function isIngestKind(value: unknown): value is IngestKind {
  return value === 'diagnostic' || value === 'ai-usage';
}

export function ingestEndpointFor(kind: IngestKind): string {
  return endpointByKind[kind];
}

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function validateAdminIngestRequest(value: unknown): AdminIngestRequest | null {
  if (!isRecord(value) || !isIngestKind(value.kind) || !isRecord(value.payload)) {
    return null;
  }

  return {
    kind: value.kind,
    payload: value.payload,
  };
}

export async function handleAdminIngest(
  request: Request,
  env: AdminIngestEnv,
  fetcher: typeof fetch = fetch,
): Promise<Response> {
  const body = validateAdminIngestRequest(await readJsonBody<unknown>(request));
  if (!body) {
    return errorResponse('Payload de ingest invalido.', 400);
  }

  const baseUrl = resolveAdminBaseUrl(env);
  const token = resolveAdminToken(env);
  if (!baseUrl || !token) {
    return errorResponse('ADMIN_INGEST_URL/ADMIN_WORKER_URL e ADMIN_INGEST_KEY nao configurados no servidor.', 503);
  }

  const upstream = await fetcher(`${baseUrl}${ingestEndpointFor(body.kind)}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body.payload),
  });

  const responseBody = (await upstream.json().catch(() => ({ ok: false }))) as unknown;
  return jsonResponse(responseBody, { status: upstream.status });
}
