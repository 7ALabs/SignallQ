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
      className={`rounded-[var(--radius-card)] overflow-hidden sq-card-hover ${className}`}
      style={{
        backgroundColor: "var(--bg-surface)",
        border: "1px solid var(--border)",
      }}
    >
      {/* Card Header */}
      <div
        className="flex flex-col sm:flex-row sm:items-center sm:justify-between px-6 py-4 gap-4"
        style={{ borderBottom: "1px solid var(--border)" }}
      >
        <div>
          <h3
            className="text-[14px] font-semibold tracking-[-0.01em]"
            style={{ color: "var(--text-primary)" }}
          >
            {title}
          </h3>
          {description && (
            <p className="text-[13px] leading-[1.5] mt-1" style={{ color: "var(--text-secondary)" }}>
              {description}
            </p>
          )}
        </div>
        {actions && (
          <div className="flex items-center gap-2 self-start sm:self-center">{actions}</div>
        )}
      </div>

      {/* Card Body */}
      <div className="p-6">{children}</div>
    </div>
  );
};
