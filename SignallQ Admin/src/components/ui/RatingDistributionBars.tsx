import React from "react";
import { Star } from "lucide-react";

interface RatingDistributionBarsProps {
  /** Contagem por nota (1 a 5). */
  distribution: Record<1 | 2 | 3 | 4 | 5, number>;
  total: number;
  id?: string;
}

/**
 * GH#1341 — item 2.3.1.2 do plano de UX: barra horizontal empilhada (5→1), não `DonutChart` —
 * ordena visualmente "onde está a massa" sem precisar de legenda de cor separada.
 */
export const RatingDistributionBars: React.FC<RatingDistributionBarsProps> = ({
  distribution,
  total,
  id,
}) => {
  return (
    <div id={id} className="flex flex-col gap-2">
      {([5, 4, 3, 2, 1] as const).map((star) => {
        const count = distribution[star] ?? 0;
        const pct = total > 0 ? Math.round((count / total) * 100) : 0;
        return (
          <div key={star} className="flex items-center gap-2.5">
            <span
              className="flex items-center gap-0.5 w-6 shrink-0 text-[11px] font-sans font-medium"
              style={{ color: "var(--text-secondary)" }}
            >
              <Star className="w-3 h-3" style={{ color: "var(--attention)" }} fill="currentColor" />
              {star}
            </span>
            <div
              className="flex-1 h-1.5 rounded-full overflow-hidden"
              style={{ backgroundColor: "var(--bg-base)" }}
            >
              <div
                className="h-full rounded-full transition-all duration-300"
                style={{ width: `${pct}%`, backgroundColor: "var(--attention)" }}
              />
            </div>
            <span
              className="w-20 shrink-0 text-right text-[10px] font-mono"
              style={{ color: "var(--text-tertiary)" }}
            >
              {count} · {pct}%
            </span>
          </div>
        );
      })}
    </div>
  );
};
