import React from "react";

export interface CategoryTabItem<T extends string = string> {
  key: T;
  label: string;
  /** Pequeno resumo de status ao lado do rótulo — ex. contagem de pendências. */
  badge?: string;
}

interface CategoryTabsProps<T extends string = string> {
  categories: CategoryTabItem<T>[];
  active: T;
  onChange: (key: T) => void;
  id?: string;
}

/**
 * GH#1341 — item 5.2 do plano de UX: pill group reaproveitável pra trocar de categoria dentro
 * da mesma Page (Google Play/Firebase), mesmo visual do toggle PROD/STAGING/TODOS do
 * `FilterBar`. Troca de categoria só re-renderiza o conteúdo abaixo — sem navegar de rota.
 */
export function CategoryTabs<T extends string = string>({
  categories,
  active,
  onChange,
  id,
}: CategoryTabsProps<T>) {
  return (
    <div
      id={id}
      className="inline-flex items-center p-1 rounded-[var(--radius-button)] gap-0.5"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
    >
      {categories.map((category) => (
        <button
          key={category.key}
          type="button"
          onClick={() => onChange(category.key)}
          className="flex items-center gap-1.5 px-3.5 py-1.5 text-[11px] font-sans font-medium uppercase tracking-wide rounded-lg transition-colors cursor-pointer"
          style={
            active === category.key
              ? { backgroundColor: "var(--sq-control-active)", color: "var(--text-primary)" }
              : { color: "var(--text-secondary)" }
          }
        >
          {category.label}
          {category.badge && (
            <span
              className="text-[10px] font-mono px-1 rounded"
              style={{ color: "var(--text-tertiary)" }}
            >
              {category.badge}
            </span>
          )}
        </button>
      ))}
    </div>
  );
}
