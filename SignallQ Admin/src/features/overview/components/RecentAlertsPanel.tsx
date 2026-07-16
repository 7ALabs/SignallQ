import React from "react";
import { RecentAlertItem } from "../../../mocks/overview.mock";
import { alertCategoryLabel } from "../../../utils/alerts";

interface RecentAlertsPanelProps {
  alerts: RecentAlertItem[];
}

function severityFor(alert: RecentAlertItem & { _severity?: string }) {
  return alert._severity ?? alert.severity;
}

function dotColorFor(severity: string | undefined) {
  if (severity === "critical") return "var(--error)";
  if (severity === "info") return "var(--info)";
  return "var(--attention)";
}

// Badge de categoria (App / IA & Custos / Sistema / Não disponível) — spec
// Lia, Md3DashboardContent.dc.html:94,102,110. App e Sistema reaproveitam
// tokens já existentes (--nav-active-bg/fg, --bg-surface-muted/--text-secondary);
// IA & Custos usa o par --attention-container/--on-attention-container
// adicionado nesta mudança (não existia token pro âmbar do protótipo).
function badgeStyleFor(category: RecentAlertItem["category"]): React.CSSProperties {
  switch (category) {
    case "app":
      return { backgroundColor: "var(--nav-active-bg)", color: "var(--nav-active-fg)" };
    case "ia":
      return { backgroundColor: "var(--attention-container)", color: "var(--on-attention-container)" };
    case "sistema":
      return { backgroundColor: "var(--bg-surface-muted)", color: "var(--text-secondary)" };
    default:
      return { backgroundColor: "var(--bg-surface-muted)", color: "var(--text-tertiary)" };
  }
}

// Paridade com o mockup (signallq-admin-mockup.dc.html) — overline seguido de
// linhas simples (dot + badge de categoria + texto + horário) com border-top.
// No mobile o horário some e badge+texto ficam empilhados/corridos em vez de
// alinhados nas duas pontas (Md3DashboardContentMobile.dc.html:72-89).
export const RecentAlertsPanel: React.FC<RecentAlertsPanelProps> = ({ alerts }) => {
  return (
    <div className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[var(--radius-card)] p-5">
      <h4 className="text-xs font-semibold font-sans uppercase tracking-wider text-[var(--text-secondary)] mb-4 select-none">
        Alertas Recentes
      </h4>

      {alerts.length === 0 ? (
        <div className="py-8 text-center text-xs text-[var(--text-tertiary)] font-sans border border-dashed border-[var(--border)] rounded-[var(--radius-card)]">
          Sem alertas ativos
        </div>
      ) : (
        <div>
          {(alerts as Array<RecentAlertItem & { _severity?: string }>).map((alert) => {
            const sev = severityFor(alert);
            return (
              <div
                key={alert.id}
                className="flex items-start lg:items-center justify-between gap-4 py-3 border-t border-[var(--border)] first:border-t-0"
              >
                <div className="flex items-start lg:items-center gap-2.5 min-w-0">
                  <span
                    className="w-2 h-2 rounded-full shrink-0 mt-1.5 lg:mt-0"
                    style={{ backgroundColor: dotColorFor(sev) }}
                  />
                  <div className="min-w-0">
                    <span
                      className="text-[10px] font-sans font-bold px-1.5 py-0.5 rounded-md mr-1.5 align-middle"
                      style={badgeStyleFor(alert.category)}
                    >
                      {alertCategoryLabel(alert.category)}
                    </span>
                    <span className="text-xs text-[var(--text-primary)] font-sans">
                      {alert.message}
                    </span>
                  </div>
                </div>
                <span className="hidden lg:inline text-[10px] font-mono text-[var(--text-tertiary)] shrink-0">
                  {new Date(alert.timestamp).toLocaleString("pt-BR")}
                </span>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
