import { Link } from 'react-router-dom'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { NOT_FOUND_META } from '../lib/pageMetaCatalog'

// Composição 1:1 com `Screen404.dc.html` (reconstrução v2): SiteNav -> AdRail(a) +
// mensagem centralizada + AdRail(b) -> AdBannerWide(a) -> SiteFooter — via
// `PageLayout`, que já traz essa moldura para as páginas institucionais.
export default function NotFoundPage() {
  useDocumentMeta(NOT_FOUND_META)

  return (
    <PageLayout active="home">
      <div className="flex min-h-[420px] w-full flex-1 flex-col items-center justify-center gap-3.5 px-5 py-12 text-center">
        <div className="overline">Erro 404</div>
        <h1 className="headline-large m-0">Página não encontrada</h1>
        <p className="body-large m-0 max-w-[380px]">O endereço que você acessou não existe ou foi movido.</p>
        <Link
          to="/"
          className="mt-1 flex h-11 items-center gap-2 rounded-[var(--radius-button)] px-5 no-underline"
          style={{ background: 'var(--accent)' }}
        >
          <span className="material-symbols-outlined" style={{ fontSize: 20, color: '#fff' }}>
            speed
          </span>
          <span className="label-large" style={{ color: '#fff' }}>
            Ir para o teste de velocidade
          </span>
        </Link>
      </div>
    </PageLayout>
  )
}
