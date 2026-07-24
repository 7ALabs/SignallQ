import React from "react";
import { integrationsService } from "../../../integrations/integrationsService";
import { GooglePlayReviewSummary } from "../../../integrations/google-play/googlePlay.types";
import { MetricCard } from "../../../components/ui/MetricCard";
import { RatingDistributionBars } from "../../../components/ui/RatingDistributionBars";
import { TermHint } from "../../../components/ui/TermHint";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";
import { ReviewCard } from "./ReviewCard";
import {
  computeReviewDisplayStatus,
  countRecentUnansweredLowRating,
  ratingDistribution,
  sortReviewsByDate,
  sortReviewsByRisk,
} from "../reviewStatus";

type FilterPill = "todas" | "pendentes" | "respondidas";
type SortMode = "risco" | "recentes";

const LIMIT_STEP = 50;
const LIMIT_MAX = 200;

interface ReviewsSectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1341 — categoria "Avaliações", item 2.3 do plano de UX: hero (nota média + pendentes),
 * distribuição de estrelas, filtro de pills e lista de `ReviewCard` ordenada por risco.
 */
export const ReviewsSection: React.FC<ReviewsSectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState(false);
  const [reviews, setReviews] = React.useState<GooglePlayReviewSummary[]>([]);
  const [limit, setLimit] = React.useState(LIMIT_STEP);
  const [loadingMore, setLoadingMore] = React.useState(false);
  const [filter, setFilter] = React.useState<FilterPill>("todas");
  const [sortMode, setSortMode] = React.useState<SortMode>("risco");

  React.useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(false);
    integrationsService
      .getGooglePlayReviewsList({ limit })
      .then((result) => {
        if (cancelled) return;
        setReviews(result);
      })
      .catch(() => {
        if (!cancelled) setError(true);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [triggerRefreshCounter]);

  const handleLoadMore = React.useCallback(async () => {
    const nextLimit = Math.min(limit + LIMIT_STEP, LIMIT_MAX);
    setLoadingMore(true);
    try {
      const result = await integrationsService.getGooglePlayReviewsList({ limit: nextLimit });
      setReviews(result);
      setLimit(nextLimit);
    } catch {
      setError(true);
    } finally {
      setLoadingMore(false);
    }
  }, [limit]);

  const pendingCount = React.useMemo(
    () => reviews.filter((r) => {
      const s = computeReviewDisplayStatus(r);
      return s === "pendente" || s === "atencao";
    }).length,
    [reviews]
  );
  const repliedCount = React.useMemo(
    () => reviews.filter((r) => computeReviewDisplayStatus(r) === "respondida").length,
    [reviews]
  );

  const filteredReviews = React.useMemo(() => {
    if (filter === "todas") return reviews;
    return reviews.filter((r) => {
      const s = computeReviewDisplayStatus(r);
      if (filter === "pendentes") return s === "pendente" || s === "atencao";
      return s === "respondida";
    });
  }, [reviews, filter]);

  const sortedReviews = React.useMemo(
    () => (sortMode === "risco" ? sortReviewsByRisk(filteredReviews) : sortReviewsByDate(filteredReviews)),
    [filteredReviews, sortMode]
  );

  if (loading) {
    return <LoadingState message="Buscando avaliações no Android Publisher API..." />;
  }

  if (error) {
    return (
      <EmptyState
        id="google-play-reviews-error"
        title="Não foi possível carregar as avaliações"
        description="Falha ao consultar o Admin Worker — tente novamente em instantes ou verifique as credenciais do Google Play."
      />
    );
  }

  if (reviews.length === 0) {
    return (
      <EmptyState
        id="google-play-reviews-empty"
        title="Nenhuma avaliação sincronizada ainda"
        description="Rode a sincronização do Google Play (via /admin/integrations/google-play/sync) para importar as avaliações."
      />
    );
  }

  const averageRating = reviews.reduce((sum, r) => sum + r.rating, 0) / reviews.length;
  const riskyCount = countRecentUnansweredLowRating(reviews);
  const distribution = ratingDistribution(reviews);

  return (
    <div className="space-y-5">
      {/* 1. Hero — nota média + pendentes de risco lado a lado (item 2.3.1.1) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <MetricCard
          id="google-play-reviews-average-hero"
          size="hero"
          label="Nota média"
          value={averageRating.toFixed(1)}
          verdictNote={`Amostra de ${reviews.length} avaliações sincronizadas`}
          source="google play"
        />
        <MetricCard
          id="google-play-reviews-risk-hero"
          size="hero"
          label="Pendentes de risco"
          value={riskyCount}
          verdict={riskyCount === 0 ? "excelente" : riskyCount <= 3 ? "regular" : "fraco"}
          verdictNote="Nota ≤ 2 sem resposta do dev, últimos 30 dias"
        />
      </div>

      {/* 2. Distribuição de estrelas (item 2.3.1.2) */}
      <div
        className="rounded-[var(--radius-card)] p-4"
        style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      >
        <RatingDistributionBars distribution={distribution} total={reviews.length} />
      </div>

      {/* 3. Filtro de pills + toggle de ordenação (item 2.3.3) */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div
          className="inline-flex items-center p-1 rounded-[var(--radius-button)] gap-0.5"
          style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
        >
          {([
            { key: "todas", label: `Todas (${reviews.length})` },
            { key: "pendentes", label: `Pendentes (${pendingCount})` },
            { key: "respondidas", label: `Respondidas (${repliedCount})` },
          ] as { key: FilterPill; label: string }[]).map((pill) => (
            <button
              key={pill.key}
              type="button"
              onClick={() => setFilter(pill.key)}
              className="px-3 py-1.5 text-[11px] font-sans font-medium rounded-lg transition-colors cursor-pointer"
              style={
                filter === pill.key
                  ? { backgroundColor: "var(--sq-control-active)", color: "var(--text-primary)" }
                  : { color: "var(--text-secondary)" }
              }
            >
              {pill.label}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-2 text-[11px]" style={{ color: "var(--text-tertiary)" }}>
          <span className="flex items-center">
            Status de tratamento
            <TermHint term="handlingStatus" />
          </span>
          <span>·</span>
          <button
            type="button"
            onClick={() => setSortMode(sortMode === "risco" ? "recentes" : "risco")}
            className="underline cursor-pointer"
          >
            {sortMode === "risco" ? "Ordenar por mais recentes" : "Ordenar por risco"}
          </button>
        </div>
      </div>

      {/* 4. Lista de reviews (item 2.3.1.3/2.3.2) */}
      {sortedReviews.length === 0 ? (
        <p className="text-xs py-4" style={{ color: "var(--text-tertiary)" }}>
          {filter === "pendentes" ? "Nenhuma review pendente de resposta." : "Nenhuma review nesse filtro."}
        </p>
      ) : (
        <div className="space-y-3">
          {sortedReviews.map((review) => (
            <ReviewCard key={review.reviewId} review={review} />
          ))}
        </div>
      )}

      {/* 5. Carregar mais — item 2.3.4, scroll incremental */}
      {limit < LIMIT_MAX && reviews.length >= limit && (
        <div className="flex justify-center pt-1">
          <button
            type="button"
            onClick={handleLoadMore}
            disabled={loadingMore}
            className="px-4 py-2 text-xs font-semibold rounded-[var(--radius-button)] transition-colors cursor-pointer disabled:opacity-50"
            style={{ border: "1px solid var(--border)", color: "var(--text-secondary)" }}
          >
            {loadingMore ? "Carregando..." : "Carregar mais"}
          </button>
        </div>
      )}
    </div>
  );
};
