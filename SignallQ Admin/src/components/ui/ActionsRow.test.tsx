import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ActionsRow } from "./ActionsRow";

describe("ActionsRow", () => {
  it("renderiza sem crash", () => {
    const onClick = vi.fn();
    const actions = [
      { label: "Ação 1", onClick },
      { label: "Ação 2", onClick, variant: "secondary" as const },
    ];
    
    render(<ActionsRow actions={actions} />);
    
    expect(screen.getByText("Ação 1")).toBeInTheDocument();
    expect(screen.getByText("Ação 2")).toBeInTheDocument();
  });

  it("retorna null quando lista vazia", () => {
    const { container } = render(<ActionsRow actions={[]} />);
    expect(container.firstChild).toBeNull();
  });

  it("chama onClick ao clicar no botão", async () => {
    const onClick = vi.fn();
    const actions = [{ label: "Clique", onClick }];
    
    render(<ActionsRow actions={actions} />);
    
    const button = screen.getByRole("button", { name: /Clique/i });
    await button.click();
    
    expect(onClick).toHaveBeenCalled();
  });
});
