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
  // Format numeric values gracefully
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

  // Determine trend presentation
  const isTrendUp = trend?.type === "up";
  const isTrendDown = trend?.type === "down";
  
  const trendColorClass = isTrendUp
    ? "text-[#22C55E] bg-[#22C55E]/10 border-[#22C55E]/20"
    : isTrendDown
    ? "text-[#FF4D4F] bg-[#FF4D4F]/10 border-[#FF4D4F]/20"
    : "text-[#9CA3AF] bg-[#18181B] border-[#262626]";

  return (
    <div
      id={id || `metric-card-${label.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className={`relative overflow-hidden bg-[#111111] border border-[#262626] rounded-[18px] p-5 hover:border-[#363636] transition-all duration-200 group ${className}`}
    >
      {/* Background Accent Grid / Glow */}
      <div className="absolute top-0 right-0 w-24 h-24 bg-gradient-to-br from-indigo-500/5 to-transparent rounded-full filter blur-xl group-hover:from-indigo-400/15 transition-all duration-300 pointer-events-none" />

      {source && (
        <span className="absolute top-3 right-3 text-[9px] font-mono text-zinc-550 bg-zinc-950 px-1.5 py-0.5 rounded border border-zinc-900/60 select-none">
          {source}
        </span>
      )}

      <p className="text-[11px] font-mono uppercase tracking-wider text-neutral-400 select-none">
        {label}
      </p>

      <div className="mt-2.5 flex items-baseline justify-between">
        <h3 className="text-2xl font-semibold tracking-tight text-white font-sans">
          {formattedValue}
        </h3>

        {trend && (
          <span className={`inline-flex items-center gap-0.5 px-2 py-0.5 rounded-md text-xs font-mono border ${trendColorClass}`}>
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
        <span className="mt-2 block text-[10px] text-neutral-500 font-sans tracking-wide">
          {trend.intervalLabel}
        </span>
      )}
    </div>
  );
};
