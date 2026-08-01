"use client";
import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { EstadoVazio } from '../../components/EstadoVazio'
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

export default function Page() {
  useDocumentMeta(PAGE_META['/historico'])
  const router = useRouter()
  const navigate = (p: string) => router.push(p)

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
      <div className="flex flex-wrap items-baseline justify-between gap-3 w-full">
        <h1 className="m-0 font-bold text-[26px] leading-[1.23] text-[color:var(--text-primary)]">Histórico</h1>
        <span className="font-normal text-[12px] leading-[1.33] text-[color:var(--text-tertiary)]">
          Salvo só neste navegador
        </span>
      </div>

      {/* Título fica sempre fixo no topo (Guia §7.2) — só o bloco de estado
          (carregando/indisponível/vazio) centraliza no espaço vertical que
          sobra abaixo dele, nunca a página inteira como uma unidade só. */}
      {status === 'loading' && (
        <div className="flex w-full flex-1 flex-col items-center justify-center gap-3">
          <span className="material-symbols-outlined text-[28px] text-[color:var(--text-tertiary)]">
            hourglass_top
          </span>
          <div className="font-normal text-[16px] leading-[1.5] text-[color:var(--text-primary)]">Carregando histórico…</div>
        </div>
      )}

      {status === 'unavailable' && (
        <div className="flex w-full flex-1 items-center justify-center">
          <EstadoVazio
            card
            icon="storage"
            iconSize={32}
            messageSize={14}
            color="var(--error)"
            title="Histórico indisponível"
            message="Não foi possível ler o armazenamento local deste navegador agora."
            actionLabel="Tentar novamente"
            actionVariant="outline"
            onAction={load}
          />
        </div>
      )}

      {isEmpty && (
        <div className="flex w-full flex-1 items-center justify-center">
          <EstadoVazio
            icon="speed"
            iconSize={36}
            messageSize={14}
            color="var(--text-tertiary)"
            title="Nenhuma medição ainda"
            message="Faça seu primeiro teste para ver o histórico aqui."
            actionIcon="speed"
            actionLabel="Testar velocidade"
            onAction={() => navigate('/')}
          />
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

          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex flex-wrap border border-[color:var(--border)] rounded-full p-[2px]">
              {FILTROS.map(f => (
                <div
                  key={f.value}
                  onClick={() => setFiltro(f.value)}
                  className={`rounded-full py-[6px] px-4 font-medium text-[12px] leading-[1.33] whitespace-nowrap cursor-pointer transition-colors ${
                    filtro === f.value ? "bg-[color:var(--accent)] text-[color:var(--on-accent)]" : "text-[color:var(--text-primary)] hover:bg-[color:var(--bg-secondary)]"
                  }`}
                >
                  {f.label}
                </div>
              ))}
            </div>
            <button onClick={() => setConfirmOpen(true)} className="flex items-center gap-[6px] whitespace-nowrap bg-transparent border-none cursor-pointer">
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
