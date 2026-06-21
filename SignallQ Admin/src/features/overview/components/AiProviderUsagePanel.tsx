import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { ProviderUsageItem } from "../../../mocks/overview.mock";
import { Cpu } from "lucide-react";

interface AiProviderUsagePanelProps {
  usage: ProviderUsageItem[];
}

export const AiProviderUsagePanel: React.FC<AiProviderUsagePanelProps> = ({ usage }) => {
  return (
    <ChartCard
      title="Uso de IA por Provedor"
      description="Divisão das requisições de triagem e inteligência preditiva processadas na extremidade."
      id="ai-provider-usage-card"
    >
      <div className="space-y-4 py-2">
        {/* Proportional Stacked Color bar */}
        <div className="w-full h-3 bg-[#18181B] rounded-full overflow-hidden flex border border-[#262626]/20 mb-2">
          {usage.map((item, idx) => (
            <div
              key={idx}
              className="h-full transition-all duration-300"
              style={{
                width: `${item.percentage}%`,
                backgroundColor: item.color,
              }}
              title={`${item.name}: ${item.percentage}%`}
            />
          ))}
        </div>

        {/* Legend listing */}
        <div className="space-y-3 pt-1">
          {usage.map((item, idx) => {
            return (
              <div key={idx} className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2.5">
                  <span
                    className="w-2.5 h-2.5 rounded-full shrink-0"
                    style={{ backgroundColor: item.color }}
                  />
                  <div>
                    <span className="text-white font-medium block">{item.name}</span>
                    <span className="text-[10px] text-[#9CA3AF] font-mono">
                      {item.tokensProcessed.toLocaleString("pt-BR")} tokens
                    </span>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <div className="px-2 py-0.5 rounded bg-[#18181B] border border-[#262626] font-mono text-[10px] text-[#9CA3AF]">
                    {(item.tokensProcessed / 1000).toFixed(0)}k Tkn
                  </div>
                  <span className="font-mono text-white font-bold w-10 text-right">
                    {item.percentage}%
                  </span>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </ChartCard>
  );
};
