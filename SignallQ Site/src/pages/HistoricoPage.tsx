import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AdBanner } from '../components/AdBanner'
import { DetailTopBar } from '../components/AppTopBar'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { HistoryRecordCard } from '../components/historico/HistoryRecordCard'
import { SegmentedControl } from '../components/SegmentedControl'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { clearAll, deleteRecord, listRecords, type MedicaoRegistro } from '../lib/historyStore'

type Status = 'loading' | 'loaded' | 'unavailable'
type Filtro = 'todos' | 'wifi' | 'celular'

const FILTROS: Array<{ value: Filtro; label: string }> = [
  { value: 'todos', label: 'Todos' },
  { value: 'wifi', label: 'Wi-Fi' },
  { value: 'celular', label: 'Rede móvel' },
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
  const text = `Meu histórico SignallQ: ${records.length} medição${records.length === 1 ? '' : 'ões'} · última em ${new Date(ultimo.timestamp).toLocaleString('pt-BR')} — Download ${ultimo.download.toFixed(1)} Mbps.`
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

// Tela 3 "Histórico" do protótipo "SignallQ WebApp.dc.html" do Luiz
// (GH#1186) — chrome mínimo (DetailTopBar), chips de filtro por tipo de
// conexão e lista de cards (substitui a tabela + gráfico anteriores; o
// gráfico de tendência não faz parte deste protótipo, ver nota no PR).
// "Limpar histórico" e exclusão individual foram mantidos mesmo não
// aparecendo no protótipo — são gestão de dado local sensível (privacidade),
// não decoração; cortar silenciosamente seria regressão, não simplificação.
export default function HistoricoPage() {
  useDocumentMeta({
    title: 'Histórico de medições — SignallQ',
    description: 'Veja o histórico local das suas medições de velocidade. Armazenado somente neste navegador.',
    path: '/historico',
  })
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
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <DetailTopBar
        title="Histórico"
        onBack={() => navigate(-1)}
        rightIcon="ios_share"
        rightLabel="Compartilhar histórico"
        onRightClick={() => shareHistorySummary(records)}
      />

      <div className="mx-auto flex w-full max-w-[560px] flex-1 flex-col gap-4 px-5 pb-6 pt-2 box-border">
        <div className="overline">Medições recentes</div>

        {status === 'loading' && (
          <div className="flex flex-col items-center gap-3 py-16">
            <span className="material-symbols-outlined" style={{ fontSize: 28, color: 'var(--text-tertiary)' }}>
              hourglass_top
            </span>
            <div className="body-large">Carregando histórico…</div>
          </div>
        )}

        {status === 'unavailable' && (
          <div className="flex flex-col items-center gap-2.5 rounded-2xl p-6 text-center" style={{ background: 'var(--bg-card)' }}>
            <span className="material-symbols-outlined" style={{ fontSize: 32, color: 'var(--error)' }}>
              storage
            </span>
            <div className="headline-small">Histórico indisponível</div>
            <div className="body-medium max-w-[360px]">Não foi possível ler o armazenamento local deste navegador agora.</div>
            <button onClick={load} className="mt-1 h-10 rounded-[var(--radius-button)] border px-4 label-large" style={{ borderColor: 'var(--border)' }}>
              Tentar novamente
            </button>
          </div>
        )}

        {isEmpty && (
          <div className="flex flex-col items-center gap-3 py-14 text-center">
            <span className="material-symbols-outlined" style={{ fontSize: 36, color: 'var(--text-tertiary)' }}>
              speed
            </span>
            <div className="headline-small">Nenhuma medição ainda</div>
            <div className="body-medium max-w-[320px]">Faça seu primeiro teste de velocidade para ver o histórico aqui.</div>
            <button
              onClick={() => navigate('/')}
              className="mt-1 flex h-11 items-center gap-2 rounded-[var(--radius-button)] px-5 text-white"
              style={{ background: 'var(--accent)' }}
            >
              <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
                speed
              </span>
              <span className="label-large" style={{ color: '#fff' }}>
                Testar velocidade
              </span>
            </button>
          </div>
        )}

        {hasRecords && (
          <div className="flex flex-col gap-4">
            <SegmentedControl options={FILTROS} value={filtro} onChange={setFiltro} />

            <div className="flex flex-col gap-2.5">
              {filtered.map((r) => (
                <HistoryRecordCard key={r.id} record={r} onShare={shareRecord} onRemove={remove} />
              ))}
              {filtered.length === 0 && (
                <div className="body-small py-6 text-center" style={{ color: 'var(--text-tertiary)' }}>
                  Nenhuma medição neste filtro.
                </div>
              )}
            </div>

            <div className="flex items-center justify-between">
              <div className="body-small" style={{ color: 'var(--text-tertiary)' }}>
                {records.length} medição{records.length === 1 ? '' : 'ões'} salva{records.length === 1 ? '' : 's'}
              </div>
              <button onClick={() => setConfirmOpen(true)} className="flex h-9 items-center gap-1.5 border-none bg-transparent">
                <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--accent)' }}>
                  delete_sweep
                </span>
                <span className="label-medium" style={{ color: 'var(--accent)' }}>
                  Limpar histórico
                </span>
              </button>
            </div>
          </div>
        )}

        {justDeleted && <div className="label-medium">Medição excluída.</div>}
      </div>

      <AdBanner />

      {confirmOpen && (
        <ConfirmDialog
          icon="delete_sweep"
          title="Limpar todo o histórico?"
          description="Essa ação remove permanentemente todas as medições salvas neste navegador. Não é possível desfazer."
          confirmLabel="Limpar tudo"
          cancelLabel="Cancelar"
          danger
          onConfirm={handleClearAll}
          onCancel={() => setConfirmOpen(false)}
        />
      )}
    </div>
  )
}
