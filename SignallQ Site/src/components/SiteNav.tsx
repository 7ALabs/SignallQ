import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useSystemTheme } from '../hooks/useSystemTheme'
import { Badge } from './Badge'
import { Logo } from './Logo'
import { PlayStoreBadge } from './PlayStoreBadge'
import { ProBadge } from './ProBadge'

type RotaAtiva = 'home' | 'pro' | 'historico' | 'sobre' | 'privacidade' | 'termos'

interface SiteNavProps {
  active: RotaAtiva
  heroMode?: boolean
}

const ITENS_ANTES = [{ key: 'home' as const, label: 'Teste de velocidade', href: '/' }]
const ITENS_DEPOIS = [
  { key: 'historico' as const, label: 'Histórico', href: '/historico' },
  { key: 'sobre' as const, label: 'Quem somos', href: '/quem-somos' },
  { key: 'privacidade' as const, label: 'Privacidade', href: '/privacidade' },
]

export function SiteNav({ active, heroMode = false }: SiteNavProps) {
  const isDark = useSystemTheme()
  const [mobileOpen, setMobileOpen] = useState(false)

  const linkClass = (key: RotaAtiva) =>
    `whitespace-nowrap border-b-2 pb-1 text-sm font-medium no-underline ${
      key === active ? 'border-[var(--accent)] text-[var(--accent)]' : 'border-transparent text-[var(--text-primary)]'
    }`

  return (
    <div
      className="relative w-full box-border border-b"
      style={{
        background: heroMode ? 'transparent' : 'var(--bg-primary)',
        borderColor: heroMode ? 'transparent' : 'color-mix(in srgb, var(--border) 25%, transparent)',
      }}
    >
      <div className="mx-auto flex min-h-[76px] max-w-[1280px] items-center justify-between gap-4 px-5 py-3.5 box-border">
        <Link to="/" className="flex flex-shrink-0 items-center">
          <Logo isDark={isDark} height={40} />
        </Link>

        <div className="hidden items-center gap-7 min-[920px]:flex">
          {ITENS_ANTES.map((link) => (
            <Link key={link.key} to={link.href} className={linkClass(link.key)}>
              {link.label}
            </Link>
          ))}
          <Link to="/pro" className={`flex items-center gap-1.5 pb-1 no-underline border-b-2 ${active === 'pro' ? 'border-[var(--accent)]' : 'border-transparent'}`}>
            <Logo isDark={isDark} height={16} />
            <ProBadge />
          </Link>
          {ITENS_DEPOIS.map((link) => (
            <Link key={link.key} to={link.href} className={linkClass(link.key)}>
              {link.label}
            </Link>
          ))}
        </div>

        <div className="hidden flex-shrink-0 items-center gap-2 min-[920px]:flex">
          <PlayStoreBadge height={40} source="nav" />
          <Badge>Beta</Badge>
        </div>

        <button
          aria-label={mobileOpen ? 'Fechar menu' : 'Abrir menu'}
          onClick={() => setMobileOpen((v) => !v)}
          className="flex h-10 w-10 items-center justify-center rounded-full border-none bg-transparent min-[920px]:hidden"
        >
          <span className="material-symbols-outlined">{mobileOpen ? 'close' : 'menu'}</span>
        </button>
      </div>

      {mobileOpen && (
        <div className="fixed inset-0 z-[1000] flex flex-col overflow-y-auto p-6 pt-4 box-border" style={{ background: 'var(--bg-primary)' }}>
          <div className="mb-8 flex items-center justify-between">
            <Logo isDark={isDark} height={28} />
            <button aria-label="Fechar menu" onClick={() => setMobileOpen(false)} className="flex h-10 w-10 items-center justify-center border-none bg-transparent">
              <span className="material-symbols-outlined">close</span>
            </button>
          </div>
          <div className="flex flex-col gap-1">
            {ITENS_ANTES.map((link) => (
              <Link
                key={link.key}
                to={link.href}
                onClick={() => setMobileOpen(false)}
                className="border-b py-3.5 text-xl font-semibold no-underline"
                style={{ color: link.key === active ? 'var(--accent)' : 'var(--text-primary)', borderColor: 'color-mix(in srgb, var(--border) 20%, transparent)' }}
              >
                {link.label}
              </Link>
            ))}
            <Link
              to="/pro"
              onClick={() => setMobileOpen(false)}
              className="flex items-center gap-2 border-b py-3.5 no-underline"
              style={{ borderColor: 'color-mix(in srgb, var(--border) 20%, transparent)' }}
            >
              <Logo isDark={isDark} height={20} />
              <ProBadge />
            </Link>
            {ITENS_DEPOIS.map((link) => (
              <Link
                key={link.key}
                to={link.href}
                onClick={() => setMobileOpen(false)}
                className="border-b py-3.5 text-xl font-semibold no-underline"
                style={{ color: link.key === active ? 'var(--accent)' : 'var(--text-primary)', borderColor: 'color-mix(in srgb, var(--border) 20%, transparent)' }}
              >
                {link.label}
              </Link>
            ))}
          </div>
          <div className="mt-8 flex items-center gap-3">
            <PlayStoreBadge height={48} source="nav-mobile" />
            <Badge>Beta</Badge>
          </div>
        </div>
      )}
    </div>
  )
}
