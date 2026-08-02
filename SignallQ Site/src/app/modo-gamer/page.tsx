"use client";
import { useRouter } from 'next/navigation'
import { EstadoVazio } from '@/components/EstadoVazio'
import { PageShell } from '@/components/PageShell'
import { useDocumentMeta } from '@/hooks/useDocumentMeta'
import { PAGE_META } from '@/lib/pageMetaCatalog'

// Placeholder — chip "Modo gamer" da Home (`DIAG_ITEMS`) linka pra cá. Modo
// gamer por jogo já existe no app Android (teste fechado, `/app`), então a
// ação leva pra lá em vez de voltar pro teste web.
export default function ModoGamerPage() {
  useDocumentMeta(PAGE_META['/modo-gamer'])
  const router = useRouter()

  return (
    <PageShell align="center">
      <EstadoVazio
        icon="sports_esports"
        badge="Em breve"
        title="Modo gamer"
        message="Diagnóstico dedicado de modo gamer por jogo, no navegador, está em construção. Hoje ele já existe no app Android, em teste fechado."
        actionIcon="android"
        actionLabel="Conhecer o app"
        onAction={() => router.push('/app')}
      />
    </PageShell>
  )
}
