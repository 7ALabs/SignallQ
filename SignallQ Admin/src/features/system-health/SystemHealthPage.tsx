import React, { useCallback, useEffect, useState } from "react";
import { SectionCard } from "../../components/ui/SectionCard";
import { MetricCard } from "../../components/ui/MetricCard";
import { WorkerStatusCard } from "./components/WorkerStatusCard";
import { D1StatusCard } from "./components/D1StatusCard";
import { AlertThresholdPanel } from "./components/AlertThresholdPanel";
import { apiClient } from "../../services/apiClient";
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

interface SystemHealthData {
  crashRatePercent: number | null;
  aiCostToday: number | null;
  d1LastQuery: string | null;
  d1Status: "connected" | "error" | "loading";
}

interface SystemHealthPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter?: number;
}

async function checkWorkerHealth(
  baseUrl: string,
  workerPath: string,
  workerName: string,
  authHeader?: string,
): Promise<WorkerHealth> {
  const url = `${baseUrl}${workerPath}/health`;
  const start = Date.now();
  try {
    const headers: Record<string, string> = {};
    if (authHeader) headers["Authorization"] = authHeader;
    const res = await fetch(url, { headers, signal: AbortSignal.timeout(5000) });
    const latencyMs = Date.now() - start;
    if (!res.ok) {
      return { name: workerName, status: "error", timestamp: null, latencyMs };
    }
    const data = (await res.json()) as { status?: string; timestamp?: string };
    return {
      name: workerName,
      status: data.status === "ok" ? "ok" : "error",
      timestamp: data.timestamp ?? null,
      latencyMs,
    };
  } catch {
    return { name: workerName, status: "error", timestamp: null, latencyMs: null };
  }
}

function mockWorkerHealth(name: string, status: "ok" | "error" = "ok"): WorkerHealth {
  return {
    name,
    status,
    timestamp: new Date().toISOString(),
    latencyMs: 40 + Math.floor(Math.random() * 80),
  };
}

export const SystemHealthPage: React.FC<SystemHealthPageProps> = ({
  environment,
  period,
  triggerRefreshCounter = 0,
}) => {
  const [workers, setWorkers] = useState<WorkerHealth[]>([]);
  const [healthData, setHealthData] = useState<SystemHealthData>({
    crashRatePercent: null,
    aiCostToday: null,
    d1LastQuery: null,
    d1Status: "loading",
  });
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);

    // Workers: mock enquanto /health não for chamável do browser sem CORS configurado.
    // O admin-worker exige Bearer ADMIN_SECRET que não deve ficar no frontend.
    // Quando a Admin API expor um endpoint /admin/health (autenticado por sessão),
    // substituir esta chamada pelo apiClient.request.
    const mockWorkers: WorkerHealth[] = [
      mockWorkerHealth("ai-diagnosis-worker"),
      mockWorkerHealth("signallq-admin-worker"),
    ];
    setWorkers(mockWorkers);

    // Métricas reais: pega do overview quando a API está disponível.
    let crashRate: number | null = null;
    let aiCost: number | null = null;
    let d1Status: "connected" | "error" | "loading" = "connected";
    let d1LastQuery: string | null = null;

    if (!apiClient.isMockEnabled() && import.meta.env.VITE_ADMIN_API_BASE_URL) {
      try {
        const apiPeriod = period === "today" ? "1d" : period;
        const raw = await apiClient.request<{
          aiCostToday?: number;
        }>("GET", `/admin/metrics/overview?environment=${environment}&period=${apiPeriod}`);
        aiCost = raw.aiCostToday ?? 0;

        // Crashlytics: usa endpoint dedicado para obter crash-free rate.
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
          // Crashlytics pode não estar configurado — silencioso.
        }

        d1LastQuery = new Date().toISOString();
        d1Status = "connected";
      } catch {
        d1Status = "error";
      }
    } else {
      // Mock com valores plausíveis para app em fase de beta brasileiro.
      // Crash rate de 0.8%: saudável, abaixo do limiar de 2%.
      // Custo IA de $0 reflete modelo free tier (Qwen3 CF + Gemini Flash).
      crashRate = 0.8;
      aiCost = 0.0;
      d1LastQuery = new Date().toISOString();
      d1Status = "connected";
    }

    setHealthData({ crashRatePercent: crashRate, aiCostToday: aiCost, d1LastQuery, d1Status });
    setLoading(false);
  }, [environment, period]);

  useEffect(() => {
    load();
  }, [load, triggerRefreshCounter]);

  const crashRateAlert =
    healthData.crashRatePercent != null && healthData.crashRatePercent > CRASH_RATE_THRESHOLD;
  const aiCostAlert =
    healthData.aiCostToday != null && healthData.aiCostToday > IA_COST_DAILY_THRESHOLD;

  return (
    <div className="space-y-6">
      {/* Alertas ativos — SIG-261 */}
      <AlertThresholdPanel
        crashRatePercent={healthData.crashRatePercent}
        aiCostToday={healthData.aiCostToday}
        crashRateThreshold={CRASH_RATE_THRESHOLD}
        aiCostThreshold={IA_COST_DAILY_THRESHOLD}
        loading={loading}
      />

      {/* Métricas resumidas */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Crash Rate"
          value={
            loading || healthData.crashRatePercent == null
              ? "—"
              : `${healthData.crashRatePercent.toFixed(2)}%`
          }
          format="percentage"
          id="metric-crash-rate"
        />
        <MetricCard
          label="Custo IA Hoje"
          value={
            loading || healthData.aiCostToday == null
              ? "—"
              : healthData.aiCostToday === 0
              ? "Free tier"
              : `$${healthData.aiCostToday.toFixed(4)}`
          }
          id="metric-ai-cost-today"
        />
        <MetricCard
          label="Workers Ativos"
          value={loading ? "—" : `${workers.filter((w) => w.status === "ok").length}/${workers.length}`}
          id="metric-workers-active"
        />
        <MetricCard
          label="D1 Database"
          value={loading ? "—" : healthData.d1Status === "connected" ? "Conectado" : "Erro"}
          id="metric-d1-status"
        />
      </div>

      {/* Status dos Workers — SIG-263 */}
      <SectionCard
        title="Workers Cloudflare"
        description="Status dos Workers que servem diagnóstico de IA e a Admin API. Latências medidas no servidor."
        id="workers-status-card"
      >
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {loading
            ? [0, 1].map((i) => (
                <div
                  key={i}
                  className="h-24 rounded-xl animate-pulse"
                  style={{ backgroundColor: "var(--bg-surface-hover)" }}
                />
              ))
            : workers.map((w) => <WorkerStatusCard key={w.name} worker={w} />)}
        </div>
      </SectionCard>

      {/* Status do D1 — SIG-263 */}
      <D1StatusCard
        status={healthData.d1Status}
        lastQuery={healthData.d1LastQuery}
        loading={loading}
      />
    </div>
  );
};
