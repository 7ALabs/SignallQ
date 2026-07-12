import React from "react";
import { SectionCard } from "../../../components/ui/SectionCard";
import { QuotaMetric, GeminiQuotaResponse } from "../../../services/geminiQuotaService";

interface GeminiQuotaCardProps {
  quota: GeminiQuotaResponse | null;
}

const QUOTA_LABELS: Array<{ key: keyof GeminiQuotaResponse["quota"]; label: string }> = [
  { key: "requestsPerMinute", label: "Requests por minuto (RPM)" },
  { key: "tokensPerMinute", label: "Tokens por minuto (TPM)" },
  { key: "requestsPerDay", label: "Requests por dia (RPD)" },
];

function barColor(percentage: number): string {
  if (percentage >= 100) return "var(--error)";
  if (percentage >= 80) return "var(--attention)";
  return "var(--success)";
}

// #884 — quota do free tier Gemini (Gemini Flash, provedor primário de IA).
// A API do Gemini não expõe consulta de quota via REST (só autenticado em AI
// Studio) — o "usado" é contagem real do worker (ai_usage); sem o teto
// configurado em admin_settings, cada métrica mostra "Não disponível" com o
// motivo, nunca número inventado (mesmo padrão do resto do painel).
export const GeminiQuotaCard: React.FC<GeminiQuotaCardProps> = ({ quota }) => {
  return (
    <SectionCard
      title="Quota do free tier Gemini"
      description="Gemini Flash (provedor primário de IA) — % do teto gratuito consumido."
    >
      {!quota ? (
        <p className="text-xs font-sans py-4 text-center" style={{ color: "var(--sq-text-secondary)" }}>
          Carregando...
        </p>
      ) : (
        <div className="space-y-4 py-2">
          {QUOTA_LABELS.map(({ key, label }) => {
            const metric: QuotaMetric = quota.quota[key];
            return (
              <div key={key} className="space-y-1" id={`gemini-quota-${key}`}>
                <div className="flex justify-between items-center text-xs gap-2">
                  <span className="font-medium truncate" style={{ color: "var(--sq-text-primary)" }}>{label}</span>
                  {metric.available && metric.percentage != null ? (
                    <div className="flex items-center gap-2 font-mono text-[11px] shrink-0">
                      <span style={{ color: "var(--sq-text-secondary)" }}>
                        {(metric.used ?? 0).toLocaleString("pt-BR")} / {(metric.limit ?? 0).toLocaleString("pt-BR")}
                      </span>
                      <span className="font-bold w-10 text-right" style={{ color: barColor(metric.percentage) }}>
                        {metric.percentage}%
                      </span>
                    </div>
                  ) : (
                    <span className="text-[11px] font-sans shrink-0" style={{ color: "var(--sq-text-secondary)" }}>
                      Não disponível
                    </span>
                  )}
                </div>
                {metric.available && metric.percentage != null ? (
                  <div className="w-full h-1.5 bg-[var(--sq-bg-card)] rounded-full overflow-hidden border border-[var(--sq-border)]/20">
                    <div
                      className="h-full rounded-full transition-all duration-500"
                      style={{
                        width: `${Math.min(metric.percentage, 100)}%`,
                        backgroundColor: barColor(metric.percentage),
                      }}
                    />
                  </div>
                ) : (
                  <p className="text-[10px] leading-tight" style={{ color: "var(--sq-text-secondary)" }}>
                    {metric.reason ?? "Sem motivo informado."}
                  </p>
                )}
              </div>
            );
          })}
        </div>
      )}
    </SectionCard>
  );
};
