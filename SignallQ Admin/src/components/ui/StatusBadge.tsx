import React from "react";
import { Severity } from "../../types/diagnostics";

interface StatusBadgeProps {
  status: Severity | "stable" | "beta" | "deprecated" | "halted" | "success" | "cached" | "failed" | string;
  customLabel?: string;
  className?: string;
  id?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, customLabel, className = "", id }) => {
  // Map severity, status keywords or version codes to technical visual styles
  let bgClass = "bg-neutral-800/80 border-neutral-700 text-neutral-300";
  let dotClass = "bg-neutral-500";
  let label = customLabel || status.toString().toUpperCase();

  switch (status) {
    case "ok":
    case "stable":
    case "success":
      bgClass = "bg-[#22C55E]/10 border-[#22C55E]/20 text-[#22C55E]";
      dotClass = "bg-[#22C55E] animate-pulse";
      if (!customLabel) label = status === "ok" ? "OK" : status === "stable" ? "Estável" : "Sucesso";
      break;
    case "attention":
    case "beta":
    case "cached":
      bgClass = "bg-[#F5A623]/10 border-[#F5A623]/20 text-[#F5A623]";
      dotClass = "bg-[#F5A623]";
      if (!customLabel) label = status === "attention" ? "Atenção" : status === "beta" ? "Beta" : "Cached";
      break;
    case "critical":
    case "failed":
    case "error":
    case "halted":
      bgClass = "bg-[#FF4D4F]/10 border-[#FF4D4F]/20 text-[#FF4D4F]";
      dotClass = "bg-[#FF4D4F]";
      if (!customLabel) label = status === "critical" ? "Crítico" : status === "failed" ? "Erro" : "Pausado";
      break;
    case "deprecated":
      bgClass = "bg-[#111111] border-[#262626] text-zinc-500";
      dotClass = "bg-zinc-600";
      if (!customLabel) label = "Obsoleto";
      break;
    case "info":
      bgClass = "bg-[#38BDF8]/10 border-[#38BDF8]/20 text-[#38BDF8]";
      dotClass = "bg-[#38BDF8]";
      break;
  }

  return (
    <span
      id={id}
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-mono font-medium border uppercase tracking-wider select-none ${bgClass} ${className}`}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${dotClass}`} />
      {label}
    </span>
  );
};
