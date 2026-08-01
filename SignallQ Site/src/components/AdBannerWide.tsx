export function AdBannerWide({ compact = false, variant }: { compact?: boolean; variant?: string }) {
  return (
    <div
      aria-label="Publicidade"
      className="w-full h-[90px] box-border rounded-xl border border-[color-mix(in_srgb,_var(--border)_22%,_transparent)] bg-[color:var(--bg-secondary)] flex items-center justify-center gap-2"
    >
      <span className="material-symbols-outlined text-[18px] text-[color:var(--text-tertiary)]">
        ads_click
      </span>
      <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] tracking-[.04em] uppercase">
        Publicidade
      </span>
    </div>
  );
}
