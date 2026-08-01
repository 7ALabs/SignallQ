export function AdRail({ variant = "a" }: { variant?: "a" | "b" }) {
  return (
    <aside
      aria-label="Publicidade"
      className="w-[240px] shrink-0 flex flex-col gap-3"
    >
      <div className="relative w-[240px] h-[600px] box-border overflow-hidden rounded-xl border border-[color-mix(in_srgb,_var(--border)_22%,_transparent)] bg-[color:var(--bg-secondary)] flex flex-col items-center justify-center gap-2">
        <span className="material-symbols-outlined text-[22px] text-[color:var(--text-tertiary)]">
          ads_click
        </span>
        <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] tracking-[.04em] uppercase">
          Publicidade
        </span>
        <span className="font-normal text-[11px] leading-[1.3] text-[color:var(--text-tertiary)]">
          240 × 600
        </span>
      </div>
    </aside>
  );
}
