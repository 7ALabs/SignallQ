import { autenticarSuporte7Agents, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type SupportLoginRequest = {
  tenant_id: string;
  email: string;
};

function validarPayload(body: unknown): body is SupportLoginRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' && typeof value.email === 'string' && value.email.includes('@');
}

export async function handleSupportLoginAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) return errorResponse('Payload inválido.', 400);

  const sessao = await autenticarSuporte7Agents(request, env, body.tenant_id, body.email);
  if (!sessao) return errorResponse('Acesso de suporte negado.', 403);

  return jsonResponse(
    {
      ok: true,
      tenantId: body.tenant_id,
      email: body.email.trim().toLowerCase(),
      session: { expiraEm: sessao.expiraEm },
    },
    { headers: { 'Set-Cookie': sessao.cookie } },
  );
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleSupportLoginAdmin(request, env);
};
