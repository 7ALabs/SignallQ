import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { BarChart } from "../../../components/charts/BarChart";

interface ErrorByEndpointChartProps {
  environment: "production" | "staging";
}

export const ErrorByEndpointChart: React.FC<ErrorByEndpointChartProps> = ({ environment }) => {
  const isStg = environment === "staging";

  const chartData = React.useMemo(() => {
    if (isStg) {
      return [
        { name: "AI Gateway", erros: 12 },
        { name: "Android App", erros: 45 },
        { name: "Edge Worker", erros: 45 },
        { name: "Analytics DB", erros: 2 },
      ];
    }
    return [
      { name: "AI Gateway", erros: 382 },
      { name: "Android App", erros: 1544 },
      { name: "Edge Worker", erros: 45 },
      { name: "Analytics DB", erros: 8 },
    ];
  }, [environment]);

  return (
    <ChartCard
      title="Volume Histórico de Erros por Interface"
      description="Frequência bruta agregada das exceções capturadas nos logs técnicos."
      id="error-endpoint-chart"
    >
      <BarChart
        data={chartData}
        xAxisKey="name"
        series={[{ key: "erros", name: "Dumps Técnico Detectados", color: "#FF4D4F" }]}
        height={240}
      />
    </ChartCard>
  );
};
