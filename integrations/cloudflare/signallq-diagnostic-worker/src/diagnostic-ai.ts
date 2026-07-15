import type { DiagnosticSnapshot } from "./contracts.ts";
import type { DiagnosticReportPayload } from "./diagnostic-report.ts";

export interface DiagnosticAiPromptPackage {
  version: number;
  mode: "single_shot_explainer";
  shouldInvoke: boolean;
  reason: string;
  systemPrompt: string;
  userPrompt: string;
  expectedOutputSchema: {
    format: "json";
    fields: string[];
  };
}

function summarizeSnapshot(snapshot: DiagnosticSnapshot): Record<string, unknown> {
  return {
    connection: snapshot.connection ?? null,
    wifi: snapshot.wifi ?? null,
    wifiScan: snapshot.wifiScan
      ? {
          connectedChannel: snapshot.wifiScan.connectedChannel ?? null,
          networksCount: snapshot.wifiScan.networks?.length ?? 0,
        }
      : null,
    speed: snapshot.speed ?? null,
    quality: snapshot.quality ?? null,
    dns: snapshot.dns ?? null,
    fiber: snapshot.fiber ?? null,
    mobile: snapshot.mobile ?? null,
    historical: snapshot.historical ?? null,
    gateway: snapshot.gateway ?? null,
  };
}

function shouldInvokeAi(report: DiagnosticReportPayload): { shouldInvoke: boolean; reason: string } {
  if (report.decisao.status === "inconclusive") {
    return {
      shouldInvoke: true,
      reason: "O diagnóstico ficou inconclusivo e a IA pode melhorar a explicação sem mudar a decisão técnica.",
    };
  }

  if (report.dadosAusentes.length >= 3) {
    return {
      shouldInvoke: true,
      reason: "Há muitos dados ausentes e a IA pode resumir a situação e as próximas ações em linguagem mais natural.",
    };
  }

  if (report.achadosSecundarios.length >= 2) {
    return {
      shouldInvoke: true,
      reason: "Há múltiplos achados relevantes e a IA pode consolidar a narrativa sem alterar a conclusão do motor.",
    };
  }

  if (report.decisao.status === "critical" && report.recomendacoes.length >= 3) {
    return {
      shouldInvoke: true,
      reason: "O caso é crítico e a IA pode priorizar melhor a comunicação da ação imediata.",
    };
  }

  return {
    shouldInvoke: false,
    reason: "O motor já entregou uma resposta suficientemente clara para a tela atual sem necessidade de pós-explicação por IA.",
  };
}

export function buildDiagnosticAiPrompt(snapshot: DiagnosticSnapshot, report: DiagnosticReportPayload): DiagnosticAiPromptPackage {
  const decision = shouldInvokeAi(report);

  const systemPrompt = [
    "Voce e um explicador de diagnostico de rede para o app SignallQ.",
    "Nao faca perguntas ao usuario.",
    "Nao conduza conversa nem fluxo de chat.",
    "Nao contradiga a decisao tecnica recebida.",
    "Nao invente sinais ausentes nem troque a causa raiz escolhida pelo motor.",
    "Use exclusivamente os dados estruturados recebidos.",
    "Responda apenas em JSON valido.",
    "Se houver ambiguidade, reconheca a limitacao com honestidade e preserve os dadosAusentes.",
    "Priorize linguagem humana, direta e calma.",
    // GH#958 — reforco explicito de voz do produto (North Star "The Calm Translator").
    "Fale sempre em segunda pessoa, direcionando o texto ao usuario com 'voce' e 'sua conexao' — nunca em terceira pessoa ou linguagem impessoal.",
    "Nunca use emoji em nenhum campo da resposta.",
  ].join(" ");

  const userPrompt = JSON.stringify(
    {
      instruction:
        "Transforme o diagnostico tecnico abaixo em uma explicacao unica, curta e clara para exibicao opcional apos o motor. Nao faca perguntas. Nao sugira chat. Nao altere a decisao principal.",
      output_rules: {
        language: "pt-BR",
        max_actions: 3,
        max_watchouts: 2,
        no_markdown: true,
      },
      expected_fields: [
        "headline",
        "summary",
        "why_this_decision",
        "actions_now",
        "watchouts",
        "confidence_note",
      ],
      diagnostic_report: report,
      raw_snapshot_summary: summarizeSnapshot(snapshot),
    },
    null,
    2,
  );

  return {
    version: 1,
    mode: "single_shot_explainer",
    shouldInvoke: decision.shouldInvoke,
    reason: decision.reason,
    systemPrompt,
    userPrompt,
    expectedOutputSchema: {
      format: "json",
      fields: [
        "headline",
        "summary",
        "why_this_decision",
        "actions_now",
        "watchouts",
        "confidence_note",
      ],
    },
  };
}
