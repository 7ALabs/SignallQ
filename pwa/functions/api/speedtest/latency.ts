import { jsonResponse, optionsResponse } from '../../_shared/http';

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction = async () => {
  return jsonResponse({ ok: true, now: Date.now() });
};
