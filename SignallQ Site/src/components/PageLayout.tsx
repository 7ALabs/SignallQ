import type { ReactNode } from 'react'
import { SiteFooter } from './SiteFooter'
import { SiteNav } from './SiteNav'

type RotaAtiva = 'home' | 'pro' | 'historico' | 'sobre' | 'privacidade' | 'termos'

interface PageLayoutProps {
  active: RotaAtiva
  children: ReactNode
}

// Layout padrão (nav opaco + conteúdo + footer) para as páginas de conteúdo
// estático. A Home tem hero com gradiente próprio e monta SiteNav/SiteFooter
// diretamente (ver HomePage.tsx) em vez de usar este wrapper genérico.
export function PageLayout({ active, children }: PageLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active={active} />
      <div className="flex flex-1 flex-col">{children}</div>
      <SiteFooter />
    </div>
  )
}
