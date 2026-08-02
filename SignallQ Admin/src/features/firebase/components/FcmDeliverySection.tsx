import React from "react";
import { RefreshCw } from "lucide-react";
import { integrationsService } from "../../../integrations/integrationsService";
import { FirebaseFcmDeliveryStatus } from "../../../integrations/firebase/firebase.types";
import { LoadingState } from "../../../components/ui/LoadingState";
import { EmptyState } from "../../../components/ui/EmptyState";

interface FcmDeliverySectionProps {
  triggerRefreshCounter: number;
}

/**
 * GH#1343/#1344 — categoria "FCM": delivery data por app/dia, hoje sempre vazio porque o
 * SignallQ não envia push ainda — não há mensagem pra medir entrega. Estado vazio explica o
 * motivo real (não é falha de sincronização nem gráfico fingindo dado com zero).
 */
export const FcmDeliverySection: React.FC<FcmDeliverySectionProps> = ({ triggerRefreshCounter }) => {
  const [loading, setLoading] = React.useState(true);
  const [status, setStatus] = React.useState<FirebaseFcmDeliveryStatus | null>(null);
  const [syncing, setSyncing] = React.useState(false);
  const [syncError, setSyncError] = React.useState<string | null>(null);

  const load = React.useCallback(() => {
    let cancelled = false;
    setLoading(true);
    integrationsService
      .getFirebaseFcmDeliveryStatus()
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
      const result = await integrationsService.triggerFirebaseFcmDeliverySync();
      if (result.status !== "ok") {
        setSyncError(result.message ?? "Falha ao sincronizar o FCM.");
      }
      load();
    } catch {
      setSyncError("Falha ao sincronizar o FCM.");
    } finally {
      setSyncing(false);
    }
  }, [load]);

  if (loading) {
    return <LoadingState message="Buscando delivery data no Firebase Cloud Messaging Data API..." />;
  }

  if (!status?.hasCredentials) {
    return (
      <EmptyState
        id="firebase-fcm-delivery-no-credentials"
        title="Firebase não configurado"
        description="Credenciais do Firebase (FIREBASE_CLIENT_EMAIL/FIREBASE_PRIVATE_KEY) ainda não foram registradas no Admin Worker."
      />
    );
  }

  return (
    <div className="space-y-3">
      <EmptyState
        id="firebase-fcm-delivery-no-push"
        title="SignallQ ainda não envia notificações push"
        description="Não há dado de entrega para mostrar porque o app não usa Firebase Cloud Messaging hoje — esta tela passa a ter conteúdo quando o SignallQ começar a enviar push."
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

      <div className="flex items-center justify-end gap-3 text-[11px]" style={{ color: "var(--text-tertiary)" }}>
        <span>
          Última sincronização:{" "}
          {status.lastSyncTimestamp ? new Date(status.lastSyncTimestamp).toLocaleString("pt-BR") : "nunca"}
        </span>
      </div>

      {syncError && (
        <p className="text-[11px]" style={{ color: "var(--error)" }}>
          {syncError}
        </p>
      )}
    </div>
  );
};
