import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { FirebaseManagementStatus } from "../../../integrations/firebase/firebase.types";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";
import { StatusBadge } from "../../../components/ui/StatusBadge";

interface ManagementSectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1343/#1344 — categoria "Management": inventário do projeto Firebase (nome, ID) + Android
 * apps cadastrados (nome, package, status). É conteúdo/config, não métrica — mesmo tratamento
 * que `StoreListingSection` deu à ficha da loja (sem `MetricCard`, sem forçar gráfico/série).
 */
export const ManagementSection: React.FC<ManagementSectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<FirebaseManagementStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getFirebaseManagementStatus()
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
      const result = await integrationsService.triggerFirebaseManagementSync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar o inventário do Firebase.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar o inventário do Firebase.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando projeto e Android apps na Firebase Management API..." />;
  }

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="firebase-management-no-credentials"
        title="Firebase não configurado"
        description="Credenciais do Firebase (FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  if (!status.project) {
    return (
      <EmptyState
        id="firebase-management-empty"
        title="Nenhum inventário sincronizado ainda"
        description="Projeto e Android apps ainda não foram sincronizados com a Firebase Management API."
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
      <div
        id="firebase-management-project"
        className="rounded-[var(--radius-card)] p-5"
        style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      >
        <div className="flex items-center justify-between gap-2">
          <h3 className="text-base font-semibold" style={{ color: "var(--text-primary)" }}>
            {status.project.displayName}
          </h3>
          <StatusBadge status={status.project.state === "ACTIVE" ? "ok" : "attention"} customLabel={status.project.state} />
        </div>
        <p className="mt-2 text-xs font-mono" style={{ color: "var(--text-tertiary)" }}>
          {status.project.projectId} · projeto nº {status.project.projectNumber}
        </p>
      </div>

      <div className="space-y-2.5">
        {status.androidApps.map((app) => (
          <div
            key={app.appId}
            id={`firebase-management-app-${app.appId}`}
            className="rounded-[var(--radius-card)] p-4 flex items-center justify-between gap-3"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <div className="min-w-0">
              <p className="text-sm font-medium truncate" style={{ color: "var(--text-primary)" }}>
                {app.displayName}
              </p>
              <p className="mt-0.5 text-xs font-mono truncate" style={{ color: "var(--text-tertiary)" }}>
                {app.packageName}
              </p>
            </div>
            <StatusBadge status={app.state === "ACTIVE" ? "ok" : "attention"} customLabel={app.state} />
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
