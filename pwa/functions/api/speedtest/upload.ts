import { jsonResponse, optionsResponse } from '../../_shared/http';

const MAX_BYTES = 4 * 1024 * 1024;

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction = async ({ request }) => {
  const bytes = await request.arrayBuffer();
  if (bytes.byteLength > MAX_BYTES) {
    return jsonResponse({ error: 'Payload acima do limite do speedtest PWA.' }, { status: 413 });
  }

  return jsonResponse({
    ok: true,
    receivedBytes: bytes.byteLength,
    receivedAt: Date.now(),
  });
};
