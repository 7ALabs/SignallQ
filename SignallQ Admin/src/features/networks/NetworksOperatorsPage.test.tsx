import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { NetworksOperatorsPage } from "./NetworksOperatorsPage";

// GH#746 — smoke test: mocka o service diretamente (mesmo padrão de
// AiCostPage.test.tsx / DiagnosticsPage.test.tsx).
vi.mock("../../services/adminMetricsService", () => ({
  adminMetricsService: {
    getNetworkTypeStats: vi.fn().mockResolvedValue([
      { name: "Wi-Fi", count: 320, avgScore: 82, avgDownloadMbps: 90, avgUploadMbps: 30, avgLatencyMs: 24, avgJitterMs: 4, avgPacketLoss: 0.3 },
      { name: "Rede móvel", count: 110, avgScore: 68, avgDownloadMbps: 42, avgUploadMbps: 12, avgLatencyMs: 58, avgJitterMs: 9, avgPacketLoss: 1.1 },
    ]),
    getOperatorMetrics: vi.fn().mockResolvedValue([
      { id: "op_vivo", name: "Vivo", country: "Brasil", type: "4g", testCount: 210, averageDownloadMbps: 55, averageUploadMbps: 18, averageLatencyMs: 32, packetLossAverage: 0.6, averageScorePercentage: 79 },
      { id: "op_oi", name: "Oi", country: "Brasil", type: "4g", testCount: 90, averageDownloadMbps: 21, averageUploadMbps: 6, averageLatencyMs: 74, packetLossAverage: 2.4, averageScorePercentage: 48 },
    ]),
  },
}));

describe("NetworksOperatorsPage", () => {
  it("mostra o gráfico principal (score por operadora) sem os outros 3 gráficos empilhados", async () => {
    render(
      <NetworksOperatorsPage environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Score Médio de Diagnóstico por Operadora")).toBeInTheDocument();
    expect(screen.queryByText("Velocidade média de download por operadora")).not.toBeInTheDocument();
    expect(screen.queryByText("Latência média por tipo de rede")).not.toBeInTheDocument();
    expect(screen.queryByText("Perda de pacote média por tipo de rede")).not.toBeInTheDocument();
  });

  it("revela os gráficos secundários ao expandir o detalhamento", async () => {
    render(
      <NetworksOperatorsPage environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />
    );

    const toggle = await screen.findByText("Detalhamento por tipo de rede e operadora");
    fireEvent.click(toggle);

    expect(await screen.findByText("Velocidade média de download por operadora")).toBeInTheDocument();
    expect(screen.getByText("Latência média por tipo de rede")).toBeInTheDocument();
    expect(screen.getByText("Perda de pacote média por tipo de rede")).toBeInTheDocument();
  });

  it("aplica veredito consistente no KPI de operadora com pior score", async () => {
    render(
      <NetworksOperatorsPage environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Operadora com pior score")).toBeInTheDocument();
    // Oi tem score 48 -> abaixo de 55 -> "Fraco" pela mesma escala de scoreVerdict usada em Diagnósticos.
    expect(screen.getByText("Fraco")).toBeInTheDocument();
  });
});
