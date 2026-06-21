import React from "react";

interface SectionCardProps {
  title: string;
  description?: string;
  actions?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  id?: string;
}

export const SectionCard: React.FC<SectionCardProps> = ({
  title,
  description,
  actions,
  children,
  className = "",
  id,
}) => {
  return (
    <div
      id={id || `section-${title.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className={`bg-[#111111] border border-[#262626] rounded-[18px] overflow-hidden transition-all duration-200 ${className}`}
    >
      {/* Card Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between px-6 py-4 border-b border-[#262626] gap-4">
        <div>
          <h3 className="text-sm font-medium font-sans text-white tracking-tight">
            {title}
          </h3>
          {description && (
            <p className="text-xs text-neutral-400 mt-1 font-sans">
              {description}
            </p>
          )}
        </div>
        {actions && <div className="flex items-center gap-2 self-start sm:self-center">{actions}</div>}
      </div>

      {/* Card Body */}
      <div className="p-6">{children}</div>
    </div>
  );
};
