"use client";
import { useRouter } from 'next/navigation'
import { EstadoVazio } from '@/components/EstadoVazio'
import { PageShell } from '@/components/PageShell'
import { useDocumentMeta } from '@/hooks/useDocumentMeta'
import { PAGE_META } from '@/lib/pageMetaCatalog'

// Placeholder — chip "Latência sob carga" da Home (`DIAG_ITEMS`) linka pra cá.
export default function LatenciaSobCargaPage() {
  useDocumentMeta(PAGE_META['/latencia-sob-carga'])
  const router = useRouter()

  return (
    <PageShell align="center">
      <EstadoVazio
        icon="hourglass_bottom"
        badge="Em breve"
        title="Latência sob carga"
        message="Medição dedicada de latência sob carga (bufferbloat) está em construção."
        actionIcon="speed"
        actionLabel="Ir para o teste de velocidade"
        onAction={() => router.push('/')}
      />
    </PageShell>
  )
}
