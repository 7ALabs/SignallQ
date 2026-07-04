// SIG-12: Safari < 16.2 nao suporta color-mix() -- iPad 2012 roda Safari 9/10 no maximo
// (iOS 9.3.6 ou 10.3.4, conforme geracao). Declaracoes com color-mix() sao invalidas
// nesses navegadores: a propriedade fica sem valor, quebrando fundo/borda translucidos.
//
// `alpha()` resolve a mistura em runtime e retorna sempre rgba(), suportado desde
// as primeiras versoes do WebKit. Aceita tanto `var(--token)` (lido via
// getComputedStyle, respeitando o tema ativo) quanto uma cor literal (hex, rgb,
// nome como "white") ja resolvida em JS.
const canvas = typeof document !== "undefined" ? document.createElement("canvas") : null;
const ctx = canvas?.getContext("2d") ?? null;

function resolveVarExpression(input: string): string {
  const varMatch = input.trim().match(/^var\((--[\w-]+)\s*(?:,\s*(.+))?\)$/);
  if (!varMatch) return input;

  if (typeof document === "undefined") {
    return varMatch[2]?.trim() ?? input;
  }

  const resolved = getComputedStyle(document.documentElement).getPropertyValue(varMatch[1]).trim();
  if (resolved) return resolveVarExpression(resolved);
  return varMatch[2]?.trim() ?? input;
}

function toRgbTriplet(color: string): [number, number, number] | null {
  if (!ctx) return null;

  // reset para detectar cor invalida (canvas mantem o valor anterior se a atribuicao falhar)
  ctx.fillStyle = "#000000";
  ctx.fillStyle = color;
  const normalized = ctx.fillStyle;

  const hexMatch = normalized.match(/^#([0-9a-f]{6})$/i);
  if (hexMatch) {
    const int = parseInt(hexMatch[1], 16);
    return [(int >> 16) & 255, (int >> 8) & 255, int & 255];
  }

  const rgbMatch = normalized.match(/^rgba?\(([\d.]+),\s*([\d.]+),\s*([\d.]+)/i);
  if (rgbMatch) {
    return [Number(rgbMatch[1]), Number(rgbMatch[2]), Number(rgbMatch[3])];
  }

  return null;
}

/**
 * Equivalente a `color-mix(in srgb, <color> <percent>%, transparent)`, mas
 * compativel com Safari antigo. `color` pode ser `var(--token)` ou uma cor
 * literal (hex/rgb/nome).
 */
export function alpha(color: string, percent: number): string {
  const resolved = resolveVarExpression(color);
  const rgb = toRgbTriplet(resolved);
  if (!rgb) return resolved;

  const a = Math.max(0, Math.min(100, percent)) / 100;
  return `rgba(${rgb[0]}, ${rgb[1]}, ${rgb[2]}, ${a})`;
}

/**
 * Equivalente a `color-mix(in srgb, <colorA> <percent>%, <colorB>)` -- mistura
 * opaca de duas cores (sem transparencia). Mesma justificativa de `alpha()`.
 */
export function mix(colorA: string, percent: number, colorB: string): string {
  const rgbA = toRgbTriplet(resolveVarExpression(colorA));
  const rgbB = toRgbTriplet(resolveVarExpression(colorB));
  if (!rgbA || !rgbB) return resolveVarExpression(colorA);

  const t = Math.max(0, Math.min(100, percent)) / 100;
  const r = Math.round(rgbA[0] * t + rgbB[0] * (1 - t));
  const g = Math.round(rgbA[1] * t + rgbB[1] * (1 - t));
  const b = Math.round(rgbA[2] * t + rgbB[2] * (1 - t));
  return `rgb(${r}, ${g}, ${b})`;
}
