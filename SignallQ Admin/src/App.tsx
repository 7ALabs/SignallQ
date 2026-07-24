import { useState, useEffect, useCallback } from "react";
import { useTheme } from "./hooks/useTheme";
import { AppLayout } from "./components/layout/AppLayout";
import { AppEnvironment } from "./types/admin";
import { LoginPage } from "./auth/LoginPage";
import { apiClient } from "./services/apiClient";
import { ErrorBoundary } from "./components/ui/ErrorBoundary";

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
import { ToolsTab } from "./features/tools/ToolsTab";
import { GooglePlayTab } from "./features/google-play/GooglePlayTab";

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
  // Functional update `prev => autenticado || prev` impede que um fetch obsoleto
  // sobrescreva um isAuthenticated=true definido por um login explícito.
  //
  // GH#1054 — só `r.ok` (status 2xx) não basta: em dev local sem VITE_ADMIN_API_BASE_URL
  // configurada, `baseUrl` fica vazio, o fetch vira same-origin e o fallback SPA do Vite
  // responde 200 text/html pra qualquer rota não mapeada (inclusive /admin/auth/me),
  // autenticando sem nenhuma credencial real. Confere content-type JSON e a forma real
  // do corpo (`{ email, role }`, ver handleAuthMe no signallq-admin-worker) antes de
  // considerar autenticado.
  useEffect(() => {
    let cancelled = false;
    const controller = typeof AbortController !== "undefined" ? new AbortController() : null;

    fetch(`${baseUrl}/admin/auth/me`, {
      credentials: "include",
      ...(controller ? { signal: controller.signal } : {}),
    })
      .then(async (r) => {
        if (cancelled) return;
        const contentType = r.headers.get("content-type") ?? "";
        if (!r.ok || !contentType.includes("application/json")) {
          setIsAuthenticated((prev) => prev);
          return;
        }
        const body = await r.json().catch(() => null);
        const autenticado = typeof body?.email === "string" && body.email.length > 0;
        setIsAuthenticated((prev) => autenticado || prev);
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
        "/google-play",
        "/ai-cost",
        "/errors",
        "/app-versions",
        "/feature-flags",
        "/system-health",
        "/settings",
        "/tools",
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
      {currentPath === "/google-play" && (
        <GooglePlayTab
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
      {currentPath === "/tools" && (
        <ToolsTab
          environment={environment}
          period={period}
          triggerRefreshCounter={refreshCounter}
        />
      )}
      </ErrorBoundary>
    </AppLayout>
  );
}
