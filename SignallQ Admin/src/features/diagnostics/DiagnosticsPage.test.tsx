import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { DiagnosticsPage } from "./DiagnosticsPage";

// GH#552 (Fase 3) — smoke test: mocka o service diretamente (em vez de depender
// do modo mock global do apiClient, que é resolvido em tempo de build pelo Vite
// via .env.local e não é sobrescrito de forma confiável em runtime de teste).
vi.mock("../../services/diagnosticsService", () => ({
  diagnosticsService: {
    getDiagnosticSessions: vi.fn().mockResolvedValue([]),
    getDiagnosticsSummary: vi.fn().mockResolvedValue({
      totalTests: 3941,
      criticalIssuesCount: 12,
      attentionIssuesCount: 40,
      averageDownloadMbps: 62,
      averageUploadMbps: 18,
      averageLatencyMs: 34,
      averageScore: 78,
      averageJitterMs: 9,
      averagePacketLossPercentage: 0.8,
      issueDistribution: {},
    }),
    getAggregateDiagnostics: vi.fn().mockResolvedValue([
      { networkType: "Wi-Fi", diagnosticsCount: 2400, avgScore: 78, avgDownload: "60 Mbps", avgUpload: "-", avgPing: "30 ms", avgJitter: "-", avgLoss: "-", topIssue: "-", trend: "stable", trendLabel: "61%" },
      { networkType: "Móvel", diagnosticsCount: 1541, avgScore: 68, avgDownload: "40 Mbps", avgUpload: "-", avgPing: "48 ms", avgJitter: "-", avgLoss: "-", topIssue: "-", trend: "up", trendLabel: "39%" },
    ]),
    triggerReDiagnosis: vi.fn().mockResolvedValue({ success: false, message: "não disponível" }),
  },
}));

describe("DiagnosticsPage", () => {
  it("renderiza sem crash e mostra os filtros de busca", () => {
    render(
      <DiagnosticsPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(
      screen.getByPlaceholderText(/Pesquise por dispositivo, ID da sessão/i)
    ).toBeInTheDocument();
  });

  it("carrega os KPIs com veredito após o fetch", async () => {
    render(
      <DiagnosticsPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Total de diagnósticos")).toBeInTheDocument();
    expect(screen.getByText("Score médio de qualidade")).toBeInTheDocument();
    expect(screen.getByText("Tipo de rede mais comum")).toBeInTheDocument();
  });

  it("renderiza a tabela de investigação (agregação por tipo de rede)", async () => {
    render(
      <DiagnosticsPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Análise Agregada de Diagnósticos")).toBeInTheDocument();
    expect(await screen.findByText("Wi-Fi")).toBeInTheDocument();
  });
});
