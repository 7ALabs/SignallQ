import React from "react";

interface PageHeaderProps {
  title: string;
  description?: string;
  /**
   * SIG-294: legenda curta indicando de onde vem o dado desta tela (ex.: "D1
   * (diagnostic_sessions) via signallq-admin-worker"). Nenhuma tela operacional
   * deve deixar implícita a origem do número exibido — ver
   * `SignallQ Admin/docs/architecture/data-architecture.md`.
   */
  dataSource?: string;
  badge?: React.ReactNode;
  actions?: React.ReactNode;
  id?: string;
}

export const PageHeader: React.FC<PageHeaderProps> = ({
  title,
  description,
  dataSource,
  badge,
  actions,
  id,
}) => {
  return (
    <div
      id={id || `page-header-${title.toLowerCase().replace(/[^a-z0-9]/g, "-")}`}
      className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 pb-6 mb-6 select-none"
      style={{ borderBottom: "1px solid var(--sq-border)" }}
    >
      <div className="space-y-1">
        <div className="flex items-center gap-2.5">
          <h2
            className="text-xl font-bold tracking-tight"
            style={{ color: "var(--sq-text-primary)" }}
          >
            {title}
          </h2>
          {badge}
        </div>
        {description && (
          <p className="text-xs leading-relaxed" style={{ color: "var(--sq-text-secondary)" }}>
            {description}
          </p>
        )}
        {dataSource && (
          <p
            className="text-[10px] font-mono uppercase tracking-[0.04em]"
            style={{ color: "var(--sq-text-tertiary, var(--sq-text-secondary))" }}
          >
            Fonte de dados · {dataSource}
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
