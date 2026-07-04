import React from "react";
import { AlertOctagon, CheckCircle2 } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { SystemHealthEvent } from "../../../services/systemHealthService";

interface LastEventsCardProps {
  lastFailure: SystemHealthEvent | null;
  lastSuccess: SystemHealthEvent | null;
  loading: boolean;
}

function formatTimestamp(ts: string | null): string {
  if (!ts) return "Sem registro";
  return new Date(ts).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// GH#425 — última falha e último sucesso vêm de system_errors e diagnostic_sessions no D1,
// não de placeholder. Ausência de registro é exibida como tal, não escondida.
export const LastEventsCard: React.FC<LastEventsCardProps> = ({
  lastFailure,
  lastSuccess,
  loading,
}) => {
  return (
    <SectionCard
      title="Últimos eventos"
      description="Última falha registrada em system_errors e último ingest bem-sucedido no D1."
      id="last-events-card"
    >
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {[0, 1].map((i) => (
            <div
              key={i}
              className="h-20 rounded-xl animate-pulse"
              style={{ backgroundColor: "var(--bg-surface-hover)" }}
            />
          ))}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div
            className="p-4 rounded-xl"
            style={{
              backgroundColor: "var(--bg-surface-hover)",
              border: "1px solid color-mix(in srgb, var(--sq-error, #ef4444) 20%, transparent)",
            }}
          >
            <div className="flex items-center gap-2">
              <AlertOctagon className="w-4 h-4" style={{ color: "var(--sq-error, #ef4444)" }} />
              <span className="text-[13px] font-semibold" style={{ color: "var(--text-primary)" }}>
                Última falha
              </span>
            </div>
            {lastFailure ? (
              <>
                <p className="mt-1.5 text-[11px] font-mono" style={{ color: "var(--text-secondary)" }}>
                  {lastFailure.source}: {lastFailure.message ?? "sem mensagem"}
                </p>
                <p className="mt-1 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
                  {formatTimestamp(lastFailure.timestamp)}
                </p>
              </>
            ) : (
              <p className="mt-1.5 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
                Nenhuma falha registrada.
              </p>
            )}
          </div>

          <div
            className="p-4 rounded-xl"
            style={{
              backgroundColor: "var(--bg-surface-hover)",
              border: "1px solid color-mix(in srgb, var(--sq-success, #22c55e) 20%, transparent)",
            }}
          >
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4" style={{ color: "var(--sq-success, #22c55e)" }} />
              <span className="text-[13px] font-semibold" style={{ color: "var(--text-primary)" }}>
                Último sucesso
              </span>
            </div>
            {lastSuccess ? (
              <>
                <p className="mt-1.5 text-[11px] font-mono" style={{ color: "var(--text-secondary)" }}>
                  {lastSuccess.source}
                </p>
                <p className="mt-1 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
                  {formatTimestamp(lastSuccess.timestamp)}
                </p>
              </>
            ) : (
              <p className="mt-1.5 text-[11px] font-sans" style={{ color: "var(--text-tertiary)" }}>
                Nenhum evento de sucesso registrado.
              </p>
            )}
          </div>
        </div>
      )}
    </SectionCard>
  );
};
