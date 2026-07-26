import { Link } from 'react-router-dom'
import { EmptyState } from '../components/EmptyState'
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
      <EmptyState
        className="mx-auto flex min-h-[520px] w-full max-w-[860px] flex-1 flex-col items-center justify-center gap-4 text-center"
        eyebrow="Erro 404"
        eyebrowClassName="label-overline"
        title="Página não encontrada"
        titleAs="h1"
        titleClassName="headline-large m-0"
        message="O endereço que você acessou não existe ou foi movido."
        messageClassName="body-medium m-0 max-w-[380px]"
        action={
          <Link
            to="/"
            className="mt-1 flex h-11 items-center gap-2 rounded-[var(--radius-button)] px-5 no-underline"
            style={{ background: 'var(--accent)' }}
          >
            <span className="material-symbols-outlined" style={{ fontSize: 20, color: 'var(--on-accent)' }}>
              speed
            </span>
            <span className="label-large" style={{ color: 'var(--on-accent)' }}>
              Ir para o teste de velocidade
            </span>
          </Link>
        }
      />
    </PageLayout>
  )
}
