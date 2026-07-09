import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { NetworksOperatorsPage } from "./NetworksOperatorsPage";

// GH#781 (paridade mockup) — smoke test: mocka o service diretamente (mesmo
// padrão de AiCostPage.test.tsx / DiagnosticsPage.test.tsx).
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
  it("mostra os 4 KPIs de paridade com o mockup (networkKpis)", async () => {
    render(
      <NetworksOperatorsPage environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Score médio de rede")).toBeInTheDocument();
    expect(screen.getByText("Sessões via Wi-Fi")).toBeInTheDocument();
    expect(screen.getByText("Operadoras monitoradas")).toBeInTheDocument();
    expect(screen.getByText("Regiões cobertas")).toBeInTheDocument();

    // Operadoras monitoradas: 2 operadoras mockadas.
    expect(screen.getByText("2")).toBeInTheDocument();
    // Sessões via Wi-Fi: 320 Wi-Fi / (320 + 110) móvel = 74% — aparece duas vezes
    // por design do mockup (KPI "Sessões via Wi-Fi" + rodapé do card de operadoras).
    expect(screen.getAllByText("74%").length).toBe(2);
    // Regiões cobertas segue indisponível — sem coluna de UF no worker.
    expect(screen.getAllByText("Não disponível").length).toBeGreaterThanOrEqual(1);
  });

  it("não mostra filtro global nem blocos fora da composição do mockup", async () => {
    render(
      <NetworksOperatorsPage environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />
    );

    await screen.findByText("Score médio de rede");
    expect(screen.queryByText("Score Médio de Diagnóstico por Operadora")).not.toBeInTheDocument();
    expect(screen.queryByText("Detalhamento por tipo de rede e operadora")).not.toBeInTheDocument();
    expect(screen.queryByText("Estudo Técnico Comparativo de Conectividade")).not.toBeInTheDocument();
    expect(screen.queryByText("Exportar relatório por operadora")).not.toBeInTheDocument();
  });

  it("mostra o mapa por UF e as sessões por operadora na mesma linha", async () => {
    render(
      <NetworksOperatorsPage environment="production" period="7d" onNavigate={vi.fn()} triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Onde o app é mais usado")).toBeInTheDocument();
    expect(screen.getByText("Sessões por operadora")).toBeInTheDocument();
  });
});
