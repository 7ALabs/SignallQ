import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { LineChart } from "../../../components/charts/LineChart";

interface DiagnosticsTimelineProps {
  timelineData: any[];
  period: string;
}

export const DiagnosticsTimeline: React.FC<DiagnosticsTimelineProps> = ({ timelineData, period }) => {
  const getTimelineTitle = () => {
    switch (period) {
      case "today":
        return "Frequência de Diagnósticos Hoje (Por Hora)";
      case "30d":
        return "Varreduras Consolidadas por Semana (Últimos 30 Dias)";
      default:
        return "Consumo de Diagnósticos vs Usuários (Histórico 7 Dias)";
    }
  };

  const getTimelineDescription = () => {
    switch (period) {
      case "today":
        return "Acompanhamento da telemetria de conectividade e taxa de varreduras disparada a cada duas horas.";
      case "30d":
        return "Estatísticas semanais agregadas de conectividade e integridade recolhidas de todos os dispositivos.";
      default:
        return "Frequência diária de varreduras de conectividade completas disparadas via SDK Android.";
    }
  };

  return (
    <ChartCard
      title={getTimelineTitle()}
      description={getTimelineDescription()}
      id="diagnostics-timeline-card"
    >
      <LineChart
        data={timelineData}
        xAxisKey="timestamp"
        series={[
          { key: "completedDiagnostics", name: "Diagnósticos Executados", color: "#6C2BFF" },
          { key: "activeUsers", name: "Dispositivos Ativos", color: "#38BDF8" },
        ]}
        height={260}
      />
    </ChartCard>
  );
};
