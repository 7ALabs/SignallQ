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
  children,
  id,
}) => {
  const [isMobileSidebarOpen, setIsMobileSidebarOpen] = React.useState(false);

  // Determine topbar page title dynamically
  const pageTitle = React.useMemo(() => {
    switch (currentPath) {
      case "/overview":
        return "Visão Geral";
      case "/product-analytics":
        return "Engajamento & Uso";
      case "/diagnostics":
        return "Diagnósticos";
      case "/networks":
        return "Frequências & RF";
      case "/operators":
        return "Benchmarks";
      case "/ai-cost":
        return "Custos IA";
      case "/errors":
        return "Erros";
      case "/app-versions":
        return "Versões Android";
      case "/settings":
        return "Configurações";
      default:
        return "SignallQ Admin";
    }
  }, [currentPath]);

  const handleNavigate = React.useCallback((path: string) => {
    onNavigate(path);
    setIsMobileSidebarOpen(false);
  }, [onNavigate]);

  return (
    <div
      id={id || "app-layout-root"}
      className="flex w-full h-screen overflow-hidden select-none font-sans"
      style={{ backgroundColor: "var(--sq-bg-primary)", color: "var(--sq-text-primary)" }}
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
      />

      {/* 2. Main content container */}
      <div className="flex-1 flex flex-col h-screen overflow-hidden min-w-0">
        {/* Top Header Row with quick controls connected globally */}
        <Topbar
          title={pageTitle}
          environment={environment}
          onEnvironmentChange={onEnvironmentChange}
          period={period}
          onPeriodChange={onPeriodChange}
          onRefresh={onRefresh}
          isRefreshing={isRefreshing}
          onLogout={onLogout}
          onOpenMobileSidebar={() => setIsMobileSidebarOpen(true)}
        />

        {/* Staging warning banner */}
        {environment === "staging" && (
          <div className="bg-amber-500/10 border-b border-amber-500/20 px-6 py-1.5 text-amber-400 text-xs text-center">
            Modo Homologacao — dados de staging. Alterne para Producao para ver dados reais.
          </div>
        )}

        {/* 3. Main scrollable panel */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-8 space-y-6 lg:space-y-8" style={{ backgroundColor: "var(--sq-bg-primary)" }}>
          {children}
        </main>
      </div>
    </div>
  );
};
