import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { TopIssueItem } from "../../../mocks/overview.mock";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";

interface TopIssuesPanelProps {
  issues: TopIssueItem[];
}

export const TopIssuesPanel: React.FC<TopIssuesPanelProps> = ({ issues }) => {
  return (
    <ChartCard
      title="Problemas mais Comuns"
      description="Principais gargalos e falhas de infraestrutura residencial e móvel analisados nas varreduras."
      id="top-issues-panel-card"
    >
      {issues.length === 0 ? (
        <FeatureComingSoon
          feature="Top Issues"
          reason="Requer agregação no worker"
        />
      ) : (
        <div className="space-y-4 py-2">
          {issues.map((item, idx) => {
            return (
              <div key={item.id || idx} className="space-y-1">
                <div className="flex justify-between items-center text-xs">
                  <div className="flex items-center gap-2">
                    <span className="w-5 h-5 rounded bg-[var(--bg-surface)] text-[10px] font-sans font-bold flex items-center justify-center border border-[var(--border)] text-[var(--text-secondary)]">
                      {idx + 1}
                    </span>
                    <span className="text-[var(--text-primary)] font-medium">{item.problem}</span>
                  </div>
                  <div className="flex items-center gap-2 font-mono text-[11px]">
                    <span className="text-[var(--text-secondary)]">{item.count} varreduras</span>
                    <span className="text-[var(--primary)] font-bold w-10 text-right">{item.percentage}%</span>
                  </div>
                </div>
                <div className="w-full h-1.5 bg-[var(--bg-surface)] rounded-full overflow-hidden border border-[var(--border)]/20">
                  <div
                    className="h-full bg-gradient-to-r from-[var(--primary)] to-[var(--info)] rounded-full transition-all duration-500"
                    style={{ width: `${item.percentage}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </ChartCard>
  );
};
