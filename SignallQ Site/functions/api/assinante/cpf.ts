import { resolveSgpCreds, type SgpEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { lookupAssinanteSgp } from '../../_modules/sgp';

type CpfRequest = {
  cpf: string;
  tenant_id: string;
};

function normalizarCpf(valor: string): string {
  return valor.replace(/\D/g, '');
}

export async function handleAssinanteCpf(
  request: Request,
  env: SgpEnv,
  fetcher: typeof fetch = fetch,
): Promise<Response> {
  const body = await readJsonBody<CpfRequest>(request);
  if (!body || typeof body.cpf !== 'string' || typeof body.tenant_id !== 'string') {
    return errorResponse('Payload inválido.', 400);
  }

  const cpf = normalizarCpf(body.cpf);
  if (cpf.length !== 11) {
    return errorResponse('CPF inválido.', 400);
  }

  const tenantId = body.tenant_id.trim();
  if (!tenantId) {
    return errorResponse('tenant_id obrigatório.', 400);
  }

  const creds = resolveSgpCreds(env, tenantId);
  if (!creds) {
    return errorResponse('Tenant não configurado.', 503);
  }

  try {
    const assinantes = await lookupAssinanteSgp(creds.baseUrl, creds.token, creds.app, cpf, fetcher);
    // CPF sem contrato é resultado de negócio, não erro HTTP — mesmo padrão do restante da
    // integração SGP (ver docs/erp/sgp/api-reference.md): lista vazia via 200, nunca 404 (issue #127).
    return jsonResponse({ assinantes });
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'erro_desconhecido';
    console.error('[assinante/cpf] falha ao consultar SGP', { tenant_id: tenantId, erro: msg });
    return errorResponse('SGP indisponível.', 503);
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<SgpEnv> = async ({ request, env }) => {
  return handleAssinanteCpf(request, env);
};
