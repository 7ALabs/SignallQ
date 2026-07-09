// GH#781 (paridade mockup) — Cloudflare Workers AI fatura em USD, mas o time
// e o mockup de referência do Luiz operam em R$. Sem uma fonte de câmbio ao
// vivo integrada ao worker, usamos uma taxa fixa documentada (~cotação
// jul/2026) só para exibição — não afeta o valor real cobrado pela Cloudflare.
// Quando o worker expuser câmbio real, substituir esta constante.
export const USD_TO_BRL_RATE = 5.3;

export function formatCurrency(valueUsd: number, decimals = 2): string {
  if (valueUsd === 0) return "R$ 0,00";
  const valueBrl = valueUsd < 0.01 ? 0 : valueUsd * USD_TO_BRL_RATE;
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  }).format(valueBrl);
}

/** Reverte uma string formatada por formatCurrency (pt-BR) para o número em BRL exibido. */
export function parseFormattedCurrencyBrl(formatted: string): number {
  const digitsAndSeparators = formatted.replace(/[^0-9,.-]/g, "");
  const normalized = digitsAndSeparators.replace(/\./g, "").replace(",", ".");
  return Number(normalized) || 0;
}

export function formatNumber(value: number): string {
  return new Intl.NumberFormat("en-US").format(Math.round(value));
}

export function formatPercent(value: number, decimals = 1): string {
  const rounded = parseFloat(value.toFixed(decimals));
  return `${rounded}%`;
}
