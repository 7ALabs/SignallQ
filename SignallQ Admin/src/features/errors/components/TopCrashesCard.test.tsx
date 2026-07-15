import { describe, it, expect, vi, afterEach } from "vitest";
import { render, screen } from "@testing-library/react";
import { TopCrashesCard } from "./TopCrashesCard";
import { integrationsService } from "../../../integrations/integrationsService";

vi.mock("../../../integrations/integrationsService", () => ({
  integrationsService: {
    getFirebaseIssues: vi.fn(),
  },
}));

const getFirebaseIssuesMock = integrationsService.getFirebaseIssues as unknown as ReturnType<typeof vi.fn>;

describe("TopCrashesCard", () => {
  afterEach(() => {
    getFirebaseIssuesMock.mockReset();
  });

  it("renderiza a tabela com dado real quando source é bigquery", async () => {
    getFirebaseIssuesMock.mockResolvedValue({
      source: "bigquery",
      issues: [
        { id: "1", title: "java.net.SocketTimeoutException", totalCrashes: 410, affectedUsers: 84, lastSeen: Date.now(), appVersion: "0.16.0" },
      ],
    });

    render(<TopCrashesCard environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("java.net.SocketTimeoutException")).toBeInTheDocument();
    expect(screen.getByText("410")).toBeInTheDocument();
    expect(screen.getByText("84")).toBeInTheDocument();
    expect(screen.getByText("0.16.0")).toBeInTheDocument();
    expect(screen.getByText("Crítico")).toBeInTheDocument();
  });

  it("mostra '-' quando appVersion não vem do worker", async () => {
    getFirebaseIssuesMock.mockResolvedValue({
      source: "bigquery",
      issues: [{ id: "1", title: "NullPointerException", totalCrashes: 5, affectedUsers: 2, lastSeen: 0 }],
    });

    render(<TopCrashesCard environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("NullPointerException")).toBeInTheDocument();
    expect(screen.getAllByText("-").length).toBeGreaterThanOrEqual(1);
  });

  it("mostra estado vazio honesto quando source é no_data_yet", async () => {
    getFirebaseIssuesMock.mockResolvedValue({ source: "no_data_yet", issues: [] });

    render(<TopCrashesCard environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("Sem crashes registrados no período")).toBeInTheDocument();
  });

  it("mostra estado vazio honesto quando não há credenciais", async () => {
    getFirebaseIssuesMock.mockResolvedValue({ source: "no_credentials", issues: [] });

    render(<TopCrashesCard environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("Firebase não configurado no Admin Worker")).toBeInTheDocument();
  });

  it("mostra erro quando a chamada falha", async () => {
    getFirebaseIssuesMock.mockRejectedValue(new Error("network"));

    render(<TopCrashesCard environment="production" period="7d" triggerRefreshCounter={0} />);

    expect(await screen.findByText("Não foi possível carregar os crashes — worker indisponível")).toBeInTheDocument();
  });
});
