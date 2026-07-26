import { apiClient } from "./apiClient";
import {
  DiagnosticDivergenceRecord,
  DiagnosticDivergencesSummary,
  DivergenceClassification,
} from "../types/diagnosticDivergences";

// Refs #1446 (shadow mode) — mesma origem/proxy que diagnosticRulesetsService (ver
// nota lá): Console fala só com signallq-admin-worker, que repassa pro
// signallq-diagnostic-worker via service binding.
const BASE = "/admin/diagnostic";

export interface ListDivergencesParams {
  classification?: DivergenceClassification;
  limit?: number;
}

export interface ListDivergencesResult {
  items: DiagnosticDivergenceRecord[];
  summary: DiagnosticDivergencesSummary;
}

export const diagnosticDivergencesService = {
  async list(params: ListDivergencesParams = {}): Promise<ListDivergencesResult> {
    if (apiClient.isMockEnabled() || !import.meta.env.VITE_ADMIN_API_BASE_URL) {
      return mockList(params);
    }
    const query = new URLSearchParams();
    if (params.classification) query.set("classification", params.classification);
    query.set("limit", String(params.limit ?? 50));
    const raw = await apiClient.request<{ items: DiagnosticDivergenceRecord[]; summary: DiagnosticDivergencesSummary }>(
      "GET",
      `${BASE}/divergences?${query.toString()}`
    );
    return { items: raw.items ?? [], summary: raw.summary ?? {} };
  },
};

// ---- Mocks (dev local sem VITE_ADMIN_API_BASE_URL / VITE_ENABLE_MOCKS=true) ----

const MOCK_RECORDS: DiagnosticDivergenceRecord[] = [
  {
    id: "div-1",
    executionId: "exec-a1b2",
    snapshotHash: "sha256:aa11",
    classification: "EXACT_MATCH",
    localStatus: "OK",
    remoteStatus: "OK",
    localScore: 92,
    remoteScore: 92,
    localFlow: "saudavel_monitorar",
    remoteFlow: "saudavel_monitorar",
    localSource: "BUNDLED_LOCAL",
    remoteSource: "REMOTE",
    localDurationMs: 180,
    remoteDurationMs: 420,
    appVersion: "0.24.0",
    versionCode: 240,
    createdAt: "2026-07-25T14:00:00.000Z",
  },
  {
    id: "div-2",
    executionId: "exec-c3d4",
    snapshotHash: "sha256:bb22",
    classification: "MINOR_DIVERGENCE",
    localStatus: "ATTENTION",
    remoteStatus: "OK",
    localScore: 74,
    remoteScore: 81,
    localFlow: "wifi_local",
    remoteFlow: "saudavel_monitorar",
    localSource: "BUNDLED_LOCAL",
    remoteSource: "REMOTE",
    localDurationMs: 165,
    remoteDurationMs: 510,
    appVersion: "0.24.0",
    versionCode: 240,
    createdAt: "2026-07-25T15:30:00.000Z",
  },
  {
    id: "div-3",
    executionId: "exec-e5f6",
    snapshotHash: "sha256:cc33",
    classification: "MAJOR_DIVERGENCE",
    localStatus: "CRITICAL",
    remoteStatus: "OK",
    localScore: 38,
    remoteScore: 88,
    localFlow: "isp_externo",
    remoteFlow: "saudavel_monitorar",
    localSource: "BUNDLED_LOCAL",
    remoteSource: "REMOTE",
    localDurationMs: 190,
    remoteDurationMs: 460,
    appVersion: "0.23.5",
    versionCode: 235,
    createdAt: "2026-07-26T09:10:00.000Z",
  },
  {
    id: "div-4",
    executionId: "exec-g7h8",
    snapshotHash: "sha256:dd44",
    classification: "REMOTE_ERROR",
    localStatus: "OK",
    remoteStatus: null,
    localScore: 90,
    remoteScore: null,
    localFlow: "saudavel_monitorar",
    remoteFlow: null,
    localSource: "BUNDLED_LOCAL",
    remoteSource: null,
    localDurationMs: 170,
    remoteDurationMs: null,
    appVersion: "0.24.0",
    versionCode: 240,
    createdAt: "2026-07-26T10:45:00.000Z",
  },
];

function mockList(params: ListDivergencesParams): ListDivergencesResult {
  const filtered = params.classification
    ? MOCK_RECORDS.filter((r) => r.classification === params.classification)
    : MOCK_RECORDS;
  const limited = filtered.slice(0, params.limit ?? 50);
  const summary: DiagnosticDivergencesSummary = {};
  for (const r of MOCK_RECORDS) {
    summary[r.classification] = (summary[r.classification] ?? 0) + 1;
  }
  return { items: limited, summary };
}
