import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { GooglePlayRatingSummary } from "../../../integrations/google-play/googlePlay.types";
import { FirebaseCrashlyticsSummary } from "../../../integrations/firebase/firebase.types";
import { crashFreeReason } from "../../../utils/crashlytics";

interface OverviewMetricGridProps {
  activeUsersToday: number | null;
  sessions7d: number | null;
  firebaseCrashlytics: FirebaseCrashlyticsSummary | null;
  playStoreRating: GooglePlayRatingSummary | null;
}

// Seção "App" do Centro de Controle (spec Lia, Md3DashboardContent.dc.html:25-30):
// Usuários Ativos, Sessões (7d), Crash-free Rate, Nota na Play Store. Custo de
// IA saiu deste grid — agora é card isolado full-width (AiCostSummaryCard).

export const OverviewMetricGrid: React.FC<OverviewMetricGridProps> = ({
  activeUsersToday,
  sessions7d,
  firebaseCrashlytics,
  playStoreRating,
}) => {
  const crashFreeAvailable = firebaseCrashlytics?.source === "bigquery";
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Usuários Ativos — real, Firebase Analytics (GA4) */}
      <MetricCard
        label="Usuários Ativos"
        value={activeUsersToday === null ? "Não disponível" : activeUsersToday}
        verdictNote={activeUsersToday === null ? "Firebase Analytics sem dado no período" : undefined}
        source="firebase"
        id="metric-active-users"
      />

      {/* 2. Sessões (7d) — real, soma de sessions do GA4 runReport (mesma
          janela 7daysAgo-today já consultada para Usuários Ativos). Sem
          variação vs. período anterior: o worker não consulta uma segunda
          janela GA4 hoje, então não há "+18%" real pra mostrar (protótipo
          tem esse veredito, mas é mock estático — não inventar aqui). */}
      <MetricCard
        label="Sessões (7d)"
        value={sessions7d === null ? "Não disponível" : sessions7d}
        verdictNote={sessions7d === null ? "Firebase Analytics sem dado no período" : undefined}
        source="firebase"
        id="metric-sessions-7d"
      />

      {/* 3. Crash-free rate — real via BigQuery export do Crashlytics quando
          source==="bigquery"; qualquer outro source é honesto-vazio, com o
          motivo exato do worker (sem credencial, sem volume ainda, ou erro). */}
      <MetricCard
        label="Crash-free Rate"
        value={crashFreeAvailable ? `${firebaseCrashlytics!.crashFreeUsersPercentage}%` : "Não disponível"}
        verdictNote={crashFreeAvailable ? undefined : crashFreeReason(firebaseCrashlytics?.source)}
        source={crashFreeAvailable ? "firebase (bigquery)" : "não disponível"}
        id="metric-crash-free"
      />

      {/* 4. Nota na Play Store — integração real via Android Publisher API
          (getGooglePlayRatings, GH#871). null hoje é porque o app ainda não
          foi publicado na Play Store (M3), não porque falta implementação. */}
      <MetricCard
        label="Nota na Play Store"
        value={playStoreRating === null ? "Não disponível" : `${playStoreRating.averageRating.toFixed(1)} ★`}
        verdictNote={playStoreRating === null ? "Sem avaliações suficientes na Play Store ainda" : `${playStoreRating.totalRatings.toLocaleString("pt-BR")} avaliações`}
        source="google play"
        id="metric-play-rating"
      />
    </div>
  );
};
