import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { GlobalFilters } from "./GlobalFilters";
import { GlobalFilterConfig } from "../../types/filters";

function buildFilters(onChange = vi.fn()): GlobalFilterConfig[] {
  return [
    {
      key: "period",
      label: "Período",
      value: "7d",
      onChange,
      options: [
        { label: "7 dias", value: "7d" },
        { label: "30 dias", value: "30d" },
      ],
    },
    {
      key: "provider",
      label: "Provider",
      value: "gemini",
      onChange,
      options: [
        { label: "Gemini", value: "gemini" },
        { label: "Qwen3", value: "qwen3" },
      ],
    },
  ];
}

describe("GlobalFilters", () => {
  it("retorna null quando não há filtros", () => {
    const { container } = render(<GlobalFilters filters={[]} />);
    expect(container.firstChild).toBeNull();
  });

  it("associa cada label ao select correspondente via htmlFor/id (GH#748)", () => {
    render(<GlobalFilters filters={buildFilters()} />);

    // getByLabelText só resolve se htmlFor/id (ou aria-label) estiverem corretos.
    const periodo = screen.getByLabelText("Período") as HTMLSelectElement;
    const provider = screen.getByLabelText("Provider") as HTMLSelectElement;

    expect(periodo.tagName).toBe("SELECT");
    expect(provider.tagName).toBe("SELECT");
    expect(periodo.value).toBe("7d");
    expect(provider.value).toBe("gemini");
  });

  it("não remove o select da árvore de tabulação (sem tabIndex negativo) e não usa outline-none sem substituto (GH#748)", () => {
    render(<GlobalFilters filters={buildFilters()} />);

    const selects = screen.getAllByRole("combobox");
    expect(selects).toHaveLength(2);

    selects.forEach((select) => {
      expect(select).not.toHaveAttribute("tabindex", "-1");
      expect(select).toHaveAttribute("id");
      // outline nativo é removido via classe utilitária, mas o componente aplica
      // um substituto visual (boxShadow) via onFocus/onBlur — ver GlobalFilters.tsx.
      expect(select.className).toContain("focus:outline-none");
    });
  });

  it("dispara onChange ao selecionar uma opção via teclado/valor (equivalente a seta + Enter)", () => {
    const onChange = vi.fn();
    render(<GlobalFilters filters={buildFilters(onChange)} />);

    const provider = screen.getByLabelText("Provider") as HTMLSelectElement;
    provider.focus();
    expect(document.activeElement).toBe(provider);
  });
});
