import React from "react";
import { ArrowUpRight, ArrowDownRight, Minus } from "lucide-react";
import { MetricTrend, MetricVerdict, METRIC_VERDICT_TONE } from "../../types/metrics";
import { alpha } from "../../utils/color";

const VERDICT_LABEL: Record<MetricVerdict, string> = {
  excelente: "Excelente",
  bom: "Bom",
  regular: "Regular",
  fraco: "Fraco",
  forte: "Forte",
};

interface MetricCardProps {
  label: string;
  value: string | number;
  trend?: MetricTrend;
  format?: "number" | "percentage" | "ms" | "mbps" | "usd";
  className?: string;
  id?: string;
  source?: string;
  /**
   * GH#552: veredito humano obrigatório para KPIs de qualidade/status (ex.: crash
   * rate, retenção, latência) — nunca deixar a métrica crua sozinha. Omitir em
   * KPIs puramente de volume (ex.: contagem de diagnósticos), onde `trend` já
   * comunica direção.
   */
  verdict?: MetricVerdict;
  /** Contexto curto do veredito — ex.: "abaixo do limiar de mercado (1%)". */
  verdictNote?: string;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  trend,
  format,
  className = "",
  id,
  source,
  verdict,
  verdictNote,
}) => {
  const formattedValue = React.useMemo(() => {
    if (typeof value === "number") {
      if (format === "usd") {
        return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
      }
      if (format === "percentage") {
        return `${value}%`;
      }
      return value.toLocaleString("pt-BR");
    }
    return value;
  }, [value, format]);

  const isTrendUp = trend?.type === "up";
  const isTrendDown = trend?.type === "down";

  const verdictColor = verdict
    ? METRIC_VERDICT_TONE[verdict] === "positive"
      ? "var(--success)"
      : METRIC_VERDICT_TONE[verdict] === "negative"
      ? "var(--error)"
      : "var(--attention)"
    : undefined;

  return (
    <div
      id={id || `metric-card-${label.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className={`relative overflow-hidden rounded-[var(--radius-card)] p-5 sq-card-hover group ${className}`}
      style={{
        backgroundColor: "var(--bg-surface)",
        border: "1px solid var(--border)",
      }}
    >
      {/* Accent glow — leve, no canto superior direito */}
      <div
        className="absolute top-0 right-0 w-24 h-24 rounded-full pointer-events-none transition-all duration-300"
        style={{
          background: `radial-gradient(circle, ${alpha("var(--primary)", 6)}, transparent)`,
          filter: "blur(20px)",
        }}
      />

      {source && (
        <span
          className="absolute top-3 right-3 text-[9px] font-mono px-1.5 py-0.5 rounded select-none"
          style={{
            color: "var(--text-tertiary)",
            backgroundColor: "var(--bg-base)",
            border: `1px solid ${alpha("var(--border)", 60)}`,
          }}
        >
          {source}
        </span>
      )}

      <p
        className="text-[11px] font-sans uppercase tracking-[0.08em] font-semibold select-none truncate"
        style={{ color: "var(--text-secondary)" }}
      >
        {label}
      </p>

      {/* Valor + veredito inline na mesma linha de base — spec KpiCard.dc.html
          ("486 Mbps · Excelente"), não em bloco separado abaixo. */}
      <div className="mt-2.5 flex items-baseline gap-2 flex-wrap">
        <h3 className="text-2xl lg:text-[28px] font-bold tracking-[-0.03em]" style={{ color: "var(--text-primary)" }}>
          {formattedValue}
        </h3>
        {verdict && (
          <span
            className="text-xs font-sans font-semibold"
            style={{ color: verdictColor }}
          >
            <span aria-hidden="true">&middot; </span>
            {VERDICT_LABEL[verdict]}
          </span>
        )}
      </div>

      {/* Linha inferior: sub-texto à esquerda, tendência com seta à direita —
          mesma composição do KpiCard do mockup. */}
      {(verdictNote || trend) && (
        <div className="mt-2.5 flex items-center justify-between gap-2 min-h-[16px]">
          <span
            className="text-[10px] leading-tight truncate"
            style={{ color: "var(--text-tertiary)" }}
          >
            {verdictNote}
          </span>
          {trend && (
            <span
              className="inline-flex items-center gap-0.5 shrink-0 text-[11px] font-sans font-semibold"
              style={{ color: isTrendUp ? "var(--success)" : isTrendDown ? "var(--error)" : "var(--text-secondary)" }}
            >
              {isTrendUp ? (
                <ArrowUpRight className="w-3.5 h-3.5" />
              ) : isTrendDown ? (
                <ArrowDownRight className="w-3.5 h-3.5" />
              ) : (
                <Minus className="w-3.5 h-3.5" />
              )}
              <span>{trend.value}%</span>
            </span>
          )}
        </div>
      )}

      {trend && (
        <span
          className="mt-1 block text-[10px] tracking-wide"
          style={{ color: "var(--text-tertiary)" }}
        >
          {trend.intervalLabel}
        </span>
      )}
    </div>
  );
};
