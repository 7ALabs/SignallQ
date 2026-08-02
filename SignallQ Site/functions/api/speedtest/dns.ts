import { jsonResponse, optionsResponse } from '../../_shared/http';

export const onRequestOptions: PagesFunction = async () => optionsResponse();

// Recurso pequeno e dedicado para a medição de DNS via Resource Timing API (issue #93). Mesma
// origem do PWA — não precisa de `Timing-Allow-Origin` para expor domainLookupStart/End (essa
// restrição só existe cross-origin), mas o header é enviado mesmo assim (ver public/_headers) caso
// o path futuramente sirva um subdomínio dedicado.
export const onRequestGet: PagesFunction = async () => {
  return jsonResponse({ ok: true });
};
