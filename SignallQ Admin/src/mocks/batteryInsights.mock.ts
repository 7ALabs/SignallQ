import { BatteryImpactMetric } from "../types/battery";

export const mockBatteryImpactData: BatteryImpactMetric[] = [
  {
    feature: "devices_scan",
    label: "Scan de Dispositivos",
    estimatedImpact: "high",
    avgDurationMs: 18200,
    backgroundExecutionPercent: 5,
    networkCallsAvg: 254,
    retryRate: 0.12,
    wakeSensitive: true,
    notes: "Utiliza broadcast Multicast (mDNS) e varredura de sockets em rede local (/24) de forma sequencial ou paralela rápida, elevando picos de rádio e CPU."
  },
  {
    feature: "mobile_analysis",
    label: "Monitoramento Móvel Passivo",
    estimatedImpact: "medium",
    backgroundExecutionPercent: 100,
    avgDurationMs: 3600000, // Background contínuo segmentado
    networkCallsAvg: 4,
    retryRate: 0.08,
    wakeSensitive: false,
    notes: "Executado via WorkManager do Android a cada 30min e ouvindo callbacks telefônicos de variação de sinal de célula móvel (TelephonyManager)."
  },
  {
    feature: "speedtest",
    label: "SpeedTest",
    estimatedImpact: "medium",
    avgDurationMs: 14500,
    backgroundExecutionPercent: 0,
    networkCallsAvg: 18,
    retryRate: 0.03,
    wakeSensitive: true,
    notes: "Uso intenso e contínuo de transferência de dados (download/upload de arquivos multi-thread) para saturação de banda por 14 segundos."
  },
  {
    feature: "diagnosis",
    label: "Diagnóstico Completo",
    estimatedImpact: "medium",
    avgDurationMs: 25400,
    backgroundExecutionPercent: 2,
    networkCallsAvg: 12,
    retryRate: 0.06,
    wakeSensitive: true,
    notes: "Pipeline encadeado de testes rápidos (DNS, ICMP, gateway, etc.), disparando chamadas de API externas sequenciais."
  },
  {
    feature: "dns_test",
    label: "DNS Test",
    estimatedImpact: "low",
    avgDurationMs: 3100,
    backgroundExecutionPercent: 0,
    networkCallsAvg: 6,
    retryRate: 0.01,
    wakeSensitive: false,
    notes: "Resoluções rápidas de nome de domínio contra servidores populares sobre UDP. Impacto mínimo em rádio e bateria."
  },
  {
    feature: "history",
    label: "Histórico",
    estimatedImpact: "low",
    avgDurationMs: 800,
    backgroundExecutionPercent: 0,
    networkCallsAvg: 0,
    retryRate: 0,
    wakeSensitive: false,
    notes: "Consultas de menor complexidade a banco de dados local SQLite sem atividades de rede ou hardware ativo."
  }
];
