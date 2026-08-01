import type { ReactNode } from "react";
import { SiteNav } from "./SiteNav";
import { SiteFooter } from "./SiteFooter";

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
  children: ReactNode;
}

// Fundação única de tela — fase Fundação da reconstrução v4 (`Guia de
// Implementação.dc.html`, §3/§6). Consolida os dois shells que existiam em
// paralelo (`PageShell` + `PageLayout`, pendência registrada no CLAUDE.md
// local) numa única implementação: SiteNav (sticky) -> miolo centralizado
// (`flex:1; max-width: contentMax`) -> SiteFooter.
//
// `SiteNav`/`SiteFooter` já resolvem rota ativa e responsividade sozinhos
// (pathname real via `usePathname` + gate CSS `md:`/`lg:`) — não repete prop
// `active`/`mobile` aqui, que o próprio CLAUDE.md documenta como bug
// corrigido em 01/08/2026 (prop nunca ligada a um viewport real).
//
// Sem `AdRail`/`AdBannerWide` — anúncio visual foi removido nesta fase; a
// infraestrutura técnica de AdSense (`AdSenseScript`) fica no layout raiz,
// sem posição reservada (ver `src/lib/adConsent.ts`).
export function PageShell({
  contentMax = "860px",
  align = "start",
  children,
}: PageShellProps) {
  return (
    <div className="flex min-h-screen w-full flex-col overflow-x-hidden">
      <SiteNav />

      <div
        className={`flex w-full flex-1 justify-center box-border py-2 px-4 pb-6 lg:pt-5 lg:px-[var(--safe-x)] lg:pb-4 ${
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

      <SiteFooter />
    </div>
  );
}
