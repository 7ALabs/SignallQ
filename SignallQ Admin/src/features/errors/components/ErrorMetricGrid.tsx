import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { FirebaseCrashlyticsSummary } from "../../../integrations/firebase/firebase.types";
import { crashFreeReason } from "../../../utils/crashlytics";

interface ErrorMetricGridProps {
  firebaseCrashlytics: FirebaseCrashlyticsSummary | null;
}

// Paridade com o mockup do Luiz (sec-errors, errorKpis): Crash-free users,
// Taxa de ANR, Crashes não resolvidos, MTTR médio — todos com fonte
// Crashlytics/Play Console. Crash-free users e Crashes não resolvidos vêm do
// Crashlytics real (GH#872 ligou o worker, este card ficou de fora até agora
// — mesmo padrão do OverviewMetricGrid, função crashFreeReason compartilhada
// via src/utils/crashlytics.ts). Taxa de ANR e MTTR seguem "Não disponível":
// Play Vitals e cálculo de MTTR não têm fonte real ainda.
export const ErrorMetricGrid: React.FC<ErrorMetricGridProps> = ({ firebaseCrashlytics }) => {
  const crashDataAvailable = firebaseCrashlytics?.source === "bigquery";

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <MetricCard
        label="Crash-free users"
        value={crashDataAvailable ? `${firebaseCrashlytics!.crashFreeUsersPercentage}%` : "Não disponível"}
        verdictNote={crashDataAvailable ? undefined : crashFreeReason(firebaseCrashlytics?.source)}
        source={crashDataAvailable ? "firebase (bigquery)" : "não disponível"}
        id="error-metric-crash-free-users"
      />
      <MetricCard
        label="Taxa de ANR"
        value="Não disponível"
        verdictNote="Google Play Console (Android Vitals) ainda não integrado ao painel"
        source="não implementado"
        id="error-metric-anr-rate"
      />
      <MetricCard
        label="Crashes não resolvidos"
        value={crashDataAvailable ? firebaseCrashlytics!.unresolvedCrashes : "Não disponível"}
        verdictNote={crashDataAvailable ? undefined : crashFreeReason(firebaseCrashlytics?.source)}
        source={crashDataAvailable ? "firebase (bigquery)" : "não disponível"}
        id="error-metric-crashes-today"
      />
      <MetricCard
        label="MTTR médio"
        value="Não disponível"
        verdictNote="Tempo médio de resolução ainda não é calculado pelo worker"
        source="não implementado"
        id="error-metric-mttr"
      />
    </div>
  );
};
