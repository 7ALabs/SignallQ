import React from "react";
import { adminSettingsService, ExtendedSettingsPayload } from "../../services/adminSettingsService";
import { AiProviderSettings } from "./components/AiProviderSettings";
import { CostLimitSettings } from "./components/CostLimitSettings";
import { DiagnosticPipelineSettings } from "./components/DiagnosticPipelineSettings";
import { PrivacySettings } from "./components/PrivacySettings";
import { IntegrationsSettings } from "./components/IntegrationsSettings";
import { MonetizationSettings } from "./components/MonetizationSettings";
import { FeatureFlagsSettings } from "./components/FeatureFlagsSettings";
import { LoadingState } from "../../components/ui/LoadingState";
import { Settings, Save, CheckCircle2, RotateCcw, ShieldCheck, AlertTriangle } from "lucide-react";

export const SettingsPage: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [settings, setSettings] = React.useState<ExtendedSettingsPayload | null>(null);
  const [loadError, setLoadError] = React.useState<string | null>(null);
  const [saveStatus, setSaveStatus] = React.useState<string | null>(null);
  const [saveError, setSaveError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!saveStatus) return;
    const id = setTimeout(() => setSaveStatus(null), 5000);
    return () => clearTimeout(id);
  }, [saveStatus]);

  React.useEffect(() => {
    if (!saveError) return;
    const id = setTimeout(() => setSaveError(null), 8000);
    return () => clearTimeout(id);
  }, [saveError]);

  const loadSettings = React.useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const payload = await adminSettingsService.getSettings();
      setSettings(payload);
    } catch (e) {
      console.error("Failed to load settings payload", e);
      // GH#416: sem dado real (worker fora do ar e sem cache) — não mostra dado inventado.
      setSettings(null);
      setLoadError(
        e instanceof Error ? e.message : "Não foi possível carregar as configurações da Admin API."
      );
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    loadSettings();
  }, [loadSettings]);

  const handleUpdate = (updates: Partial<ExtendedSettingsPayload>) => {
    setSaveStatus(null);
    setSaveError(null);
    setSettings((prev) => {
      if (!prev) return null;
      return { ...prev, ...updates };
    });
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!settings) return;

    setSaving(true);
    setSaveStatus(null);
    setSaveError(null);
    try {
      const res = await adminSettingsService.saveSettings(settings);
      if (res.success) {
        setSaveStatus(res.message);
      }
    } catch (err: unknown) {
      console.error("Save config failure", err);
      setSaveError(
        err instanceof Error ? err.message : "Falha ao salvar configurações. Tente novamente."
      );
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (window.confirm("Deseja realmente redefinir as configurações para os padrões de fábrica?")) {
      setSaveStatus(null);
      setSaveError(null);
      localStorage.removeItem("@signallq/admin_settings_v1");
      await loadSettings();
      setSaveStatus("Configurações originais redefinidas com sucesso!");
    }
  };

  if (loading) {
    return <LoadingState message="Estruturando painéis de controle, limites síncronos e chaves de segurança..." />;
  }

  if (!settings) {
    return (
      <div className="py-20 text-center select-none space-y-3">
        <p className="text-sm text-[var(--text-tertiary)]">
          {loadError ?? "Sem dados: não foi possível carregar as configurações."}
        </p>
        <button
          type="button"
          onClick={() => loadSettings()}
          className="text-xs text-[var(--primary)] hover:underline cursor-pointer"
        >
          Tentar novamente
        </button>
      </div>
    );
  }

  return (
    <form onSubmit={handleSave} className="space-y-6">
      {/* Settings Action Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[8px] p-5 select-none">
        <div className="flex items-center gap-3">
          <Settings className="w-5 h-5 text-[var(--text-secondary)]" />
          <div>
            <h4 className="text-xs font-semibold font-sans text-[var(--text-secondary)] uppercase">Centro Operacional de Parâmetros</h4>
            <p className="text-[10px] text-[var(--text-tertiary)] font-sans mt-0.5">Defina quotas, timeouts e destinos do pipeline de diagnósticos de RF.</p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 self-end sm:self-auto">
          {/* Reset factory default button */}
          <button
            type="button"
            onClick={handleReset}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-transparent hover:bg-zinc-900 border border-transparent text-xs text-[var(--text-secondary)] hover:text-[var(--text-primary)] rounded-xl transition-all cursor-pointer font-sans"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Padrões</span>
          </button>

          {/* Core save command button */}
          <button
            type="submit"
            disabled={saving}
            className="flex items-center gap-1.5 px-4 py-2.5 bg-[var(--primary)] hover:opacity-90 disabled:opacity-40 font-sans text-xs font-semibold text-white rounded-xl transition-all shadow-md active:scale-98 cursor-pointer"
          >
            <Save className={`w-3.5 h-3.5 ${saving ? "animate-spin" : ""}`} />
            <span>{saving ? "PERSISTINDO..." : "PERSISTIR ALTERAÇÕES"}</span>
          </button>
        </div>
      </div>

      {saveStatus && (
        <div className="p-3.5 bg-emerald-950/20 border border-emerald-500/20 rounded-xl flex items-center justify-center gap-2 text-emerald-400 text-xs font-sans select-none animate-fade-in text-center">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{saveStatus}</span>
        </div>
      )}

      {saveError && (
        <div className="p-3.5 bg-red-950/20 border border-red-500/20 rounded-xl flex items-center justify-center gap-2 text-red-400 text-xs font-sans select-none animate-fade-in text-center">
          <AlertTriangle className="w-4 h-4 text-red-400" />
          <span>{saveError}</span>
        </div>
      )}

      {/* Grid configuration panels layouts */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 font-sans text-xs">
        <AiProviderSettings settings={settings} onChange={handleUpdate} />
        <CostLimitSettings settings={settings} onChange={handleUpdate} />
        <DiagnosticPipelineSettings settings={settings} onChange={handleUpdate} />
        <PrivacySettings settings={settings} onChange={handleUpdate} />
        <MonetizationSettings settings={settings} onChange={handleUpdate} />
        <IntegrationsSettings />
      </div>

      {/* SIG-13: Feature Flags — fora do grid 2-col para ocupar largura total */}
      <FeatureFlagsSettings />

      <div className="bg-[var(--bg-sidebar)]/30 border border-dashed border-[var(--border)] rounded-[8px] p-4 flex items-center gap-2.5 text-[10px] font-sans text-[var(--text-tertiary)] select-none justify-center">
        <ShieldCheck className="w-4 h-4 text-[var(--success)]" />
        <span>Todos os dados de seguranças e tokens de gateway estão criptografados na borda do cluster.</span>
      </div>
    </form>
  );
};
