import React from "react";
import { Sidebar } from "./Sidebar";
import { NavRail } from "./NavRail";
import { BottomNav } from "./BottomNav";
import { Topbar } from "./Topbar";
import { AppEnvironment } from "../../types/admin";

interface AppLayoutProps {
  currentPath: string;
  onNavigate: (path: string) => void;
  environment: AppEnvironment;
  onEnvironmentChange: (env: AppEnvironment) => void;
  period: string;
  onPeriodChange: (p: string) => void;
  onRefresh: () => void;
  isRefreshing: boolean;
  onLogout?: () => void;
  theme?: "dark" | "light";
  onToggleTheme?: () => void;
  children: React.ReactNode;
  id?: string;
}

/**
 * GH#1041 — três estados de navegação, um só ativo por vez conforme a
 * largura de viewport (breakpoint exato não definido pelo protótipo
 * `md3-tobe`, decisão de engenharia documentada em
 * FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md item 11):
 *   - Desktop (>=1024px, `lg:`): Sidebar completa, 300px.
 *   - Tablet (768-1024px, `md:` até `lg:`): NavRail colapsado, 88px, ícone-only.
 *   - Mobile (<768px): BottomNav, 80px, substitui o antigo drawer/hambúrguer
 *     (não coexistem — ver item 12 do mesmo doc).
 */
export const AppLayout: React.FC<AppLayoutProps> = ({
  currentPath,
  onNavigate,
  environment,
  onEnvironmentChange,
  period,
  onPeriodChange,
  onRefresh,
  isRefreshing,
  onLogout,
  theme,
  onToggleTheme,
  children,
  id,
}) => {
  return (
    <div
      id={id || "app-layout-root"}
      className="flex w-full h-screen overflow-hidden select-none font-sans"
      style={{ backgroundColor: "var(--bg-base)", color: "var(--text-primary)" }}
    >
      {/* 1. Navegação — um único componente visível por breakpoint */}
      <div className="hidden lg:block">
        <Sidebar
          currentPath={currentPath}
          onNavigate={onNavigate}
          environment={environment}
          theme={theme}
          onToggleTheme={onToggleTheme}
        />
      </div>
      <div className="hidden md:block lg:hidden">
        <NavRail
          currentPath={currentPath}
          onNavigate={onNavigate}
          environment={environment}
          theme={theme}
          onToggleTheme={onToggleTheme}
          onLogout={onLogout}
        />
      </div>

      {/* 2. Main content container */}
      <div className="flex-1 flex flex-col h-screen overflow-hidden min-w-0">
        {/* Top Header Row with quick controls connected globally */}
        <Topbar
          environment={environment}
          onEnvironmentChange={onEnvironmentChange}
          period={period}
          onPeriodChange={onPeriodChange}
          onRefresh={onRefresh}
          isRefreshing={isRefreshing}
          onLogout={onLogout}
          onNavigate={onNavigate}
          theme={theme}
        />

        {/* 3. Main scrollable panel — padding inferior extra no mobile pra não
            ficar atrás do BottomNav fixo (80px + folga) */}
        <main
          className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8 pb-[calc(var(--bottom-nav-height)+16px)] md:pb-6 lg:pb-8 space-y-4 md:space-y-6 lg:space-y-8"
          style={{ backgroundColor: "var(--bg-content)" }}
        >
          {children}
        </main>
      </div>

      {/* 4. Bottom Nav — mobile only, fixed, fora do fluxo de scroll */}
      <BottomNav
        currentPath={currentPath}
        onNavigate={onNavigate}
        environment={environment}
        theme={theme}
        onToggleTheme={onToggleTheme}
        onLogout={onLogout}
      />
    </div>
  );
};
