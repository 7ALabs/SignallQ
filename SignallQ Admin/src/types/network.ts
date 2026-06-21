export type NetworkType = "wifi" | "mobile" | "fiber" | "ethernet" | "unknown";

export interface NetworkStrength {
  type: NetworkType;
  signalStrengthDbm: number; // e.g. -65
  signalQualityPercentage: number; // 0 to 100
  carrierName?: string; // e.g. "Claro", "Vivo", "TIM"
  frequencyBandGhz?: number; // e.g. 2.4, 5.0, or LTE band
  channel?: number;
  ssid?: string;
}

export interface NetworkSpeed {
  downloadMbps: number;
  uploadMbps: number;
  latencyMs: number;
  jitterMs: number;
  packetLossPercentage: number;
  bufferbloatGrade: string; // e.g. "A+", "B", "C", "F"
}
