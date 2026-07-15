import React from "react";
import { ErrorMetricGrid } from "./components/ErrorMetricGrid";
import { ErrorByEndpointChart } from "./components/ErrorByEndpointChart";
import { TopCrashesCard } from "./components/TopCrashesCard";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { ChartCard } from "../../components/ui/ChartCard";
import { AppEnvironment } from "../../types/admin";
import { integrationsService } from "../../integrations/integrationsService";
import { FirebaseCrashlyticsSummary } from "../../integrations/firebase/firebase.types";

interface ErrorsPageProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

// Composição paridade mockup do Luiz (sec-errors): overline/H1/parágrafo/FONTE
// (SectionIntro) → grid de KPIs → linha 2fr/1fr (taxa de erro · erros por
// tela) → card full-width TOP CRASHES. Sem busca livre, filtros de categoria,
// alertas de infraestrutura, bloco de insight ou ações de rodapé — nenhum
// desses blocos existe na seção sec-errors do mockup.
export const ErrorsPage: React.FC<ErrorsPageProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [firebaseCrashlytics, setFirebaseCrashlytics] = React.useState<FirebaseCrashlyticsSummary | null>(null);

  React.useEffect(() => {
    let active = true;
    integrationsService
      .getFirebaseCrashlytics({ environment, period })
      .then((result) => {
        if (active) setFirebaseCrashlytics(result);
      })
      .catch(() => {
        if (active) setFirebaseCrashlytics(null);
      });
    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  return (
    <div className="space-y-6">
      <SectionIntro
        id="errors-section-intro"
        overline="PROBLEMAS & INCIDENTES"
        question="O app está falhando em algum lugar?"
        description="Crashes, ANRs e erros — priorizados por impacto em usuários, não por volume bruto."
        source="FONTE · FIREBASE CRASHLYTICS"
      />

      <ErrorMetricGrid firebaseCrashlytics={firebaseCrashlytics} />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in">
        <div className="lg:col-span-2">
          <ChartCard title="Taxa de erro · 14 dias" id="error-rate-timeline-card">
            <FeatureComingSoon
              feature="Taxa de erro · série temporal"
              reason="Métrica ainda não disponível — aguardando exposição no worker (sem série temporal de erros hoje)"
            />
          </ChartCard>
        </div>
        <div className="lg:col-span-1">
          <ErrorByEndpointChart />
        </div>
      </div>

      <TopCrashesCard environment={environment} period={period} triggerRefreshCounter={triggerRefreshCounter} />
    </div>
  );
};
