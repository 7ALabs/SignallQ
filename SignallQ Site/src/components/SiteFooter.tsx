import { Link } from 'react-router-dom'
import { useSystemTheme } from '../hooks/useSystemTheme'
import { Badge } from './Badge'
import { Logo } from './Logo'
import { PlayStoreBadge } from './PlayStoreBadge'

// Achado importante da Lia (revisão pré-construção): o rodapé não tinha
// nenhum CTA de download do app — perdia a oportunidade em 100% das páginas
// fora da Home pós-resultado. Coluna "Baixe o app" adicionada aqui.
export function SiteFooter() {
  const isDark = useSystemTheme()

  return (
    <div className="w-full box-border border-t" style={{ background: 'var(--bg-secondary)', borderColor: 'color-mix(in srgb, var(--border) 25%, transparent)' }}>
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
          © 2026 SignallQ. Produto em fase Beta.
        </div>
      </div>
    </div>
  )
}
