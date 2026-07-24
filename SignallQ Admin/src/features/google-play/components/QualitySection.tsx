import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { GooglePlayVitalsStatus } from "../../../integrations/google-play/googlePlay.types";
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

function formatDateRange(start: string | null, end: string | null): string | undefined {
  if (!start || !end) return undefined;
  const fmt = (iso: string) => new Date(`${iso}T00:00:00`).toLocaleDateString("pt-BR");
  return `${fmt(start)} a ${fmt(end)} (média diária)`;
}

interface QualitySectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1341/#1346 — categoria "Qualidade" (Android Vitals/ANR), item 2.1 do plano de UX: a
 * métrica-âncora (ANR rate) é a única coisa que abre a categoria — sem série temporal
 * (endpoint real não expõe histórico dia-a-dia consumível pelo frontend ainda, só o agregado
 * da última sincronização).
 */
export const QualitySection: React.FC<QualitySectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<GooglePlayVitalsStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getGooglePlayVitalsStatus()
      .then((result) => {
        if (!cancelled) setStatus(result);
      })
      .catch(() => {
        if (!cancelled) setStatus(null);
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
      const result = await integrationsService.triggerGooglePlayVitalsSync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar Android Vitals.");
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

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="google-play-vitals-no-credentials"
        title="Android Vitals não configurado"
        description="Credenciais do Google Play (GOOGLE_PLAY_CLIENT_EMAIL/GOOGLE_PLAY_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  if (status.anrRatePercent === null) {
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

  const verdict = anrVerdict(status.anrRatePercent);

  return (
    <div className="space-y-3">
      <div className="max-w-sm">
        <MetricCard
          id="google-play-anr-rate-hero"
          size="hero"
          label="ANR rate"
          labelExtra={<TermHint term="anrRate" />}
          value={`${status.anrRatePercent}%`}
          verdict={verdict}
          verdictNote={formatDateRange(status.rangeStart, status.rangeEnd)}
          source="google play"
        />
      </div>

      <div className="flex items-center justify-between gap-3 text-[11px]" style={{ color: "var(--text-tertiary)" }}>
        <span>
          Última sincronização:{" "}
          {status.lastSyncTimestamp ? new Date(status.lastSyncTimestamp).toLocaleString("pt-BR") : "nunca"}
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
