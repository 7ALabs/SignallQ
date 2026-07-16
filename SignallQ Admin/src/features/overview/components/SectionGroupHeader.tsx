import React from "react";

interface SectionGroupHeaderProps {
  label: string;
  dotColor: string;
  id?: string;
}

// Cabeçalho de agrupamento (dot + label uppercase + divisória) das seções
// "App" e "Rede & Operadora" do Centro de Controle — spec Lia,
// Md3DashboardContent.dc.html:20-24 e :62-67. dotColor usa os tokens já
// migrados pro protótipo md3-tobe (var(--primary) para App, var(--info) para
// Rede & Operadora — ver PR #1044), nunca hex solto.
export const SectionGroupHeader: React.FC<SectionGroupHeaderProps> = ({ label, dotColor, id }) => {
  return (
    <div id={id} className="flex items-center gap-2 select-none">
      <span
        className="w-2 h-2 rounded-full shrink-0"
        style={{ backgroundColor: dotColor }}
      />
      <span
        className="text-xs font-sans font-bold uppercase tracking-[0.08em]"
        style={{ color: "var(--text-primary)" }}
      >
        {label}
      </span>
      <span className="flex-1 h-px" style={{ backgroundColor: "var(--border)" }} />
    </div>
  );
};
