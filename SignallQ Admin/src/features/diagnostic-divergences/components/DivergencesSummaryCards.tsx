import React from "react";
import {
  DIVERGENCE_CLASSIFICATIONS,
  DiagnosticDivergencesSummary,
  DivergenceClassification,
} from "../../../types/diagnosticDivergences";
import { alpha } from "../../../utils/color";

interface DivergencesSummaryCardsProps {
  summary: DiagnosticDivergencesSummary;
  activeClassification: DivergenceClassification | null;
  onSelect: (classification: DivergenceClassification | null) => void;
  id?: string;
}

// Refs #1446 (shadow mode) — cor por classificação segue a semântica de severidade
// já usada no resto do Console: EXACT_MATCH/EQUIVALENT_RESULT = sucesso (motor remoto
// bate com o local), MINOR_DIVERGENCE = atenção, MAJOR_DIVERGENCE/REMOTE_ERROR/
// LOCAL_ERROR = erro (algo real diverge ou falhou, vale investigar).
const CLASSIFICATION_LABEL: Record<DivergenceClassification, string> = {
  EXACT_MATCH: "Match exato",
  EQUIVALENT_RESULT: "Resultado equivalente",
  MINOR_DIVERGENCE: "Divergência menor",
  MAJOR_DIVERGENCE: "Divergência maior",
  REMOTE_ERROR: "Erro remoto",
  LOCAL_ERROR: "Erro local",
};

const CLASSIFICATION_COLOR: Record<DivergenceClassification, string> = {
  EXACT_MATCH: "var(--success)",
  EQUIVALENT_RESULT: "var(--success)",
  MINOR_DIVERGENCE: "var(--attention)",
  MAJOR_DIVERGENCE: "var(--error)",
  REMOTE_ERROR: "var(--error)",
  LOCAL_ERROR: "var(--error)",
};

export const DivergencesSummaryCards: React.FC<DivergencesSummaryCardsProps> = ({
  summary,
  activeClassification,
  onSelect,
  id,
}) => {
  return (
    <div id={id} className="grid grid-cols-2 sm:grid-cols-3 xl:grid-cols-6 gap-3">
      {DIVERGENCE_CLASSIFICATIONS.map((classification) => {
        const count = summary[classification] ?? 0;
        const color = CLASSIFICATION_COLOR[classification];
        const active = activeClassification === classification;
        return (
          <button
            key={classification}
            type="button"
            onClick={() => onSelect(active ? null : classification)}
            className="text-left rounded-[var(--radius-card)] p-3.5 cursor-pointer transition-colors"
            style={{
              backgroundColor: active ? alpha(color, 10) : "var(--bg-surface)",
              border: `1px solid ${active ? color : "var(--border)"}`,
            }}
            aria-pressed={active}
          >
            <p className="text-[20px] font-semibold font-mono" style={{ color }}>
              {count}
            </p>
            <p className="text-[11px] mt-1 leading-tight" style={{ color: "var(--text-tertiary)" }}>
              {CLASSIFICATION_LABEL[classification]}
            </p>
          </button>
        );
      })}
    </div>
  );
};

export { CLASSIFICATION_LABEL, CLASSIFICATION_COLOR };
