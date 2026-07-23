import { listarUsuariosTenant, resolverSessaoAdmin, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse } from '../../../_shared/http';

function tenantIdFrom(request: Request): string | null {
  return new URL(request.url).searchParams.get('tenant_id')?.trim() ?? null;
}

export async function handleListarUsuariosAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);
  const tenantId = tenantIdFrom(request);
  if (!tenantId) return errorResponse('tenant_id obrigatório.', 400);

  const sessao = await resolverSessaoAdmin(request, env);
  if (!sessao) return errorResponse('Sessão expirada.', 401);
  if (sessao.tenantId !== tenantId) return errorResponse('Acesso negado ao tenant solicitado.', 403);

  const usuarios = await listarUsuariosTenant(env.DB, tenantId);
  return jsonResponse({ usuarios });
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleListarUsuariosAdmin(request, env);
};
