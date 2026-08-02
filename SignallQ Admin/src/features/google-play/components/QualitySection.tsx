import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { GooglePlayVitalsStatus, GooglePlayCrashRateStatus } from "../../../integrations/google-play/googlePlay.types";
import { MetricCard } from "../../../components/ui/MetricCard";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";
import { TermHint } from "../../../components/ui/TermHint";
import { MetricVerdict } from "../../../types/metrics";

// Android Vitals considera ANR rate "ruim" acima de 0,47% (limiar usado pelo próprio Play
// Console pra classificar apps com comportamento problemático) — ver
// https://developer.android.com/topic/performance/vitals/anr.
const ANR_BAD_THRESHOLD = 0.47;
const ANR_REGULAR_THRESHOLD = 0.2;

function anrVerdict(pct: number): MetricVerdict {
  if (pct <= ANR_REGULAR_THRESHOLD) return "excelente";
  if (pct <= ANR_BAD_THRESHOLD) return "bom";
  if (pct <= ANR_BAD_THRESHOLD * 2) return "regular";
  return "fraco";
}

// GH#1352 — crash rate "ruim" acima de 1,09% (mesmo critério de badge do Play Console) — ver
// https://developer.android.com/topic/performance/vitals/crash.
const CRASH_BAD_THRESHOLD = 1.09;
const CRASH_REGULAR_THRESHOLD = 0.5;

function crashVerdict(pct: number): MetricVerdict {
  if (pct <= CRASH_REGULAR_THRESHOLD) return "excelente";
  if (pct <= CRASH_BAD_THRESHOLD) return "bom";
  if (pct <= CRASH_BAD_THRESHOLD * 2) return "regular";
  return "fraco";
}

function formatDateRange(start: string | null, end: string | null): string | undefined {
  if (!start || !end) return undefined;
  const fmt = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("pt-BR");
  return `${fmt(start)} a ${fmt(end)} (média diária)`;
}

interface QualitySectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1341/#1346/#1352 — categoria "Qualidade" (Android Vitals), item 2.1 do plano de UX: as
 * duas métricas-âncora (ANR rate e crash rate) abrem a categoria lado a lado — sem série
 * temporal (endpoint real não expõe histórico dia-a-dia consumível pelo frontend ainda, só o
 * agregado da última sincronização de cada uma). Credenciais e sincronização são compartilhadas
 * (mesma service account, endpoints irmãos) — uma única ação de sincronizar atualiza as duas.
 */
export const QualitySection: React.FC<QualitySectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [anrStatus, setAnrStatus] = React.useState<GooglePlayVitalsStatus | null>(null);
  const [crashStatus, setCrashStatus] = React.useState<GooglePlayCrashRateStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([
      integrationsService.getGooglePlayVitalsStatus(),
      integrationsService.getGooglePlayCrashRateStatus(),
    ])
      .then(([anr, crash]) => {
        if (cancelled) return;
        setAnrStatus(anr);
        setCrashStatus(crash);
      })
      .catch(() => {
        if (!cancelled) {
          setAnrStatus(null);
          setCrashStatus(null);
        }
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  React.useEffect(() => load(), [load, triggerRefreshCounter]);

  const handleSync = React.useCallback(async () => {
    setSyncing(true);
    setSyncError(null);
    try {
      const [anrResult, crashResult] = await Promise.all([
        integrationsService.triggerGooglePlayVitalsSync(),
        integrationsService.triggerGooglePlayCrashRateSync(),
      ]);
      const failed = [anrResult, crashResult].find((r) => r.status !== "ok");
      if (failed) {
        setSyncError(failed.message ?? "Falha ao sincronizar Android Vitals.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar Android Vitals.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando Android Vitals no Play Developer Reporting API..." />;
  }

  if (!anrStatus?.hasCredentials && !crashStatus?.hasCredentials) {
    return (
      <EmptyState
        id="google-play-vitals-no-credentials"
        title="Android Vitals não configurado"
        description="Credenciais do Google Play (GOOGLE_PLAY_CLIENT_EMAIL/GOOGLE_PLAY_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  if (anrStatus?.anrRatePercent === null && crashStatus?.crashRatePercent === null) {
    return (
      <EmptyState
        id="google-play-vitals-no-data"
        title="Nenhum dado sincronizado ainda"
        description="Android Vitals está conectado, mas ainda não foi sincronizado — ou a janela de 7 dias mais recente não tem dado disponível na Play Developer Reporting API."
        action={
          <button
            type="button"
            onClick={handleSync}
            disabled={syncing}
            className="flex items-center gap-1.5 px-3.5 py-2 text-xs font-semibold rounded-[var(--radius-button)] transition-colors cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--primary)", color: "var(--bg-base)" }}
          >
            <RefreshCw className={`w-3.5 h-3.5 ${syncing ? "animate-spin" : ""}`} />
            Sincronizar agora
          </button>
        }
      />
    );
  }

  const lastSyncTimestamp = anrStatus?.lastSyncTimestamp ?? crashStatus?.lastSyncTimestamp ?? null;

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-w-2xl">
        {anrStatus?.anrRatePercent != null ? (
          <MetricCard
            id="google-play-anr-rate-hero"
            size="hero"
            label="ANR rate"
            labelExtra={<TermHint term="anrRate" />}
            value={`${anrStatus.anrRatePercent}%`}
            verdict={anrVerdict(anrStatus.anrRatePercent)}
            verdictNote={formatDateRange(anrStatus.rangeStart, anrStatus.rangeEnd)}
            source="google play"
          />
        ) : (
          <div
            className="rounded-[var(--radius-card)] p-6 flex flex-col justify-center"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <p className="text-xs uppercase tracking-[0.08em] font-semibold flex items-center" style={{ color: "var(--text-secondary)" }}>
              ANR rate
              <TermHint term="anrRate" />
            </p>
            <p className="text-xs mt-2" style={{ color: "var(--text-tertiary)" }}>
              Sem dado disponível na janela de 7 dias mais recente.
            </p>
          </div>
        )}

        {crashStatus?.crashRatePercent != null ? (
          <MetricCard
            id="google-play-crash-rate-hero"
            size="hero"
            label="Crash rate"
            labelExtra={<TermHint term="crashRate" />}
            value={`${crashStatus.crashRatePercent}%`}
            verdict={crashVerdict(crashStatus.crashRatePercent)}
            verdictNote={formatDateRange(crashStatus.rangeStart, crashStatus.rangeEnd)}
            source="google play"
          />
        ) : (
          <div
            className="rounded-[var(--radius-card)] p-6 flex flex-col justify-center"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <p className="text-xs uppercase tracking-[0.08em] font-semibold flex items-center" style={{ color: "var(--text-secondary)" }}>
              Crash rate
              <TermHint term="crashRate" />
            </p>
            <p className="text-xs mt-2" style={{ color: "var(--text-tertiary)" }}>
              Sem dado disponível na janela de 7 dias mais recente.
            </p>
          </div>
        )}
      </div>

      <div className="flex items-center justify-between gap-3 text-[11px]" style={{ color: "var(--text-tertiary)" }}>
        <span>
          Última sincronização:{" "}
          {lastSyncTimestamp ? new Date(lastSyncTimestamp).toLocaleString("pt-BR") : "nunca"}
        </span>
        <button
          type="button"
          onClick={handleSync}
          disabled={syncing}
          className="flex items-center gap-1.5 px-2.5 py-1 rounded-[var(--radius-button)] transition-colors cursor-pointer disabled:opacity-50"
          style={{ border: "1px solid var(--border)", color: "var(--text-secondary)" }}
        >
          <RefreshCw className={`w-3 h-3 ${syncing ? "animate-spin" : ""}`} />
          Sincronizar
        </button>
      </div>

      {syncError && (
        <p className="text-[11px]" style={{ color: "var(--error)" }}>
          {syncError}
        </p>
      )}
    </div>
  );
};
