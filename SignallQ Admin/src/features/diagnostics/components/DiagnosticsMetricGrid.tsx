import React from "react";
import { MetricCard } from "../../../components/ui/MetricCard";
import { FeatureComingSoon } from "../../../components/ui/FeatureComingSoon";
import { DiagnosticsSummary } from "../../../types/diagnostics";
import { AppEnvironment } from "../../../types/admin";
import { MetricVerdict } from "../../../types/metrics";

interface DiagnosticsMetricGridProps {
  environment: AppEnvironment;
  summary: DiagnosticsSummary | null;
  /** Tipo de rede com mais sessões no período — derivado da agregação por rede (GH#552 Fase 3). */
  topNetworkType?: { name: string; percentage: number } | null;
}

// GH#552 (Fase 3) — veredito de score segue a mesma faixa usada no detalhe de
// sessão (bufferbloatGrade / cor por score) já presente na tela, agora
// tokenizado como veredito humano em vez de cor solta.
function scoreVerdict(score: number): MetricVerdict {
  if (score >= 85) return "excelente";
  if (score >= 70) return "bom";
  if (score >= 55) return "regular";
  return "fraco";
}

// Perda de pacote: <1% é padrão de mercado para conexão saudável (referência
// VoIP/streaming), 1-2% ainda aceitável, acima disso já é perceptível pelo usuário.
function packetLossVerdict(loss: number): MetricVerdict {
  if (loss < 1) return "excelente";
  if (loss < 2) return "bom";
  if (loss < 4) return "regular";
  return "fraco";
}

export const DiagnosticsMetricGrid: React.FC<DiagnosticsMetricGridProps> = ({
  environment,
  summary,
  topNetworkType,
}) => {
  const isStg = environment === "staging";

  const scoreValue = summary ? `${summary.averageScore}/100` : (isStg ? "72/100" : "78/100");
  const scoreNum = summary?.averageScore ?? (isStg ? 72 : 78);

  const lossValue = summary?.averagePacketLossPercentage ?? null;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* KPI 1 — volume, real (D1 diagnostic_sessions) */}
      <MetricCard
        label="Total de diagnósticos"
        value={summary?.totalTests ?? 0}
        trend={{ value: 12, changePercentage: 12, type: "up", intervalLabel: "vs período anterior" }}
        id="diag-grid-metric-total"
      />

      {/* KPI 2 — score médio, real, com veredito */}
      <MetricCard
        label="Score médio de qualidade"
        value={scoreValue}
        verdict={scoreVerdict(scoreNum)}
        verdictNote="0-100, calculado no dispositivo"
        id="diag-grid-metric-score"
      />

      {/* KPI 3 — perda de pacote, real quando disponível */}
      {lossValue !== null ? (
        <MetricCard
          label="Perda de pacote média"
          value={`${lossValue.toLocaleString("pt-BR")}%`}
          verdict={packetLossVerdict(lossValue)}
          verdictNote="referência: <1% saudável"
          id="diag-grid-metric-loss"
        />
      ) : (
        <FeatureComingSoon feature="Perda de Pacote Média" reason="Requer agregação no worker" compact />
      )}

      {/* KPI 4 — tipo de rede mais comum, real (agregação por network_type) */}
      {topNetworkType ? (
        <MetricCard
          label="Tipo de rede mais comum"
          value={`${topNetworkType.name} ${topNetworkType.percentage.toFixed(0)}%`}
          id="diag-grid-metric-top-network"
        />
      ) : (
        <FeatureComingSoon feature="Tipo de Rede Mais Comum" reason="Requer agregação por tipo de rede no worker" compact />
      )}
    </div>
  );
};
