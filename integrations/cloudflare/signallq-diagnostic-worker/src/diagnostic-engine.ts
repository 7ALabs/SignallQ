import type {
  DiagnosticCondition,
  DiagnosticConditionGroup,
  DiagnosticFinding,
  DiagnosticFlowCode,
  DiagnosticResult,
  DiagnosticRule,
  DiagnosticRuleset,
  DiagnosticSnapshot,
  JsonValue,
} from "./contracts.ts";
import { calcularScore } from "./score-engine.ts";

function getNestedValue(input: Record<string, JsonValue | undefined>, path: string): JsonValue | undefined {
  const segments = path.split(".");
  let current: JsonValue | Record<string, JsonValue | undefined> | undefined = input;

  for (const segment of segments) {
    if (!current || Array.isArray(current) || typeof current !== "object") {
      return undefined;
    }
    current = (current as Record<string, JsonValue | undefined>)[segment];
  }

  return current as JsonValue | undefined;
}

function compareNumber(actual: JsonValue | undefined, expected: JsonValue | undefined, mode: "GT" | "GTE" | "LT" | "LTE"): boolean {
  if (typeof actual !== "number" || typeof expected !== "number") {
    return false;
  }

  switch (mode) {
    case "GT":
      return actual > expected;
    case "GTE":
      return actual >= expected;
    case "LT":
      return actual < expected;
    case "LTE":
      return actual <= expected;
  }
}

function evaluateCondition(snapshot: DiagnosticSnapshot, condition: DiagnosticCondition): boolean {
  const actual = getNestedValue(snapshot as unknown as Record<string, JsonValue | undefined>, condition.field);
  const expected = condition.value;

  switch (condition.operator) {
    case "EQ":
      return actual === expected;
    case "NEQ":
      return actual !== expected;
    case "GT":
    case "GTE":
    case "LT":
    case "LTE":
      return compareNumber(actual, expected, condition.operator);
    case "IN":
      return Array.isArray(expected) && expected.includes(actual as never);
    case "NOT_IN":
      return Array.isArray(expected) && !expected.includes(actual as never);
    case "EXISTS":
      return typeof actual !== "undefined" && actual !== null;
    case "NOT_EXISTS":
      return typeof actual === "undefined" || actual === null;
    case "BETWEEN":
      return Array.isArray(expected)
        && expected.length === 2
        && typeof actual === "number"
        && typeof expected[0] === "number"
        && typeof expected[1] === "number"
        && actual >= expected[0]
        && actual <= expected[1];
  }
}

function evaluateGroup(snapshot: DiagnosticSnapshot, group: DiagnosticConditionGroup): boolean {
  switch (group.operator) {
    case "ALL":
      return group.conditions.every((condition) => evaluateCondition(snapshot, condition));
    case "ANY":
      return group.conditions.some((condition) => evaluateCondition(snapshot, condition));
    case "NONE":
      return group.conditions.every((condition) => !evaluateCondition(snapshot, condition));
  }
}

function evaluateRule(snapshot: DiagnosticSnapshot, rule: DiagnosticRule): boolean {
  if (!rule.enabled || snapshot.schemaVersion < rule.minimumSchemaVersion) {
    return false;
  }

  if (rule.conditionGroup) {
    return evaluateGroup(snapshot, rule.conditionGroup);
  }

  if (rule.conditions) {
    return rule.conditions.every((condition) => evaluateCondition(snapshot, condition));
  }

  return false;
}

function statusFromFindings(findings: Array<{ severity: "INFO" | "WARNING" | "ERROR" }>): DiagnosticResult["overallStatus"] {
  if (findings.some((finding) => finding.severity === "ERROR")) {
    return "CRITICAL";
  }
  if (findings.some((finding) => finding.severity === "WARNING")) {
    return "ATTENTION";
  }
  if (findings.length === 0) {
    return "OK";
  }
  return "INCONCLUSIVE";
}

function confidenceFromFindings(findings: Array<{ confidence: "LOW" | "MEDIUM" | "HIGH" }>): DiagnosticResult["confidence"] {
  if (findings.some((finding) => finding.confidence === "HIGH")) {
    return "HIGH";
  }
  if (findings.some((finding) => finding.confidence === "MEDIUM")) {
    return "MEDIUM";
  }
  return findings.length > 0 ? "LOW" : "MEDIUM";
}

function calculateDegradation(avg30?: number, avg7?: number, higherIsBetter = true): number | null {
  if (typeof avg30 !== "number" || typeof avg7 !== "number" || avg30 <= 0) return null;
  return higherIsBetter ? ((avg30 - avg7) / avg30) * 100 : ((avg7 - avg30) / avg30) * 100;
}

function wifiWeak(snapshot: DiagnosticSnapshot): boolean {
  const rssi = snapshot.wifi?.rssiDbm;
  const band = snapshot.wifi?.band;
  if (typeof rssi !== "number") return false;
  if (band === "2_4_GHZ") return rssi <= -70;
  if (band === "5_GHZ" || band === "6_GHZ") return rssi <= -75;
  return rssi <= -75;
}

function overlapChannel(networkChannel: number, connectedChannel: number, band?: string): boolean {
  if (band === "2_4_GHZ") {
    return Math.abs(networkChannel - connectedChannel) <= 4;
  }
  return networkChannel === connectedChannel;
}

function evaluateDerivedFindings(snapshot: DiagnosticSnapshot, findings: DiagnosticFinding[]): DiagnosticFinding[] {
  const derived: DiagnosticFinding[] = [];
  const hasCriticalInternet = findings.some((finding) => finding.category === "internet" && finding.severity === "ERROR");
  const hasWarningInternet = findings.some((finding) => finding.category === "internet" && finding.severity === "WARNING");
  const hasFiberProblem = findings.some((finding) => finding.category === "fibra");
  const hasDnsProblem = findings.some((finding) => finding.category === "dns");
  const hasWifiChannelProblem = findings.some((finding) => finding.findingCode === "WIFI_CHANNEL_CONGESTED");

  const historical = snapshot.historical;
  if (historical) {
    const enoughHistory = (historical.testsCount7d ?? 0) >= 5 && (historical.testsCount30d ?? 0) >= 10;
    if (enoughHistory) {
      const degradations = [
        calculateDegradation(historical.avgDownload30d, historical.avgDownload7d, true),
        calculateDegradation(historical.avgUpload30d, historical.avgUpload7d, true),
        calculateDegradation(historical.avgPing30d, historical.avgPing7d, false),
        calculateDegradation(historical.avgDns30d, historical.avgDns7d, false),
      ].filter((value): value is number => typeof value === "number");
      const maxDegradation = degradations.length > 0 ? Math.max(...degradations) : 0;
      if (maxDegradation >= 40) {
        derived.push({
          findingCode: "HISTORY_DEGRADATION_DETECTED",
          category: "historico",
          severity: "ERROR",
          confidence: "MEDIUM",
          recommendationId: "COMPARE_7D_VS_30D_AND_CONTACT_PROVIDER",
          matchedRuleId: "derived_history_degradation_critical",
          matchedRuleVersion: 1,
        });
      } else if (maxDegradation >= 20) {
        derived.push({
          findingCode: "HISTORY_POSSIBLE_DEGRADATION",
          category: "historico",
          severity: "WARNING",
          confidence: "MEDIUM",
          recommendationId: "MONITOR_HISTORY_AND_RETEST",
          matchedRuleId: "derived_history_degradation_warning",
          matchedRuleVersion: 1,
        });
      }
    }
  }

  const scan = snapshot.wifiScan;
  if (scan?.connectedChannel && scan.networks && scan.networks.length >= 6) {
    const overlapped = scan.networks.filter((network) =>
      typeof network.rssiDbm === "number"
      && typeof (network.channel ?? network.frequencyMhz) !== "undefined"
      && overlapChannel(network.channel ?? 0, scan.connectedChannel ?? 0, snapshot.wifi?.band)
      && network.rssiDbm >= -70,
    ).length;
    if (overlapped >= 4 && !hasWifiChannelProblem) {
      derived.push({
        findingCode: "WIFI_CHANNEL_CONGESTED",
        category: "wifi-canal",
        severity: "WARNING",
        confidence: "MEDIUM",
        recommendationId: "CHANGE_WIFI_CHANNEL",
        matchedRuleId: "derived_wifi_channel_congested",
        matchedRuleVersion: 1,
      });
    }
  }

  const weakWifi = wifiWeak(snapshot);
  const gatewayRtt = snapshot.gateway?.rttMs;
  const latency = snapshot.quality?.latencyMs;

  if (typeof gatewayRtt === "number" && gatewayRtt < 10 && typeof latency === "number" && latency > 200 && !weakWifi) {
    derived.push({
      findingCode: "ISP_PROBLEM_DETECTED",
      category: "decisao",
      severity: "ERROR",
      confidence: "HIGH",
      recommendationId: "CONTACT_PROVIDER_WITH_EVIDENCE",
      matchedRuleId: "derived_decisao_gw_01",
      matchedRuleVersion: 1,
    });
  }

  if (typeof gatewayRtt === "number" && gatewayRtt > 50) {
    derived.push({
      findingCode: "ROUTER_SLOW_RESPONSE",
      category: "decisao",
      severity: "WARNING",
      confidence: "MEDIUM",
      recommendationId: "RESTART_ROUTER_AND_RETEST",
      matchedRuleId: "derived_decisao_gw_02",
      matchedRuleVersion: 1,
    });
  }

  if ((hasCriticalInternet || hasWarningInternet) && weakWifi) {
    derived.push({
      findingCode: "POSSIBLE_WIFI_INTERFERENCE",
      category: "decisao",
      severity: "WARNING",
      confidence: "MEDIUM",
      recommendationId: "MOVE_CLOSER_TO_ROUTER_AND_RETEST",
      matchedRuleId: "derived_decisao_01",
      matchedRuleVersion: 1,
    });
  } else if (hasCriticalInternet && !weakWifi && !hasFiberProblem && !hasDnsProblem) {
    derived.push({
      findingCode: "INTERNET_PROBLEM_DETECTED",
      category: "decisao",
      severity: "ERROR",
      confidence: "HIGH",
      recommendationId: "RESTART_ROUTER_OR_CONTACT_PROVIDER",
      matchedRuleId: "derived_decisao_02",
      matchedRuleVersion: 1,
    });
  } else if (hasWarningInternet && !weakWifi && !hasFiberProblem && !hasDnsProblem) {
    derived.push({
      findingCode: "INTERNET_QUALITY_ATTENTION",
      category: "decisao",
      severity: "WARNING",
      confidence: "MEDIUM",
      recommendationId: "MONITOR_CONNECTION_AND_RETEST",
      matchedRuleId: "derived_decisao_02b",
      matchedRuleVersion: 1,
    });
  }

  if (!hasCriticalInternet && !hasWarningInternet && weakWifi) {
    derived.push({
      findingCode: "WIFI_NEEDS_ATTENTION",
      category: "decisao",
      severity: "WARNING",
      confidence: "MEDIUM",
      recommendationId: "IMPROVE_WIFI_SIGNAL",
      matchedRuleId: "derived_decisao_04_wifi",
      matchedRuleVersion: 1,
    });
  }

  return derived;
}

interface DiagnosticFlowDecision {
  primaryFlow: DiagnosticFlowCode;
  secondaryFlows: DiagnosticFlowCode[];
  humanSummary: string;
  humanResolution: string[];
  missingInputs: string[];
  nextBestChecks: string[];
  resolvableNow: boolean;
}

function hasMeaningfulDiagnosticData(snapshot: DiagnosticSnapshot): boolean {
  return [
    snapshot.speed?.downloadMbps,
    snapshot.speed?.uploadMbps,
    snapshot.quality?.latencyMs,
    snapshot.quality?.jitterMs,
    snapshot.quality?.packetLossPercent,
    snapshot.quality?.loadedLatencyMs,
    snapshot.wifi?.rssiDbm,
    snapshot.gateway?.rttMs,
    snapshot.dns?.latencyMs,
    snapshot.fiber?.rxPowerDbm,
    snapshot.mobile?.rsrpDbm,
    snapshot.historical?.avgDownload7d,
    snapshot.historical?.avgDownload30d,
  ].some((value) => typeof value === "number");
}

function getMissingInputs(snapshot: DiagnosticSnapshot): string[] {
  const missing = new Set<string>();

  if (typeof snapshot.speed?.downloadMbps !== "number") missing.add("download_mbps");
  if (typeof snapshot.speed?.uploadMbps !== "number") missing.add("upload_mbps");
  if (typeof snapshot.quality?.latencyMs !== "number") missing.add("latency_ms");
  if (typeof snapshot.quality?.jitterMs !== "number") missing.add("jitter_ms");
  if (typeof snapshot.quality?.packetLossPercent !== "number") missing.add("packet_loss_percent");
  if (typeof snapshot.quality?.loadedLatencyMs !== "number") missing.add("loaded_latency_ms");
  if (typeof snapshot.gateway?.rttMs !== "number") missing.add("gateway_rtt_ms");
  if (typeof snapshot.dns?.latencyMs !== "number") missing.add("dns_latency_ms");
  if (snapshot.connection?.type === "WIFI" || snapshot.wifi || snapshot.wifiScan) {
    if (typeof snapshot.wifi?.rssiDbm !== "number") missing.add("wifi_rssi_dbm");
    if (!snapshot.wifi?.band) missing.add("wifi_band");
  }
  if (snapshot.fiber !== null && snapshot.fiber !== undefined && typeof snapshot.fiber?.rxPowerDbm !== "number") {
    missing.add("fiber_rx_power_dbm");
  }
  if (snapshot.mobile !== null && snapshot.mobile !== undefined) {
    if (typeof snapshot.mobile?.rsrpDbm !== "number") missing.add("mobile_rsrp_dbm");
    if (typeof snapshot.mobile?.sinrDb !== "number") missing.add("mobile_sinr_db");
  }
  if (snapshot.historical) {
    if (typeof snapshot.historical.testsCount7d !== "number") missing.add("history_tests_count_7d");
    if (typeof snapshot.historical.testsCount30d !== "number") missing.add("history_tests_count_30d");
    if (typeof snapshot.historical.avgDownload7d !== "number") missing.add("history_avg_download_7d");
    if (typeof snapshot.historical.avgDownload30d !== "number") missing.add("history_avg_download_30d");
  }

  return [...missing];
}

function hasFinding(findings: DiagnosticFinding[], predicate: (finding: DiagnosticFinding) => boolean): boolean {
  return findings.some(predicate);
}

function buildFlowDecision(snapshot: DiagnosticSnapshot, findings: DiagnosticFinding[]): DiagnosticFlowDecision {
  const missingInputs = getMissingInputs(snapshot);
  const hasPartialData = missingInputs.length > 0;
  const enoughData = hasMeaningfulDiagnosticData(snapshot);
  const weakWifiDetected = hasFinding(findings, (finding) =>
    finding.category === "wifi"
    || finding.category === "wifi-canal"
    || finding.findingCode === "POSSIBLE_WIFI_INTERFERENCE"
    || finding.findingCode === "WIFI_NEEDS_ATTENTION",
  );
  const fiberDetected = hasFinding(findings, (finding) => finding.category === "fibra");
  const dnsDetected = hasFinding(findings, (finding) => finding.category === "dns");
  const mobileDetected = hasFinding(findings, (finding) => finding.category === "mobile");
  const historyDetected = hasFinding(findings, (finding) => finding.category === "historico");
  const ispDetected = hasFinding(findings, (finding) =>
    finding.findingCode === "ISP_PROBLEM_DETECTED" || finding.findingCode === "INTERNET_PROBLEM_DETECTED",
  );
  const internetDetected = hasFinding(findings, (finding) =>
    finding.category === "internet" || finding.findingCode === "INTERNET_QUALITY_ATTENTION",
  );

  const secondaryFlows: DiagnosticFlowCode[] = [];
  const pushSecondary = (flow: DiagnosticFlowCode): void => {
    if (!secondaryFlows.includes(flow)) {
      secondaryFlows.push(flow);
    }
  };

  if (weakWifiDetected) pushSecondary("wifi_local");
  if (ispDetected) pushSecondary("isp_externo");
  if (dnsDetected) pushSecondary("dns");
  if (fiberDetected) pushSecondary("fibra");
  if (mobileDetected) pushSecondary("rede_movel");
  if (historyDetected) pushSecondary("historico_degradacao");
  if (internetDetected) pushSecondary("internet_instavel");

  if (!enoughData) {
    return {
      primaryFlow: "sem_dados_suficientes",
      secondaryFlows: [],
      humanSummary: "Ainda nao temos dados suficientes pra apontar a causa com seguranca, mas ja podemos orientar sua proxima coleta.",
      humanResolution: [
        "Rode uma medicao completa com download, upload, latencia, jitter e perda.",
        "Se estiver no Wi-Fi, capture tambem sinal RSSI e teste perto do roteador.",
        "Se possivel, colete o tempo de resposta do gateway para separar problema local de ISP.",
      ],
      missingInputs,
      nextBestChecks: [
        "coletar download/upload/latencia/jitter/perda",
        "coletar gateway_rtt_ms",
        "coletar wifi_rssi_dbm se a conexao for Wi-Fi",
      ],
      resolvableNow: false,
    };
  }

  if (fiberDetected) {
    return {
      primaryFlow: "fibra",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "fibra"),
      humanSummary: hasPartialData
        ? "Os sinais da sua conexao apontam degradacao na fibra ou ONU, mesmo com parte do contexto ainda ausente."
        : "Os sinais da sua conexao apontam degradacao na fibra ou ONU.",
      humanResolution: [
        "Verifique conectores, curva da fibra e energia da ONU/ONT.",
        "Reinicie a ONU se isso fizer parte do procedimento suportado pelo provedor.",
        "Se a potencia optica seguir ruim, acione o ISP com a leitura de RX/TX como evidencia.",
      ],
      missingInputs,
      nextBestChecks: [
        "confirmar fiber_rx_power_dbm e, se existir, tx_power_dbm",
        "comparar com latencia e perda para medir impacto real",
      ],
      resolvableNow: true,
    };
  }

  if (dnsDetected) {
    return {
      primaryFlow: "dns",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "dns"),
      humanSummary: hasPartialData
        ? "O gargalo principal da sua conexao parece estar na resolucao DNS, e ja ha indicio suficiente pra agir."
        : "O gargalo principal da sua conexao parece estar na resolucao DNS.",
      humanResolution: [
        "Compare o comportamento com outro resolvedor DNS confiavel.",
        "Reinicie o roteador para renovar a sessao DNS local.",
        "Se o problema persistir, mantenha a evidencia de latencia DNS para contato com o provedor.",
      ],
      missingInputs,
      nextBestChecks: [
        "comparar dns_latency_ms com gateway_rtt_ms",
        "retestar navegacao e abertura de apps apos trocar o DNS",
      ],
      resolvableNow: true,
    };
  }

  if (ispDetected) {
    return {
      primaryFlow: "isp_externo",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "isp_externo"),
      humanSummary: hasPartialData
        ? "Mesmo sem todos os sinais, o padrao atual indica que sua rede local responde bem e o problema esta mais para fora de casa, no caminho do provedor."
        : "Sua rede local responde bem e o problema parece estar no caminho do provedor.",
      humanResolution: [
        "Guarde a evidencia de gateway baixo com internet alta ou instavel.",
        "Reteste por cabo ou perto do roteador so para descartar variacao local.",
        "Se repetir, abra chamado com o provedor informando horario, latencia e perda observados.",
      ],
      missingInputs,
      nextBestChecks: [
        "validar packet_loss_percent e loaded_latency_ms em novo teste",
        "comparar o comportamento em outro horario para confirmar recorrencia",
      ],
      resolvableNow: true,
    };
  }

  if (weakWifiDetected) {
    return {
      primaryFlow: "wifi_local",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "wifi_local"),
      humanSummary: hasPartialData
        ? "O sinal ja e suficiente pra indicar um problema no seu Wi-Fi local, mesmo sem o conjunto completo de medidas."
        : "O problema parece concentrado no seu Wi-Fi local.",
      humanResolution: [
        "Aproxime o dispositivo do roteador e refaca o teste.",
        "Se houver 5 GHz disponivel, troque da rede 2.4 GHz para 5 GHz.",
        "Se o canal estiver congestionado, altere o canal do Wi-Fi e reduza interferencia ao redor.",
      ],
      missingInputs,
      nextBestChecks: [
        "coletar wifi_rssi_dbm e wifi_link_speed_mbps em novo teste",
        "comparar perto e longe do roteador",
        "validar gateway_rtt_ms para confirmar se o problema para no Wi-Fi",
      ],
      resolvableNow: true,
    };
  }

  if (mobileDetected) {
    return {
      primaryFlow: "rede_movel",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "rede_movel"),
      humanSummary: hasPartialData
        ? "A qualidade da sua rede movel ja mostra um gargalo de cobertura ou radio, mesmo com parte dos dados faltando."
        : "A qualidade da sua rede movel aponta gargalo de cobertura ou radio.",
      humanResolution: [
        "Teste em outro ponto da casa ou ao ar livre para comparar o sinal.",
        "Alterne entre 4G e 5G se o aparelho permitir.",
        "Se o sinal continuar ruim, registre operadora, tecnologia e horario para suporte.",
      ],
      missingInputs,
      nextBestChecks: [
        "coletar mobile_rsrp_dbm e mobile_sinr_db completos",
        "comparar o resultado em outro local fisico",
      ],
      resolvableNow: true,
    };
  }

  if (historyDetected) {
    return {
      primaryFlow: "historico_degradacao",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "historico_degradacao"),
      humanSummary: hasPartialData
        ? "Ha sinal de degradacao recente no seu historico, mesmo que o retrato atual ainda esteja incompleto."
        : "Ha sinal de degradacao recente quando comparamos seu historico curto com o baseline maior.",
      humanResolution: [
        "Compare horarios ruins e bons para encontrar padrao de recorrencia.",
        "Repita o teste no pior horario identificado para reforcar a evidencia.",
        "Se a degradacao for recorrente, leve o historico consolidado para o provedor.",
      ],
      missingInputs,
      nextBestChecks: [
        "confirmar media de download, upload, ping e DNS em 7d e 30d",
        "identificar worstTimeWindow para correlacionar com congestionamento",
      ],
      resolvableNow: true,
    };
  }

  if (internetDetected) {
    return {
      primaryFlow: "internet_instavel",
      secondaryFlows: secondaryFlows.filter((flow) => flow !== "internet_instavel"),
      humanSummary: hasPartialData
        ? "Sua internet apresenta instabilidade, mas ainda faltam alguns sinais pra cravar se a origem e local ou externa."
        : "Sua internet apresenta instabilidade, mas sem um indicador dominante unico.",
      humanResolution: [
        "Refaca o teste em outro horario para ver se a instabilidade se repete.",
        "Compare o comportamento por cabo e no Wi-Fi.",
        "Se houver perda, jitter ou latencia alta de forma recorrente, registre a evidencia antes de acionar o provedor.",
      ],
      missingInputs,
      nextBestChecks: [
        "coletar gateway_rtt_ms para separar local vs ISP",
        "coletar wifi_rssi_dbm se estiver em Wi-Fi",
      ],
      resolvableNow: true,
    };
  }

  return {
    primaryFlow: "saudavel_monitorar",
    secondaryFlows: [],
    humanSummary: hasPartialData
      ? "Nao apareceu um problema dominante e os sinais da sua conexao parecem saudaveis, embora ainda faltem alguns dados auxiliares."
      : "Nao apareceu um problema dominante e os sinais da sua conexao parecem saudaveis.",
    humanResolution: [
      "Mantenha monitoramento leve e repita o teste se notar lentidao real.",
      "Se surgir problema em horario especifico, salve uma nova medicao para comparacao.",
    ],
    missingInputs,
    nextBestChecks: hasPartialData
      ? ["completar as metricas faltantes na proxima coleta para aumentar a confianca"]
      : ["seguir monitorando historico e repetir em caso de nova degradacao"],
    resolvableNow: true,
  };
}

export function validateRuleset(input: unknown): { ok: true; ruleset: DiagnosticRuleset } | { ok: false; errors: string[] } {
  const errors: string[] = [];

  if (!input || typeof input !== "object") {
    return { ok: false, errors: ["Ruleset must be an object."] };
  }

  const ruleset = input as Partial<DiagnosticRuleset>;
  if (typeof ruleset.version !== "number") errors.push("version must be a number.");
  if (typeof ruleset.schemaVersion !== "number") errors.push("schemaVersion must be a number.");
  if (typeof ruleset.engineVersion !== "number") errors.push("engineVersion must be a number.");
  if (typeof ruleset.publishedAt !== "string") errors.push("publishedAt must be a string.");
  if (!Array.isArray(ruleset.rules)) {
    errors.push("rules must be an array.");
  } else {
    for (const rule of ruleset.rules) {
      if (!rule || typeof rule !== "object") {
        errors.push("Each rule must be an object.");
        continue;
      }
      const typedRule = rule as Partial<DiagnosticRule>;
      if (!typedRule.ruleId) errors.push("ruleId is required.");
      if (typeof typedRule.ruleVersion !== "number") errors.push(`ruleVersion missing for ${typedRule.ruleId ?? "unknown rule"}.`);
      if (typeof typedRule.minimumSchemaVersion !== "number") errors.push(`minimumSchemaVersion missing for ${typedRule.ruleId ?? "unknown rule"}.`);
      if (!typedRule.result?.findingCode) errors.push(`result.findingCode missing for ${typedRule.ruleId ?? "unknown rule"}.`);
      if (!typedRule.result?.category) errors.push(`result.category missing for ${typedRule.ruleId ?? "unknown rule"}.`);
      const hasConditions = Array.isArray(typedRule.conditions) && typedRule.conditions.length > 0;
      const hasGroup = Boolean(typedRule.conditionGroup?.conditions?.length);
      if (!hasConditions && !hasGroup) errors.push(`Rule ${typedRule.ruleId ?? "unknown rule"} must define conditions or conditionGroup.`);
    }
  }

  if (errors.length > 0) {
    return { ok: false, errors };
  }

  return { ok: true, ruleset: ruleset as DiagnosticRuleset };
}

export function validateSnapshot(input: unknown): { ok: true; snapshot: DiagnosticSnapshot } | { ok: false; errors: string[] } {
  const errors: string[] = [];
  if (!input || typeof input !== "object") {
    return { ok: false, errors: ["Snapshot must be an object."] };
  }
  const snapshot = input as Partial<DiagnosticSnapshot>;
  if (typeof snapshot.schemaVersion !== "number") {
    errors.push("schemaVersion must be a number.");
  }
  if (snapshot.speed && typeof snapshot.speed !== "object") {
    errors.push("speed must be an object when present.");
  }
  if (snapshot.quality && typeof snapshot.quality !== "object") {
    errors.push("quality must be an object when present.");
  }
  if (errors.length > 0) {
    return { ok: false, errors };
  }
  return { ok: true, snapshot: snapshot as DiagnosticSnapshot };
}

export function evaluateSnapshot(
  snapshot: DiagnosticSnapshot,
  ruleset: DiagnosticRuleset,
  source: DiagnosticResult["evaluationSource"] = "REMOTE",
): DiagnosticResult {
  const orderedRules = [...ruleset.rules].sort((left, right) => right.priority - left.priority);
  const findings = orderedRules
    .filter((rule) => evaluateRule(snapshot, rule))
    .map((rule) => ({
      findingCode: rule.result.findingCode,
      category: rule.result.category,
      severity: rule.result.severity,
      confidence: rule.result.confidence,
      recommendationId: rule.result.recommendationId,
      matchedRuleId: rule.ruleId,
      matchedRuleVersion: rule.ruleVersion,
    }));

  findings.push(...evaluateDerivedFindings(snapshot, findings));
  const flowDecision = buildFlowDecision(snapshot, findings);
  const scoreResult = calcularScore(snapshot, findings);

  return {
    resultSchemaVersion: ruleset.schemaVersion,
    engineVersion: ruleset.engineVersion,
    rulesetVersion: ruleset.version,
    evaluationSource: source,
    overallStatus: statusFromFindings(findings),
    score: scoreResult.score,
    confidence: confidenceFromFindings(findings),
    matchedRules: findings.map((finding) => finding.matchedRuleId),
    findings,
    recommendations: findings.map((finding) => finding.recommendationId),
    primaryFlow: flowDecision.primaryFlow,
    secondaryFlows: flowDecision.secondaryFlows,
    humanSummary: flowDecision.humanSummary,
    humanResolution: flowDecision.humanResolution,
    missingInputs: flowDecision.missingInputs,
    nextBestChecks: flowDecision.nextBestChecks,
    resolvableNow: flowDecision.resolvableNow,
    evaluatedAt: new Date().toISOString(),
    traceId: crypto.randomUUID(),
  };
}
