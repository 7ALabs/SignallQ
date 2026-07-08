import { describe, it, expect } from "vitest";
import { readFileSync } from "fs";
import path from "path";

// GH#552 (Fase 3 / #746 item 1) — trava a decisão de paleta: --success/--attention
// não podem voltar a apontar para --text-primary (cinza), e --chart-line-* não
// podem voltar a ser 3 tons de cinza. Ver docs_ai/design-system/ADMIN_PALETTE_DECISION_552.md.
describe("index.css — paleta semântica (GH#552 Fase 3)", () => {
  const cssPath = path.resolve(__dirname, "index.css");
  const css = readFileSync(cssPath, "utf-8");

  it("--success não aponta mais para --text-primary em nenhum tema", () => {
    expect(css).not.toMatch(/--success:\s*var\(--text-primary\)/);
  });

  it("--attention não aponta mais para --text-primary em nenhum tema", () => {
    expect(css).not.toMatch(/--attention:\s*var\(--text-primary\)/);
  });

  it("define cor própria para --success e --attention no tema dark padrão", () => {
    const rootBlock = css.slice(css.indexOf(":root"), css.indexOf("[data-theme=\"dark\"]"));
    expect(rootBlock).toMatch(/--success:\s*#34D399/i);
    expect(rootBlock).toMatch(/--attention:\s*#F59E0B/i);
  });

  it("mantém o acento de marca violeta (#6C2BFF) como --primary", () => {
    expect(css).toMatch(/--primary:\s*#6C2BFF/i);
  });

  it("--chart-line-* usa cores distintas (primary/info/success), não tons de cinza", () => {
    expect(css).toMatch(/--chart-line-primary:\s*var\(--primary\)/);
    expect(css).toMatch(/--chart-line-secondary:\s*var\(--info\)/);
    expect(css).toMatch(/--chart-line-tertiary:\s*var\(--success\)/);
  });
});
