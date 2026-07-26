import React from "react";
import { Plus } from "lucide-react";
import { SectionIntro } from "../../components/ui/SectionIntro";
import { LoadingState } from "../../components/ui/LoadingState";
import { ConfirmDialog } from "../../components/ui/ConfirmDialog";
import { RulesetsTable } from "./components/RulesetsTable";
import { RulesetDetailPanel } from "./components/RulesetDetailPanel";
import { RulesetEditorDrawer } from "./components/RulesetEditorDrawer";
import { SimulatePanel } from "./components/SimulatePanel";
import { diagnosticRulesetsService } from "../../services/diagnosticRulesetsService";
import { RulesetDetail, RulesetListItem } from "../../types/diagnosticRulesets";

export const DiagnosticRulesetsPage: React.FC = () => {
  const [items, setItems] = React.useState<RulesetListItem[]>([]);
  const [loadingList, setLoadingList] = React.useState(true);
  const [listError, setListError] = React.useState<string | null>(null);

  const [selectedVersion, setSelectedVersion] = React.useState<number | null>(null);
  const [detail, setDetail] = React.useState<RulesetDetail | null>(null);
  const [loadingDetail, setLoadingDetail] = React.useState(false);

  const [editorOpen, setEditorOpen] = React.useState(false);
  const [editorSeed, setEditorSeed] = React.useState<RulesetDetail | null>(null);

  const [simulateOpen, setSimulateOpen] = React.useState(false);
  const [simulateTarget, setSimulateTarget] = React.useState<RulesetDetail | null>(null);

  const [pendingAction, setPendingAction] = React.useState<{ type: "publish" | "rollback"; version: number } | null>(null);
  const [actionBusy, setActionBusy] = React.useState(false);
  const [actionError, setActionError] = React.useState<string | null>(null);

  const loadList = React.useCallback(async () => {
    setLoadingList(true);
    setListError(null);
    try {
      const data = await diagnosticRulesetsService.listRulesets();
      setItems(data);
      // Mantém a versão publicada em foco por padrão na primeira carga.
      if (selectedVersion === null && data.length > 0) {
        const published = data.find((r) => r.status === "PUBLISHED");
        setSelectedVersion((published ?? data[0]).version);
      }
    } catch (e) {
      setListError(e instanceof Error ? e.message : "Não foi possível carregar os rulesets.");
    } finally {
      setLoadingList(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  React.useEffect(() => {
    loadList();
  }, [loadList]);

  React.useEffect(() => {
    if (selectedVersion === null) {
      setDetail(null);
      return;
    }
    let active = true;
    setLoadingDetail(true);
    diagnosticRulesetsService.getRuleset(selectedVersion).then((d) => {
      if (active) {
        setDetail(d);
        setLoadingDetail(false);
      }
    });
    return () => {
      active = false;
    };
  }, [selectedVersion]);

  const handleOpenEditor = (seed: RulesetDetail | null) => {
    setEditorSeed(seed);
    setEditorOpen(true);
  };

  const handleDraftCreated = async (version: number) => {
    setEditorOpen(false);
    await loadList();
    setSelectedVersion(version);
  };

  const handleConfirmAction = async () => {
    if (!pendingAction) return;
    setActionBusy(true);
    setActionError(null);
    const result =
      pendingAction.type === "publish"
        ? await diagnosticRulesetsService.publish(pendingAction.version)
        : await diagnosticRulesetsService.rollback(pendingAction.version);
    setActionBusy(false);
    if (!result.ok) {
      setActionError(result.error ?? "Ação falhou.");
      return;
    }
    setPendingAction(null);
    await loadList();
    setSelectedVersion(pendingAction.version);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <SectionIntro
          id="diagnostic-rulesets-section-intro"
          overline="MOTOR DE DIAGNÓSTICO"
          question="Qual ruleset está publicado e o que está em rascunho?"
          description="Listar, editar como JSON validado, simular contra fixtures e publicar/reverter as regras que o motor de diagnóstico usa em produção."
          source="FONTE · SIGNALLQ-DIAGNOSTIC-WORKER (D1)"
        />
        <button
          type="button"
          onClick={() => handleOpenEditor(detail)}
          className="flex items-center gap-1.5 px-4 py-2.5 rounded-xl text-xs font-semibold shrink-0 cursor-pointer"
          style={{ backgroundColor: "var(--primary)", color: "var(--on-primary)" }}
        >
          <Plus className="w-4 h-4" />
          Novo draft
        </button>
      </div>

      {loadingList ? (
        <LoadingState message="Carregando rulesets de diagnóstico..." />
      ) : listError ? (
        <div
          className="rounded-[var(--radius-card)] p-4 text-[13px]"
          style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--error)", color: "var(--error)" }}
        >
          {listError}
        </div>
      ) : (
        <div className="grid grid-cols-1 xl:grid-cols-[1fr_1fr] gap-6 items-start">
          <RulesetsTable
            id="diagnostic-rulesets-table"
            items={items}
            selectedVersion={selectedVersion}
            onSelect={setSelectedVersion}
          />
          <div
            className="rounded-[var(--radius-card)] p-5"
            style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
          >
            <RulesetDetailPanel
              id="diagnostic-ruleset-detail-panel"
              detail={detail}
              loading={loadingDetail}
              onEditAsDraft={(d) => handleOpenEditor(d)}
              onPublish={(version) => setPendingAction({ type: "publish", version })}
              onRollback={(version) => setPendingAction({ type: "rollback", version })}
              onSimulate={(d) => {
                setSimulateTarget(d);
                setSimulateOpen(true);
              }}
            />
          </div>
        </div>
      )}

      <RulesetEditorDrawer
        id="diagnostic-ruleset-editor-drawer"
        open={editorOpen}
        initialRuleset={editorSeed?.ruleset ?? null}
        onClose={() => setEditorOpen(false)}
        onCreated={handleDraftCreated}
      />

      <SimulatePanel
        id="diagnostic-ruleset-simulate-panel"
        open={simulateOpen}
        ruleset={simulateTarget?.ruleset ?? null}
        onClose={() => setSimulateOpen(false)}
      />

      <ConfirmDialog
        id="diagnostic-ruleset-confirm-dialog"
        open={pendingAction !== null}
        tone={pendingAction?.type === "rollback" ? "danger" : "default"}
        title={pendingAction?.type === "publish" ? `Publicar ruleset v${pendingAction.version}?` : `Reverter ruleset v${pendingAction?.version}?`}
        description={
          pendingAction?.type === "publish" ? (
            <>
              Isso substitui o ruleset atualmente publicado e passa a valer para <strong>100% do rollout</strong> em
              produção imediatamente. Essa ação fica registrada na trilha de auditoria.
            </>
          ) : (
            <>
              Isso reverte esta versão e restaura a versão publicada anterior. O motor de diagnóstico em produção
              volta a usar o ruleset anterior imediatamente. Essa ação fica registrada na trilha de auditoria.
            </>
          )
        }
        confirmLabel={pendingAction?.type === "publish" ? "Publicar" : "Reverter"}
        busy={actionBusy}
        onConfirm={handleConfirmAction}
        onCancel={() => {
          setPendingAction(null);
          setActionError(null);
        }}
      />

      {actionError && (
        <div
          className="fixed bottom-6 right-6 z-50 rounded-[var(--radius-card)] px-4 py-3 text-[13px] max-w-sm"
          style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--error)", color: "var(--error)" }}
        >
          {actionError}
        </div>
      )}
    </div>
  );
};
