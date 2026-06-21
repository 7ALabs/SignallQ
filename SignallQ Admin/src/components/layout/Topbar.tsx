import React from "react";
import { RefreshCw, Download, Database, HelpCircle } from "lucide-react";
import { AppEnvironment } from "../../types/admin";
import { PERIOD_FILTERS } from "../../config/constants";

interface TopbarProps {
  title: string;
  environment: AppEnvironment;
  onEnvironmentChange: (env: AppEnvironment) => void;
  period: string;
  onPeriodChange: (period: string) => void;
  onRefresh?: () => void;
  onExport?: () => void;
  isRefreshing?: boolean;
  id?: string;
}

export const Topbar: React.FC<TopbarProps> = ({
  title,
  environment,
  onEnvironmentChange,
  period,
  onPeriodChange,
  onRefresh,
  onExport,
  isRefreshing = false,
  id,
}) => {
  return (
    <header
      id={id || "topbar-header"}
      className="h-16 border-b border-[#262626] bg-[#08080A] px-8 flex items-center justify-between sticky top-0 z-30 select-none"
    >
      {/* Page Title */}
      <div className="flex items-center gap-4">
        <h1 className="text-xl font-semibold text-white tracking-tight font-sans">
          {title}
        </h1>
        <div className="hidden sm:block h-4 w-px bg-[#262626]"></div>
        <div className="hidden sm:flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-[#111111] border border-[#262626] text-[10px] text-[#9CA3AF] font-mono">
          <Database className="w-3 h-3 text-[#6C2BFF]" />
          <span>Real-Time Edge Sync</span>
        </div>
      </div>

      {/* Right Controls Segment */}
      <div className="flex items-center gap-3.5">
        {/* Environment Filter (Prod vs Staging) */}
        <div className="flex bg-[#111111] p-0.5 rounded-xl border border-[#262626]">
          <button
            onClick={() => onEnvironmentChange("production")}
            className={`px-3 py-1.5 text-[10px] font-mono tracking-wider transition-all rounded-lg cursor-pointer ${
              environment === "production"
                ? "bg-[#1f1f24] text-white font-semibold shadow-sm"
                : "text-[#9CA3AF] hover:text-white"
            }`}
          >
            PROD
          </button>
          <button
            onClick={() => onEnvironmentChange("staging")}
            className={`px-3 py-1.5 text-[10px] font-mono tracking-wider transition-all rounded-lg cursor-pointer ${
              environment === "staging"
                ? "bg-[#1f1f24] text-white font-semibold shadow-sm"
                : "text-[#9CA3AF] hover:text-white"
            }`}
          >
            STAGING
          </button>
        </div>

        {/* Period selection */}
        <div className="hidden md:flex items-center gap-0.5 bg-[#111111] border border-[#262626] rounded-xl p-0.5">
          {PERIOD_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => onPeriodChange(f.value)}
              className={`px-3 py-1.5 text-[11px] rounded-lg transition-colors cursor-pointer ${
                period === f.value
                  ? "bg-[#1f1f24] text-white font-medium"
                  : "text-[#9CA3AF] hover:text-white"
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Action: Export */}
        {onExport && (
          <button
            onClick={onExport}
            className="px-4 py-2 rounded-xl border border-[#262626] bg-[#111111] text-xs font-semibold text-[#9CA3AF] hover:text-white transition-all cursor-pointer flex items-center gap-2"
            title="Download CSV database dump"
          >
            <Download className="w-3.5 h-3.5" />
            <span>Export CSV</span>
          </button>
        )}

        {/* Action: Refresh */}
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="px-4 py-2 rounded-xl bg-[#6C2BFF] hover:bg-[#7D45FF] text-xs font-semibold text-white transition-all flex items-center gap-2 shadow-lg shadow-[#6C2BFF]/20 cursor-pointer disabled:opacity-40"
            title="Sincronizar telemetria"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin" : ""}`} />
            <span>Refresh</span>
          </button>
        )}
      </div>
    </header>
  );
};
