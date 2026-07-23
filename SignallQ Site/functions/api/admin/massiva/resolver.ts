import { registrarAuditoriaAdmin, resolverSessaoAdmin, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { resolverMassivaManual, type AdminDashboardEnv } from '../../../_modules/admin-dashboard';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type ResolverMassivaEnv = AdminAuthEnv & AdminDashboardEnv;

type ResolverRequest = {
  tenant_id: string;
  alerta_id: number;
};

function validarPayload(body: unknown): body is ResolverRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return (
    typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' &&
    typeof value.alerta_id === 'number' && Number.isInteger(value.alerta_id) && value.alerta_id > 0
  );
}

export async function handleResolverMassiva(request: Request, env: ResolverMassivaEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const sessao = await resolverSessaoAdmin(request, env);
  if (!sessao) return errorResponse('Sessão expirada.', 401);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) return errorResponse('Payload inválido.', 400);
  if (sessao.tenantId !== body.tenant_id) return errorResponse('Acesso negado ao tenant solicitado.', 403);

  const ok = await resolverMassivaManual(env.DB, body.tenant_id, body.alerta_id);
  if (!ok) return errorResponse('Alerta não encontrado ou já resolvido.', 404);

  await registrarAuditoriaAdmin(env.DB, {
    tenantId: body.tenant_id,
    atorEmail: sessao.email,
    origem: 'isp',
    acao: 'massiva_resolvida_manual',
    alvoTipo: 'alerta_massiva',
    alvoId: String(body.alerta_id),
  });

  return jsonResponse({ ok: true });
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<ResolverMassivaEnv> = async ({ request, env }) => {
  return handleResolverMassiva(request, env);
};
