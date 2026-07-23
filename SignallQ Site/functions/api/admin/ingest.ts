import { handleAdminIngest } from '../../_modules/admin-ingest';
import { type AdminIngestEnv } from '../../_shared/env';
import { optionsResponse } from '../../_shared/http';

export const onRequestOptions: PagesFunction<AdminIngestEnv> = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminIngestEnv> = async ({ request, env }) => {
  return handleAdminIngest(request, env);
};
