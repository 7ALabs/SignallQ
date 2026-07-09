import React from "react";
import { RefreshCw, Download, Database, LogOut, Menu } from "lucide-react";
import { AppEnvironment } from "../../types/admin";
import { PERIOD_FILTERS } from "../../config/constants";
import { alpha } from "../../utils/color";

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
  theme?: "dark" | "light";
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
  theme = "dark",
  id,
}) => {
  return (
    <header
      id={id || "topbar-header"}
      className="h-14 lg:h-16 px-4 lg:px-8 flex items-center justify-between sticky top-0 z-30 select-none"
      style={{
        borderBottom: "1px solid var(--border)",
        backgroundColor: "var(--bg-base)",
      }}
    >
      {/* Left: hamburger (mobile) + title */}
      <div className="flex items-center gap-3 min-w-0">
        {/* Hamburger — mobile only */}
        {onOpenMobileSidebar && (
          <button
            onClick={onOpenMobileSidebar}
            className="lg:hidden p-2 min-w-[44px] min-h-[44px] flex items-center justify-center rounded-xl transition-colors shrink-0"
            style={{
              border: "1px solid var(--border)",
              backgroundColor: "var(--bg-surface)",
              color: "var(--text-secondary)",
            }}
            aria-label="Abrir menu"
          >
            <Menu className="w-4 h-4" />
          </button>
        )}

        {/* Page Title */}
        <div className="flex items-center gap-3 min-w-0">
          <div className="flex items-center gap-2 min-w-0">
            <h1 className="text-[18px] font-semibold tracking-[-0.01em] truncate" style={{ color: "var(--text-primary)" }}>
              {title}
            </h1>
            {environment === "staging" && (
              <span
                className="shrink-0 px-2 py-0.5 text-[10px] font-mono rounded"
                style={{
                  backgroundColor: alpha("var(--attention)", 20),
                  color: "var(--attention)",
                  border: `1px solid ${alpha("var(--attention)", 30)}`,
                }}
              >
                STAGING
              </span>
            )}
          </div>
          <div
            className="hidden lg:block h-4 w-px"
            style={{ backgroundColor: "var(--border)" }}
          />
          <div
            className="hidden lg:flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-sans"
            style={{
              backgroundColor: "var(--bg-surface)",
              border: "1px solid var(--border)",
              color: "var(--text-secondary)",
            }}
          >
            <Database className="w-3 h-3" style={{ color: "var(--primary)" }} />
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
            backgroundColor: "var(--bg-surface)",
            border: "1px solid var(--border)",
          }}
        >
          {(["production", "staging"] as AppEnvironment[]).map((env) => (
            <button
              key={env}
              onClick={() => onEnvironmentChange(env)}
              className="px-2.5 lg:px-3 py-1.5 text-[11px] font-sans tracking-[0.04em] uppercase transition-all rounded-lg cursor-pointer"
              style={
                environment === env
                  ? {
                      backgroundColor: "var(--bg-sidebar-active)",
                      color: "var(--text-primary)",
                      fontWeight: 600,
                    }
                  : { color: "var(--text-secondary)" }
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
            backgroundColor: "var(--bg-surface)",
            border: "1px solid var(--border)",
          }}
        >
          {PERIOD_FILTERS.map((f) => (
            <button
              key={f.value}
              onClick={() => onPeriodChange(f.value)}
              className="px-3 py-1.5 text-[11px] font-sans rounded-lg transition-colors cursor-pointer"
              style={
                period === f.value
                  ? {
                      backgroundColor: "var(--bg-sidebar-active)",
                      color: "var(--text-primary)",
                      fontWeight: 500,
                    }
                  : { color: "var(--text-secondary)" }
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
              border: "1px solid var(--border)",
              backgroundColor: "var(--bg-surface)",
              color: "var(--text-secondary)",
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
            className="p-2 min-w-[44px] min-h-[44px] lg:px-4 lg:py-2 lg:min-w-0 lg:min-h-0 rounded-xl text-xs font-semibold text-white transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-40"
            style={{
              backgroundColor: "var(--primary)",
              boxShadow: `0 4px 12px ${alpha("var(--primary)", 20)}`,
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
            className="p-2 min-w-[44px] min-h-[44px] flex items-center justify-center rounded-xl transition-all cursor-pointer"
            style={{
              border: "1px solid var(--border)",
              backgroundColor: "var(--bg-surface)",
              color: "var(--text-tertiary)",
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
