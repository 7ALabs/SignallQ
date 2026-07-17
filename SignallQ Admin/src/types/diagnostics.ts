import { NetworkType, NetworkSpeed, NetworkStrength } from "./network";

export type DistChannel = "play_store" | "firebase_app_distribution" | "sideload";
export type BuildType = "release" | "debug";
// GH#442: origem do dado. Android ainda nao envia este campo (default no worker
// e' 'android' para preservar dado historico) — 'web' e' dado historico do
// extinto PWA, mantido no tipo para nao quebrar leitura de registros antigos.
export type DataPlatform = "android" | "web";

// migration 012_play_track.sql: trilha do Play Console mapeada via Android Publisher
// API (internal/alpha/beta/production sao as 4 trilhas padrao; o Play Console tambem
// permite trilhas customizadas de teste fechado/aberto, por isso o campo no dado real
// e' `string`, nao restrito a este union — este type cobre só as opções de filtro/UI.
export type PlayTrack = "internal" | "alpha" | "beta" | "production";

export const PLAY_TRACK_LABELS: Record<PlayTrack, string> = {
  internal: "Interno",
  alpha: "Fechado (Alfa)",
  beta: "Aberto (Beta)",
  production: "Produção",
};

export function playTrackLabel(track?: string | null): string {
  if (!track) return "Não mapeado";
  return PLAY_TRACK_LABELS[track as PlayTrack] ?? track;
}

// GH#881 — vocabulario canonico de issue de diagnostico, decidido em 2026-07-16
// (docs_ai/decisions/ADR-009-vocabulario-diagnostic-issue.md). Espelha 1:1 o que o
// Android realmente envia hoje (`idParaIssueLabel` em AdminIngestPayloads.kt): 12 categorias
// snake_case + "none" (sem issue detectado) + "unknown" (fallback para tag nao reconhecida,
// incluindo dado legado pre-normalizacao como "Resposta").
export type DiagnosisIssue =
  | "sinal_fraco"
  | "alta_latencia"
  | "falha_dns"
  | "jitter_alto"
  | "perda_de_pacotes"
  | "upload_lento"
  | "download_lento"
  | "problema_fibra"
  | "gateway_inacessivel"
  | "bufferbloat"
  | "interferencia_canal_wifi"
  | "problema_banda"
  | "none"
  | "unknown";

export type Severity = "ok" | "attention" | "critical";

export interface DiagnosticSession {
  id: string;
  deviceId: string;
  deviceModel: string;
  osVersion: string;
  appVersion: string;
  timestamp: string; // ISO string
  networkType: NetworkType;
  environment: "production" | "staging";
  operator?: string; // operadora móvel/ISP identificada (coluna `operator` em diagnostic_sessions)
  score: number; // score 0-100 calculado pelo engine local (coluna `score`)
  speed: NetworkSpeed;
  networkStrength?: NetworkStrength;
  issues: {
    issue: DiagnosisIssue;
    severity: Severity;
    description: string;
  }[];
  aiStatus: "pending" | "completed" | "failed" | "none";
  aiSummaryReport?: string;
  distChannel?: DistChannel;
  buildType?: BuildType;
  platform?: DataPlatform;
  // migration 012_play_track.sql — trilha do Play Console, preenchida via backfill.
  // null/undefined = ainda não mapeada (nunca assumir "production" por padrão).
  playTrack?: string | null;
}

export interface DiagnosticsSummary {
  totalTests: number;
  criticalIssuesCount: number;
  attentionIssuesCount: number;
  averageDownloadMbps: number | null;
  averageUploadMbps: number | null;
  averageLatencyMs: number | null;
  averageScore: number;
  averageJitterMs: number | null;
  averagePacketLossPercentage: number | null;
  issueDistribution: Record<DiagnosisIssue, number>;
}

export interface AggregateRow {
  networkType: string;
  diagnosticsCount: number;
  avgScore: number;
  avgDownload: string;
  avgUpload: string;
  avgPing: string;
  avgJitter: string;
  avgLoss: string;
  topIssue: string;
  trend: "up" | "down" | "stable";
  trendLabel: string;
}
