import { resolveReligaEnabled, resolveSgpCreds, type SgpEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { liberarPromessaSgp } from '../../_modules/sgp';

type ReligaRequest = {
  tenant_id: string;
  contrato_id: string;
};

function validarPayload(body: unknown): body is ReligaRequest {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  return (
    typeof b.tenant_id === 'string' && b.tenant_id.trim() !== '' &&
    typeof b.contrato_id === 'string' && b.contrato_id.trim() !== ''
  );
}

// Religa de confiança (issue #99): dispara a promessa de pagamento no SGP após o aceite do
// assinante. O toggle RELIGA_ENABLED_<TENANT> é validado aqui — o backend não confia no frontend:
// com a feature desligada, nenhuma chamada chega ao SGP. Liberação negada pelo SGP (status 0 ou 2)
// não é erro HTTP: volta { liberado: false } e o frontend cai na 2ª via.
export async function handleErpReliga(
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

  if (!resolveReligaEnabled(env, body.tenant_id)) {
    return jsonResponse({ error: 'religa_indisponivel' }, { status: 409 });
  }

  try {
    const resultado = await liberarPromessaSgp(
      creds.baseUrl,
      creds.token,
      creds.app,
      body.contrato_id,
      fetcher,
    );

    if (!resultado.liberado) {
      // status 0 vs 2 do SGP é só observabilidade (api-reference.md §1.8) — o produto trata os
      // dois como "não liberado" e o assinante segue para a 2ª via
      console.log('[erp/religa] liberação não concedida pelo SGP', {
        tenant_id: body.tenant_id,
        contrato_id: body.contrato_id,
        sgp_status: resultado.sgpStatus,
      });
      return jsonResponse({ liberado: false });
    }

    console.log('[erp/religa] promessa de pagamento liberada', {
      tenant_id: body.tenant_id,
      contrato_id: body.contrato_id,
      protocolo: resultado.protocolo,
    });

    return jsonResponse({
      liberado: true,
      ...(resultado.dataPromessa ? { dataPromessa: resultado.dataPromessa } : {}),
    });
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'erro_desconhecido';

    if (msg === 'timeout') {
      console.error('[erp/religa] timeout', { tenant_id: body.tenant_id, contrato_id: body.contrato_id });
      return jsonResponse({ error: 'timeout' }, { status: 503 });
    }

    // Nunca expor credenciais ou CPF na resposta de erro
    console.error('[erp/religa] falha ao liberar promessa no SGP', {
      tenant_id: body.tenant_id,
      contrato_id: body.contrato_id,
      erro: msg,
    });

    return jsonResponse({ error: msg }, { status: 502 });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<SgpEnv> = async ({ request, env }) => {
  return handleErpReliga(request, env);
};
