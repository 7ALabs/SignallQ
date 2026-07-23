import { resolverSessaoAdmin, type AdminAuthEnv } from '../../_modules/admin-auth';
import { carregarDashboardOverview, type AdminDashboardEnv } from '../../_modules/admin-dashboard';
import { errorResponse, jsonResponse, optionsResponse } from '../../_shared/http';

type OverviewEnv = AdminAuthEnv & AdminDashboardEnv;

export async function handleAdminOverview(request: Request, env: OverviewEnv): Promise<Response> {
  const url = new URL(request.url);
  const tenantId = url.searchParams.get('tenant_id')?.trim();
  if (!tenantId) return errorResponse('tenant_id obrigatório.', 400);

  const sessao = await resolverSessaoAdmin(request, env);
  if (!sessao) return errorResponse('Sessão expirada.', 401);
  if (sessao.tenantId !== tenantId) return errorResponse('Acesso negado ao tenant solicitado.', 403);

  const periodoDias = Number(url.searchParams.get('periodo_dias') ?? '30');
  try {
    const overview = await carregarDashboardOverview(env, tenantId, periodoDias);
    return jsonResponse(overview);
  } catch {
    return errorResponse('Não foi possível carregar o painel agora.', 503);
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction<OverviewEnv> = async ({ request, env }) => {
  return handleAdminOverview(request, env);
};
