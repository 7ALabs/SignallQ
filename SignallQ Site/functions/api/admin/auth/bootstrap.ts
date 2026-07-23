import { contarUsuariosTenant, criarSessaoAdmin, criarUsuarioIsp, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type BootstrapRequest = {
  tenant_id: string;
  email: string;
  senha: string;
};

function validarPayload(body: unknown): body is BootstrapRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return (
    typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' &&
    typeof value.email === 'string' && value.email.includes('@') &&
    typeof value.senha === 'string' && value.senha.length >= 8
  );
}

export async function handleBootstrapAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) {
    return errorResponse('Payload inválido. Informe tenant_id, email válido e senha com 8+ caracteres.', 400);
  }

  if ((await contarUsuariosTenant(env.DB, body.tenant_id)) > 0) {
    return errorResponse('Tenant já possui acesso administrativo provisionado.', 409);
  }

  await criarUsuarioIsp(env.DB, body.tenant_id, body.email, body.senha);
  const sessao = await criarSessaoAdmin(env.DB, body.tenant_id, body.email);

  return jsonResponse(
    {
      tenantId: body.tenant_id,
      email: body.email.trim().toLowerCase(),
      needsBootstrap: false,
      session: { expiraEm: sessao.expiraEm },
    },
    {
      status: 201,
      headers: { 'Set-Cookie': sessao.cookie },
    },
  );
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleBootstrapAdmin(request, env);
};
