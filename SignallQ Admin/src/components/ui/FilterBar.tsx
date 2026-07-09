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
    <div
      className="flex flex-col xl:flex-row gap-3.5 items-stretch xl:items-center justify-between pb-5 mb-5"
      style={{ borderBottom: "1px solid var(--sq-border)" }}
    >
      {/* Search Bar Input */}
      <div className="relative flex-1 max-w-lg">
        {onSearchChange ? (
          <>
            <span
              className="absolute inset-y-0 left-0 flex items-center pl-3.5 pointer-events-none"
              style={{ color: "var(--sq-text-tertiary)" }}
            >
              <Search className="w-4 h-4" />
            </span>
            <input
              type="text"
              value={searchText}
              onChange={(e) => onSearchChange(e.target.value)}
              placeholder={searchPlaceholder}
              className="w-full pl-10 pr-4 py-2 text-xs rounded-[var(--radius-button)] transition-colors focus:outline-none"
              style={{
                color: "var(--sq-text-primary)",
                backgroundColor: "var(--sq-bg-card)",
                border: "1px solid var(--sq-border)",
              }}
              onFocus={(e) => {
                e.currentTarget.style.borderColor = "var(--sq-accent)";
              }}
              onBlur={(e) => {
                e.currentTarget.style.borderColor = "var(--sq-border)";
              }}
            />
          </>
        ) : (
          <div className="h-9" />
        )}
      </div>

      {/* Controls Container */}
      <div className="flex flex-wrap items-center gap-3.5">
        {/* Environment Toggle */}
        <div
          className="flex items-center p-1 rounded-[var(--radius-button)]"
          style={{
            backgroundColor: "var(--sq-bg-card)",
            border: "1px solid var(--sq-border)",
          }}
        >
          {(["production", "staging", "all"] as (AppEnvironment | "all")[]).map((env) => (
            <button
              key={env}
              onClick={() => onEnvironmentChange(env as AppEnvironment)}
              className="px-3.5 py-1 text-[11px] font-mono tracking-wider uppercase rounded-lg transition-all duration-200 cursor-pointer"
              style={
                environment === env
                  ? {
                      backgroundColor: "var(--sq-control-active)",
                      color: "var(--sq-text-primary)",
                      fontWeight: 500,
                    }
                  : { color: "var(--sq-text-secondary)" }
              }
            >
              {env === "production" ? "PROD" : env === "staging" ? "STAGING" : "TODOS"}
            </button>
          ))}
        </div>

        {/* Period Filter */}
        <div
          className="flex items-center gap-1 rounded-[var(--radius-button)] p-0.5"
          style={{
            backgroundColor: "var(--sq-bg-card)",
            border: "1px solid var(--sq-border)",
          }}
        >
          {PERIOD_FILTERS.map((item) => (
            <button
              key={item.value}
              onClick={() => onPeriodChange(item.value)}
              className="px-3 py-1.5 text-xs rounded-lg transition-colors cursor-pointer"
              style={
                period === item.value
                  ? {
                      backgroundColor: "var(--sq-control-active)",
                      color: "var(--sq-text-primary)",
                      fontWeight: 500,
                    }
                  : { color: "var(--sq-text-secondary)" }
              }
            >
              {item.label}
            </button>
          ))}
        </div>

        {/* Action: Refresh */}
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="flex items-center gap-1.5 px-3 py-2 text-xs rounded-[var(--radius-button)] transition-colors cursor-pointer disabled:opacity-50"
            style={{
              backgroundColor: "var(--sq-bg-card)",
              border: "1px solid var(--sq-border)",
              color: "var(--sq-text-secondary)",
            }}
          >
            <RefreshCw
              className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin" : ""}`}
              style={isRefreshing ? { color: "var(--sq-accent)" } : undefined}
            />
            <span className="hidden sm:inline">Atualizar</span>
          </button>
        )}

        {/* Action: Export */}
        {onExport && (
          <button
            onClick={onExport}
            className="flex items-center gap-1.5 px-4 py-2 text-xs font-semibold rounded-[var(--radius-button)] transition-colors cursor-pointer"
            style={{
              backgroundColor: "var(--sq-bg-card)",
              border: "1px solid var(--sq-border)",
              color: "var(--sq-text-secondary)",
            }}
          >
            <Download className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Exportar</span>
          </button>
        )}
      </div>
    </div>
  );
};
