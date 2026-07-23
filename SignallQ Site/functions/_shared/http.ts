export interface JsonError {
  error: string;
}

export function jsonResponse(body: unknown, init: ResponseInit = {}): Response {
  return new Response(JSON.stringify(body), {
    ...init,
    headers: {
      'Content-Type': 'application/json; charset=utf-8',
      'Cache-Control': 'no-store',
      ...init.headers,
    },
  });
}

export function errorResponse(error: string, status = 500): Response {
  return jsonResponse({ error } satisfies JsonError, { status });
}

export function optionsResponse(): Response {
  return new Response(null, {
    status: 204,
    headers: {
      'Access-Control-Allow-Methods': 'GET,POST,OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
      'Access-Control-Max-Age': '86400',
    },
  });
}

export async function readJsonBody<T>(request: Request, maxBytes = 64_000): Promise<T | null> {
  const buffer = await request.arrayBuffer();
  if (buffer.byteLength > maxBytes) return null;
  try {
    return JSON.parse(new TextDecoder().decode(buffer)) as T;
  } catch {
    return null;
  }
}
