import React, { useState, useEffect, useCallback } from "react";
import { AppLayout } from "./components/layout/AppLayout";
import { PageHeader } from "./components/layout/PageHeader";
import { AppEnvironment } from "./types/admin";
import { LoginPage } from "./auth/LoginPage";
import { apiClient } from "./services/apiClient";

// Tab/Feature Components
import { OverviewTab } from "./features/overview/OverviewTab";
import { ProductAnalyticsTab } from "./features/product-analytics/ProductAnalyticsTab";
import { DiagnosticsTab } from "./features/diagnostics/DiagnosticsTab";
import { NetworksTab } from "./features/networks/NetworksTab";
import { OperatorsTab } from "./features/operators/OperatorsTab";
import { AiCostTab } from "./features/ai-cost/AiCostTab";
import { ErrorsTab } from "./features/errors/ErrorsTab";
import { VersionsTab } from "./features/app-versions/VersionsTab";
import { SettingsTab } from "./features/settings/SettingsTab";

// Lucide accessories
import { Sparkles, Activity, AlertTriangle } from "lucide-react";

export default function App() {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(
    () => !!apiClient.getToken()
  );
  const [currentPath, setCurrentPath] = useState<string>("/overview");
  const [environment, setEnvironment] = useState<AppEnvironment>("production");
  const [period, setPeriod] = useState<string>("7d");
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [refreshCounter, setRefreshCounter] = useState<number>(0);

  const handleLogin = useCallback((token: string) => {
    localStorage.setItem("signallq_admin_token", token);
    apiClient.setToken(token);
    setIsAuthenticated(true);
  }, []);

  const handleLogout = useCallback(() => {
    localStorage.removeItem("signallq_admin_token");
    apiClient.setToken(null);
    setIsAuthenticated(false);
  }, []);

  useEffect(() => {
    apiClient.onAuthError(handleLogout);
  }, [handleLogout]);

  // Hash Routing Synchronizer for a native visual feel
  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.replace("#", "");
      const validPaths = [
        "/overview",
        "/product-analytics",
        "/diagnostics",
        "/networks",
        "/operators",
        "/ai-cost",
        "/errors",
        "/app-versions",
        "/settings",
      ];
      if (hash && validPaths.includes(hash)) {
        setCurrentPath(hash);
      }
    };

    if (!window.location.hash) {
      window.location.hash = "#/overview";
    } else {
      handleHashChange();
    }

    window.addEventListener("hashchange", handleHashChange);
    return () => window.removeEventListener("hashchange", handleHashChange);
  }, []);

  const handleNavigate = useCallback((path: string) => {
    window.location.hash = `#${path}`;
    setCurrentPath(path);
  }, []);

  const handleEnvironmentChange = useCallback((env: AppEnvironment) => {
    setEnvironment(env);
  }, []);

  const handlePeriodChange = useCallback((p: string) => {
    setPeriod(p);
  }, []);

  // Globally synchronized refresh simulator
  const handleRefresh = useCallback(async () => {
    setIsRefreshing(true);
    // Simulate real network synchronization latency (350ms)
    await new Promise((resolve) => setTimeout(resolve, 350));
    setRefreshCounter((prev) => prev + 1);
    setIsRefreshing(false);
  }, []);

  // Export CSV simulated action
  const handleExport = useCallback(() => {
    const csvContent = "data:text/csv;charset=utf-8,ID,Timestamp,Model,Status,Latency,Download,Upload\r\n";
    const encodedUri = encodeURI(csvContent);
    const link = document.createElement("a");
    link.setAttribute("href", encodedUri);
    link.setAttribute("download", `signallq_telemetry_${environment}_${period}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }, [environment, period]);

  // Determine page header detail and descriptive subtitles
  const pageHeaderDetail = React.useMemo(() => {
    switch (currentPath) {
      case "/overview":
        return {
          title: "Visão Geral",
          description: "Acompanhe uso, diagnósticos, qualidade da rede e custo de IA em tempo real.",
          badge: (
            <span className="flex items-center gap-1.5 px-2 py-0.5 bg-indigo-950/40 border border-indigo-500/20 text-[#6C2BFF] text-[10px] font-mono rounded-lg">
              <Sparkles className="w-3.5 h-3.5" /> Live Engine
            </span>
          ),
        };
      case "/product-analytics":
        return {
          title: "Produto & Uso",
          description: "Monitore o engajamento de features, navegação, retenção, métricas de performance e monetização futura.",
          badge: (
            <span className="flex items-center gap-1.5 px-2 py-0.5 bg-purple-950/40 border border-purple-500/20 text-[#A855F7] text-[10px] font-mono rounded-lg">
              <Sparkles className="w-3.5 h-3.5" /> Product Insights
            </span>
          ),
        };
      case "/diagnostics":
        return {
          title: "Sessões de Diagnósticos",
          description: "Pesquise por logs brutos, meça o bufferbloat local e emita diagnósticos preditivos baseados em IA.",
          badge: (
            <span className="flex items-center gap-1.5 px-2 py-0.5 bg-emerald-950/40 border border-emerald-500/20 text-emerald-400 text-[10px] font-mono rounded-lg">
              <Activity className="w-3.5 h-3.5 animate-pulse" /> Telemetria de Rádio
            </span>
          ),
        };
      case "/networks":
        return {
          title: "Análise de Redes",
          description: "Visibilidade agregada sobre frequências celulares de rádio, canais Wi-Fi residenciais e gateways atenuados.",
          badge: null,
        };
      case "/operators":
        return {
          title: "Benchmarks de Operadoras",
          description: "Análise comparativa das latências e perdas de pacotes entre as principais operadoras de telecomunicações do Brasil.",
          badge: null,
        };
      case "/ai-cost":
        return {
          title: "IA & Custo de Telemetria",
          description: "Demonstrativos de tokens processados, custos previstos da API do Gemini e tempos médios de resposta de pareceres.",
          badge: null,
        };
      case "/errors":
        return {
          title: "Logs de Erros",
          description: "Erros do Cloudflare Worker, dumps de depuração do app Android e problemas nas conexões com tabelas de banco Analytics DB.",
          badge: (
            <span className="flex items-center gap-1.5 px-2 py-0.5 bg-red-950/40 border border-red-500/20 text-red-500 text-[10px] font-mono rounded-lg uppercase">
              <AlertTriangle className="w-3.5 h-3.5" /> Auditoria de Erros
            </span>
          ),
        };
      case "/app-versions":
        return {
          title: "Versões do App",
          description: "Lista de builds Android transmitindo métricas e status de distribuição de novas atualizações via Play Store.",
          badge: null,
        };
      case "/settings":
        return {
          title: "Configurações Técnicas",
          description: "Ajustes de model gateway, limiares de canais de rádio, webhooks de alerta e pontos de sincronização de dados.",
          badge: null,
        };
      default:
        return {
          title: "SignallQ Admin",
          description: "Plataforma administrativa móvel",
          badge: null,
        };
    }
  }, [currentPath]);

  if (!isAuthenticated) {
    return <LoginPage onLogin={handleLogin} />;
  }

  return (
    <AppLayout
      currentPath={currentPath}
      onNavigate={handleNavigate}
      environment={environment}
      onEnvironmentChange={handleEnvironmentChange}
      period={period}
      onPeriodChange={handlePeriodChange}
      onRefresh={handleRefresh}
      isRefreshing={isRefreshing}
      onLogout={handleLogout}
    >
      {/* Dynamic Sub-header */}
      <PageHeader
        title={pageHeaderDetail.title}
        description={pageHeaderDetail.description}
        badge={pageHeaderDetail.badge}
      />

      {/* Structured Switch Rendering the Active tab dynamically */}
      {currentPath === "/overview" && (
        <OverviewTab
          environment={environment}
          period={period}
          onNavigate={handleNavigate}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/product-analytics" && (
        <ProductAnalyticsTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/diagnostics" && (
        <DiagnosticsTab
          environment={environment}
          period={period}
          onEnvironmentChange={handleEnvironmentChange}
          onPeriodChange={handlePeriodChange}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/networks" && (
        <NetworksTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/operators" && (
        <OperatorsTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/ai-cost" && (
        <AiCostTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/errors" && (
        <ErrorsTab
          environment={environment}
          period={period}
          onEnvironmentChange={handleEnvironmentChange}
          onPeriodChange={handlePeriodChange}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/app-versions" && (
        <VersionsTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/settings" && (
        <SettingsTab
          triggerRefreshCounter={refreshCounter}
          onRefreshTrigger={handleRefresh}
        />
      )}
    </AppLayout>
  );
}
