import { createDownloadResponse, parseDownloadBytes } from '../../_modules/speedtest';
import { optionsResponse } from '../../_shared/http';

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction = async ({ request }) => {
  return createDownloadResponse(parseDownloadBytes(request));
};
