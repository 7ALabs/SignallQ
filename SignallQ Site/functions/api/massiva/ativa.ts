import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { consultarMassivaAtiva } from '../../_modules/massiva';
import { resolveMassivaTtlHoras, type SgpEnv } from '../../_shared/env';

type MassivaAtivaRequest = {
  tenant_id: string;
  pop_nome: string;
};

// Interseção (e não interface) porque SgpEnv tem index signature string — o binding D1 não cabe
// nela, mas cabe numa interseção (mesmo padrão de functions/api/erp/chamado.ts).
export type MassivaEnv = SgpEnv & { DB?: D1Database };

// Consulta massiva ativa por tenant+POP lendo alertas abertos no D1 (issue #95) — sem chamada ao
// SGP. Indisponibilidade do D1 é fail-open na fonte: o produto se comporta como "sem massiva" e o
// assinante segue o fluxo normal; informar massiva é proteção, não o core.
export async function handleMassivaAtiva(request: Request, env: MassivaEnv): Promise<Response> {
  const body = await readJsonBody<MassivaAtivaRequest>(request);
  if (
    !body ||
    typeof body.tenant_id !== 'string' || body.tenant_id.trim() === '' ||
    typeof body.pop_nome !== 'string' || body.pop_nome.trim() === ''
  ) {
    return errorResponse('Payload inválido.', 400);
  }

  if (!env.DB) {
    console.warn('[massiva/ativa] binding D1 ausente — respondendo sem massiva (fail-open)', {
      tenant_id: body.tenant_id,
    });
    return jsonResponse({ ativa: false });
  }

  try {
    const ttlHoras = resolveMassivaTtlHoras(env, body.tenant_id);
    return jsonResponse(await consultarMassivaAtiva(env.DB, body.tenant_id, body.pop_nome, ttlHoras));
  } catch (err) {
    console.error('[massiva/ativa] falha na consulta ao D1 — respondendo sem massiva (fail-open)', {
      tenant_id: body.tenant_id,
      erro: err instanceof Error ? err.message : 'erro_desconhecido',
    });
    return jsonResponse({ ativa: false });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<MassivaEnv> = async ({ request, env }) => {
  return handleMassivaAtiva(request, env);
};
