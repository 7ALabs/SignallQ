import React from "react";
import { Inbox } from "lucide-react";

interface EmptyStateProps {
  title?: string;
  description?: string;
  action?: React.ReactNode;
  id?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = "Nenhum resultado encontrado",
  description = "Ajuste os filtros de busca ou verifique se as credenciais do Android SDK foram registradas corretamente.",
  action,
  id,
}) => {
  return (
    <div
      id={id || "empty-state"}
      className="flex flex-col items-center justify-center text-center py-14 px-6 rounded-[18px] border border-dashed border-zinc-800 bg-zinc-950/20"
    >
      <div className="flex items-center justify-center w-12 h-12 rounded-xl bg-zinc-900 border border-zinc-800 text-zinc-500 mb-4 select-none">
        <Inbox className="w-5 h-5 text-neutral-400" />
      </div>

      <h4 className="text-sm font-medium text-white tracking-wide font-sans">{title}</h4>
      <p className="text-xs text-neutral-500 max-w-sm mt-1.5 leading-relaxed font-sans">
        {description}
      </p>

      {action && <div className="mt-5">{action}</div>}
    </div>
  );
};
