export const APP_NAME = "SignallQ Admin";

export const DEV_ENVIRONMENT_LABEL = "Staging";
export const PROD_ENVIRONMENT_LABEL = "Produção";

export const COLOR_PALETTE = {
  background: "#08080A", // or #0A0A0D
  surface: "#111111", // Card background
  surfaceSecondary: "#18181B", // Sidebar/Secondary background
  border: "#262626", // Border color hairline
  textPrimary: "#F3F4F6",
  textSecondary: "#9CA3AF",
  textTertiary: "#6B7280",
  primary: "#6C2BFF", // Accent primary main purple
  success: "#22C55E",
  attention: "#F5A623",
  error: "#FF4D4F",
  info: "#38BDF8",
};

export const REFRESH_INTERVALS = [
  { label: "Manual", value: 0 },
  { label: "A cada 30s", value: 30000 },
  { label: "A cada 1min", value: 60000 },
  { label: "A cada 5min", value: 300000 },
];

export const PERIOD_FILTERS = [
  { label: "Hoje", value: "today" },
  { label: "Últimos 7 dias", value: "7d" },
  { label: "Últimos 30 dias", value: "30d" },
  { label: "Personalizado", value: "custom" },
];
