import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { BarChart } from "../../../components/charts/BarChart";
import { errorMetricsService } from "../../../services/errorMetricsService";
import { ErrorByEndpointEntry } from "../../../mocks/errors.mock";
import { AppEnvironment } from "../../../types/admin";

interface ErrorByEndpointChartProps {
  environment: AppEnvironment;
}

export const ErrorByEndpointChart: React.FC<ErrorByEndpointChartProps> = ({ environment }) => {
  const [chartData, setChartData] = React.useState<ErrorByEndpointEntry[]>([]);

  React.useEffect(() => {
    let active = true;
    errorMetricsService.getErrorByEndpoint({ environment }).then((data) => {
      if (active) setChartData(data);
    });
    return () => { active = false; };
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
        series={[{ key: "erros", name: "Dumps Técnico Detectados", color: "var(--error)" }]}
        height={240}
      />
    </ChartCard>
  );
};
