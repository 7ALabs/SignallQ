import React from "react";
import { Sidebar } from "./Sidebar";
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
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = React.useState(false);

  const handleNavigate = React.useCallback((path: string) => {
    onNavigate(path);
    setIsMobileSidebarOpen(false);
  }, [onNavigate]);

  return (
    <div
      id={id || "app-layout-root"}
      className="flex w-full h-screen overflow-hidden select-none font-sans"
      style={{ backgroundColor: "var(--bg-base)", color: "var(--text-primary)" }}
    >
      {/* Mobile sidebar overlay */}
      {isMobileSidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/60 lg:hidden"
          onClick={() => setIsMobileSidebarOpen(false)}
          aria-hidden="true"
        />
      )}

      {/* 1. Left Sidebar Navigation Panel */}
      <Sidebar
        currentPath={currentPath}
        onNavigate={handleNavigate}
        environment={environment}
        isOpen={isMobileSidebarOpen}
        onClose={() => setIsMobileSidebarOpen(false)}
        theme={theme}
        onToggleTheme={onToggleTheme}
      />

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
          onNavigate={handleNavigate}
          onOpenMobileSidebar={() => setIsMobileSidebarOpen(true)}
          theme={theme}
        />

        {/* Staging warning banner — removido em 2026-07-16: o protótipo
            `md3-tobe` não tem banner equivalente, o chip PROD/STG segmentado
            do Topbar já comunica o ambiente ativo (ver
            FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md, item 4). */}

        {/* 3. Main scrollable panel */}
        <main className="flex-1 overflow-y-auto p-4 md:p-6 lg:p-8 space-y-4 md:space-y-6 lg:space-y-8" style={{ backgroundColor: "var(--bg-content)" }}>
          {children}
        </main>
      </div>
    </div>
  );
};
