// Motor de recomendações pós-resultado — 100% regras determinísticas sobre os
// números do teste + histórico local. Nenhuma chamada de IA em nenhuma
// hipótese (regra do squad: zero IA no Site, ver issue #1184).
//
// Adaptado do motor equivalente do repo antecessor `linka-speedtest`
// (src/utils/recommendations.ts) para os tipos reais do Site
// (lib/classification.ts / lib/historyStore.ts) — sem "Prova Real"/modo
// prova, feature que não existe aqui.
import { classifyDownload, classifyJitter, classifyLatency, classifyUpload } from './classification'
import type { MedicaoRegistro } from './historyStore'
import type { SpeedTestResult } from './speedEngine'

export type RecommendationPriority = 'high' | 'medium' | 'low'
// 'repeat_test' reaproveita o onRetry já existente no ResultPanel — as demais
// recomendações são só informativas (sem fluxo próprio no Site).
export type RecommendationAction = 'repeat_test' | 'none'

export interface Recommendation {
  id: string
  icon: string
  title: string
  description: string
  priority: RecommendationPriority
  actionType: RecommendationAction
}

function rec(
  id: string,
  icon: string,
  title: string,
  description: string,
  priority: RecommendationPriority,
  actionType: RecommendationAction = 'none',
): Recommendation {
  return { id, icon, title, description, priority, actionType }
}

function hasRecurringProblem(history: MedicaoRegistro[], check: (r: MedicaoRegistro) => boolean, threshold = 3): boolean {
  return history.slice(0, 5).filter(check).length >= threshold
}

const PRIORITY_ORDER: Record<RecommendationPriority, number> = { high: 0, medium: 1, low: 2 }

export function buildRecommendations(result: SpeedTestResult, recentHistory: MedicaoRegistro[] = []): Recommendation[] {
  const download = classifyDownload(result.download.mbps)
  const upload = classifyUpload(result.upload.mbps)
  const latency = classifyLatency(result.latency.ms)
  const jitter = result.jitter ? classifyJitter(result.jitter.ms) : null

  if (download.nivel === 'indisponivel') {
    return [
      rec(
        'check_conn',
        'wifi_off',
        'Verifique sua conexão',
        'Não foi possível medir sua velocidade agora. Confirme se o Wi-Fi ou cabo está ativo e teste novamente.',
        'high',
        'repeat_test',
      ),
    ]
  }

  // Tudo boa — nenhum card, sem forçar recomendação onde não há problema.
  if (download.nivel === 'success' && upload.nivel === 'success' && latency.nivel === 'success') {
    return []
  }

  const recs: Recommendation[] = []
  const isDownloadLow = download.nivel !== 'success'
  const isUploadLow = upload.nivel === 'error'
  const isHighLatency = latency.nivel === 'error'
  const isUnstable = jitter != null && jitter.nivel !== 'success'

  if (isUnstable) {
    const recurring = hasRecurringProblem(recentHistory, (r) => r.jitter != null && r.jitter >= 30)
    if (recurring) {
      recs.push(
        rec(
          'restart_router',
          'restart_alt',
          'Reinicie o roteador',
          'A oscilação apareceu em vários testes recentes. Desligue o roteador por 30 segundos e ligue novamente.',
          'medium',
        ),
      )
    } else {
      recs.push(
        rec(
          'compare_loc',
          'compare_arrows',
          'Teste em outro cômodo',
          'A conexão está oscilando. Teste perto do roteador para ver se o resultado melhora.',
          'medium',
          'repeat_test',
        ),
      )
    }
  }

  if (isHighLatency) {
    recs.push(
      rec(
        'move_router',
        'wifi_tethering',
        'Fique mais perto do roteador',
        'A resposta está lenta — isso afeta jogos e chamadas de vídeo. Aproxime-se do roteador e teste novamente.',
        'high',
        'repeat_test',
      ),
    )
  }

  if (isDownloadLow) {
    const recurring = hasRecurringProblem(recentHistory, (r) => r.download < 25)
    if (recurring) {
      recs.push(
        rec(
          'contact_op',
          'support_agent',
          'Fale com a operadora',
          'A lentidão apareceu em vários testes recentes. Vale abrir um chamado ou revisar seu plano.',
          'high',
        ),
      )
    } else {
      recs.push(
        rec(
          'close_apps',
          'close_fullscreen',
          'Feche outros apps que usam internet',
          'O download está abaixo do esperado. Feche downloads e streamings em segundo plano e teste de novo.',
          'medium',
          'repeat_test',
        ),
      )
    }
  }

  if (isUploadLow) {
    recs.push(
      rec(
        'upload_warn',
        'upload',
        'Upload fraco pode afetar chamadas',
        'O envio de dados está baixo — videochamadas e envio de arquivos podem travar. Prefira cabo, se possível.',
        'medium',
      ),
    )
  }

  return recs.sort((a, b) => PRIORITY_ORDER[a.priority] - PRIORITY_ORDER[b.priority]).slice(0, 3)
}
