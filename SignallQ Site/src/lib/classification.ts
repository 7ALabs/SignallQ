// Regras de classificação do site — nenhum veredito é sorteado ou aproximado.
//
// Portadas 1:1 dos cortes reais em produção no app Android (reconciliação
// registrada na decisão de arquitetura do épico #1147..#1155), não da tabela
// provisória de 4 níveis (Excelente/Bom/Regular/Ruim) que o protótipo Claude
// Design tinha inventado sem fonte oficial:
// - Cor semântica das métricas cruas (3 níveis success/warning/error):
//   android/app/.../ui/screen/ResultadoVelocidadeScreen.kt:212-247
//   (download >=50/>=25, upload >=10/>=3, latência <20/<60, jitter <10/<30)
// - Veredito por caso de uso (good/acceptable/poor -> 'Boa'/'Aceitável'/'Ruim'):
//   android/feature/speedtest/.../SpeedtestQualityClassifier.kt
export type Nivel = 'success' | 'warning' | 'error' | 'indisponivel'

export interface Classificacao {
  label: string
  nivel: Nivel
}

function indisponivel(): Classificacao {
  return { label: 'Indisponível', nivel: 'indisponivel' }
}

export function classifyDownload(mbps: number | null | undefined): Classificacao {
  if (mbps == null || Number.isNaN(mbps)) return indisponivel()
  if (mbps >= 50) return { label: 'Boa', nivel: 'success' }
  if (mbps >= 25) return { label: 'Aceitável', nivel: 'warning' }
  return { label: 'Ruim', nivel: 'error' }
}

export function classifyUpload(mbps: number | null | undefined): Classificacao {
  if (mbps == null || Number.isNaN(mbps)) return indisponivel()
  if (mbps >= 10) return { label: 'Boa', nivel: 'success' }
  if (mbps >= 3) return { label: 'Aceitável', nivel: 'warning' }
  return { label: 'Ruim', nivel: 'error' }
}

// Latência/jitter: menor é melhor.
export function classifyLatency(ms: number | null | undefined): Classificacao {
  if (ms == null || Number.isNaN(ms)) return indisponivel()
  if (ms < 20) return { label: 'Boa', nivel: 'success' }
  if (ms < 60) return { label: 'Aceitável', nivel: 'warning' }
  return { label: 'Ruim', nivel: 'error' }
}

export function classifyJitter(ms: number | null | undefined): Classificacao {
  if (ms == null || Number.isNaN(ms)) return indisponivel()
  if (ms < 10) return { label: 'Boa', nivel: 'success' }
  if (ms < 30) return { label: 'Aceitável', nivel: 'warning' }
  return { label: 'Ruim', nivel: 'error' }
}

export interface ResultadoMedicao {
  download: number | null
  upload: number | null
  latency: number | null
  jitter: number | null
}

export interface UseCases {
  navegacao: Classificacao
  streaming: Classificacao
  videochamada: Classificacao
  jogosOnline: Classificacao
}

const PIOR_NIVEL: Record<Nivel, number> = { indisponivel: 0, error: 1, warning: 2, success: 3 }

function piorDeDois(a: Classificacao, b: Classificacao): Classificacao {
  return PIOR_NIVEL[a.nivel] <= PIOR_NIVEL[b.nivel] ? a : b
}

// Perda de pacotes não é medida pelo motor XHR do site (só download/upload/
// latência/jitter) — tratada como 0 (não penaliza) nas fórmulas abaixo, que
// espelham SpeedtestQualityClassifier.classificarQualidade(). Diferença
// documentada, não inventa threshold novo.
export function interpretUseCases({ download, upload, latency, jitter }: ResultadoMedicao): UseCases {
  const dl = download ?? 0
  const ul = upload ?? 0
  const lat = latency ?? Infinity
  const jit = jitter ?? 0

  const streaming: Classificacao =
    dl >= 25 && lat <= 200 && jit <= 50
      ? { label: 'Boa', nivel: 'success' }
      : dl >= 15 && lat <= 500 && jit <= 100
        ? { label: 'Aceitável', nivel: 'warning' }
        : { label: 'Ruim', nivel: 'error' }

  const jogosOnline: Classificacao =
    dl >= 10 && ul >= 3 && lat <= 50 && jit <= 15
      ? { label: 'Boa', nivel: 'success' }
      : dl >= 5 && ul >= 1 && lat <= 100 && jit <= 30
        ? { label: 'Aceitável', nivel: 'warning' }
        : { label: 'Ruim', nivel: 'error' }

  const videochamada: Classificacao =
    dl >= 10 && ul >= 3 && lat <= 80 && jit <= 30
      ? { label: 'Boa', nivel: 'success' }
      : dl >= 5 && ul >= 1 && lat <= 150 && jit <= 50
        ? { label: 'Aceitável', nivel: 'warning' }
        : { label: 'Ruim', nivel: 'error' }

  // Navegação não tem veredito próprio no motor Android — composição do pior
  // resultado entre download e latência, reaproveitando os cortes já oficiais
  // acima (sem threshold novo inventado).
  const navegacao = piorDeDois(classifyDownload(download), classifyLatency(latency))

  return { navegacao, streaming, videochamada, jogosOnline }
}
