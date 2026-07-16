export type Primitive = string | number | boolean | null;

export type JsonValue =
  | Primitive
  | { [key: string]: JsonValue }
  | JsonValue[];

export type DiagnosticFieldOperator =
  | "EQ"
  | "NEQ"
  | "GT"
  | "GTE"
  | "LT"
  | "LTE"
  | "IN"
  | "NOT_IN"
  | "EXISTS"
  | "NOT_EXISTS"
  | "BETWEEN";

export type DiagnosticGroupOperator = "ALL" | "ANY" | "NONE";

export interface DiagnosticCondition {
  field: string;
  operator: DiagnosticFieldOperator;
  value?: JsonValue;
}

export interface DiagnosticConditionGroup {
  operator: DiagnosticGroupOperator;
  conditions: DiagnosticCondition[];
}

export interface DiagnosticRuleResult {
  findingCode: string;
  category: "internet" | "wifi" | "dns" | "fibra" | "mobile" | "historico" | "wifi-canal" | "decisao";
  severity: "INFO" | "WARNING" | "ERROR";
  confidence: "LOW" | "MEDIUM" | "HIGH";
  recommendationId: string;
}

export interface DiagnosticRule {
  ruleId: string;
  ruleVersion: number;
  enabled: boolean;
  priority: number;
  minimumSchemaVersion: number;
  conditionGroup?: DiagnosticConditionGroup;
  conditions?: DiagnosticCondition[];
  result: DiagnosticRuleResult;
}

export interface DiagnosticRuleset {
  version: number;
  schemaVersion: number;
  engineVersion: number;
  publishedAt: string;
  rules: DiagnosticRule[];
}

export interface DiagnosticSnapshot {
  schemaVersion: number;
  sessionId?: string;
  appVersion?: string;
  platform?: string;
  connection?: {
    type?: string;
    hasInternet?: boolean;
    ipv6Available?: boolean;
  };
  wifi?: {
    band?: string;
    rssiDbm?: number;
    frequencyMhz?: number;
    linkSpeedMbps?: number;
    has5GhzAvailable?: boolean;
    devicesOnNetwork?: number;
  };
  wifiScan?: {
    connectedChannel?: number;
    networks?: Array<{
      channel?: number;
      frequencyMhz?: number;
      rssiDbm?: number;
      ssid?: string;
    }>;
  };
  speed?: {
    downloadMbps?: number;
    uploadMbps?: number;
  };
  quality?: {
    latencyMs?: number;
    jitterMs?: number;
    packetLossPercent?: number;
    loadedLatencyMs?: number;
  };
  dns?: {
    latencyMs?: number;
    currentProvider?: string;
    [key: string]: JsonValue | undefined;
  };
  fiber?: {
    rxPowerDbm?: number;
    txPowerDbm?: number;
    temperatureCelsius?: number;
    [key: string]: JsonValue | undefined;
  } | null;
  mobile?: {
    technology?: string;
    rsrpDbm?: number;
    rsrqDb?: number;
    sinrDb?: number;
    operatorName?: string;
    [key: string]: JsonValue | undefined;
  } | null;
  historical?: {
    testsCount7d?: number;
    testsCount30d?: number;
    avgDownload7d?: number;
    avgDownload30d?: number;
    avgUpload7d?: number;
    avgUpload30d?: number;
    avgPing7d?: number;
    avgPing30d?: number;
    avgDns7d?: number;
    avgDns30d?: number;
    worstTimeWindow?: string;
    bestTimeWindow?: string;
  };
  gateway?: {
    rttMs?: number;
  };
  localEquipment?: Record<string, JsonValue> | null;
}

export interface DiagnosticFinding {
  findingCode: string;
  category: "internet" | "wifi" | "dns" | "fibra" | "mobile" | "historico" | "wifi-canal" | "decisao";
  severity: "INFO" | "WARNING" | "ERROR";
  confidence: "LOW" | "MEDIUM" | "HIGH";
  recommendationId: string;
  matchedRuleId: string;
  matchedRuleVersion: number;
}

export type DiagnosticFlowCode =
  | "isp_externo"
  | "wifi_local"
  | "dns"
  | "fibra"
  | "rede_movel"
  | "historico_degradacao"
  | "internet_instavel"
  | "sem_dados_suficientes"
  | "saudavel_monitorar";

export interface DiagnosticResult {
  resultSchemaVersion: number;
  engineVersion: number;
  rulesetVersion: number;
  evaluationSource: "REMOTE" | "CACHED_LOCAL" | "BUNDLED_LOCAL";
  overallStatus: "OK" | "ATTENTION" | "CRITICAL" | "INCONCLUSIVE";
  score: number;
  confidence: "LOW" | "MEDIUM" | "HIGH";
  matchedRules: string[];
  findings: DiagnosticFinding[];
  recommendations: string[];
  primaryFlow: DiagnosticFlowCode;
  secondaryFlows: DiagnosticFlowCode[];
  humanSummary: string;
  humanResolution: string[];
  missingInputs: string[];
  nextBestChecks: string[];
  resolvableNow: boolean;
  evaluatedAt: string;
  traceId: string;
}

export interface ProviderLogo {
  url: string;
  version: number;
  updatedAt: string;
}

export interface ProviderSupport {
  sacPhone: string | null;
  technicalSupportPhone: string | null;
  whatsappUrl: string | null;
  websiteUrl: string | null;
  customerAreaUrl: string | null;
  ombudsmanPhone: string | null;
}

export interface ProviderRecord {
  id: string;
  displayName: string;
  legalName?: string | null;
  cnpj?: string | null;
  officialDomain?: string | null;
  providerType: string;
  status: string;
  logo: ProviderLogo | null;
  support: ProviderSupport;
  aliases: string[];
  asns: number[];
  verifiedAt: string | null;
  cacheVersion: number;
  cacheExpiresAt: string;
}

export interface ProviderDetectionInput {
  providerId?: string | null;
  asn?: number | null;
  rawNameSample?: string | null;
  normalizedName?: string | null;
  installationHash?: string | null;
  appVersion?: string | null;
  platform?: string | null;
  detectedAt?: string | null;
}

export interface GameProfileRecord {
  profileCode: string;
  displayName: string;
  latencyGoodMax: number | null;
  latencyAttentionMax: number | null;
  jitterGoodMax: number | null;
  jitterAttentionMax: number | null;
  lossGoodMax: number | null;
  lossAttentionMax: number | null;
  downloadGoodMin: number | null;
  downloadAttentionMin: number | null;
  bufferbloatGoodMax: number | null;
  bufferbloatAttentionMax: number | null;
  wifiPolicy: string | null;
  updatedAt: string;
}

export interface GameCatalogRecord {
  gameId: string;
  displayName: string;
  slug: string;
  active: boolean;
  profileCode: string;
  testStrategy: string;
  regionCode: string;
  resultLabel: string;
  providerNetworkMode: string;
  sortOrder: number;
  iconKey: string | null;
  platforms: string[];
  createdAt: string;
  updatedAt: string;
}

export interface GameCatalogVersion {
  version: string;
  totalGames: number;
  generatedAt: string;
}

export interface GameCatalogAdminInput {
  gameId: string;
  displayName: string;
  slug: string;
  active?: boolean;
  profileCode: string;
  testStrategy: string;
  regionCode: string;
  resultLabel: string;
  providerNetworkMode?: string;
  sortOrder?: number;
  iconKey?: string | null;
  platforms: string[];
}

export interface GameProfileAdminInput {
  profileCode: string;
  displayName: string;
  latencyGoodMax?: number | null;
  latencyAttentionMax?: number | null;
  jitterGoodMax?: number | null;
  jitterAttentionMax?: number | null;
  lossGoodMax?: number | null;
  lossAttentionMax?: number | null;
  downloadGoodMin?: number | null;
  downloadAttentionMin?: number | null;
  bufferbloatGoodMax?: number | null;
  bufferbloatAttentionMax?: number | null;
  wifiPolicy?: string | null;
}

export interface GameAuditEntry {
  id: string;
  entityType: string;
  entityId: string;
  action: string;
  actor: string;
  beforeJson: string | null;
  afterJson: string | null;
  createdAt: string;
}
