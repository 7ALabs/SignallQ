import React from "react";
import { diagnosticsService } from "../../services/diagnosticsService";
import { adminMetricsService } from "../../services/adminMetricsService";
import { DiagnosticsMetricGrid } from "./components/DiagnosticsMetricGrid";
import { FailureReasonsPanel } from "./components/FailureReasonsPanel";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { ChartCard } from "../../components/ui/ChartCard";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { LineChart } from "../../components/charts/LineChart";
import { DiagnosticSession, DiagnosticsSummary } from "../../types/diagnostics";
import { AppEnvironment } from "../../types/admin";

interface DiagnosticsPageProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
  onNavigate?: (path: string) => void;
}

// GH#781 (paridade mockup) — sec-diagnostics no mockup só tem SectionIntro +
// KPIs + o par "Diagnósticos executados · 14 dias" / "Motivos de falha". Sem
// filtros locais (o único seletor de período/ambiente é o header global do
// dashboard), sem gráfico extra por tipo de rede, sem funil, sem tabela de
// agregação e sem tabela/inspetor de sessões — tudo isso não está no mockup
// desta tela.
export const DiagnosticsPage: React.FC<DiagnosticsPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [sessions, setSessions] = React.useState<DiagnosticSession[]>([]);
  const [summary, setSummary] = React.useState<DiagnosticsSummary | null>(null);
  const [timelineData, setTimelineData] = React.useState<any[]>([]);

  React.useEffect(() => {
    let active = true;
    Promise.all([
      diagnosticsService.getDiagnosticSessions({ environment, period }),
      diagnosticsService.getDiagnosticsSummary({ environment, period }),
      adminMetricsService.getDiagnosticsTimeline({ environment, period }),
    ])
      .then(([sessionsData, summaryData, timelineRes]) => {
        if (!active) return;
        setSessions(sessionsData);
        setSummary(summaryData);
        setTimelineData(timelineRes);
      })
      .catch((err) => console.error("Failed to fetch diagnostics data:", err));
    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  return (
    <div className="space-y-6">
      {/* 0. Identidade da tela — paridade com mockup do Luiz */}
      <SectionIntro
        id="diagnostics-section-intro"
        overline="DIAGNÓSTICOS"
        question="O motor de diagnóstico está funcionando bem?"
        description="Volume, taxa de sucesso e motivos de falha das varreduras de conectividade disparadas pelo SDK."
        source="FONTE · FIREBASE ANALYTICS (DIAGNOSTIC_SESSIONS)"
      />

      {/* 1. KPIs — diagnosticsKpis do mockup (4 cards) */}
      <DiagnosticsMetricGrid environment={environment} summary={summary} />

      {/* 2. Composição fixa do mockup — diagnósticos executados · 14 dias
          (GH#783: granularidade diária real via /admin/metrics/timeline,
          mesmo endpoint/campo completedDiagnostics já usado em
          OverviewPage/DiagnosticsTimeline) + motivos de falha (real, a
          partir das issues das sessões carregadas). */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <ChartCard title="Diagnósticos executados · 14 dias" id="diagnostics-volume-timeline-card">
            {timelineData.length === 0 ? (
              <FeatureComingSoon
                feature="Diagnósticos executados · série temporal"
                reason="Sem diagnósticos registrados no período selecionado"
              />
            ) : (
              <LineChart
                data={timelineData}
                xAxisKey="date"
                series={[
                  { key: "completedDiagnostics", name: "Diagnósticos", color: "var(--sq-accent)" },
                ]}
                height={260}
              />
            )}
          </ChartCard>
        </div>
        <div className="lg:col-span-1">
          <FailureReasonsPanel sessions={sessions} />
        </div>
      </div>
    </div>
  );
};
