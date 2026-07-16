import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { OverviewMetricGrid } from "./OverviewMetricGrid";

// Paridade com o protótipo md3-tobe (Md3DashboardContent.dc.html:25-30): os 4
// KPIs da seção "App" do Centro de Controle são Usuários Ativos, Sessões
// (7d), Crash-free Rate e Nota na Play Store. Custo de IA saiu deste grid —
// virou card isolado (AiCostSummaryCard), fora deste componente.
describe("OverviewMetricGrid", () => {
  it("renderiza os 4 KPIs da seção App com dado real quando disponível", () => {
    render(
      <OverviewMetricGrid
        activeUsersToday={18240}
        sessions7d={12860}
        firebaseCrashlytics={{ source: "bigquery", unresolvedCrashes: 4, affectedUsers: 142, crashFreeUsersPercentage: 98.6 }}
        playStoreRating={{ averageRating: 4.6, totalRatings: 2340, starDistribution: { five: 0, four: 0, three: 0, two: 0, one: 0 } }}
      />
    );

    expect(screen.getByText("Usuários Ativos")).toBeInTheDocument();
    expect(screen.getByText("18.240")).toBeInTheDocument();
    expect(screen.getByText("Sessões (7d)")).toBeInTheDocument();
    expect(screen.getByText("12.860")).toBeInTheDocument();
    expect(screen.getByText("Crash-free Rate")).toBeInTheDocument();
    expect(screen.getByText("98.6%")).toBeInTheDocument();
    expect(screen.getByText("Nota na Play Store")).toBeInTheDocument();
    expect(screen.getByText("4.6 ★")).toBeInTheDocument();
  });

  it("mostra 'Não disponível' honesto quando a integração real não existe ainda", () => {
    render(
      <OverviewMetricGrid
        activeUsersToday={null}
        sessions7d={null}
        firebaseCrashlytics={null}
        playStoreRating={null}
      />
    );

    expect(screen.getAllByText("Não disponível").length).toBeGreaterThanOrEqual(3);
  });

  it("mostra o motivo certo por source de crash-free quando não há dado real", () => {
    const { rerender } = render(
      <OverviewMetricGrid
        activeUsersToday={100}
        sessions7d={500}
        firebaseCrashlytics={{ source: "no_credentials", unresolvedCrashes: 0, crashFreeUsersPercentage: 100 }}
        playStoreRating={null}
      />
    );
    expect(screen.getByText("Firebase não configurado no Admin Worker")).toBeInTheDocument();

    rerender(
      <OverviewMetricGrid
        activeUsersToday={100}
        sessions7d={500}
        firebaseCrashlytics={{ source: "no_data_yet", unresolvedCrashes: 0, crashFreeUsersPercentage: 100 }}
        playStoreRating={null}
      />
    );
    expect(screen.getByText("BigQuery export ainda sem volume de crash")).toBeInTheDocument();
  });
});
