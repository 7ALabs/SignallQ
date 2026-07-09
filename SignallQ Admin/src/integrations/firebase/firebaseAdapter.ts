import { apiClient } from "../../services/apiClient";
import {
  mockFirebaseStatus,
  mockFirebaseAnalytics,
  mockFirebaseCrashlytics,
  mockFirebaseAppVersions,
  mockFirebaseCrashIssues,
} from "./firebase.mock";
import {
  FirebaseIntegrationStatus,
  FirebaseAnalyticsSummary,
  FirebaseCrashlyticsSummary,
  FirebaseAppVersionCrashStats,
  FirebaseCrashIssue,
} from "./firebase.types";
import { DashboardFilters } from "../../services/adminMetricsService";

// O painel NÃO acessa o Firebase diretamente.
// Em modo mock: usa dados locais para desenvolvimento.
// Em modo real: todas as chamadas vão ao Cloudflare Admin Worker, que detém
// as credenciais do Firebase service account e serve dados já normalizados.

// Sentinela: indica que os dados não estão disponíveis nesta rota.
export const NOT_AVAILABLE = null;

// Formata timestamp ISO do worker no mesmo padrão pt-BR usado pelos demais
// cards de integração (ex.: "21/06/2026 14:05"). Sem valor real ainda: fallback.
function formatSyncTimestamp(isoTimestamp: string | null | undefined): string {
  if (!isoTimestamp) return "Nunca sincronizado";
  const date = new Date(isoTimestamp);
  if (Number.isNaN(date.getTime())) return "Nunca sincronizado";
  return date.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function buildQuery(filters: DashboardFilters): string {
  const params = new URLSearchParams();
  if (filters.environment) params.set("environment", filters.environment);
  if (filters.period) params.set("period", filters.period);
  return params.toString() ? `?${params.toString()}` : "";
}

// Estrutura crua retornada pela rota GA4 do worker.
interface GA4WorkerResponse {
  source: string;
  data?: {
    rows?: Array<{
      dimensionValues: Array<{ value: string }>;
      metricValues: Array<{ value: string }>;
    }>;
    dimensionHeaders?: Array<{ name: string }>;
    metricHeaders?: Array<{ name: string }>;
  };
}

// Estrutura crua retornada pelas rotas stub do worker.
interface StubWorkerResponse {
  source: string;
  [key: string]: unknown;
}

// Estrutura crua retornada pela rota de status do worker (ver admin-api-schema.md).
interface FirebaseStatusWorkerResponse {
  source: string;
  projectId?: string;
  status: "connected" | "mock" | "attention" | "planned" | "disabled";
  hasCredentials: boolean;
  ga4PropertyConfigured: boolean;
  lastSyncTimestamp?: string | null;
  eventsImported?: number;
  crashesImported?: number;
}

export async function getFirebaseIntegrationStatus(): Promise<FirebaseIntegrationStatus> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseStatus, {});
  }

  const raw = await apiClient.request<FirebaseStatusWorkerResponse>(
    "GET",
    "/admin/integrations/firebase/status"
  );

  // O worker não expõe "platform" nesta rota (fixo aqui na UI). lastSync e os
  // contadores vêm do estado persistido em D1 (admin_settings/'firebase_sync'),
  // gravado pelo último POST /admin/integrations/firebase/sync. Se nunca houve
  // sync bem-sucedido, o worker retorna lastSyncTimestamp: null.
  return {
    enabled: raw.hasCredentials,
    status: raw.status,
    message: raw.hasCredentials
      ? "Sincronizado via Conta de Serviço do Google Cloud Platform (GCP)"
      : "Credenciais do Firebase ainda não configuradas no Admin Worker",
    platform: "Android (Firebase Analytics + Crashlytics)",
    lastSyncTimestamp: formatSyncTimestamp(raw.lastSyncTimestamp),
    eventsImported: raw.eventsImported ?? 0,
    crashesImported: raw.crashesImported ?? 0,
  };
}

export async function getFirebaseAnalyticsSummary(
  filters: DashboardFilters = {}
): Promise<FirebaseAnalyticsSummary | null> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseAnalytics, filters);
    if (filters.environment === "staging") {
      return {
        ...result,
        activeUsersToday: Math.round(result.activeUsersToday * 0.12),
        crashFreeUsersPercentage: 99.8,
        crashFreeSessionsPercentage: 99.9,
      };
    }
    return result;
  }

  const raw = await apiClient.request<GA4WorkerResponse>(
    "GET",
    `/admin/integrations/firebase/analytics${buildQuery(filters)}`
  );

  // Rota retorna source != "firebase_analytics" ou sem rows: dados não disponíveis.
  if (raw.source !== "firebase_analytics" || !raw.data?.rows?.length) {
    return NOT_AVAILABLE;
  }

  // Parsear as rows da GA4 API.
  // dimensionValues[0] = data YYYYMMDD
  // metricValues[0] = activeUsers, [1] = sessions, [2] = crashAffectedUsers
  let activeUsersToday = 0;

  for (const row of raw.data.rows) {
    const activeUsersRaw = row.metricValues?.[0]?.value;
    if (activeUsersRaw) {
      activeUsersToday += parseInt(activeUsersRaw, 10) || 0;
    }
  }

  return {
    activeUsersToday,
    averageSessionsDurationMs: 0,
    crashFreeUsersPercentage: 0,
    crashFreeSessionsPercentage: 0,
    topEvents: [],
  };
}

export async function getFirebaseCrashlyticsSummary(
  filters: DashboardFilters = {}
): Promise<FirebaseCrashlyticsSummary | null> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseCrashlytics, filters);
    if (filters.environment === "staging") {
      return {
        unresolvedCrashesCount: 1,
        unresolvedNonFatalsCount: 3,
        affectedUsersCount: 4,
        totalCrashesTrend: "down",
      };
    }
    return result;
  }

  const raw = await apiClient.request<StubWorkerResponse>(
    "GET",
    `/admin/integrations/firebase/crashlytics${buildQuery(filters)}`
  );

  if (raw.source === "stub") {
    return NOT_AVAILABLE;
  }

  return raw as unknown as FirebaseCrashlyticsSummary;
}

export async function getFirebaseAppVersions(
  filters: DashboardFilters = {}
): Promise<FirebaseAppVersionCrashStats[] | null> {
  if (apiClient.isMockEnabled()) {
    const result = await apiClient.simulateFetch(mockFirebaseAppVersions, filters);
    if (filters.environment === "staging") {
      return result.map((v) => ({
        ...v,
        crashCount: Math.round(v.crashCount * 0.1),
        nonFatalCount: Math.round(v.nonFatalCount * 0.1),
        status: "stable" as const,
      }));
    }
    return result;
  }

  const raw = await apiClient.request<{
    source: string;
    versions: Array<{ version: string; totalCrashes: number; affectedUsers: number }>;
  }>("GET", `/admin/integrations/firebase/versions${buildQuery(filters)}`);

  // "no_credentials" (Firebase não configurado) e "no_data_yet" (export do BigQuery
  // ainda sem linhas) não têm dado real — o painel deve exibir "não configurado",
  // nunca fabricar números. O worker nunca retorna "stub" nesta rota (era um bug
  // deste adapter — ele nunca detectava a ausência de credenciais).
  if (raw.source === "no_credentials" || raw.source === "no_data_yet" || raw.source === "error") {
    return NOT_AVAILABLE;
  }

  return (raw.versions ?? []).map((v): FirebaseAppVersionCrashStats => ({
    appVersion: v.version,
    crashCount: v.totalCrashes ?? 0,
    nonFatalCount: 0, // worker não separa fatal/não-fatal por versão ainda.
    crashFreeUsersPercentage: 100, // worker não calcula base de usuários por versão ainda.
    status: (v.totalCrashes ?? 0) > 100 ? "critical" : (v.totalCrashes ?? 0) > 20 ? "unstable" : "stable",
  }));
}

export async function getFirebaseCrashIssues(
  filters: DashboardFilters = {}
): Promise<FirebaseCrashIssue[] | null> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch(mockFirebaseCrashIssues, filters);
  }

  const raw = await apiClient.request<StubWorkerResponse>(
    "GET",
    `/admin/integrations/firebase/crash-issues${buildQuery(filters)}`
  );

  if (raw.source === "stub") {
    return NOT_AVAILABLE;
  }

  return raw as unknown as FirebaseCrashIssue[];
}

export async function syncFirebaseMetrics(): Promise<{ jobId: string; status: string; startedAt: string }> {
  if (apiClient.isMockEnabled()) {
    return apiClient.simulateFetch({
      jobId: "job_fb_mock_" + Date.now().toString(36),
      status: "started",
      startedAt: new Date().toISOString(),
    });
  }
  return apiClient.request<{ jobId: string; status: string; startedAt: string }>(
    "POST",
    "/admin/integrations/firebase/sync",
    {}
  );
}
