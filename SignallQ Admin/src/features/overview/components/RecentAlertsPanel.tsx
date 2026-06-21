import React from "react";
import { AlertList } from "../../../components/ui/AlertList";
import { RecentAlertItem } from "../../../mocks/overview.mock";
import { AlertOctagon } from "lucide-react";

interface RecentAlertsPanelProps {
  alerts: RecentAlertItem[];
}

export const RecentAlertsPanel: React.FC<RecentAlertsPanelProps> = ({ alerts }) => {
  return (
    <div className="bg-[#111111] border border-[#262626] rounded-[18px] p-5 hover:border-[#363636] transition-all duration-200 flex flex-col justify-between h-full">
      <div>
        <div className="flex items-center justify-between gap-4 pb-4 border-b border-[#262626] mb-5 select-none">
          <div>
            <h4 className="text-xs font-semibold font-mono uppercase tracking-wider text-neutral-400">
              Alertas Recentes
            </h4>
            <p className="text-[11px] text-zinc-500 font-sans mt-0.5">
              Anomalias operacionais e picos de falha de rádio disparados nas últimas horas.
            </p>
          </div>
          <AlertOctagon className="w-5 h-5 text-[#FF4D4F] shrink-0" />
        </div>

        <div className="max-h-[290px] overflow-y-auto pr-1 space-y-1">
          <AlertList alerts={alerts} />
        </div>
      </div>
    </div>
  );
};
