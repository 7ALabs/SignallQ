import React, { useState, useEffect, useCallback } from "react";
import { useTheme } from "./hooks/useTheme";
import { AppLayout } from "./components/layout/AppLayout";
import { PageHeader } from "./components/layout/PageHeader";
import { AppEnvironment } from "./types/admin";
import { LoginPage } from "./auth/LoginPage";
import { apiClient } from "./services/apiClient";
import { ErrorBoundary } from "./components/ui/ErrorBoundary";
import { alpha } from "./utils/color";

// Tab/Feature Components
import { OverviewTab } from "./features/overview/OverviewTab";
import { ProductAnalyticsTab } from "./features/product-analytics/ProductAnalyticsTab";
import { DiagnosticsTab } from "./features/diagnostics/DiagnosticsTab";
import { NetworksTab } from "./features/networks/NetworksTab";
import { AiCostTab } from "./features/ai-cost/AiCostTab";
import { ErrorsTab } from "./features/errors/ErrorsTab";
import { VersionsTab } from "./features/app-versions/VersionsTab";
import { SettingsTab } from "./features/settings/SettingsTab";
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
          title: "O SignallQ está saudável agora?",
          description: "Acompanhe uso, diagnósticos, qualidade da rede e custo de IA em tempo real.",
          dataSource: "D1 (diagnostic_sessions, ai_usage, alerts) via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: alpha("var(--sq-accent)", 10),
                border: `1px solid ${alpha("var(--sq-accent)", 20)}`,
                color: "var(--sq-accent)",
              }}
            >
              <Sparkles className="w-3.5 h-3.5" /> Live Engine
            </span>
          ),
        };
      case "/product-analytics":
        return {
          title: "As pessoas estão usando o app como esperado?",
          description: "Monitore o engajamento de features, navegação, retenção, métricas de performance e monetização futura.",
          dataSource: "D1 (analytics_events) via signallq-admin-worker — ingest Android pendente (GH#417)",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: alpha("var(--sq-accent)", 10),
                border: `1px solid ${alpha("var(--sq-accent)", 20)}`,
                color: "var(--sq-accent)",
              }}
            >
              <Sparkles className="w-3.5 h-3.5" /> Product Insights
            </span>
          ),
        };
      case "/diagnostics":
        return {
          title: "O que os usuários estão medindo?",
          description: "Volume e composição dos diagnósticos por tipo de rede, com drill-down por sessão individual.",
          dataSource: "D1 (diagnostic_sessions) via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: alpha("var(--sq-success)", 10),
                border: `1px solid ${alpha("var(--sq-success)", 20)}`,
                color: "var(--sq-success)",
              }}
            >
              <Activity className="w-3.5 h-3.5 animate-pulse" /> Telemetria de Rádio
            </span>
          ),
        };
      // GH#552 (Fase 2): "Redes & Provedores" — fusão de /networks + /operators,
      // mesmo texto de cabeçalho nos dois hashes.
      case "/networks":
      case "/operators":
        return {
          title: "Onde a qualidade varia?",
          description: "Visibilidade agregada sobre frequências celulares de rádio, canais Wi-Fi residenciais e benchmark comparativo entre operadoras.",
          dataSource: "D1 (diagnostic_sessions agregado por network_type e por operator) via signallq-admin-worker",
          badge: null,
        };
      case "/ai-cost":
        return {
          title: "A IA está entregando valor com custo controlado?",
          description: "Custo por provider, latência, taxa de fallback e falha, com auditoria de sessões outlier.",
          dataSource: "D1 (ai_usage) via signallq-admin-worker",
          badge: null,
        };
      case "/errors":
        return {
          title: "O que está prejudicando a experiência?",
          description: "Erros do Cloudflare Worker, dumps de depuração do app Android e problemas nas conexões com tabelas de banco Analytics DB.",
          dataSource: "D1 (system_errors) via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg uppercase"
              style={{
                backgroundColor: alpha("var(--sq-error)", 10),
                border: `1px solid ${alpha("var(--sq-error)", 20)}`,
                color: "var(--sq-error)",
              }}
            >
              <AlertTriangle className="w-3.5 h-3.5" /> Auditoria de Erros
            </span>
          ),
        };
      case "/app-versions":
        return {
          title: "A versão publicada está estável?",
          description: "Lista de builds Android transmitindo métricas e status de distribuição de novas atualizações via Play Store.",
          dataSource: "D1 (diagnostic_sessions por versão) + BigQuery (export Firebase Crashlytics) via signallq-admin-worker",
          badge: null,
        };
      case "/system-health":
        return {
          title: "A infraestrutura está funcionando?",
          description: "Status dos Workers Cloudflare, D1 Database e alertas de threshold para crash rate e custo de IA.",
          dataSource: "D1 + Firebase + BigQuery — checagem de conectividade via signallq-admin-worker",
          badge: (
            <span
              className="flex items-center gap-1.5 px-2 py-0.5 text-[10px] font-mono rounded-lg"
              style={{
                backgroundColor: alpha("var(--sq-success)", 10),
                border: `1px solid ${alpha("var(--sq-success)", 20)}`,
                color: "var(--sq-success)",
              }}
            >
              <HeartPulse className="w-3.5 h-3.5" /> System Monitor
            </span>
          ),
        };
      // GH#552 (Fase 2): "Configurações" — fusão de /settings + /feature-flags,
      // mesmo texto de cabeçalho nos dois hashes.
      case "/settings":
      case "/feature-flags":
        return {
          title: "O que posso controlar com segurança?",
          description: "Feature flags remotas, limiares de alerta consumidos pelo worker e integrações administrativas externas.",
          dataSource: "D1 (admin_settings, feature_flags, feature_flag_audit) via signallq-admin-worker",
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
          onPeriodChange={handlePeriodChange}
          onNavigate={handleNavigate}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/product-analytics" && (
        <ProductAnalyticsTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
          onNavigate={handleNavigate}
        />
      )}
      {currentPath === "/diagnostics" && (
        <DiagnosticsTab
          environment={environment}
          period={period}
          onEnvironmentChange={handleEnvironmentChange}
          onPeriodChange={handlePeriodChange}
          triggerRefreshCounter={refreshCounter}
          onNavigate={handleNavigate}
        />
      )}
      {/* GH#552 (Fase 2): "/operators" fundido em "/networks" — mesmo componente
          renderizado nos dois hashes até que nada mais linke pro slug antigo. */}
      {(currentPath === "/networks" || currentPath === "/operators") && (
        <NetworksTab
          environment={environment}
          period={period}
          onNavigate={handleNavigate}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/ai-cost" && (
        <AiCostTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
          onNavigate={handleNavigate}
        />
      )}
      {currentPath === "/errors" && (
        <ErrorsTab
          environment={environment}
          period={period}
          onNavigate={handleNavigate}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/app-versions" && (
        <VersionsTab
          environment={environment}
          period={period}
          onNavigate={handleNavigate}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      {currentPath === "/system-health" && (
        <SystemHealthTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
          onNavigate={handleNavigate}
        />
      )}
      {/* GH#552 (Fase 2): "/feature-flags" fundido em "/settings" — mesmo componente
          renderizado nos dois hashes até que nada mais linke pro slug antigo. */}
      {(currentPath === "/settings" || currentPath === "/feature-flags") && <SettingsTab />}
      </ErrorBoundary>
    </AppLayout>
  );
}
