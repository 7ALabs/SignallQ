import React from "react";
import { AlertTriangle, X } from "lucide-react";
import { RolloutConfigInput } from "../../../types/diagnosticRulesets";
import { alpha } from "../../../utils/color";

// Refs #1446 (rollout) — substitui o ConfirmDialog genérico para publish/ajuste de
// rollout: as duas ações exigem escolher `rolloutPercent` explicitamente, sempre
// começando de 0 no publish (nunca um default silencioso de 100%). Rollback continua
// no ConfirmDialog simples (não tem parâmetro a escolher).
interface RolloutDialogProps {
  open: boolean;
  mode: "publish" | "rollout";
  version: number;
  /** Só usado em mode="rollout", pra pré-preencher o valor atual do PUBLISHED. */
  currentPercent?: number;
  busy?: boolean;
  error?: string | null;
  onConfirm: (config: RolloutConfigInput) => void;
  onCancel: () => void;
  id?: string;
}

export const RolloutDialog: React.FC<RolloutDialogProps> = ({
  open,
  mode,
  version,
  currentPercent,
  busy = false,
  error,
  onConfirm,
  onCancel,
  id,
}) => {
  const [percent, setPercent] = React.useState<number>(mode === "rollout" ? (currentPercent ?? 0) : 0);
  const [percentInput, setPercentInput] = React.useState<string>(String(mode === "rollout" ? (currentPercent ?? 0) : 0));

  React.useEffect(() => {
    if (open) {
      const initial = mode === "rollout" ? (currentPercent ?? 0) : 0;
      setPercent(initial);
      setPercentInput(String(initial));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, mode, version]);

  if (!open) return null;

  const title = mode === "publish" ? `Publicar ruleset v${version}?` : `Ajustar rollout do ruleset v${version}`;
  const confirmLabel = mode === "publish" ? "Publicar" : "Salvar rollout";

  const clampedInvalid = Number.isNaN(percent) || percent < 0 || percent > 100;

  const handlePercentChange = (raw: string) => {
    setPercentInput(raw);
    const n = Number(raw);
    setPercent(n);
  };

  const handleConfirm = () => {
    if (clampedInvalid) return;
    onConfirm({ rolloutPercent: percent });
  };

  return (
    <div
      id={id}
      role="dialog"
      aria-modal="true"
      aria-labelledby={id ? `${id}-title` : undefined}
      className="fixed inset-0 z-50 flex items-center justify-center px-4"
    >
      <div className="absolute inset-0" style={{ backgroundColor: "rgba(0,0,0,0.55)" }} onClick={busy ? undefined : onCancel} />
      <div
        className="relative w-full max-w-md rounded-[24px] p-6"
        style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      >
        <div className="flex items-start gap-3">
          <div
            className="flex items-center justify-center w-10 h-10 rounded-full shrink-0"
            style={{ backgroundColor: alpha("var(--primary)", 10), color: "var(--primary)" }}
          >
            <AlertTriangle className="w-5 h-5" />
          </div>
          <div className="min-w-0 flex-1">
            <h2 id={id ? `${id}-title` : undefined} className="text-[15px] font-semibold" style={{ color: "var(--text-primary)" }}>
              {title}
            </h2>
            <p className="text-[13px] leading-relaxed mt-1.5" style={{ color: "var(--text-secondary)" }}>
              {mode === "publish"
                ? "Isso substitui o ruleset atualmente publicado e despublica o anterior. O percentual abaixo define quantas instalações participam do shadow mode (comparação local-vs-remoto) — não é quem vê o resultado remoto como definitivo. Essa ação fica registrada na trilha de auditoria."
                : "Só altera o percentual/segmentação em vigor — não cria versão nova nem troca o conteúdo do ruleset. Some pra 0% remove instalações do shadow mode imediatamente, sem redeploy."}
            </p>
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

        <div className="mt-5">
          <label htmlFor={id ? `${id}-percent` : undefined} className="text-[11px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
            Rollout (% de shadow mode)
          </label>
          <div className="mt-1.5 flex items-center gap-3">
            <input
              id={id ? `${id}-percent` : undefined}
              type="number"
              min={0}
              max={100}
              value={percentInput}
              onChange={(e) => handlePercentChange(e.target.value)}
              disabled={busy}
              className="w-24 px-3 py-2 rounded-xl text-[13px] font-mono"
              style={{ backgroundColor: "var(--bg-base)", border: `1px solid ${clampedInvalid ? "var(--error)" : "var(--border)"}`, color: "var(--text-primary)" }}
            />
            <span className="text-[13px]" style={{ color: "var(--text-tertiary)" }}>%</span>
          </div>
          {clampedInvalid && (
            <p className="text-[11px] mt-1.5" style={{ color: "var(--error)" }}>
              Informe um número entre 0 e 100.
            </p>
          )}
          {!clampedInvalid && percent === 0 && (
            <p className="text-[11px] mt-1.5" style={{ color: "var(--text-tertiary)" }}>
              0% = nenhuma instalação participa do shadow mode ainda — modo dark, só a trilha de auditoria registra a publicação.
            </p>
          )}
          {!clampedInvalid && percent > 0 && (
            <p className="text-[11px] mt-1.5" style={{ color: "var(--attention)" }}>
              {percent}% das instalações elegíveis vão comparar o resultado local com o resultado deste ruleset em produção.
            </p>
          )}
        </div>

        {error && (
          <p className="text-[12px] mt-3" style={{ color: "var(--error)" }}>
            {error}
          </p>
        )}

        <div className="flex items-center justify-end gap-3 mt-6">
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
          >
            Cancelar
          </button>
          <button
            type="button"
            onClick={handleConfirm}
            disabled={busy || clampedInvalid}
            className="px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--primary)", color: "var(--on-primary)" }}
          >
            {busy ? "Processando..." : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};
