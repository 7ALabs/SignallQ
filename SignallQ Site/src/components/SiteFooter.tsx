import { Link } from 'react-router-dom'
import { useSystemTheme } from '../hooks/useSystemTheme'
import { Badge } from './Badge'
import { Logo } from './Logo'
import { SIGNALLQ_TEST_GROUP_URL } from '../lib/config'

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

// Decisão do Luiz (2026-07-25, revertendo divergência da reconstrução v2):
// a coluna "Baixe o app" volta a ser 1:1 com SiteFooter.dc.html — o card de
// anúncio animado do protótipo, não o PlayStoreBadge real. É "um anúncio
// interno com direcionamento": o CTA "Entrar na lista de teste" leva de
// verdade para o grupo de testadores fechado (SIGNALLQ_TEST_GROUP_URL, mesmo
// destino usado pelos outros CTAs de teste fechado do site) — conteúdo fixo
// do design, não depende do catálogo dinâmico de anúncios locais (AdRail/
// AdBannerWide).
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
            <div className="flex flex-col items-start gap-2.5">
              <div className="overline">Baixe o app</div>
              <div
                className="relative box-border flex w-[260px] flex-col gap-2.5 overflow-hidden rounded-2xl p-3.5"
                style={{ background: 'linear-gradient(140deg, #241A45 0%, #17132B 60%, #0E0D14 100%)' }}
              >
                <div
                  className="sq-ad-glow-a absolute rounded-full"
                  style={{
                    right: -50,
                    top: -50,
                    width: 160,
                    height: 160,
                    background: 'radial-gradient(circle, rgba(124,77,255,.42), rgba(124,77,255,0) 70%)',
                  }}
                />
                <div className="relative flex items-center gap-2.5">
                  <div className="flex h-[22px] flex-shrink-0 items-end gap-[3px]">
                    <div className="sq-ad-bar w-1 rounded-sm" style={{ height: 9, background: 'rgba(255,255,255,.28)', animationDelay: '0s' }} />
                    <div className="sq-ad-bar w-1 rounded-sm" style={{ height: 15, background: '#7C4DFF', animationDelay: '.12s' }} />
                    <div className="sq-ad-bar w-1 rounded-sm" style={{ height: 22, background: '#C9F26B', animationDelay: '.24s' }} />
                    <div className="sq-ad-bar w-1 rounded-sm" style={{ height: 13, background: '#7C4DFF', animationDelay: '.36s' }} />
                  </div>
                  <div className="font-bold" style={{ font: '700 15px/1.15 var(--font-sans)', color: '#fff', letterSpacing: '-0.01em' }}>
                    Baixe o app SignallQ
                  </div>
                </div>
                <div className="relative" style={{ font: '400 11px/1.4 var(--font-sans)', color: 'rgba(255,255,255,.62)' }}>
                  Mede o Wi-Fi de cada cômodo e mostra onde o sinal morre.
                </div>
                <div className="relative flex items-center gap-2">
                  <a
                    href={SIGNALLQ_TEST_GROUP_URL}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="relative flex h-8 items-center overflow-hidden rounded-2xl px-4 no-underline"
                    style={{ background: 'linear-gradient(135deg, #7C4DFF, #5B21D6)' }}
                  >
                    <span className="relative" style={{ font: '600 12px/1.2 var(--font-sans)', color: '#fff' }}>
                      Entrar na lista de teste
                    </span>
                    <span
                      className="sq-ad-sweep absolute top-0 bottom-0 left-0 w-[30px]"
                      style={{ background: 'linear-gradient(100deg, transparent, rgba(255,255,255,.4), transparent)' }}
                    />
                  </a>
                  <span style={{ font: '500 10px/1.3 var(--font-sans)', color: 'rgba(255,255,255,.5)' }}>Teste fechado · Android</span>
                </div>
              </div>
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
