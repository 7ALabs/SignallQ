import { errorResponse, optionsResponse } from '../../_shared/http';

interface Env {
  AI_WORKER_URL?: string;
}

export const onRequestOptions: PagesFunction<Env> = async () => optionsResponse();

export const onRequestPost: PagesFunction<Env> = async ({ request, env }) => {
  const workerUrl = env.AI_WORKER_URL?.replace(/\/$/, '');
  if (!workerUrl) {
    return errorResponse('AI_WORKER_URL não configurada no ambiente Pages.', 503);
  }

  const upstream = await fetch(`${workerUrl}/api/ai/diagnostico-conexao`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: await request.text(),
  });

  return new Response(upstream.body, {
    status: upstream.status,
    headers: {
      'Content-Type': upstream.headers.get('Content-Type') ?? 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
    },
  });
};
