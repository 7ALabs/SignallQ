import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";

import { DiagnosticsSummary } from "../../../types/diagnostics";

interface DiagnosticsMetricGridProps {
  environment: "production" | "staging";
  summary: DiagnosticsSummary | null;
}

export const DiagnosticsMetricGrid: React.FC<DiagnosticsMetricGridProps> = ({ environment, summary }) => {
  // We can scale very slightly if staging is selected, or stick beautifully to requested values as baseline.
  const isStg = environment === "staging";
  
  const scoreValue = summary ? `${summary.averageScore}/100` : (isStg ? "72/100" : "78/100");
  const latencyValue = summary ? `${summary.averageLatencyMs} ms` : (isStg ? "31 ms" : "24 ms");
  const jitterValue = summary ? `${summary.averageJitterMs} ms` : (isStg ? "11 ms" : "8 ms");
  const lossValue = summary ? `${summary.averagePacketLossPercentage.toLocaleString("pt-BR")}%` : (isStg ? "1,2%" : "0,7%");
  const downloadValue = summary ? `${summary.averageDownloadMbps} Mbps` : (isStg ? "198 Mbps" : "284 Mbps");
  const uploadValue = summary ? `${summary.averageUploadMbps} Mbps` : (isStg ? "64 Mbps" : "92 Mbps");
  
  const metrics = [
    {
      label: "Score Médio",
      value: scoreValue,
      trend: { value: 1.2, changePercentage: 1.2, type: (isStg ? "down" : "up") as "up" | "down", intervalLabel: "vs semana anterior" }
    },
    {
      label: "Latência Média",
      value: latencyValue,
      trend: { value: 4.8, changePercentage: 4.8, type: (isStg ? "up" : "down") as "up" | "down", intervalLabel: "estabilidade global" }
    },
    {
      label: "Jitter Médio",
      value: jitterValue,
      trend: { value: 2.1, changePercentage: 2.1, type: "neutral" as "neutral", intervalLabel: "variação estável" }
    },
    {
      label: "Perda Média",
      value: lossValue,
      trend: { value: 0.1, changePercentage: 14.1, type: (isStg ? "up" : "down") as "up" | "down", intervalLabel: "taxa de descarte" }
    },
    {
      label: "Download Médio",
      value: downloadValue,
      trend: { value: 8.5, changePercentage: 8.5, type: "up" as "up", intervalLabel: "vs semana anterior" }
    },
    {
      label: "Upload Médio",
      value: uploadValue,
      trend: { value: 3.2, changePercentage: 3.2, type: "up" as "up", intervalLabel: "fluxo local" }
    }
  ];

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-6 gap-4">
      {metrics.map((m, idx) => (
        <MetricCard
          key={idx}
          label={m.label}
          value={m.value}
          trend={m.trend}
          id={`diag-grid-metric-${idx}`}
        />
      ))}
    </div>
  );
};
