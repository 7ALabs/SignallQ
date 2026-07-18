export const LK = {
  // Primary — chave violeta (MD3, migração 2026-07-13; era #6C2BFF antes)
  accent: '#5B21D6',
  // Secondary — azul FIXO, não deriva mais do accent (era accentBlue: #2563EB)
  accentBlue: '#2851B8',
  success: '#146C2E',
  warning: '#8A5000',
  error: '#BA1A1A',
  phaseLatencia: '#2563EB',
  phaseDownload: '#146C2E',
  phaseUpload: '#8A5000',
  bgPrimary: '#FFFFFF',
  bgSecondary: '#F8F5FB',
  bgCard: '#FFFFFF',
  textPrimary: '#1C1B1F',
  textSecondary: '#49454F',
  textTertiary: '#49454F',
  border: '#79747E',
  rCard: 16,
  rBtn: 20,
  rField: 12,
  rSheet: 28,
  rDialog: 24,
  rPill: 999,
  font: "'Google Sans Flex', 'Google Sans', 'Roboto', system-ui, sans-serif",
} as const;

/** SignallQ AI surfaces — always dark, theme-independent */
export const ORB = {
  bg: '#0D0D1A',
  surface: '#1A0B2E',
  card: '#1E1130',
  text: '#F3F4F6',
  sub: '#9CA3AF',
} as const;
