import React from "react";
import { adminSettingsService, ExtendedSettingsPayload } from "../../services/adminSettingsService";
import { CostLimitSettings } from "./components/CostLimitSettings";
import { IntegrationsSettings } from "./components/IntegrationsSettings";
import { LoadingState } from "../../components/ui/LoadingState";
import { SectionCard } from "../../components/ui/SectionCard";
import { FeatureComingSoon } from "../../components/ui/FeatureComingSoon";
import { FeatureFlagsTab } from "../feature-flags/FeatureFlagsTab";
import { Settings, Save, CheckCircle2, RotateCcw, ShieldCheck, AlertTriangle } from "lucide-react";
import { alpha } from "../../utils/color";

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
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 bg-[var(--bg-sidebar)] border border-[var(--border)] rounded-[var(--radius-card)] p-5 select-none">
        <div className="flex items-center gap-3">
          <Settings className="w-5 h-5 text-[var(--text-secondary)]" />
          <div>
            <h4 className="text-xs font-semibold font-sans text-[var(--text-secondary)] uppercase">Parâmetros operacionais</h4>
            <p className="text-[10px] text-[var(--text-tertiary)] font-sans mt-0.5">Ajustes que efetivamente alteram o comportamento do worker de alertas ou de feature flags (GH#426).</p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 self-end sm:self-auto">
          {/* Reset factory default button */}
          <button
            type="button"
            onClick={handleReset}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-transparent border border-transparent text-xs text-[var(--text-secondary)] hover:text-[var(--text-primary)] rounded-xl transition-all cursor-pointer font-sans"
            onMouseEnter={(e) => { e.currentTarget.style.backgroundColor = "var(--bg-surface-hover)"; }}
            onMouseLeave={(e) => { e.currentTarget.style.backgroundColor = "transparent"; }}
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
            <span>{saving ? "Salvando..." : "Salvar alterações"}</span>
          </button>
        </div>
      </div>

      {saveStatus && (
        <div
          role="status"
          aria-live="polite"
          className="p-3.5 rounded-xl flex items-center justify-center gap-2 text-xs font-sans select-none animate-fade-in text-center"
          style={{
            backgroundColor: alpha("var(--success)", 20),
            border: `1px solid ${alpha("var(--success)", 20)}`,
            color: "var(--success)",
          }}
        >
          <CheckCircle2 className="w-4 h-4" style={{ color: "var(--success)" }} />
          <span>{saveStatus}</span>
        </div>
      )}

      {saveError && (
        <div
          role="status"
          aria-live="polite"
          className="p-3.5 rounded-xl flex items-center justify-center gap-2 text-xs font-sans select-none animate-fade-in text-center"
          style={{
            backgroundColor: alpha("var(--error)", 20),
            border: `1px solid ${alpha("var(--error)", 20)}`,
            color: "var(--error)",
          }}
        >
          <AlertTriangle className="w-4 h-4" style={{ color: "var(--error)" }} />
          <span>{saveError}</span>
        </div>
      )}

      {/* GH#552 (Fase 2) — "Configurações": fusão de settings/ + feature-flags/. Único
          escritor real de flags continua sendo PUT /admin/feature-flags/:key (GH#424) —
          o bloco abaixo usa o mesmo componente da rota antiga, sem segundo caminho de escrita. */}
      <SectionCard
        title="Feature Flags ativas"
        description="Controle remoto de telas e funcionalidades do app — efeito real no Android, sem novo build."
        id="settings-feature-flags-block"
      >
        <FeatureFlagsTab embedded />
      </SectionCard>

      {/* Bloco: Notificações e alertas — limiares que o worker consome de fato (GH#426) */}
      {/* Bloco: Integrações administrativas externas */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 font-sans text-xs">
        <CostLimitSettings settings={settings} onChange={handleUpdate} />
        <IntegrationsSettings />
      </div>

      {/* Bloco: Conta e acesso — sem dado real hoje (não existe tabela de admins/auditoria) */}
      <FeatureComingSoon
        feature="Conta e Acesso · Log de Auditoria"
        reason="Requer tabela de admins com papéis e trilha de auditoria (quem mudou o quê, quando) no worker"
        compact
      />

      <div className="bg-[var(--bg-sidebar)]/30 border border-dashed border-[var(--border)] rounded-[var(--radius-card)] p-4 flex items-center gap-2.5 text-[10px] font-sans text-[var(--text-tertiary)] select-none justify-center">
        <ShieldCheck className="w-4 h-4 text-[var(--success)]" />
        <span>Chaves e tokens de integração ficam armazenados de forma criptografada no worker — nunca expostos no navegador.</span>
      </div>
    </form>
  );
};
