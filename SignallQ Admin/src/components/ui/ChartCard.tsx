import React from "react";

interface ChartCardProps {
  title: string;
  description?: string;
  children: React.ReactNode;
  actions?: React.ReactNode;
  id?: string;
}

export const ChartCard: React.FC<ChartCardProps> = ({
  title,
  description,
  children,
  actions,
  id,
}) => {
  return (
    <div
      id={id || `chart-card-${title.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className="rounded-[var(--radius-card)] p-5 sq-card-hover"
      style={{
        backgroundColor: "var(--bg-surface)",
        border: "1px solid var(--border)",
      }}
    >
      <div
        className="flex items-center justify-between gap-4 pb-4 mb-5 select-none"
        style={{ borderBottom: "1px solid var(--border)" }}
      >
        <div>
          <h4
            className="text-[11px] font-semibold font-sans uppercase tracking-[0.08em]"
            style={{ color: "var(--text-secondary)" }}
          >
            {title}
          </h4>
          {description && (
            <p className="text-[11px] mt-0.5" style={{ color: "var(--text-tertiary)" }}>
              {description}
            </p>
          )}
        </div>
        {actions && <div className="shrink-0">{actions}</div>}
      </div>

      <div className="relative select-none">{children}</div>
    </div>
  );
};
