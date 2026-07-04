import React from "react";
import { Database, CheckCircle, XCircle } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { alpha } from "../../../utils/color";

interface D1StatusCardProps {
  status: "connected" | "error" | "loading";
  lastQuery: string | null;
  loading: boolean;
}

export const D1StatusCard: React.FC<D1StatusCardProps> = ({ status, lastQuery, loading }) => {
  const isOk = status === "connected";

  const statusColor = isOk
    ? "var(--sq-success, #22c55e)"
    : "var(--sq-error, #ef4444)";

  const formattedTs = lastQuery
    ? new Date(lastQuery).toLocaleString("pt-BR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit",
      })
    : null;

  return (
    <SectionCard
      title="D1 Database"
      description="Banco de dados Cloudflare D1 (SQLite) — armazena sessões de diagnóstico, uso de IA e alertas."
      id="d1-status-card"
    >
      {loading ? (
        <div
          className="h-16 rounded-xl animate-pulse"
          style={{ backgroundColor: "var(--bg-surface-hover)" }}
        />
      ) : (
        <div className="flex items-center gap-4">
          <div
            className="p-3 rounded-xl"
            style={{
              backgroundColor: alpha(statusColor, 10),
              border: `1px solid ${alpha(statusColor, 20)}`,
            }}
          >
            <Database className="w-5 h-5" style={{ color: statusColor }} />
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              {isOk ? (
                <CheckCircle className="w-4 h-4 shrink-0" style={{ color: statusColor }} />
              ) : (
                <XCircle className="w-4 h-4 shrink-0" style={{ color: statusColor }} />
              )}
              <span
                className="text-[13px] font-semibold"
                style={{ color: "var(--text-primary)" }}
              >
                {isOk ? "Conectado" : "Erro de conexão"}
              </span>
            </div>
            {formattedTs && (
              <p className="mt-1 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
                Última query bem-sucedida: {formattedTs}
              </p>
            )}
            {!formattedTs && (
              <p className="mt-1 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
                Sem registro de query recente
              </p>
            )}
          </div>

          <span
            className="text-[11px] font-sans font-semibold uppercase tracking-wider px-2 py-1 rounded-lg shrink-0"
            style={{
              color: statusColor,
              backgroundColor: alpha(statusColor, 12),
              border: `1px solid ${alpha(statusColor, 25)}`,
            }}
          >
            {isOk ? "Online" : "Offline"}
          </span>
        </div>
      )}
    </SectionCard>
  );
};
