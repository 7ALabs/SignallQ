import { jsonResponse, optionsResponse } from '../../_shared/http';
import { createLatencyPayload } from '../../_modules/speedtest';

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction = async () => {
  return jsonResponse(createLatencyPayload());
};
