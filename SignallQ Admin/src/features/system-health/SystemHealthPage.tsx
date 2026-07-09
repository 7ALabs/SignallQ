import React, { useCallback, useEffect, useState } from "react";
import { SectionCard } from "../../components/ui/SectionCard";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { MetricCard } from "../../components/ui/MetricCard";
import { D1StatusCard } from "./components/D1StatusCard";
import { IntegrationCheckRow } from "./components/IntegrationCheckRow";
import { LastEventsCard } from "./components/LastEventsCard";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { ChartCard } from "../../components/ui/ChartCard";
import { DataTable } from "../../components/ui/DataTable";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { systemHealthService, SystemHealthResponse, HealthStatus } from "../../services/systemHealthService";
import { AppEnvironment } from "../../types/admin";
import { MetricVerdict } from "../../types/metrics";

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
  onNavigate?: (path: string) => void;
}

interface ServiceRow {
  service: string;
  status: HealthStatus | "ok" | "error" | "loading";
  detail: string;
  message?: string;
}

function statusVerdict(status: string): MetricVerdict {
  return status === "ok" ? "excelente" : status === "not_configured" || status === "idle" ? "regular" : "fraco";
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
  onNavigate,
}) => {
  const [health, setHealth] = useState<SystemHealthResponse | null>(null);
  const [healthError, setHealthError] = useState<string | null>(null);
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

  useEffect(() => {
    load();
  }, [load, triggerRefreshCounter]);

  const d1Check = health?.checks.d1;
  const ingestCheck = health?.checks.ingest;

  // GH#552 (Fase 3) — tabela de investigação: um serviço monitorado por linha,
  // é a representação honesta mais próxima de "incidentes de infra" que os
  // dados atuais permitem (não há histórico de incidentes, só o check atual).
  const serviceRows: ServiceRow[] = health ? [
    { service: "Worker Admin API", status: adminWorker.status, detail: adminWorker.latencyMs != null ? `${adminWorker.latencyMs} ms` : "—" },
    { service: "D1 Database", status: d1Check?.status ?? "error", detail: d1Check?.latencyMs != null ? `${d1Check.latencyMs} ms` : "—", message: d1Check?.message },
    { service: "Credenciais Firebase", status: health.checks.firebaseCredentials.status, detail: health.checks.firebaseCredentials.latencyMs != null ? `${health.checks.firebaseCredentials.latencyMs} ms` : "—", message: health.checks.firebaseCredentials.message },
    { service: "Acesso BigQuery", status: health.checks.bigQuery.status, detail: health.checks.bigQuery.latencyMs != null ? `${health.checks.bigQuery.latencyMs} ms` : "—", message: health.checks.bigQuery.message },
    { service: "Ingest configurado", status: ingestCheck?.status ?? "error", detail: ingestCheck?.lastSuccessAt ? new Date(ingestCheck.lastSuccessAt).toLocaleString("pt-BR") : "Sem ingest registrado", message: ingestCheck?.message },
  ] : [];

  const failingServices = serviceRows.filter((r) => r.status === "error");

  const insightText = !health
    ? null
    : failingServices.length === 0
    ? "Todos os serviços monitorados (Worker Admin, D1, Firebase, BigQuery, ingest) respondem normalmente."
    : `${failingServices.length} serviço(s) com falha: ${failingServices.map((s) => s.service).join(", ")}. Verifique a tabela abaixo antes de escalar.`;

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

      {/* 1. KPIs — 4 checks reais de infraestrutura, com veredito (GH#552 Fase 3) */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Worker Admin"
          value={loading ? "—" : statusLabel(adminWorker.status)}
          verdict={loading ? undefined : statusVerdict(adminWorker.status)}
          verdictNote={adminWorker.latencyMs != null ? `${adminWorker.latencyMs} ms` : undefined}
          id="metric-worker-admin-status"
        />
        <MetricCard
          label="D1 Database"
          value={loading || !d1Check ? "—" : statusLabel(d1Check.status)}
          verdict={loading || !d1Check ? undefined : statusVerdict(d1Check.status)}
          id="metric-d1-status"
        />
        <MetricCard
          label="Credenciais Firebase"
          value={loading || !health ? "—" : statusLabel(health.checks.firebaseCredentials.status)}
          verdict={loading || !health ? undefined : statusVerdict(health.checks.firebaseCredentials.status)}
          id="metric-firebase-status"
        />
        <MetricCard
          label="Acesso BigQuery"
          value={loading || !health ? "—" : statusLabel(health.checks.bigQuery.status)}
          verdict={loading || !health ? undefined : statusVerdict(health.checks.bigQuery.status)}
          id="metric-bigquery-status"
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

      {/* 3. Bloco de explicação */}
      {insightText && <InsightBlock id="system-health-insight-block">{insightText}</InsightBlock>}

      {/* 4. Tabela de investigação — um serviço monitorado por linha (detalhe completo) */}
      <SectionCard
        title="Serviços monitorados"
        description="Status real de cada dependência, medido a cada chamada a /admin/system-health."
        id="system-health-services-table"
      >
        <DataTable
          data={serviceRows}
          columns={[
            { header: "Serviço", accessor: (r: ServiceRow) => <span className="font-sans font-semibold text-[var(--text-primary)]">{r.service}</span> },
            { header: "Status", accessor: (r: ServiceRow) => <StatusBadge status={r.status === "ok" ? "ok" : r.status === "error" ? "critical" : "attention"} customLabel={statusLabel(r.status)} /> },
            { header: "Latência / detalhe", accessor: (r: ServiceRow) => <span className="font-mono text-[var(--text-secondary)] text-xs">{r.detail}</span> },
            { header: "Mensagem", accessor: (r: ServiceRow) => <span className="text-xs text-[var(--text-tertiary)]">{r.message ?? "—"}</span> },
          ]}
          keyExtractor={(r) => r.service}
          emptyMessage="Sem checks disponíveis."
          id="system-health-services-datatable"
        />
      </SectionCard>

      {/* Detalhe adicional do D1 (mantido: traz o timestamp da última query, que
          não cabe na tabela compacta acima) */}
      <D1StatusCard
        status={!d1Check ? "error" : d1Check.status === "ok" ? "connected" : "error"}
        lastQuery={health?.timestamp ?? null}
        loading={loading}
      />

      {loading ? (
        <div className="space-y-3">
          {[0, 1, 2].map((i) => (
            <div key={i} className="h-16 rounded-xl animate-pulse" style={{ backgroundColor: "var(--bg-surface-hover)" }} />
          ))}
        </div>
      ) : (
        <SectionCard
          title="Integrações externas"
          description="Credenciais Firebase, acesso ao BigQuery (Crashlytics export) e configuração do ingest do app."
          id="integrations-status-card"
        >
          <div className="space-y-3">
            <IntegrationCheckRow
              label="Credenciais Firebase"
              status={health?.checks.firebaseCredentials.status ?? "error"}
              detail={health?.checks.firebaseCredentials.latencyMs != null ? `${health.checks.firebaseCredentials.latencyMs} ms` : undefined}
              message={health?.checks.firebaseCredentials.message}
            />
            <IntegrationCheckRow
              label="Acesso BigQuery"
              status={health?.checks.bigQuery.status ?? "error"}
              detail={health?.checks.bigQuery.latencyMs != null ? `${health.checks.bigQuery.latencyMs} ms` : undefined}
              message={health?.checks.bigQuery.message}
            />
            <IntegrationCheckRow
              label="Ingest configurado"
              status={ingestCheck?.status ?? "error"}
              detail={ingestCheck?.lastSuccessAt ? `Último ingest: ${new Date(ingestCheck.lastSuccessAt).toLocaleString("pt-BR")}` : "Sem ingest registrado"}
              message={ingestCheck?.message}
            />
          </div>
        </SectionCard>
      )}

      <LastEventsCard
        lastFailure={health?.lastFailure ?? null}
        lastSuccess={health?.lastSuccess ?? null}
        loading={loading}
      />

      {/* 5. Ações */}
      <ActionsRow
        id="system-health-actions-row"
        actions={[
          { label: "Ver logs no Cloudflare", onClick: () => window.open("https://dash.cloudflare.com", "_blank", "noopener,noreferrer"), variant: "secondary" },
          ...(onNavigate ? [{ label: "Ver Problemas & Incidentes", onClick: () => onNavigate("/errors") }] : []),
        ]}
      />
    </div>
  );
};
