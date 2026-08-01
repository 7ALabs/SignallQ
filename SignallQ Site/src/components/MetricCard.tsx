export function MetricCard({
  icon,
  label,
  value,
  unit,
  status,
  colorClass,
}: {
  icon: string;
  label: string;
  value: string;
  unit: string;
  status?: string;
  colorClass: string;
}) {
  return (
    <div className="flex flex-col items-center gap-1 rounded-[20px] py-5 px-2 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.18)]">
      <div className="flex items-center gap-[6px]">
        <span className={`material-symbols-outlined text-[16px] ${colorClass}`}>
          {icon}
        </span>
        <span className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase">
          {label}
        </span>
      </div>
      <div className="font-bold text-[34px] leading-[1.1] text-[color:var(--text-primary)] tabular-nums">
        {value}
      </div>
      <div className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-secondary)]">
        {unit} {status && `• ${status}`}
      </div>
    </div>
  );
}
