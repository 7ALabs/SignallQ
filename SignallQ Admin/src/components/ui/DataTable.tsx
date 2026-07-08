import React from "react";
import { alpha } from "../../utils/color";

export interface DataTableColumn<T> {
  header: string;
  accessor: (row: T) => React.ReactNode;
  className?: string;
  thClassName?: string;
}

interface DataTableProps<T> {
  data: T[];
  columns: DataTableColumn<T>[];
  keyExtractor: (row: T) => string;
  emptyMessage?: string;
  /** GH#552 (Fase 2): aceita string fixa ou função por linha (ex.: destacar a versão em foco). */
  rowClassName?: string | ((row: T) => string);
  onRowClick?: (row: T) => void;
  id?: string;
}

export function DataTable<T>({
  data,
  columns,
  keyExtractor,
  emptyMessage = "Não há elementos cadastrados no banco temporal.",
  rowClassName = "",
  onRowClick,
  id,
}: DataTableProps<T>) {
  return (
    <div
      id={id}
      className="w-full overflow-x-auto rounded-[8px]"
      style={{
        border: "1px solid var(--sq-border)",
        backgroundColor: "var(--sq-bg-card)",
      }}
    >
      <table className="min-w-[600px] w-full text-left border-collapse">
        <thead>
          <tr style={{ borderBottom: "1px solid var(--sq-border)", backgroundColor: "var(--sq-bg-card)" }}>
            {columns.map((col, idx) => (
              <th
                key={idx}
                className={`py-4 px-6 text-[11px] font-mono font-bold tracking-widest uppercase select-none ${col.thClassName || ""}`}
                style={{ color: "var(--sq-text-tertiary)" }}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="py-12 text-center text-xs"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row) => {
              const rowKey = keyExtractor(row);
              const resolvedRowClassName = typeof rowClassName === "function" ? rowClassName(row) : rowClassName;
              return (
                <tr
                  key={rowKey}
                  onClick={() => onRowClick && onRowClick(row)}
                  className={`transition-colors ${onRowClick ? "cursor-pointer" : ""} ${resolvedRowClassName}`}
                  style={{ borderTop: "1px solid var(--sq-border-subtle)" }}
                  onMouseEnter={(e) => {
                    (e.currentTarget as HTMLTableRowElement).style.backgroundColor = alpha("var(--sq-bg-overlay)", 50);
                  }}
                  onMouseLeave={(e) => {
                    (e.currentTarget as HTMLTableRowElement).style.backgroundColor = "";
                  }}
                >
                  {columns.map((col, cIdx) => (
                    <td
                      key={cIdx}
                      className={`py-4 px-6 text-xs ${col.className || ""}`}
                      style={{ color: "var(--sq-text-primary)" }}
                    >
                      {col.accessor(row)}
                    </td>
                  ))}
                </tr>
              );
            })
          )}
        </tbody>
      </table>
    </div>
  );
}
