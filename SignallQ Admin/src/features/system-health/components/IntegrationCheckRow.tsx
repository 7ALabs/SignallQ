import React from "react";
import { CheckCircle, XCircle, MinusCircle, Clock } from "lucide-react";
import { HealthStatus } from "../../../services/systemHealthService";

interface IntegrationCheckRowProps {
  label: string;
  status: HealthStatus;
  detail?: string | null;
  message?: string;
}

const STATUS_META: Record<HealthStatus, { color: string; label: string; icon: React.ReactNode }> = {
  ok: {
    color: "var(--sq-success, #22c55e)",
    label: "OK",
    icon: <CheckCircle className="w-4 h-4" />,
  },
  error: {
    color: "var(--sq-error, #ef4444)",
    label: "Erro",
    icon: <XCircle className="w-4 h-4" />,
  },
  not_configured: {
    color: "var(--text-tertiary)",
    label: "Não configurado",
    icon: <MinusCircle className="w-4 h-4" />,
  },
  idle: {
    color: "var(--sq-warning, #f59e0b)",
    label: "Inativo",
    icon: <Clock className="w-4 h-4" />,
  },
};

// GH#425 — cada linha reflete uma verificação real feita pelo worker (/admin/system-health).
// Sem status "sempre verde": not_configured e idle são estados legítimos, não erro escondido.
export const IntegrationCheckRow: React.FC<IntegrationCheckRowProps> = ({
  label,
  status,
  detail,
  message,
}) => {
  const meta = STATUS_META[status];

  return (
    <div
      className="flex items-start gap-3 p-3.5 rounded-xl"
      style={{
        backgroundColor: "var(--bg-surface-hover)",
        border: `1px solid color-mix(in srgb, ${meta.color} 20%, transparent)`,
      }}
    >
      <div className="mt-0.5 shrink-0" style={{ color: meta.color }}>
        {meta.icon}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <span className="text-[13px] font-semibold" style={{ color: "var(--text-primary)" }}>
            {label}
          </span>
          <span
            className="text-[10px] font-sans font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full shrink-0"
            style={{
              color: meta.color,
              backgroundColor: `color-mix(in srgb, ${meta.color} 12%, transparent)`,
              border: `1px solid color-mix(in srgb, ${meta.color} 25%, transparent)`,
            }}
          >
            {meta.label}
          </span>
        </div>
        {detail && (
          <p className="mt-1 text-[11px] font-mono" style={{ color: "var(--text-secondary)" }}>
            {detail}
          </p>
        )}
        {message && (
          <p className="mt-1 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
            {message}
          </p>
        )}
      </div>
    </div>
  );
};
