import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { DiagnosticsFilters } from "./DiagnosticsFilters";

function buildProps(overrides: Partial<React.ComponentProps<typeof DiagnosticsFilters>> = {}) {
  return {
    searchText: "",
    onSearchChange: vi.fn(),
    selectedNetwork: "all",
    onNetworkChange: vi.fn(),
    selectedOperator: "all",
    onOperatorChange: vi.fn(),
    selectedScore: "all",
    onScoreChange: vi.fn(),
    selectedIssue: "all",
    onIssueChange: vi.fn(),
    selectedVersion: "all",
    onVersionChange: vi.fn(),
    availableVersions: [],
    selectedPeriod: "7d",
    onPeriodChange: vi.fn(),
    selectedEnvironment: "production" as const,
    onEnvironmentChange: vi.fn(),
    selectedDistChannel: "" as const,
    onDistChannelChange: vi.fn(),
    selectedBuildType: "" as const,
    onBuildTypeChange: vi.fn(),
    selectedPlatform: "" as const,
    onPlatformChange: vi.fn(),
    selectedPlayTrack: "" as const,
    onPlayTrackChange: vi.fn(),
    onRefresh: vi.fn(),
    ...overrides,
  };
}

// migration 012_play_track.sql — filtro "Trilha" (Todas/Interno/Fechado (Alfa)/Aberto (Beta)/Produção).
describe("DiagnosticsFilters — filtro de Trilha do Play Console", () => {
  it("renderiza o filtro de Trilha com as 4 trilhas padrão e a opção Todas", () => {
    render(<DiagnosticsFilters {...buildProps()} />);

    const trilhaLabel = screen.getByText("Trilha");
    const trilhaSelect = trilhaLabel.parentElement?.querySelector("select") as HTMLSelectElement;
    const optionLabels = Array.from(trilhaSelect.options).map((o) => o.textContent);

    expect(optionLabels).toEqual(["Todas", "Interno", "Fechado (Alfa)", "Aberto (Beta)", "Produção"]);
  });

  it("dispara onPlayTrackChange com o valor da trilha selecionada", () => {
    const onPlayTrackChange = vi.fn();
    render(<DiagnosticsFilters {...buildProps({ onPlayTrackChange })} />);

    const trilhaSelect = screen.getByText("Interno").closest("select") as HTMLSelectElement;
    fireEvent.change(trilhaSelect, { target: { value: "internal" } });

    expect(onPlayTrackChange).toHaveBeenCalledWith("internal");
  });

  it("nunca assume 'production' por padrão — valor inicial é 'Todas' (string vazia)", () => {
    render(<DiagnosticsFilters {...buildProps()} />);

    const trilhaSelect = screen.getByText("Interno").closest("select") as HTMLSelectElement;
    expect(trilhaSelect.value).toBe("");
  });
});
