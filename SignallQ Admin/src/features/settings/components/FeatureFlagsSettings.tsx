import React from "react";
import { adminSettingsService, FeatureFlag } from "../../../services/adminSettingsService";

export const FeatureFlagsSettings: React.FC = () => {
  const [flags, setFlags] = React.useState<FeatureFlag[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [saveMsg, setSaveMsg] = React.useState<{ ok: boolean; text: string } | null>(null);

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    adminSettingsService.getFeatureFlags().then((data) => {
      if (active) {
        setFlags(data);
        setLoading(false);
      }
    }).catch(() => {
      if (active) setLoading(false);
    });
    return () => { active = false; };
  }, []);

  React.useEffect(() => {
    if (!saveMsg) return;
    const id = setTimeout(() => setSaveMsg(null), 4000);
    return () => clearTimeout(id);
  }, [saveMsg]);

  const handleToggle = async (key: string) => {
    const updated = flags.map((f) =>
      f.key === key ? { ...f, enabled: !f.enabled } : f
    );
    setFlags(updated);
    setSaving(true);
    setSaveMsg(null);
    try {
      await adminSettingsService.setFeatureFlags(updated);
      setSaveMsg({ ok: true, text: "Flags atualizadas com sucesso." });
    } catch {
      setSaveMsg({ ok: false, text: "Falha ao salvar feature flags." });
      // Reverte em caso de erro
      setFlags(flags);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="bg-[#111111] border border-[#262626] rounded-2xl p-5 flex flex-col gap-4">
      <div>
        <h4 className="text-xs font-semibold font-mono uppercase tracking-wider text-zinc-300">
          Feature Flags (Controle de Funcionalidades)
        </h4>
        <p className="text-[11px] text-zinc-500 font-sans mt-0.5">
          Ative ou desative funcionalidades remotamente sem necessidade de novo deploy.
        </p>
      </div>

      {loading ? (
        <div className="space-y-2">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="h-12 bg-zinc-900/40 border border-zinc-800 rounded-xl animate-pulse" />
          ))}
        </div>
      ) : flags.length === 0 ? (
        <div className="py-8 text-center text-xs text-zinc-500 font-sans border border-dashed border-zinc-800 rounded-xl">
          Nenhuma feature flag configurada
        </div>
      ) : (
        <div className="space-y-2">
          {flags.map((flag) => (
            <div
              key={flag.key}
              className="flex items-center justify-between gap-4 p-3.5 bg-zinc-950/30 border border-zinc-800 rounded-xl hover:border-zinc-700 transition-colors"
            >
              <div className="min-w-0">
                <span className="text-[11px] font-mono text-zinc-200 block truncate">
                  {flag.key}
                </span>
                {flag.description && (
                  <span className="text-[10px] text-zinc-500 font-sans block mt-0.5 truncate">
                    {flag.description}
                  </span>
                )}
              </div>
              <button
                type="button"
                onClick={() => handleToggle(flag.key)}
                disabled={saving}
                aria-label={`${flag.enabled ? "Desativar" : "Ativar"} ${flag.key}`}
                className={`
                  relative shrink-0 inline-flex h-5 w-9 items-center rounded-full transition-colors duration-200
                  focus:outline-none focus-visible:ring-2 focus-visible:ring-purple-500
                  disabled:opacity-50 cursor-pointer disabled:cursor-not-allowed
                  ${flag.enabled ? "bg-[#6C2BFF]" : "bg-zinc-700"}
                `}
              >
                <span
                  className={`
                    inline-block h-3.5 w-3.5 rounded-full bg-white shadow transition-transform duration-200
                    ${flag.enabled ? "translate-x-4" : "translate-x-0.5"}
                  `}
                />
              </button>
            </div>
          ))}
        </div>
      )}

      {saveMsg && (
        <p className={`text-[10px] font-mono text-center ${saveMsg.ok ? "text-emerald-400" : "text-red-400"}`}>
          {saveMsg.text}
        </p>
      )}
    </div>
  );
};
