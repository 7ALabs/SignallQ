import { classifyDownload } from '../../lib/classification'
import { iconeConexao, labelConexao } from '../../lib/connection'
import { formatarTempoRelativo } from '../../lib/relativeTime'
import type { MedicaoRegistro } from '../../lib/historyStore'

const NIVEL_COR: Record<string, string> = { success: 'var(--success)', warning: 'var(--warning)', error: 'var(--error)', indisponivel: 'var(--text-tertiary)' }

interface HistoryRecordCardProps {
  record: MedicaoRegistro
  onShare: (record: MedicaoRegistro) => void
  onRemove: (id: string) => void
}

// Card por medição da Tela 3 "Histórico" (protótipo "SignallQ WebApp.dc.html"
// do Luiz, GH#1186) — substitui a tabela (HistoryTable.tsx) por um card por
// linha: topo com ícone/tipo de conexão + horário relativo, base com
// download/upload/latência inline.
export function HistoryRecordCard({ record, onShare, onRemove }: HistoryRecordCardProps) {
  const verdict = classifyDownload(record.download)

  return (
    <div className="flex flex-col gap-2.5 rounded-2xl p-3.5" style={{ background: 'var(--bg-secondary)' }}>
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <span className="material-symbols-outlined" style={{ fontSize: 16, color: 'var(--text-tertiary)' }}>
            {iconeConexao(record.connectionKind)}
          </span>
          <span className="label-medium">{labelConexao(record.connectionKind)}</span>
        </div>
        <span className="label-small" style={{ color: 'var(--text-tertiary)' }}>
          {formatarTempoRelativo(record.timestamp)}
        </span>
      </div>

      <div className="flex flex-wrap items-center gap-x-4 gap-y-1">
        <div className="flex items-center gap-1">
          <span className="material-symbols-outlined" style={{ fontSize: 14, color: 'var(--success)' }}>
            arrow_downward
          </span>
          <span className="label-large" style={{ color: NIVEL_COR[verdict.nivel] }}>
            {record.download.toFixed(1)} Mbps
          </span>
        </div>
        <div className="flex items-center gap-1">
          <span className="material-symbols-outlined" style={{ fontSize: 14, color: 'var(--accent)' }}>
            arrow_upward
          </span>
          <span className="label-large">{record.upload.toFixed(1)} Mbps</span>
        </div>
        <div className="body-small">{Math.round(record.latency)} ms</div>

        <div className="ml-auto flex gap-0.5">
          <button aria-label="Compartilhar medição" onClick={() => onShare(record)} className="flex h-8 w-8 items-center justify-center border-none bg-transparent">
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>
              share
            </span>
          </button>
          <button aria-label="Excluir medição" onClick={() => onRemove(record.id)} className="flex h-8 w-8 items-center justify-center border-none bg-transparent">
            <span className="material-symbols-outlined" style={{ fontSize: 16 }}>
              delete
            </span>
          </button>
        </div>
      </div>
    </div>
  )
}
