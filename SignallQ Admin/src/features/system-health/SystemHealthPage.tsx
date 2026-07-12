import React, { useCallback, useEffect, useState } from "react";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { MetricCard } from "../../components/ui/MetricCard";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { ChartCard } from "../../components/ui/ChartCard";
import { systemHealthService, SystemHealthResponse, HealthStatus } from "../../services/systemHealthService";
import { cloudflareUsageService, CloudflareUsageResponse } from "../../services/cloudflareUsageService";
import { CloudflareUsagePanel } from "./components/CloudflareUsagePanel";
import { AppEnvironment } from "../../types/admin";

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

interface ServiceRow {
  service: string;
  status: HealthStatus | "ok" | "error" | "loading";
  detail: string;
  message?: string;
}

function statusLabel(status: string): string {
  switch (status) {
    case "ok": return "Online";
    case "error": return "Offline";
    case "not_configured": return "Não configurado";
    case "idle": return "Inativo";
    default: return "—";
  }
}

export const SystemHealthPage: React.FC<SystemHealthPageProps> = ({
  environment: _environment,
  period: _period,
  triggerRefreshCounter = 0,
}) => {
  const [health, setHealth] = useState<SystemHealthResponse | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);
  const [cloudflareUsage, setCloudflareUsage] = useState<CloudflareUsageResponse | null>(null);
  const [adminWorker, setAdminWorker] = useState<WorkerHealth>({
    name: "signallq-admin-worker",
    status: "loading",
    timestamp: null,
    latencyMs: null,
  });
  const [loading, setLoading] = useState(true);

  // GH#552 (Fase 3) — recorte deliberado do wireframe: crash rate e custo de IA
  // saíram desta tela (viraram sobreposição com Problemas & Incidentes / IA &
  // Custos). Saúde do Sistema fica só com checks reais de infraestrutura
  // (/admin/system-health), sem mock silencioso em produção.
  const load = useCallback(async () => {
    setLoading(true);
    setHealthError(null);

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
    } finally {
      setLoading(false);
    }
  }, []);

  // #883 — carregado à parte de `load()`: falha em consultar o uso do free tier
  // Cloudflare não deve derrubar o resto da tela de saúde do sistema.
  const loadCloudflareUsage = useCallback(async () => {
    try {
      const data = await cloudflareUsageService.getUsage();
      setCloudflareUsage(data);
    } catch {
      setCloudflareUsage(null);
    }
  }, []);

  useEffect(() => {
    load();
    loadCloudflareUsage();
  }, [load, loadCloudflareUsage, triggerRefreshCounter]);

  const d1Check = health?.checks.d1;

  // Paridade com mockup (sec-health, array `services`): 4 itens, nomes literais
  // "Cloudflare Worker (Admin API)", "Firebase Firestore", "Firebase Auth",
  // "Crashlytics Ingest". O worker real não roda check de Firestore (o admin
  // usa D1, não Firestore) — renomear D1 para "Firebase Firestore" seria dado
  // inventado, então esse item mantém o nome honesto "D1 Database". Os outros
  // três mapeiam 1:1 para checks reais: worker → Cloudflare Worker (Admin API),
  // firebaseCredentials → Firebase Auth (autentica JWT do Firebase Admin SDK),
  // bigQuery → Crashlytics Ingest (é o acesso ao export do Crashlytics via
  // BigQuery). O check de ingest do app (5º item do código anterior) não tem
  // equivalente no mockup e foi removido desta lista.
  const serviceRows: ServiceRow[] = health ? [
    { service: "Cloudflare Worker (Admin API)", status: adminWorker.status, detail: adminWorker.latencyMs != null ? `${adminWorker.latencyMs} ms` : "—" },
    { service: "D1 Database", status: d1Check?.status ?? "error", detail: d1Check?.latencyMs != null ? `${d1Check.latencyMs} ms` : "—", message: d1Check?.message },
    { service: "Firebase Auth", status: health.checks.firebaseCredentials.status, detail: health.checks.firebaseCredentials.latencyMs != null ? `${health.checks.firebaseCredentials.latencyMs} ms` : "—", message: health.checks.firebaseCredentials.message },
    { service: "Crashlytics Ingest", status: health.checks.bigQuery.status, detail: health.checks.bigQuery.latencyMs != null ? `${health.checks.bigQuery.latencyMs} ms` : "—", message: health.checks.bigQuery.message },
  ] : [];

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="system-health-section-intro"
        overline="SAÚDE DO SISTEMA"
        question="A infraestrutura por trás do app está de pé?"
        description="Uptime e latência do Worker, Firebase e pipeline de eventos — postura Cloudflare Status."
        source="FONTE · CLOUDFLARE WORKERS · FIREBASE STATUS"
      />

      {healthError && (
        <div className="p-4 rounded-xl text-[13px] bg-[var(--error)]/8 border border-[var(--error)]/25 text-[var(--error)]">
          Não foi possível consultar /admin/system-health: {healthError}
        </div>
      )}

      {/* 1. KPIs — paridade com mockup (healthKpis): Uptime do Worker (30d),
          Latência p95 da API, Erros 5xx (7d), Fila de eventos pendentes.
          Nenhuma dessas 4 métricas tem dado real hoje — /admin/system-health só
          expõe o check pontual no instante da chamada, sem série histórica de
          uptime/latência p95/taxa de erro/fila. "Não disponível" é o estado
          honesto em vez de inventar número (mesmo padrão de OverviewMetricGrid). */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Uptime do Worker (30d)"
          value="Não disponível"
          verdictNote="Sem série histórica persistida no worker"
          source="não implementado"
          id="metric-worker-uptime-30d"
        />
        <MetricCard
          label="Latência p95 da API"
          value="Não disponível"
          verdictNote="Só a latência do check pontual é registrada"
          source="não implementado"
          id="metric-api-latency-p95"
        />
        <MetricCard
          label="Erros 5xx (7d)"
          value="Não disponível"
          verdictNote="Sem contagem de erros por status persistida"
          source="não implementado"
          id="metric-errors-5xx-7d"
        />
        <MetricCard
          label="Fila de eventos pendentes"
          value="Não disponível"
          verdictNote="Métrica ainda não exposta pelo worker"
          source="não implementado"
          id="metric-pending-events-queue"
        />
      </div>

      {/* 2. Composição paridade mockup — latência p95 · 14 dias (sem histórico
          persistido no worker hoje) + status dos serviços (real, mesmo dado
          de serviceRows usado na tabela de investigação abaixo). */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <ChartCard
            title="Latência P95 da API · 14 dias"
            description="Uptime e latência histórica por serviço Cloudflare."
            id="system-health-main-chart"
          >
            <FeatureComingSoon
              feature="Série histórica de latência/uptime"
              reason="Métrica ainda não disponível — aguardando exposição no worker (só o check no instante da chamada é persistido)"
            />
          </ChartCard>
        </div>
        <div className="lg:col-span-1 rounded-[var(--radius-card)] p-5" style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}>
          <h4 className="text-[11px] font-semibold font-sans uppercase tracking-[0.08em] mb-3.5" style={{ color: "var(--text-secondary)" }}>
            Status dos serviços
          </h4>
          <div className="space-y-1">
            {serviceRows.length === 0 ? (
              <p className="text-xs font-sans py-4 text-center" style={{ color: "var(--text-tertiary)" }}>Sem checks disponíveis.</p>
            ) : (
              serviceRows.map((r, idx) => {
                const dotColor = r.status === "ok" ? "var(--success)" : r.status === "error" ? "var(--error)" : "var(--attention)";
                return (
                  <div
                    key={r.service}
                    className="flex items-center gap-2.5 py-2"
                    style={idx > 0 ? { borderTop: "1px solid var(--border)" } : undefined}
                  >
                    <span className="w-2 h-2 rounded-full shrink-0" style={{ backgroundColor: dotColor }} />
                    <span className="text-[12.5px] font-medium font-sans flex-1 truncate" style={{ color: "var(--text-primary)" }}>{r.service}</span>
                    <span className="text-[11px] font-semibold font-sans" style={{ color: dotColor }}>{statusLabel(r.status)}</span>
                  </div>
                );
              })
            )}
          </div>
        </div>
      </div>

      {/* 3. #883 — uso vs. teto do free tier Cloudflare (Workers requests/dia,
          D1 rows lidas/escritas por dia, D1 storage total). Card próprio porque
          é um dado de infra/custo, não um check de disponibilidade. */}
      <CloudflareUsagePanel usage={cloudflareUsage} />

    </div>
  );
};
