import { createUploadResult } from '../../_modules/speedtest';
import { optionsResponse } from '../../_shared/http';

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction = async ({ request }) => {
  return createUploadResult(request);
};
