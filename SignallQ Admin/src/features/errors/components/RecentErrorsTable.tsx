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
        <span className="font-mono font-bold text-zinc-500">{row.id.replace("err_", "")}</span>
      ),
    },
    {
      header: "Componente (Fonte)",
      accessor: (row: SystemError) => {
        let label = "ANDROID SDK";
        let color = "text-[#38BDF8] bg-[#38BDF8]/10 border-[#38BDF8]/15";
        if (row.source === "ai_gateway") {
          label = "AI GATEWAY";
          color = "text-purple-400 bg-purple-950/20 border-purple-500/15";
        } else if (row.source === "worker") {
          label = "EDGE WORKER";
          color = "text-amber-400 bg-amber-950/25 border-amber-500/15";
        } else if (row.source === "analytics_db") {
          label = "POSTGRES SQL";
          color = "text-emerald-400 bg-emerald-950/20 border-emerald-500/15";
        }
        return (
          <span className={`px-2 py-0.5 rounded-md font-mono text-[9px] font-bold border ${color}`}>
            {label}
          </span>
        );
      },
    },
    {
      header: "Mensagem de Erro",
      accessor: (row: SystemError) => (
        <span className="font-sans font-medium text-white block max-w-[280px] truncate" title={row.message}>
          {row.message}
        </span>
      ),
    },
    {
      header: "Eventos",
      accessor: (row: SystemError) => (
        <span className="font-mono text-zinc-400 font-semibold">{row.count}</span>
      ),
    },
    {
      header: "Usuários Afetados",
      accessor: (row: SystemError) => (
        <span className="font-mono text-zinc-400 font-semibold">{row.affectedUserCount}</span>
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
      title="Auditorias de Exceções Técnicas Recentes"
      description="Console central de crashes de borda Android, gargalos no AI Gateway e quedas de conexões Analytics DB."
    >
      <DataTable
        data={errors}
        columns={tableColumns}
        keyExtractor={(row) => row.id}
        emptyMessage="Nenhum dump técnico registrado para estes parâmetros."
        rowClassName="cursor-pointer font-sans"
        onRowClick={onSelectError}
        id="errors-logs-central-table"
      />
      <div className="mt-4 flex items-center gap-2 text-[10px] text-zinc-400 font-mono select-none">
        <Info className="w-3.5 h-3.5 text-zinc-550" />
        <span>Clique em qualquer linha acima para inspecionar o stacktrace e emitir comando manual de mitigação/resolução.</span>
      </div>
    </SectionCard>
  );
};
