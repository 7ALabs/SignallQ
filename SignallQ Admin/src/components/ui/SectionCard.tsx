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
      className={`rounded-2xl overflow-hidden transition-all duration-200 ${className}`}
      style={{
        backgroundColor: "var(--sq-bg-card)",
        border: "1px solid var(--sq-border)",
      }}
    >
      {/* Card Header */}
      <div
        className="flex flex-col sm:flex-row sm:items-center sm:justify-between px-6 py-4 gap-4"
        style={{ borderBottom: "1px solid var(--sq-border)" }}
      >
        <div>
          <h3
            className="text-sm font-medium tracking-tight"
            style={{ color: "var(--sq-text-primary)" }}
          >
            {title}
          </h3>
          {description && (
            <p className="text-xs mt-1" style={{ color: "var(--sq-text-secondary)" }}>
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
