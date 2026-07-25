import { Link } from 'react-router-dom'
import { useSystemTheme } from '../hooks/useSystemTheme'
import { Badge } from './Badge'
import { Logo } from './Logo'
import { PlayStoreBadge } from './PlayStoreBadge'

// 3 densidades responsivas (reconstrução v2, SiteFooter.dc.html): mobile (<640px,
// grid 2 colunas de links planos), compact (640-1023px, linha única com wrap) e
// full (>=1024px, 4 colunas — mesmo breakpoint `lg:` do AdRail/AdBannerWide).
const LINKS_MOBILE = [
  { label: 'Teste de velocidade', href: '/' },
  { label: 'Histórico', href: '/historico' },
  { label: 'SignallQ PRO', href: '/pro' },
  { label: 'Como medimos', href: '/como-medimos' },
  { label: 'Internet boa mas travando', href: '/internet-boa-mas-travando' },
  { label: 'Lag em jogos online', href: '/lag-em-jogos-online' },
  { label: 'Quem somos', href: '/quem-somos' },
  { label: 'Política de Privacidade', href: '/privacidade' },
  { label: 'Termos de Uso', href: '/termos' },
]

const LINKS_COMPACT = [
  { label: 'Teste de velocidade', href: '/' },
  { label: 'SignallQ PRO', href: '/pro' },
  { label: 'Histórico', href: '/historico' },
  { label: 'Como medimos', href: '/como-medimos' },
  { label: 'Internet boa mas travando', href: '/internet-boa-mas-travando' },
  { label: 'Lag em jogos online', href: '/lag-em-jogos-online' },
  { label: 'Quem somos', href: '/quem-somos' },
  { label: 'Política de Privacidade', href: '/privacidade' },
  { label: 'Termos de Uso', href: '/termos' },
]

const COPYRIGHT = '© 2026 SignallQ · by 7A. Produto em fase Beta.'

// Achado importante da Lia (revisão pré-construção): o rodapé não tinha
// nenhum CTA de download do app — perdia a oportunidade em 100% das páginas
// fora da Home pós-resultado. Coluna "Baixe o app" adicionada na densidade
// full. Mantém `PlayStoreBadge` real em vez do card de anúncio animado que o
// protótipo mostra ali — CTA de download de verdade bate melhor que imitar um
// house ad, e os 3 espaços de anúncio universais (AdRail/AdBannerWide) já
// cobrem a superfície de publicidade da tela.
export function SiteFooter() {
  const isDark = useSystemTheme()

  return (
    <div className="w-full box-border border-t" style={{ background: 'var(--bg-secondary)', borderColor: 'color-mix(in srgb, var(--border) 25%, transparent)' }}>
      {/* Mobile (<640px) */}
      <div className="flex flex-col gap-4 px-5 pb-7 pt-6 box-border sm:hidden">
        <div className="flex items-center gap-2.5">
          <Logo isDark={isDark} height={24} />
          <Badge>Beta</Badge>
        </div>
        <div className="grid grid-cols-2 gap-x-3 gap-y-0.5">
          {LINKS_MOBILE.map((link) => (
            <Link key={link.href} to={link.href} className="body-small flex min-h-11 items-center no-underline" style={{ color: 'var(--text-primary)' }}>
              {link.label}
            </Link>
          ))}
        </div>
        <div className="body-small" style={{ color: 'var(--text-tertiary)' }}>
          {COPYRIGHT}
        </div>
      </div>

      {/* Compact (640-1023px) */}
      <div className="mx-auto hidden max-w-[1280px] flex-wrap items-center gap-x-5 gap-y-2.5 px-5 py-3.5 box-border sm:flex lg:hidden">
        <Logo isDark={isDark} height={22} />
        <Badge>Beta</Badge>
        <div className="flex flex-wrap gap-x-4 gap-y-1">
          {LINKS_COMPACT.map((link) => (
            <Link key={link.href} to={link.href} className="body-small no-underline" style={{ color: 'var(--text-primary)' }}>
              {link.label}
            </Link>
          ))}
        </div>
        <div className="ml-auto body-small" style={{ color: 'var(--text-tertiary)' }}>
          {COPYRIGHT}
        </div>
      </div>

      {/* Full (>=1024px) */}
      <div className="hidden lg:block">
        <div className="mx-auto flex max-w-[1280px] flex-wrap justify-between gap-10 px-5 pb-7 pt-12 box-border">
          <div className="flex max-w-[320px] flex-col gap-3">
            <Logo isDark={isDark} height={32} />
            <div className="body-small">Teste de velocidade e diagnóstico de conexão.</div>
            <Badge>Beta</Badge>
          </div>

          <div className="flex flex-wrap gap-12">
            <div className="flex flex-col gap-2.5">
              <div className="overline">Produto</div>
              <Link to="/" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Teste de velocidade
              </Link>
              <Link to="/pro" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                SignallQ PRO
              </Link>
              <Link to="/historico" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Histórico
              </Link>
              <Link to="/como-medimos" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Como medimos
              </Link>
            </div>
            <div className="flex flex-col gap-2.5">
              <div className="overline">Guias</div>
              <Link to="/internet-boa-mas-travando" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Internet boa mas travando
              </Link>
              <Link to="/lag-em-jogos-online" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Lag em jogos online
              </Link>
            </div>
            <div className="flex flex-col gap-2.5">
              <div className="overline">Institucional</div>
              <Link to="/quem-somos" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Quem somos
              </Link>
              <Link to="/privacidade" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Política de Privacidade
              </Link>
              <Link to="/termos" className="body-medium no-underline" style={{ color: 'var(--text-primary)' }}>
                Termos de Uso
              </Link>
            </div>
            <div className="flex flex-col gap-2.5">
              <div className="overline">Baixe o app</div>
              <PlayStoreBadge height={44} source="footer" />
              <Badge>Beta</Badge>
            </div>
          </div>
        </div>

        <div
          className="mx-auto flex flex-wrap justify-between gap-2 border-t px-5 pb-7 pt-4 box-border"
          style={{ maxWidth: 1280, borderColor: 'color-mix(in srgb, var(--border) 18%, transparent)' }}
        >
          <div className="body-small" style={{ color: 'var(--text-tertiary)' }}>
            {COPYRIGHT}
          </div>
        </div>
      </div>
    </div>
  )
}
