import React from "react";
import { Severity } from "../../types/diagnostics";
import { alpha } from "../../utils/color";

interface StatusBadgeProps {
  status: Severity | "stable" | "beta" | "deprecated" | "halted" | "success" | "cached" | "failed" | string;
  customLabel?: string;
  className?: string;
  id?: string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, customLabel, className = "", id }) => {
  let style: React.CSSProperties = {
    backgroundColor: alpha("var(--text-tertiary)", 10),
    borderColor: alpha("var(--text-tertiary)", 20),
    color: "var(--text-secondary)",
  };
  let dotStyle: React.CSSProperties = { backgroundColor: "var(--text-tertiary)" };
  let dotClassName = "";
  let label = customLabel || status.toString().toUpperCase();

  switch (status) {
    case "ok":
    case "stable":
    case "success":
      style = {
        backgroundColor: alpha("var(--success)", 10),
        borderColor: alpha("var(--success)", 20),
        color: "var(--success)",
      };
      dotStyle = { backgroundColor: "var(--success)" };
      dotClassName = "animate-pulse";
      if (!customLabel) label = status === "ok" ? "OK" : status === "stable" ? "Estável" : "Sucesso";
      break;
    case "attention":
    case "beta":
    case "cached":
      style = {
        backgroundColor: alpha("var(--attention)", 10),
        borderColor: alpha("var(--attention)", 20),
        color: "var(--attention)",
      };
      dotStyle = { backgroundColor: "var(--attention)" };
      if (!customLabel) label = status === "attention" ? "Atenção" : status === "beta" ? "Beta" : "Cached";
      break;
    case "critical":
    case "failed":
    case "error":
    case "halted":
      style = {
        backgroundColor: alpha("var(--error)", 10),
        borderColor: alpha("var(--error)", 20),
        color: "var(--error)",
      };
      dotStyle = { backgroundColor: "var(--error)" };
      if (!customLabel) label = status === "critical" ? "Crítico" : status === "failed" ? "Erro" : "Pausado";
      break;
    case "deprecated":
      style = {
        backgroundColor: "var(--bg-surface)",
        borderColor: "var(--border)",
        color: "var(--text-tertiary)",
      };
      dotStyle = { backgroundColor: "var(--text-tertiary)" };
      if (!customLabel) label = "Obsoleto";
      break;
    case "info":
      style = {
        backgroundColor: alpha("var(--info)", 10),
        borderColor: alpha("var(--info)", 20),
        color: "var(--info)",
      };
      dotStyle = { backgroundColor: "var(--info)" };
      break;
  }

  return (
    <span
      id={id}
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-sans font-medium border uppercase tracking-[0.04em] select-none ${className}`}
      style={style}
    >
      <span className={`w-1.5 h-1.5 rounded-full ${dotClassName}`} style={dotStyle} />
      {label}
    </span>
  );
};
