import React from "react";
import { RetentionMetric } from "../../../types/productAnalytics";
import { SectionCard } from "../../../components/ui/SectionCard";
import { AlertCircle } from "lucide-react";

interface RetentionPanelProps {
  metrics: RetentionMetric[];
  sessionDuration?: { avgDurationMs: number | null; sessionCount: number } | null;
}

const pct = (v: number | null | undefined): string => (v == null ? "—" : `${v.toFixed(0)}%`);

const formatDuration = (ms: number | null | undefined): string => {
  if (ms == null) return "—";
  const totalSec = Math.round(ms / 1000);
  const min = Math.floor(totalSec / 60);
  const sec = totalSec % 60;
  return min > 0 ? `${min}m ${sec}s` : `${sec}s`;
};

export const RetentionPanel: React.FC<RetentionPanelProps> = ({ metrics, sessionDuration }) => {
  const m = metrics[0];

  // GH#552 (Fase 3) — D1/D7 já aparecem como KPI no topo da tela (com veredito
  // de mercado); aqui fica só o que não é redundante: D30, duração de sessão e
  // o proxy de inatividade do cohort.
  const rates = [
    { label: "Retenção D30", val: pct(m?.day30), desc: "Retorno no primeiro mês" },
    { label: "Tempo Médio de Sessão", val: formatDuration(sessionDuration?.avgDurationMs), desc: `${sessionDuration?.sessionCount ?? 0} sessões no período` }
  ];

  return (
    <SectionCard
      title="Contexto do cohort de retenção"
      description="Coorte de dispositivos por primeiro evento visto (device_id anônimo) e duração média de sessão."
    >
      <div className="space-y-6">
        {m && (
          <div className="flex justify-between items-center mb-1 bg-zinc-950/20 px-3 py-1.5 rounded border border-zinc-900/45">
            <span className="text-[11px] font-sans text-[var(--text-secondary)]">Filtro Ativo: {m.cohort}</span>
            <span className="text-[10px] font-bold text-indigo-400">
              Cohort: {m.cohortSize ?? 0} dispositivos · Tempo médio ativo: {m.avgInstalledDays != null ? `${m.avgInstalledDays} dias` : "—"}
            </span>
          </div>
        )}

        <div className="grid grid-cols-2 gap-4">
          {rates.map((rate, i) => (
            <div key={i} className="p-4 bg-zinc-950/40 border border-zinc-900 rounded-xl flex flex-col justify-between">
              <span className="text-[10px] font-sans uppercase text-[var(--text-tertiary)] tracking-wider font-semibold">{rate.label}</span>
              <div className="my-2.5">
                <span className="text-xl font-bold font-mono text-[var(--text-primary)]">{rate.val}</span>
              </div>
              <p className="text-[10px] font-sans text-[var(--text-tertiary)] leading-tight">{rate.desc}</p>
            </div>
          ))}
        </div>

        {m && (
          <div className="p-4 bg-[var(--bg-sidebar)]/30 border border-dashed border-[var(--border)] rounded-xl space-y-2">
            <div className="flex items-start gap-2.5">
              <AlertCircle className="w-4 h-4 text-amber-500 shrink-0 mt-0.5" />
              <div className="text-[10px] font-sans text-[var(--text-secondary)] leading-relaxed space-y-1.5">
                <p className="text-[var(--text-primary)] font-semibold">
                  Proxy de inatividade: {pct(m.uninstallRate)}
                </p>
                <p>
                  % de dispositivos do cohort sem nenhum evento registrado nos últimos 14 dias.
                  Isto é um proxy de inatividade calculado a partir da telemetria — não é confirmação
                  de desinstalação real (isso exigiria integração com Play Console, não implementada).
                  Causas específicas de abandono (versão, fluxo) não são atribuíveis com os dados
                  disponíveis hoje.
                </p>
              </div>
            </div>
          </div>
        )}
      </div>
    </SectionCard>
  );
};
