import React from "react";
import { AlertTriangle, X } from "lucide-react";

// Refs #1446 — primeiro uso de um dialog de confirmação modal no Console
// (publish/rollback de ruleset de diagnóstico são ações sensíveis e exigiam
// confirmação — não havia componente equivalente ainda). Radius 24px alinhado
// ao token de dialog do Design System (DESIGN.md, rounded.dialog).
interface ConfirmDialogProps {
  open: boolean;
  title: string;
  description: React.ReactNode;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: "default" | "danger";
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  id?: string;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  open,
  title,
  description,
  confirmLabel,
  cancelLabel = "Cancelar",
  tone = "default",
  busy = false,
  onConfirm,
  onCancel,
  id,
}) => {
  if (!open) return null;

  const accentColor = tone === "danger" ? "var(--error)" : "var(--primary)";
  const accentOnColor = tone === "danger" ? "var(--on-error-container)" : "var(--on-primary)";

  return (
    <div
      id={id}
      role="dialog"
      aria-modal="true"
      aria-labelledby={id ? `${id}-title` : undefined}
      className="fixed inset-0 z-50 flex items-center justify-center px-4"
    >
      <div
        className="absolute inset-0"
        style={{ backgroundColor: "rgba(0,0,0,0.55)" }}
        onClick={busy ? undefined : onCancel}
      />
      <div
        className="relative w-full max-w-md rounded-[24px] p-6"
        style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      >
        <div className="flex items-start gap-3">
          <div
            className="flex items-center justify-center w-10 h-10 rounded-full shrink-0"
            style={{ backgroundColor: `${accentColor}1A`, color: accentColor }}
          >
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 id={id ? `${id}-title` : undefined} className="text-[15px] font-semibold" style={{ color: "var(--text-primary)" }}>
              {title}
            </h2>
            <div className="text-[13px] leading-relaxed mt-1.5" style={{ color: "var(--text-secondary)" }}>
              {description}
            </div>
          </div>
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            aria-label="Fechar"
            className="shrink-0 p-1 rounded-lg cursor-pointer disabled:opacity-40"
            style={{ color: "var(--text-tertiary)" }}
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex items-center justify-end gap-3 mt-6">
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
          >
            {cancelLabel}
          </button>
          <button
            type="button"
            onClick={onConfirm}
            disabled={busy}
            className="px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: accentColor, color: accentOnColor }}
          >
            {busy ? "Processando..." : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
