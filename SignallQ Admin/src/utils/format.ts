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

/** GH#1341 — data relativa curta (pt-BR) pra metadados de review/eventos ("há 3 dias"). */
export function formatRelativeDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) return "—";

  const diffMs = Date.now() - date.getTime();
  const diffMinutes = Math.round(diffMs / 60000);
  if (diffMinutes < 1) return "agora";
  if (diffMinutes < 60) return `há ${diffMinutes} min`;

  const diffHours = Math.round(diffMinutes / 60);
  if (diffHours < 24) return `há ${diffHours}h`;

  const diffDays = Math.round(diffHours / 24);
  if (diffDays < 30) return `há ${diffDays} dia${diffDays === 1 ? "" : "s"}`;

  const diffMonths = Math.round(diffDays / 30);
  if (diffMonths < 12) return `há ${diffMonths} mês${diffMonths === 1 ? "" : "es"}`;

  const diffYears = Math.round(diffMonths / 12);
  return `há ${diffYears} ano${diffYears === 1 ? "" : "s"}`;
}
