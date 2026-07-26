import type { ProblemPhase } from '../../hooks/useSpeedTest'

interface ProblemInfo {
  icon: string
  title: string
  message: string
  actionIcon: string
  actionLabel: string
  colorVar: string
}

const PROBLEM_MAP: Record<ProblemPhase, ProblemInfo> = {
  'bloqueado-outra-aba': {
    icon: 'tab',
    title: 'Teste em andamento em outra aba',
    message: 'Evitamos rodar duas medições ao mesmo tempo no mesmo navegador. Você pode iniciar mesmo assim, se preferir.',
    actionIcon: 'play_arrow',
    actionLabel: 'Iniciar mesmo assim',
    colorVar: 'var(--text-secondary)',
  },
  cancelado: {
    icon: 'cancel',
    title: 'Teste cancelado',
    message: 'Você cancelou a medição antes do fim.',
    actionIcon: 'refresh',
    actionLabel: 'Tentar novamente',
    colorVar: 'var(--text-secondary)',
  },
  'sem-conexao': {
    icon: 'wifi_off',
    title: 'Sem conexão',
    message: 'Não conseguimos detectar uma conexão com a internet neste aparelho.',
    actionIcon: 'refresh',
    actionLabel: 'Tentar novamente',
    colorVar: 'var(--error)',
  },
  'conexao-interrompida': {
    icon: 'sync_problem',
    title: 'Conexão interrompida',
    message: 'A conexão caiu no meio da medição e não foi possível concluir com confiança.',
    actionIcon: 'refresh',
    actionLabel: 'Tentar novamente',
    colorVar: 'var(--error)',
  },
  'endpoint-indisponivel': {
    icon: 'cloud_off',
    title: 'Servidor indisponível',
    message: 'Não foi possível contatar o servidor de medição agora. Tente novamente em instantes.',
    actionIcon: 'refresh',
    actionLabel: 'Tentar novamente',
    colorVar: 'var(--error)',
  },
  'erro-inesperado': {
    icon: 'error',
    title: 'Erro inesperado',
    message: 'Algo deu errado durante a medição.',
    actionIcon: 'refresh',
    actionLabel: 'Tentar novamente',
    colorVar: 'var(--error)',
  },
}

interface ProblemPanelProps {
  phase: ProblemPhase
  onAction: () => void
}

export function ProblemPanel({ phase, onAction }: ProblemPanelProps) {
  const problem = PROBLEM_MAP[phase] ?? PROBLEM_MAP['erro-inesperado']
  return (
    <div className="flex max-w-[420px] flex-col items-center gap-3.5 px-2 py-16 text-center">
      <span className="material-symbols-outlined" style={{ fontSize: 44, color: problem.colorVar }}>
        {problem.icon}
      </span>
      <div className="headline-small">{problem.title}</div>
      <div className="body-large" style={{ color: 'var(--text-secondary)', textWrap: 'pretty' }}>
        {problem.message}
      </div>
      <button
        onClick={onAction}
        className="flex h-11 items-center gap-2 rounded-[var(--radius-button)] px-5"
        style={{ background: 'var(--accent)', color: 'var(--on-accent)' }}
      >
        <span className="material-symbols-outlined" style={{ fontSize: 20 }}>
          {problem.actionIcon}
        </span>
        <span className="label-large" style={{ color: 'var(--on-accent)' }}>
          {problem.actionLabel}
        </span>
      </button>
    </div>
  )
}
