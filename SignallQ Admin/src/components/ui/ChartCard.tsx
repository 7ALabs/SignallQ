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
      className="bg-[#111111] border border-[#262626] rounded-[18px] p-5 hover:border-[#363636] transition-all duration-200"
    >
      <div className="flex items-center justify-between gap-4 pb-4 border-b border-[#262626] mb-5 select-none">
        <div>
          <h4 className="text-xs font-semibold font-mono uppercase tracking-wider text-neutral-400">
            {title}
          </h4>
          {description && (
            <p className="text-[11px] text-zinc-500 mt-0.5 font-sans">
              {description}
            </p>
          )}
        </div>
        {actions && <div className="shrink-0">{actions}</div>}
      </div>

      <div className="relative font-sans select-none">
        {children}
      </div>
    </div>
  );
};
