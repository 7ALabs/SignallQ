import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor, within, fireEvent } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { DiagnosticRulesetsPage } from "./DiagnosticRulesetsPage";

// Refs #1446 — smoke test: mocka o service diretamente (mesmo padrão de
// NetworksOperatorsPage.test.tsx), cobre listagem, seleção de detalhe, o
// bloqueio de JSON inválido no editor de draft e o gate de confirmação de
// publish (ação sensível, GH#issue "confirmação obrigatória").
const listRulesets = vi.fn();
const getRuleset = vi.fn();
const validateRuleset = vi.fn();
const createDraft = vi.fn();
const publish = vi.fn();
const rollback = vi.fn();
const simulate = vi.fn();

vi.mock("../../services/diagnosticRulesetsService", () => ({
  diagnosticRulesetsService: {
    listRulesets: (...args: unknown[]) => listRulesets(...args),
    getRuleset: (...args: unknown[]) => getRuleset(...args),
    validateRuleset: (...args: unknown[]) => validateRuleset(...args),
    createDraft: (...args: unknown[]) => createDraft(...args),
    publish: (...args: unknown[]) => publish(...args),
    rollback: (...args: unknown[]) => rollback(...args),
    simulate: (...args: unknown[]) => simulate(...args),
  },
}));

const LIST = [
  { version: 3, schemaVersion: 3, engineVersion: 1, status: "PUBLISHED" as const, rolloutPercent: 100, publishedAt: "2026-07-20T12:00:00.000Z", updatedAt: "2026-07-20T12:00:00.000Z" },
  { version: 4, schemaVersion: 3, engineVersion: 1, status: "DRAFT" as const, rolloutPercent: 0, publishedAt: null, updatedAt: "2026-07-24T15:30:00.000Z" },
];

const DETAIL_V3 = {
  ...LIST[0],
  author: "camilo@signallq.app",
  justification: "Publicação inicial.",
  ruleset: { version: 3, schemaVersion: 3, engineVersion: 1, publishedAt: "2026-07-20T12:00:00.000Z", rules: [] },
};

const DETAIL_V4 = {
  ...LIST[1],
  author: "marina@signallq.app",
  justification: "Ajuste de limiar.",
  ruleset: { version: 4, schemaVersion: 3, engineVersion: 1, publishedAt: "", rules: [] },
};

describe("DiagnosticRulesetsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    listRulesets.mockResolvedValue(LIST);
    getRuleset.mockImplementation(async (version: number) => (version === 3 ? DETAIL_V3 : DETAIL_V4));
    validateRuleset.mockResolvedValue({ ok: true, version: 5, rules: 0 });
    createDraft.mockResolvedValue({ ok: true, version: 5 });
    publish.mockResolvedValue({ ok: true });
    rollback.mockResolvedValue({ ok: true });
    simulate.mockResolvedValue({
      ok: true,
      result: { overallStatus: "OK", score: 90, confidence: "HIGH", matchedRules: [], findings: [], humanSummary: "ok", primaryFlow: "saudavel_monitorar" },
    });
  });

  it("lista rulesets e seleciona automaticamente o publicado", async () => {
    render(<DiagnosticRulesetsPage />);

    expect(await screen.findByText("v3")).toBeInTheDocument();
    expect(screen.getByText("v4")).toBeInTheDocument();
    await waitFor(() => expect(getRuleset).toHaveBeenCalledWith(3));
    expect(await screen.findByText(/Ruleset v3/)).toBeInTheDocument();
  });

  it("abre o editor com JSON pré-preenchido e bloqueia validação com JSON inválido", async () => {
    const user = userEvent.setup();
    render(<DiagnosticRulesetsPage />);

    await screen.findByText("v3");
    await user.click(screen.getByText("Novo draft"));

    const area = document.querySelector("textarea") as HTMLTextAreaElement;
    expect(area).toBeTruthy();
    expect(area.value.length).toBeGreaterThan(0);

    fireEvent.change(area, { target: { value: "{ invalido" } });
    await user.click(screen.getByText("Validar"));

    await waitFor(() => expect(screen.getAllByText(/JSON inválido/).length).toBeGreaterThan(0));
    expect(validateRuleset).not.toHaveBeenCalled();
  });

  it("pede confirmação antes de publicar um draft e só chama o service após confirmar", async () => {
    const user = userEvent.setup();
    render(<DiagnosticRulesetsPage />);

    await screen.findByText("v3");
    await user.click(screen.getByText("v4"));
    await screen.findByText(/Ruleset v4/);

    await user.click(screen.getByText("Publicar"));

    expect(await screen.findByText(/Publicar ruleset v4\?/)).toBeInTheDocument();
    expect(publish).not.toHaveBeenCalled();

    const dialog = screen.getByRole("dialog");
    await user.click(within(dialog).getByRole("button", { name: "Publicar" }));
    await waitFor(() => expect(publish).toHaveBeenCalledWith(4));
  });
});
