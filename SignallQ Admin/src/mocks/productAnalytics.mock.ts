import { 
  FeatureUsageMetric, 
  ScreenNavigationMetric, 
  FeatureCrashMetric, 
  RetentionMetric, 
  FeatureAiUsageMetric 
} from "../types/productAnalytics";

export const mockFeatureUsage: FeatureUsageMetric[] = [
  {
    feature: "speedtest",
    label: "SpeedTest",
    usageCount: 12480,
    uniqueUsers: 4120,
    completionRate: 0.94,
    failureRate: 0.04,
    avgDurationMs: 14500,
    trendPercent: 12.4
  },
  {
    feature: "diagnosis",
    label: "Diagnóstico Completo",
    usageCount: 8210,
    uniqueUsers: 3380,
    completionRate: 0.88,
    failureRate: 0.08,
    avgDurationMs: 25400,
    trendPercent: 8.1
  },
  {
    feature: "wifi_analysis",
    label: "Análise Wi-Fi",
    usageCount: 5770,
    uniqueUsers: 2940,
    completionRate: 0.91,
    failureRate: 0.05,
    avgDurationMs: 8200,
    trendPercent: 4.5
  },
  {
    feature: "dns_test",
    label: "DNS Test",
    usageCount: 3110,
    uniqueUsers: 1720,
    completionRate: 0.96,
    failureRate: 0.02,
    avgDurationMs: 3100,
    trendPercent: -2.1
  },
  {
    feature: "devices_scan",
    label: "Scan de Dispositivos",
    usageCount: 1840,
    uniqueUsers: 920,
    completionRate: 0.76,
    failureRate: 0.15,
    avgDurationMs: 18200,
    trendPercent: 15.6
  },
  {
    feature: "fiber_modem",
    label: "Diagnóstico Fibra/Modem",
    usageCount: 1420,
    uniqueUsers: 810,
    completionRate: 0.83,
    failureRate: 0.11,
    avgDurationMs: 19500,
    trendPercent: 11.2
  },
  {
    feature: "guided_questions",
    label: "Perguntas Guiadas",
    usageCount: 1350,
    uniqueUsers: 720,
    completionRate: 0.65,
    failureRate: 0.25,
    avgDurationMs: 45000,
    trendPercent: -5.4
  }
];

export const mockScreenNavigation: ScreenNavigationMetric[] = [
  {
    screen: "home",
    label: "Início (Home)",
    views: 45200,
    uniqueUsers: 8400,
    avgTimeOnScreenSec: 28,
    exitRate: 0.18,
    nextMostCommonScreen: "speedtest"
  },
  {
    screen: "speedtest",
    label: "Velocidade",
    views: 18450,
    uniqueUsers: 4120,
    avgTimeOnScreenSec: 42,
    exitRate: 0.12,
    nextMostCommonScreen: "laudo"
  },
  {
    screen: "laudo",
    label: "Laudo Detalhado",
    views: 14200,
    uniqueUsers: 3380,
    avgTimeOnScreenSec: 55,
    exitRate: 0.35,
    nextMostCommonScreen: "home"
  },
  {
    screen: "diagnosis",
    label: "Diagnóstico Técnico",
    views: 9210,
    uniqueUsers: 2940,
    avgTimeOnScreenSec: 35,
    exitRate: 0.22,
    nextMostCommonScreen: "laudo"
  },
  {
    screen: "signal",
    label: "Monitoramento de Sinal",
    views: 7500,
    uniqueUsers: 2150,
    avgTimeOnScreenSec: 65,
    exitRate: 0.25,
    nextMostCommonScreen: "home"
  },
  {
    screen: "history",
    label: "Histórico",
    views: 4120,
    uniqueUsers: 1450,
    avgTimeOnScreenSec: 15,
    exitRate: 0.45,
    nextMostCommonScreen: "home"
  },
  {
    screen: "dns",
    label: "DNS",
    views: 3100,
    uniqueUsers: 1150,
    avgTimeOnScreenSec: 12,
    exitRate: 0.30,
    nextMostCommonScreen: "home"
  },
  {
    screen: "devices",
    label: "Dispositivos",
    views: 2850,
    uniqueUsers: 920,
    avgTimeOnScreenSec: 40,
    exitRate: 0.55,
    nextMostCommonScreen: "home"
  }
];

export const mockFeatureCrashes: FeatureCrashMetric[] = [
  {
    feature: "devices_scan",
    label: "Scan de Dispositivos",
    crashes: 31,
    nonFatalErrors: 88,
    anrs: 4,
    crashRate: 0.0168,
    affectedVersions: ["0.18.1", "0.18.0", "0.17.5"],
    severity: "critical"
  },
  {
    feature: "mobile_analysis",
    label: "Análise Móvel",
    crashes: 14,
    nonFatalErrors: 52,
    anrs: 2,
    crashRate: 0.0076,
    affectedVersions: ["0.18.1", "0.18.0"],
    severity: "attention"
  },
  {
    feature: "diagnosis",
    label: "Diagnóstico",
    crashes: 9,
    nonFatalErrors: 37,
    anrs: 0,
    crashRate: 0.0011,
    affectedVersions: ["0.18.1"],
    severity: "attention"
  },
  {
    feature: "speedtest",
    label: "SpeedTest",
    crashes: 4,
    nonFatalErrors: 18,
    anrs: 0,
    crashRate: 0.0003,
    affectedVersions: ["0.18.0"],
    severity: "ok"
  }
];

export const mockRetention: RetentionMetric[] = [
  {
    cohort: "Cohort geral (30d)",
    cohortSize: 4120,
    day1: 68,
    day7: 32,
    day30: 14,
    avgInstalledDays: 18.4,
    uninstallRate: 28
  }
];

export const mockFeatureAiUsage: FeatureAiUsageMetric[] = [
  {
    feature: "diagnosis",
    label: "Diagnóstico Técnico",
    aiCalls: 7840,
    tokensInput: 8400000,
    tokensOutput: 5800000,
    totalTokens: 14200000,
    estimatedCost: 112.80,
    avgLatencyMs: 1420,
    providerBreakdown: [
      {
        provider: "Google Gemini",
        calls: 7840,
        tokensInput: 8400000,
        tokensOutput: 5800000,
        estimatedCost: 112.80,
        avgLatencyMs: 1420
      }
    ]
  },
  {
    feature: "laudo",
    label: "Geração de Laudo",
    aiCalls: 6920,
    tokensInput: 5900000,
    tokensOutput: 3900000,
    totalTokens: 9800000,
    estimatedCost: 76.40,
    avgLatencyMs: 1850,
    providerBreakdown: [
      {
        provider: "Google Gemini",
        calls: 6920,
        tokensInput: 5900000,
        tokensOutput: 3900000,
        estimatedCost: 76.40,
        avgLatencyMs: 1850
      }
    ]
  },
  {
    feature: "guided_questions",
    label: "Perguntas Guiadas",
    aiCalls: 1220,
    tokensInput: 1300000,
    tokensOutput: 800000,
    totalTokens: 2100000,
    estimatedCost: 18.20,
    avgLatencyMs: 1150,
    providerBreakdown: [
      {
        provider: "Google Gemini",
        calls: 1220,
        tokensInput: 1300000,
        tokensOutput: 800000,
        estimatedCost: 18.20,
        avgLatencyMs: 1150
      }
    ]
  }
];
