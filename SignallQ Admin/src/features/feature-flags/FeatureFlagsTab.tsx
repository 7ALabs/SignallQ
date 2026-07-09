import React, { useState, useEffect } from "react";
import { ToggleRight, ToggleLeft, Clock, User } from "lucide-react";
import { featureFlagsService, FeatureFlag } from "../../services/featureFlagsService";

interface Props {
  filters?: { environment?: string };
  /** GH#552 (Fase 2): true quando embutido dentro de Configurações (sem cabeçalho de página próprio). */
  embedded?: boolean;
}

function formatFlagKey(key: string): string {
  // Remove prefixo 'feature_' e formata: feature_speedtest → Speedtest
  return key
    .replace(/^feature_/, "")
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

function formatTimestamp(unixSec: number): string {
  if (!unixSec) return "—";
  const d = new Date(unixSec * 1000);
  return d.toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export const FeatureFlagsTab: React.FC<Props> = ({ embedded = false }) => {
  const [flags, setFlags] = useState<FeatureFlag[]>([]);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState<string | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    let active = true;
    setLoading(true);
    featureFlagsService.getFlags().then((data) => {
      if (active) {
        setFlags(data);
        setLoading(false);
      }
    }).catch(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  const handleToggle = async (key: string, currentEnabled: boolean) => {
    if (toggling) return;
    setToggling(key);
    setErrors((prev) => { const next = { ...prev }; delete next[key]; return next; });

    // Atualização otimista
    setFlags((prev) =>
      prev.map((f) => (f.key === key ? { ...f, enabled: !currentEnabled } : f))
    );

    const ok = await featureFlagsService.updateFlag(key, !currentEnabled);

    if (!ok) {
      // Reverte
      setFlags((prev) =>
        prev.map((f) => (f.key === key ? { ...f, enabled: currentEnabled } : f))
      );
      setErrors((prev) => ({ ...prev, [key]: "Falha ao atualizar. Tente novamente." }));
    }

    setToggling(null);
  };

  return (
    <div className="space-y-6">
      {/* Cabeçalho da seção — omitido quando embutido em Configurações (GH#552) */}
      {!embedded && (
        <div className="flex items-start justify-between gap-4">
          <div>
            <h2 className="text-base font-semibold text-[var(--text-primary)]">Feature Flags</h2>
            <p className="text-xs text-[var(--text-tertiary)] mt-0.5">
              Controle remoto de telas e funcionalidades
            </p>
          </div>
          <span className="shrink-0 text-[10px] font-mono text-[var(--text-tertiary)] bg-zinc-900 border border-zinc-800 rounded-lg px-2.5 py-1">
            {flags.length} flags
          </span>
        </div>
      )}

      {/* Loading */}
      {loading && (
        <div className="space-y-3">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="h-20 bg-zinc-900/40 border border-zinc-800 rounded-[var(--radius-card)] animate-pulse" />
          ))}
        </div>
      )}

      {/* Empty state */}
      {!loading && flags.length === 0 && (
        <div className="flex flex-col items-center justify-center py-16 border border-dashed border-zinc-800 rounded-[var(--radius-card)] text-[var(--text-tertiary)]">
          <ToggleRight className="w-8 h-8 mb-3 opacity-40" />
          <p className="text-sm">Nenhuma feature flag encontrada</p>
          <p className="text-xs mt-1 opacity-60">Configure flags no worker ou verifique a migration 005_sig13.sql</p>
        </div>
      )}

      {/* Lista de flags */}
      {!loading && flags.length > 0 && (
        <div className="space-y-3">
          {flags.map((flag) => (
            <div
              key={flag.key}
              className="bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[var(--radius-card)] p-4 hover:border-zinc-700 transition-colors"
            >
              <div className="flex items-center justify-between gap-4">
                {/* Info da flag */}
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-2 mb-0.5">
                    <span className="text-sm font-semibold text-[var(--text-primary)] truncate">
                      {formatFlagKey(flag.key)}
                    </span>
                    <span
                      className={`shrink-0 text-[9px] font-sans uppercase tracking-wider px-1.5 py-0.5 rounded-md ${
                        flag.enabled
                          ? "bg-emerald-950/60 text-emerald-400 border border-emerald-800/40"
                          : "bg-zinc-900 text-[var(--text-tertiary)] border border-zinc-800"
                      }`}
                    >
                      {flag.enabled ? "ativo" : "inativo"}
                    </span>
                  </div>
                  <p className="text-[11px] text-[var(--text-secondary)] truncate">{flag.description}</p>
                  <div className="flex items-center gap-3 mt-1.5">
                    {flag.updatedBy && (
                      <span className="flex items-center gap-1 text-[10px] text-[var(--text-tertiary)]">
                        <User className="w-3 h-3" />
                        {flag.updatedBy}
                      </span>
                    )}
                    {flag.updatedAt > 0 && (
                      <span className="flex items-center gap-1 text-[10px] text-[var(--text-tertiary)]">
                        <Clock className="w-3 h-3" />
                        {formatTimestamp(flag.updatedAt)}
                      </span>
                    )}
                  </div>
                </div>

                {/* Toggle */}
                <button
                  type="button"
                  onClick={() => handleToggle(flag.key, flag.enabled)}
                  disabled={toggling === flag.key}
                  aria-label={`${flag.enabled ? "Desativar" : "Ativar"} ${formatFlagKey(flag.key)}`}
                  className="shrink-0 focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--primary)] rounded-full disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer transition-opacity"
                >
                  {flag.enabled ? (
                    <ToggleRight className="w-8 h-8 text-[var(--primary)]" />
                  ) : (
                    <ToggleLeft className="w-8 h-8 text-zinc-600" />
                  )}
                </button>
              </div>

              {/* Erro inline */}
              {errors[flag.key] && (
                <p className="mt-2 text-[10px] font-sans text-red-400 border-t border-red-900/30 pt-2">
                  {errors[flag.key]}
                </p>
              )}
            </div>
          ))}
        </div>
      )}

      {/* Nota de rodapé */}
      <p className="text-[10px] text-[var(--text-tertiary)] font-sans text-center">
        Alterações entram em vigor no próximo poll do app · endpoint público: <code className="text-[var(--text-secondary)]">/flags</code>
      </p>
    </div>
  );
};
