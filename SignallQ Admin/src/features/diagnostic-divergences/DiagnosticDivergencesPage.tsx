import React from "react";
import { RefreshCw } from "lucide-react";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { LoadingState } from "../../components/ui/LoadingState";
import { DivergencesSummaryCards } from "./components/DivergencesSummaryCards";
import { DivergencesTable } from "./components/DivergencesTable";
import { diagnosticDivergencesService } from "../../services/diagnosticDivergencesService";
import { DiagnosticDivergenceRecord, DiagnosticDivergencesSummary, DivergenceClassification } from "../../types/diagnosticDivergences";

// Refs #1446 (shadow mode, GH#1444) — tela de leitura: mostra o que o cliente Android
// já classificou (local-vs-remoto) via GET /admin/diagnostic/divergences. Nenhuma ação
// de escrita aqui — é o painel de observação que informa a decisão de rollout na tela
// de rulesets (ex.: taxa alta de MAJOR_DIVERGENCE é sinal pra não subir o percentual).
export const DiagnosticDivergencesPage: React.FC = () => {
  const [items, setItems] = React.useState<DiagnosticDivergenceRecord[]>([]);
  const [summary, setSummary] = React.useState<DiagnosticDivergencesSummary>({});
  const [classification, setClassification] = React.useState<DivergenceClassification | null>(null);
  const [limit, setLimit] = React.useState<number>(50);
  const [loading, setLoading] = React.useState(true);
  const [error, setError] = React.useState<string | null>(null);

  const load = React.useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await diagnosticDivergencesService.list({ classification: classification ?? undefined, limit });
      setItems(res.items);
      setSummary(res.summary);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível carregar as divergências.");
    } finally {
      setLoading(false);
    }
  }, [classification, limit]);

  React.useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <SectionIntro
          id="diagnostic-divergences-section-intro"
          overline="MOTOR DE DIAGNÓSTICO · SHADOW MODE"
          question="Onde o resultado local diverge do resultado remoto?"
          description="Comparação já classificada no cliente Android entre o diagnóstico local (bundled) e o resultado do ruleset remoto — sem alterar o que o usuário vê. Use para decidir se um rollout está seguro pra subir."
          source="FONTE · SIGNALLQ-DIAGNOSTIC-WORKER (D1)"
        />
        <button
          type="button"
          onClick={load}
          disabled={loading}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs font-semibold shrink-0 cursor-pointer disabled:opacity-50"
          style={{ backgroundColor: "var(--bg-surface-hover)", color: "var(--text-secondary)", border: "1px solid var(--border)" }}
        >
          <RefreshCw className={`w-4 h-4 ${loading ? "animate-spin" : ""}`} />
          Atualizar
        </button>
      </div>

      <DivergencesSummaryCards
        id="diagnostic-divergences-summary"
        summary={summary}
        activeClassification={classification}
        onSelect={setClassification}
      />

      <div className="flex items-center justify-between gap-3">
        <p className="text-[12px]" style={{ color: "var(--text-tertiary)" }}>
          {classification ? `Filtrando por classificação selecionada acima — clique de novo pra limpar.` : "Sem filtro de classificação — clique em um card acima pra filtrar."}
        </p>
        <select
          value={limit}
          onChange={(e) => setLimit(Number(e.target.value))}
          className="bg-[var(--bg-surface)] border border-[var(--border)] rounded-xl px-3 py-2 text-xs text-[var(--text-primary)] focus:outline-none focus:border-[var(--primary)] cursor-pointer font-sans"
        >
          <option value={50}>Últimos 50</option>
          <option value={100}>Últimos 100</option>
          <option value={200}>Últimos 200</option>
        </select>
      </div>

      {loading ? (
        <LoadingState message="Carregando divergências do shadow mode..." />
      ) : error ? (
        <div
          className="rounded-[var(--radius-card)] p-4 text-[13px]"
          style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--error)", color: "var(--error)" }}
        >
          {error}
        </div>
      ) : (
        <DivergencesTable id="diagnostic-divergences-table" items={items} />
      )}
    </div>
  );
};
