import type { ReactNode } from 'react'
import Link from 'next/link'

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
  ctaLabel?: string
  ctaTo?: string
  children?: ReactNode
}

// Template único para as 8 rotas de conteúdo (Guia de Implementação v4, §7.3;
// fonte de código `ScreenDoc.dc.html`, componente `renderVals()`) — cada rota
// só fornece overline/título/seções, o layout é sempre o mesmo. `renderVals()`
// do protótipo fixa `cardBg: 'transparent'`, `cardShadow: 'none'`,
// `cardPadding: '0'`, `cardRadius: '0'` e `sectionColumns: 1` incondicionalmente
// (o campo `page.card` do mapa `PAGES` não é lido em lugar nenhum do template) —
// não existe mais distinção visual de "cartão" nem grid de 2/3 colunas: toda
// seção é um bloco simples empilhado, sem fundo/sombra/raio.
export function DocPage({ overline, title, intro, updated, sections, ctaLabel, ctaTo = '/', children }: DocPageProps) {
  return (
    <div className="mx-auto flex w-full max-w-[860px] flex-col gap-5 box-border">
      <div className="flex flex-col gap-2">
        <h1
          className="m-0 text-[26px] leading-[1.2] font-bold lg:text-[28px]"
          style={{ fontFamily: 'var(--font-sans)', color: 'var(--text-primary)', textWrap: 'pretty' }}
        >
          {title}
        </h1>
        {intro && (
          <p className="m-0 max-w-[720px]" style={{ font: '400 14px/1.45 var(--font-sans)', color: 'var(--text-secondary)', textWrap: 'pretty' }}>
            {intro}
          </p>
        )}
        {updated && (
          <div className="body-small" style={{ color: 'var(--text-tertiary)' }}>
            {updated}
          </div>
        )}
      </div>

      <div className="flex flex-col gap-4">
        {sections.map((secao) => (
          <section key={secao.title} className="flex flex-col gap-2">
            <h2 className="m-0" style={{ font: '600 16px/1.35 var(--font-sans)', color: 'var(--text-primary)' }}>
              {secao.title}
            </h2>
            <p className="m-0" style={{ font: '400 12px/1.5 var(--font-sans)', color: 'var(--text-secondary)', textWrap: 'pretty' }}>
              {secao.text}
            </p>
          </section>
        ))}
      </div>

      {children}

      {ctaLabel && (
        <Link href={ctaTo}
          className="flex h-10 w-fit items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
          style={{ background: 'var(--accent)', color: 'var(--on-accent)' }}
        >
          <span className="label-large" style={{ color: 'var(--on-accent)' }}>
            {ctaLabel}
          </span>
        </Link>
      )}
    </div>
  )
}


