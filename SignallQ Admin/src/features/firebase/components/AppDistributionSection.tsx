import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { FirebaseAppDistributionStatus } from "../../../integrations/firebase/firebase.types";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";

interface AppDistributionSectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1343/#1344 — categoria "App Distribution": releases reais (versão, build, notas, data) —
 * a mais "viva" das 5 integrações desta rodada, tem dado de verdade pra mostrar (ex.: SignallQ
 * 0.25.0 build 60). Lista ordenada como o worker devolve (mais recente primeiro).
 */
export const AppDistributionSection: React.FC<AppDistributionSectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<FirebaseAppDistributionStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getFirebaseAppDistributionStatus()
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
      const result = await integrationsService.triggerFirebaseAppDistributionSync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar o App Distribution.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar o App Distribution.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando releases no Firebase App Distribution API..." />;
  }

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="firebase-app-distribution-no-credentials"
        title="Firebase não configurado"
        description="Credenciais do Firebase (FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  if (status.releases.length === 0) {
    return (
      <EmptyState
        id="firebase-app-distribution-empty"
        title="Nenhuma release sincronizada ainda"
        description="Nenhuma release foi distribuída via Firebase App Distribution, ou a lista ainda não foi sincronizada."
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

  return (
    <div className="space-y-3">
      <div className="space-y-2.5">
        {status.releases.map((release) => (
          <div
            key={release.name}
            id={`firebase-app-distribution-release-${release.buildVersion}`}
            className="rounded-[var(--radius-card)] p-4"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <div className="flex items-center justify-between gap-2">
              <h3 className="text-sm font-semibold" style={{ color: "var(--text-primary)" }}>
                SignallQ {release.displayVersion}
              </h3>
              <span
                className="text-[10px] font-mono px-1.5 py-0.5 rounded"
                style={{ color: "var(--text-tertiary)", backgroundColor: "var(--bg-base)", border: "1px solid var(--border)" }}
              >
                build {release.buildVersion}
              </span>
            </div>
            {release.releaseNotesText && (
              <p className="mt-2 text-xs leading-relaxed" style={{ color: "var(--text-secondary)" }}>
                {release.releaseNotesText}
              </p>
            )}
            <p className="mt-2.5 text-[11px] font-mono" style={{ color: "var(--text-tertiary)" }}>
              {new Date(release.createTime).toLocaleString("pt-BR")}
            </p>
          </div>
        ))}
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
