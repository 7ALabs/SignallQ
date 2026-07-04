import { NetworkType, NetworkSpeed, NetworkStrength } from "./network";

export type DistChannel = "play_store" | "firebase_app_distribution" | "sideload";
export type BuildType = "release" | "debug";
// GH#442: origem do dado. Android ainda nao envia este campo (default no worker
// e' 'android' para preservar dado historico) — so o PWA envia 'web' hoje.
export type DataPlatform = "android" | "web";

export type DiagnosisIssue =
  | "wifi_signal_weak"
  | "bufferbloat_upload"
  | "dns_latency_high"
  | "mobile_congestion_suspected"
  | "gateway_slow"
  | "packet_loss"
  | "upload_bottleneck"
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
