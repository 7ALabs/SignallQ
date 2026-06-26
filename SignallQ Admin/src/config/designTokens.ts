/**
 * Valores resolvidos dos CSS tokens --sq-*.
 * Usados em contextos que não aceitam CSS vars diretamente (ex: SVG stroke no Recharts).
 * Mantido em sincronia com src/index.css — qualquer mudança de valor deve atualizar ambos.
 */
export const SQ_TOKENS = {
  accent:              "#6C2BFF",
  accentBlue:          "#2563EB",
  success:             "#22C55E",
  warning:             "#F5A623",
  error:               "#FF4D4F",
  phaseLatency:        "#60A5FA",
  phaseDownload:       "#34D399",
  phaseUpload:         "#FBBF24",
  providerCloudflare:  "#7C3AED",
  providerOpenAI:      "#2563EB",
  providerAnthropic:   "#E040FB",
  providerLocal:       "#71717A",
  textSecondary:       "#9CA3AF",
  textTertiary:        "#6B7280",
} as const;
