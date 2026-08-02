import React from "react";
import { Star } from "lucide-react";
import { GooglePlayReviewSummary } from "../../../integrations/google-play/googlePlay.types";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { formatRelativeDate } from "../../../utils/format";
import { computeReviewDisplayStatus } from "../reviewStatus";

interface ReviewCardProps {
  review: GooglePlayReviewSummary;
  id?: string;
}

const COMMENT_COLLAPSE_LENGTH = 320;

/**
 * GH#1341 — item 2.3.2 do plano de UX: card compacto, não `DataTable` — texto de comentário
 * varia de 1 palavra a parágrafos, não cabe em coluna sem truncar de um jeito que destrua a
 * leitura.
 */
export const ReviewCard: React.FC<ReviewCardProps> = ({ review, id }) => {
  const status = computeReviewDisplayStatus(review);
  const [expanded, setExpanded] = React.useState(false);
  const comment = review.comment?.trim() || "(sem comentário)";
  const isLong = comment.length > COMMENT_COLLAPSE_LENGTH;
  const displayedComment = isLong && !expanded ? `${comment.slice(0, COMMENT_COLLAPSE_LENGTH)}…` : comment;

  const metadata = [
    review.language,
    review.device,
    review.appVersion ? `v${review.appVersion}` : undefined,
    formatRelativeDate(review.commentTime),
  ].filter(Boolean);

  return (
    <div
      id={id}
      className="rounded-[var(--radius-card)] p-4"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
    >
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2 min-w-0">
          <span className="flex items-center gap-0.5 shrink-0" aria-label={`Nota ${review.rating} de 5`}>
            {Array.from({ length: 5 }).map((_, i) => (
              <Star
                key={i}
                className="w-3.5 h-3.5"
                style={{ color: i < review.rating ? "var(--attention)" : "var(--border)" }}
                fill={i < review.rating ? "currentColor" : "none"}
              />
            ))}
          </span>
          <span className="text-xs font-sans font-medium truncate" style={{ color: "var(--text-primary)" }}>
            {review.userName || "Usuário anônimo"}
          </span>
        </div>

        {status !== "nenhuma" && (
          <StatusBadge
            status={status === "pendente" ? "critical" : status === "atencao" ? "attention" : "ok"}
            customLabel={status === "pendente" ? "Pendente" : status === "atencao" ? "Atenção" : "Respondida"}
          />
        )}
      </div>

      <p className="mt-2.5 text-[13px] leading-relaxed font-sans whitespace-pre-line" style={{ color: "var(--text-secondary)" }}>
        {displayedComment}
      </p>
      {isLong && (
        <button
          type="button"
          onClick={() => setExpanded((v) => !v)}
          className="mt-1 text-[11px] font-sans font-medium cursor-pointer"
          style={{ color: "var(--primary)" }}
        >
          {expanded ? "ver menos" : "ver mais"}
        </button>
      )}

      {metadata.length > 0 && (
        <div
          className="mt-2.5 flex flex-wrap items-center gap-x-1.5 text-[10px] font-mono uppercase tracking-wide"
          style={{ color: "var(--text-tertiary)" }}
        >
          {metadata.map((item, idx) => (
            <React.Fragment key={idx}>
              {idx > 0 && <span>·</span>}
              <span>{item}</span>
            </React.Fragment>
          ))}
        </div>
      )}

      {review.replyText && (
        <div className="mt-3 pl-3 py-1.5" style={{ borderLeft: "2px solid var(--border)" }}>
          <p className="text-[12px] leading-relaxed font-sans" style={{ color: "var(--text-secondary)" }}>
            {review.replyText}
          </p>
          {review.replyTime && (
            <span className="mt-1 block text-[10px] font-mono" style={{ color: "var(--text-tertiary)" }}>
              {formatRelativeDate(review.replyTime)}
            </span>
          )}
        </div>
      )}
    </div>
  );
};
