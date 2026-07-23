import type { GenieACSResult } from '../../src/shared/genieacs';
import { mapToGenieAcsResult } from './genieacs-params';

const GENIEACS_TIMEOUT_MS = 3_000; // caminho síncrono do assinante (issue #66) — mais curto que os
// 10s do SGP porque o assinante está aguardando visualmente entre a declaração do problema e a
// medição; exceder isso piora a experiência mais do que a informação agrega (ver
// docs/erp/genieacs/integracao-tecnico-virtual.md).

function basicAuthHeader(user?: string, pass?: string): string | undefined {
  if (!user || !pass) return undefined;
  return `Basic ${btoa(`${user}:${pass}`)}`;
}

// Consulta a NBI do GenieACS pelo serial da ONU (issue #66). NUNCA lança para o chamador: timeout,
// erro de rede, HTTP >= 400, device não encontrado ou JSON inválido sempre viram
// `{available: false}` — é o comportamento normal esperado (ISP sem GenieACS, ONU não provisionada,
// rede instável), não uma falha a logar como erro.
export async function consultarSinalGenieAcs(
  baseUrl: string,
  serial: string,
  thresholdDbm: number,
  auth: { user?: string; pass?: string } | undefined,
  fetcher: typeof fetch = fetch,
): Promise<GenieACSResult> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), GENIEACS_TIMEOUT_MS);

  try {
    // `_deviceId._SerialNumber` é a forma que o GenieACS 1.2.16 resolve de fato — a variante
    // documentada `DeviceID.SerialNumber` retorna 200 com array vazio (validado contra simulador,
    // issue #84; evidência em docs/erp/genieacs/payload-examples/sim-device-query-by-serial.json).
    const query = encodeURIComponent(JSON.stringify({ '_deviceId._SerialNumber': serial }));
    const headers: Record<string, string> = { Accept: 'application/json' };
    const authHeader = basicAuthHeader(auth?.user, auth?.pass);
    if (authHeader) headers.Authorization = authHeader;

    const response = await fetcher(`${baseUrl}/devices?query=${query}`, {
      method: 'GET',
      headers,
      signal: controller.signal,
    });

    if (!response.ok) return { available: false };

    const devices = (await response.json()) as unknown;
    if (!Array.isArray(devices) || devices.length === 0) return { available: false };

    return mapToGenieAcsResult(devices[0], thresholdDbm, Date.now());
  } catch {
    // AbortError (timeout), erro de rede, JSON malformado — tudo cai aqui. Fallback gracioso
    // sempre; nunca propagar exceção pro endpoint HTTP.
    return { available: false };
  } finally {
    clearTimeout(timeoutId);
  }
}

const OFFLINE_LAST_INFORM_MS = 10 * 60 * 1000;

// Conta devices do ACS cujo último inform passou de 10 min (issue #98) — enriquecimento do alerta
// de massiva. Sem vínculo POP↔device no GenieACS hoje, a contagem cobre o parque inteiro do
// tenant: é heurística de confiança (sinal fraco), nunca confirmação de massiva. A forma da query
// ({"_lastInform":{"$lt":"<ISO>"}}) foi validada ao vivo contra o simulador local (NBI 1.2.16).
// null em qualquer falha — o alerta sai com onus_offline NULL.
export async function contarOnusOffline(
  baseUrl: string,
  auth: { user?: string; pass?: string } | undefined,
  fetcher: typeof fetch = fetch,
): Promise<number | null> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), GENIEACS_TIMEOUT_MS);

  try {
    const corte = new Date(Date.now() - OFFLINE_LAST_INFORM_MS).toISOString();
    const query = encodeURIComponent(JSON.stringify({ _lastInform: { $lt: corte } }));
    const headers: Record<string, string> = { Accept: 'application/json' };
    const authHeader = basicAuthHeader(auth?.user, auth?.pass);
    if (authHeader) headers.Authorization = authHeader;

    const response = await fetcher(`${baseUrl}/devices/?query=${query}&projection=_id`, {
      method: 'GET',
      headers,
      signal: controller.signal,
    });

    if (!response.ok) return null;

    const devices = (await response.json()) as unknown;
    return Array.isArray(devices) ? devices.length : null;
  } catch {
    return null;
  } finally {
    clearTimeout(timeoutId);
  }
}
