export function EstadoVazio({
  icon,
  iconSize = 44,
  badge,
  title,
  message,
  messageSize = 16,
  actionIcon,
  actionLabel,
  actionVariant = "filled",
  card = false,
  color = "var(--text-secondary)",
  onAction,
}: {
  icon: string;
  /** Tamanho do ícone em px — estados diferentes usam 28–44 (Guia de Implementação, §7.2). */
  iconSize?: number;
  /** Selo pequeno acima do ícone (ex.: "Em breve") — opcional, usado pelas telas placeholder. */
  badge?: string;
  title: string;
  message: string;
  /** Tamanho da mensagem em px — default 16 (Home, ScreenHome.dc.html "mensagem 16"); Histórico usa 14 (ScreenHistorico.dc.html, estados vazio/indisponível). */
  messageSize?: 14 | 16;
  actionIcon?: string;
  actionLabel?: string;
  /** "filled" (accent, com ícone) ou "outline" (borda, sem ícone, ação secundária como "Tentar novamente"). */
  actionVariant?: "filled" | "outline";
  /** Envolve o bloco num card (bg-secondary + sombra) — usado pelo estado "indisponível" do Histórico. */
  card?: boolean;
  color?: string;
  onAction?: () => void;
}) {
  return (
    <div
      className={
        card
          ? "w-full flex flex-col items-center gap-2.5 rounded-2xl py-10 px-6 text-center bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.16)]"
          : "max-w-[420px] flex flex-col items-center gap-[14px] py-16 px-2 text-center mx-auto"
      }
    >
      {badge && (
        <span className="rounded-full py-[2px] px-[10px] font-semibold text-[11px] leading-[1.4] tracking-[.04em] uppercase text-[color:var(--on-accent)] bg-[color:var(--accent)]">
          {badge}
        </span>
      )}
      <span
        className="material-symbols-outlined"
        style={{ fontSize: iconSize, color }}
      >
        {icon}
      </span>
      <div className="font-semibold text-[22px] leading-[1.27] text-[color:var(--text-primary)]">
        {title}
      </div>
      <div
        className={`font-normal text-[color:var(--text-secondary)] text-balance ${
          messageSize === 14 ? "text-[14px] leading-[1.45]" : "text-[16px] leading-[1.5]"
        }`}
      >
        {message}
      </div>
      {actionLabel && actionVariant === "outline" && (
        <button
          onClick={onAction}
          className="mt-1 h-10 flex items-center px-4 rounded-[var(--radius-button)] border border-[color:var(--border)] font-medium text-[14px] leading-[1.43] text-[color:var(--text-primary)] bg-transparent cursor-pointer"
        >
          {actionLabel}
        </button>
      )}
      {actionLabel && actionVariant === "filled" && (
        <button
          onClick={onAction}
          className="h-[44px] flex items-center gap-2 px-5 rounded-[var(--radius-button,_999px)] bg-[color:var(--accent)] hover:opacity-90 transition-opacity"
        >
          {actionIcon && (
            <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">
              {actionIcon}
            </span>
          )}
          <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--on-accent)]">
            {actionLabel}
          </span>
        </button>
      )}
    </div>
  );
}
