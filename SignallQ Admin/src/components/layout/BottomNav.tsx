import React from "react";
import { LayoutDashboard, LineChart, Activity, Wifi, MoreHorizontal, LogOut, Sun, Moon } from "lucide-react";
import { NAVIGATION_SECTIONS } from "../../config/navigation";
import { NAVIGATION_ICON_MAP as iconMap } from "../../config/navigationIcons";
import { AppEnvironment } from "../../types/admin";

/**
 * GH#1041 — Bottom Nav: padrão mobile do protótipo `md3-tobe` (<768px),
 * SUBSTITUI o antigo drawer off-canvas/hambúrguer — não coexistem (ver
 * FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md item 12). 80px de altura, 5
 * itens fixos (os 4 primeiros navegam direto, "Mais" abre um sheet com o
 * resto das 10 seções do Console — o protótipo não especifica o conteúdo do
 * "Mais" além do ícone/label, decisão de produto pragmática do Camilo:
 * reaproveitar a mesma lista do drawer/rail em vez de inventar uma rota
 * "/more" nova).
 */
const FIXED_ITEMS: { name: string; path: string; icon: typeof LayoutDashboard }[] = [
  { name: "Início", path: "/overview", icon: LayoutDashboard },
  { name: "App", path: "/product-analytics", icon: LineChart },
  { name: "Diagnóstico", path: "/diagnostics", icon: Activity },
  { name: "Redes", path: "/networks", icon: Wifi },
];

interface BottomNavProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  theme?: "dark" | "light";
  onToggleTheme?: () => void;
  onLogout?: () => void;
  id?: string;
}

export const BottomNav: React.FC<BottomNavProps> = ({
  currentPath,
  onNavigate,
  theme = "dark",
  onToggleTheme,
  onLogout,
  id,
}) => {
  const [isMaisOpen, setIsMaisOpen] = React.useState(false);

  const fixedPaths = new Set(FIXED_ITEMS.map((i) => i.path));
  const restItems = NAVIGATION_SECTIONS.flatMap((s) => s.items).filter(
    (item) => !fixedPaths.has(item.path)
  );
  const isMaisActive = restItems.some((item) => item.path === currentPath);

  const handleNavigate = (path: string) => {
    onNavigate(path);
    setIsMaisOpen(false);
  };

  return (
    <>
      {isMaisOpen && (
        <div
          id="bottom-nav-mais-sheet"
          className="fixed inset-0 z-40 flex flex-col justify-end md:hidden"
        >
          <div
            className="absolute inset-0"
            style={{ backgroundColor: "rgba(0,0,0,0.5)" }}
            onClick={() => setIsMaisOpen(false)}
            aria-hidden="true"
          />
          <div
            className="relative z-50 rounded-t-[var(--radius-card)] p-4 pb-[calc(var(--bottom-nav-height)+12px)] max-h-[70vh] overflow-y-auto"
            style={{ backgroundColor: "var(--bg-sidebar)", border: "1px solid var(--border)", borderBottom: "none" }}
          >
            <div className="w-10 h-1 rounded-full mx-auto mb-4" style={{ backgroundColor: "var(--border)" }} />
            <div className="grid grid-cols-3 gap-3">
              {restItems.map((item) => {
                const IconComponent = iconMap[item.iconName];
                const isActive = currentPath === item.path;
                return (
                  <button
                    key={item.path}
                    onClick={() => handleNavigate(item.path)}
                    className="flex flex-col items-center gap-1.5 py-3 rounded-[var(--radius-card)] transition-colors cursor-pointer"
                    style={isActive ? { backgroundColor: "var(--nav-active-bg)" } : undefined}
                  >
                    {IconComponent && (
                      <IconComponent
                        className="w-5 h-5"
                        style={{ color: isActive ? "var(--nav-active-fg)" : "var(--text-secondary)" }}
                      />
                    )}
                    <span
                      className="text-[10px] font-sans text-center leading-tight"
                      style={{ color: isActive ? "var(--nav-active-fg)" : "var(--text-secondary)" }}
                    >
                      {item.name}
                    </span>
                  </button>
                );
              })}
            </div>

            <div className="mt-4 pt-4 flex items-center gap-2" style={{ borderTop: "1px solid var(--border-subtle)" }}>
              {onToggleTheme && (
                <button
                  onClick={() => { onToggleTheme(); setIsMaisOpen(false); }}
                  className="flex-1 flex items-center justify-center gap-2 py-2.5 text-xs rounded-[var(--radius-button)] cursor-pointer"
                  style={{ backgroundColor: "var(--bg-surface)", color: "var(--text-secondary)" }}
                >
                  {theme === "dark" ? <Sun className="w-3.5 h-3.5" /> : <Moon className="w-3.5 h-3.5" />}
                  {theme === "dark" ? "Tema claro" : "Tema escuro"}
                </button>
              )}
              {onLogout && (
                <button
                  onClick={() => { onLogout(); setIsMaisOpen(false); }}
                  className="flex-1 flex items-center justify-center gap-2 py-2.5 text-xs rounded-[var(--radius-button)] cursor-pointer"
                  style={{ backgroundColor: "var(--bg-surface)", color: "var(--text-secondary)" }}
                >
                  <LogOut className="w-3.5 h-3.5" />
                  Sair
                </button>
              )}
            </div>
          </div>
        </div>
      )}

      <nav
        id={id || "bottom-nav-container"}
        className="fixed bottom-0 left-0 right-0 z-30 md:hidden flex items-center justify-around"
        style={{
          height: "var(--bottom-nav-height)",
          padding: "12px 8px",
          backgroundColor: "var(--bg-sidebar)",
          borderTop: "1px solid var(--border)",
        }}
      >
        {FIXED_ITEMS.map((item) => {
          const isActive = currentPath === item.path;
          const Icon = item.icon;
          return (
            <button
              key={item.path}
              onClick={() => handleNavigate(item.path)}
              className="flex flex-col items-center justify-center gap-1 w-16 h-8 rounded-2xl transition-colors cursor-pointer"
              style={isActive ? { backgroundColor: "var(--nav-active-bg)" } : undefined}
            >
              <Icon
                className="w-[22px] h-[22px]"
                style={{ color: isActive ? "var(--nav-active-fg)" : "var(--text-secondary)" }}
              />
            </button>
          );
        })}

        <button
          onClick={() => setIsMaisOpen(true)}
          className="flex flex-col items-center justify-center gap-1 w-16 h-8 rounded-2xl transition-colors cursor-pointer"
          style={isMaisActive ? { backgroundColor: "var(--nav-active-bg)" } : undefined}
          aria-label="Mais opções"
        >
          <MoreHorizontal
            className="w-[22px] h-[22px]"
            style={{ color: isMaisActive ? "var(--nav-active-fg)" : "var(--text-secondary)" }}
          />
        </button>
      </nav>
    </>
  );
};
