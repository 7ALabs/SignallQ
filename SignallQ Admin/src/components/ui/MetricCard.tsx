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
  /**
   * GH#1341 — item 2.1.1 do plano de UX Google Play/Firebase: métrica-âncora de categoria,
   * sempre a primeira coisa renderizada, maior que um `MetricCard` comum (valor 48–56px) e com
   * borda tingida pelo tom do veredito. Usar no máximo uma por categoria/seção.
   */
  size?: "default" | "hero";
  /** Conteúdo extra ao lado do label — usado pelo `TermHint` (item 2.2.2 do plano de UX). */
  labelExtra?: React.ReactNode;
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
  size = "default",
  labelExtra,
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

  const isHero = size === "hero";
  const heroBorderColor = isHero && verdictColor ? alpha(verdictColor, 40) : "var(--border)";

  return (
    <div
      id={id || `metric-card-${label.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className={`relative overflow-hidden rounded-[var(--radius-card)] sq-card-hover group ${isHero ? "p-6" : "p-5"} ${className}`}
      style={{
        backgroundColor: "var(--bg-surface)",
        border: `1px solid ${heroBorderColor}`,
        background: isHero && verdictColor
          ? `linear-gradient(135deg, ${alpha(verdictColor, 6)}, var(--bg-surface) 60%)`
          : undefined,
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

      {/* GH#1081: label + badge de fonte num flex justify-between em vez do badge em
          position:absolute -- antes o badge não reservava espaço horizontal e o
          truncate do label media contra a largura total do card, cortando o texto
          bruscamente (sem reticências) bem onde o badge começava. */}
      <div className="flex items-start justify-between gap-2">
        <p
          className={`min-w-0 flex-1 font-sans uppercase tracking-[0.08em] font-semibold select-none truncate flex items-center ${isHero ? "text-xs" : "text-[11px]"}`}
          style={{ color: "var(--text-secondary)" }}
        >
          {label}
          {labelExtra}
        </p>
        {source && (
          <span
            className="shrink-0 text-[9px] font-mono px-1.5 py-0.5 rounded select-none"
            style={{
              color: "var(--text-tertiary)",
              backgroundColor: "var(--bg-base)",
              border: `1px solid ${alpha("var(--border)", 60)}`,
            }}
          >
            {source}
          </span>
        )}
      </div>

      {/* Valor + veredito inline na mesma linha de base — spec KpiCard.dc.html
          ("486 Mbps · Excelente"), não em bloco separado abaixo. */}
      <div className="mt-2.5 flex items-baseline gap-2 flex-wrap">
        <h3
          className={isHero ? "text-[40px] lg:text-[56px] font-bold tracking-[-0.03em]" : "text-2xl lg:text-[28px] font-bold tracking-[-0.03em]"}
          style={{ color: "var(--text-primary)" }}
        >
          {formattedValue}
        </h3>
        {verdict && (
          <span
            className={isHero ? "text-sm font-sans font-semibold" : "text-xs font-sans font-semibold"}
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
