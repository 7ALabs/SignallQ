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
  /** Rótulo de grupo — string vazia renderiza a seção sem cabeçalho (menu flat). */
  label: string;
  items: NavigationItem[];
}

/**
 * GH#552 (Fase 1) — menu redesenhado como console operacional, 9 entradas
 * (uma por pergunta-guia), sem agrupamento por seção — ver
 * `docs_ai/design-system/WIREFRAME_ADMIN_REDESIGN_552.md`.
 *
 * Cada item ainda aponta para a rota/slug técnico atual (`/overview`, `/errors`
 * etc.) — a migração de rótulo é feita aqui, a migração de CONTEÚDO de tela é
 * Fase 2/3, fora deste escopo. "Redes & Provedores" e "Configurações" são
 * fusões previstas (networks+operators, settings+feature-flags): por ora
 * apontam para a rota primária (`/networks`, `/settings`); `/operators` e
 * `/feature-flags` continuam existindo e acessíveis por hash direto até a
 * fusão de conteúdo acontecer.
 */
export const NAVIGATION_SECTIONS: NavigationSection[] = [
  {
    label: "",
    items: [
      { name: "Centro de Controle", path: "/overview", iconName: "LayoutDashboard" },
      { name: "Diagnósticos", path: "/diagnostics", iconName: "Activity" },
      { name: "Problemas & Incidentes", path: "/errors", iconName: "AlertTriangle", badge: "12", badgeType: "error" },
      { name: "Redes & Provedores", path: "/networks", iconName: "Wifi" },
      { name: "Uso do App", path: "/product-analytics", iconName: "LineChart" },
      { name: "Releases & Qualidade", path: "/app-versions", iconName: "GitBranch" },
      { name: "IA & Custos", path: "/ai-cost", iconName: "BrainCircuit" },
      { name: "Saúde do Sistema", path: "/system-health", iconName: "HeartPulse" },
      { name: "Configurações", path: "/settings", iconName: "Settings" },
    ],
  },
];

/** Lista achatada — mantida para quem consumia NAVIGATION_ITEMS diretamente. */
export const NAVIGATION_ITEMS: NavigationItem[] = NAVIGATION_SECTIONS.flatMap((s) => s.items);
