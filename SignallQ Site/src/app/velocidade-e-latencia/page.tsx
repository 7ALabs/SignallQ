"use client";
import { useRouter } from 'next/navigation'
import { EstadoVazio } from '@/components/EstadoVazio'
import { PageShell } from '@/components/PageShell'
import { useDocumentMeta } from '@/hooks/useDocumentMeta'
import { PAGE_META } from '@/lib/pageMetaCatalog'

// Placeholder — chip "Velocidade e latência" da Home (`DIAG_ITEMS`) linka pra
// cá. Diagnóstico dedicado ainda não existe; hoje o teste da Home já mede
// velocidade e latência juntos, então a ação leva de volta pra lá.
export default function VelocidadeELatenciaPage() {
  useDocumentMeta(PAGE_META['/velocidade-e-latencia'])
  const router = useRouter()

  return (
    <PageShell align="center">
      <EstadoVazio
        icon="speed"
        badge="Em breve"
        title="Velocidade e latência"
        message="Um diagnóstico dedicado de velocidade e latência está em construção. Por enquanto, use o teste completo na tela inicial — ele já mede as duas coisas."
        actionIcon="speed"
        actionLabel="Ir para o teste de velocidade"
        onAction={() => router.push('/')}
      />
    </PageShell>
  )
}
