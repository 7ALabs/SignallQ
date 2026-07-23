import {
  criarTokenAcessoAdmin,
  listarUsuariosTenant,
  registrarAuditoriaAdmin,
  resolverSessaoAdmin,
  type AdminAuthEnv,
} from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type ResetRequest = {
  tenant_id: string;
  email: string;
};

function validarPayload(body: unknown): body is ResetRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' && typeof value.email === 'string' && value.email.includes('@');
}

export async function handleSolicitarResetAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const sessao = await resolverSessaoAdmin(request, env);
  if (!sessao) return errorResponse('Sessão expirada.', 401);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) return errorResponse('Payload inválido.', 400);
  if (sessao.tenantId !== body.tenant_id) return errorResponse('Acesso negado ao tenant solicitado.', 403);

  const email = body.email.trim().toLowerCase();
  const usuarios = await listarUsuariosTenant(env.DB, body.tenant_id);
  const usuarioExiste = usuarios.some((usuario) => usuario.email === email);
  if (!usuarioExiste) {
    return jsonResponse({ ok: true });
  }

  const reset = await criarTokenAcessoAdmin(env.DB, body.tenant_id, email, 'reset');
  await registrarAuditoriaAdmin(env.DB, {
    tenantId: body.tenant_id,
    atorEmail: sessao.email,
    origem: 'isp',
    acao: 'reset_gerado',
    alvoTipo: 'usuario_isp',
    alvoId: email,
  });

  return jsonResponse({
    ok: true,
    reset: {
      email,
      expiraEm: reset.expiraEm,
      token: reset.token,
    },
  });
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleSolicitarResetAdmin(request, env);
};
