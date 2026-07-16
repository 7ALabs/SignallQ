import React from "react";
import { RefreshCw, Bell, Palette, LogOut } from "lucide-react";
import { AppEnvironment } from "../../types/admin";
import { PERIOD_FILTERS } from "../../config/constants";
import { adminMetricsService } from "../../services/adminMetricsService";
import { alpha } from "../../utils/color";

// Link para o DESIGN.md do repositório — mesmo destino real usado como
// referência de design system, sem inventar uma tela nova só pra esse link.
const DESIGN_SYSTEM_URL =
  "https://github.com/gmmattey/linka-android/blob/main/SignallQ%20Admin/DESIGN.md";

interface TopbarProps {
  environment: AppEnvironment;
  onEnvironmentChange: (env: AppEnvironment) => void;
  period: string;
  onPeriodChange: (period: string) => void;
  onRefresh?: () => void;
  onLogout?: () => void;
  onNavigate?: (path: string) => void;
  isRefreshing?: boolean;
  theme?: "dark" | "light";
  id?: string;
}

export const Topbar: React.FC<TopbarProps> = ({
  environment,
  onEnvironmentChange,
  period,
  onPeriodChange,
  onRefresh,
  onLogout,
  onNavigate,
  isRefreshing = false,
  id,
}) => {
  // Contagem real de alertas — mesma fonte de dado do card "Alertas Recentes"
  // da Overview (adminMetricsService.getRecentAlerts), não valor estático.
  const [alertsCount, setAlertsCount] = React.useState<number | null>(null);

  React.useEffect(() => {
    let active = true;
    adminMetricsService.getRecentAlerts({ environment }).then((alerts) => {
      if (active) setAlertsCount(alerts.length);
    }).catch(() => {
      if (active) setAlertsCount(null);
    });
    return () => { active = false; };
  }, [environment]);

  return (
    <header
      id={id || "topbar-header"}
      className="h-14 lg:h-16 px-4 lg:px-8 flex items-center justify-between sticky top-0 z-30 select-none"
      style={{
        backgroundColor: "var(--bg-topbar)",
      }}
    >
      {/* Left: filtros globais (paridade com mockup). Sem hamburger — o
          BottomNav (mobile) e o NavRail (tablet) cobrem a navegação nesses
          breakpoints, não sobra estado que precise abrir a Sidebar como
          overlay (ver GH#1041). */}
      <div className="flex items-center gap-2 lg:gap-3.5 min-w-0">
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
              className="px-2.5 lg:px-3 py-1.5 min-h-[44px] lg:min-h-0 flex items-center justify-center text-[11px] font-sans tracking-[0.04em] uppercase transition-all rounded-lg cursor-pointer"
              style={
                environment === env
                  ? {
                      backgroundColor: "var(--nav-active-bg)",
                      color: "var(--nav-active-fg)",
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
              className="px-3 py-1.5 min-h-[44px] lg:min-h-0 flex items-center justify-center text-[11px] font-sans rounded-lg transition-colors cursor-pointer"
              style={
                period === f.value
                  ? {
                      backgroundColor: "var(--nav-active-bg)",
                      color: "var(--nav-active-fg)",
                      fontWeight: 500,
                    }
                  : { color: "var(--text-secondary)" }
              }
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Right Controls Segment */}
      <div className="flex items-center gap-2 lg:gap-3.5 shrink-0">
        {/* Alertas não lidos — mesma contagem real da Overview */}
        <button
          onClick={() => onNavigate?.("/overview")}
          className="hidden md:flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer"
          style={{
            backgroundColor: alpha("var(--sq-accent-blue)", 10),
            color: "var(--sq-accent-blue)",
          }}
          title="Ver alertas recentes"
        >
          <Bell className="w-3.5 h-3.5" />
          <span>
            {alertsCount != null && alertsCount > 0 ? `Alertas não lidos (${alertsCount})` : "Alertas não lidos"}
          </span>
        </button>

        {/* Action: Refresh */}
        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isRefreshing}
            className="p-2 min-w-[44px] min-h-[44px] lg:px-4 lg:py-2 lg:min-w-0 lg:min-h-0 rounded-xl text-xs font-semibold transition-all flex items-center justify-center gap-2 cursor-pointer disabled:opacity-40"
            style={{
              backgroundColor: "var(--primary)",
              color: "var(--on-primary)",
              boxShadow: `0 4px 12px ${alpha("var(--primary)", 20)}`,
            }}
            title="Sincronizar telemetria"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? "animate-spin" : ""}`} />
            <span className="hidden lg:inline">Atualizar</span>
          </button>
        )}

        {/* Link Design System — aponta pro DESIGN.md real do repo, sem tela nova */}
        <a
          href={DESIGN_SYSTEM_URL}
          target="_blank"
          rel="noreferrer"
          className="hidden lg:flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs font-semibold transition-all cursor-pointer"
          style={{
            backgroundColor: "var(--bg-surface)",
            color: "var(--text-secondary)",
          }}
          title="Abrir DESIGN.md no GitHub"
        >
          <Palette className="w-3.5 h-3.5" />
          <span>Design System</span>
        </a>

        {/* Sair */}
        {onLogout && (
          <button
            onClick={onLogout}
            className="w-8 h-8 shrink-0 flex items-center justify-center rounded-full transition-all cursor-pointer"
            style={{
              backgroundColor: "var(--bg-surface)",
              color: "var(--text-secondary)",
            }}
            title="Sair"
            aria-label="Sair"
          >
            <LogOut className="w-4 h-4" />
          </button>
        )}
      </div>
    </header>
  );
};
