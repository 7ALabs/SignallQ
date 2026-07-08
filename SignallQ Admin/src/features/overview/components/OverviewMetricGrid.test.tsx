import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { OverviewMetricGrid } from "./OverviewMetricGrid";
import { mockOverviewProd7d } from "../../../mocks/overview.mock";

// GH#746 — Centro de Controle: grid tinha que cair de 10 para 3-5 KPIs.
describe("OverviewMetricGrid", () => {
  it("renderiza só os 4 KPIs de saúde geral (volume, alcance, estabilidade, custo)", () => {
    render(<OverviewMetricGrid metrics={mockOverviewProd7d} />);

    expect(screen.getByText(mockOverviewProd7d.diagnosticsCount.label)).toBeInTheDocument();
    expect(screen.getByText(mockOverviewProd7d.activeUsers.label)).toBeInTheDocument();
    expect(screen.getByText(mockOverviewProd7d.successRate!.label)).toBeInTheDocument();
    expect(screen.getByText(mockOverviewProd7d.aiCost.label)).toBeInTheDocument();

    // KPIs migrados/duplicados na mesma tela não devem mais aparecer aqui.
    expect(screen.queryByText(mockOverviewProd7d.topProblem!.label)).not.toBeInTheDocument();
    expect(screen.queryByText(mockOverviewProd7d.mostTestType!.label)).not.toBeInTheDocument();
    expect(screen.queryByText(mockOverviewProd7d.prodVersion!.label)).not.toBeInTheDocument();
    expect(screen.queryByText(mockOverviewProd7d.crashFreeUsers!.label)).not.toBeInTheDocument();
  });
});
