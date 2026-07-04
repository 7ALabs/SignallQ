import React, { useState, useEffect, useCallback } from "react";
import { useTheme } from "./hooks/useTheme";
import { AppLayout } from "./components/layout/AppLayout";
import { PageHeader } from "./components/layout/PageHeader";
import { AppEnvironment } from "./types/admin";
import { LoginPage } from "./auth/LoginPage";
import { apiClient } from "./services/apiClient";
import { ErrorBoundary } from "./components/ui/ErrorBoundary";

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
import { FeatureFlagsTab } from "./features/feature-flags/FeatureFlagsTab";
import { SystemHealthTab } from "./features/system-health/SystemHealthTab";

// Lucide accessories
import { Sparkles, Activity, AlertTriangle, HeartPulse } from "lucide-react";

export default function App() {
  const { theme, toggle: onToggleTheme } = useTheme();
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [authChecked, setAuthChecked] = useState<boolean>(false);
  const [currentPath, setCurrentPath] = useState<string>("/overview");
  const [environment, setEnvironment] = useState<AppEnvironment>(() => {
    const saved = sessionStorage.getItem("signallq_env_filter");
    return (saved as AppEnvironment) ?? "production";
  });
  const [period, setPeriod] = useState<string>(
    () => sessionStorage.getItem("signallq_period_filter") ?? "7d"
  );
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [refreshCounter, setRefreshCounter] = useState<number>(0);

  const baseUrl = import.meta.env.VITE_ADMIN_API_BASE_URL ?? "";

  // SIG-136: verifica sessão via cookie httpOnly na montagem.
  // AbortController garante que o fetch seja cancelado no cleanup (evita atualização
  // de estado em componente desmontado e race condition do StrictMode).
  // Functional update `prev => r.ok || prev` impede que um fetch obsoleto
  // sobrescreva um isAuthenticated=true definido por um login explícito.
  useEffect(() => {
    let cancelled = false;
    const controller = typeof AbortController !== "undefined" ? new AbortController() : null;

    fetch(`${baseUrl}/admin/auth/me`, {
      credentials: "include",
      ...(controller ? { signal: controller.signal } : {}),
    })
      .then((r) => {
        if (!cancelled) setIsAuthenticated((prev) => r.ok || prev);
      })
      .catch((e) => {
        if (!cancelled && e.name !== "AbortError") setIsAuthenticated(false);
      })
      .finally(() => {
        if (!cancelled) setAuthChecked(true);
      });

    return () => {
      cancelled = true;
      if (controller) controller.abort();
    };
  }, [baseUrl]);

  const handleLogin = useCallback(() => {
    setIsAuthenticated(true);
  }, []);

  const handleLogout = useCallback(() => {
    fetch(`${baseUrl}/admin/auth/logout`, { method: "POST", credentials: "include" })
      .finally(() => setIsAuthenticated(false));
  }, [baseUrl]);

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
        "/feature-flags",
        "/system-health",
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
    sessionStorage.setItem("signallq_env_filter", env);
    setEnvironment(env);
  }, []);

  const handlePeriodChange = useCallback((p: string) => {
    sessionStorage.setItem("signallq_period_filter", p);
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
          dataSource: "D1 (diagnostic_sessions, ai_usage, alerts) via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: "color-mix(in srgb, var(--sq-accent) 10%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-accent) 20%, transparent)",
                color: "var(--sq-accent)",
              }}
            >
              <Sparkles className="w-3.5 h-3.5" /> Live Engine
            </span>
          ),
        };
      case "/product-analytics":
        return {
          title: "Produto & Uso",
          description: "Monitore o engajamento de features, navegação, retenção, métricas de performance e monetização futura.",
          dataSource: "D1 (analytics_events) via signallq-admin-worker — ingest Android pendente (GH#417)",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: "color-mix(in srgb, var(--sq-accent) 10%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-accent) 20%, transparent)",
                color: "var(--sq-accent)",
              }}
            >
              <Sparkles className="w-3.5 h-3.5" /> Product Insights
            </span>
          ),
        };
      case "/diagnostics":
        return {
          title: "Sessões de Diagnósticos",
          description: "Pesquise por logs brutos, meça o bufferbloat local e emita diagnósticos preditivos baseados em IA.",
          dataSource: "D1 (diagnostic_sessions) via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: "color-mix(in srgb, var(--sq-success) 10%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-success) 20%, transparent)",
                color: "var(--sq-success)",
              }}
            >
              <Activity className="w-3.5 h-3.5 animate-pulse" /> Telemetria de Rádio
            </span>
          ),
        };
      case "/networks":
        return {
          title: "Análise de Redes",
          description: "Visibilidade agregada sobre frequências celulares de rádio, canais Wi-Fi residenciais e gateways atenuados.",
          dataSource: "D1 (diagnostic_sessions agregado por network_type) via signallq-admin-worker",
          badge: null,
        };
      case "/operators":
        return {
          title: "Benchmarks de Operadoras",
          description: "Análise comparativa das latências e perdas de pacotes entre as principais operadoras de telecomunicações do Brasil.",
          dataSource: "D1 (diagnostic_sessions agregado por operator) via signallq-admin-worker",
          badge: null,
        };
      case "/ai-cost":
        return {
          title: "IA & Custo de Telemetria",
          description: "Demonstrativos de tokens processados, custos previstos da API do Gemini e tempos médios de resposta de pareceres.",
          dataSource: "D1 (ai_usage) via signallq-admin-worker",
          badge: null,
        };
      case "/errors":
        return {
          title: "Logs de Erros",
          description: "Erros do Cloudflare Worker, dumps de depuração do app Android e problemas nas conexões com tabelas de banco Analytics DB.",
          dataSource: "D1 (system_errors) via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg uppercase"
              style={{
                backgroundColor: "color-mix(in srgb, var(--sq-error) 10%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-error) 20%, transparent)",
                color: "var(--sq-error)",
              }}
            >
              <AlertTriangle className="w-3.5 h-3.5" /> Auditoria de Erros
            </span>
          ),
        };
      case "/app-versions":
        return {
          title: "Versões do App",
          description: "Lista de builds Android transmitindo métricas e status de distribuição de novas atualizações via Play Store.",
          dataSource: "BigQuery (export Firebase Crashlytics) via signallq-admin-worker",
          badge: null,
        };
      case "/feature-flags":
        return {
          title: "Feature Flags",
          description: "Ative ou desative telas e funcionalidades do app remotamente, sem necessidade de novo build.",
          dataSource: "D1 (feature_flags, feature_flag_audit) via signallq-admin-worker",
          badge: null,
        };
      case "/system-health":
        return {
          title: "Saúde do Sistema",
          description: "Status dos Workers Cloudflare, D1 Database e alertas de threshold para crash rate e custo de IA.",
          dataSource: "D1 + Firebase + BigQuery — checagem de conectividade via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: "color-mix(in srgb, var(--sq-success) 10%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-success) 20%, transparent)",
                color: "var(--sq-success)",
              }}
            >
              <HeartPulse className="w-3.5 h-3.5" /> System Monitor
            </span>
          ),
        };
      case "/settings":
        return {
          title: "Configurações Técnicas",
          description: "Ajustes de model gateway, limiares de canais de rádio, webhooks de alerta e pontos de sincronização de dados.",
          dataSource: "D1 (admin_settings) via signallq-admin-worker",
          badge: null,
        };
      default:
        return {
          title: "7Agents Admin Console",
          description: "Plataforma administrativa móvel",
          badge: null,
        };
    }
  }, [currentPath]);

  // Aguarda verificação inicial para não piscar a tela de login desnecessariamente.
  if (!authChecked) {
    return (
      <div
        className="min-h-screen flex items-center justify-center"
        style={{ backgroundColor: "var(--sq-bg-primary)" }}
      >
        <div
          className="w-6 h-6 border-2 border-t-transparent rounded-full animate-spin"
          style={{ borderColor: "var(--sq-accent)", borderTopColor: "transparent" }}
        />
      </div>
    );
  }

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
      theme={theme}
      onToggleTheme={onToggleTheme}
    >
      {/* Dynamic Sub-header */}
      <PageHeader
        title={pageHeaderDetail.title}
        description={pageHeaderDetail.description}
        dataSource={pageHeaderDetail.dataSource}
        badge={pageHeaderDetail.badge}
      />

      {/* Structured Switch Rendering the Active tab dynamically.
          ErrorBoundary com key={currentPath} isola crashes por aba e reseta ao navegar. */}
      <ErrorBoundary key={currentPath}>
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
      {currentPath === "/feature-flags" && <FeatureFlagsTab />}
      {currentPath === "/system-health" && (
        <SystemHealthTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/settings" && <SettingsTab />}
      </ErrorBoundary>
    </AppLayout>
  );
}
