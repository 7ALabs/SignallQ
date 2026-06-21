import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { ShieldAlert, Cpu, Database, WifiOff, FileText, CheckCircle2 } from "lucide-react";

interface InfrastructureAlert {
  id: string;
  source: string;
  message: string;
  severity: "high" | "medium" | "low";
  timestamp: string;
}

const mockInfraAlerts: InfrastructureAlert[] = [
  {
    id: "inf_al_01",
    source: "analytics_db",
    message: "Analytics DB com latência elevada em horário de pico de telemetria.",
    severity: "high",
    timestamp: "Há 12 min"
  },
  {
    id: "inf_al_02",
    source: "worker",
    message: "Cloudflare Workers subrequest limit exceeded nas requisições da zona Nordeste.",
    severity: "high",
    timestamp: "Há 32 min"
  },
  {
    id: "inf_al_03",
    source: "android_app",
    message: "Crash Outbreak: Taxa de crash-free rate sob v1.3.0-beta1 caiu abaixo de 98.4% devido a NullPointerException no scanner de WiFi.",
    severity: "medium",
    timestamp: "Há 2 horas"
  }
];

export const ErrorAlertsPanel: React.FC = () => {
  return (
    <SectionCard
      title="Alertas Críticos de Infraestrutura"
      description="Gargalos ativos em nossos servidores, bancos SQL e APIs capturados pelo robô de auditorias."
      id="infra-alerts-card"
    >
      <div className="space-y-3 font-sans text-xs">
        {mockInfraAlerts.map((alert) => {
          let icon = <Cpu className="w-4 h-4 text-zinc-450" />;
          let containerClass = "bg-[#161619] border-[#262626]";
          let labelColor = "text-white";

          if (alert.severity === "high") {
            icon = <Database className="w-4 h-4 text-[#FF4D4F] animate-pulse" />;
            containerClass = "bg-red-950/15 border-red-500/10";
            labelColor = "text-red-400";
          } else if (alert.severity === "medium") {
            icon = <WifiOff className="w-4 h-4 text-[#Eab308]" />;
            containerClass = "bg-amber-950/10 border-amber-500/10";
            labelColor = "text-[#Eab308]";
          }

          return (
            <div
              key={alert.id}
              className={`p-3 border rounded-xl flex items-start gap-2.5 hover:bg-zinc-900/50 transition-colors ${containerClass}`}
            >
              <span className="p-1 px-1.5 bg-zinc-950/40 border border-[#2d2d31] rounded-lg shrink-0 mt-0.5">
                {icon}
              </span>
              <div className="flex-1 min-w-0">
                <div className="flex items-center justify-between gap-1.5 select-none">
                  <span className="font-mono text-[9px] uppercase tracking-wider text-zinc-500 font-bold block">
                    {alert.source}
                  </span>
                  <span className="font-mono text-[9px] text-zinc-550 block shrink-0">{alert.timestamp}</span>
                </div>
                <p className="text-zinc-350 text-[10.5px] leading-snug mt-1 font-sans">
                  {alert.message}
                </p>
              </div>
            </div>
          );
        })}
      </div>
    </SectionCard>
  );
};
