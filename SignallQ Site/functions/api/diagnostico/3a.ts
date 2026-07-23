import { registrarDiagnostico3a } from '../../_modules/diagnosticos-3a';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';

export type Diagnostico3aEnv = {
  DB?: D1Database;
};

type Diagnostico3aRequest = {
  tenant_id: string;
  diagnostico_id: string;
  tipo_problema: string;
  pop_nome?: string;
};

function validarPayload(body: unknown): body is Diagnostico3aRequest {
  if (typeof body !== 'object' || body === null) return false;
  const value = body as Record<string, unknown>;
  return (
    typeof value.tenant_id === 'string' && value.tenant_id.trim() !== '' &&
    typeof value.diagnostico_id === 'string' && value.diagnostico_id.trim() !== '' &&
    typeof value.tipo_problema === 'string' && value.tipo_problema.trim() !== '' &&
    (value.pop_nome === undefined || typeof value.pop_nome === 'string')
  );
}

export async function handleRegistrarDiagnostico3a(
  request: Request,
  env: Diagnostico3aEnv,
): Promise<Response> {
  if (!env.DB) return errorResponse('Banco de dados indisponível.', 503);

  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  const status = await registrarDiagnostico3a(env.DB, {
    tenantId: body.tenant_id,
    popNome: body.pop_nome?.trim() ? body.pop_nome : null,
    diagnosticoId: body.diagnostico_id,
    tipoProblema: body.tipo_problema,
  });

  return jsonResponse({ status }, { status: status === 'registrado' ? 201 : 200 });
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<Diagnostico3aEnv> = async ({ request, env }) => {
  return handleRegistrarDiagnostico3a(request, env);
};
