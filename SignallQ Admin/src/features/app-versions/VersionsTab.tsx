import React from "react";
import { AlertTriangle } from "lucide-react";
import { appVersionsService, AppVersionUsage } from "../../services/appVersionsService";
import { integrationsService } from "../../integrations/integrationsService";
import { FirebaseAppVersionCrashStats, FirebaseAppVersionsResult } from "../../integrations/firebase/firebase.types";
import { crashFreeReason } from "../../utils/crashlytics";
import { GooglePlayCrashAnrSummary, GooglePlayReleaseTrack, GooglePlayAppVersionStats } from "../../integrations/google-play/googlePlay.types";
import { DataTable } from "../../components/ui/DataTable";
import { SectionCard } from "../../components/ui/SectionCard";
import { MetricCard } from "../../components/ui/MetricCard";
import { LoadingState } from "../../components/ui/LoadingState";
import { EmptyState } from "../../components/ui/EmptyState";
import { StatusBadge } from "../../components/ui/StatusBadge";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { MetricVerdict } from "../../types/metrics";
import { AppEnvironment } from "../../types/admin";

interface VersionsTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

function formatDate(unixSeconds: number | null): string {
  if (!unixSeconds) return "—";
  return new Date(unixSeconds * 1000).toLocaleDateString("pt-BR");
}

// Crash rate 0,05% é o exemplo "Excelente" do mockup — limiares seguem a
// mesma lógica de crash-free rate usada no Play Console Vitals.
function crashRateVerdict(pct: number): MetricVerdict {
  if (pct < 0.1) return "excelente";
  if (pct < 0.5) return "bom";
  if (pct < 1) return "regular";
  return "fraco";
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
  // #880 (achado 3): guarda o `source` real do worker (no_credentials/
  // no_data_yet/error/bigquery) — não colapsa mais em null genérico.
  const [crashResultState, setCrashResultState] = React.useState<FirebaseAppVersionsResult | null>(null);
  const crashStats = crashResultState?.source === "bigquery" ? crashResultState.versions : null;
  // null = Android Publisher API não expõe ANR (só via export CSV, não implementado — ver GH#761).
  const [crashAnr, setCrashAnr] = React.useState<GooglePlayCrashAnrSummary | null>(null);
  // [] = Android Publisher API de tracks/base instalada por versão ainda não implementada em produção.
  const [releaseTracks, setReleaseTracks] = React.useState<GooglePlayReleaseTrack[]>([]);
  const [appVersionStats, setAppVersionStats] = React.useState<GooglePlayAppVersionStats[]>([]);

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);

    Promise.all([
      appVersionsService.getAppVersions({ environment, period }),
      integrationsService.getFirebaseVersions({ environment, period }),
      integrationsService.getGooglePlayCrashAnr({ environment, period }),
      integrationsService.getGooglePlayTracks(),
      integrationsService.getGooglePlayVersions({ environment, period }),
    ]).then(([appVersionsResult, crashResult, crashAnrResult, tracksResult, versionStatsResult]) => {
      if (cancelled) return;
      setVersions(appVersionsResult.versions);
      setProductionVersion(appVersionsResult.productionVersion);
      setCrashResultState(crashResult);
      setCrashAnr(crashAnrResult);
      setReleaseTracks(tracksResult);
      setAppVersionStats(versionStatsResult);
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

  const installShareByVersion = React.useMemo(() => {
    const map = new Map<string, GooglePlayAppVersionStats>();
    appVersionStats.forEach((s) => map.set(s.versionCode, s));
    return map;
  }, [appVersionStats]);

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

  // KPI 1 — "Versão estável": versão em produção + % da base instalada nela,
  // quando a Android Publisher API retornar essa versão em appVersionStats.
  const productionInstallStat = productionVersion ? installShareByVersion.get(productionVersion.appVersion) : undefined;

  // KPI 2 — "Em rollout": track de release com rollout gradual (<100%) ainda
  // em andamento. releaseTracks vem vazio em produção real (endpoint de
  // tracks não implementado — ver GH#761), então o card assume "Não disponível".
  const rolloutTrack = releaseTracks.find((t) => t.rolloutPercentage < 100) ?? null;

  // KPI 3 — "Crash rate (release atual)": crashByVersion (Crashlytics) contra
  // as sessões reais da versão em produção (D1). Só existe quando Crashlytics
  // está configurado (crashStats !== null) e há sessões no período.
  const productionCrashRate =
    crashStats !== null && productionVersion && productionVersion.sessions > 0
      ? ((crashByVersion.get(productionVersion.appVersion)?.crashCount ?? 0) / productionVersion.sessions) * 100
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

      {/* 1. KPIs — paridade com o mockup: Versão estável / Em rollout / Crash
          rate (release atual) / ANR rate (release atual). Base instalada e
          tracks de rollout dependem de endpoints da Android Publisher API
          ainda não implementados em produção (ver googlePlayAdapter.ts
          GH#761) — nesse caso o card mostra "Não disponível" em vez de
          inventar percentual. */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <MetricCard
          label="Versão estável"
          value={productionVersion ? productionVersion.appVersion : "—"}
          verdictNote={
            productionInstallStat
              ? `${productionInstallStat.activeUsersPercent}% da base instalada`
              : "Base instalada não disponível"
          }
          source={productionInstallStat ? "google play" : "não implementado"}
        />
        <MetricCard
          label="Em rollout"
          value={rolloutTrack ? rolloutTrack.versionCode : "Não disponível"}
          verdictNote={
            rolloutTrack
              ? `${rolloutTrack.rolloutPercentage}% da base instalada`
              : "Android Publisher API não retorna tracks de rollout ainda"
          }
          source={rolloutTrack ? "google play" : "não implementado"}
        />
        <MetricCard
          label="Crash rate (release atual)"
          value={productionCrashRate !== null ? `${productionCrashRate.toFixed(2)}%` : "Não disponível"}
          verdict={productionCrashRate !== null ? crashRateVerdict(productionCrashRate) : undefined}
          verdictNote={
            productionCrashRate === null
              ? crashStats === null
                ? crashFreeReason(crashResultState?.source)
                : "Sem sessões da versão em produção no período"
              : undefined
          }
          source={productionCrashRate !== null ? "crashlytics" : "sem credenciais"}
        />
        {/* #880 (achado 16): crashAnr era buscado e nunca lido — card ficava
            hardcoded mesmo com dado real disponível. anrCountWeekly é agregado
            do app inteiro (a Android Publisher API não quebra ANR por versão,
            só via export CSV — isso continua indisponível), então o card segue
            sem valor numérico de "release atual" pra não fabricar taxa por
            versão, mas agora mostra o dado real que existe em vez de ignorá-lo. */}
        <MetricCard
          label="ANR rate (release atual)"
          value="Não disponível"
          verdictNote={
            crashAnr
              ? `${crashAnr.anrCountWeekly} ANRs no app inteiro nos últimos 7 dias — sem quebra por versão (Android Publisher API só expõe isso via export CSV)`
              : "Android Publisher API não expõe ANR por versão (só via export CSV, não implementado)"
          }
          source={crashAnr ? "google play (agregado, não por versão)" : "não implementado"}
        />
      </div>

      {/* 2. Tabela de investigação — versões em produção, alinhada às colunas
          do mockup (Versão / Base instalada / Crash rate / Status /
          Lançamento). Coluna "ANR rate" removida (#1047) — mostrava sempre
          "Não disponível" e o dado já existe no KPI "ANR rate (release
          atual)" acima, tornando a coluna redundante; ausente nos dois
          protótipos (Md3AppVersionsContent.dc.html e
          AppVersionsScreen.dc.html). "Base instalada" fica "Não disponível"
          quando a Android Publisher API não retorna o dado para a versão
          (endpoint ainda não implementado — ver googlePlayAdapter.ts). */}
      <SectionCard
        title="Versões em produção"
        description={
          crashResultState?.environmentScope === "all"
            ? "Sessões de diagnóstico agrupadas por versão e build, direto do D1 (filtradas por ambiente). Crash rate vem do Crashlytics/BigQuery, que não distingue produção de staging — coluna reflete todos os ambientes."
            : "Sessões de diagnóstico agrupadas por versão e build, direto do D1, cruzadas com Crashlytics e Google Play quando disponíveis."
        }
      >
        <DataTable
          data={versions}
          keyExtractor={(v) => `${v.appVersion}-${v.versionCode}-${v.distChannel}-${v.buildType}`}
          rowClassName={(v) => (v.appVersion === productionVersion?.appVersion ? "bg-[var(--primary)]/5" : "")}
          columns={[
            {
              header: "Versão",
              accessor: (v) => `${v.appVersion} (build ${v.versionCode ?? "—"})`,
            },
            {
              header: "Base instalada",
              accessor: (v) => {
                const stat = installShareByVersion.get(v.appVersion);
                if (!stat) {
                  return (
                    <span className="inline-flex items-center gap-1 text-[11px]" style={{ color: "var(--text-tertiary)" }}>
                      <AlertTriangle className="w-3 h-3" />
                      Não disponível
                    </span>
                  );
                }
                const pct = stat.activeUsersPercent;
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
              header: "Crash rate",
              accessor: (v) => {
                if (crashStats === null) {
                  return (
                    <span
                      className="inline-flex items-center gap-1 text-[11px]"
                      style={{ color: "var(--text-tertiary)" }}
                      title={crashFreeReason(crashResultState?.source)}
                    >
                      <AlertTriangle className="w-3 h-3" />
                      {crashResultState?.source === "no_credentials" ? "Não configurado" : "Sem dados ainda"}
                    </span>
                  );
                }
                if (v.sessions === 0) return "—";
                const crashCount = crashByVersion.get(v.appVersion)?.crashCount ?? 0;
                return `${((crashCount / v.sessions) * 100).toFixed(2)}%`;
              },
            },
            {
              header: "Status",
              // #880 (achado 13): qualquer build que não fosse a mais recente do
              // Play Store virava "Obsoleto" — inclusive beta ativo distribuído
              // via Firebase App Distribution, que não é obsoleto, é outro canal.
              accessor: (v) => {
                const status =
                  v.appVersion === productionVersion?.appVersion
                    ? "stable"
                    : v.distChannel === "firebase_app_distribution"
                      ? "beta"
                      : "deprecated";
                return <StatusBadge status={status} />;
              },
            },
            { header: "Lançamento", accessor: (v) => formatDate(v.firstSeen) },
          ]}
        />
      </SectionCard>

      {/* 3. Notas de release recentes — o worker ainda não expõe changelog por
          versão (rota /admin/metrics/app-versions não retorna esse campo),
          então o card mostra estado vazio explícito em vez de composição
          ausente. */}
      <SectionCard
        title="Notas de release recentes"
        description="Resumo do que mudou em cada versão publicada."
      >
        <EmptyState
          id="versions-release-notes-empty"
          title="Notas de release não disponíveis"
          description="O worker ainda não expõe changelog por versão — /admin/metrics/app-versions retorna só métricas de uso, não o texto de release notes."
        />
      </SectionCard>
    </div>
  );
};
