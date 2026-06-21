import { ContextualAdOpportunity, MonetizationSettings } from "../types/ads";

export const mockAdOpportunities: ContextualAdOpportunity[] = [
  {
    issue: "wifi_signal_weak",
    label: "Sinal Wi-Fi Fraco",
    eligibleDiagnostics: 3820,
    estimatedImpressions: 11460,
    recommendedCategories: ["Roteadores Mesh", "Repetidores Wi-Fi", "Cabos Ethernet", "Suporte Técnico de Rede"],
    sensitivity: "low",
    requiresConsent: false,
    status: "planned"
  },
  {
    issue: "bufferbloat_upload",
    label: "Bufferbloat no Upload (Latência alta sob carga)",
    eligibleDiagnostics: 2140,
    estimatedImpressions: 6420,
    recommendedCategories: ["Roteadores com Smart Queue Management (SQM/QoS)", "Planos de Fibra com maior Upload"],
    sensitivity: "medium",
    requiresConsent: true,
    status: "planned"
  },
  {
    issue: "dns_latency_high",
    label: "Problemas ou lentidão de DNS",
    eligibleDiagnostics: 1680,
    estimatedImpressions: 5040,
    recommendedCategories: ["Serviços de DNS Premium", "Provedores de VPN Segura/Otimizada", "Roteadores Configuráveis"],
    sensitivity: "medium",
    requiresConsent: true,
    status: "planned"
  },
  {
    issue: "mobile_congestion_suspected",
    label: "Rede Móvel Congestionada",
    eligibleDiagnostics: 1120,
    estimatedImpressions: 3360,
    recommendedCategories: ["Planos de Telefonia Móvel (Upgrade)", "Operadoras eSIM alternativas", "Comparativo de Operadoras"],
    sensitivity: "high",
    requiresConsent: true,
    status: "blocked_by_privacy"
  }
];

export const defaultMonetizationSettings: MonetizationSettings = {
  adsEnabled: false,
  contextualAdsEnabled: false,
  personalizedAdsEnabled: false,
  requireConsent: true,
  provider: "none",
  blockSensitiveTargeting: true,
  connectivityAdsOnly: true
};
