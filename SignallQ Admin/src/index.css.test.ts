import { describe, it, expect } from "vitest";
import { readFileSync } from "fs";
import path from "path";

// GH#552 (Fase 3 / #746 item 1) — trava a decisão de paleta: --success/--attention
// não podem voltar a apontar para --text-primary (cinza), e --chart-line-* não
// podem voltar a ser 3 tons de cinza. Ver docs_ai/_archive/2026-07-12_ADMIN_PALETTE_DECISION_552.md.
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
    // Hex fechados 2026-07-16 a partir do protótipo `signallq-admin-md3-tobe`
    // (ver docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md) —
    // substitui a paleta anterior (#34D399/#F59E0B), mas mantém a garantia de
    // que --success/--attention nunca voltam a ser cinza.
    expect(rootBlock).toMatch(/--success:\s*#7DDB93/i);
    expect(rootBlock).toMatch(/--attention:\s*#FFB955/i);
  });

  it("mantém o acento de marca violeta (#6C2BFF) como --primary no tema claro", () => {
    // Tema dark padrão usa o tom 80 do M3 baseline (#CFBCFF) para acessibilidade
    // sobre superfície escura — o #6C2BFF de marca vive no tema claro.
    expect(css).toMatch(/--primary:\s*#6C2BFF/i);
  });

  it("--chart-line-* usa cores distintas (primary/info/success), não tons de cinza", () => {
    expect(css).toMatch(/--chart-line-primary:\s*var\(--primary\)/);
    expect(css).toMatch(/--chart-line-secondary:\s*var\(--info\)/);
    expect(css).toMatch(/--chart-line-tertiary:\s*var\(--success\)/);
  });
});
