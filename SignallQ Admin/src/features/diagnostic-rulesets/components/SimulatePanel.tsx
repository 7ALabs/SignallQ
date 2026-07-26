import React from "react";
import { X, PlayCircle } from "lucide-react";
import { diagnosticRulesetsService } from "../../../services/diagnosticRulesetsService";
import { SIMULATE_FIXTURES } from "../simulateFixtures";
import { DiagnosticRuleset, SimulateResult } from "../../../types/diagnosticRulesets";
import { StatusBadge } from "../../../components/ui/StatusBadge";

interface SimulatePanelProps {
  open: boolean;
  ruleset: DiagnosticRuleset | null;
  onClose: () => void;
  id?: string;
}

export const SimulatePanel: React.FC<SimulatePanelProps> = ({ open, ruleset, onClose, id }) => {
  const [fixtureId, setFixtureId] = React.useState(SIMULATE_FIXTURES[0].id);
  const [running, setRunning] = React.useState(false);
  const [result, setResult] = React.useState<SimulateResult | null>(null);

  React.useEffect(() => {
    if (open) {
      setResult(null);
      setFixtureId(SIMULATE_FIXTURES[0].id);
    }
  }, [open]);

  if (!open || !ruleset) return null;

  const fixture = SIMULATE_FIXTURES.find((f) => f.id === fixtureId) ?? SIMULATE_FIXTURES[0];

  const handleRun = async () => {
    setRunning(true);
    try {
      const res = await diagnosticRulesetsService.simulate(fixture.snapshot, ruleset);
      setResult(res);
    } finally {
      setRunning(false);
    }
  };

  return (
    <div id={id} className="fixed inset-0 z-40 flex justify-end">
      <div className="absolute inset-0" style={{ backgroundColor: "rgba(0,0,0,0.5)" }} onClick={onClose} />
      <div
        className="relative w-full max-w-xl h-full flex flex-col"
        style={{ backgroundColor: "var(--bg-surface)", borderLeft: "1px solid var(--border)" }}
      >
        <div className="flex items-center justify-between px-6 py-4" style={{ borderBottom: "1px solid var(--border)" }}>
          <div>
            <h3 className="text-[15px] font-semibold" style={{ color: "var(--text-primary)" }}>
              Simular ruleset v{ruleset.version}
            </h3>
            <p className="text-[12px] mt-0.5" style={{ color: "var(--text-tertiary)" }}>
              Contra snapshots sintéticos de exemplo — não são sessões reais
            </p>
          </div>
          <button type="button" onClick={onClose} aria-label="Fechar" className="p-1.5 rounded-lg cursor-pointer" style={{ color: "var(--text-tertiary)" }}>
            <X className="w-4 h-4" />
          </button>
        </div>

        <div className="flex-1 overflow-auto px-6 py-4 space-y-4">
          <div>
            <label className="text-[11px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
              Fixture
            </label>
            <div className="mt-1.5 space-y-2">
              {SIMULATE_FIXTURES.map((f) => (
                <label
                  key={f.id}
                  className="flex items-start gap-2.5 rounded-[var(--radius-card)] p-3 cursor-pointer"
                  style={{
                    backgroundColor: fixtureId === f.id ? "var(--bg-surface-hover)" : "var(--bg-base)",
                    border: `1px solid ${fixtureId === f.id ? "var(--primary)" : "var(--border)"}`,
                  }}
                >
                  <input
                    type="radio"
                    name="fixture"
                    checked={fixtureId === f.id}
                    onChange={() => {
                      setFixtureId(f.id);
                      setResult(null);
                    }}
                    className="mt-0.5"
                  />
                  <span>
                    <span className="block text-[13px] font-medium" style={{ color: "var(--text-primary)" }}>
                      {f.label}
                    </span>
                    <span className="block text-[11px] mt-0.5" style={{ color: "var(--text-tertiary)" }}>
                      {f.description}
                    </span>
                  </span>
                </label>
              ))}
            </div>
          </div>

          <button
            type="button"
            onClick={handleRun}
            disabled={running}
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-semibold cursor-pointer disabled:opacity-50"
            style={{ backgroundColor: "var(--primary)", color: "var(--on-primary)" }}
          >
            <PlayCircle className="w-4 h-4" />
            {running ? "Simulando..." : "Rodar simulação"}
          </button>

          {result && (
            <div
              className="rounded-[var(--radius-card)] p-4 space-y-3"
              style={{ backgroundColor: "var(--bg-surface-variant)", border: "1px solid var(--border)" }}
            >
              {result.ok && result.result ? (
                <>
                  <div className="flex items-center gap-2">
                    <StatusBadge
                      status={
                        result.result.overallStatus === "OK"
                          ? "ok"
                          : result.result.overallStatus === "CRITICAL"
                            ? "critical"
                            : result.result.overallStatus === "INCONCLUSIVE"
                              ? "cached"
                              : "attention"
                      }
                      customLabel={result.result.overallStatus}
                    />
                    <span className="text-[12px]" style={{ color: "var(--text-secondary)" }}>
                      score {result.result.score} · confiança {result.result.confidence}
                    </span>
                  </div>
                  <p className="text-[13px]" style={{ color: "var(--text-primary)" }}>{result.result.humanSummary}</p>
                  <div>
                    <p className="text-[10px] font-semibold uppercase tracking-[0.06em]" style={{ color: "var(--text-tertiary)" }}>
                      Regras casadas ({result.result.matchedRules.length})
                    </p>
                    <p className="text-[12px] font-mono mt-1" style={{ color: "var(--text-secondary)" }}>
                      {result.result.matchedRules.length > 0 ? result.result.matchedRules.join(", ") : "nenhuma"}
                    </p>
                  </div>
                </>
              ) : (
                <p className="text-[13px]" style={{ color: "var(--error)" }}>
                  {(result.errors ?? ["Falha ao simular."]).join(" · ")}
                </p>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
