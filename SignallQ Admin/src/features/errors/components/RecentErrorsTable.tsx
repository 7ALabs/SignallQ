import React from "react";
import { SystemError } from "../../../types/errors";
import { DataTable } from "../../../components/ui/DataTable";
import { SectionCard } from "../../../components/ui/SectionCard";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { Info } from "lucide-react";

interface RecentErrorsTableProps {
  errors: SystemError[];
  selectedError: SystemError | null;
  onSelectError: (error: SystemError) => void;
}

export const RecentErrorsTable: React.FC<RecentErrorsTableProps> = ({
  errors,
  selectedError,
  onSelectError,
}) => {
  const tableColumns = [
    {
      header: "Caso ID",
      accessor: (row: SystemError) => (
        <span className="font-mono font-bold text-[var(--text-tertiary)]">{row.id.replace("err_", "")}</span>
      ),
    },
    {
      header: "Componente (Fonte)",
      accessor: (row: SystemError) => {
        let label = "ANDROID SDK";
        let color = "text-[var(--info)] bg-[var(--info)]/10 border-[var(--info)]/15";
        if (row.source === "ai_gateway") {
          label = "AI GATEWAY";
          color = "text-[var(--text-secondary)] bg-[var(--bg-surface-muted)] border-[var(--border)]";
        } else if (row.source === "worker") {
          label = "EDGE WORKER";
          color = "text-amber-400 bg-amber-950/25 border-amber-500/15";
        } else if (row.source === "analytics_db") {
          label = "POSTGRES SQL";
          color = "text-emerald-400 bg-emerald-950/20 border-emerald-500/15";
        }
        return (
          <span className={`px-2 py-0.5 rounded-md font-sans text-[9px] font-bold border ${color}`}>
            {label}
          </span>
        );
      },
    },
    {
      header: "Categoria",
      accessor: (row: SystemError) => {
        const category = row.category ?? "backend";
        const labels: Record<string, string> = {
          app: "APP",
          backend: "BACKEND",
          ia: "IA",
          integration: "INTEGRAÇÃO",
        };
        const colors: Record<string, string> = {
          app: "text-emerald-400 bg-emerald-950/20 border-emerald-500/15",
          backend: "text-amber-400 bg-amber-950/25 border-amber-500/15",
          ia: "text-[var(--text-secondary)] bg-[var(--bg-surface-muted)] border-[var(--border)]",
          integration: "text-[var(--info)] bg-[var(--info)]/10 border-[var(--info)]/15",
        };
        return (
          <span className={`px-2 py-0.5 rounded-md font-sans text-[9px] font-bold border ${colors[category] ?? colors.backend}`}>
            {labels[category] ?? category.toUpperCase()}
          </span>
        );
      },
    },
    {
      header: "Mensagem de Erro",
      accessor: (row: SystemError) => (
        <span className="font-sans font-medium text-[var(--text-primary)] block max-w-[280px] truncate" title={row.message}>
          {row.message}
        </span>
      ),
    },
    {
      header: "Eventos",
      accessor: (row: SystemError) => (
        <span className="font-mono text-[var(--text-secondary)] font-semibold">{row.count}</span>
      ),
    },
    {
      header: "Usuários Afetados",
      accessor: (row: SystemError) => (
        <span className="font-mono text-[var(--text-secondary)] font-semibold">{row.affectedUserCount}</span>
      ),
    },
    {
      header: "Status",
      accessor: (row: SystemError) => {
        const severity = row.resolved ? "ok" : "critical";
        return (
          <StatusBadge
            status={severity}
            customLabel={row.resolved ? "RESOLVIDO" : "ATIVO"}
          />
        );
      },
    },
  ];

  return (
    <SectionCard
      title="Erros recentes do sistema"
      description="Erros do app Android, do gateway de IA e do banco de analytics, em um só lugar."
    >
      <DataTable
        data={errors}
        columns={tableColumns}
        keyExtractor={(row) => row.id}
        emptyMessage="Nenhum erro registrado para estes parâmetros."
        rowClassName={(row) =>
          `cursor-pointer font-sans ${selectedError?.id === row.id ? "bg-[var(--primary)]/5" : ""}`
        }
        onRowClick={onSelectError}
        id="errors-logs-central-table"
      />
      <div className="mt-4 flex items-center gap-2 text-[10px] text-[var(--text-secondary)] font-sans select-none">
        <Info className="w-3.5 h-3.5 text-[var(--text-tertiary)]" />
        <span>Clique em qualquer linha acima para inspecionar o stacktrace e emitir comando manual de mitigação/resolução.</span>
      </div>
    </SectionCard>
  );
};
