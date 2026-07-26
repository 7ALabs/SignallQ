import { describe, it, expect } from "vitest";
import { render, screen } from "@testing-library/react";
import { BrandEndorsement } from "./BrandEndorsement";

describe("BrandEndorsement", () => {
  it("renderiza o texto 'by 7A' na variante padrão", () => {
    render(<BrandEndorsement />);
    expect(screen.getByText("by")).toBeInTheDocument();
    expect(screen.getByText("7A")).toBeInTheDocument();
  });

  it("não renderiza símbolo quando symbolSrc não é informado, mesmo pedindo variant symbol-text", () => {
    const { container } = render(<BrandEndorsement variant="symbol-text" />);
    expect(container.querySelector("img")).toBeNull();
  });

  it("renderiza símbolo decorativo (alt vazio, aria-hidden) quando symbolSrc é informado", () => {
    const { container } = render(
      <BrandEndorsement variant="symbol-text" symbolSrc="/brand/7a/symbol.svg" />
    );
    const img = container.querySelector("img");
    expect(img).not.toBeNull();
    expect(img).toHaveAttribute("alt", "");
    expect(img).toHaveAttribute("aria-hidden", "true");
  });

  it("aceita id e className customizados", () => {
    const { container } = render(<BrandEndorsement id="footer-brand" className="mt-2" />);
    expect(container.querySelector("#footer-brand")).toHaveClass("mt-2");
  });
});
