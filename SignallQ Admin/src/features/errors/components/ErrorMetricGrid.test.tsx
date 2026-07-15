import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { ErrorMetricGrid } from "./ErrorMetricGrid";

// Paridade com o mockup do Luiz (sec-errors, errorKpis): Crash-free users,
// Taxa de ANR, Crashes não resolvidos, MTTR médio. Crash-free users e Crashes
// não resolvidos vêm do Crashlytics real quando source==="bigquery" — mesmo
// contrato de FirebaseCrashlyticsSummary usado no OverviewMetricGrid.
describe("ErrorMetricGrid", () => {
  it("mostra dado real do Crashlytics quando source é bigquery", () => {
    render(
      <ErrorMetricGrid
        firebaseCrashlytics={{ source: "bigquery", unresolvedCrashes: 4, affectedUsers: 142, crashFreeUsersPercentage: 98.6 }}
      />
    );

    expect(screen.getByText("Crash-free users")).toBeInTheDocument();
    expect(screen.getByText("98.6%")).toBeInTheDocument();
    expect(screen.getByText("Crashes não resolvidos")).toBeInTheDocument();
    expect(screen.getByText("4")).toBeInTheDocument();
  });

  it("mostra motivo honesto quando ainda não há volume no BigQuery export", () => {
    render(
      <ErrorMetricGrid
        firebaseCrashlytics={{ source: "no_data_yet", unresolvedCrashes: 0, crashFreeUsersPercentage: 100 }}
      />
    );

    expect(screen.getAllByText("Não disponível").length).toBeGreaterThanOrEqual(2);
    expect(screen.getAllByText("BigQuery export ainda sem volume de crash").length).toBeGreaterThanOrEqual(2);
  });

  it("mostra 'Não disponível' quando ainda não carregou (null)", () => {
    render(<ErrorMetricGrid firebaseCrashlytics={null} />);

    expect(screen.getAllByText("Não disponível").length).toBeGreaterThanOrEqual(2);
    expect(screen.getByText("Taxa de ANR")).toBeInTheDocument();
    expect(screen.getByText("MTTR médio")).toBeInTheDocument();
  });
});
