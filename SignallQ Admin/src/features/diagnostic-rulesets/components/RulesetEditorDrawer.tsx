import React from "react";
import { X, CheckCircle2, AlertCircle } from "lucide-react";
import { diagnosticRulesetsService } from "../../../services/diagnosticRulesetsService";
import { DiagnosticRuleset } from "../../../types/diagnosticRulesets";

interface RulesetEditorDrawerProps {
  open: boolean;
  initialRuleset: DiagnosticRuleset | null;
  onClose: () => void;
  onCreated: (version: number) => void;
  id?: string;
}

type ParseState = { ok: true; value: DiagnosticRuleset } | { ok: false; error: string };

function tryParse(text: string): ParseState {
  try {
    const value = JSON.parse(text);
    if (!value || typeof value !== "object" || Array.isArray(value)) {
      return { ok: false, error: "O JSON precisa ser um objeto (não array/primitivo)." };
    }
    return { ok: true, value: value as DiagnosticRuleset };
  } catch (e: any) {
    return { ok: false, error: `JSON inválido: ${e?.message ?? "erro de sintaxe"}.` };
  }
}

// Refs #1446 — "editor de draft (JSON estruturado, não textarea livre sem
// validação)": o campo em si é um textarea (não existe editor visual de regra
// no MVP, explicitamente fora de escopo da issue), mas nunca aceita texto sem
// checagem — parse de sintaxe local a cada digitação + validação de schema
// contra o worker antes de permitir criar o draft.
export const RulesetEditorDrawer: React.FC<RulesetEditorDrawerProps> = ({
  open,
  initialRuleset,
  onClose,
  onCreated,
  id,
}) => {
  const [text, setText] = React.useState("");
  const [justification, setJustification] = React.useState("");
  const [validating, setValidating] = React.useState(false);
  const [submitting, setSubmitting] = React.useState(false);
  const [validation, setValidation] = React.useState<{ ok: boolean; message: string } | null>(null);
  const [submitError, setSubmitError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!open) return;
    const seed = initialRuleset
      ? { ...initialRuleset, version: initialRuleset.version + 1 }
      : { version: 1, schemaVersion: 3, engineVersion: 1, publishedAt: new Date().toISOString(), rules: [] };
    setText(JSON.stringify(seed, null, 2));
    setJustification("");
    setValidation(null);
    setSubmitError(null);
  }, [open, initialRuleset]);

  if (!open) return null;

  const parsed = tryParse(text);
  const parseError = parsed.ok === false ? parsed.error : null;

  const handleValidate = async () => {
    const current = tryParse(text);
    if (current.ok === false) {
      setValidation({ ok: false, message: current.error });
      return;
    }
    setValidating(true);
    setValidation(null);
    try {
      const result = await diagnosticRulesetsService.validateRuleset(current.value);
      if (result.ok) {
        setValidation({ ok: true, message: `Válido — versão ${result.version}, ${result.rules} regra(s).` });
      } else {
        setValidation({ ok: false, message: (result.errors ?? ["Ruleset inválido."]).join(" · ") });
      }
    } finally {
      setValidating(false);
    }
  };

  const handleCreate = async () => {
    const current = tryParse(text);
    if (current.ok === false) {
      setValidation({ ok: false, message: current.error });
      return;
    }
    if (!justification.trim()) {
      setSubmitError("Justificativa é obrigatória para criar um draft.");
      return;
    }
    setSubmitting(true);
    setSubmitError(null);
    try {
      const res = await diagnosticRulesetsService.createDraft(current.value, justification.trim());
      if (res.ok && res.version !== undefined) {
        onCreated(res.version);
      } else {
        setSubmitError(res.error ?? "Falha ao criar draft.");
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div id={id} className="fixed inset-0 z-40 flex justify-end">
      <div className="absolute inset-0" style={{ backgroundColor: "rgba(0,0,0,0.5)" }} onClick={onClose} />
      <div
        className="relative w-full max-w-2xl h-full flex flex-col"
        style={{ backgroundColor: "var(--bg-surface)", borderLeft: "1px solid var(--border)" }}
      >
        <div className="flex items-center justify-between px-6 py-4" style={{ borderBottom: "1px solid var(--border)" }}>
          <div>
            <h3 className="text-[15px] font-semibold" style={{ color: "var(--text-primary)" }}>
              Novo draft de ruleset
            </h3>
            <p className="text-[12px] mt-0.5" style={{ color: "var(--text-tertiary)" }}>
              JSON estruturado — validado contra o schema antes de criar
            </p>
          </div>
          <button type="button" onClick={onClose} aria-label="Fechar" className="p-1.5 rounded-lg cursor-pointer" style={{ color: "var(--text-tertiary)" }}>
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex-1 overflow-auto px-6 py-4 space-y-4">
          <div>
            <label className="text-[11px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
              Ruleset (JSON)
            </label>
            <textarea
              value={text}
              onChange={(e) => {
                setText(e.target.value);
                setValidation(null);
              }}
              spellCheck={false}
              rows={18}
              className="w-full mt-1.5 rounded-[var(--radius-input)] p-3 text-[12px] font-mono leading-relaxed resize-y outline-none"
              style={{
                backgroundColor: "var(--bg-base)",
                border: `1px solid ${parseError ? "var(--error)" : "var(--border)"}`,
                color: "var(--text-primary)",
              }}
            />
            {parseError && (
              <p className="text-[11px] mt-1.5 flex items-center gap-1.5" style={{ color: "var(--error)" }}>
                <AlertCircle className="w-3.5 h-3.5" /> {parseError}
              </p>
            )}
          </div>

          <div>
            <label className="text-[11px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
              Justificativa
            </label>
            <input
              type="text"
              value={justification}
              onChange={(e) => setJustification(e.target.value)}
              placeholder="Por que este draft está sendo criado?"
              className="w-full mt-1.5 rounded-[var(--radius-input)] px-3 py-2 text-[13px] outline-none"
              style={{ backgroundColor: "var(--bg-base)", border: "1px solid var(--border)", color: "var(--text-primary)" }}
            />
          </div>

          {validation && (
            <div
              className="rounded-[var(--radius-card)] px-3 py-2.5 text-[12px] flex items-start gap-2"
              style={{
                backgroundColor: validation.ok ? `${"var(--success)"}1A` : `${"var(--error)"}1A`,
                border: `1px solid ${validation.ok ? "var(--success)" : "var(--error)"}`,
                color: validation.ok ? "var(--success)" : "var(--error)",
              }}
            >
              {validation.ok ? <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" /> : <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />}
              <span>{validation.message}</span>
            </div>
          )}

          {submitError && (
            <p className="text-[12px]" style={{ color: "var(--error)" }}>
              {submitError}
            </p>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 px-6 py-4" style={{ borderTop: "1px solid var(--border)" }}>
          <button
            type="button"
            onClick={handleValidate}
            disabled={validating || submitting}
            className="px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
          >
            {validating ? "Validando..." : "Validar"}
          </button>
          <button
            type="button"
            onClick={handleCreate}
            disabled={submitting || validating}
            className="px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--primary)", color: "var(--on-primary)" }}
          >
            {submitting ? "Criando..." : "Criar draft"}
          </button>
        </div>
      </div>
    </div>
  );
};
