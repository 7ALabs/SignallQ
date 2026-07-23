import { autenticarUsuarioIsp, contarUsuariosTenant, criarSessaoAdmin, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type LoginRequest = {
  tenant_id: string;
  email: string;
  senha: string;
};

function validarPayload(body: unknown): body is LoginRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return (
    typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' &&
    typeof value.email === 'string' && value.email.includes('@') &&
    typeof value.senha === 'string' && value.senha.length >= 1
  );
}

export async function handleLoginAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  const totalUsuarios = await contarUsuariosTenant(env.DB, body.tenant_id);
  if (totalUsuarios === 0) {
    return jsonResponse({ error: 'bootstrap_pendente', needsBootstrap: true }, { status: 409 });
  }

  const usuario = await autenticarUsuarioIsp(env.DB, body.tenant_id, body.email, body.senha);
  if (!usuario) {
    return errorResponse('Credenciais inválidas.', 401);
  }

  const sessao = await criarSessaoAdmin(env.DB, body.tenant_id, body.email);
  return jsonResponse(
    {
      tenantId: body.tenant_id,
      email: body.email.trim().toLowerCase(),
      needsBootstrap: false,
      session: { expiraEm: sessao.expiraEm },
    },
    {
      headers: { 'Set-Cookie': sessao.cookie },
    },
  );
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleLoginAdmin(request, env);
};
