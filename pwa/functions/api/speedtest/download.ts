import { optionsResponse } from '../../_shared/http';

const MAX_BYTES = 2 * 1024 * 1024;

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction = async ({ request }) => {
  const url = new URL(request.url);
  const requestedBytes = Number(url.searchParams.get('bytes') ?? 524_288);
  const byteLength = Math.min(Math.max(requestedBytes, 64 * 1024), MAX_BYTES);
  const payload = new Uint8Array(byteLength);

  return new Response(payload, {
    headers: {
      'Content-Type': 'application/octet-stream',
      'Cache-Control': 'no-store, no-cache, must-revalidate',
      'Content-Length': String(byteLength),
      'X-SignallQ-Speedtest-Bytes': String(byteLength),
    },
  });
};
