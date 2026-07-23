import {
  criarTokenAcessoAdmin,
  registrarAuditoriaAdmin,
  resolverSessaoAdmin,
  type AdminAuthEnv,
} from '../../../_modules/admin-auth';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../../_shared/http';

type InviteRequest = {
  tenant_id: string;
  email: string;
};

function validarPayload(body: unknown): body is InviteRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' && typeof value.email === 'string' && value.email.includes('@');
}

export async function handleConvidarUsuarioAdmin(request: Request, env: AdminAuthEnv): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const sessao = await resolverSessaoAdmin(request, env);
  if (!sessao) return errorResponse('Sessão expirada.', 401);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) return errorResponse('Payload inválido.', 400);
  if (sessao.tenantId !== body.tenant_id) return errorResponse('Acesso negado ao tenant solicitado.', 403);

  const convite = await criarTokenAcessoAdmin(env.DB, body.tenant_id, body.email, 'invite');
  await registrarAuditoriaAdmin(env.DB, {
    tenantId: body.tenant_id,
    atorEmail: sessao.email,
    origem: 'isp',
    acao: 'convite_gerado',
    alvoTipo: 'usuario_isp',
    alvoId: body.email.trim().toLowerCase(),
  });

  return jsonResponse({
    ok: true,
    convite: {
      email: body.email.trim().toLowerCase(),
      expiraEm: convite.expiraEm,
      token: convite.token,
    },
  });
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<AdminAuthEnv> = async ({ request, env }) => {
  return handleConvidarUsuarioAdmin(request, env);
};
