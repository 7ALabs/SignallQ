"use client";
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { HistoryEvolutionChart } from '../../components/historico/HistoryEvolutionChart'
import { HistoryRecordCard } from '../../components/historico/HistoryRecordCard'
import { PageShell } from '../../components/PageShell'
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { clearAll, deleteRecord, listRecords, type MedicaoRegistro } from '../../lib/historyStore'
import { PAGE_META } from '../../lib/pageMetaCatalog'

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
      // cancelado
    }
  }
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    window.prompt('Copie o resumo:', text)
  }
}

async function shareHistorySummary(records: MedicaoRegistro[]) {
  if (records.length === 0) return
  const ultimo = records[0]
  const text = `Meu histórico SignallQ: ${records.length} ${records.length === 1 ? 'medição' : 'medições'} · última em ${new Date(ultimo.timestamp).toLocaleString('pt-BR')} — Download ${ultimo.download.toFixed(1)} Mbps.`
  if (navigator.share) {
    try {
      await navigator.share({ title: 'Meu histórico SignallQ', text })
      return
    } catch {
      // cancelado
    }
  }
  try {
    await navigator.clipboard.writeText(text)
  } catch {
    window.prompt('Copie o resumo:', text)
  }
}

export default function Page() {
  useDocumentMeta(PAGE_META['/historico'])
  const router = useRouter(); const navigate = (p: string) => router.push(p)

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
    <PageShell>
      <div className="md:hidden w-full flex items-center justify-between p-[14px_8px] box-border">
        <div className="w-[40px] h-[40px] flex items-center justify-center cursor-pointer" onClick={() => router.back()}>
          <span className="material-symbols-outlined text-[24px]">arrow_back</span>
        </div>
        <div className="flex-1 text-center font-medium text-[16px] leading-[1.38] text-[color:var(--text-primary)]">
          Histórico
        </div>
        <div className="w-[40px] h-[40px] flex items-center justify-center cursor-pointer" onClick={() => shareHistorySummary(records)}>
          <span className="material-symbols-outlined text-[24px]">ios_share</span>
        </div>
      </div>

      <div className="hidden md:flex items-baseline justify-between gap-3 w-full">
        <h1 className="m-0 font-bold text-[26px] leading-[1.23] text-[color:var(--text-primary)]">Histórico</h1>
        <span className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
          Salvo só neste navegador
        </span>
      </div>

      {status === 'loading' && (
        <div className="flex flex-col items-center gap-3 py-[96px] w-full">
          <span className="material-symbols-outlined text-[28px] text-[color:var(--text-tertiary)]">
            hourglass_top
          </span>
          <div className="font-normal text-[16px] leading-[1.5] text-[color:var(--text-primary)]">Carregando histórico…</div>
        </div>
      )}

      {status === 'unavailable' && (
        <div className="flex flex-col items-center gap-[10px] rounded-[16px] py-[40px] px-[24px] text-center bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.16)] w-full">
          <span className="material-symbols-outlined text-[32px] text-[color:var(--error)]">
            storage
          </span>
          <div className="font-semibold text-[22px] leading-[1.27] text-[color:var(--text-primary)]">Histórico indisponível</div>
          <div className="max-w-[360px] font-normal text-[14px] leading-[1.45] text-[color:var(--text-secondary)]">
            Não foi possível ler o armazenamento local deste navegador agora.
          </div>
          <button onClick={load} className="mt-1 h-[40px] flex items-center px-4 border border-[color:var(--border)] rounded-[var(--radius-button)] font-medium text-[14px] leading-[1.43] text-[color:var(--text-primary)] bg-transparent cursor-pointer">
            Tentar novamente
          </button>
        </div>
      )}

      {isEmpty && (
        <div className="flex flex-col items-center gap-3 py-[80px] text-center w-full">
          <span className="material-symbols-outlined text-[36px] text-[color:var(--text-tertiary)]">
            speed
          </span>
          <div className="font-semibold text-[22px] leading-[1.27] text-[color:var(--text-primary)]">Nenhuma medição ainda</div>
          <div className="max-w-[320px] font-normal text-[14px] leading-[1.45] text-[color:var(--text-secondary)]">
            Faça seu primeiro teste para ver o histórico aqui.
          </div>
          <button
            onClick={() => navigate('/')}
            className="mt-1 h-[44px] flex items-center gap-2 rounded-[var(--radius-button)] px-5 bg-[color:var(--accent)] cursor-pointer"
          >
            <span className="material-symbols-outlined text-[20px] text-[color:var(--on-accent)]">
              speed
            </span>
            <span className="font-medium text-[14px] leading-[1.43] text-[color:var(--on-accent)]">
              Testar velocidade
            </span>
          </button>
        </div>
      )}

      {hasRecords && (
        <div className="flex flex-col gap-[14px] w-full">
          <div className="flex items-start gap-3 rounded-[16px] p-4 bg-[color-mix(in_srgb,_var(--accent)_15%,_transparent)]">
            <span className="material-symbols-outlined text-[20px] text-[color:var(--accent)] mt-0.5">lightbulb</span>
            <div className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-secondary)]">
              <b className="text-[color:var(--text-primary)]">Dica de Diagnóstico:</b> Compare a sua conexão fazendo um teste perto do roteador e outro no cômodo onde a internet fica lenta. A diferença mostra o quanto você perde no Wi-Fi.
            </div>
          </div>
          
          <div className="flex items-center justify-between gap-3">
            <div className="flex border border-[color:var(--border)] rounded-full p-[2px]">
              {FILTROS.map(f => (
                <div
                  key={f.value}
                  onClick={() => setFiltro(f.value)}
                  className={`rounded-full py-[6px] px-4 font-medium text-[12px] leading-[1.33] cursor-pointer transition-colors ${
                    filtro === f.value ? "bg-[color:var(--accent)] text-[color:var(--on-accent)]" : "text-[color:var(--text-primary)] hover:bg-[color:var(--bg-secondary)]"
                  }`}
                >
                  {f.label}
                </div>
              ))}
            </div>
            <button onClick={() => setConfirmOpen(true)} className="flex items-center gap-[6px] bg-transparent border-none cursor-pointer">
              <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
                delete_sweep
              </span>
              <span className="font-medium text-[12px] leading-[1.33] text-[color:var(--accent)] hover:underline">
                Limpar histórico
              </span>
            </button>
          </div>

          <HistoryEvolutionChart records={records} />

          <div className="grid grid-cols-1 gap-[10px] md:grid-cols-2">
            {filtered.map((r) => (
              <HistoryRecordCard key={r.id} record={r} onShare={shareRecord} onRemove={remove} />
            ))}
            {filtered.length === 0 && (
              <div className="col-span-full py-6 text-center font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
                Nenhuma medição neste filtro.
              </div>
            )}
          </div>

          <div className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
            {records.length} {records.length === 1 ? 'medição salva' : 'medições salvas'}
          </div>
        </div>
      )}

      {justDeleted && <div className="font-medium text-[14px] leading-[1.43] text-center mt-2">Medição excluída.</div>}

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
    </PageShell>
  )
}
