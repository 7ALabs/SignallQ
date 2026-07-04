import React, { useCallback, useEffect, useState } from "react";
import { SectionCard } from "../../components/ui/SectionCard";
import { MetricCard } from "../../components/ui/MetricCard";
import { WorkerStatusCard } from "./components/WorkerStatusCard";
import { D1StatusCard } from "./components/D1StatusCard";
import { AlertThresholdPanel } from "./components/AlertThresholdPanel";
import { IntegrationCheckRow } from "./components/IntegrationCheckRow";
import { LastEventsCard } from "./components/LastEventsCard";
import { apiClient } from "../../services/apiClient";
import { systemHealthService, SystemHealthResponse } from "../../services/systemHealthService";
import { alpha, mix } from "../../utils/color";
import { AppEnvironment } from "../../types/admin";

// SIG-261 — Thresholds configuráveis (hardcoded por enquanto).
// Crash rate acima de 2% é considerado crítico (limiar Play Store é <1%; aqui usamos
// 2% como gate para alerta no painel, dando margem para apps em fase de beta).
export const CRASH_RATE_THRESHOLD = 2;
// Custo diário de IA em USD: nosso modelo atual (Qwen3 CF + Gemini Flash) é free tier,
// então qualquer custo acima de $10/dia indica uso anômalo ou mudança de modelo pago.
export const IA_COST_DAILY_THRESHOLD = 10;

interface WorkerHealth {
  name: string;
  status: "ok" | "error" | "loading";
  timestamp: string | null;
  latencyMs: number | null;
}

interface SystemHealthPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter?: number;
}

export const SystemHealthPage: React.FC<SystemHealthPageProps> = ({
  environment,
  period,
  triggerRefreshCounter = 0,
}) => {
  const [health, setHealth] = useState<SystemHealthResponse | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);
  const [adminWorker, setAdminWorker] = useState<WorkerHealth>({
    name: "signallq-admin-worker",
    status: "loading",
    timestamp: null,
    latencyMs: null,
  });
  const [crashRatePercent, setCrashRatePercent] = useState<number | null>(null);
  const [aiCostToday, setAiCostToday] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setHealthError(null);

    // GH#425 — /admin/system-health é a única fonte de verdade para D1, credenciais
    // Firebase, acesso BigQuery e configuração de ingest. Sem fallback silencioso: se a
    // chamada falhar, o worker admin é marcado como "error", não como "ok" por omissão.
    try {
      const { data, clientLatencyMs } = await systemHealthService.getSystemHealth();
      setHealth(data);
      setAdminWorker({
        name: "signallq-admin-worker",
        status: data.checks.worker.status === "ok" ? "ok" : "error",
        timestamp: data.timestamp,
        latencyMs: clientLatencyMs ?? data.checks.worker.latencyMs ?? null,
      });
    } catch (e) {
      setHealth(null);
      setHealthError(e instanceof Error ? e.message : "Falha ao consultar /admin/system-health");
      setAdminWorker({
        name: "signallq-admin-worker",
        status: "error",
        timestamp: null,
        latencyMs: null,
      });
    }

    // Crash rate e custo de IA continuam vindo do overview/crashlytics — métricas de
    // produto, não checks de infraestrutura, então ficam fora do contrato de system-health.
    let crashRate: number | null = null;
    let aiCost: number | null = null;

    if (!apiClient.isMockEnabled() && import.meta.env.VITE_ADMIN_API_BASE_URL) {
      try {
        const apiPeriod = period === "today" ? "1d" : period;
        const raw = await apiClient.request<{ aiCostToday?: number }>(
          "GET",
          `/admin/metrics/overview?environment=${environment}&period=${apiPeriod}`
        );
        aiCost = raw.aiCostToday ?? 0;

        try {
          const crashData = await apiClient.request<{
            crashFreeUsersPercentage?: number;
            source?: string;
          }>("GET", "/admin/integrations/firebase/crashlytics");
          if (
            crashData.source !== "no_credentials" &&
            crashData.source !== "no_data_yet" &&
            crashData.crashFreeUsersPercentage != null
          ) {
            crashRate = parseFloat((100 - crashData.crashFreeUsersPercentage).toFixed(2));
          }
        } catch {
          // Crashlytics pode não estar configurado — silencioso apenas para esta métrica
          // secundária; o status real de BigQuery já é exibido pelo check de system-health.
        }
      } catch {
        // Overview indisponível — cards de crash rate/custo ficam em "—", sem inventar valor.
      }
    } else {
      // Mock com valores plausíveis para app em fase de beta brasileiro.
      crashRate = 0.8;
      aiCost = 0.0;
    }

    setCrashRatePercent(crashRate);
    setAiCostToday(aiCost);
    setLoading(false);
  }, [environment, period]);

  useEffect(() => {
    load();
  }, [load, triggerRefreshCounter]);

  const d1Check = health?.checks.d1;
  const ingestCheck = health?.checks.ingest;

  return (
    <div className="space-y-6">
      {/* Alertas ativos — SIG-261 */}
      <AlertThresholdPanel
        crashRatePercent={crashRatePercent}
        aiCostToday={aiCostToday}
        crashRateThreshold={CRASH_RATE_THRESHOLD}
        aiCostThreshold={IA_COST_DAILY_THRESHOLD}
        loading={loading}
      />

      {healthError && (
        <div
          className="p-4 rounded-xl text-[13px]"
          style={{
            backgroundColor: mix("var(--sq-error, #ef4444)", 8, "var(--bg-surface)"),
            border: `1px solid ${alpha("var(--sq-error, #ef4444)", 25)}`,
            color: "var(--sq-error, #ef4444)",
          }}
        >
          Não foi possível consultar /admin/system-health: {healthError}
        </div>
      )}

      {/* Métricas resumidas */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Crash Rate"
          value={loading || crashRatePercent == null ? "—" : `${crashRatePercent.toFixed(2)}%`}
          format="percentage"
          id="metric-crash-rate"
        />
        <MetricCard
          label="Custo IA Hoje"
          value={
            loading || aiCostToday == null
              ? "—"
              : aiCostToday === 0
              ? "Free tier"
              : `$${aiCostToday.toFixed(4)}`
          }
          id="metric-ai-cost-today"
        />
        <MetricCard
          label="Worker Admin"
          value={loading ? "—" : adminWorker.status === "ok" ? "Online" : "Offline"}
          id="metric-worker-admin-status"
        />
        <MetricCard
          label="D1 Database"
          value={loading || !d1Check ? "—" : d1Check.status === "ok" ? "Conectado" : "Erro"}
          id="metric-d1-status"
        />
      </div>

      {/* Status do Worker Admin — SIG-263 / GH#425 */}
      <SectionCard
        title="Worker Admin API"
        description="Status real do signallq-admin-worker, medido pela chamada a /admin/system-health. Latência medida no cliente."
        id="workers-status-card"
      >
        {loading ? (
          <div
            className="h-24 rounded-xl animate-pulse"
            style={{ backgroundColor: "var(--bg-surface-hover)" }}
          />
        ) : (
          <WorkerStatusCard worker={adminWorker} />
        )}
      </SectionCard>

      {/* Status do D1 — SIG-263 */}
      <D1StatusCard
        status={!d1Check ? "error" : d1Check.status === "ok" ? "connected" : "error"}
        lastQuery={health?.timestamp ?? null}
        loading={loading}
      />

      {/* Integrações externas — GH#425: credenciais Firebase, acesso BigQuery, ingest */}
      <SectionCard
        title="Integrações externas"
        description="Credenciais Firebase, acesso ao BigQuery (Crashlytics export) e configuração do ingest do app, verificados em tempo real pelo worker."
        id="integrations-status-card"
      >
        {loading ? (
          <div className="space-y-3">
            {[0, 1, 2].map((i) => (
              <div
                key={i}
                className="h-16 rounded-xl animate-pulse"
                style={{ backgroundColor: "var(--bg-surface-hover)" }}
              />
            ))}
          </div>
        ) : (
          <div className="space-y-3">
            <IntegrationCheckRow
              label="Credenciais Firebase"
              status={health?.checks.firebaseCredentials.status ?? "error"}
              detail={
                health?.checks.firebaseCredentials.latencyMs != null
                  ? `${health.checks.firebaseCredentials.latencyMs} ms`
                  : undefined
              }
              message={health?.checks.firebaseCredentials.message}
            />
            <IntegrationCheckRow
              label="Acesso BigQuery"
              status={health?.checks.bigQuery.status ?? "error"}
              detail={
                health?.checks.bigQuery.latencyMs != null
                  ? `${health.checks.bigQuery.latencyMs} ms`
                  : undefined
              }
              message={health?.checks.bigQuery.message}
            />
            <IntegrationCheckRow
              label="Ingest configurado"
              status={ingestCheck?.status ?? "error"}
              detail={
                ingestCheck?.lastSuccessAt
                  ? `Último ingest: ${new Date(ingestCheck.lastSuccessAt).toLocaleString("pt-BR")}`
                  : "Sem ingest registrado"
              }
              message={ingestCheck?.message}
            />
          </div>
        )}
      </SectionCard>

      {/* Última falha e último sucesso — GH#425 */}
      <LastEventsCard
        lastFailure={health?.lastFailure ?? null}
        lastSuccess={health?.lastSuccess ?? null}
        loading={loading}
      />
    </div>
  );
};
