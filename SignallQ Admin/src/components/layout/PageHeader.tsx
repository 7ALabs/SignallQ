import React from "react";

interface PageHeaderProps {
  title: string;
  description?: string;
  badge?: React.ReactNode;
  actions?: React.ReactNode;
  id?: string;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  description,
  badge,
  actions,
  id,
}) => {
  return (
    <div
      id={id || `page-header-${title.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 border-b border-zinc-800/60 mb-6 select-none"
    >
      <div className="space-y-1">
        <div className="flex items-center gap-2.5">
          <h2 className="text-xl font-bold font-sans text-white tracking-tight">
            {title}
          </h2>
          {badge}
        </div>
        {description && (
          <p className="text-xs text-neutral-400 font-sans leading-relaxed">
            {description}
          </p>
        )}
      </div>

      {actions && (
        <div className="flex items-center gap-3.5 self-start md:self-center shrink-0">
          {actions}
        </div>
      )}
    </div>
  );
};
