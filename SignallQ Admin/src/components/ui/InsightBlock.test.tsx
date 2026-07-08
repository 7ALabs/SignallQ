import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { InsightBlock } from "./InsightBlock";

describe("InsightBlock", () => {
  it("renderiza sem crash com conteúdo", () => {
    render(<InsightBlock>Teste de insight</InsightBlock>);
    
    expect(screen.getByText("Teste de insight")).toBeInTheDocument();
  });

  it("renderiza com id customizado", () => {
    const { container } = render(
      <InsightBlock id="custom-insight">Conteúdo</InsightBlock>
    );
    
    expect(container.querySelector("#custom-insight")).toBeInTheDocument();
  });

  it("renderiza com ícone Lightbulb", () => {
    const { container } = render(<InsightBlock>Insight</InsightBlock>);
    
    // Verifica se o SVG do Lightbulb está presente (Lucide)
    const svg = container.querySelector("svg");
    expect(svg).toBeInTheDocument();
  });
});
