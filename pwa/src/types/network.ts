export type {
  AdminDiagnosticPayload,
  DiagnosisResult,
  DiagnosticPayload,
  LegacySpeedtestResult as SpeedtestResult,
} from '@shared/contracts';

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
  Partial = 'partial',
  Complete = 'complete',
  Error = 'error',
  Canceled = 'canceled',
}

export interface ConnectionSnapshot {
  status: ConnectionStatus;
  effectiveType: string | null;
  downlinkMbps: number | null;
  browserSupportsNetworkInfo: boolean;
}
