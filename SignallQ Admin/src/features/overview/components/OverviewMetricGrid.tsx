import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { GooglePlayRatingSummary } from "../../../integrations/google-play/googlePlay.types";

interface OverviewMetricGridProps {
  activeUsersToday: number | null;
  aiCostMonthLabel: string | null;
  playStoreRating: GooglePlayRatingSummary | null;
}

// Paridade com o mockup do Luiz (signallq-admin-mockup.dc.html, sec-overview):
// Usuários Ativos, Crash-free rate, Custo de IA (mês), Nota na Play Store.
// Crash-free rate e Nota na Play Store não têm integração real ainda
// (getGooglePlayRatings/crashAnr são mock-only hoje) — "Não disponível" é o
// estado honesto: mostrar um número fabricado seria pior que não mostrar nada.
export const OverviewMetricGrid: React.FC<OverviewMetricGridProps> = ({
  activeUsersToday,
  aiCostMonthLabel,
  playStoreRating,
}) => {
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

      {/* 2. Crash-free rate — sem integração real ainda (Crashlytics não expõe
          percentual, só contagens de crash) */}
      <MetricCard
        label="Crash-free Rate"
        value="Não disponível"
        verdictNote="Crashlytics ainda não expõe percentual — só contagem de crashes"
        source="não implementado"
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

      {/* 4. Nota na Play Store — sem integração real ainda (Android Publisher
          API de reviews/rating não implementada, só instalações/tracks) */}
      <MetricCard
        label="Nota na Play Store"
        value={playStoreRating === null ? "Não disponível" : `${playStoreRating.averageRating.toFixed(1)} ★`}
        verdictNote={playStoreRating === null ? "Google Play Ratings API não implementada ainda" : `${playStoreRating.totalRatings.toLocaleString("pt-BR")} avaliações`}
        source={playStoreRating === null ? "não implementado" : "google play"}
        id="metric-play-rating"
      />
    </div>
  );
};
