export interface NavigationItem {
  name: string;
  path: string;
  iconName: "LayoutDashboard" | "LineChart" | "Activity" | "Wifi" | "Globe" | "BrainCircuit" | "AlertTriangle" | "GitBranch" | "Settings";
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
    name: "Produto & uso",
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
    name: "Análise de Redes",
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
    name: "Logs de Erros",
    path: "/errors",
    iconName: "AlertTriangle",
    badge: "12",
    badgeType: "error",
  },
  {
    name: "Versões do App",
    path: "/app-versions",
    iconName: "GitBranch",
  },
  {
    name: "Configurações",
    path: "/settings",
    iconName: "Settings",
  },
];
