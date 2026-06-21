import React from "react";
import { Search, RefreshCw, Download } from "lucide-react";
import { AppEnvironment } from "../../types/admin";
import { PERIOD_FILTERS } from "../../config/constants";

interface FilterBarProps {
  searchText?: string;
  onSearchChange?: (val: string) => void;
  searchPlaceholder?: string;
  
  environment: AppEnvironment;
  onEnvironmentChange: (env: AppEnvironment) => void;
  
  period: string;
  onPeriodChange: (period: string) => void;
  
  onRefresh?: () => void;
  onExport?: () => void;
  isRefreshing?: boolean;
}

export const FilterBar: React.FC<FilterBarProps> = ({
  searchText = "",
  onSearchChange,
  searchPlaceholder = "Buscar registros...",
  environment,
  onEnvironmentChange,
  period,
  onPeriodChange,
  onRefresh,
  onExport,
  isRefreshing = false,
}) => {
  return (
    <div className="flex flex-col xl:flex-row gap-3.5 items-stretch xl:items-center justify-between pb-5 border-b border-[#262626] mb-5">
      {/* Search Bar Input */}
      <div className="relative flex-1 max-w-lg">
        {onSearchChange ? (
          <>
            <span className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none text-[#6B7280]">
              <Search className="w-4 h-4" />
            </span>
            <input
              type="text"
              value={searchText}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder={searchPlaceholder}
              className="w-full pl-10 pr-4 py-2 text-xs text-white placeholder-[#6B7280] bg-[#111111] border border-[#262626] rounded-xl focus:border-[#6C2BFF] focus:outline-none focus:ring-0 transition-colors"
            />
          </>
        ) : (
          <div className="h-9" /> // spacer
        )}
      </div>

      {/* Controls Container */}
      <div className="flex flex-wrap items-center gap-3.5">
        {/* Environment Toggle (Segmented Selector) */}
        <div className="flex items-center bg-[#111111] p-1 rounded-xl border border-[#262626]">
          <button
            onClick={() => onEnvironmentChange("production")}
            className={`px-3.5 py-1 text-[11px] font-mono tracking-wider uppercase rounded-lg transition-all duration-200 cursor-pointer ${
              environment === "production"
                ? "bg-[#1f1f24] text-white border-[#262626] font-medium"
                : "text-[#9CA3AF] border-transparent hover:text-white"
            }`}
          >
            PROD
          </button>
          <button
            onClick={() => onEnvironmentChange("staging")}
            className={`px-3.5 py-1 text-[11px] font-mono tracking-wider uppercase rounded-lg transition-all duration-200 cursor-pointer ${
              environment === "staging"
                ? "bg-[#1f1f24] text-white border-[#262626] font-medium"
                : "text-[#9CA3AF] border-transparent hover:text-white"
            }`}
          >
            STAGING
          </button>
        </div>

        {/* Period Filter Selector */}
        <div className="flex items-center gap-1 bg-[#111111] rounded-xl border border-[#262626] p-0.5">
          {PERIOD_FILTERS.map((item) => (
            <button
              key={item.value}
              onClick={() => onPeriodChange(item.value)}
              className={`px-3 py-1.5 text-xs rounded-lg transition-colors cursor-pointer ${
                period === item.value
                  ? "bg-[#1f1f24] text-white font-medium"
                  : "text-[#9CA3AF] hover:text-white border border-transparent"
              }`}
            >
              {item.label}
            </button>
          ))}
        </div>

        {/* Action Button: Refresh */}
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="flex items-center gap-1.5 px-3 py-2 bg-[#111111] text-xs text-[#9CA3AF] hover:text-white border border-[#262626] rounded-xl transition-colors hover:border-[#363636] cursor-pointer disabled:opacity-50"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin text-[#6C2BFF]" : ""}`} />
            <span className="hidden sm:inline">Atualizar</span>
          </button>
        )}

        {/* Action Button: Export */}
        {onExport && (
          <button
            onClick={onExport}
            className="flex items-center gap-1.5 px-4 py-2 bg-[#111111] border border-[#262626] text-xs font-semibold text-[#9CA3AF] hover:text-white rounded-xl transition-colors cursor-pointer"
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Exportar</span>
          </button>
        )}
      </div>
    </div>
  );
};
