import React from "react";
import { alpha } from "../../utils/color";
import {
  LayoutDashboard,
  LineChart,
  Activity,
  Wifi,
  BrainCircuit,
  AlertTriangle,
  GitBranch,
  Settings,
  HeartPulse,
  X,
} from "lucide-react";
import { NAVIGATION_SECTIONS } from "../../config/navigation";
import { AppEnvironment } from "../../types/admin";
import { errorMetricsService } from "../../services/errorMetricsService";

interface SidebarProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  isOpen?: boolean;
  onClose?: () => void;
  id?: string;
  theme?: "dark" | "light";
  onToggleTheme?: () => void;
}

// Map strings to Lucide components directly to prevent type issue
const iconMap = {
  LayoutDashboard: LayoutDashboard,
  LineChart: LineChart,
  Activity: Activity,
  Wifi: Wifi,
  BrainCircuit: BrainCircuit,
  AlertTriangle: AlertTriangle,
  GitBranch: GitBranch,
  Settings: Settings,
  HeartPulse: HeartPulse,
};

export const Sidebar: React.FC<SidebarProps> = ({
  currentPath,
  onNavigate,
  environment,
  isOpen = false,
  onClose,
  id,
  theme = "dark",
  onToggleTheme,
}) => {
  // Badge de "Problemas & Incidentes" vem da mesma fonte de dado da tela de
  // erros (errorMetricsService), não de valor estático — evita dessincronia
  // entre o número do menu e os KPIs reais da página (GH#issue badge vs KPI).
  const [errorBadgeCount, setErrorBadgeCount] = React.useState<number | null>(null);

  React.useEffect(() => {
    let active = true;
    errorMetricsService.getErrorMetricSummary({ environment }).then((data) => {
      if (!active) return;
      const match = data?.activeErrors.match(/\d+/);
      setErrorBadgeCount(match ? parseInt(match[0], 10) : null);
    }).catch(() => {
      if (active) setErrorBadgeCount(null);
    });
    return () => { active = false; };
  }, [environment]);

  // GH#443: caminho relativo ao BASE_URL — o Console pode ser servido em /console
  const logoSrc = theme === "dark"
    ? `${import.meta.env.BASE_URL}brand/7agents/logo-light.svg`
    : `${import.meta.env.BASE_URL}brand/7agents/logo-dark.svg`;
  return (
    <div
      id={id || "sidebar-container"}
      className={`
        w-[264px] h-screen flex flex-col justify-between shrink-0 select-none
        fixed lg:relative z-50 lg:z-auto
        transition-transform duration-200 ease-in-out
        ${isOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"}
      `}
      style={{
        backgroundColor: "var(--bg-sidebar)",
        borderRight: "1px solid var(--border)",
      }}
    >
      {/* Top Session / Branding */}
      <div className="flex flex-col">
        {/* Logo Section */}
        <div
          className="p-6 flex items-center justify-between"
          style={{ borderBottom: "1px solid var(--border)" }}
        >
          <div className="flex items-center gap-3 min-w-0">
            <img
              src={logoSrc}
              alt="7Agents Admin Console"
              className="h-10 w-auto shrink-0"
              draggable={false}
            />
            <span
              className="text-[10px] font-sans font-semibold uppercase tracking-[0.08em] truncate hidden xl:block"
              style={{ color: "var(--text-tertiary)" }}
            >
              Admin Console
            </span>
          </div>
          {/* Close button — mobile only */}
          {onClose && (
            <button
              onClick={onClose}
              className="lg:hidden p-1.5 rounded-lg transition-colors"
              style={{ color: "var(--text-tertiary)" }}
              aria-label="Fechar menu"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Environment Status Badge */}
        <div className="px-6 py-4" style={{ backgroundColor: "var(--bg-sidebar)" }}>
          <div
            className="flex items-center gap-2 px-3 py-1.5 rounded-full w-fit"
            style={{
              backgroundColor: "var(--bg-surface-hover)",
              border: "1px solid var(--border)",
            }}
          >
            <div
              className="w-2 h-2 rounded-full"
              style={{
                backgroundColor: environment === "production" ? "var(--success)" : "var(--sq-phase-latency)",
                boxShadow: environment === "production"
                  ? "0 0 8px var(--success)"
                  : "0 0 8px var(--sq-phase-latency)",
              }}
            />
            <span
              className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em]"
              style={{ color: "var(--text-secondary)" }}
            >
              {environment === "production" ? "Production" : "Staging"}
            </span>
          </div>
        </div>

        {/* Navigation Menus List — agrupada por proveniência de dado (SIG-294) */}
        <nav className="px-4 py-2 space-y-4">
          {NAVIGATION_SECTIONS.map((section, sectionIndex) => (
            <div key={section.label || `section-${sectionIndex}`} className="space-y-1">
              {section.label && (
                <div
                  className="px-3 pt-2 pb-1 text-[10px] font-sans font-semibold uppercase tracking-[0.08em] select-none"
                  style={{ color: "var(--text-tertiary)" }}
                >
                  {section.label}
                </div>
              )}
              {section.items.map((item) => {
                const IconComponent = iconMap[item.iconName as keyof typeof iconMap];
                const isActive = currentPath === item.path;
                const badgeLabel = item.path === "/errors"
                  ? (errorBadgeCount && errorBadgeCount > 0 ? String(errorBadgeCount) : undefined)
                  : item.badge;

                return (
                  <button
                    key={item.path}
                    onClick={() => onNavigate(item.path)}
                    className="w-full flex items-center justify-between px-3 py-2 min-h-[44px] rounded-xl text-[13px] font-medium border transition-all duration-150 select-none cursor-pointer"
                    style={
                      isActive
                        ? {
                            backgroundColor: "var(--bg-sidebar-active)",
                            color: "var(--primary)",
                            borderColor: "transparent",
                          }
                        : {
                            color: "var(--text-secondary)",
                            borderColor: "transparent",
                          }
                    }
                  >
                    <div className="flex items-center gap-3">
                      {IconComponent && (
                        <IconComponent
                          className="w-4 h-4 shrink-0 transition-colors"
                          style={{ color: isActive ? "var(--primary)" : "var(--text-secondary)" }}
                        />
                      )}
                      <span>{item.name}</span>
                    </div>

                    {badgeLabel && (
                      <span
                        className="text-[11px] font-sans font-medium px-2 py-0.5 rounded-md tracking-[0.04em] uppercase"
                        style={
                          item.badgeType === "error"
                            ? {
                                backgroundColor: alpha("var(--sq-error)", 10),
                                color: "var(--sq-error)",
                                border: `1px solid ${alpha("var(--sq-error)", 20)}`,
                              }
                            : {
                                backgroundColor: "var(--sq-bg-overlay)",
                                border: "1px solid var(--sq-border)",
                                color: "var(--sq-text-secondary)",
                              }
                        }
                      >
                        {badgeLabel}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>
      </div>

      {/* Sidebar Footer — usuário + tema (Design System.dc.html) */}
      <div className="p-4 flex items-center gap-3" style={{ borderTop: "1px solid var(--border)" }}>
        <div
          className="w-9 h-9 rounded-full flex items-center justify-center text-[11px] font-sans font-semibold text-white shrink-0 select-none"
          style={{ background: "linear-gradient(135deg, var(--primary), var(--sq-accent-blue))" }}
        >
          SQ
        </div>
        <div className="min-w-0 flex-1">
          <span className="text-[12px] font-sans font-semibold block truncate" style={{ color: "var(--text-primary)" }}>
            Admin
          </span>
          <span className="text-[10px] font-sans block leading-tight truncate" style={{ color: "var(--text-tertiary)" }}>
            Squad técnico
          </span>
        </div>
        {onToggleTheme && (
          <button
            onClick={onToggleTheme}
            className="w-[30px] h-[30px] shrink-0 flex items-center justify-center rounded-full transition-colors cursor-pointer"
            style={{
              border: "1px solid var(--border)",
              backgroundColor: "var(--bg-surface)",
              color: "var(--text-tertiary)",
            }}
            title={theme === "dark" ? "Alternar para tema claro" : "Alternar para tema escuro"}
            aria-label={theme === "dark" ? "Alternar para tema claro" : "Alternar para tema escuro"}
          >
            <span className="material-symbols-outlined" style={{ fontSize: "16px", lineHeight: 1, display: "block" }}>
              {theme === "dark" ? "light_mode" : "dark_mode"}
            </span>
          </button>
        )}
      </div>
    </div>
  );
};
