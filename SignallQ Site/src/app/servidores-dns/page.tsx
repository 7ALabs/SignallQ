"use client";
import { useRouter } from 'next/navigation'
import { EstadoVazio } from '@/components/EstadoVazio'
import { PageShell } from '@/components/PageShell'
import { useDocumentMeta } from '@/hooks/useDocumentMeta'
import { PAGE_META } from '@/lib/pageMetaCatalog'

// Placeholder — chip "Servidores DNS" da Home (`DIAG_ITEMS`) linka pra cá.
export default function ServidoresDnsPage() {
  useDocumentMeta(PAGE_META['/servidores-dns'])
  const router = useRouter()

  return (
    <PageShell align="center">
      <EstadoVazio
        icon="dns"
        badge="Em breve"
        title="Servidores DNS"
        message="Comparação dedicada de servidores DNS está em construção."
        actionIcon="speed"
        actionLabel="Ir para o teste de velocidade"
        onAction={() => router.push('/')}
      />
    </PageShell>
  )
}
