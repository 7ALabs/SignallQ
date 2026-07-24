export interface NavigationItem {
  name: string;
  path: string;
  iconName: "LayoutDashboard" | "LineChart" | "Activity" | "Wifi" | "Globe" | "BrainCircuit" | "AlertTriangle" | "GitBranch" | "ToggleRight" | "Settings" | "HeartPulse" | "Wrench" | "PlayCircle" | "Flame";
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
 * Alinhado ao NAV.map do protótipo To-Be MD3 confirmado em 2026-07-17
 * (`docs_ai/design-system/PLANO_APLICACAO_TOBE_CONSOLE_2026-07-17.md`, achado
 * transversal #1 — fonte: `Md3NavDrawer.dc.html`/`Md3NavRail.dc.html`, projeto
 * Claude Design "SignallQ Design System", pasta `signallq-admin-fluxo-tobe-md3`).
 * 5 seções: "Centro de Controle" solta no topo (sem rótulo de grupo), depois
 * "App", "Rede & Operadora", "Custos & Sistema", "Administração" — substitui o
 * agrupamento flat de 9/10 entradas do GH#552 (Fase 1, ver
 * `docs_ai/_archive/2026-07-12_WIREFRAME_ADMIN_REDESIGN_552.md`), que usava a
 * referência de design errada.
 *
 * Cada item ainda aponta para a rota/slug técnico atual (`/overview`, `/errors`
 * etc.). "Redes & Provedores" e "Configurações" são fusões previstas
 * (networks+operators, settings+feature-flags): por ora apontam para a rota
 * primária (`/networks`, `/settings`); `/operators` e `/feature-flags`
 * continuam existindo e acessíveis por hash direto até a fusão de conteúdo
 * acontecer.
 */
export const NAVIGATION_SECTIONS: NavigationSection[] = [
  {
    label: "",
    items: [
      { name: "Centro de Controle", path: "/overview", iconName: "LayoutDashboard" },
    ],
  },
  {
    label: "App",
    items: [
      { name: "Uso do App", path: "/product-analytics", iconName: "LineChart" },
      { name: "Releases & Qualidade", path: "/app-versions", iconName: "GitBranch" },
      { name: "Problemas & Incidentes", path: "/errors", iconName: "AlertTriangle", badgeType: "error" },
    ],
  },
  {
    label: "Rede & Operadora",
    items: [
      { name: "Diagnósticos", path: "/diagnostics", iconName: "Activity" },
      { name: "Redes & Provedores", path: "/networks", iconName: "Wifi" },
    ],
  },
  {
    // GH#1341/#1342/#1343/#1344 — item 1 do plano de UX Google Play/Firebase: fontes externas
    // (proveniência SIG-294), não features do produto. "Firebase" adicionado com as 5
    // integrações que já têm endpoint real em produção (Management, Remote Config, App Check,
    // App Distribution, FCM delivery) — RTDN, CSV instalação/desinstalação, Crashlytics/
    // Performance via BigQuery, A/B Testing e In-App Messaging seguem fora de escopo.
    label: "Plataformas",
    items: [
      { name: "Google Play", path: "/google-play", iconName: "PlayCircle" },
      { name: "Firebase", path: "/firebase", iconName: "Flame" },
    ],
  },
  {
    label: "Custos & Sistema",
    items: [
      { name: "IA & Custos", path: "/ai-cost", iconName: "BrainCircuit" },
      { name: "Saúde do Sistema", path: "/system-health", iconName: "HeartPulse" },
    ],
  },
  {
    label: "Administração",
    items: [
      { name: "Configurações", path: "/settings", iconName: "Settings" },
      { name: "Ferramentas", path: "/tools", iconName: "Wrench" },
    ],
  },
];

/** Lista achatada — mantida para quem consumia NAVIGATION_ITEMS diretamente. */
export const NAVIGATION_ITEMS: NavigationItem[] = NAVIGATION_SECTIONS.flatMap((s) => s.items);
