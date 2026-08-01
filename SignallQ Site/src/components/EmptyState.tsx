export function EmptyState({
  icon,
  title,
  message,
  actionIcon,
  actionLabel,
  color = "var(--text-secondary)",
  onAction,
}: {
  icon: string;
  title: string;
  message: string;
  actionIcon: string;
  actionLabel: string;
  color?: string;
  onAction?: () => void;
}) {
  return (
    <div className="max-w-[420px] flex flex-col items-center gap-[14px] py-[64px] px-2 text-center mx-auto">
      <span
        className="material-symbols-outlined text-[44px]"
        style={{ color }}
      >
        {icon}
      </span>
      <div className="font-semibold text-[22px] leading-[1.27] text-[color:var(--text-primary)]">
        {title}
      </div>
      <div className="font-normal text-[16px] leading-[1.5] text-[color:var(--text-secondary)] text-balance">
        {message}
      </div>
      <button
        onClick={onAction}
        className="h-[44px] flex items-center gap-2 px-5 rounded-[var(--radius-button,_999px)] bg-[color:var(--accent)] hover:opacity-90 transition-opacity"
      >
        <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">
          {actionIcon}
        </span>
        <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--on-accent)]">
          {actionLabel}
        </span>
      </button>
    </div>
  );
}
