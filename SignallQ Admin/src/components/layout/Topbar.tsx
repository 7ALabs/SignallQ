import React from "react";
import { RefreshCw, Download, Database, LogOut, Menu } from "lucide-react";
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
  onLogout?: () => void;
  onOpenMobileSidebar?: () => void;
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
  onLogout,
  onOpenMobileSidebar,
  isRefreshing = false,
  id,
}) => {
  return (
    <header
      id={id || "topbar-header"}
      className="h-14 lg:h-16 px-4 lg:px-8 flex items-center justify-between sticky top-0 z-30 select-none"
      style={{
        borderBottom: "1px solid var(--sq-border)",
        backgroundColor: "var(--sq-bg-primary)",
      }}
    >
      {/* Left: hamburger (mobile) + title */}
      <div className="flex items-center gap-3 min-w-0">
        {/* Hamburger — mobile only */}
        {onOpenMobileSidebar && (
          <button
            onClick={onOpenMobileSidebar}
            className="lg:hidden p-2 rounded-xl transition-colors shrink-0"
            style={{
              border: "1px solid var(--sq-border)",
              backgroundColor: "var(--sq-bg-card)",
              color: "var(--sq-text-secondary)",
            }}
            aria-label="Abrir menu"
          >
            <Menu className="w-4 h-4" />
          </button>
        )}

        {/* Page Title */}
        <div className="flex items-center gap-3 min-w-0">
          <div className="flex items-center gap-2 min-w-0">
            <h1 className="text-base lg:text-xl font-semibold tracking-tight truncate" style={{ color: "var(--sq-text-primary)" }}>
              {title}
            </h1>
            {environment === "staging" && (
              <span
                className="shrink-0 px-2 py-0.5 text-[10px] font-mono rounded"
                style={{
                  backgroundColor: "color-mix(in srgb, var(--sq-warning) 20%, transparent)",
                  color: "var(--sq-warning)",
                  border: "1px solid color-mix(in srgb, var(--sq-warning) 30%, transparent)",
                }}
              >
                STAGING
              </span>
            )}
          </div>
          <div
            className="hidden lg:block h-4 w-px"
            style={{ backgroundColor: "var(--sq-border)" }}
          />
          <div
            className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[10px] font-mono"
            style={{
              backgroundColor: "var(--sq-bg-card)",
              border: "1px solid var(--sq-border)",
              color: "var(--sq-text-secondary)",
            }}
          >
            <Database className="w-3 h-3" style={{ color: "var(--sq-accent)" }} />
            <span>Real-Time Edge Sync</span>
          </div>
        </div>
      </div>

      {/* Right Controls Segment */}
      <div className="flex items-center gap-2 lg:gap-3.5 shrink-0">
        {/* Environment Filter */}
        <div
          className="flex p-0.5 rounded-xl"
          style={{
            backgroundColor: "var(--sq-bg-card)",
            border: "1px solid var(--sq-border)",
          }}
        >
          {(["production", "staging"] as AppEnvironment[]).map((env) => (
            <button
              key={env}
              onClick={() => onEnvironmentChange(env)}
              className="px-2.5 lg:px-3 py-1.5 text-[10px] font-mono tracking-wider transition-all rounded-lg cursor-pointer"
              style={
                environment === env
                  ? {
                      backgroundColor: "var(--sq-control-active)",
                      color: "var(--sq-text-primary)",
                      fontWeight: 600,
                    }
                  : { color: "var(--sq-text-secondary)" }
              }
            >
              {env === "production" ? "PROD" : "STG"}
            </button>
          ))}
        </div>

        {/* Period selection — hidden on mobile */}
        <div
          className="hidden md:flex items-center gap-0.5 p-0.5 rounded-xl"
          style={{
            backgroundColor: "var(--sq-bg-card)",
            border: "1px solid var(--sq-border)",
          }}
        >
          {PERIOD_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => onPeriodChange(f.value)}
              className="px-3 py-1.5 text-[11px] rounded-lg transition-colors cursor-pointer"
              style={
                period === f.value
                  ? {
                      backgroundColor: "var(--sq-control-active)",
                      color: "var(--sq-text-primary)",
                      fontWeight: 500,
                    }
                  : { color: "var(--sq-text-secondary)" }
              }
            >
              {f.label}
            </button>
          ))}
        </div>

        {/* Action: Export — hidden on mobile */}
        {onExport && (
          <button
            onClick={onExport}
            className="hidden lg:flex px-4 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer items-center gap-2"
            style={{
              border: "1px solid var(--sq-border)",
              backgroundColor: "var(--sq-bg-card)",
              color: "var(--sq-text-secondary)",
            }}
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
            className="p-2 lg:px-4 lg:py-2 rounded-xl text-xs font-semibold text-white transition-all flex items-center gap-2 cursor-pointer disabled:opacity-40"
            style={{
              backgroundColor: "var(--sq-accent)",
              boxShadow: "0 4px 12px color-mix(in srgb, var(--sq-accent) 20%, transparent)",
            }}
            title="Sincronizar telemetria"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin" : ""}`} />
            <span className="hidden lg:inline">Refresh</span>
          </button>
        )}

        {/* Action: Logout */}
        {onLogout && (
          <button
            onClick={onLogout}
            className="p-2 rounded-xl transition-all cursor-pointer"
            style={{
              border: "1px solid var(--sq-border)",
              backgroundColor: "var(--sq-bg-card)",
              color: "var(--sq-text-tertiary)",
            }}
            title="Sair"
          >
            <LogOut className="w-4 h-4" />
          </button>
        )}
      </div>
    </header>
  );
};
