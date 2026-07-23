import { resolveReligaEnabled, resolveSgpCreds, type SgpEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { listarTitulosSgp } from '../../_modules/sgp';

type TitulosRequest = {
  tenant_id: string;
  contrato_id: string;
};

function validarPayload(body: unknown): body is TitulosRequest {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  return (
    typeof b.tenant_id === 'string' && b.tenant_id.trim() !== '' &&
    typeof b.contrato_id === 'string' && b.contrato_id.trim() !== ''
  );
}

// Lista os títulos em aberto do contrato para a tela de 2ª via (issue #101). Os dados de
// pagamento (Pix, linha digitável, boleto) vêm do SGP como estão — quando o SGP não fornece um
// meio (ex.: sem codigoPix), o campo simplesmente não vem: a tela degrada honesto para o boleto.
export async function handleErpTitulos(
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
    const titulos = await listarTitulosSgp(
      creds.baseUrl,
      creds.token,
      creds.app,
      body.contrato_id,
      fetcher,
    );
    // religaDisponivel informa a oferta na tela de suspensão (issue #99) — é o toggle do tenant,
    // não uma garantia do SGP: a liberação em si é revalidada em POST /api/erp/religa
    return jsonResponse({ titulos, religaDisponivel: resolveReligaEnabled(env, body.tenant_id) });
  } catch (err) {
    const msg = err instanceof Error ? err.message : 'erro_desconhecido';

    if (msg === 'timeout') {
      console.error('[erp/titulos] timeout', { tenant_id: body.tenant_id, contrato_id: body.contrato_id });
      return jsonResponse({ error: 'timeout' }, { status: 503 });
    }

    // Nunca expor credenciais na resposta de erro
    console.error('[erp/titulos] falha ao consultar títulos SGP', {
      tenant_id: body.tenant_id,
      contrato_id: body.contrato_id,
      erro: msg,
    });

    return jsonResponse({ error: msg }, { status: 502 });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<SgpEnv> = async ({ request, env }) => {
  return handleErpTitulos(request, env);
};
