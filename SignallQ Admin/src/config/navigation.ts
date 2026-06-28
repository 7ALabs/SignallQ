export interface NavigationItem {
  name: string;
  path: string;
  iconName: "LayoutDashboard" | "LineChart" | "Activity" | "Wifi" | "Globe" | "BrainCircuit" | "AlertTriangle" | "GitBranch" | "ToggleRight" | "Settings" | "HeartPulse";
  badge?: string;
  badgeType?: "info" | "error" | "warning";
}

export const NAVIGATION_ITEMS: NavigationItem[] = [
  {
    name: "Visão Geral",
    path: "/overview",
    iconName: "LayoutDashboard",
  },
  {
    name: "Produto & Uso",
    path: "/product-analytics",
    iconName: "LineChart",
  },
  {
    name: "Diagnósticos",
    path: "/diagnostics",
    iconName: "Activity",
    badge: "Novo",
    badgeType: "info",
  },
  {
    name: "Redes & RF",
    path: "/networks",
    iconName: "Wifi",
  },
  {
    name: "Operadoras",
    path: "/operators",
    iconName: "Globe",
  },
  {
    name: "IA & Custo",
    path: "/ai-cost",
    iconName: "BrainCircuit",
  },
  {
    name: "Erros",
    path: "/errors",
    iconName: "AlertTriangle",
    badge: "12",
    badgeType: "error",
  },
  {
    name: "Versões Android",
    path: "/app-versions",
    iconName: "GitBranch",
  },
  {
    name: "Feature Flags",
    path: "/feature-flags",
    iconName: "ToggleRight",
  },
  {
    name: "Saúde do Sistema",
    path: "/system-health",
    iconName: "HeartPulse",
  },
  {
    name: "Configurações",
    path: "/settings",
    iconName: "Settings",
  },
];
