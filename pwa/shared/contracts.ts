export type MetricStatus = 'measured' | 'failed' | 'not_supported' | 'not_available' | 'insufficient_samples';

export type IngestKind = 'diagnostic' | 'ai-usage';

export interface ApiErrorResponse {
  error: string;
}

export interface LatencyMetric {
  ms: number | null;
  samples: number;
  status: Extract<MetricStatus, 'measured' | 'failed'>;
  method: 'http_timing';
}

export interface JitterMetric {
  ms: number | null;
  samples: number;
  status: Extract<MetricStatus, 'measured' | 'insufficient_samples' | 'failed'>;
}

export interface DownloadMetric {
  mbps: number | null;
  durationMs: number;
  bytes: number;
  samples: number;
  status: Extract<MetricStatus, 'measured' | 'failed' | 'not_supported'>;
}

export interface UploadMetric {
  mbps: number | null;
  durationMs: number;
  bytes: number;
  samples: number;
  status: Extract<MetricStatus, 'measured' | 'failed' | 'not_available'>;
}

export interface AvailabilityMetric {
  failedRequests: number;
  totalRequests: number;
  perceivedLossPercent: number | null;
  status: 'inferred' | 'not_measured';
}

export interface BrowserInfo {
  userAgent?: string;
  platform?: string;
  language?: string;
  viewport?: {
    width: number;
    height: number;
  };
}

export interface BrowserConnectionInfo {
  effectiveType?: string;
  downlink?: number;
  rtt?: number;
  saveData?: boolean;
  source: 'network_information_api' | 'unavailable';
}

export interface SpeedTestResult {
  id: string;
  measuredAt: string;
  download: DownloadMetric;
  upload: UploadMetric;
  latency: LatencyMetric;
  jitter: JitterMetric;
  availability: AvailabilityMetric;
  browser: BrowserInfo;
  connection: BrowserConnectionInfo;
  limitations: string[];
}

export interface HistoryEntry {
  id: string;
  createdAt: string;
  speedTest: SpeedTestResult;
  diagnosis: DiagnosisResult;
  appVersion?: string;
}

export interface HistoryRepository {
  save(entry: HistoryEntry): Promise<void>;
  list(): Promise<HistoryEntry[]>;
  getById(id: string): Promise<HistoryEntry | null>;
  remove(id: string): Promise<void>;
  clear(): Promise<void>;
}

export interface LegacySpeedtestResult {
  latencyMs: number | null;
  downloadMbps: number | null;
  uploadMbps: number | null;
  jitterMs?: number | null;
  measuredAt: string;
}

export interface DiagnosisInput {
  speedTest?: SpeedTestResult | LegacySpeedtestResult | null;
  userContext?: {
    declaredProblem?: 'slow' | 'unstable' | 'video' | 'gaming' | 'work_call' | 'unknown';
    usageIntent?: 'general' | 'streaming' | 'gaming' | 'video_call' | 'work' | 'unknown';
  };
}

export interface DiagnosticPayload {
  schemaVersion: 'pwa_foundation_v1';
  source: 'pwa';
  connectionType: string;
  metricasAtuais: {
    downloadMbps: number | null;
    uploadMbps: number | null;
    latenciaMs: number | null;
    jitterMs?: number | null;
    /** Nome alinhado ao payload do Android (`perdaPacotesPercentual`) — o Worker
     *  de IA e o prompt tratam este campo pelo mesmo nome nas duas plataformas. */
    perdaPacotesPercentual?: number | null;
  };
  speedTest?: SpeedTestResult;
  browserContext?: {
    connection: BrowserConnectionInfo;
    browser: BrowserInfo;
    limitations: string[];
    unavailableNativeSignals: string[];
  };
}

export type QualityClassification = 'good' | 'attention' | 'bad' | 'unknown';
export type SpeedClassification = 'fast' | 'ok' | 'slow' | 'unknown';
export type StabilityClassification = 'stable' | 'unstable' | 'unknown';

export interface RecommendedAction {
  priority: 1 | 2 | 3;
  title: string;
  description: string;
  category: 'router' | 'wifi' | 'device' | 'provider' | 'retry' | 'unknown';
}

export interface DiagnosisResult {
  id: string;
  generatedAt: string;
  source: 'local' | 'ai' | 'fallback';
  summary: string;
  quality: QualityClassification;
  speed: SpeedClassification;
  stability: StabilityClassification;
  actions: RecommendedAction[];
  limitations: Array<{ code: string; message: string }>;
  confidence: 'high' | 'medium' | 'low';
}

export interface HistoryEntry {
  id: string;
  createdAt: string;
  speedTest: SpeedTestResult;
  diagnosis: DiagnosisResult;
  appVersion?: string;
}

export interface HistoryRepository {
  save(entry: HistoryEntry): Promise<void>;
  list(): Promise<HistoryEntry[]>;
  getById(id: string): Promise<HistoryEntry | null>;
  remove(id: string): Promise<void>;
  clear(): Promise<void>;
}

export interface AdminIngestRequest {
  kind: IngestKind;
  payload: Record<string, unknown>;
}

/** Origem do dado no SignallQ Console (GH#442). Android nao envia ainda — default no worker. */
export type AdminIngestPlatform = 'android' | 'web';

export interface AdminDiagnosticPayload {
  id: string;
  created_at?: number;
  network_type?: string;
  status?: string;
  score?: number | null;
  download_mbps?: number | null;
  upload_mbps?: number | null;
  latency_ms?: number | null;
  jitter_ms?: number | null;
  packet_loss?: number | null;
  issues?: string[];
  environment?: string;
  dist_channel?: string;
  build_type?: string;
  version_code?: number;
  device_id?: string;
  ai_summary_report?: string;
  platform?: AdminIngestPlatform;
}

export interface AdminAiUsagePayload {
  id: string;
  session_id?: string;
  created_at?: number;
  model: string;
  prompt_tokens?: number;
  completion_tokens?: number;
  total_tokens?: number;
  cost_usd?: number;
  environment?: string;
  version_code?: number;
  platform?: AdminIngestPlatform;
}
