export enum ConnectionStatus {
  Online = 'online',
  Offline = 'offline',
  Unknown = 'unknown',
}

export enum SpeedtestPhase {
  Idle = 'idle',
  Latency = 'latency',
  Download = 'download',
  Upload = 'upload',
  Complete = 'complete',
  Error = 'error',
}

export interface ConnectionSnapshot {
  status: ConnectionStatus;
  effectiveType: string | null;
  downlinkMbps: number | null;
  browserSupportsNetworkInfo: boolean;
}

export interface SpeedtestResult {
  latencyMs: number | null;
  downloadMbps: number | null;
  uploadMbps: number | null;
  measuredAt: string;
}

export interface DiagnosticPayload {
  schemaVersion: 'pwa_foundation_v1';
  source: 'pwa';
  connectionType: string;
  metricasAtuais: {
    downloadMbps: number | null;
    uploadMbps: number | null;
    latenciaMs: number | null;
  };
}
