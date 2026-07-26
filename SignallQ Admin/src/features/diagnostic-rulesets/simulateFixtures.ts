import { DiagnosticSnapshotFixture } from "../../types/diagnosticRulesets";

// Refs #1446 — o worker não expõe um catálogo de fixtures de simulação (fora
// de escopo alterar o contrato dele). Estas 3 fixtures são snapshots sintéticos
// escritos à mão, no formato real de DiagnosticSnapshot (contracts.ts do
// signallq-diagnostic-worker) — servem só para exercitar a regra antes de
// publicar, nunca representam sessão real de usuário.
export const SIMULATE_FIXTURES: DiagnosticSnapshotFixture[] = [
  {
    id: "wifi-fraco",
    label: "Wi-Fi fraco (RSSI baixo, banda 2.4GHz)",
    description: "Sinal Wi-Fi degradado, sem 5GHz disponível — cenário típico de finding wifi_local.",
    snapshot: {
      schemaVersion: 3,
      connection: { type: "wifi", hasInternet: true, ipv6Available: false },
      wifi: { band: "2.4", rssiDbm: -78, frequencyMhz: 2437, linkSpeedMbps: 65, has5GhzAvailable: false, devicesOnNetwork: 6 },
      speed: { downloadMbps: 18, uploadMbps: 4 },
      quality: { latencyMs: 65, jitterMs: 22, packetLossPercent: 1.8, loadedLatencyMs: 140 },
    },
  },
  {
    id: "fibra-rx-baixo",
    label: "Fibra com RX power baixo",
    description: "ONT reportando potência óptica de recepção abaixo do esperado — cenário típico de finding fibra.",
    snapshot: {
      schemaVersion: 3,
      connection: { type: "ethernet", hasInternet: true, ipv6Available: true },
      speed: { downloadMbps: 210, uploadMbps: 95 },
      quality: { latencyMs: 12, jitterMs: 3, packetLossPercent: 0.1, loadedLatencyMs: 20 },
      fiber: { rxPowerDbm: -27.5, txPowerDbm: 2.1, temperatureCelsius: 52 },
    },
  },
  {
    id: "rede-movel-degradada",
    label: "Rede móvel degradada (RSRP/SINR ruins)",
    description: "4G com sinal fraco e SINR baixo — cenário típico de finding mobile.",
    snapshot: {
      schemaVersion: 3,
      connection: { type: "cellular", hasInternet: true, ipv6Available: true },
      speed: { downloadMbps: 6, uploadMbps: 1.2 },
      quality: { latencyMs: 110, jitterMs: 40, packetLossPercent: 3.5, loadedLatencyMs: 260 },
      mobile: { technology: "4G", rsrpDbm: -112, rsrqDb: -14, sinrDb: 1, operatorName: "Operadora Exemplo" },
    },
  },
];
