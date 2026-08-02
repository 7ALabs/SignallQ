export interface AdminIngestEnv {
  ADMIN_INGEST_URL?: string;
  ADMIN_WORKER_URL?: string;
  ADMIN_INGEST_KEY?: string;
  ADMIN_SECRET?: string;
}

export interface AiDiagnosisEnv {
  AI_WORKER_URL?: string;
}

export function trimTrailingSlash(value: string): string {
  return value.replace(/\/$/, '');
}

export function resolveAdminBaseUrl(env: AdminIngestEnv): string | null {
  const value = env.ADMIN_INGEST_URL ?? env.ADMIN_WORKER_URL;
  return value ? trimTrailingSlash(value) : null;
}

export function resolveAdminToken(env: AdminIngestEnv): string | null {
  return env.ADMIN_INGEST_KEY ?? env.ADMIN_SECRET ?? null;
}

export function resolveAiWorkerUrl(env: AiDiagnosisEnv): string | null {
  return env.AI_WORKER_URL ? trimTrailingSlash(env.AI_WORKER_URL) : null;
}

export interface SgpEnv {
  [key: string]: string | undefined;
}

export function resolveSgpCreds(
  env: SgpEnv,
  tenantId: string,
): { token: string; app: string; baseUrl: string } | null {
  const upper = tenantId.toUpperCase();
  const token = env[`SGP_TOKEN_${upper}`];
  const app = env[`SGP_APP_${upper}`];
  const baseUrl = env[`SGP_BASE_URL_${upper}`];
  if (!token || !app || !baseUrl) return null;
  return { token, app, baseUrl: trimTrailingSlash(baseUrl) };
}

// Religa de confiança (promessa de pagamento, issue #99) — toggle por tenant, mesmo padrão do
// GENIEACS_ENABLED: ausente ou qualquer valor diferente de 'true' desliga a feature. O backend
// valida este toggle antes de qualquer chamada de liberação ao SGP — não confia no frontend.
export function resolveReligaEnabled(env: SgpEnv, tenantId: string): boolean {
  return env[`RELIGA_ENABLED_${tenantId.toUpperCase()}`] === 'true';
}

const MASSIVA_TTL_HORAS_DEFAULT = 3;

// Rede de proteção contra alerta de massiva eterno: sem painel administrativo (M4) para o ISP
// marcar "resolvido", o alerta expira sozinho após N horas sem novo diagnóstico "provedor" do
// mesmo POP. Configurável por tenant porque o tempo de reparo típico varia por infraestrutura.
export function resolveMassivaTtlHoras(env: SgpEnv, tenantId: string): number {
  const raw = env[`MASSIVA_TTL_HORAS_${tenantId.toUpperCase()}`];
  const parsed = raw ? Number(raw) : NaN;
  return Number.isFinite(parsed) && parsed > 0 ? parsed : MASSIVA_TTL_HORAS_DEFAULT;
}

const GENIEACS_RX_THRESHOLD_DEFAULT_DBM = -27;

export interface GenieAcsEnv {
  [key: string]: string | undefined;
}

// null quando o tenant não tem GenieACS habilitado (issues #69/#70) — esse é o mecanismo que
// garante que ISPs sem GenieACS nunca geram log de erro: a chamada à NBI nem chega a acontecer.
export function resolveGenieAcsCreds(
  env: GenieAcsEnv,
  tenantId: string,
): { baseUrl: string; user?: string; pass?: string; thresholdDbm: number } | null {
  const upper = tenantId.toUpperCase();
  if (env[`GENIEACS_ENABLED_${upper}`] !== 'true') return null;

  const baseUrl = env[`GENIEACS_NBI_URL_${upper}`];
  if (!baseUrl) return null;

  const user = env[`GENIEACS_NBI_USER_${upper}`];
  const pass = env[`GENIEACS_NBI_PASS_${upper}`];

  const rawThreshold = env[`GENIEACS_RX_THRESHOLD_${upper}`];
  const parsedThreshold = rawThreshold ? Number(rawThreshold) : NaN;
  const thresholdDbm = Number.isFinite(parsedThreshold) ? parsedThreshold : GENIEACS_RX_THRESHOLD_DEFAULT_DBM;

  return {
    baseUrl: trimTrailingSlash(baseUrl),
    thresholdDbm,
    ...(user ? { user } : {}),
    ...(pass ? { pass } : {}),
  };
}
