import { useNavigate } from 'react-router-dom'
import { PageLayout } from '../components/PageLayout'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { NOT_FOUND_META } from '../lib/pageMetaCatalog'

export default function NotFoundPage() {
  useDocumentMeta(NOT_FOUND_META)
  const navigate = useNavigate()

  return (
    <PageLayout active="home">
      <div className="flex flex-1 flex-col items-center justify-center gap-4 px-5 py-12 text-center">
        <div className="overline">Erro 404</div>
        <div className="headline-large">Página não encontrada</div>
        <div className="body-large max-w-[380px]">O endereço que você acessou não existe ou foi movido.</div>
        <button onClick={() => navigate('/')} className="mt-1 flex h-11 items-center gap-2 rounded-[var(--radius-button)] px-5 text-white" style={{ background: 'var(--accent)' }}>
          <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
            speed
          </span>
          <span className="label-large" style={{ color: '#fff' }}>
            Ir para o teste de velocidade
          </span>
        </button>
      </div>
    </PageLayout>
  )
}
