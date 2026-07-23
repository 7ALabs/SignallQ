import { resolveSgpCreds, type SgpEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { atualizarOsSgp, type SgpOsAtualizarCampos } from '../../_modules/sgp';

type OsAtualizarRequest = {
  tenant_id: string;
  os_id: string;
  campos: SgpOsAtualizarCampos;
};

const CAMPOS_VALIDOS = new Set([
  'os_servicoprestado',
  'os_observacao',
  'os_data_alteracao',
  'os_data_finalizacao',
  'checkin_data',
  'checkin_latitude',
  'checkin_longitude',
  'assinatura_cliente',
  'assinatura_tecnico',
  'assinatura_contrato',
  'os_status',
  'classificacao_adicionar',
  'classificacao_remover',
]);

function validarPayload(body: unknown): body is OsAtualizarRequest {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  if (typeof b.tenant_id !== 'string' || b.tenant_id.trim() === '') return false;
  if (typeof b.os_id !== 'string' || b.os_id.trim() === '') return false;
  if (typeof b.campos !== 'object' || b.campos === null) return false;

  const campos = b.campos as Record<string, unknown>;
  const chaves = Object.keys(campos);
  if (chaves.length === 0) return false;

  return chaves.every((chave) => CAMPOS_VALIDOS.has(chave));
}

export async function handleErpOsAtualizar(
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
    const resultado = await atualizarOsSgp(
      creds.baseUrl,
      creds.token,
      creds.app,
      body.os_id,
      body.campos,
      fetcher,
    );
    return jsonResponse(resultado);
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'erro_desconhecido';

    if (msg === 'timeout') {
      console.error('[erp/os-atualizar] timeout', { tenant_id: body.tenant_id, os_id: body.os_id });
      return jsonResponse({ error: 'timeout' }, { status: 503 });
    }

    // Nunca expor credenciais na resposta de erro
    console.error('[erp/os-atualizar] falha ao atualizar OS no SGP', {
      tenant_id: body.tenant_id,
      os_id: body.os_id,
      erro: msg,
    });

    return jsonResponse({ error: msg }, { status: 502 });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<SgpEnv> = async ({ request, env }) => {
  return handleErpOsAtualizar(request, env);
};
