import { NetworkType, NetworkSpeed, NetworkStrength } from "./network";

export type DistChannel = "play_store" | "firebase_app_distribution" | "sideload";
export type BuildType = "release" | "debug";

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
