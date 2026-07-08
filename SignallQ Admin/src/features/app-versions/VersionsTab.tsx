import React from "react";
import { AlertTriangle } from "lucide-react";
import { appVersionsService, AppVersionUsage } from "../../services/appVersionsService";
import { integrationsService } from "../../integrations/integrationsService";
import { FirebaseAppVersionCrashStats } from "../../integrations/firebase/firebase.types";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { MetricCard } from "../../components/ui/MetricCard";
import { LoadingState } from "../../components/ui/LoadingState";
import { EmptyState } from "../../components/ui/EmptyState";
import { GlobalFilters } from "../../components/ui/GlobalFilters";
import { InsightBlock } from "../../components/ui/InsightBlock";
import { ActionsRow } from "../../components/ui/ActionsRow";
import { AppEnvironment } from "../../types/admin";

interface VersionsTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

const DIST_CHANNEL_LABEL: Record<string, string> = {
  play_store: "Play Store",
  firebase_app_distribution: "Firebase App Distribution",
  sideload: "Sideload",
};

function distChannelLabel(channel: string): string {
  return DIST_CHANNEL_LABEL[channel] ?? (channel === "unknown" ? "Desconhecido" : channel);
}

function formatDate(unixSeconds: number | null): string {
  if (!unixSeconds) return "—";
  return new Date(unixSeconds * 1000).toLocaleDateString("pt-BR");
}

export const VersionsTab: React.FC<VersionsTabProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  const [loading, setLoading] = React.useState(true);
  const [versions, setVersions] = React.useState<AppVersionUsage[]>([]);
  const [productionVersion, setProductionVersion] = React.useState<AppVersionUsage | null>(null);
  // null = Firebase/Crashlytics não configurado ou sem dados ainda (não é "zero crashes").
  const [crashStats, setCrashStats] = React.useState<FirebaseAppVersionCrashStats[] | null>(null);
  const [focusVersion, setFocusVersion] = React.useState<string>("all");

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);

    Promise.all([
      appVersionsService.getAppVersions({ environment, period }),
      integrationsService.getFirebaseVersions({ environment, period }),
    ]).then(([appVersionsResult, crashResult]) => {
      if (cancelled) return;
      setVersions(appVersionsResult.versions);
      setProductionVersion(appVersionsResult.productionVersion);
      setCrashStats(crashResult);
      setLoading(false);
    });

    return () => {
      cancelled = true;
    };
  }, [environment, period, triggerRefreshCounter]);

  const crashByVersion = React.useMemo(() => {
    const map = new Map<string, FirebaseAppVersionCrashStats>();
    (crashStats ?? []).forEach((c) => map.set(c.appVersion, c));
    return map;
  }, [crashStats]);

  if (loading) {
    return <LoadingState message="Buscando sessões por versão no D1..." />;
  }

  if (versions.length === 0) {
    return (
      <EmptyState
        title="Nenhuma versão registrada no período"
        description="Nenhuma sessão de diagnóstico com dados de versão foi recebida via /ingest/diagnostic no período e ambiente selecionados."
      />
    );
  }

  const focusedVersion = focusVersion === "all" ? null : versions.find((v) => v.appVersion === focusVersion) ?? null;

  // GH#552 (Fase 2) — síntese derivada de dado real já carregado. Sem BigQuery
  // configurado, a frase não afirma estabilidade — só reporta a ausência do dado.
  const insightText = focusedVersion
    ? crashStats === null
      ? `Versão ${focusedVersion.appVersion} tem ${focusedVersion.sessions.toLocaleString("pt-BR")} sessões registradas no período. Crash rate indisponível — Firebase Crashlytics não está configurado no worker.`
      : `Versão ${focusedVersion.appVersion} tem ${focusedVersion.sessions.toLocaleString("pt-BR")} sessões e ${(crashByVersion.get(focusedVersion.appVersion)?.crashCount ?? 0).toLocaleString("pt-BR")} crashes registrados no Crashlytics no período.`
    : null;

  return (
    <div className="flex flex-col gap-6">
      {/* 1. Filtro — versão em foco (usa só versões reais já carregadas) */}
      <GlobalFilters
        id="versions-global-filters"
        filters={[
          {
            key: "version",
            label: "Versão foco",
            value: focusVersion,
            onChange: setFocusVersion,
            options: [
              { label: "Todas", value: "all" },
              ...versions.map((v) => ({ label: `${v.appVersion} (build ${v.versionCode ?? "—"})`, value: v.appVersion })),
            ],
          },
        ]}
      />

      {/* 2. KPIs */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <MetricCard
          label="Versão em produção"
          value={
            productionVersion
              ? `${productionVersion.appVersion} (build ${productionVersion.versionCode ?? "—"})`
              : "—"
          }
          source="d1"
        />
        <MetricCard
          label="Canal de distribuição"
          value={productionVersion ? distChannelLabel(productionVersion.distChannel) : "—"}
          source="d1"
        />
        <MetricCard
          label="Crash-free (Crashlytics)"
          value={crashStats === null ? "Não configurado" : `${crashStats.length} versões monitoradas`}
          source={crashStats === null ? "sem credenciais" : "bigquery"}
        />
      </div>

      {/* 3. Bloco de explicação — antes da tabela, só quando há versão em foco */}
      {insightText && <InsightBlock id="versions-insight-block">{insightText}</InsightBlock>}

      {/* 4. Tabela de investigação — dados por release, versão em foco destacada */}
      <SectionCard
        title="Dados por release"
        description="Sessões de diagnóstico agrupadas por versão, build e canal de distribuição, direto do D1."
      >
        <DataTable
          data={versions}
          keyExtractor={(v) => `${v.appVersion}-${v.versionCode}-${v.distChannel}-${v.buildType}`}
          rowClassName={(v) => (v.appVersion === focusVersion ? "bg-[var(--primary)]/5" : "")}
          columns={[
            {
              header: "Versão",
              accessor: (v) => `${v.appVersion} (build ${v.versionCode ?? "—"})`,
            },
            { header: "Canal", accessor: (v) => distChannelLabel(v.distChannel) },
            { header: "Build", accessor: (v) => v.buildType },
            { header: "Sessões", accessor: (v) => v.sessions.toLocaleString("pt-BR") },
            {
              header: "Score médio",
              accessor: (v) => (v.avgScore != null ? String(v.avgScore) : "—"),
            },
            { header: "Última sessão", accessor: (v) => formatDate(v.lastSeen) },
            {
              header: "Crashes",
              accessor: (v) => {
                if (crashStats === null) {
                  return (
                    <span
                      className="inline-flex items-center gap-1 text-[11px]"
                      style={{ color: "var(--text-tertiary)" }}
                    >
                      <AlertTriangle className="w-3 h-3" />
                      Não configurado
                    </span>
                  );
                }
                const stat = crashByVersion.get(v.appVersion);
                return stat ? stat.crashCount.toLocaleString("pt-BR") : "—";
              },
            },
          ]}
        />
      </SectionCard>

      {crashStats === null && (
        <p className="text-xs" style={{ color: "var(--text-tertiary)" }}>
          Crash rate por versão depende da exportação BigQuery do Firebase Crashlytics — configure
          as credenciais do Firebase no worker <code>signallq-admin-worker</code> para habilitar.
        </p>
      )}

      {/* 5. Ações */}
      <ActionsRow
        id="versions-actions-row"
        actions={[
          { label: "Ver erros relacionados", onClick: () => onNavigate("/errors") },
        ]}
      />
    </div>
  );
};
