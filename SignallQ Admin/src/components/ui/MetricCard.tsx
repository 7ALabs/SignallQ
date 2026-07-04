import React from "react";
import { ArrowUpRight, ArrowDownRight, Minus } from "lucide-react";
import { MetricTrend } from "../../types/metrics";
import { alpha } from "../../utils/color";

interface MetricCardProps {
  label: string;
  value: string | number;
  trend?: MetricTrend;
  format?: "number" | "percentage" | "ms" | "mbps" | "usd";
  className?: string;
  id?: string;
  source?: string;
}

export const MetricCard: React.FC<MetricCardProps> = ({
  label,
  value,
  trend,
  format,
  className = "",
  id,
  source,
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

  const trendStyle = isTrendUp
    ? {
        color: "var(--success)",
        backgroundColor: alpha("var(--success)", 10),
        border: `1px solid ${alpha("var(--success)", 20)}`,
      }
    : isTrendDown
    ? {
        color: "var(--error)",
        backgroundColor: alpha("var(--error)", 10),
        border: `1px solid ${alpha("var(--error)", 20)}`,
      }
    : {
        color: "var(--text-secondary)",
        backgroundColor: "var(--bg-surface-hover)",
        border: "1px solid var(--border)",
      };

  return (
    <div
      id={id || `metric-card-${label.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className={`relative overflow-hidden rounded-[8px] p-5 transition-all duration-200 group ${className}`}
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
        className="text-[11px] font-sans uppercase tracking-[0.08em] font-semibold select-none"
        style={{ color: "var(--text-secondary)" }}
      >
        {label}
      </p>

      <div className="mt-2.5 flex items-baseline justify-between">
        <h3 className="text-2xl lg:text-[28px] font-bold tracking-[-0.03em]" style={{ color: "var(--text-primary)" }}>
          {formattedValue}
        </h3>

        {trend && (
          <span
            className="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-[11px] font-sans font-medium"
            style={trendStyle}
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

      {trend && (
        <span
          className="mt-2 block text-[10px] tracking-wide"
          style={{ color: "var(--text-tertiary)" }}
        >
          {trend.intervalLabel}
        </span>
      )}
    </div>
  );
};
