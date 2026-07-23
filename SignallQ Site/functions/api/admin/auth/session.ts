import { clearSessionCookie, contarUsuariosTenant, logoutSessaoAdmin, resolverSessaoAdmin, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse } from '../../../_shared/http';

function tenantFromRequest(request: Request): string | null {
  return new URL(request.url).searchParams.get('tenant_id')?.trim() ?? null;
}

export async function handleLerSessaoAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const tenantId = tenantFromRequest(request);
  if (!tenantId) return errorResponse('tenant_id obrigatório.', 400);

  const sessao = await resolverSessaoAdmin(request, env);
  if (!sessao) {
    const needsBootstrap = (await contarUsuariosTenant(env.DB, tenantId)) === 0;
    return jsonResponse({ autenticado: false, needsBootstrap }, { status: 401 });
  }

  if (sessao.tenantId !== tenantId) {
    return jsonResponse({ autenticado: false, needsBootstrap: false }, { status: 403 });
  }

  return jsonResponse({
    autenticado: true,
    needsBootstrap: false,
    session: {
      tenantId: sessao.tenantId,
      email: sessao.email,
      expiraEm: sessao.expiraEm,
    },
  });
}

export async function handleLogoutAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);
  await logoutSessaoAdmin(request, env.DB);
  return jsonResponse({ ok: true }, { headers: { 'Set-Cookie': clearSessionCookie() } });
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestGet: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleLerSessaoAdmin(request, env);
};

export const onRequestDelete: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleLogoutAdmin(request, env);
};
