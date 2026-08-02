export interface ItemFaixaMetricas {
  icon?: string;
  label: string;
  value: string;
  caption?: string;
  color?: string;
}

// Faixa de 3 métricas separada por hairline — nunca card (sem fundo, sombra
// ou raio). Reconstrução v4 (`Guia de Implementação.dc.html`, §6/§10):
// substitui o MetricStrip do protótipo, reaproveitado tanto pelo trio
// Latência/Download/Upload da tela "running" (variant="execucao") quanto
// pela grade de veredito Download/Upload/Latência da tela "result"
// (variant="resultado") — únicos dois usos hoje, mas o mesmo componente serve
// qualquer faixa de N métricas separadas por hairline no futuro.
export function FaixaMetricas({
  items,
  variant = "resultado",
}: {
  items: ItemFaixaMetricas[];
  variant?: "resultado" | "execucao";
}) {
  if (variant === "execucao") {
    return (
      <div className="w-full max-w-[520px] flex border border-[color-mix(in_srgb,_var(--border)_22%,_transparent)] rounded-2xl overflow-hidden">
        {items.map((item, i) => (
          <div
            key={item.label}
            className={`flex-1 flex flex-col items-center gap-1 py-[14px] px-2 ${
              i > 0
                ? "border-l border-[color-mix(in_srgb,_var(--border)_22%,_transparent)]"
                : ""
            }`}
          >
            <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
              {item.label}
            </span>
            <span
              className="font-medium text-[14px] leading-[1.43]"
              style={{ color: item.color ?? "var(--text-primary)" }}
            >
              {item.value}
            </span>
          </div>
        ))}
      </div>
    );
  }

  return (
    <div className="w-full grid grid-cols-3 pt-1 pb-7 border-b border-[color-mix(in_srgb,_var(--border)_16%,_transparent)]">
      {items.map((item, i) => (
        <div
          key={item.label}
          className={`flex flex-col items-center gap-1 px-2 ${
            i < items.length - 1
              ? "border-r border-[color-mix(in_srgb,_var(--border)_16%,_transparent)]"
              : ""
          }`}
        >
          <div className="flex items-center gap-[6px]">
            {item.icon && (
              <span
                className="material-symbols-outlined text-[16px]"
                style={{ color: item.color ?? "var(--success)" }}
              >
                {item.icon}
              </span>
            )}
            <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
              {item.label}
            </span>
          </div>
          <div className="font-bold text-[34px] leading-[1.1] text-[color:var(--text-primary)] tabular-nums">
            {item.value}
          </div>
          {item.caption && (
            <div className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-secondary)]">
              {item.caption}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}
