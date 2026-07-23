import { resolveSgpCreds, type SgpEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { listarOcorrenciasAbertasSgp } from '../../_modules/sgp';

type OcorrenciaAbertaRequest = {
  tenant_id: string;
  contrato_id: string;
};

function validarPayload(body: unknown): body is OcorrenciaAbertaRequest {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  return (
    typeof b.tenant_id === 'string' && b.tenant_id.trim() !== '' &&
    typeof b.contrato_id === 'string' && b.contrato_id.trim() !== ''
  );
}

export async function handleErpOcorrenciaAberta(
  request: Request,
  env: SgpEnv,
  fetcher: typeof fetch = fetch,
): Promise<Response> {
  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  const creds = resolveSgpCreds(env, body.tenant_id);
  if (!creds) {
    return errorResponse('Tenant não configurado.', 503);
  }

  try {
    const resultado = await listarOcorrenciasAbertasSgp(
      creds.baseUrl,
      creds.token,
      creds.app,
      body.contrato_id,
      fetcher,
    );
    return jsonResponse(resultado);
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'erro_desconhecido';

    if (msg === 'timeout') {
      console.error('[erp/ocorrencia-aberta] timeout', { tenant_id: body.tenant_id, contrato_id: body.contrato_id });
      return jsonResponse({ error: 'timeout' }, { status: 503 });
    }

    // Nunca expor credenciais na resposta de erro
    console.error('[erp/ocorrencia-aberta] falha ao consultar ocorrências SGP', {
      tenant_id: body.tenant_id,
      contrato_id: body.contrato_id,
      erro: msg,
    });

    return jsonResponse({ error: msg }, { status: 502 });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<SgpEnv> = async ({ request, env }) => {
  return handleErpOcorrenciaAberta(request, env);
};
