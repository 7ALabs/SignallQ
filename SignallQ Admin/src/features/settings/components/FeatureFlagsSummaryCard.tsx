import React, { useEffect, useState } from "react";
import { featureFlagsService, FeatureFlag } from "../../../services/featureFlagsService";

function formatFlagName(key: string): string {
  return key
    .replace(/^feature_/, "")
    .split("_")
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join(" ");
}

/**
 * Card compacto de FEATURE FLAGS — paridade com o grid 2 colunas do mockup
 * (sec-settings), pareado com "Acesso da equipe". Escreve pelo mesmo
 * endpoint PUT /admin/feature-flags/:key usado pela tela dedicada (GH#424).
 */
export const FeatureFlagsSummaryCard: React.FC = () => {
  const [flags, setFlags] = useState<FeatureFlag[]>([]);
  const [loading, setLoading] = useState(true);
  const [toggling, setToggling] = useState<string | null>(null);

  useEffect(() => {
    let active = true;
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
    setFlags((prev) => prev.map((f) => (f.key === key ? { ...f, enabled: !currentEnabled } : f)));
    const ok = await featureFlagsService.updateFlag(key, !currentEnabled);
    if (!ok) {
      setFlags((prev) => prev.map((f) => (f.key === key ? { ...f, enabled: currentEnabled } : f)));
    }
    setToggling(null);
  };

  return (
    <div
      className="rounded-[var(--radius-card)] overflow-hidden sq-card-hover p-5"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
    >
      <div
        className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em] mb-4"
        style={{ color: "var(--text-tertiary)" }}
      >
        Feature flags
      </div>

      {loading && (
        <div className="space-y-2.5">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="h-9 bg-[var(--bg-surface-muted)] rounded-lg animate-pulse" />
          ))}
        </div>
      )}

      {!loading && flags.length === 0 && (
        <p className="text-[11px] font-sans text-[var(--text-tertiary)]">Nenhuma feature flag encontrada.</p>
      )}

      {!loading && flags.length > 0 && (
        <ul className="space-y-2.5">
          {flags.map((flag) => (
            <li key={flag.key} className="flex items-center justify-between gap-3">
              <span className="text-[12px] font-sans text-[var(--text-primary)] truncate">
                {formatFlagName(flag.key)}
              </span>
              <button
                type="button"
                role="switch"
                aria-checked={flag.enabled}
                aria-label={`${flag.enabled ? "Desativar" : "Ativar"} ${formatFlagName(flag.key)}`}
                onClick={() => handleToggle(flag.key, flag.enabled)}
                disabled={toggling === flag.key}
                className="shrink-0 w-9 h-5 rounded-full relative transition-colors cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed focus:outline-none focus-visible:ring-2 focus-visible:ring-[var(--primary)]"
                style={{ backgroundColor: flag.enabled ? "var(--primary)" : "var(--bg-surface-muted)" }}
              >
                <span
                  className="absolute top-0.5 w-4 h-4 rounded-full bg-white transition-all"
                  style={{ left: flag.enabled ? "18px" : "2px" }}
                />
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};
