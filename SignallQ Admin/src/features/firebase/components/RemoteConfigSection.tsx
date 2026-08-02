import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { FirebaseRemoteConfigStatus } from "../../../integrations/firebase/firebase.types";
import { MetricCard } from "../../../components/ui/MetricCard";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";
import { TermHint } from "../../../components/ui/TermHint";

interface RemoteConfigSectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1343/#1344 — categoria "Remote Config": hoje só temos contagem e nomes de chave de
 * parâmetro (o valor de cada parâmetro é dívida registrada na issue #1349) — a tela mostra
 * exatamente isso, sem fingir ter mais dado do que tem (sem coluna de valor, sem tabela de
 * "valor por parâmetro").
 */
export const RemoteConfigSection: React.FC<RemoteConfigSectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<FirebaseRemoteConfigStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getFirebaseRemoteConfigStatus()
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
      const result = await integrationsService.triggerFirebaseRemoteConfigSync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar o Remote Config.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar o Remote Config.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando template no Firebase Remote Config API..." />;
  }

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="firebase-remote-config-no-credentials"
        title="Firebase não configurado"
        description="Credenciais do Firebase (FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  if (status.parameterCount === 0) {
    return (
      <EmptyState
        id="firebase-remote-config-empty"
        title="Nenhum parâmetro sincronizado ainda"
        description="O template do Remote Config ainda não foi sincronizado, ou não tem nenhum parâmetro cadastrado no Console."
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
      <MetricCard
        id="firebase-remote-config-parameter-count"
        size="hero"
        label="Parâmetros cadastrados"
        labelExtra={<TermHint term="remoteConfigParameter" />}
        value={status.parameterCount}
        source="firebase"
      />

      <div
        id="firebase-remote-config-keys"
        className="rounded-[var(--radius-card)] p-5"
        style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
      >
        <p className="text-[11px] font-mono uppercase tracking-[0.08em]" style={{ color: "var(--text-tertiary)" }}>
          Chaves
        </p>
        <div className="mt-2.5 flex flex-wrap gap-2">
          {status.parameterKeys.map((key) => (
            <span
              key={key}
              className="text-xs font-mono px-2 py-1 rounded"
              style={{ color: "var(--text-secondary)", backgroundColor: "var(--bg-base)", border: "1px solid var(--border)" }}
            >
              {key}
            </span>
          ))}
        </div>
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
