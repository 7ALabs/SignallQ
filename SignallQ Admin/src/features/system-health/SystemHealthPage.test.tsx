import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { SystemHealthPage } from "./SystemHealthPage";

// GH#552 (Fase 3) — smoke test: mocka o service diretamente (ver nota em
// DiagnosticsPage.test.tsx sobre o modo mock global do apiClient em teste).
vi.mock("../../services/systemHealthService", () => ({
  systemHealthService: {
    getSystemHealth: vi.fn().mockResolvedValue({
      data: {
        source: "worker",
        timestamp: new Date().toISOString(),
        checks: {
          worker: { status: "ok" },
          d1: { status: "ok", latencyMs: 38 },
          firebaseCredentials: { status: "ok", latencyMs: 210 },
          bigQuery: { status: "not_configured", message: "Requer credenciais Firebase válidas." },
          ingest: { status: "ok", keyConfigured: true, lastSuccessAt: new Date().toISOString() },
        },
        lastFailure: null,
        lastSuccess: { source: "ingest", timestamp: new Date().toISOString() },
      },
      clientLatencyMs: 120,
    }),
  },
}));

describe("SystemHealthPage", () => {
  it("renderiza os KPIs de infraestrutura", () => {
    render(
      <SystemHealthPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(screen.getByText("Worker Admin")).toBeInTheDocument();
    expect(screen.getAllByText("D1 Database").length).toBeGreaterThan(0);
    expect(screen.getByText("Credenciais Firebase")).toBeInTheDocument();
    expect(screen.getByText("Acesso BigQuery")).toBeInTheDocument();
  });

  it("carrega a tabela de serviços monitorados após o fetch", async () => {
    render(
      <SystemHealthPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(await screen.findByText("Serviços monitorados")).toBeInTheDocument();
  });

  it("mostra o bloco de explicação com o resultado do check", async () => {
    render(
      <SystemHealthPage environment="production" period="7d" triggerRefreshCounter={0} />
    );

    expect(
      await screen.findByText(/respondem normalmente/i)
    ).toBeInTheDocument();
  });
});
