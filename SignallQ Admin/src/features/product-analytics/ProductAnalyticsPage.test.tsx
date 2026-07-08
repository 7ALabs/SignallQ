import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ProductAnalyticsPage } from "./ProductAnalyticsPage";

// GH#552 (Fase 3) — smoke test: mocka o service diretamente (ver nota em
// DiagnosticsPage.test.tsx sobre o modo mock global do apiClient em teste).
vi.mock("../../services/productAnalyticsService", () => ({
  productAnalyticsService: {
    getFeatureUsage: vi.fn().mockResolvedValue([
      { feature: "diagnosis", label: "Diagnóstico", usageCount: 4200, uniqueUsers: 1800, completionRate: 0.82, failureRate: 0.05, avgDurationMs: 8400, trendPercent: 4 },
      { feature: "speedtest", label: "SpeedTest", usageCount: 6100, uniqueUsers: 2400, completionRate: 0.93, failureRate: 0.02, avgDurationMs: 5200, trendPercent: 8 },
    ]),
    getScreenNavigation: vi.fn().mockResolvedValue([]),
    getFeatureCrashes: vi.fn().mockResolvedValue([]),
    getRetention: vi.fn().mockResolvedValue([
      { cohort: "device_id", cohortSize: 1200, day1: 31, day7: 14, day30: 6, avgInstalledDays: 18, uninstallRate: 22 },
    ]),
    getSessionDuration: vi.fn().mockResolvedValue({ avgDurationMs: 187000, sessionCount: 6200 }),
  },
}));

describe("ProductAnalyticsPage", () => {
  it("mostra o estado de carregamento inicial", () => {
    render(
      <ProductAnalyticsPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(screen.getByText(/Carregando dados de produto/i)).toBeInTheDocument();
  });

  it("carrega KPIs de retenção com veredito após o fetch", async () => {
    render(
      <ProductAnalyticsPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Retenção D1")).toBeInTheDocument();
    expect(screen.getByText("Duração média de sessão")).toBeInTheDocument();
  });

  it("renderiza a tabela de investigação (engajamento por função)", async () => {
    render(
      <ProductAnalyticsPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Engajamento por Funcionalidade")).toBeInTheDocument();
  });
});
