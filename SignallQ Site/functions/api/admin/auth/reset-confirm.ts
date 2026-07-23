import { atualizarSenhaUsuarioIsp, consumirTokenAcessoAdmin, criarSessaoAdmin, type AdminAuthEnv } from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type ResetConfirmRequest = {
  tenant_id: string;
  token: string;
  senha: string;
};

function validarPayload(body: unknown): body is ResetConfirmRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return (
    typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' &&
    typeof value.token === 'string' && value.token.trim() !== '' &&
    typeof value.senha === 'string' && value.senha.length >= 8
  );
}

export async function handleConfirmarResetAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) return errorResponse('Payload inválido.', 400);

  const token = await consumirTokenAcessoAdmin(env.DB, body.token, 'reset');
  if (!token || token.tenantId !== body.tenant_id) {
    return errorResponse('Token inválido ou expirado.', 400);
  }

  const atualizou = await atualizarSenhaUsuarioIsp(env.DB, token.tenantId, token.email, body.senha);
  if (!atualizou) return errorResponse('Usuário não encontrado para redefinição.', 404);

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
  return handleConfirmarResetAdmin(request, env);
};
