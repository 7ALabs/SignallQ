export interface NavigationItem {
  name: string;
  path: string;
  iconName: "LayoutDashboard" | "LineChart" | "Activity" | "Wifi" | "Globe" | "BrainCircuit" | "AlertTriangle" | "GitBranch" | "ToggleRight" | "Settings" | "HeartPulse";
  badge?: string;
  badgeType?: "info" | "error" | "warning";
}

/**
 * SIG-294: seções agrupam as páginas pela proveniência real do dado (ver
 * `docs/architecture/data-architecture.md`), não por ordem arbitrária de feature.
 * Cada seção mapeia para um bloco do documento de arquitetura de dados (SIG-295).
 */
export interface NavigationSection {
  label: string;
  items: NavigationItem[];
}

export const NAVIGATION_SECTIONS: NavigationSection[] = [
  {
    label: "Visão Geral",
    items: [
      { name: "Visão Geral", path: "/overview", iconName: "LayoutDashboard" },
    ],
  },
  {
    label: "Produto & Diagnóstico",
    items: [
      { name: "Produto & Uso", path: "/product-analytics", iconName: "LineChart" },
      { name: "Diagnósticos", path: "/diagnostics", iconName: "Activity", badge: "Novo", badgeType: "info" },
      { name: "Redes & RF", path: "/networks", iconName: "Wifi" },
      { name: "Operadoras", path: "/operators", iconName: "Globe" },
    ],
  },
  {
    label: "IA & Confiabilidade",
    items: [
      { name: "IA & Custo", path: "/ai-cost", iconName: "BrainCircuit" },
      { name: "Erros", path: "/errors", iconName: "AlertTriangle", badge: "12", badgeType: "error" },
      { name: "Versões Android", path: "/app-versions", iconName: "GitBranch" },
    ],
  },
  {
    label: "Operação",
    items: [
      { name: "Feature Flags", path: "/feature-flags", iconName: "ToggleRight" },
      { name: "Saúde do Sistema", path: "/system-health", iconName: "HeartPulse" },
      { name: "Configurações", path: "/settings", iconName: "Settings" },
    ],
  },
];

/** Lista achatada — mantida para quem consumia NAVIGATION_ITEMS diretamente. */
export const NAVIGATION_ITEMS: NavigationItem[] = NAVIGATION_SECTIONS.flatMap((s) => s.items);
