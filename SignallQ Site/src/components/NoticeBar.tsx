export function NoticeBar({
  title,
  message,
  icon = "warning",
  type = "warning", // "warning" | "error" | "info"
}: {
  title: string;
  message: string;
  icon?: string;
  type?: "warning" | "error" | "info";
}) {
  let bgClass = "bg-[color-mix(in_srgb,_var(--warning)_10%,_transparent)]";
  let colorClass = "text-[color:var(--warning)]";

  if (type === "error") {
    bgClass = "bg-[color-mix(in_srgb,_var(--error)_10%,_transparent)]";
    colorClass = "text-[color:var(--error)]";
  } else if (type === "info") {
    bgClass = "bg-[color-mix(in_srgb,_var(--accent)_10%,_transparent)]";
    colorClass = "text-[color:var(--accent)]";
  }

  return (
    <div
      className={`flex items-start gap-2 rounded-xl py-3 px-[14px] ${bgClass}`}
    >
      <span className={`material-symbols-outlined text-[16px] ${colorClass}`}>
        {icon}
      </span>
      <div className="font-normal text-[12px] leading-[1.4] text-[color:var(--text-secondary)]">
        <b className="text-[color:var(--text-primary)] font-bold">{title} </b>
        {message}
      </div>
    </div>
  );
}
