import React from "react";
import { DataTable, DataTableColumn } from "../../../components/ui/DataTable";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { DiagnosticDivergenceRecord } from "../../../types/diagnosticDivergences";
import { CLASSIFICATION_LABEL } from "./DivergencesSummaryCards";

interface DivergencesTableProps {
  items: DiagnosticDivergenceRecord[];
  id?: string;
}

function badgeStatus(classification: DiagnosticDivergenceRecord["classification"]): string {
  switch (classification) {
    case "EXACT_MATCH":
    case "EQUIVALENT_RESULT":
      return "success";
    case "MINOR_DIVERGENCE":
      return "attention";
    default:
      return "critical";
  }
}

function formatDate(value: string): string {
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

export const DivergencesTable: React.FC<DivergencesTableProps> = ({ items, id }) => {
  const columns: DataTableColumn<DiagnosticDivergenceRecord>[] = [
    {
      header: "Classificação",
      accessor: (row) => <StatusBadge status={badgeStatus(row.classification)} customLabel={CLASSIFICATION_LABEL[row.classification]} />,
    },
    {
      header: "Execução",
      accessor: (row) => <span className="font-mono">{row.executionId}</span>,
    },
    {
      header: "Local",
      accessor: (row) => (
        <span>
          {row.localStatus ?? "—"} · {row.localScore ?? "—"} · {row.localFlow ?? "—"}
        </span>
      ),
    },
    {
      header: "Remoto",
      accessor: (row) => (
        <span>
          {row.remoteStatus ?? "—"} · {row.remoteScore ?? "—"} · {row.remoteFlow ?? "—"}
        </span>
      ),
    },
    {
      header: "Duração (local/remoto)",
      accessor: (row) => `${row.localDurationMs ?? "—"}ms / ${row.remoteDurationMs ?? "—"}ms`,
      className: "font-mono",
    },
    {
      header: "App",
      accessor: (row) => row.appVersion ?? "—",
    },
    {
      header: "Data",
      accessor: (row) => formatDate(row.createdAt),
    },
  ];

  return (
    <DataTable
      id={id}
      data={items}
      columns={columns}
      keyExtractor={(row) => row.id}
      emptyMessage="Nenhuma divergência registrada para este filtro."
    />
  );
};
