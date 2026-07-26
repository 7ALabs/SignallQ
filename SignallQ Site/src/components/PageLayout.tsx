import type { ReactNode } from 'react'
import { AdBannerWide } from './AdBannerWide'
import { AdRail } from './AdRail'
import { AdSlotsProvider } from './AdSlotsProvider'
import { SiteFooter } from './SiteFooter'
import { SiteNav } from './SiteNav'

type RotaAtiva = 'home' | 'pro' | 'historico' | 'sobre' | 'privacidade' | 'termos' | 'como-medimos' | 'bufferbloat' | 'cgnat'

interface PageLayoutProps {
  active: RotaAtiva
  children: ReactNode
  /**
   * Conteúdo do topo exibido só no mobile (`lg:hidden`), no lugar do `SiteNav` — usado pelo
   * fluxo do PWA (Home/Histórico), que trocam o nav institucional por um chrome mínimo
   * (`FlowTopBar`/`DetailTopBar`) nesse breakpoint (issue #1186). Quando omitido (páginas
   * institucionais e `/pro`), o `SiteNav` aparece em todas as larguras — comportamento
   * original do layout.
   */
  mobileTopBar?: ReactNode
  /** Skin de cor do `AdBannerWide` de rodapé. Default `'a'` (roxo). */
  adBannerVariant?: 'a' | 'b'
  /**
   * Suprime o `AdBannerWide` de rodapé embutido no layout — usado pela Home, que decide
   * sozinha (dentro do próprio fluxo de conteúdo) quando mostrar o banner, porque no estado
   * de Resultado o anúncio já vem embutido no card (`ResultAdCard`, ver `ResultPanel.tsx`).
   */
  hideAdBannerWide?: boolean
  /**
   * Override do className do wrapper de conteúdo (AdRail + children + AdRail) — usado quando
   * a página tem um perfil de padding próprio (ex.: Home, cujo padding mobile difere do
   * institucional padrão). Quando omitido, usa o padding institucional padrão.
   */
  contentClassName?: string
}

const DEFAULT_CONTENT_CLASSNAME = 'flex w-full flex-1 items-start justify-center gap-6 box-border px-5 pt-7 pb-10 lg:px-6 lg:pt-5 lg:pb-4'

// Layout padrão do site — reconstrução v2 (`ScreenDoc.dc.html`/`Screen404.dc.html`,
// `.claude/design-specs/2026-07-25-site-webapp-v2/`): SiteNav -> AdRail(a) + conteúdo +
// AdRail(b) (ambos só desktop, `lg:`) -> AdBannerWide -> SiteFooter. Usado pelas 6 páginas
// de `ScreenDoc` (Como medimos, Quem somos, Privacidade, Termos, Bufferbloat, CGNAT), pela
// 404 (`Screen404`) e, desde a unificação de 2026-07-26 (auditoria 1:1), também por
// Home/Histórico/PRO — cada uma via as props opcionais acima, que preservam o que tinham de
// específico (chrome mobile mínimo, skin do banner, padding próprio) sem duplicar a moldura.
export function PageLayout({ active, children, mobileTopBar, adBannerVariant = 'a', hideAdBannerWide = false, contentClassName }: PageLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      {mobileTopBar ? (
        <>
          <div className="hidden lg:block">
            <SiteNav active={active} />
          </div>
          <div className="lg:hidden">{mobileTopBar}</div>
        </>
      ) : (
        <SiteNav active={active} />
      )}

      {/* `AdSlotsProvider` coordena os espaços de anúncio desta página (2 AdRail +
          1 AdBannerWide) — busca o catálogo uma vez e distribui itens distintos, sem
          repetir o mesmo anúncio em 2 espaços (issue #1402/#1405). */}
      <AdSlotsProvider>
        <div className={contentClassName ?? DEFAULT_CONTENT_CLASSNAME}>
          <AdRail variant="a" />
          <div className="flex w-full flex-1 flex-col">{children}</div>
          <AdRail variant="b" />
        </div>

        {!hideAdBannerWide && (
          <div className="mx-auto w-full max-w-[860px] px-5 pb-7 box-border">
            <AdBannerWide variant={adBannerVariant} />
          </div>
        )}
      </AdSlotsProvider>

      <SiteFooter />
    </div>
  )
}
