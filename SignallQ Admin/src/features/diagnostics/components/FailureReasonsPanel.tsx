import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { DiagnosticSession } from "../../../types/diagnostics";

interface FailureReasonsPanelProps {
  sessions: DiagnosticSession[];
}

const ISSUE_LABELS: Record<string, string> = {
  sinal_fraco: "Sinal fraco",
  alta_latencia: "Alta latência",
  falha_dns: "Falha de DNS",
  jitter_alto: "Jitter alto",
  perda_de_pacotes: "Perda de pacotes",
  upload_lento: "Upload lento",
  download_lento: "Download lento",
  problema_fibra: "Problema de fibra",
  gateway_inacessivel: "Gateway inacessível",
  bufferbloat: "Bufferbloat",
  interferencia_canal_wifi: "Interferência de canal Wi-Fi",
  problema_banda: "Problema de banda",
};

// GH#781 (paridade mockup) — "Motivos de falha" a partir das issues já
// carregadas nas sessões de diagnóstico (mesma fonte da tabela de
// investigação), sem inventar categoria de timeout/permissão que o app
// ainda não reporta como campo próprio.
export const FailureReasonsPanel: React.FC<FailureReasonsPanelProps> = ({ sessions }) => {
  const counts = new Map<string, number>();
  let total = 0;
  sessions.forEach((s) => {
    s.issues.forEach((i) => {
      counts.set(i.issue, (counts.get(i.issue) ?? 0) + 1);
      total += 1;
    });
  });
  const ranked = [...counts.entries()].sort((a, b) => b[1] - a[1]).slice(0, 6);

  return (
    <ChartCard
      title="Motivos de falha"
      description="Anomalias mais frequentes identificadas nas sessões de diagnóstico do período."
      id="failure-reasons-panel-card"
    >
      {ranked.length === 0 ? (
        <FeatureComingSoon
          feature="Motivos de falha"
          reason="Nenhuma anomalia registrada nas sessões do período selecionado"
        />
      ) : (
        <div className="space-y-3 py-1">
          {ranked.map(([issue, count]) => {
            const pct = total > 0 ? Math.round((count / total) * 100) : 0;
            return (
              <div key={issue}>
                <div className="flex justify-between items-baseline mb-1">
                  <span className="text-xs font-sans text-[var(--text-secondary)]">
                    {ISSUE_LABELS[issue] ?? issue.replace(/_/g, " ")}
                  </span>
                  <span className="text-xs font-semibold font-sans text-[var(--text-primary)]">{pct}%</span>
                </div>
                <div className="h-1.5 rounded-full overflow-hidden" style={{ backgroundColor: "var(--bg-base)" }}>
                  <div className="h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: "var(--attention)" }} />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </ChartCard>
  );
};
