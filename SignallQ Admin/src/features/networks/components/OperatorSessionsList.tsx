import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { OperatorRecord } from "../../../types/admin";
import { NetworkTypeStat } from "../../../services/adminMetricsService";

interface OperatorSessionsListProps {
  operators: OperatorRecord[];
  wifiStat?: NetworkTypeStat;
  mobileStat?: NetworkTypeStat;
}

// GH#781 (paridade mockup) — "Sessões por operadora" com % do total de testes
// consolidados (mesmo dado real de OperatorRecord.testCount usado na tabela
// principal) e o resumo Wi-Fi vs. rede móvel no rodapé, a partir de
// NetworkTypeStat.count (já usado nos cards "Wi-Fi/Rede móvel · diagnósticos
// no período" logo abaixo, nesta mesma tela).
export const OperatorSessionsList: React.FC<OperatorSessionsListProps> = ({ operators, wifiStat, mobileStat }) => {
  const total = operators.reduce((sum, op) => sum + op.testCount, 0);
  const ranked = [...operators].sort((a, b) => b.testCount - a.testCount).slice(0, 6);

  const wifiCount = wifiStat?.count ?? 0;
  const mobileCount = mobileStat?.count ?? 0;
  const wifiMobileTotal = wifiCount + mobileCount;
  const wifiPct = wifiMobileTotal > 0 ? Math.round((wifiCount / wifiMobileTotal) * 100) : null;
  const mobilePct = wifiMobileTotal > 0 ? Math.round((mobileCount / wifiMobileTotal) * 100) : null;

  return (
    <ChartCard
      title="Sessões por operadora"
      description="Participação de cada operadora no total de testes consolidados no período."
      id="operator-sessions-list-card"
    >
      {ranked.length === 0 ? (
        <FeatureComingSoon
          feature="Sessões por operadora"
          reason="Métrica ainda não disponível — aguardando exposição no worker"
        />
      ) : (
        <>
          <div className="space-y-2.5 py-1">
            {ranked.map((op) => {
              const pct = total > 0 ? Math.round((op.testCount / total) * 100) : 0;
              return (
                <div key={op.id} className="flex items-center gap-2.5">
                  <span className="text-[12.5px] font-medium font-sans text-[var(--text-primary)] flex-1 truncate">{op.name}</span>
                  <div className="w-20 h-1.5 rounded-full overflow-hidden" style={{ backgroundColor: "var(--bg-base)" }}>
                    <div className="h-full rounded-full" style={{ width: `${pct}%`, backgroundColor: "var(--accent-blue)" }} />
                  </div>
                  <span className="text-[12.5px] font-semibold font-sans text-[var(--text-secondary)] w-9 text-right">{pct}%</span>
                </div>
              );
            })}
          </div>
          <div className="mt-4 pt-3.5 flex justify-between" style={{ borderTop: "1px solid var(--border)" }}>
            <span className="text-[11px] font-sans text-[var(--text-tertiary)]">Wi-Fi</span>
            <span className="text-xs font-semibold font-sans text-[var(--text-primary)]">{wifiPct != null ? `${wifiPct}%` : "—"}</span>
          </div>
          <div className="mt-1.5 flex justify-between">
            <span className="text-[11px] font-sans text-[var(--text-tertiary)]">Rede móvel</span>
            <span className="text-xs font-semibold font-sans text-[var(--text-primary)]">{mobilePct != null ? `${mobilePct}%` : "—"}</span>
          </div>
        </>
      )}
    </ChartCard>
  );
};
