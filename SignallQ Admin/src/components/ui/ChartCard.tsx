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
      className="rounded-2xl p-5 transition-all duration-200"
      style={{
        backgroundColor: "var(--sq-bg-card)",
        border: "1px solid var(--sq-border)",
      }}
    >
      <div
        className="flex items-center justify-between gap-4 pb-4 mb-5 select-none"
        style={{ borderBottom: "1px solid var(--sq-border)" }}
      >
        <div>
          <h4
            className="text-xs font-semibold font-mono uppercase tracking-wider"
            style={{ color: "var(--sq-text-secondary)" }}
          >
            {title}
          </h4>
          {description && (
            <p className="text-[11px] mt-0.5" style={{ color: "var(--sq-text-tertiary)" }}>
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
