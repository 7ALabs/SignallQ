import React from "react";
import { LogOut } from "lucide-react";
import { NAVIGATION_SECTIONS } from "../../config/navigation";
import { NAVIGATION_ICON_MAP as iconMap } from "../../config/navigationIcons";
import { AppEnvironment } from "../../types/admin";
import { errorMetricsService } from "../../services/errorMetricsService";

/**
 * GH#1041 — Nav Rail: terceiro estado de navegação para tablet (768-1024px,
 * decisão de engenharia — o protótipo `md3-tobe` não define o breakpoint
 * numérico, só o componente em si; ver
 * docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md item 11).
 * 88px de largura, ícone-only sem label (diferente do padrão M3 canônico de
 * rail, que normalmente tem label sob o ícone), pill ativo 56x32/radius16,
 * badge de contagem circular 16px.
 */
interface NavRailProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  theme?: "dark" | "light";
  onToggleTheme?: () => void;
  onLogout?: () => void;
  id?: string;
}

export const NavRail: React.FC<NavRailProps> = ({
  currentPath,
  onNavigate,
  environment,
  theme = "dark",
  onToggleTheme,
  onLogout,
  id,
}) => {
  const [errorBadgeCount, setErrorBadgeCount] = React.useState<number | null>(null);
  const [accountMenuOpen, setAccountMenuOpen] = React.useState(false);

  // Mesma fonte real de dado do badge do Sidebar/Topbar (errorMetricsService) —
  // evita dessincronia entre o número exibido no rail e o KPI real da tela.
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

  const items = NAVIGATION_SECTIONS.flatMap((s) => s.items);

  return (
    <div
      id={id || "nav-rail-container"}
      className="w-[var(--nav-rail-width)] h-screen flex flex-col items-center justify-between shrink-0 select-none py-4"
      style={{ backgroundColor: "var(--bg-sidebar)" }}
    >
      <div className="flex flex-col items-center gap-1 w-full">
        {/* Avatar de projeto — só ícone, sem menu de troca (diferente do drawer) */}
        <div
          className="w-8 h-8 rounded-full flex items-center justify-center text-[11px] font-sans font-semibold text-white shrink-0 select-none mb-4"
          style={{ background: "linear-gradient(135deg, var(--primary), var(--sq-accent-blue))" }}
          title="SignallQ Admin"
        >
          SQ
        </div>

        <div className="w-8 border-t mb-2" style={{ borderColor: "var(--border-subtle)" }} />

        <nav className="flex flex-col items-center gap-1.5 w-full">
          {items.map((item) => {
            const IconComponent = iconMap[item.iconName];
            const isActive = currentPath === item.path;
            const badgeLabel = item.path === "/errors"
              ? (errorBadgeCount && errorBadgeCount > 0 ? String(errorBadgeCount) : undefined)
              : item.badge;

            return (
              <button
                key={item.path}
                onClick={() => onNavigate(item.path)}
                title={item.name}
                aria-label={item.name}
                className="relative w-[56px] h-8 flex items-center justify-center rounded-2xl transition-all duration-150 cursor-pointer"
                style={
                  isActive
                    ? { backgroundColor: "var(--nav-active-bg)" }
                    : undefined
                }
              >
                {IconComponent && (
                  <IconComponent
                    className="w-[22px] h-[22px] shrink-0 transition-colors"
                    style={{ color: isActive ? "var(--nav-active-fg)" : "var(--text-secondary)" }}
                  />
                )}
                {badgeLabel && (
                  <span
                    className="absolute -top-1 -right-1 w-4 h-4 rounded-full flex items-center justify-center text-[9px] font-sans font-bold"
                    style={{
                      backgroundColor: "var(--error-container)",
                      color: "var(--on-error-container)",
                    }}
                  >
                    {errorBadgeCount != null && errorBadgeCount > 9 ? "9+" : badgeLabel}
                  </span>
                )}
              </button>
            );
          })}
        </nav>
      </div>

      {/* Avatar de conta fixo embaixo com popover lateral (theme + logout) */}
      <div className="relative">
        {accountMenuOpen && (
          <>
            <div
              className="fixed inset-0 z-40"
              onClick={() => setAccountMenuOpen(false)}
              aria-hidden="true"
            />
            <div
              className="absolute bottom-0 z-50 w-44 rounded-[var(--radius-card)] p-1.5 flex flex-col gap-0.5"
              style={{
                left: "76px",
                backgroundColor: "var(--bg-surface)",
                border: "1px solid var(--border)",
                boxShadow: "0 12px 28px -8px rgba(0,0,0,0.35)",
              }}
            >
              {onToggleTheme && (
                <button
                  onClick={() => { onToggleTheme(); setAccountMenuOpen(false); }}
                  className="flex items-center gap-2 px-2.5 py-2 text-xs rounded-lg transition-colors cursor-pointer"
                  style={{ color: "var(--text-secondary)" }}
                >
                  {theme === "dark" ? "Tema claro" : "Tema escuro"}
                </button>
              )}
              {onLogout && (
                <button
                  onClick={() => { onLogout(); setAccountMenuOpen(false); }}
                  className="flex items-center gap-2 px-2.5 py-2 text-xs rounded-lg transition-colors cursor-pointer"
                  style={{ color: "var(--text-secondary)" }}
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Sair
                </button>
              )}
            </div>
          </>
        )}
        <button
          onClick={() => setAccountMenuOpen((v) => !v)}
          className="w-9 h-9 rounded-full flex items-center justify-center text-[11px] font-sans font-semibold shrink-0 select-none cursor-pointer transition-colors"
          style={{
            backgroundColor: "var(--bg-surface)",
            color: "var(--text-secondary)",
          }}
          title="Conta"
          aria-label="Abrir menu de conta"
        >
          SQ
        </button>
      </div>
    </div>
  );
};
