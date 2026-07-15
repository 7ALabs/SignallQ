import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { LoadingState } from "../../../components/ui/LoadingState";
import { StatusBadge } from "../../../components/ui/StatusBadge";
import { integrationsService } from "../../../integrations/integrationsService";
import { FirebaseCrashIssue, FirebaseCrashIssuesResult } from "../../../integrations/firebase/firebase.types";
import { AppEnvironment } from "../../../types/admin";

interface TopCrashesCardProps {
  environment: AppEnvironment;
  period: string;
  triggerRefreshCounter: number;
}

// Mesmo threshold já usado em getFirebaseAppVersions (firebaseAdapter.ts) para
// classificar severidade de crash sem inventar escala nova — só renomeado
// para os rótulos que StatusBadge já suporta (critical/attention/ok).
function severityFor(totalCrashes: number): "critical" | "attention" | "ok" {
  if (totalCrashes > 100) return "critical";
  if (totalCrashes > 20) return "attention";
  return "ok";
}

function formatLastSeen(epochMs: number): string {
  if (!epochMs) return "-";
  return new Date(epochMs).toLocaleString("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// Paridade com o mockup do Luiz (sec-errors): card full-width "TOP CRASHES".
// Busca própria via GET /admin/integrations/firebase/crash-issues — loading e
// erro independentes do resto da ErrorsPage, para não travar a tela inteira
// se essa integração falhar. "Sem crashes registrados" (lista vazia real) é
// um estado diferente de "integração não existe" (era o texto antigo, errado).
export const TopCrashesCard: React.FC<TopCrashesCardProps> = ({
  environment,
  period,
  triggerRefreshCounter,
}) => {
  const [issues, setIssues] = React.useState<FirebaseCrashIssue[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);
  const [emptyReason, setEmptyReason] = React.useState<string | null>(null);

  React.useEffect(() => {
    let active = true;
    setLoading(true);
    setError(null);

    integrationsService
      .getFirebaseIssues({ environment, period })
      .then((result: FirebaseCrashIssuesResult) => {
        if (!active) return;
        if (result.source === "no_credentials") {
          setIssues([]);
          setEmptyReason("Firebase não configurado no Admin Worker");
        } else if (result.source === "no_data_yet" || result.issues.length === 0) {
          setIssues([]);
          setEmptyReason("Sem crashes registrados no período");
        } else if (result.source === "error") {
          setIssues([]);
          setEmptyReason(null);
          setError("Erro ao consultar o BigQuery — tente novamente");
        } else {
          setIssues(result.issues);
          setEmptyReason(null);
        }
      })
      .catch(() => {
        if (active) setError("Não foi possível carregar os crashes — worker indisponível");
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [environment, period, triggerRefreshCounter]);

  return (
    <ChartCard title="TOP CRASHES" id="top-crashes-card">
      {loading && <LoadingState message="Buscando crashes no Firebase Crashlytics..." rows={4} />}

      {!loading && (error || emptyReason) && (
        <FeatureComingSoon
          feature="Top crashes por assinatura"
          reason={error ?? emptyReason ?? undefined}
        />
      )}

      {!loading && !error && !emptyReason && (
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr style={{ borderBottom: "1px solid var(--border)" }}>
                <th className="py-2 px-3 text-[10px] font-mono uppercase tracking-wider font-normal" style={{ color: "var(--text-tertiary)" }}>
                  Assinatura
                </th>
                <th className="py-2 px-3 text-right text-[10px] font-mono uppercase tracking-wider font-normal" style={{ color: "var(--text-tertiary)" }}>
                  Ocorrências
                </th>
                <th className="py-2 px-3 text-right text-[10px] font-mono uppercase tracking-wider font-normal" style={{ color: "var(--text-tertiary)" }}>
                  Usuários Afetados
                </th>
                <th className="py-2 px-3 text-right text-[10px] font-mono uppercase tracking-wider font-normal" style={{ color: "var(--text-tertiary)" }}>
                  Versão
                </th>
                <th className="py-2 px-3 text-right text-[10px] font-mono uppercase tracking-wider font-normal" style={{ color: "var(--text-tertiary)" }}>
                  Última Ocorrência
                </th>
                <th className="py-2 px-3 text-right text-[10px] font-mono uppercase tracking-wider font-normal" style={{ color: "var(--text-tertiary)" }}>
                  Status
                </th>
              </tr>
            </thead>
            <tbody>
              {issues.map((issue) => (
                <tr key={issue.id} style={{ borderBottom: "1px solid var(--border)" }}>
                  <td className="py-3 px-3 text-[12px] font-sans font-medium" style={{ color: "var(--text-primary)" }}>
                    {issue.title}
                  </td>
                  <td className="py-3 px-3 text-right text-[12px] font-mono" style={{ color: "var(--text-primary)" }}>
                    {issue.totalCrashes.toLocaleString("pt-BR")}
                  </td>
                  <td className="py-3 px-3 text-right text-[12px] font-mono" style={{ color: "var(--text-primary)" }}>
                    {issue.affectedUsers.toLocaleString("pt-BR")}
                  </td>
                  <td className="py-3 px-3 text-right text-[12px] font-mono" style={{ color: "var(--text-secondary)" }}>
                    {issue.appVersion ?? "-"}
                  </td>
                  <td className="py-3 px-3 text-right text-[11px] font-mono" style={{ color: "var(--text-tertiary)" }}>
                    {formatLastSeen(issue.lastSeen)}
                  </td>
                  <td className="py-3 px-3 text-right">
                    <StatusBadge status={severityFor(issue.totalCrashes)} />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </ChartCard>
  );
};
