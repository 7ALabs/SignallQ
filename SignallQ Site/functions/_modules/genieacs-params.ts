// Mapeamento TR-098 vs TR-181 dos parâmetros GenieACS relevantes ao Técnico Virtual (issue #68).
// Fonte: docs/erp/genieacs/api-reference.md (extraída da documentação oficial do GenieACS).
// Validação (issue #84, GenieACS 1.2.16 + simulador genieacs-docker): paths TR-098 de uptime e
// wanStatus confirmados contra payload real (docs/erp/genieacs/payload-examples/sim-*.json).
// rxPower e a árvore TR-181 seguem pendentes — o simulador não expõe parâmetro óptico nem TR-181;
// só validáveis contra ONU real (ver docs/erp/genieacs/homologacao-local.md).

import type { GenieACSResult } from '../../src/shared/genieacs';

const PARAM_PATHS = {
  tr098: {
    // X_ONU_RxPower é vendor extension (prefixo X_) — o nome real varia por fabricante
    // (FiberHome/ZTE/Huawei usam nomes distintos). Confirmar com a ONU do ISP-piloto antes de
    // confiar neste path; se divergir, rx_power_dbm sai null silenciosamente.
    rxPower: 'InternetGatewayDevice.WANDevice.1.X_ONU_RxPower',
    wanStatus: 'InternetGatewayDevice.WANDevice.1.WANConnectionDevice.1.WANIPConnection.1.ConnectionStatus',
    uptime: 'InternetGatewayDevice.DeviceInfo.UpTime',
  },
  tr181: {
    rxPower: 'Device.Optical.Interface.1.CurrentOpticalReceivePower',
    wanStatus: 'Device.IP.Interface.1.Status',
    uptime: 'Device.DeviceInfo.UpTime',
  },
} as const;

// Nome estável recomendado no onboarding: um Virtual Parameter definido no GenieACS do ISP lê o
// caminho proprietário do fabricante (X_ONU_RxPower, X_HW_*, etc.) e expõe este nome uniforme.
// Quem conhece o parque de ONUs é o ACS do ISP — normalizar lá evita lista de fabricantes no
// adapter. O path por modelo de dados abaixo fica como fallback para ISPs sem o virtual parameter.
const VIRTUAL_RX_PATH = 'VirtualParameters.rx_power_dbm';

const TIMESTAMP_STALE_MS = 5 * 60 * 1000; // 5 minutos — dado mais antigo que isso é tratado como indisponível

export type DataModel = 'tr098' | 'tr181';

// Detecta o modelo de dados pelo campo raiz do device retornado pela NBI. `InternetGatewayDevice` é
// TR-098 (mais comum em ONUs de ISPs brasileiros), `Device` é TR-181 (RFC 6413, mais recente).
export function detectDataModel(device: unknown): DataModel | null {
  if (typeof device !== 'object' || device === null) return null;
  const d = device as Record<string, unknown>;
  if ('InternetGatewayDevice' in d) return 'tr098';
  if ('Device' in d) return 'tr181';
  return null;
}

type ParamValue = { value: unknown; type: string; timestamp: string };

// Navega o path com dots (ex.: "InternetGatewayDevice.WANDevice.1.X_ONU_RxPower") e lê
// `_value`/`_type`/`_timestamp` do nó final. Parâmetro ausente em qualquer nível → null, nunca
// lança — modelos de ONU variam no que expõem, e isso não é um erro (issue #68, critério
// "ausente = null"). O leaf real traz também `_object`/`_writable` (confirmado no simulador,
// issue #84) — ignorados aqui por irrelevantes ao diagnóstico.
export function extractParam(device: unknown, path: string): ParamValue | null {
  const segments = path.split('.');
  let node: unknown = device;
  for (const segment of segments) {
    if (typeof node !== 'object' || node === null) return null;
    node = (node as Record<string, unknown>)[segment];
  }
  if (typeof node !== 'object' || node === null) return null;
  const leaf = node as Record<string, unknown>;
  if (!('_value' in leaf)) return null;
  return {
    value: leaf._value,
    type: typeof leaf._type === 'string' ? leaf._type : '',
    timestamp: typeof leaf._timestamp === 'string' ? leaf._timestamp : '',
  };
}

const NUMERIC_XSD_TYPES = new Set(['xsd:int', 'xsd:unsignedInt', 'xsd:long', 'xsd:unsignedLong', 'xsd:float', 'xsd:double']);

// Alguns CPEs serializam `_value` numérico como string; o `_type` (xsd:*) desambigua (issue #68,
// critério "parse de _type"). String sem _type numérico declarado não é coagida.
function paramToNumber(param: ParamValue | null): number | null {
  if (!param) return null;
  if (typeof param.value === 'number') return Number.isFinite(param.value) ? param.value : null;
  if (typeof param.value === 'string' && NUMERIC_XSD_TYPES.has(param.type)) {
    const parsed = Number(param.value);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
}

function parseWanStatus(value: unknown): 'connected' | 'disconnected' | 'unknown' {
  if (typeof value !== 'string') return 'unknown';
  const normalized = value.toLowerCase();
  if (normalized === 'connected' || normalized === 'up') return 'connected';
  if (normalized === 'disconnected' || normalized === 'down') return 'disconnected';
  return 'unknown';
}

function mostRecentTimestampMs(entries: Array<ParamValue | null>, nowMs: number): number {
  const timestamps = entries
    .filter((e): e is ParamValue => e !== null && e.timestamp !== '')
    .map((e) => Date.parse(e.timestamp))
    .filter((ms) => Number.isFinite(ms));
  if (timestamps.length === 0) return nowMs; // sem timestamp nenhum — não trata como stale por ausência de dado
  return Math.max(...timestamps);
}

// Orquestra a extração dos 3 campos de interesse e aplica as regras de negócio já documentadas em
// integracao-tecnico-virtual.md: timestamp desatualizado (>5min) = indisponível; RX abaixo do
// threshold OU WAN desconectado = fibra_comprometida. Retorna o GenieACSResult compartilhado
// (src/shared/genieacs.ts) — mesmo tipo consumido pelo frontend, contrato garantido pelo compilador
// (issue #66).
export function mapToGenieAcsResult(
  device: unknown,
  thresholdDbm: number,
  nowMs: number,
): GenieACSResult {
  const dataModel = detectDataModel(device);
  if (!dataModel) return { available: false };

  const paths = PARAM_PATHS[dataModel];
  const rxPowerParam = extractParam(device, VIRTUAL_RX_PATH) ?? extractParam(device, paths.rxPower);
  const wanStatusParam = extractParam(device, paths.wanStatus);
  const uptimeParam = extractParam(device, paths.uptime);

  const mostRecentMs = mostRecentTimestampMs([rxPowerParam, wanStatusParam, uptimeParam], nowMs);
  if (nowMs - mostRecentMs > TIMESTAMP_STALE_MS) return { available: false };

  const rxPowerDbm = paramToNumber(rxPowerParam);
  const wanStatus = wanStatusParam ? parseWanStatus(wanStatusParam.value) : 'unknown';
  const uptimeS = paramToNumber(uptimeParam);

  const fibraComprometida = (rxPowerDbm !== null && rxPowerDbm < thresholdDbm) || wanStatus === 'disconnected';

  return {
    available: true,
    rx_power_dbm: rxPowerDbm,
    wan_status: wanStatus,
    uptime_s: uptimeS,
    timestamp: new Date(mostRecentMs).toISOString(),
    data_model: dataModel,
    fibra_comprometida: fibraComprometida,
  };
}
