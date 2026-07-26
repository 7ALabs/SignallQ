import React from "react";
import { RulesetDetail } from "../../../types/diagnosticRulesets";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";

interface RulesetDetailPanelProps {
  detail: RulesetDetail | null;
  loading: boolean;
  onEditAsDraft: (detail: RulesetDetail) => void;
  onPublish: (version: number) => void;
  onAdjustRollout: (version: number, currentPercent: number) => void;
  onRollback: (version: number) => void;
  onSimulate: (detail: RulesetDetail) => void;
  id?: string;
}

function formatDate(value: string | null): string {
  if (!value) return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

export const RulesetDetailPanel: React.FC<RulesetDetailPanelProps> = ({
  detail,
  loading,
  onEditAsDraft,
  onPublish,
  onAdjustRollout,
  onRollback,
  onSimulate,
  id,
}) => {
  if (loading) return <LoadingState message="Carregando ruleset..." rows={4} />;
  if (!detail) {
    return <EmptyState title="Selecione um ruleset" description="Clique em uma linha da tabela para ver o detalhe, editar como novo draft, validar e simular." />;
  }

  return (
    <div id={id} className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-[15px] font-semibold" style={{ color: "var(--text-primary)" }}>
              Ruleset v{detail.version}
            </h3>
            <StatusBadge
              status={detail.status === "PUBLISHED" ? "success" : detail.status === "DRAFT" ? "info" : "halted"}
              customLabel={detail.status === "PUBLISHED" ? "Publicado" : detail.status === "DRAFT" ? "Rascunho" : "Revertido"}
            />
          </div>
          <p className="text-[12px] mt-1" style={{ color: "var(--text-tertiary)" }}>
            Schema v{detail.schemaVersion} · Engine v{detail.engineVersion} · Rollout {detail.rolloutPercent}%
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => onEditAsDraft(detail)}
            className="px-3.5 py-2 rounded-xl text-xs font-semibold cursor-pointer"
            style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
          >
            Editar como novo draft
          </button>
          <button
            type="button"
            onClick={() => onSimulate(detail)}
            className="px-3.5 py-2 rounded-xl text-xs font-semibold cursor-pointer"
            style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
          >
            Simular
          </button>
          {detail.status === "DRAFT" && (
            <button
              type="button"
              onClick={() => onPublish(detail.version)}
              className="px-3.5 py-2 rounded-xl text-xs font-semibold cursor-pointer"
              style={{ backgroundColor: "var(--primary)", color: "var(--on-primary)" }}
            >
              Publicar
            </button>
          )}
          {detail.status === "PUBLISHED" && (
            <>
              <button
                type="button"
                onClick={() => onAdjustRollout(detail.version, detail.rolloutPercent)}
                className="px-3.5 py-2 rounded-xl text-xs font-semibold cursor-pointer"
                style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
              >
                Ajustar rollout
              </button>
              <button
                type="button"
                onClick={() => onRollback(detail.version)}
                className="px-3.5 py-2 rounded-xl text-xs font-semibold cursor-pointer"
                style={{ backgroundColor: "var(--error)", color: "var(--on-error-container)" }}
              >
                Rollback
              </button>
            </>
          )}
        </div>
      </div>

      {/* Trilha de auditoria — só o que o worker realmente expõe por versão
          (author/justification/publishedAt/updatedAt, ver ruleset-store.ts).
          Não existe endpoint que liste diagnostic_rule_audit_log completo
          ainda; por isso o rótulo é explícito sobre a limitação. */}
      <div
        className="rounded-[var(--radius-card)] p-4 grid grid-cols-1 sm:grid-cols-2 gap-4"
        style={{ backgroundColor: "var(--bg-surface-variant)", border: "1px solid var(--border)" }}
      >
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
            Autor
          </p>
          <p className="text-[13px] mt-0.5" style={{ color: "var(--text-primary)" }}>
            {detail.author ?? "—"}
          </p>
        </div>
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
            Publicado em
          </p>
          <p className="text-[13px] mt-0.5" style={{ color: "var(--text-primary)" }}>
            {formatDate(detail.publishedAt)}
          </p>
        </div>
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
            Última atualização
          </p>
          <p className="text-[13px] mt-0.5" style={{ color: "var(--text-primary)" }}>
            {formatDate(detail.updatedAt)}
          </p>
        </div>
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
            Justificativa
          </p>
          <p className="text-[13px] mt-0.5" style={{ color: "var(--text-primary)" }}>
            {detail.justification || "—"}
          </p>
        </div>
        <p className="text-[10px] leading-relaxed sm:col-span-2" style={{ color: "var(--text-tertiary)" }}>
          Mostra a última ação registrada nesta versão (autor, publicação, atualização). O worker
          ainda não expõe o histórico completo de auditoria (tabela diagnostic_rule_audit_log)
          por um endpoint de leitura.
        </p>
      </div>

      <div>
        <p className="text-[10px] font-semibold uppercase tracking-[0.06em] mb-2" style={{ color: "var(--text-tertiary)" }}>
          Regras ({detail.ruleset.rules?.length ?? 0})
        </p>
        <pre
          className="text-[11px] font-mono leading-relaxed p-4 rounded-[var(--radius-card)] overflow-auto max-h-[420px]"
          style={{ backgroundColor: "var(--bg-base)", border: "1px solid var(--border)", color: "var(--text-secondary)" }}
        >
          {JSON.stringify(detail.ruleset, null, 2)}
        </pre>
      </div>
    </div>
  );
};
