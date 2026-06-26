import React from "react";
import { ArrowUpRight, ArrowDownRight, Minus } from "lucide-react";
import { MetricTrend } from "../../types/metrics";

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
        return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD", minimumFractionDigits: 4 }).format(value);
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
        color: "var(--sq-success)",
        backgroundColor: "color-mix(in srgb, var(--sq-success) 10%, transparent)",
        border: "1px solid color-mix(in srgb, var(--sq-success) 20%, transparent)",
      }
    : isTrendDown
    ? {
        color: "var(--sq-error)",
        backgroundColor: "color-mix(in srgb, var(--sq-error) 10%, transparent)",
        border: "1px solid color-mix(in srgb, var(--sq-error) 20%, transparent)",
      }
    : {
        color: "var(--sq-text-secondary)",
        backgroundColor: "var(--sq-bg-overlay)",
        border: "1px solid var(--sq-border)",
      };

  return (
    <div
      id={id || `metric-card-${label.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className={`relative overflow-hidden rounded-2xl p-5 transition-all duration-200 group ${className}`}
      style={{
        backgroundColor: "var(--sq-bg-card)",
        border: "1px solid var(--sq-border)",
      }}
    >
      {/* Accent glow — leve, no canto superior direito */}
      <div
        className="absolute top-0 right-0 w-24 h-24 rounded-full pointer-events-none transition-all duration-300"
        style={{
          background: "radial-gradient(circle, color-mix(in srgb, var(--sq-accent) 6%, transparent), transparent)",
          filter: "blur(20px)",
        }}
      />

      {source && (
        <span
          className="absolute top-3 right-3 text-[9px] font-mono px-1.5 py-0.5 rounded select-none"
          style={{
            color: "var(--sq-text-tertiary)",
            backgroundColor: "var(--sq-bg-primary)",
            border: "1px solid var(--sq-border-subtle)",
          }}
        >
          {source}
        </span>
      )}

      <p
        className="text-[11px] font-mono uppercase tracking-wider select-none"
        style={{ color: "var(--sq-text-secondary)" }}
      >
        {label}
      </p>

      <div className="mt-2.5 flex items-baseline justify-between">
        <h3 className="text-2xl font-semibold tracking-tight" style={{ color: "var(--sq-text-primary)" }}>
          {formattedValue}
        </h3>

        {trend && (
          <span
            className="inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-xs font-mono"
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
          style={{ color: "var(--sq-text-tertiary)" }}
        >
          {trend.intervalLabel}
        </span>
      )}
    </div>
  );
};
