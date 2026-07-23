import { resolveGenieAcsCreds, type GenieAcsEnv } from '../../_shared/env';
import { errorResponse, jsonResponse, optionsResponse, readJsonBody } from '../../_shared/http';
import { consultarSinalGenieAcs } from '../../_modules/genieacs';

type GenieAcsSignalRequest = {
  tenant_id: string;
  serial_onu: string;
};

function validarPayload(body: unknown): body is GenieAcsSignalRequest {
  if (typeof body !== 'object' || body === null) return false;
  const b = body as Record<string, unknown>;
  return (
    typeof b.tenant_id === 'string' && b.tenant_id.trim() !== '' &&
    typeof b.serial_onu === 'string' && b.serial_onu.trim() !== ''
  );
}

// Contrato (issue #67): sempre responde 200 com um GenieACSResult — nunca 5xx. O consumidor deste
// endpoint está no caminho síncrono do assinante (entre Estado 1 e Estado 2) e o fallback gracioso
// é obrigatório em qualquer cenário: tenant sem GenieACS, timeout, ONU não encontrada, erro do
// GenieACS. Só payload malformado (bug do chamador, não do GenieACS) retorna 400.
export async function handleGenieAcsSignal(
  request: Request,
  env: GenieAcsEnv,
  fetcher: typeof fetch = fetch,
): Promise<Response> {
  const body = await readJsonBody<unknown>(request);
  if (!validarPayload(body)) {
    return errorResponse('Payload inválido.', 400);
  }

  const creds = resolveGenieAcsCreds(env, body.tenant_id);
  if (!creds) {
    // Tenant sem GENIEACS_ENABLED=true — comportamento esperado, não é erro (issues #69/#70).
    return jsonResponse({ available: false });
  }

  const auth =
    creds.user && creds.pass ? { user: creds.user, pass: creds.pass } : undefined;

  try {
    const resultado = await consultarSinalGenieAcs(
      creds.baseUrl,
      body.serial_onu,
      creds.thresholdDbm,
      auth,
      fetcher,
    );

    console.log('[genieacs/signal] consulta', {
      tenant_id: body.tenant_id,
      available: resultado.available,
      ...(resultado.available
        ? { rx_power_dbm: resultado.rx_power_dbm, wan_status: resultado.wan_status }
        : {}),
    });

    return jsonResponse(resultado);
  } catch {
    // consultarSinalGenieAcs já não lança (fallback interno), mas se algo escapar por bug,
    // ainda assim nunca propagamos erro pro consumidor síncrono.
    return jsonResponse({ available: false });
  }
}

export const onRequestOptions: PagesFunction = async () => optionsResponse();

export const onRequestPost: PagesFunction<GenieAcsEnv> = async ({ request, env }) => {
  return handleGenieAcsSignal(request, env);
};
