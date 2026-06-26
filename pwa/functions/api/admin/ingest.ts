import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';

interface Env {
  ADMIN_INGEST_URL?: string;
  ADMIN_WORKER_URL?: string;
  ADMIN_INGEST_KEY?: string;
  ADMIN_SECRET?: string;
}

type IngestKind = 'diagnostic' | 'ai-usage';

interface AdminIngestRequest {
  kind: IngestKind;
  payload: Record<string, unknown>;
}

const endpointByKind: Record<IngestKind, string> = {
  diagnostic: '/ingest/diagnostic',
  'ai-usage': '/ingest/ai-usage',
};

function isIngestKind(value: unknown): value is IngestKind {
  return value === 'diagnostic' || value === 'ai-usage';
}

export const onRequestOptions: PagesFunction<Env> = async () => optionsResponse();

export const onRequestPost: PagesFunction<Env> = async ({ request, env }) => {
  const body = await readJsonBody<AdminIngestRequest>(request);
  if (!body || !isIngestKind(body.kind) || typeof body.payload !== 'object') {
    return errorResponse('Payload de ingest inválido.', 400);
  }

  const baseUrl = (env.ADMIN_INGEST_URL ?? env.ADMIN_WORKER_URL)?.replace(/\/$/, '');
  const token = env.ADMIN_INGEST_KEY ?? env.ADMIN_SECRET;
  if (!baseUrl || !token) {
    return errorResponse('ADMIN_INGEST_URL/ADMIN_WORKER_URL e ADMIN_INGEST_KEY não configurados no servidor.', 503);
  }

  const upstream = await fetch(`${baseUrl}${endpointByKind[body.kind]}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(body.payload),
  });

  const responseBody = (await upstream.json().catch(() => ({ ok: false }))) as unknown;
  if (!upstream.ok) {
    return jsonResponse(responseBody, { status: upstream.status });
  }

  return jsonResponse(responseBody, { status: upstream.status });
};
