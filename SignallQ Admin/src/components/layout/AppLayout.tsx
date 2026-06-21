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
  children,
  id,
}) => {
  // Determine topbar page title dynamically
  const pageTitle = React.useMemo(() => {
    switch (currentPath) {
      case "/overview":
        return "Visão Geral do Sistema";
      case "/product-analytics":
        return "Análise de Engajamento & Uso";
      case "/diagnostics":
        return "Central de Diagnósticos Ativos";
      case "/networks":
        return "Análise de Frequências & RF";
      case "/operators":
        return "Benchmarks de Conectividade Internacional";
      case "/ai-cost":
        return "Demonstrativos de Custos de Orçamento IA";
      case "/errors":
        return "Painel de Registro de Crash de Sistema";
      case "/app-versions":
        return "Monitoramento de Rollouts Android";
      case "/settings":
        return "Parâmetros Técnicos de Configuração";
      default:
        return "SignallQ Admin Console";
    }
  }, [currentPath]);

  return (
    <div
      id={id || "app-layout-root"}
      className="flex w-full h-screen bg-[#08080A] text-[#F3F4F6] overflow-hidden select-none font-sans"
    >
      {/* 1. Left Sidebar Navigation Panel */}
      <Sidebar
        currentPath={currentPath}
        onNavigate={onNavigate}
        environment={environment}
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
        />

        {/* 3. Main scrollable panel */}
        <main className="flex-1 overflow-y-auto p-8 space-y-8 bg-[#08080A]">
          {children}
        </main>
      </div>
    </div>
  );
};
