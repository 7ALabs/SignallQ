import { FeatureKey } from "./productAnalytics";

export type AdReadinessStatus =
  | "disabled"
  | "planned"
  | "eligible"
  | "blocked_by_privacy"
  | "needs_consent";

export type DiagnosisIssue =
  | "wifi_signal_weak"
  | "dns_latency_high"
  | "bufferbloat_upload"
  | "mobile_congestion_suspected"
  | "latency_spike"
  | "packet_loss_detected";

export interface ContextualAdOpportunity {
  issue: DiagnosisIssue;
  label: string;
  eligibleDiagnostics: number;
  estimatedImpressions: number;
  recommendedCategories: string[];
  sensitivity: "low" | "medium" | "high";
  requiresConsent: boolean;
  status: AdReadinessStatus;
}

export interface MonetizationSettings {
  adsEnabled: boolean;
  contextualAdsEnabled: boolean;
  personalizedAdsEnabled: boolean;
  requireConsent: boolean;
  provider: "none" | "admob" | "custom";
  blockSensitiveTargeting: boolean;
  connectivityAdsOnly: boolean;
}
