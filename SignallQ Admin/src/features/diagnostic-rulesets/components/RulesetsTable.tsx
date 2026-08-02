import React from "react";
import { DataTable, DataTableColumn } from "../../../components/ui/DataTable";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { RulesetListItem } from "../../../types/diagnosticRulesets";

interface RulesetsTableProps {
  items: RulesetListItem[];
  selectedVersion: number | null;
  onSelect: (version: number) => void;
  id?: string;
}

function statusToBadge(status: RulesetListItem["status"]): { status: string; label: string } {
  switch (status) {
    case "PUBLISHED":
      return { status: "success", label: "Publicado" };
    case "DRAFT":
      return { status: "info", label: "Rascunho" };
    case "ROLLED_BACK":
      return { status: "halted", label: "Revertido" };
    default:
      return { status: "cached", label: "Bundled local" };
  }
}

function formatDate(value: string | null): string {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

export const RulesetsTable: React.FC<RulesetsTableProps> = ({ items, selectedVersion, onSelect, id }) => {
  const columns: DataTableColumn<RulesetListItem>[] = [
    {
      header: "Versão",
      accessor: (row) => (
        <span className="font-mono font-semibold" style={{ color: "var(--text-primary)" }}>
          v{row.version}
        </span>
      ),
    },
    {
      header: "Status",
      accessor: (row) => {
        const badge = statusToBadge(row.status);
        return <StatusBadge status={badge.status} customLabel={badge.label} />;
      },
    },
    {
      header: "Rollout",
      accessor: (row) => `${row.rolloutPercent}%`,
    },
    {
      header: "Schema / Engine",
      accessor: (row) => `v${row.schemaVersion} / v${row.engineVersion}`,
      className: "font-mono",
    },
    {
      header: "Publicado em",
      accessor: (row) => formatDate(row.publishedAt),
    },
    {
      header: "Atualizado em",
      accessor: (row) => formatDate(row.updatedAt),
    },
  ];

  return (
    <DataTable
      id={id}
      data={items}
      columns={columns}
      keyExtractor={(row) => String(row.version)}
      onRowClick={(row) => onSelect(row.version)}
      rowClassName={(row) => (row.version === selectedVersion ? "outline outline-2 outline-offset-[-2px] outline-[var(--primary)]" : "")}
      emptyMessage="Nenhum ruleset cadastrado ainda."
    />
  );
};
