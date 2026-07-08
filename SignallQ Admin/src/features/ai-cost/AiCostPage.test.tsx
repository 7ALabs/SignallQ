import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { AiCostPage } from "./AiCostPage";

// GH#552 (Fase 3) — smoke test: mocka o service diretamente (ver nota em
// DiagnosticsPage.test.tsx sobre o modo mock global do apiClient em teste).
vi.mock("../../services/aiUsageService", () => ({
  aiUsageService: {
    getAiUsageMetrics: vi.fn().mockResolvedValue([
      { provider: "gemini_flash", displayName: "Gemini", totalCalls: 900, totalTokens: 4_200_000, averageLatencyMs: 0, estimatedCostUsd: 12.4, reliabilityPercentage: 98.2 },
      { provider: "cloudflare_qwen", displayName: "Qwen / Workers AI", totalCalls: 60, totalTokens: 180_000, averageLatencyMs: 0, estimatedCostUsd: 0, reliabilityPercentage: 91.5 },
    ]),
    getAiUsageTimeSeries: vi.fn().mockResolvedValue([
      { date: "2026-07-06", byProvider: { "Gemini": 500000, "Qwen / Workers AI": 20000 } },
      { date: "2026-07-07", byProvider: { "Gemini": 480000, "Qwen / Workers AI": 60000 } },
    ]),
    getAiUsageRecords: vi.fn().mockResolvedValue([]),
    getAiCostSummary: vi.fn().mockResolvedValue({
      totalCostUsd: "$12.40",
      totalRequests: "960",
      avgCostPerRequest: "$0.01",
      tokensSentM: "3.1M",
      tokensReceivedM: "1.3M",
      successRate: "97.4%",
      reliabilityPercentage: 97.4,
    }),
  },
}));

describe("AiCostPage", () => {
  it("mostra o estado de carregamento inicial", () => {
    render(<AiCostPage environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(screen.getByText(/Recuperando telemetria/i)).toBeInTheDocument();
  });

  it("carrega os KPIs de custo com veredito após o fetch", async () => {
    render(<AiCostPage environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("Custo no período")).toBeInTheDocument();
    expect(screen.getByText("Taxa de fallback")).toBeInTheDocument();
    expect(screen.getByText("Taxa de falha")).toBeInTheDocument();
  });

  it("renderiza a tabela de investigação (custo por provider)", async () => {
    render(<AiCostPage environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("Custo e Métricas Reais por Provedor")).toBeInTheDocument();
  });
});
