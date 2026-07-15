import React from "react";
import { ChartCard } from "../../../components/ui/ChartCard";
import { CloudflareResourceUsage, CloudflareUsageResponse } from "../../../services/cloudflareUsageService";

interface CloudflareUsagePanelProps {
  usage: CloudflareUsageResponse | null;
}

const RESOURCE_LABELS: Array<{ key: keyof CloudflareUsageResponse["resources"]; label: string }> = [
  { key: "workersRequestsDay", label: "Workers requests · hoje (UTC)" },
  { key: "d1RowsReadDay", label: "D1 rows lidas · hoje (UTC)" },
  { key: "d1RowsWrittenDay", label: "D1 rows escritas · hoje (UTC)" },
  { key: "d1StorageTotal", label: "D1 storage total" },
  { key: "workersAiNeuronsDay", label: "Workers AI Neurons · hoje (UTC)" },
];

function formatUnit(value: number, unit: CloudflareResourceUsage["unit"]): string {
  if (unit === "bytes") {
    const mb = value / (1024 * 1024);
    return mb >= 1024 ? `${(mb / 1024).toFixed(2)} GB` : `${mb.toFixed(1)} MB`;
  }
  return value.toLocaleString("pt-BR");
}

function barColor(percentage: number): string {
  if (percentage >= 100) return "var(--error)";
  if (percentage >= 80) return "var(--attention)";
  return "var(--success)";
}

// #883 — cada linha é um recurso do free tier Cloudflare (conta inteira, não só
// este worker). "Não disponível" é estado honesto quando CLOUDFLARE_API_TOKEN não
// está configurado no worker ou a consulta à GraphQL Analytics API falha — nunca
// número fabricado (mesmo padrão de FeatureComingSoon usado no resto do painel).
// #921 — Workers AI Neurons não tem dataset na GraphQL Analytics API (Cloudflare
// só expõe via dashboard), então esse recurso é sempre estimado a partir de tokens
// reais gravados em ai_usage — marcado com "(estimado)" no rótulo (resource.estimated).
export const CloudflareUsagePanel: React.FC<CloudflareUsagePanelProps> = ({ usage }) => {
  return (
    <ChartCard
      title="Uso do free tier Cloudflare"
      description="Workers e D1 no plano gratuito — % do teto diário/total consumido."
      id="cloudflare-usage-panel-card"
    >
      {!usage ? (
        <p className="text-xs font-sans py-4 text-center" style={{ color: "var(--text-tertiary)" }}>
          Carregando...
        </p>
      ) : (
        <div className="space-y-4 py-2">
          {RESOURCE_LABELS.map(({ key, label }) => {
            const resource = usage.resources[key];
            return (
              <div key={key} className="space-y-1" id={`cloudflare-usage-${key}`}>
                <div className="flex justify-between items-center text-xs gap-2">
                  <span className="text-[var(--text-primary)] font-medium truncate">
                    {label}
                    {resource.estimated ? (
                      <span className="ml-1 font-normal" style={{ color: "var(--text-tertiary)" }}>
                        (estimado)
                      </span>
                    ) : null}
                  </span>
                  {resource.available && resource.percentage != null ? (
                    <div className="flex items-center gap-2 font-mono text-[11px] shrink-0">
                      <span className="text-[var(--text-secondary)]">
                        {formatUnit(resource.used ?? 0, resource.unit)} / {formatUnit(resource.limit ?? 0, resource.unit)}
                      </span>
                      <span className="font-bold w-10 text-right" style={{ color: barColor(resource.percentage) }}>
                        {resource.percentage}%
                      </span>
                    </div>
                  ) : (
                    <span className="text-[11px] font-sans shrink-0" style={{ color: "var(--text-tertiary)" }}>
                      Não disponível
                    </span>
                  )}
                </div>
                {resource.available && resource.percentage != null ? (
                  <div className="w-full h-1.5 bg-[var(--bg-surface)] rounded-full overflow-hidden border border-[var(--border)]/20">
                    <div
                      className="h-full rounded-full transition-all duration-500"
                      style={{
                        width: `${Math.min(resource.percentage, 100)}%`,
                        backgroundColor: barColor(resource.percentage),
                      }}
                    />
                  </div>
                ) : (
                  <p className="text-[10px] leading-tight" style={{ color: "var(--text-tertiary)" }}>
                    {resource.reason ?? "Sem motivo informado."}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}
    </ChartCard>
  );
};
