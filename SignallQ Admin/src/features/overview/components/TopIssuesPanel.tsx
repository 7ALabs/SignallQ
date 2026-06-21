import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { TopIssueItem } from "../../../mocks/overview.mock";

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
      <div className="space-y-4 py-2">
        {issues.map((item, idx) => {
          return (
            <div key={item.id || idx} className="space-y-1">
              <div className="flex justify-between items-center text-xs">
                <div className="flex items-center gap-2">
                  <span className="w-5 h-5 rounded bg-[#18181B] text-[10px] font-mono font-bold flex items-center justify-center border border-[#262626] text-[#9CA3AF]">
                    {idx + 1}
                  </span>
                  <span className="text-white font-medium">{item.problem}</span>
                </div>
                <div className="flex items-center gap-2 font-mono text-[11px]">
                  <span className="text-[#9CA3AF]">{item.count} varreduras</span>
                  <span className="text-[#6C2BFF] font-bold w-10 text-right">{item.percentage}%</span>
                </div>
              </div>
              <div className="w-full h-1.5 bg-[#18181B] rounded-full overflow-hidden border border-[#262626]/20">
                <div
                  className="h-full bg-gradient-to-r from-[#6C2BFF] to-[#38BDF8] rounded-full transition-all duration-500"
                  style={{ width: `${item.percentage}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>
    </ChartCard>
  );
};
