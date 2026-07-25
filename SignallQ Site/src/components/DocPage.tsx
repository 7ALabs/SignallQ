import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'

export interface DocSection {
  title: string
  text: string
}

interface DocPageProps {
  overline: string
  title: string
  intro?: string
  updated?: string
  sections: DocSection[]
  /**
   * Cartões com fundo (`como-medimos`/`bufferbloat`/`cgnat`) vs. texto corrido
   * sem cartão (`quem-somos`/`privacidade`/`termos`) — 1:1 com o campo
   * `page.card` do protótipo (`ScreenDoc.dc.html`).
   */
  card?: boolean
  ctaLabel?: string
  ctaTo?: string
  children?: ReactNode
}

// Template único para as páginas institucionais e de metodologia/SEO —
// reconstrução v2 (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenDoc.dc.html`,
// um componente cuja prop `page` troca o conteúdo). Em vez de 6 componentes de
// página duplicando a mesma composição de overline/título/seções/CTA, cada rota
// (ComoMedimosPage, QuemSomosPage, PrivacidadePage, TermosPage, BufferbloatPage,
// CgnatPage) só fornece o conteúdo — o layout (colunas, cartão, CTA) é decidido
// aqui a partir da contagem de seções e do prop `card`, igual ao `renderVals()`
// do protótipo (`sectionColumns: sections.length >= 5 ? 3 : 2`).
export function DocPage({ overline, title, intro, updated, sections, card = false, ctaLabel, ctaTo = '/', children }: DocPageProps) {
  const columns = sections.length >= 5 ? 'lg:columns-3' : 'lg:columns-2'

  return (
    <div className="mx-auto flex w-full max-w-[860px] flex-col gap-5 px-5 pb-16 pt-8 box-border">
      <div className="flex flex-col gap-2">
        <div className="overline">{overline}</div>
        <h1 className="headline-large m-0" style={{ textWrap: 'pretty' }}>
          {title}
        </h1>
        {intro && (
          <p className="body-large m-0 max-w-[720px]" style={{ textWrap: 'pretty' }}>
            {intro}
          </p>
        )}
        {updated && (
          <div className="body-small" style={{ color: 'var(--text-tertiary)' }}>
            {updated}
          </div>
        )}
      </div>

      <div className={`columns-1 gap-5 ${columns}`}>
        {sections.map((secao) => (
          <section
            key={secao.title}
            className={`mb-3.5 flex flex-col gap-1.5 break-inside-avoid ${card ? 'rounded-2xl p-4' : ''}`}
            style={card ? { background: 'var(--bg-secondary)' } : undefined}
          >
            <h2 className={`m-0 ${card ? 'title-large' : 'title-medium'}`}>{secao.title}</h2>
            <p className="body-medium m-0" style={{ textWrap: 'pretty' }}>
              {secao.text}
            </p>
          </section>
        ))}
      </div>

      {children}

      {ctaLabel && (
        <Link
          to={ctaTo}
          className="flex h-10 w-fit items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
          style={{ background: 'var(--accent)', color: '#fff' }}
        >
          <span className="label-large" style={{ color: '#fff' }}>
            {ctaLabel}
          </span>
        </Link>
      )}
    </div>
  )
}
