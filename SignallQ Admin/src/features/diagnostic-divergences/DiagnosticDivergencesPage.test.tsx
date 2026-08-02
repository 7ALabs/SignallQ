import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DiagnosticDivergencesPage } from "./DiagnosticDivergencesPage";

// Refs #1446 (shadow mode) — smoke test: mocka o service diretamente (mesmo padrão
// de DiagnosticRulesetsPage.test.tsx). Cobre carregamento inicial, contagem por
// classificação e o filtro ao clicar num card de resumo.
const list = vi.fn();

vi.mock("../../services/diagnosticDivergencesService", () => ({
  diagnosticDivergencesService: {
    list: (...args: unknown[]) => list(...args),
  },
}));

const ITEMS = [
  {
    id: "div-1",
    executionId: "exec-a1b2",
    snapshotHash: "sha256:aa11",
    classification: "EXACT_MATCH" as const,
    localStatus: "OK",
    remoteStatus: "OK",
    localScore: 92,
    remoteScore: 92,
    localFlow: "saudavel_monitorar",
    remoteFlow: "saudavel_monitorar",
    localSource: "BUNDLED_LOCAL",
    remoteSource: "REMOTE",
    localDurationMs: 180,
    remoteDurationMs: 420,
    appVersion: "0.24.0",
    versionCode: 240,
    createdAt: "2026-07-25T14:00:00.000Z",
  },
  {
    id: "div-3",
    executionId: "exec-e5f6",
    snapshotHash: "sha256:cc33",
    classification: "MAJOR_DIVERGENCE" as const,
    localStatus: "CRITICAL",
    remoteStatus: "OK",
    localScore: 38,
    remoteScore: 88,
    localFlow: "isp_externo",
    remoteFlow: "saudavel_monitorar",
    localSource: "BUNDLED_LOCAL",
    remoteSource: "REMOTE",
    localDurationMs: 190,
    remoteDurationMs: 460,
    appVersion: "0.23.5",
    versionCode: 235,
    createdAt: "2026-07-26T09:10:00.000Z",
  },
];

describe("DiagnosticDivergencesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    list.mockResolvedValue({
      items: ITEMS,
      summary: { EXACT_MATCH: 1, MAJOR_DIVERGENCE: 1 },
    });
  });

  it("carrega o resumo por classificação e a lista de divergências", async () => {
    render(<DiagnosticDivergencesPage />);

    expect(await screen.findByText("exec-a1b2")).toBeInTheDocument();
    expect(screen.getByText("exec-e5f6")).toBeInTheDocument();
    expect(list).toHaveBeenCalledWith({ classification: undefined, limit: 50 });
  });

  it("filtra ao clicar num card de classificação e recarrega o service", async () => {
    const user = userEvent.setup();
    render(<DiagnosticDivergencesPage />);

    await screen.findByText("exec-a1b2");
    list.mockClear();
    list.mockResolvedValue({
      items: [ITEMS[1]],
      summary: { EXACT_MATCH: 1, MAJOR_DIVERGENCE: 1 },
    });

    const summary = document.getElementById("diagnostic-divergences-summary") as HTMLElement;
    await user.click(within(summary).getByText("Divergência maior"));

    await waitFor(() => expect(list).toHaveBeenCalledWith({ classification: "MAJOR_DIVERGENCE", limit: 50 }));
  });
});
