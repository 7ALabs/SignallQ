import { buildManifestResponseBody, resolveTenantIdFromRequest } from './_modules/manifest';

export function handleManifest(request: Request): Response {
  const tenantId = resolveTenantIdFromRequest(request);
  const body = buildManifestResponseBody(tenantId);

  return new Response(JSON.stringify(body), {
    headers: {
      'Content-Type': 'application/manifest+json; charset=utf-8',
      'Cache-Control': 'no-store',
    },
  });
}

export const onRequestGet: PagesFunction = async ({ request }) => handleManifest(request);
