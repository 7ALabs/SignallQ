import React from "react";
import { AlertTriangle } from "lucide-react";
import { appVersionsService, AppVersionUsage } from "../../services/appVersionsService";
import { integrationsService } from "../../integrations/integrationsService";
import { FirebaseAppVersionCrashStats } from "../../integrations/firebase/firebase.types";
import { GooglePlayCrashAnrSummary } from "../../integrations/google-play/googlePlay.types";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { ChartCard } from "../../components/ui/ChartCard";
import { BarChart } from "../../components/charts/BarChart";
import { MetricCard } from "../../components/ui/MetricCard";
import { LoadingState } from "../../components/ui/LoadingState";
import { EmptyState } from "../../components/ui/EmptyState";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { GlobalFilters } from "../../components/ui/GlobalFilters";
import { SectionIntro } from "../../components/ui/SectionIntro";
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
  // null = Android Publisher API não expõe ANR (só via export CSV, não implementado — ver GH#761).
  const [crashAnr, setCrashAnr] = React.useState<GooglePlayCrashAnrSummary | null>(null);
  const [focusVersion, setFocusVersion] = React.useState<string>("all");

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);

    Promise.all([
      appVersionsService.getAppVersions({ environment, period }),
      integrationsService.getFirebaseVersions({ environment, period }),
      integrationsService.getGooglePlayCrashAnr({ environment, period }),
    ]).then(([appVersionsResult, crashResult, crashAnrResult]) => {
      if (cancelled) return;
      setVersions(appVersionsResult.versions);
      setProductionVersion(appVersionsResult.productionVersion);
      setCrashStats(crashResult);
      setCrashAnr(crashAnrResult);
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

  // GH#746 — gráfico principal faltante: distribuição de sessões por versão,
  // dado real do D1 (appVersionsService), sempre disponível quando há versões
  // (guard de EmptyState acima já garante versions.length > 0 aqui). Crash rate
  // por versão entraria como segunda série só quando Crashlytics estiver
  // configurado — sem inventar contagem quando crashStats é null.
  const topVersionsBySessions = [...versions].sort((a, b) => b.sessions - a.sessions).slice(0, 8);
  // O InsightBlock abaixo relata a versão em foco com confiança — o gráfico
  // precisa sempre incluí-la, mesmo fora do top-8, senão o texto fala de uma
  // barra que o usuário nunca vê.
  const chartVersions =
    focusedVersion && !topVersionsBySessions.some((v) => v.appVersion === focusedVersion.appVersion)
      ? [...topVersionsBySessions, focusedVersion]
      : topVersionsBySessions;
  const chartData = chartVersions.map((v) => ({
    name: v.appVersion,
    sessões: v.sessions,
    ...(crashStats !== null ? { crashes: crashByVersion.get(v.appVersion)?.crashCount ?? 0 } : {}),
  }));

  // GH#552 (Fase 2) — síntese derivada de dado real já carregado. Sem BigQuery
  // configurado, a frase não afirma estabilidade — só reporta a ausência do dado.
  const insightText = focusedVersion
    ? crashStats === null
      ? `Versão ${focusedVersion.appVersion} tem ${focusedVersion.sessions.toLocaleString("pt-BR")} sessões registradas no período. Crash rate indisponível — Firebase Crashlytics não está configurado no worker.`
      : `Versão ${focusedVersion.appVersion} tem ${focusedVersion.sessions.toLocaleString("pt-BR")} sessões e ${(crashByVersion.get(focusedVersion.appVersion)?.crashCount ?? 0).toLocaleString("pt-BR")} crashes registrados no Crashlytics no período.`
    : null;

  return (
    <div className="flex flex-col gap-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="app-versions-section-intro"
        overline="RELEASES & QUALIDADE"
        question="Os últimos releases estão estáveis?"
        description="Adoção de versão, rollout gradual e qualidade (crash rate / ANR) por release — modelo Play Console Vitals."
        source="FONTE · GOOGLE PLAY CONSOLE"
      />

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

      {/* 2. KPIs — GH#781: 4º card (ANR) adicionado para paridade com o mockup.
          Android Publisher API não expõe ANR (só via export CSV/GCS, não
          implementado — ver googlePlayAdapter.ts GH#761), então em produção
          real esse card mostra estado vazio explícito em vez de inventar taxa. */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
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
          label="Cobertura Crashlytics"
          value={crashStats === null ? "Não configurado" : `${crashStats.length} versões monitoradas`}
          source={crashStats === null ? "sem credenciais" : "bigquery"}
        />
        <MetricCard
          label="ANR semanal (Play Console)"
          value={crashAnr === null ? "Não disponível" : crashAnr.anrCountWeekly}
          verdictNote={
            crashAnr === null
              ? "Android Publisher API não expõe ANR (só via export CSV, não implementado)"
              : undefined
          }
          source={crashAnr === null ? "não implementado" : "google play"}
        />
      </div>

      {/* 3. Gráfico principal — sessões por versão (D1, sempre real); crashes por
          versão entra como segunda série quando Crashlytics está configurado. */}
      <ChartCard
        title="Sessões por versão"
        description={
          crashStats === null
            ? "Volume de sessões de diagnóstico por versão e build, direto do D1, ordenado pelas versões mais ativas no período."
            : "Volume de sessões e crashes reportados por versão e build, ordenado pelas versões mais ativas no período."
        }
        id="versions-main-chart"
      >
        <BarChart
          data={chartData}
          xAxisKey="name"
          series={[
            { key: "sessões", name: "Sessões", color: "var(--info)" },
            ...(crashStats !== null ? [{ key: "crashes", name: "Crashes (Crashlytics)", color: "var(--error)" }] : []),
          ]}
        />
      </ChartCard>

      {/* 4. Bloco de explicação — antes da tabela, só quando há versão em foco */}
      {insightText && <InsightBlock id="versions-insight-block">{insightText}</InsightBlock>}

      {/* 5. Tabela de investigação — dados por release, versão em foco destacada.
          GH#781 (paridade mockup): coluna "Base instalada" do mockup vira aqui
          "Participação em sessões" (share real de v.sessions sobre o total do
          período) — o worker não expõe % de base instalada (isso viria da
          Android Publisher API, que hoje só retorna rating, ver GH#761).
          "Situação" é derivada honestamente comparando com productionVersion,
          sem inventar rollout%/crash rate/ANR que o Play Console real não
          expõe ainda (ver googlePlayAdapter.ts). */}
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
            {
              header: "Participação em sessões",
              accessor: (v) => {
                const totalSessions = versions.reduce((sum, x) => sum + x.sessions, 0);
                const pct = totalSessions > 0 ? Math.round((v.sessions / totalSessions) * 100) : 0;
                return (
                  <div className="flex items-center gap-2 min-w-[110px]">
                    <div className="flex-1 h-1.5 rounded-full overflow-hidden" style={{ backgroundColor: "var(--bg-base)" }}>
                      <div className="h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: "var(--accent-blue)" }} />
                    </div>
                    <span className="text-[11px] font-sans w-9 text-right" style={{ color: "var(--text-secondary)" }}>{pct}%</span>
                  </div>
                );
              },
            },
            {
              header: "Situação",
              accessor: (v) => (
                <StatusBadge status={v.appVersion === productionVersion?.appVersion ? "stable" : "deprecated"} />
              ),
            },
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

      {/* 6. Ações */}
      <ActionsRow
        id="versions-actions-row"
        actions={[
          { label: "Ver erros relacionados", onClick: () => onNavigate("/errors") },
        ]}
      />
    </div>
  );
};
