import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { registrarAvisoNormalizacao } from '../../_modules/massiva';
import type { MassivaEnv } from './ativa';

type AvisoRequest = {
  tenant_id: string;
  contrato_id: string;
  pop_nome: string;
};

function validarAviso(body: AvisoRequest | null): body is AvisoRequest {
  return (
    body !== null &&
    typeof body.tenant_id === 'string' && body.tenant_id.trim() !== '' &&
    typeof body.contrato_id === 'string' && body.contrato_id.trim() !== '' &&
    typeof body.pop_nome === 'string' && body.pop_nome.trim() !== ''
  );
}

// Registra o pedido de aviso de normalização de massiva no D1 (issue #96). Só registro — o envio
// efetivo do aviso é pendência documentada para o M4 (mensagem externa exige aprovação e canal).
export async function handleMassivaAviso(request: Request, env: MassivaEnv): Promise<Response> {
  const body = await readJsonBody<AvisoRequest>(request);
  if (!validarAviso(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  if (!env.DB) {
    console.error('[massiva/aviso] binding D1 ausente — pedido não registrado', {
      tenant_id: body.tenant_id,
    });
    return errorResponse('Serviço indisponível.', 503);
  }

  try {
    const resultado = await registrarAvisoNormalizacao(env.DB, {
      tenantId: body.tenant_id,
      contratoId: body.contrato_id,
      popNome: body.pop_nome,
    });
    return jsonResponse({ registrado: true, duplicado: resultado === 'ja_pendente' });
  } catch (err) {
    console.error('[massiva/aviso] falha ao registrar pedido no D1', {
      tenant_id: body.tenant_id,
      erro: err instanceof Error ? err.message : 'erro_desconhecido',
    });
    return errorResponse('Não foi possível registrar o aviso.', 500);
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<MassivaEnv> = async ({ request, env }) => {
  return handleMassivaAviso(request, env);
};
