import { apiClient } from "./apiClient";
import {
  DiagnosticRuleset,
  RulesetDetail,
  RulesetListItem,
  RulesetStatus,
  RulesetValidationResult,
  SimulateResult,
} from "../types/diagnosticRulesets";

// Refs #1446 — todas as rotas passam pelo signallq-admin-worker (mesma origem do
// Console, sessão httpOnly já validada) que faz proxy transparente para
// /admin/diagnostic/* no signallq-diagnostic-worker via service binding — ver
// proxyDiagnosticAdmin em signallq-admin-worker/src/index.ts. O Console nunca
// fala direto com o diagnostic-worker.
const BASE = "/admin/diagnostic";

// GH#960/GH#241 padrão do worker: `handleRulesetList` sem D1 devolve o item
// bundled em camelCase (schemaVersion/publishedAt), enquanto `listRulesets`
// com D1 devolve as colunas cruas em snake_case (schema_version/published_at).
// Normaliza os dois formatos aqui — produção sempre tem D1, mas dev local sem
// binding não deve quebrar a tela.
function normalizeListItem(raw: Record<string, unknown>): RulesetListItem {
  return {
    version: Number(raw.version ?? 0),
    schemaVersion: Number(raw.schemaVersion ?? raw.schema_version ?? 0),
    engineVersion: Number(raw.engineVersion ?? raw.engine_version ?? 0),
    status: (raw.status as RulesetStatus) ?? "DRAFT",
    rolloutPercent: Number(raw.rolloutPercent ?? raw.rollout_percent ?? 0),
    publishedAt: (raw.publishedAt as string) ?? (raw.published_at as string) ?? null,
    updatedAt: (raw.updatedAt as string) ?? (raw.updated_at as string) ?? null,
  };
}

function normalizeDetail(raw: Record<string, unknown>): RulesetDetail {
  const listPart = normalizeListItem(raw);
  const rulesJsonRaw = raw.rules_json ?? raw.rulesJson;
  let ruleset: DiagnosticRuleset;
  if (typeof rulesJsonRaw === "string") {
    ruleset = JSON.parse(rulesJsonRaw) as DiagnosticRuleset;
  } else if (rulesJsonRaw && typeof rulesJsonRaw === "object") {
    ruleset = rulesJsonRaw as DiagnosticRuleset;
  } else {
    // Fallback bundled (sem D1): a própria resposta já É o ruleset.
    ruleset = raw as unknown as DiagnosticRuleset;
  }
  return {
    ...listPart,
    author: (raw.author as string) ?? null,
    justification: (raw.justification as string) ?? null,
    ruleset,
  };
}

export const diagnosticRulesetsService = {
  async listRulesets(): Promise<RulesetListItem[]> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return mockList();
    }
    const raw = await apiClient.request<{ items: Record<string, unknown>[] }>("GET", `${BASE}/rulesets`);
    return (raw.items ?? []).map(normalizeListItem).sort((a, b) => b.version - a.version);
  },

  async getRuleset(version: number): Promise<RulesetDetail | null> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return mockDetail(version);
    }
    try {
      const raw = await apiClient.request<Record<string, unknown>>("GET", `${BASE}/rulesets/${version}`);
      return normalizeDetail(raw);
    } catch {
      return null;
    }
  },

  async validateRuleset(ruleset: unknown): Promise<RulesetValidationResult> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return mockValidate(ruleset);
    }
    try {
      const res = await apiClient.request<{ ok: boolean; version?: number; rules?: number }>(
        "POST",
        `${BASE}/rulesets/validate`,
        ruleset as Record<string, unknown>
      );
      return { ok: res.ok, version: res.version, rules: res.rules };
    } catch (e: any) {
      // ApiError não carrega o corpo JSON de erro (só status) — apiClient.request
      // lança antes de expor `details`. Sem detalhe estruturado, expõe o que dá.
      return { ok: false, errors: [e?.message ?? "Falha ao validar ruleset."] };
    }
  },

  async createDraft(ruleset: unknown, justification: string): Promise<{ ok: boolean; version?: number; error?: string }> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return { ok: true, version: (ruleset as { version?: number })?.version };
    }
    try {
      const res = await apiClient.request<{ ok: boolean; version: number }>("POST", `${BASE}/rulesets`, {
        ruleset,
        justification,
      });
      return { ok: res.ok, version: res.version };
    } catch (e: any) {
      return { ok: false, error: e?.message ?? "Falha ao criar draft." };
    }
  },

  async simulate(snapshot: unknown, ruleset: unknown): Promise<SimulateResult> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return mockSimulate();
    }
    try {
      const res = await apiClient.request<SimulateResult>("POST", `${BASE}/simulate`, {
        snapshot,
        ruleset,
      } as Record<string, unknown>);
      return res;
    } catch (e: any) {
      return { ok: false, errors: [e?.message ?? "Falha ao simular."] };
    }
  },

  async publish(version: number): Promise<{ ok: boolean; error?: string }> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return { ok: true };
    }
    try {
      await apiClient.request("POST", `${BASE}/rulesets/${version}/publish`);
      return { ok: true };
    } catch (e: any) {
      return { ok: false, error: e?.message ?? "Falha ao publicar." };
    }
  },

  async rollback(version: number): Promise<{ ok: boolean; error?: string }> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return { ok: true };
    }
    try {
      await apiClient.request("POST", `${BASE}/rulesets/${version}/rollback`);
      return { ok: true };
    } catch (e: any) {
      return { ok: false, error: e?.message ?? "Falha ao reverter." };
    }
  },
};

// ---- Mocks (dev local sem VITE_ADMIN_API_BASE_URL / VITE_ENABLE_MOCKS=true) ----

const MOCK_RULESET_V3: DiagnosticRuleset = {
  version: 3,
  schemaVersion: 3,
  engineVersion: 1,
  publishedAt: "2026-07-20T12:00:00.000Z",
  rules: [
    {
      ruleId: "wifi_rssi_fraco",
      ruleVersion: 1,
      enabled: true,
      priority: 10,
      minimumSchemaVersion: 1,
      conditions: [{ field: "wifi.rssiDbm", operator: "LT", value: -75 }],
      result: {
        findingCode: "wifi_sinal_fraco",
        category: "wifi",
        severity: "WARNING",
        confidence: "HIGH",
        recommendationId: "REC-01",
      },
    },
  ],
};

function mockList(): RulesetListItem[] {
  return [
    { version: 3, schemaVersion: 3, engineVersion: 1, status: "PUBLISHED", rolloutPercent: 100, publishedAt: "2026-07-20T12:00:00.000Z", updatedAt: "2026-07-20T12:00:00.000Z" },
    { version: 2, schemaVersion: 3, engineVersion: 1, status: "ROLLED_BACK", rolloutPercent: 0, publishedAt: "2026-07-10T09:00:00.000Z", updatedAt: "2026-07-20T12:00:00.000Z" },
    { version: 4, schemaVersion: 3, engineVersion: 1, status: "DRAFT", rolloutPercent: 0, publishedAt: null, updatedAt: "2026-07-24T15:30:00.000Z" },
  ];
}

function mockDetail(version: number): RulesetDetail | null {
  const item = mockList().find((r) => r.version === version);
  if (!item) return null;
  return {
    ...item,
    author: version === 4 ? "marina@signallq.app" : "camilo@signallq.app",
    justification: version === 4 ? "Ajuste de limiar de RSSI (issue #1440)." : "Publicação inicial do motor v3.",
    ruleset: { ...MOCK_RULESET_V3, version },
  };
}

function mockValidate(ruleset: unknown): RulesetValidationResult {
  const r = ruleset as Partial<DiagnosticRuleset> | null;
  if (!r || typeof r !== "object" || !Array.isArray(r.rules) || typeof r.version !== "number") {
    return { ok: false, errors: ["Ruleset precisa ter 'version' (number) e 'rules' (array)."] };
  }
  return { ok: true, version: r.version, rules: r.rules.length };
}

function mockSimulate(): SimulateResult {
  return {
    ok: true,
    result: {
      overallStatus: "ATTENTION",
      score: 62,
      confidence: "HIGH",
      matchedRules: ["wifi_rssi_fraco"],
      findings: [{ findingCode: "wifi_sinal_fraco", category: "wifi", severity: "WARNING", confidence: "HIGH" }],
      humanSummary: "Sinal Wi-Fi fraco detectado — simulação em modo mock (sem backend real).",
      primaryFlow: "wifi_local",
    },
  };
}
