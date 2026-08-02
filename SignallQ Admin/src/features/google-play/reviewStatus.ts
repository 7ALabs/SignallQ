import { GooglePlayReviewSummary } from "../../integrations/google-play/googlePlay.types";

/**
 * GH#1341 — item 2.3.2 do plano de UX: status de tratamento calculado a partir de nota +
 * resposta do dev, não do `handlingStatus` cru do D1 (esse é campo admin-side, marcado
 * manualmente no Console — ver `googlePlay.types.ts`). "nenhuma" = nota alta sem resposta,
 * caso comum que não deve ganhar badge.
 */
export type DisplayReviewStatus = "pendente" | "atencao" | "respondida" | "nenhuma";

export function computeReviewDisplayStatus(review: GooglePlayReviewSummary): DisplayReviewStatus {
  if (review.replyText) return "respondida";
  if (review.rating <= 2) return "pendente";
  if (review.rating === 3) return "atencao";
  return "nenhuma";
}

/** Item 2.3.1.4 — risco primeiro (pendente > atenção > respondida/nenhuma), depois data desc. */
export function reviewRiskRank(status: DisplayReviewStatus): number {
  switch (status) {
    case "pendente":
      return 0;
    case "atencao":
      return 1;
    default:
      return 2;
  }
}

export function sortReviewsByRisk(reviews: GooglePlayReviewSummary[]): GooglePlayReviewSummary[] {
  return [...reviews].sort((a, b) => {
    const rankDiff = reviewRiskRank(computeReviewDisplayStatus(a)) - reviewRiskRank(computeReviewDisplayStatus(b));
    if (rankDiff !== 0) return rankDiff;
    return new Date(b.commentTime).getTime() - new Date(a.commentTime).getTime();
  });
}

export function sortReviewsByDate(reviews: GooglePlayReviewSummary[]): GooglePlayReviewSummary[] {
  return [...reviews].sort((a, b) => new Date(b.commentTime).getTime() - new Date(a.commentTime).getTime());
}

const THIRTY_DAYS_MS = 30 * 24 * 60 * 60 * 1000;

/** Item 2.3.1.1 — contagem de pendentes (nota ≤ 2, sem resposta) nos últimos 30 dias. */
export function countRecentUnansweredLowRating(reviews: GooglePlayReviewSummary[]): number {
  const now = Date.now();
  return reviews.filter((r) => {
    if (r.rating > 2 || r.replyText) return false;
    const commentDate = new Date(r.commentTime).getTime();
    if (Number.isNaN(commentDate)) return false;
    return now - commentDate <= THIRTY_DAYS_MS;
  }).length;
}

export function ratingDistribution(reviews: GooglePlayReviewSummary[]): Record<1 | 2 | 3 | 4 | 5, number> {
  const dist: Record<1 | 2 | 3 | 4 | 5, number> = { 1: 0, 2: 0, 3: 0, 4: 0, 5: 0 };
  reviews.forEach((r) => {
    if (r.rating >= 1 && r.rating <= 5) {
      dist[r.rating as 1 | 2 | 3 | 4 | 5]++;
    }
  });
  return dist;
}
