import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { GooglePlayRatingSummary } from "../../../integrations/google-play/googlePlay.types";
import { FirebaseCrashlyticsSummary } from "../../../integrations/firebase/firebase.types";
import { crashFreeReason } from "../../../utils/crashlytics";

interface OverviewMetricGridProps {
  activeUsersToday: number | null;
  firebaseCrashlytics: FirebaseCrashlyticsSummary | null;
  aiCostMonthLabel: string | null;
  playStoreRating: GooglePlayRatingSummary | null;
}

// Paridade com o mockup do Luiz (signallq-admin-mockup.dc.html, sec-overview):
// Usuários Ativos, Crash-free rate, Custo de IA (mês), Nota na Play Store.

export const OverviewMetricGrid: React.FC<OverviewMetricGridProps> = ({
  activeUsersToday,
  firebaseCrashlytics,
  aiCostMonthLabel,
  playStoreRating,
}) => {
  const crashFreeAvailable = firebaseCrashlytics?.source === "bigquery";
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* 1. Usuários Ativos — real, Firebase Analytics (GA4) */}
      <MetricCard
        label="Usuários Ativos"
        value={activeUsersToday === null ? "Não disponível" : activeUsersToday}
        verdictNote={activeUsersToday === null ? "Firebase Analytics sem dado no período" : undefined}
        source="firebase"
        id="metric-active-users"
      />

      {/* 2. Crash-free rate — real via BigQuery export do Crashlytics quando
          source==="bigquery"; qualquer outro source é honesto-vazio, com o
          motivo exato do worker (sem credencial, sem volume ainda, ou erro). */}
      <MetricCard
        label="Crash-free Rate"
        value={crashFreeAvailable ? `${firebaseCrashlytics!.crashFreeUsersPercentage}%` : "Não disponível"}
        verdictNote={crashFreeAvailable ? undefined : crashFreeReason(firebaseCrashlytics?.source)}
        source={crashFreeAvailable ? "firebase (bigquery)" : "não disponível"}
        id="metric-crash-free"
      />

      {/* 3. Custo de IA (mês) — real, Cloudflare Workers AI, últimos 30 dias */}
      <MetricCard
        label="Custo de IA (mês)"
        value={aiCostMonthLabel ?? "Não disponível"}
        verdictNote={aiCostMonthLabel === null ? "Worker de IA sem dado no período" : undefined}
        source="signallq worker"
        id="metric-ai-cost-month"
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
