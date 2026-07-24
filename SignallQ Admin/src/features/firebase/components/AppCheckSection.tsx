import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { FirebaseAppCheckStatus } from "../../../integrations/firebase/firebase.types";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { TermHint } from "../../../components/ui/TermHint";

interface AppCheckSectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1343/#1344 — categoria "App Check": status por provedor (Play Integrity, DeviceCheck etc.).
 * Caso mais provável na prática hoje: nenhum provedor configurado no Console ainda — por isso o
 * estado vazio aqui não é "erro"/"indisponível" genérico, é uma explicação específica de que
 * App Check está conectado mas sem provedor habilitado, com o link de onde configurar.
 */
export const AppCheckSection: React.FC<AppCheckSectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<FirebaseAppCheckStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getFirebaseAppCheckStatus()
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
      const result = await integrationsService.triggerFirebaseAppCheckSync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar o App Check.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar o App Check.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando provedores no Firebase App Check API..." />;
  }

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="firebase-app-check-no-credentials"
        title="Firebase não configurado"
        description="Credenciais do Firebase (FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  const services = status.services?.services ?? [];
  const neverSynced = status.lastSyncTimestamp == null;

  if (neverSynced || services.length === 0) {
    return (
      <div className="space-y-3">
        <EmptyState
          id="firebase-app-check-no-provider"
          title={neverSynced ? "Nenhum provedor sincronizado ainda" : "Nenhum provedor de App Check configurado"}
          description={
            neverSynced
              ? "App Check está conectado, mas o inventário de provedores ainda não foi sincronizado com a Firebase App Check API."
              : "App Check está conectado, mas nenhum provedor (Play Integrity, DeviceCheck etc.) foi habilitado no Firebase Console ainda — o app não está sendo verificado no momento."
          }
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
        {syncError && (
          <p className="text-[11px]" style={{ color: "var(--error)" }}>
            {syncError}
          </p>
        )}
      </div>
    );
  }

  return (
    <div className="space-y-3">
      <div className="space-y-2.5">
        {services.map((service) => (
          <div
            key={service.name}
            id={`firebase-app-check-service-${service.name}`}
            className="rounded-[var(--radius-card)] p-4 flex items-center justify-between gap-3"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <p className="text-sm font-mono truncate" style={{ color: "var(--text-primary)" }}>
              {service.name}
              <TermHint term="appCheckState" />
            </p>
            <StatusBadge status={service.state === "ENFORCED" ? "ok" : "attention"} customLabel={service.state} />
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
