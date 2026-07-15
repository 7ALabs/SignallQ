// GH#955 — porta do ScoreEngine.kt / MetricClassifier.kt (Android,
// :featureDiagnostico) para o worker. Antes o score era uma formula linear
// generica (100 - 35*ERROR - 18*WARNING - 5*INFO) que nao pesava por tipo de
// conexao nem aplicava teto por metrica critica isolada — divergia do motor
// real. Este modulo replica a mesma logica de pesos + reponderacao + tetos.
//
// Faixas de MetricClassifier.kt reproduzidas aqui (fonte de verdade:
// android/feature/diagnostico/.../MetricClassifier.kt e ScoreEngine.kt):
//   - latencia, jitter, perda de pacotes, RSSI Wi-Fi (por banda), RSRP/RSRQ/SINR
//     movel (por tecnologia), latencia DNS, bufferbloat (delta loaded-idle).
// "fibra" (saude optica RX) e "velocidade" (download/upload) nao tem tabela
// dedicada nos 4 arquivos de referencia (ClassificadorSaudeGpon.kt e o
// classificador de velocidade ficam fora do escopo desta issue) — as faixas
// abaixo sao uma extrapolacao documentada a partir dos thresholds ja usados
// no bundled-ruleset.ts (FIBER_RX_POWER_LOW=-27dBm, DOWNLOAD_LOW=25Mbps).

import type { DiagnosticFinding, DiagnosticSnapshot } from "./contracts.ts";

export type MetricStatus = "excelente" | "bom" | "regular" | "ruim" | "critico" | "inconclusivo";

export type TipoConexao = "WIFI" | "FIBRA" | "MOVEL" | "DESCONHECIDO";

interface PesoDimensao {
  nome: string;
  pesoBase: number;
}

const PESOS_WIFI: PesoDimensao[] = [
  { nome: "estabilidade", pesoBase: 0.35 },
  { nome: "wifiRedeLocal", pesoBase: 0.25 },
  { nome: "velocidade", pesoBase: 0.25 },
  { nome: "dns", pesoBase: 0.10 },
  { nome: "historico", pesoBase: 0.05 },
];

const PESOS_FIBRA: PesoDimensao[] = [
  { nome: "fibra", pesoBase: 0.35 },
  { nome: "estabilidade", pesoBase: 0.30 },
  { nome: "velocidade", pesoBase: 0.20 },
  { nome: "dns", pesoBase: 0.10 },
  { nome: "historico", pesoBase: 0.05 },
];

const PESOS_MOVEL: PesoDimensao[] = [
  { nome: "sinalMovel", pesoBase: 0.30 },
  { nome: "estabilidade", pesoBase: 0.25 },
  { nome: "velocidade", pesoBase: 0.25 },
  { nome: "dns", pesoBase: 0.10 },
  { nome: "historico", pesoBase: 0.05 },
];

const PESOS_DESCONHECIDO: PesoDimensao[] = [
  { nome: "estabilidade", pesoBase: 0.40 },
  { nome: "velocidade", pesoBase: 0.40 },
  { nome: "dns", pesoBase: 0.15 },
  { nome: "historico", pesoBase: 0.05 },
];

function pesosPara(tipo: TipoConexao): PesoDimensao[] {
  switch (tipo) {
    case "WIFI": return PESOS_WIFI;
    case "FIBRA": return PESOS_FIBRA;
    case "MOVEL": return PESOS_MOVEL;
    case "DESCONHECIDO": return PESOS_DESCONHECIDO;
  }
}

function notaParaStatus(status: MetricStatus): number {
  switch (status) {
    case "excelente": return 100;
    case "bom": return 80;
    case "regular": return 60;
    case "ruim": return 40;
    case "critico": return 15;
    case "inconclusivo": return 50;
  }
}

// ── Classificadores portados de MetricClassifier.kt ────────────────────────

function classificarLatencia(ms: number): MetricStatus {
  if (ms < 100) return "excelente";
  if (ms <= 150) return "bom";
  if (ms <= 200) return "regular";
  return "ruim";
}

function classificarJitter(ms: number): MetricStatus {
  if (ms < 5) return "excelente";
  if (ms <= 10) return "bom";
  if (ms <= 20) return "regular";
  return "ruim";
}

function classificarPerdaPacotes(percent: number): MetricStatus {
  if (percent <= 0) return "excelente";
  if (percent < 0.5) return "bom";
  if (percent <= 2.0) return "regular";
  return "ruim";
}

/** deltaMs = loadedLatencyMs - latencyMs (bufferbloat real, nao a latencia sob carga bruta). */
function classificarBufferbloat(deltaMs: number): MetricStatus {
  const delta = Math.max(0, deltaMs);
  if (delta < 5) return "excelente";
  if (delta <= 30) return "bom";
  if (delta <= 100) return "regular";
  return "ruim";
}

function classificarRssiWifi(rssiDbm: number, band: string | undefined): MetricStatus {
  if (band === "2_4_GHZ") {
    if (rssiDbm > -50) return "excelente";
    if (rssiDbm > -60) return "bom";
    if (rssiDbm > -70) return "regular";
    if (rssiDbm > -80) return "ruim";
    return "critico";
  }
  // 5GHz e 6GHz reaproveitam a mesma regua (sem tabela propria pra 6GHz).
  if (rssiDbm > -55) return "excelente";
  if (rssiDbm > -65) return "bom";
  if (rssiDbm > -75) return "regular";
  if (rssiDbm > -82) return "ruim";
  return "critico";
}

function classificarLatenciaDns(ms: number): MetricStatus {
  if (ms <= 50) return "excelente";
  if (ms <= 150) return "bom";
  if (ms <= 300) return "regular";
  return "ruim";
}

function isNr5g(technology: string | undefined): boolean {
  if (!technology) return false;
  const upper = technology.toUpperCase();
  return upper.includes("5G") || upper.includes("NR");
}

function classificarRsrp(rsrpDbm: number, nr5g: boolean): MetricStatus {
  if (nr5g) {
    if (rsrpDbm > -80) return "excelente";
    if (rsrpDbm > -95) return "bom";
    if (rsrpDbm > -110) return "regular";
    return "ruim";
  }
  if (rsrpDbm > -80) return "excelente";
  if (rsrpDbm > -90) return "bom";
  if (rsrpDbm > -100) return "regular";
  return "ruim";
}

function classificarRsrq(rsrqDb: number): MetricStatus {
  if (rsrqDb > -10) return "excelente";
  if (rsrqDb > -15) return "bom";
  if (rsrqDb > -20) return "regular";
  return "ruim";
}

function classificarSinr(sinrDb: number, nr5g: boolean): MetricStatus {
  if (nr5g) {
    if (sinrDb > 20) return "excelente";
    if (sinrDb > 10) return "bom";
    if (sinrDb > 0) return "regular";
    return "ruim";
  }
  if (sinrDb > 20) return "excelente";
  if (sinrDb > 13) return "bom";
  if (sinrDb > 0) return "regular";
  return "ruim";
}

// Extrapolacoes documentadas (sem tabela Android dedicada — ver cabecalho do arquivo).
function classificarFibraRx(rxDbm: number): MetricStatus {
  if (rxDbm >= -20) return "excelente";
  if (rxDbm >= -24) return "bom";
  if (rxDbm >= -27) return "regular";
  return "critico";
}

function classificarVelocidade(mbps: number): MetricStatus {
  if (mbps >= 100) return "excelente";
  if (mbps >= 50) return "bom";
  if (mbps >= 25) return "regular";
  return "ruim";
}

function piorStatus(status: MetricStatus[]): MetricStatus | null {
  if (status.length === 0) return null;
  const ordem: MetricStatus[] = ["excelente", "bom", "regular", "ruim", "critico", "inconclusivo"];
  const severidade = (s: MetricStatus): number => (s === "inconclusivo" ? 2.5 : ordem.indexOf(s));
  return status.reduce((pior, atual) => (severidade(atual) > severidade(pior) ? atual : pior));
}

function calcularDegradacao(avg30?: number, avg7?: number, higherIsBetter = true): number | null {
  if (typeof avg30 !== "number" || typeof avg7 !== "number" || avg30 <= 0) return null;
  return higherIsBetter ? ((avg30 - avg7) / avg30) * 100 : ((avg7 - avg30) / avg30) * 100;
}

interface DimensaoEvidencia {
  nome: string;
  nota: number | null;
}

function inferirTipoConexao(snapshot: DiagnosticSnapshot): TipoConexao {
  if (snapshot.fiber !== null && snapshot.fiber !== undefined) return "FIBRA";
  if (snapshot.mobile !== null && snapshot.mobile !== undefined) return "MOVEL";
  if (snapshot.connection?.type === "WIFI" || snapshot.wifi || snapshot.wifiScan) return "WIFI";
  return "DESCONHECIDO";
}

function dimensaoEstabilidade(snapshot: DiagnosticSnapshot): number | null {
  const status: MetricStatus[] = [];
  const q = snapshot.quality;
  if (typeof q?.latencyMs === "number") status.push(classificarLatencia(q.latencyMs));
  if (typeof q?.jitterMs === "number") status.push(classificarJitter(q.jitterMs));
  if (typeof q?.packetLossPercent === "number") status.push(classificarPerdaPacotes(q.packetLossPercent));
  if (typeof q?.loadedLatencyMs === "number" && typeof q?.latencyMs === "number") {
    status.push(classificarBufferbloat(q.loadedLatencyMs - q.latencyMs));
  }
  const pior = piorStatus(status);
  return pior ? notaParaStatus(pior) : null;
}

function dimensaoWifiRedeLocal(snapshot: DiagnosticSnapshot): number | null {
  const rssi = snapshot.wifi?.rssiDbm;
  if (typeof rssi !== "number") return null;
  return notaParaStatus(classificarRssiWifi(rssi, snapshot.wifi?.band));
}

function dimensaoVelocidade(snapshot: DiagnosticSnapshot): number | null {
  const download = snapshot.speed?.downloadMbps;
  if (typeof download !== "number") return null;
  return notaParaStatus(classificarVelocidade(download));
}

function dimensaoDns(snapshot: DiagnosticSnapshot): number | null {
  const latencia = snapshot.dns?.latencyMs;
  if (typeof latencia !== "number") return null;
  return notaParaStatus(classificarLatenciaDns(latencia));
}

function dimensaoHistorico(snapshot: DiagnosticSnapshot): number | null {
  const h = snapshot.historical;
  if (!h) return null;
  const degradacoes = [
    calcularDegradacao(h.avgDownload30d, h.avgDownload7d, true),
    calcularDegradacao(h.avgUpload30d, h.avgUpload7d, true),
    calcularDegradacao(h.avgPing30d, h.avgPing7d, false),
    calcularDegradacao(h.avgDns30d, h.avgDns7d, false),
  ].filter((v): v is number => typeof v === "number");
  if (degradacoes.length === 0) return null;
  const maxDegradacao = Math.max(...degradacoes);
  if (maxDegradacao >= 40) return notaParaStatus("ruim");
  if (maxDegradacao >= 20) return notaParaStatus("regular");
  return notaParaStatus("excelente");
}

function dimensaoFibra(snapshot: DiagnosticSnapshot): number | null {
  const rx = snapshot.fiber?.rxPowerDbm;
  if (typeof rx !== "number") return null;
  return notaParaStatus(classificarFibraRx(rx));
}

function dimensaoSinalMovel(snapshot: DiagnosticSnapshot): number | null {
  const mobile = snapshot.mobile;
  if (!mobile) return null;
  const nr5g = isNr5g(mobile.technology as string | undefined);
  const status: MetricStatus[] = [];
  if (typeof mobile.rsrpDbm === "number") status.push(classificarRsrp(mobile.rsrpDbm, nr5g));
  if (typeof mobile.rsrqDb === "number") status.push(classificarRsrq(mobile.rsrqDb));
  if (typeof mobile.sinrDb === "number") status.push(classificarSinr(mobile.sinrDb, nr5g));
  const pior = piorStatus(status);
  return pior ? notaParaStatus(pior) : null;
}

function coletarEvidencias(snapshot: DiagnosticSnapshot): DimensaoEvidencia[] {
  return [
    { nome: "estabilidade", nota: dimensaoEstabilidade(snapshot) },
    { nome: "wifiRedeLocal", nota: dimensaoWifiRedeLocal(snapshot) },
    { nome: "velocidade", nota: dimensaoVelocidade(snapshot) },
    { nome: "dns", nota: dimensaoDns(snapshot) },
    { nome: "historico", nota: dimensaoHistorico(snapshot) },
    { nome: "fibra", nota: dimensaoFibra(snapshot) },
    { nome: "sinalMovel", nota: dimensaoSinalMovel(snapshot) },
  ];
}

const NOTA_CRITICO = 25;
const NOTA_RUIM = 45;

const TETO_PERDA_PACOTES_CRITICA = 45;
const TETO_BUFFERBLOAT_CRITICO = 60;
const TETO_FIBRA_RX_CRITICA = 35;
const TETO_RSSI_FRACO_DOWNLOAD_BAIXO = 65;

function aplicarTetos(scoreBase: number, snapshot: DiagnosticSnapshot): number {
  const tetos: number[] = [];
  const q = snapshot.quality;

  if (typeof q?.packetLossPercent === "number" && notaParaStatus(classificarPerdaPacotes(q.packetLossPercent)) <= NOTA_CRITICO) {
    tetos.push(TETO_PERDA_PACOTES_CRITICA);
  }

  if (typeof q?.loadedLatencyMs === "number" && typeof q?.latencyMs === "number") {
    const bufferbloatStatus = classificarBufferbloat(q.loadedLatencyMs - q.latencyMs);
    if (notaParaStatus(bufferbloatStatus) <= NOTA_CRITICO) tetos.push(TETO_BUFFERBLOAT_CRITICO);
  }

  const rx = snapshot.fiber?.rxPowerDbm;
  if (typeof rx === "number" && notaParaStatus(classificarFibraRx(rx)) <= NOTA_CRITICO) {
    tetos.push(TETO_FIBRA_RX_CRITICA);
  }

  const rssi = snapshot.wifi?.rssiDbm;
  const download = snapshot.speed?.downloadMbps;
  if (
    typeof rssi === "number" && notaParaStatus(classificarRssiWifi(rssi, snapshot.wifi?.band)) <= NOTA_CRITICO
    && typeof download === "number" && notaParaStatus(classificarVelocidade(download)) <= NOTA_RUIM
  ) {
    tetos.push(TETO_RSSI_FRACO_DOWNLOAD_BAIXO);
  }

  if (tetos.length === 0) return scoreBase;
  return Math.min(scoreBase, Math.min(...tetos));
}

export interface ScoreResult {
  score: number;
  tipoConexao: TipoConexao;
}

/**
 * Calcula o score 0-100 ponderado por tipo de conexao, com reponderacao para
 * dimensoes sem dado e teto por metrica critica isolada. `findings` nao entra
 * diretamente no calculo (o motor real tambem nao usa findings, so metricas
 * brutas) — mantido no parametro apenas para simetria com o call site.
 */
export function calcularScore(snapshot: DiagnosticSnapshot, _findings: DiagnosticFinding[]): ScoreResult {
  const tipoConexao = inferirTipoConexao(snapshot);
  const pesos = pesosPara(tipoConexao);
  const evidenciasPorNome = new Map(coletarEvidencias(snapshot).map((e) => [e.nome, e]));

  const disponiveis = pesos
    .map((peso) => ({ peso, evidencia: evidenciasPorNome.get(peso.nome) }))
    .filter((item): item is { peso: PesoDimensao; evidencia: DimensaoEvidencia } =>
      item.evidencia !== undefined && item.evidencia.nota !== null,
    );

  if (disponiveis.length === 0) {
    return { score: 50, tipoConexao }; // inconclusivo — sem nenhuma dimensao disponivel
  }

  const somaPesos = disponiveis.reduce((sum, item) => sum + item.peso.pesoBase, 0);
  const mediaPonderada = disponiveis.reduce((sum, item) => {
    const pesoReponderado = item.peso.pesoBase / somaPesos;
    return sum + (item.evidencia.nota ?? 0) * pesoReponderado;
  }, 0);

  const scoreBase = Math.max(0, Math.min(100, Math.round(mediaPonderada)));
  const scoreComTeto = aplicarTetos(scoreBase, snapshot);

  return { score: scoreComTeto, tipoConexao };
}

/** Veredito humano a partir do score — GH#958, exigencia do design system de
 *  nunca expor metrica crua sem veredito. Faixas proprias deste campo (nao
 *  confundir com MetricStatus, que tem 6 niveis e serve pra dimensao unica). */
export type VeredictoHumano = "excelente" | "bom" | "regular" | "fraco";

export function veredictoHumanoDoScore(score: number): VeredictoHumano {
  if (score >= 85) return "excelente";
  if (score >= 65) return "bom";
  if (score >= 40) return "regular";
  return "fraco";
}
