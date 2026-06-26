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
  X,
} from "lucide-react";
import { NAVIGATION_ITEMS } from "../../config/navigation";
import { AppEnvironment } from "../../types/admin";

interface SidebarProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  isOpen?: boolean;
  onClose?: () => void;
  id?: string;
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
};

export const Sidebar: React.FC<SidebarProps> = ({
  currentPath,
  onNavigate,
  environment,
  isOpen = false,
  onClose,
  id,
}) => {
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
        backgroundColor: "var(--sq-bg-primary)",
        borderRight: "1px solid var(--sq-border)",
      }}
    >
      {/* Top Session / Branding */}
      <div className="flex flex-col">
        {/* Logo Section */}
        <div
          className="p-6 flex items-center justify-between"
          style={{ borderBottom: "1px solid var(--sq-border)" }}
        >
          <div className="flex items-center gap-3">
            <div
              className="w-8 h-8 rounded-lg flex items-center justify-center text-white"
              style={{
                background: "linear-gradient(135deg, var(--sq-accent), var(--sq-accent-blue))",
                boxShadow: "0 4px 12px color-mix(in srgb, var(--sq-accent) 20%, transparent)",
              }}
            >
              <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2.5" d="M13 10V3L4 14h7v7l9-11h-7z" />
              </svg>
            </div>
            <div>
              <span className="font-bold text-lg tracking-tight block" style={{ color: "var(--sq-text-primary)" }}>
                SignallQ <span className="font-normal" style={{ color: "var(--sq-text-tertiary)" }}>Admin</span>
              </span>
            </div>
          </div>
          {/* Close button — mobile only */}
          {onClose && (
            <button
              onClick={onClose}
              className="lg:hidden p-1.5 rounded-lg transition-colors"
              style={{ color: "var(--sq-text-tertiary)" }}
              aria-label="Fechar menu"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Environment Status Badge */}
        <div className="px-6 py-4" style={{ backgroundColor: "var(--sq-bg-primary)" }}>
          <div
            className="flex items-center gap-2 px-3 py-1.5 rounded-full w-fit"
            style={{
              backgroundColor: "var(--sq-bg-overlay)",
              border: "1px solid var(--sq-border)",
            }}
          >
            <div
              className="w-2 h-2 rounded-full"
              style={{
                backgroundColor: environment === "production" ? "var(--sq-success)" : "var(--sq-phase-latency)",
                boxShadow: environment === "production"
                  ? "0 0 8px var(--sq-success)"
                  : "0 0 8px var(--sq-phase-latency)",
              }}
            />
            <span
              className="text-[10px] font-semibold uppercase tracking-wider"
              style={{ color: "var(--sq-text-secondary)" }}
            >
              {environment === "production" ? "Production" : "Staging"}
            </span>
          </div>
        </div>

        {/* Navigation Menus List */}
        <nav className="px-4 py-2 space-y-1">
          {NAVIGATION_ITEMS.map((item) => {
            const IconComponent = iconMap[item.iconName as keyof typeof iconMap];
            const isActive = currentPath === item.path;

            return (
              <button
                key={item.path}
                onClick={() => onNavigate(item.path)}
                className="w-full flex items-center justify-between px-3 py-2 rounded-xl text-xs font-medium border transition-all duration-150 select-none cursor-pointer"
                style={
                  isActive
                    ? {
                        backgroundColor: "color-mix(in srgb, var(--sq-accent) 10%, transparent)",
                        color: "var(--sq-accent)",
                        borderColor: "color-mix(in srgb, var(--sq-accent) 20%, transparent)",
                      }
                    : {
                        color: "var(--sq-text-secondary)",
                        borderColor: "transparent",
                      }
                }
              >
                <div className="flex items-center gap-3">
                  {IconComponent && (
                    <IconComponent
                      className="w-4 h-4 shrink-0 transition-colors"
                      style={{ color: isActive ? "var(--sq-accent)" : "var(--sq-text-secondary)" }}
                    />
                  )}
                  <span>{item.name}</span>
                </div>

                {item.badge && (
                  <span
                    className="text-[10px] font-mono font-semibold px-2 py-0.5 rounded-md"
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
        </nav>
      </div>

      {/* Sidebar Footer Info */}
      <div className="p-4" style={{ borderTop: "1px solid var(--sq-border)" }}>
        <div
          className="p-3 rounded-xl flex items-center gap-3"
          style={{
            backgroundColor: "var(--sq-bg-card)",
            border: "1px solid var(--sq-border)",
          }}
        >
          <div
            className="w-7 h-7 rounded-lg flex items-center justify-center text-[10px] font-mono"
            style={{
              backgroundColor: "var(--sq-bg-overlay)",
              color: "var(--sq-text-secondary)",
              border: "1px solid var(--sq-border)",
            }}
          >
            CF
          </div>
          <div className="min-w-0">
            <span className="text-[11px] font-semibold block truncate" style={{ color: "var(--sq-text-primary)" }}>
              Cloudflare Gateway
            </span>
            <span className="text-[10px] font-mono block leading-tight" style={{ color: "var(--sq-success)" }}>
              ONLINE · EDGE
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};
