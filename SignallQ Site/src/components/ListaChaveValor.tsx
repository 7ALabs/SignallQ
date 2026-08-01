import React from "react";

export function ListaChaveValor({
  title,
  items,
}: {
  title: string;
  items: { label: string; value: string }[];
}) {
  return (
    <div>
      <div className="font-medium text-[11px] leading-[1.45] text-[color:var(--text-secondary)] tracking-[.3px] uppercase">
        {title}
      </div>
      <div className="mt-[10px] grid grid-cols-[auto_1fr] gap-x-3 gap-y-[6px] font-normal text-[12px] leading-[1.35] text-[color:var(--text-secondary)]">
        {items.map((item, idx) => (
          <React.Fragment key={idx}>
            <div>{item.label}</div>
            <div className="text-right text-[color:var(--text-primary)]">
              {item.value}
            </div>
          </React.Fragment>
        ))}
      </div>
    </div>
  );
}
