import React from "react";

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
  rowClassName?: string;
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
      className="w-full overflow-x-auto rounded-xl border border-[#262626] bg-[#111111]"
    >
      <table className="w-full text-left border-collapse">
        <thead>
          <tr className="border-b border-[#262626] bg-[#111111] sticky top-0">
            {columns.map((col, idx) => (
              <th
                key={idx}
                className={`py-4 px-6 text-[11px] font-mono font-bold tracking-widest text-[#6B7280] uppercase select-none ${
                  col.thClassName || ""
                }`}
              >
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="divide-y divide-[#18181B] font-sans">
          {data.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="py-12 text-center text-xs text-[#9CA3AF] font-sans"
              >
                {emptyMessage}
              </td>
            </tr>
          ) : (
            data.map((row) => {
              const rowKey = keyExtractor(row);
              return (
                <tr
                  key={rowKey}
                  onClick={() => onRowClick && onRowClick(row)}
                  className={`hover:bg-[#18181B]/50 transition-colors ${
                    onRowClick ? "cursor-pointer" : ""
                  } ${rowClassName}`}
                >
                  {columns.map((col, cIdx) => (
                    <td
                      key={cIdx}
                      className={`py-4 px-6 text-xs text-white ${col.className || ""}`}
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
