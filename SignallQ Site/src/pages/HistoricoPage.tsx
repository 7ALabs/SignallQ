import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { DetailTopBar } from '../components/AppTopBar'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { EmptyState } from '../components/EmptyState'
import { HistoryEvolutionChart } from '../components/historico/HistoryEvolutionChart'
import { HistoryRecordCard } from '../components/historico/HistoryRecordCard'
import { PageLayout } from '../components/PageLayout'
import { SegmentedControl } from '../components/SegmentedControl'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { clearAll, deleteRecord, listRecords, type MedicaoRegistro } from '../lib/historyStore'
import { PAGE_META } from '../lib/pageMetaCatalog'

type Status = 'loading' | 'loaded' | 'unavailable'
type Filtro = 'todos' | 'wifi' | 'celular' | 'ethernet'

const FILTROS: Array<{ value: Filtro; label: string }> = [
  { value: 'todos', label: 'Todos' },
  { value: 'wifi', label: 'Wi-Fi' },
  { value: 'celular', label: 'Rede móvel' },
  { value: 'ethernet', label: 'Ethernet' },
]

async function shareRecord(record: MedicaoRegistro) {
  const text = `Meu teste de velocidade SignallQ (${new Date(record.timestamp).toLocaleString('pt-BR')}): Download ${record.download.toFixed(1)} Mbps · Upload ${record.upload.toFixed(1)} Mbps · Latência ${Math.round(record.latency)} ms.`
  if (navigator.share) {
    try {
      await navigator.share({ title: 'Meu teste de velocidade SignallQ', text })
      return
    } catch {
      // cancelado — cai no fallback de cópia
    }
  }
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    window.prompt('Copie o resumo:', text)
  }
}

// Ícone de compartilhar do topo (ios_share) compartilha um resumo do
// histórico inteiro, não de uma medição — comportamento não especificado
// linha a linha no handoff (o protótipo só mostra o ícone), decisão minha,
// sinalizada no resumo da entrega.
async function shareHistorySummary(records: MedicaoRegistro[]) {
  if (records.length === 0) return
  const ultimo = records[0]
  const text = `Meu histórico SignallQ: ${records.length} ${records.length === 1 ? 'medição' : 'medições'} · última em ${new Date(ultimo.timestamp).toLocaleString('pt-BR')} — Download ${ultimo.download.toFixed(1)} Mbps.`
  if (navigator.share) {
    try {
      await navigator.share({ title: 'Meu histórico SignallQ', text })
      return
    } catch {
      // cancelado — cai no fallback de cópia
    }
  }
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    window.prompt('Copie o resumo:', text)
  }
}

// Tela "Histórico" reconstruída 1:1 a partir do protótipo v2
// (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenHistorico.dc.html`,
// Fase 2 da reconstrução do Site, issue-mãe #1402). Desktop: SiteNav + AdRail
// (esquerda/direita) + conteúdo, AdBannerWide embaixo, SiteFooter — mesma
// composição dos outros 3 espaços de anúncio universais (ver README do
// pacote de design-specs). Mobile: topo mínimo (DetailTopBar), sem AdRail —
// mesma regra de breakpoint `lg:` já usada em SiteNav/AdRail/AdBannerWide.
export default function HistoricoPage() {
  // noindex: conteúdo vem do IndexedDB do visitante, pro Google é sempre página
  // vazia (issue #1369) — sai do sitemap.xml também, ver public/sitemap.xml.
  useDocumentMeta(PAGE_META['/historico'])
  const navigate = useNavigate()

  const [status, setStatus] = useState<Status>('loading')
  const [records, setRecords] = useState<MedicaoRegistro[]>([])
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [justDeleted, setJustDeleted] = useState(false)
  const [filtro, setFiltro] = useState<Filtro>('todos')

  const load = async () => {
    setStatus('loading')
    try {
      const r = await listRecords()
      setRecords(r)
      setStatus('loaded')
    } catch {
      setStatus('unavailable')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const remove = async (id: string) => {
    await deleteRecord(id)
    setRecords((prev) => prev.filter((r) => r.id !== id))
    setJustDeleted(true)
    setTimeout(() => setJustDeleted(false), 2500)
  }

  const handleClearAll = async () => {
    await clearAll()
    setRecords([])
    setConfirmOpen(false)
  }

  const isEmpty = status === 'loaded' && records.length === 0
  const hasRecords = status === 'loaded' && records.length > 0
  const filtered = records.filter((r) => filtro === 'todos' || r.connectionKind === filtro)

  return (
    <>
      <PageLayout
        active="historico"
        adBannerVariant="b"
        contentClassName="flex w-full flex-1 items-start justify-center gap-6 box-border px-5 pt-2 pb-8 lg:px-6 lg:pt-5 lg:pb-4"
        mobileTopBar={
          <DetailTopBar
            title="Histórico"
            onBack={() => navigate(-1)}
            rightIcon="ios_share"
            rightLabel="Compartilhar histórico"
            onRightClick={() => shareHistorySummary(records)}
          />
        }
      >
        <div className="flex w-full max-w-[860px] flex-1 flex-col gap-4">
          <div className="hidden items-baseline justify-between gap-3 lg:flex">
            <h1 className="headline-large m-0">Histórico</h1>
            <span className="body-small" style={{ color: 'var(--text-tertiary)' }}>
              Salvo só neste navegador
            </span>
          </div>

          {status === 'loading' && (
            <EmptyState
              className="flex flex-col items-center gap-3 py-24"
              icon="hourglass_top"
              iconSize={28}
              message="Carregando histórico…"
              messageClassName="body-large"
            />
          )}

          {status === 'unavailable' && (
            <EmptyState
              className="flex flex-col items-center gap-2.5 rounded-2xl text-center"
              style={{ background: 'var(--bg-secondary)', padding: '40px 24px' }}
              icon="storage"
              iconColor="var(--error)"
              iconSize={32}
              title="Histórico indisponível"
              message="Não foi possível ler o armazenamento local deste navegador agora."
              messageClassName="max-w-[360px]"
              messageStyle={{ font: '400 14px/1.45 var(--font-sans)', color: 'var(--text-secondary)' }}
              action={
                <button onClick={load} className="mt-1 h-10 rounded-[var(--radius-button)] border px-4 label-large" style={{ borderColor: 'var(--border)' }}>
                  Tentar novamente
                </button>
              }
            />
          )}

          {isEmpty && (
            <EmptyState
              className="flex flex-col items-center gap-3 py-20 text-center"
              icon="speed"
              iconSize={36}
              title="Nenhuma medição ainda"
              message="Faça seu primeiro teste para ver o histórico aqui."
              messageClassName="max-w-[320px]"
              messageStyle={{ font: '400 14px/1.45 var(--font-sans)', color: 'var(--text-secondary)' }}
              action={
                <button
                  onClick={() => navigate('/')}
                  className="mt-1 flex h-11 items-center gap-2 rounded-[var(--radius-button)] px-5"
                  style={{ background: 'var(--accent)', color: 'var(--on-accent)' }}
                >
                  <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
                    speed
                  </span>
                  <span className="label-large" style={{ color: 'var(--on-accent)' }}>
                    Testar velocidade
                  </span>
                </button>
              }
            />
          )}

          {hasRecords && (
            <div className="flex flex-col gap-3.5">
              <div className="flex items-center justify-between gap-3">
                <SegmentedControl options={FILTROS} value={filtro} onChange={setFiltro} />
                <button onClick={() => setConfirmOpen(true)} className="flex h-9 shrink-0 items-center gap-1.5 border-none bg-transparent">
                  <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--accent)' }}>
                    delete_sweep
                  </span>
                  <span className="label-medium" style={{ color: 'var(--accent)' }}>
                    Limpar histórico
                  </span>
                </button>
              </div>

              <HistoryEvolutionChart records={records} />

              <div className="grid grid-cols-1 gap-2.5 lg:grid-cols-2">
                {filtered.map((r) => (
                  <HistoryRecordCard key={r.id} record={r} onShare={shareRecord} onRemove={remove} />
                ))}
                {filtered.length === 0 && (
                  <div className="body-small col-span-full py-6 text-center" style={{ color: 'var(--text-tertiary)' }}>
                    Nenhuma medição neste filtro.
                  </div>
                )}
              </div>

              <div className="body-small" style={{ color: 'var(--text-tertiary)' }}>
                {/* Bug de pluralização pré-existente corrigido nesta reconstrução: "medição"
                    + "ões" gerava "mediçãoões" em vez de "medições" (o plural troca "ção"
                    por "ções", não concatena sufixo). */}
                {records.length} {records.length === 1 ? 'medição salva' : 'medições salvas'}
              </div>
            </div>
          )}

          {justDeleted && <div className="label-medium">Medição excluída.</div>}
        </div>
      </PageLayout>

      {confirmOpen && (
        <ConfirmDialog
          icon="delete_sweep"
          title="Limpar todo o histórico?"
          description="Remove todas as medições salvas neste navegador. Não é possível desfazer."
          confirmLabel="Limpar tudo"
          cancelLabel="Cancelar"
          danger
          onConfirm={handleClearAll}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </>
  )
}
