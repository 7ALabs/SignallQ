import React from "react";
import {
  LayoutDashboard,
  LineChart,
  Activity,
  Wifi,
  Globe,
  BrainCircuit,
  AlertTriangle,
  GitBranch,
  ToggleRight,
  Settings,
  HeartPulse,
  X,
} from "lucide-react";
import { NAVIGATION_SECTIONS } from "../../config/navigation";
import { AppEnvironment } from "../../types/admin";

interface SidebarProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  isOpen?: boolean;
  onClose?: () => void;
  id?: string;
  theme?: "dark" | "light";
}

// Map strings to Lucide components directly to prevent type issue
const iconMap = {
  LayoutDashboard: LayoutDashboard,
  LineChart: LineChart,
  Activity: Activity,
  Wifi: Wifi,
  Globe: Globe,
  BrainCircuit: BrainCircuit,
  AlertTriangle: AlertTriangle,
  GitBranch: GitBranch,
  ToggleRight: ToggleRight,
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
}) => {
  const logoSrc = theme === "dark"
    ? "/brand/7agents/logo-light.svg"
    : "/brand/7agents/logo-dark.svg";
  return (
    <div
      id={id || "sidebar-container"}
      className={`
        w-[240px] h-screen flex flex-col justify-between shrink-0 select-none
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
          <img
            src={logoSrc}
            alt="7Agents Admin Console"
            className="h-10 w-auto"
            draggable={false}
          />
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
          {NAVIGATION_SECTIONS.map((section) => (
            <div key={section.label} className="space-y-1">
              <div
                className="px-3 pt-2 pb-1 text-[10px] font-sans font-semibold uppercase tracking-[0.08em] select-none"
                style={{ color: "var(--text-tertiary)" }}
              >
                {section.label}
              </div>
              {section.items.map((item) => {
                const IconComponent = iconMap[item.iconName as keyof typeof iconMap];
                const isActive = currentPath === item.path;

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

                    {item.badge && (
                      <span
                        className="text-[11px] font-sans font-medium px-2 py-0.5 rounded-md tracking-[0.04em] uppercase"
                        style={
                          item.badgeType === "error"
                            ? {
                                backgroundColor: "color-mix(in srgb, var(--sq-error) 10%, transparent)",
                                color: "var(--sq-error)",
                                border: "1px solid color-mix(in srgb, var(--sq-error) 20%, transparent)",
                              }
                            : {
                                backgroundColor: "var(--sq-bg-overlay)",
                                border: "1px solid var(--sq-border)",
                                color: "var(--sq-text-secondary)",
                              }
                        }
                      >
                        {item.badge}
                      </span>
                    )}
                  </button>
                );
              })}
            </div>
          ))}
        </nav>
      </div>

      {/* Sidebar Footer Info */}
      <div className="p-4" style={{ borderTop: "1px solid var(--border)" }}>
        <div
          className="p-3 rounded-xl flex items-center gap-3"
          style={{
            backgroundColor: "var(--bg-surface)",
            border: "1px solid var(--border)",
          }}
        >
          <div
            className="w-7 h-7 rounded-lg flex items-center justify-center text-[10px] font-mono"
            style={{
              backgroundColor: "var(--bg-surface-hover)",
              color: "var(--text-secondary)",
              border: "1px solid var(--border)",
            }}
          >
            CF
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-semibold block truncate" style={{ color: "var(--text-primary)" }}>
              Cloudflare Gateway
            </span>
            <span className="text-[10px] font-mono block leading-tight" style={{ color: "var(--success)" }}>
              ONLINE · EDGE
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
