import type { ReactNode } from 'react'
import { AdBannerWide } from './AdBannerWide'
import { AdRail } from './AdRail'
import { SiteFooter } from './SiteFooter'
import { SiteNav } from './SiteNav'

type RotaAtiva = 'home' | 'pro' | 'historico' | 'sobre' | 'privacidade' | 'termos' | 'como-medimos' | 'bufferbloat' | 'cgnat'

interface PageLayoutProps {
  active: RotaAtiva
  children: ReactNode
}

// Layout padrão para as páginas de conteúdo institucional/estático — reconstrução
// v2 (`ScreenDoc.dc.html`/`Screen404.dc.html`, `.claude/design-specs/2026-07-25-site-webapp-v2/`):
// SiteNav -> AdRail(a) + conteúdo + AdRail(b) (ambos só desktop, `lg:`) ->
// AdBannerWide(a) -> SiteFooter. Usado hoje só pelas 6 páginas de `ScreenDoc`
// (Como medimos, Quem somos, Privacidade, Termos, Bufferbloat, CGNAT) e pela 404
// (`Screen404`) — Home/Histórico/PRO montam a própria composição (hero e layout
// próprios, ver HomePage.tsx) em vez de usar este wrapper genérico.
export function PageLayout({ active, children }: PageLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active={active} />

      <div className="mx-auto flex w-full max-w-[1280px] flex-1 items-start justify-center gap-6 px-5 py-5 box-border">
        <AdRail variant="a" />
        <div className="flex w-full flex-1 flex-col">{children}</div>
        <AdRail variant="b" />
      </div>

      <div className="mx-auto w-full max-w-[860px] px-5 pb-7 box-border">
        <AdBannerWide variant="a" />
      </div>

      <SiteFooter />
    </div>
  )
}
