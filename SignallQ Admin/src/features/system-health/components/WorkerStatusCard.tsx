import React from "react";
import { CheckCircle, XCircle, Loader } from "lucide-react";
import { alpha } from "../../../utils/color";

interface WorkerHealth {
  name: string;
  status: "ok" | "error" | "loading";
  timestamp: string | null;
  latencyMs: number | null;
}

interface WorkerStatusCardProps {
  worker: WorkerHealth;
}

export const WorkerStatusCard: React.FC<WorkerStatusCardProps> = ({ worker }) => {
  const isOk = worker.status === "ok";
  const isLoading = worker.status === "loading";

  const statusColor = isOk
    ? "var(--sq-success, #22c55e)"
    : isLoading
    ? "var(--sq-warning, #f59e0b)"
    : "var(--sq-error, #ef4444)";

  const statusLabel = isOk ? "Online" : isLoading ? "Verificando" : "Offline";

  const formattedTs = worker.timestamp
    ? new Date(worker.timestamp).toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" })
    : null;

  return (
    <div
      className="p-4 rounded-xl flex items-start gap-3"
      style={{
        backgroundColor: "var(--bg-surface-hover)",
        border: `1px solid ${isOk ? alpha("var(--sq-success, #22c55e)", 20) : isLoading ? "var(--border)" : alpha("var(--sq-error, #ef4444)", 20)}`,
      }}
    >
      <div className="mt-0.5 shrink-0">
        {isOk ? (
          <CheckCircle className="w-5 h-5" style={{ color: statusColor }} />
        ) : isLoading ? (
          <Loader className="w-5 h-5 animate-spin" style={{ color: statusColor }} />
        ) : (
          <XCircle className="w-5 h-5" style={{ color: statusColor }} />
        )}
      </div>

      <div className="flex-1 min-w-0">
        <div className="flex items-center justify-between gap-2">
          <span
            className="text-[13px] font-semibold font-mono truncate"
            style={{ color: "var(--text-primary)" }}
          >
            {worker.name}
          </span>
          <span
            className="text-[10px] font-sans font-semibold uppercase tracking-wider px-2 py-0.5 rounded-full shrink-0"
            style={{
              color: statusColor,
              backgroundColor: alpha(statusColor, 12),
              border: `1px solid ${alpha(statusColor, 25)}`,
            }}
          >
            {statusLabel}
          </span>
        </div>

        <div className="mt-1.5 flex items-center gap-3 flex-wrap">
          {worker.latencyMs != null && (
            <span className="text-[11px] font-mono" style={{ color: "var(--text-secondary)" }}>
              {worker.latencyMs} ms
            </span>
          )}
          {formattedTs && (
            <span className="text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
              Verificado {formattedTs}
            </span>
          )}
          {!formattedTs && !worker.latencyMs && (
            <span className="text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
              Sem resposta
            </span>
          )}
        </div>
      </div>
    </div>
  );
};
