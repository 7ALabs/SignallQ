import React from "react";
import { DeviceBreakdownMetric } from "../../../types/productAnalytics";

interface DeviceBreakdownListProps {
  metrics: DeviceBreakdownMetric[];
}

// #785 — modelo + versão Android + % de sessões, fonte diagnostic_sessions
// (D1). Mesmo padrão visual de barra/percentual já usado em TopIssuesPanel.
export const DeviceBreakdownList: React.FC<DeviceBreakdownListProps> = ({ metrics }) => {
  return (
    <div className="space-y-4 py-2">
      {metrics.map((item, idx) => (
        <div key={`${item.deviceModel}-${item.osVersion}`} className="space-y-1">
          <div className="flex justify-between items-center text-xs gap-2">
            <div className="flex items-center gap-2 min-w-0">
              <span className="w-5 h-5 rounded bg-[var(--bg-surface)] text-[10px] font-sans font-bold flex items-center justify-center border border-[var(--border)] text-[var(--text-secondary)] shrink-0">
                {idx + 1}
              </span>
              <div className="min-w-0">
                <span className="text-[var(--text-primary)] font-medium truncate block">{item.deviceModel}</span>
                <span className="text-[10px] text-[var(--text-tertiary)]">{item.osVersion}</span>
              </div>
            </div>
            <div className="flex items-center gap-2 font-mono text-[11px] shrink-0">
              <span className="text-[var(--text-secondary)]">{item.sessionCount} sessões</span>
              <span className="text-[var(--primary)] font-bold w-12 text-right">{item.percentage}%</span>
            </div>
          </div>
          <div className="w-full h-1.5 bg-[var(--bg-surface)] rounded-full overflow-hidden border border-[var(--border)]/20">
            <div
              className="h-full bg-gradient-to-r from-[var(--primary)] to-[var(--info)] rounded-full transition-all duration-500"
              style={{ width: `${Math.min(item.percentage, 100)}%` }}
            />
          </div>
        </div>
      ))}
    </div>
  );
};
