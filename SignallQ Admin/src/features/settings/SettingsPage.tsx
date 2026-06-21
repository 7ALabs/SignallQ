import React from "react";
import { adminSettingsService, ExtendedSettingsPayload } from "../../services/adminSettingsService";
import { AiProviderSettings } from "./components/AiProviderSettings";
import { CostLimitSettings } from "./components/CostLimitSettings";
import { DiagnosticPipelineSettings } from "./components/DiagnosticPipelineSettings";
import { PrivacySettings } from "./components/PrivacySettings";
import { IntegrationsSettings } from "./components/IntegrationsSettings";
import { MonetizationSettings } from "./components/MonetizationSettings";
import { LoadingState } from "../../components/ui/LoadingState";
import { Settings, Save, CheckCircle2, RotateCcw, ShieldCheck } from "lucide-react";

export const SettingsPage: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [settings, setSettings] = React.useState<ExtendedSettingsPayload | null>(null);
  const [saveStatus, setSaveStatus] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!saveStatus) return;
    const id = setTimeout(() => setSaveStatus(null), 5000);
    return () => clearTimeout(id);
  }, [saveStatus]);

  React.useEffect(() => {
    async function loadSettings() {
      setLoading(true);
      try {
        const payload = await adminSettingsService.getSettings();
        setSettings(payload);
      } catch (e) {
        console.error("Failed to load settings payload", e);
      } finally {
        setLoading(false);
      }
    }
    loadSettings();
  }, []);

  const handleUpdate = (updates: Partial<ExtendedSettingsPayload>) => {
    setSaveStatus(null);
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
    try {
      const res = await adminSettingsService.saveSettings(settings);
      if (res.success) {
        setSaveStatus(res.message);
      }
    } catch (err: any) {
      console.error("Save config failure", err);
      setSaveStatus(err?.message || "Ocorreu um erro ao guardar as configurações.");
    } finally {
      setSaving(false);
    }
  };

  const handleReset = async () => {
    if (window.confirm("Deseja realmente redefinir as configurações para os padrões de fábrica?")) {
      setLoading(true);
      setSaveStatus(null);
      try {
        localStorage.removeItem("@signallq/admin_settings_v1");
        const payload = await adminSettingsService.getSettings();
        setSettings(payload);
        setSaveStatus("Configurações originais redefinidas com sucesso!");
      } catch (e) {
        console.error(e);
      } finally {
        setLoading(false);
      }
    }
  };

  if (loading) {
    return <LoadingState message="Estruturando painéis de controle, limites síncronos e chaves de segurança..." />;
  }

  if (!settings) {
    return (
      <div className="py-20 text-center select-none">
        <p className="text-sm text-zinc-500">Erro crítico ao processar o arquivo de configurações persistent.</p>
      </div>
    );
  }

  return (
    <form onSubmit={handleSave} className="space-y-6">
      {/* Settings Action Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-[#111111] border border-[#262626] rounded-2xl p-5 select-none">
        <div className="flex items-center gap-3">
          <Settings className="w-5 h-5 text-purple-400" />
          <div>
            <h4 className="text-xs font-semibold font-mono text-zinc-300 uppercase">Centro Operacional de Parâmetros</h4>
            <p className="text-[10px] text-zinc-500 font-sans mt-0.5">Defina quotas, timeouts e destinos do pipeline de diagnósticos de RF.</p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 self-end sm:self-auto">
          {/* Reset factory default button */}
          <button
            type="button"
            onClick={handleReset}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-transparent hover:bg-zinc-900 border border-transparent text-xs text-zinc-400 hover:text-white rounded-xl transition-all cursor-pointer font-sans"
          >
            <RotateCcw className="w-3.5 h-3.5" />
            <span>Padrões</span>
          </button>

          {/* Core save command button */}
          <button
            type="submit"
            disabled={saving}
            className="flex items-center gap-1.5 px-4 py-2.5 bg-[#6C2BFF] hover:bg-[#5b1ee6] disabled:bg-purple-900/60 font-mono text-xs font-semibold text-white rounded-xl transition-all shadow-md active:scale-98 cursor-pointer"
          >
            <Save className={`w-3.5 h-3.5 ${saving ? "animate-spin" : ""}`} />
            <span>{saving ? "PERSISTINDO..." : "PERSISTIR ALTERAÇÕES"}</span>
          </button>
        </div>
      </div>

      {saveStatus && (
        <div className="p-3.5 bg-emerald-950/20 border border-emerald-500/20 rounded-xl flex items-center justify-center gap-2 text-emerald-400 text-xs font-mono select-none animate-fade-in text-center">
          <CheckCircle2 className="w-4 h-4 text-emerald-400" />
          <span>{saveStatus}</span>
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

      <div className="bg-[#111111]/30 border border-dashed border-[#262626] rounded-2xl p-4 flex items-center gap-2.5 text-[10px] font-mono text-zinc-550 select-none justify-center">
        <ShieldCheck className="w-4 h-4 text-[#22C55E]" />
        <span>Todos os dados de seguranças e tokens de gateway estão criptografados na borda do cluster.</span>
      </div>
    </form>
  );
};
