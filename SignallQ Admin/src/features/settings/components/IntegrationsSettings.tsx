import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { integrationsService } from "../../../integrations/integrationsService";
import {
  Database,
  Play,
  RotateCw,
  CheckCircle2,
  Smartphone,
  Clock,
  AlertTriangle
} from "lucide-react";

export const IntegrationsSettings: React.FC = () => {
  const [loading, setLoading] = React.useState(true);
  const [data, setData] = React.useState<any>(null);

  const [syncingFb, setSyncingFb] = React.useState(false);
  const [syncingGp, setSyncingGp] = React.useState(false);
  const [syncFeedback, setSyncFeedback] = React.useState<{ [key: string]: string }>({});

  const fbTimeoutRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);
  const gpTimeoutRef = React.useRef<ReturnType<typeof setTimeout> | null>(null);

  React.useEffect(() => {
    return () => {
      if (fbTimeoutRef.current) clearTimeout(fbTimeoutRef.current);
      if (gpTimeoutRef.current) clearTimeout(gpTimeoutRef.current);
    };
  }, []);

  const fetchStatus = React.useCallback(async () => {
    try {
      const stats = await integrationsService.getAllStatus();
      setData(stats);
    } catch (e) {
      console.error("Failed to query integrations status logs:", e);
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  const handleSyncFirebase = async () => {
    setSyncingFb(true);
    setSyncFeedback(prev => ({ ...prev, firebase: "" }));
    try {
      const res = await integrationsService.triggerFirebaseSync();
      setSyncFeedback(prev => ({
        ...prev,
        firebase: `Iniciado! Job ID: ${res.jobId}. Importando crashlogs e telemetria...`
      }));
      fbTimeoutRef.current = setTimeout(() => {
        fetchStatus();
        setSyncingFb(false);
      }, 1500);
    } catch (err: any) {
      setSyncFeedback(prev => ({ ...prev, firebase: "Erro na sincronização síncrona." }));
      setSyncingFb(false);
    }
  };

  const handleSyncGooglePlay = async () => {
    setSyncingGp(true);
    setSyncFeedback(prev => ({ ...prev, googlePlay: "" }));
    try {
      const res = await integrationsService.triggerGooglePlaySync();
      setSyncFeedback(prev => ({
        ...prev,
        googlePlay: `Iniciado! Job ID: ${res.jobId}. Crawlando instalações e avaliações...`
      }));
      gpTimeoutRef.current = setTimeout(() => {
        fetchStatus();
        setSyncingGp(false);
      }, 1500);
    } catch (err: any) {
      setSyncFeedback(prev => ({ ...prev, googlePlay: "Falha na sincronização síncrona." }));
      setSyncingGp(false);
    }
  };

  if (loading || !data) {
    return (
      <div className="p-6 bg-zinc-900/30 border border-zinc-850 rounded-[8px] flex items-center justify-center font-sans text-[var(--text-tertiary)] text-xs">
        <RotateCw className="w-4 h-4 animate-spin mr-2 text-[var(--text-secondary)]" />
        <span>Consultando endpoints de integradores...</span>
      </div>
    );
  }

  const renderBadge = (status: string) => {
    const neutral = "text-[10px] bg-[var(--bg-surface-muted)] text-[var(--text-secondary)] border border-[var(--border)] px-2 py-0.5 rounded-full font-bold select-none font-sans";
    switch (status) {
      case "connected":
        return <span className={neutral}>CONECTADO</span>;
      case "mock":
        return <span className={neutral}>MOCK DATA</span>;
      case "attention":
        return <span className="text-[10px] bg-amber-950/40 text-amber-500 border border-amber-500/20 px-2 py-0.5 rounded-full font-bold select-none font-sans">ATENÇÃO</span>;
      case "planned":
        return <span className={neutral}>PLANEJADO</span>;
      case "disabled":
      default:
        return <span className={neutral}>DESATIVADO</span>;
    }
  };

  const fb = data.firebase;
  const gp = data.googlePlay;
  const as = data.appStore;

  return (
    <div className="col-span-1 lg:col-span-2 select-none">
      <SectionCard
        title="Integrações Administrativas Externas"
        description="Gerencie pontes de dados de telemetria analítica com a console do Firebase, estatísticas do Google Play e App Store futura da Apple."
      >
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          {/* 1. Card Firebase */}
          <div className="bg-zinc-950/60 border border-zinc-850/70 p-5 rounded-xl flex flex-col justify-between space-y-4 hover:border-zinc-800 transition-colors">
            <div className="space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <Database className="w-4 h-4 text-amber-500 shrink-0" />
                  <span className="text-sm font-bold text-[var(--text-primary)]">Google Firebase</span>
                </div>
                {renderBadge(fb.status)}
              </div>

              <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed font-sans">{fb.message}</p>

              {/* Telemetry metadata block */}
              <div className="space-y-1.5 pt-2 border-t border-zinc-900 text-[10px] font-mono text-[var(--text-tertiary)]">
                <div className="flex justify-between">
                  <span>Plataforma:</span>
                  <span className="text-[var(--text-primary)]">{fb.platform || "Android (Firebase Analytics + Crashlytics)"}</span>
                </div>
                <div className="flex justify-between">
                  <span>Última Sync:</span>
                  <span className="text-[var(--text-primary)] flex items-center gap-1">
                    <Clock className="w-3 h-3 text-[var(--text-tertiary)]" />
                    {fb.lastSyncTimestamp || "Nunca sincronizado"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Eventos Importados:</span>
                  <span className="text-[var(--text-primary)]">{(fb.eventsImported ?? 0).toLocaleString("pt-BR")}</span>
                </div>
                <div className="flex justify-between">
                  <span>Crashes Importados:</span>
                  <span className="text-[var(--text-primary)]">{(fb.crashesImported ?? 0).toLocaleString("pt-BR")}</span>
                </div>
              </div>
            </div>

            <div className="space-y-2">
              {syncFeedback.firebase && (
                <div className="text-[9px] font-mono text-amber-400 bg-amber-950/20 px-2 py-1 rounded border border-amber-900/40">
                  {syncFeedback.firebase}
                </div>
              )}
              <button
                type="button"
                onClick={handleSyncFirebase}
                disabled={syncingFb || !fb.enabled}
                className="w-full h-8 flex items-center justify-center gap-1.5 text-[11px] font-sans font-semibold bg-zinc-900 hover:bg-zinc-850 text-[var(--text-secondary)] disabled:opacity-40 rounded-lg cursor-pointer transition-colors"
              >
                <RotateCw className={`w-3.5 h-3.5 text-amber-500 ${syncingFb ? "animate-spin" : ""}`} />
                <span>{syncingFb ? "SINCRONIZANDO..." : "SINCRONIZAR AGORA"}</span>
              </button>
            </div>
          </div>

          {/* 2. Card Google Play */}
          <div className="bg-zinc-950/60 border border-zinc-850/70 p-5 rounded-xl flex flex-col justify-between space-y-4 hover:border-zinc-800 transition-colors">
            <div className="space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <Play className="w-4 h-4 text-[var(--primary)] shrink-0 fill-[var(--primary)]" />
                  <span className="text-sm font-bold text-[var(--text-primary)]">Google Play Console</span>
                </div>
                {renderBadge(gp.status)}
              </div>

              <p className="text-[11px] text-[var(--text-secondary)] leading-relaxed font-sans">{gp.message}</p>

              <div className="space-y-1.5 pt-2 border-t border-zinc-900 text-[10px] font-mono text-[var(--text-tertiary)]">
                <div className="flex justify-between">
                  <span>Plataforma:</span>
                  <span className="text-[var(--text-primary)]">{gp.platform || "Android (Google Play Console)"}</span>
                </div>
                <div className="flex justify-between">
                  <span>Última Sync:</span>
                  <span className="text-[var(--text-primary)] flex items-center gap-1">
                    <Clock className="w-3 h-3 text-[var(--text-tertiary)]" />
                    {gp.lastSyncTimestamp || "Nunca sincronizado"}
                  </span>
                </div>
                <div className="flex justify-between">
                  <span>Downloads Ativos:</span>
                  <span className="text-[var(--text-primary)]">{(gp.downloadsImported ?? 0).toLocaleString("pt-BR")}</span>
                </div>
                <div className="flex justify-between">
                  <span>Mapeamento:</span>
                  <span className="text-[var(--text-primary)]">Instalações + Ratings</span>
                </div>
              </div>
            </div>

            <div className="space-y-2">
              {syncFeedback.googlePlay && (
                <div className="text-[9px] font-mono text-[var(--text-secondary)] bg-[var(--bg-surface-muted)] px-2 py-1 rounded border border-[var(--border)]">
                  {syncFeedback.googlePlay}
                </div>
              )}
              <button
                type="button"
                onClick={handleSyncGooglePlay}
                disabled={syncingGp || !gp.enabled}
                className="w-full h-8 flex items-center justify-center gap-1.5 text-[11px] font-sans font-semibold bg-zinc-900 hover:bg-zinc-850 text-[var(--text-secondary)] disabled:opacity-40 rounded-lg cursor-pointer transition-colors"
              >
                <RotateCw className={`w-3.5 h-3.5 text-[var(--primary)] ${syncingGp ? "animate-spin" : ""}`} />
                <span>{syncingGp ? "SINCRONIZANDO..." : "SINCRONIZAR AGORA"}</span>
              </button>
            </div>
          </div>

          {/* 3. Card App Store Connect (iOS Futuro) */}
          <div className="bg-zinc-950/20 border border-dashed border-zinc-850/60 p-5 rounded-xl flex flex-col justify-between space-y-4 opacity-75">
            <div className="space-y-3">
              <div className="flex items-start justify-between">
                <div className="flex items-center gap-2">
                  <Smartphone className="w-4 h-4 text-[var(--text-tertiary)] shrink-0" />
                  <span className="text-sm font-bold text-[var(--text-secondary)]">App Store Connect</span>
                </div>
                {renderBadge(as.status)}
              </div>

              <p className="text-[11px] text-[var(--text-tertiary)] leading-relaxed font-sans">{as.message}</p>

              <div className="space-y-1.5 pt-2 border-t border-zinc-900 text-[10px] font-mono text-[var(--text-tertiary)]">
                <div className="flex justify-between">
                  <span>Plataforma:</span>
                  <span className="text-[var(--text-tertiary)]">{as.platform || "Apple iOS"}</span>
                </div>
                <div className="flex justify-between">
                  <span>Sincronização:</span>
                  <span className="text-red-400 font-bold">Planned (Desativada)</span>
                </div>
                <div className="flex justify-between">
                  <span>Custo Atual:</span>
                  <span className="text-[var(--text-tertiary)]">$0,00</span>
                </div>
              </div>
            </div>

            <div>
              <button
                type="button"
                disabled={true}
                className="w-full h-8 flex items-center justify-center gap-1.5 text-[11px] font-sans font-semibold bg-zinc-900/30 text-[var(--text-tertiary)] border border-transparent rounded-lg cursor-not-allowed"
              >
                <AlertTriangle className="w-3.5 h-3.5 text-[var(--text-tertiary)]" />
                <span>DESABILITADO</span>
              </button>
            </div>
          </div>
        </div>
      </SectionCard>
    </div>
  );
};
