import React from "react";
import { AlertTriangle, CheckCircle, TrendingUp, DollarSign } from "lucide-react";
import { SectionCard } from "../../../components/ui/SectionCard";

interface AlertThresholdPanelProps {
  crashRatePercent: number | null;
  aiCostToday: number | null;
  crashRateThreshold: number;
  aiCostThreshold: number;
  loading: boolean;
}

interface ThresholdRowProps {
  icon: React.ReactNode;
  label: string;
  value: string;
  threshold: string;
  isAlert: boolean;
  loading: boolean;
}

const ThresholdRow: React.FC<ThresholdRowProps> = ({
  icon,
  label,
  value,
  threshold,
  isAlert,
  loading,
}) => {
  const alertColor = "var(--sq-error, #ef4444)";
  const okColor = "var(--sq-success, #22c55e)";
  const activeColor = isAlert ? alertColor : okColor;

  return (
    <div
      className="flex items-center gap-4 p-4 rounded-xl"
      style={{
        backgroundColor: isAlert
          ? "color-mix(in srgb, var(--sq-error, #ef4444) 6%, var(--bg-surface-hover))"
          : "var(--bg-surface-hover)",
        border: `1px solid ${isAlert ? "color-mix(in srgb, var(--sq-error, #ef4444) 20%, transparent)" : "var(--border)"}`,
      }}
    >
      <div
        className="p-2.5 rounded-xl shrink-0"
        style={{
          backgroundColor: `color-mix(in srgb, ${activeColor} 10%, transparent)`,
          border: `1px solid color-mix(in srgb, ${activeColor} 20%, transparent)`,
        }}
      >
        <div style={{ color: activeColor }}>{icon}</div>
      </div>

      <div className="flex-1 min-w-0">
        <p
          className="text-[13px] font-semibold"
          style={{ color: "var(--text-primary)" }}
        >
          {label}
        </p>
        <p className="text-[11px] font-sans mt-0.5" style={{ color: "var(--text-tertiary)" }}>
          Threshold: {threshold}
        </p>
      </div>

      {loading ? (
        <div
          className="h-8 w-24 rounded-lg animate-pulse"
          style={{ backgroundColor: "var(--border)" }}
        />
      ) : (
        <div className="flex items-center gap-2 shrink-0">
          <span
            className="text-[15px] font-bold font-mono"
            style={{ color: isAlert ? alertColor : "var(--text-primary)" }}
          >
            {value}
          </span>
          {isAlert ? (
            <span
              className="flex items-center gap-1 text-[10px] font-sans font-semibold uppercase tracking-wider px-2 py-1 rounded-full"
              style={{
                color: alertColor,
                backgroundColor: "color-mix(in srgb, var(--sq-error, #ef4444) 12%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-error, #ef4444) 25%, transparent)",
              }}
            >
              <AlertTriangle className="w-3 h-3" />
              Alerta
            </span>
          ) : (
            <span
              className="flex items-center gap-1 text-[10px] font-sans font-semibold uppercase tracking-wider px-2 py-1 rounded-full"
              style={{
                color: okColor,
                backgroundColor: "color-mix(in srgb, var(--sq-success, #22c55e) 12%, transparent)",
                border: "1px solid color-mix(in srgb, var(--sq-success, #22c55e) 25%, transparent)",
              }}
            >
              <CheckCircle className="w-3 h-3" />
              Normal
            </span>
          )}
        </div>
      )}
    </div>
  );
};

export const AlertThresholdPanel: React.FC<AlertThresholdPanelProps> = ({
  crashRatePercent,
  aiCostToday,
  crashRateThreshold,
  aiCostThreshold,
  loading,
}) => {
  const crashValue =
    crashRatePercent == null ? "—" : `${crashRatePercent.toFixed(2)}%`;
  const aiCostValue =
    aiCostToday == null
      ? "—"
      : aiCostToday === 0
      ? "$0,00"
      : `$${aiCostToday.toFixed(4)}`;

  const crashAlert = crashRatePercent != null && crashRatePercent > crashRateThreshold;
  const costAlert = aiCostToday != null && aiCostToday > aiCostThreshold;
  const anyAlert = crashAlert || costAlert;

  return (
    <SectionCard
      title="Alertas de Threshold"
      description="Limites configuráveis para crash rate e custo diário de IA. Badge vermelho indica threshold excedido."
      id="alert-threshold-panel"
      actions={
        anyAlert ? (
          <span
            className="flex items-center gap-1.5 text-[11px] font-sans font-semibold px-2.5 py-1 rounded-full"
            style={{
              color: "var(--sq-error, #ef4444)",
              backgroundColor: "color-mix(in srgb, var(--sq-error, #ef4444) 10%, transparent)",
              border: "1px solid color-mix(in srgb, var(--sq-error, #ef4444) 20%, transparent)",
            }}
          >
            <AlertTriangle className="w-3.5 h-3.5" />
            {[crashAlert, costAlert].filter(Boolean).length} alerta{anyAlert && [crashAlert, costAlert].filter(Boolean).length > 1 ? "s" : ""} ativo{anyAlert && [crashAlert, costAlert].filter(Boolean).length > 1 ? "s" : ""}
          </span>
        ) : !loading ? (
          <span
            className="flex items-center gap-1.5 text-[11px] font-sans font-semibold px-2.5 py-1 rounded-full"
            style={{
              color: "var(--sq-success, #22c55e)",
              backgroundColor: "color-mix(in srgb, var(--sq-success, #22c55e) 10%, transparent)",
              border: "1px solid color-mix(in srgb, var(--sq-success, #22c55e) 20%, transparent)",
            }}
          >
            <CheckCircle className="w-3.5 h-3.5" />
            Tudo normal
          </span>
        ) : null
      }
    >
      <div className="space-y-3">
        <ThresholdRow
          icon={<TrendingUp className="w-4 h-4" />}
          label="Crash Rate"
          value={crashValue}
          threshold={`> ${crashRateThreshold}%`}
          isAlert={crashAlert}
          loading={loading}
        />
        <ThresholdRow
          icon={<DollarSign className="w-4 h-4" />}
          label="Custo IA (hoje)"
          value={aiCostValue}
          threshold={`> $${aiCostThreshold}/dia`}
          isAlert={costAlert}
          loading={loading}
        />
      </div>
    </SectionCard>
  );
};
