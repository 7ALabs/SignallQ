import {
  consumirTokenAcessoAdmin,
  contarUsuariosTenant,
  criarSessaoAdmin,
  criarUsuarioIsp,
  type AdminAuthEnv,
} from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type InviteRedeemRequest = {
  tenant_id: string;
  token: string;
  senha: string;
};

function validarPayload(body: unknown): body is InviteRedeemRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return (
    typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' &&
    typeof value.token === 'string' && value.token.trim() !== '' &&
    typeof value.senha === 'string' && value.senha.length >= 8
  );
}

export async function handleRedeemInviteAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) return errorResponse('Payload inválido.', 400);

  const token = await consumirTokenAcessoAdmin(env.DB, body.token, 'invite');
  if (!token || token.tenantId !== body.tenant_id) {
    return errorResponse('Convite inválido ou expirado.', 400);
  }

  if ((await contarUsuariosTenant(env.DB, body.tenant_id)) > 0) {
    // Convites só entram aqui quando já existe o primeiro admin; para evitar duplicatas silenciosas,
    // tentamos criar e deixamos o D1 acusar conflito se o email já foi usado.
  }

  try {
    await criarUsuarioIsp(env.DB, token.tenantId, token.email, body.senha);
  } catch {
    return errorResponse('Não foi possível concluir o convite. Verifique se o usuário já existe.', 409);
  }

  const sessao = await criarSessaoAdmin(env.DB, token.tenantId, token.email);
  return jsonResponse(
    {
      ok: true,
      tenantId: token.tenantId,
      email: token.email,
      session: { expiraEm: sessao.expiraEm },
    },
    { headers: { 'Set-Cookie': sessao.cookie } },
  );
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleRedeemInviteAdmin(request, env);
};
