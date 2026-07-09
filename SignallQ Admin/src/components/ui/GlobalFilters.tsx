import React from "react";
import { ChevronDown } from "lucide-react";
import { GlobalFilterConfig } from "../../types/filters";
import { alpha } from "../../utils/color";

/**
 * GH#552 (Fase 1) — filtros globais compartilhados por todas as 9 telas do
 * redesenho. Cada tela monta seu próprio array de `GlobalFilterConfig` (período,
 * versão, OS, região/operadora etc.) conforme a pergunta-guia — este componente
 * só renderiza, não decide quais filtros existem em cada contexto.
 *
 * Ainda não conectado a nenhuma tela existente (isso é migração de conteúdo,
 * Fase 2/3). Uso previsto:
 *
 * <GlobalFilters filters={[
 *   { key: "period", label: "Período", value: period, onChange: setPeriod, options: GLOBAL_PERIOD_OPTIONS },
 *   { key: "os", label: "OS", value: os, onChange: setOs, options: GLOBAL_OS_OPTIONS },
 * ]} />
 */
interface GlobalFiltersProps {
  filters: GlobalFilterConfig[];
  className?: string;
  id?: string;
}

export const GlobalFilters: React.FC<GlobalFiltersProps> = ({ filters, className = "", id }) => {
  const [focusedKey, setFocusedKey] = React.useState<string | null>(null);

  if (filters.length === 0) return null;

  return (
    <div
      id={id || "global-filters"}
      className={`flex flex-wrap items-center gap-3 pb-5 mb-5 ${className}`}
      style={{ borderBottom: "1px solid var(--sq-border)" }}
    >
      {filters.map((filter) => (
        <div
          key={filter.key}
          className="relative flex items-center gap-2 pl-3 pr-2 py-1.5 rounded-[var(--radius-button)] transition-shadow"
          style={{
            backgroundColor: "var(--sq-bg-card)",
            border: "1px solid var(--sq-border)",
            boxShadow:
              focusedKey === filter.key ? `0 0 0 2px ${alpha("var(--sq-accent)", 40)}` : "none",
          }}
        >
          <label
            htmlFor={`global-filter-${filter.key}`}
            className="text-[10px] font-sans font-semibold uppercase tracking-[0.08em] select-none shrink-0"
            style={{ color: "var(--sq-text-tertiary)" }}
          >
            {filter.label}
          </label>
          <select
            id={`global-filter-${filter.key}`}
            value={filter.value}
            onChange={(e) => filter.onChange(e.target.value)}
            className="appearance-none bg-transparent cursor-pointer text-xs font-medium pr-4 rounded-[4px] transition-colors focus:outline-none"
            style={{ color: "var(--sq-text-primary)" }}
            onFocus={() => setFocusedKey(filter.key)}
            onBlur={() => setFocusedKey((current) => (current === filter.key ? null : current))}
          >
            {filter.options.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
          <ChevronDown
            className="w-3 h-3 absolute right-2.5 pointer-events-none"
            style={{ color: "var(--sq-text-tertiary)" }}
          />
        </div>
      ))}
    </div>
  );
};
