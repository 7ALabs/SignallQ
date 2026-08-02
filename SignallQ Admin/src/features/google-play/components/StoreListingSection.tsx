import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { GooglePlayStoreListingStatus } from "../../../integrations/google-play/googlePlay.types";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";

interface StoreListingSectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1342 — categoria "Distribuição": conteúdo de publicação (título/descrição por idioma na
 * ficha da loja), não é métrica — não usa `MetricCard`. Hoje só pt-BR é publicado; a lista
 * simplesmente renderiza o que o worker sincronizar, sem forçar seletor de idioma que não existe
 * ainda (quando houver mais de um idioma, cada um vira um card a mais, sem mudança de layout).
 */
export const StoreListingSection: React.FC<StoreListingSectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<GooglePlayStoreListingStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getGooglePlayStoreListingStatus()
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
      const result = await integrationsService.triggerGooglePlayStoreListingSync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar a ficha da loja.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar a ficha da loja.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando ficha da loja no Android Publisher API..." />;
  }

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="google-play-store-listing-no-credentials"
        title="Ficha da loja não configurada"
        description="Credenciais do Google Play (GOOGLE_PLAY_CLIENT_EMAIL/GOOGLE_PLAY_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  if (status.listings.length === 0) {
    return (
      <EmptyState
        id="google-play-store-listing-empty"
        title="Nenhuma ficha sincronizada ainda"
        description="A ficha da loja (título e descrição) ainda não foi sincronizada com a Android Publisher API."
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
      <div className="space-y-3">
        {status.listings.map((listing) => (
          <div
            key={listing.language}
            id={`google-play-store-listing-${listing.language}`}
            className="rounded-[var(--radius-card)] p-5"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <div className="flex items-center justify-between gap-2">
              <span
                className="text-[9px] font-mono px-1.5 py-0.5 rounded select-none uppercase"
                style={{
                  color: "var(--text-tertiary)",
                  backgroundColor: "var(--bg-base)",
                  border: "1px solid var(--border)",
                }}
              >
                {listing.language}
              </span>
            </div>

            <h3 className="mt-2.5 text-base font-semibold" style={{ color: "var(--text-primary)" }}>
              {listing.title}
            </h3>

            <p className="mt-2 text-xs leading-relaxed" style={{ color: "var(--text-secondary)" }}>
              {listing.shortDescription}
            </p>

            <p className="mt-3 text-xs leading-relaxed whitespace-pre-line" style={{ color: "var(--text-tertiary)" }}>
              {listing.fullDescription}
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
