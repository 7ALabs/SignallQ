import type { ReactNode } from "react";

interface PageShellProps {
  contentMax?: string;
  /**
   * Alinhamento vertical do miolo dentro do espaço disponível (Guia de
   * Implementação, §7.1): "start" para conteúdo extenso ancorado ao topo
   * (ex.: Home idle/result), "center" para conteúdo curto centralizado na
   * altura disponível (ex.: Home running/erro). Default "start" preserva o
   * comportamento anterior à introdução deste prop.
   */
  align?: "start" | "center";
  /**
   * Padding do shell no mobile (classes Tailwind, <640px). Default 8/16/24
   * (Home/Histórico/PRO — Guia §7.1). ScreenDoc usa 28/20/40 (Guia §7.3,
   * `shellPadding` de `ScreenDoc.dc.html`), diferente das demais telas.
   */
  mobilePadding?: string;
  children: ReactNode;
}

// Miolo de tela — fase Fundação da reconstrução v4 (`Guia de Implementação.dc.html`,
// §3/§6). Consolida os dois shells que existiam em paralelo (`PageShell` +
// `PageLayout`, pendência registrada no CLAUDE.md local) numa única
// implementação: miolo centralizado (`flex:1; max-width: contentMax`).
//
// `SiteNav`/`SiteFooter` NÃO são renderizados aqui — desde o achado de
// 01/08/2026 ("topbar sambando" ao trocar de rota), os dois vivem no layout
// raiz (`src/app/layout.tsx`), fora da árvore de cada página, pra persistir
// entre navegações em vez de remontar a cada troca de tela (guia §1: "layout
// raiz com SiteNav + SiteFooter").
//
// Sem `AdRail`/`AdBannerWide` — anúncio visual foi removido nesta fase; a
// infraestrutura técnica de AdSense (`AdSenseScript`) fica no layout raiz,
// sem posição reservada (ver `src/lib/adConsent.ts`).
export function PageShell({
  contentMax = "860px",
  align = "start",
  mobilePadding = "py-2 px-4 pb-6",
  children,
}: PageShellProps) {
  return (
    <div
      className={`flex w-full flex-1 justify-center box-border ${mobilePadding} lg:pt-5 lg:px-[var(--safe-x)] lg:pb-4 ${
        align === "center" ? "items-center" : ""
      }`}
    >
      <div
        className="flex w-full flex-1 flex-col items-center gap-5"
        style={{ maxWidth: contentMax }}
      >
        {children}
      </div>
    </div>
  );
}
