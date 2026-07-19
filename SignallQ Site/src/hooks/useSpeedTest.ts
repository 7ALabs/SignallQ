import { useCallback, useEffect, useRef, useState } from 'react'
import { addRecord, resultToRecord } from '../lib/historyStore'
import { createSpeedTest, SpeedTestError, type SpeedTestPhase, type SpeedTestResult } from '../lib/speedEngine'
import { FEATURE_SPEEDTEST_COMPLETOU, FEATURE_SPEEDTEST_INICIADO, trackFeatureUsed } from '../lib/telemetry'

const LOCK_KEY = 'signallq_speedtest_lock_v1'
const LOCK_TTL_MS = 4000

// Estados de problema em PT-BR — mapeados explicitamente a partir do código
// de erro real do motor (SpeedTestError.code). O protótipo original tinha um
// bug aqui: usava o `err.code` em inglês como chave de estado, mas o mapa de
// mensagens só tinha chaves em português — todo erro de rede real caía no
// fallback genérico "Erro inesperado", perdendo a mensagem específica
// (sem-conexão, conexão interrompida, endpoint indisponível). Corrigido nesta
// versão com o mapeamento abaixo.
export type ProblemPhase = 'sem-conexao' | 'conexao-interrompida' | 'endpoint-indisponivel' | 'erro-inesperado' | 'cancelado' | 'bloqueado-outra-aba'

export type FasePainel = SpeedTestPhase | 'concluido' | 'parcial' | ProblemPhase

const CODE_TO_PROBLEM_PHASE: Record<string, ProblemPhase> = {
  'no-connection': 'sem-conexao',
  'connection-interrupted': 'conexao-interrompida',
  'endpoint-unavailable': 'endpoint-indisponivel',
  'unexpected-error': 'erro-inesperado',
  cancelled: 'cancelado',
}

export interface PhaseResults {
  latencia?: number
  download?: number
  upload?: number
}

export function useSpeedTest() {
  const [phase, setPhase] = useState<FasePainel>('preparando')
  const [liveValue, setLiveValue] = useState(0)
  const [phaseResults, setPhaseResults] = useState<PhaseResults>({})
  const [result, setResult] = useState<SpeedTestResult | null>(null)

  const engineRef = useRef<ReturnType<typeof createSpeedTest> | null>(null)
  const tabIdRef = useRef(Math.random().toString(36).slice(2))
  const heartbeatRef = useRef<ReturnType<typeof setInterval> | null>(null)
  // Espelham o state síncronamente para o callback onPhase do motor (precisa
  // ler a fase/valor "atuais" no exato instante da transição, antes do
  // próximo render — useState sozinho não garante isso dentro do mesmo tick).
  const phaseRef = useRef<FasePainel>('preparando')
  const liveValueRef = useRef(0)

  const acquireLock = useCallback(() => {
    try {
      localStorage.setItem(LOCK_KEY, JSON.stringify({ tabId: tabIdRef.current, ts: Date.now() }))
    } catch {
      // localStorage indisponível (modo privado restrito) — segue sem lock
    }
  }, [])

  const stopHeartbeat = useCallback(() => {
    if (heartbeatRef.current) {
      clearInterval(heartbeatRef.current)
      heartbeatRef.current = null
    }
  }, [])

  const releaseLock = useCallback(() => {
    stopHeartbeat()
    try {
      const raw = localStorage.getItem(LOCK_KEY)
      if (raw && JSON.parse(raw).tabId === tabIdRef.current) localStorage.removeItem(LOCK_KEY)
    } catch {
      // idem
    }
  }, [stopHeartbeat])

  const readForeignLock = useCallback((): boolean => {
    try {
      const raw = localStorage.getItem(LOCK_KEY)
      if (!raw) return false
      const lock = JSON.parse(raw) as { tabId: string; ts: number }
      return lock.tabId !== tabIdRef.current && Date.now() - lock.ts < LOCK_TTL_MS
    } catch {
      return false
    }
  }, [])

  const startTest = useCallback(
    // Reservado para diferenciar telemetria de repetição no futuro, se o Console
    // pedir esse recorte — hoje conta como o mesmo evento de funil "iniciado".
    async (_isRepeat: boolean) => {
      acquireLock()
      stopHeartbeat()
      heartbeatRef.current = setInterval(acquireLock, 1500)
      phaseRef.current = 'preparando'
      liveValueRef.current = 0
      setPhase('preparando')
      setLiveValue(0)
      setPhaseResults({})
      setResult(null)
      trackFeatureUsed(FEATURE_SPEEDTEST_INICIADO)

      const STEP_ORDER: FasePainel[] = ['latencia', 'download', 'upload']
      const engine = createSpeedTest()
      engineRef.current = engine
      try {
        const r = await engine.run({
          onPhase: (p) => {
            if (STEP_ORDER.includes(phaseRef.current)) {
              const key = phaseRef.current as 'latencia' | 'download' | 'upload'
              setPhaseResults((prev) => ({ ...prev, [key]: liveValueRef.current }))
            }
            phaseRef.current = p
            liveValueRef.current = 0
            setLiveValue(0)
            setPhase(p)
          },
          onTick: ({ instantMbps }) => {
            liveValueRef.current = instantMbps
            setLiveValue(instantMbps)
          },
          onLatencySample: (ms) => {
            liveValueRef.current = ms
            setLiveValue(ms)
          },
        })
        setPhaseResults({ latencia: r.latency.ms, download: r.download.mbps, upload: r.upload.mbps })
        setResult(r)
        phaseRef.current = r.partial ? 'parcial' : 'concluido'
        setPhase(phaseRef.current)
        trackFeatureUsed(FEATURE_SPEEDTEST_COMPLETOU)
        try {
          await addRecord(resultToRecord(r))
        } catch {
          // histórico é best-effort — falha aqui não derruba o resultado exibido
        }
      } catch (err) {
        stopHeartbeat()
        const code = err instanceof SpeedTestError ? err.code : 'unexpected-error'
        phaseRef.current = CODE_TO_PROBLEM_PHASE[code] ?? 'erro-inesperado'
        setPhase(phaseRef.current)
      } finally {
        releaseLock()
      }
    },
    [acquireLock, stopHeartbeat, releaseLock]
  )

  useEffect(() => {
    if (readForeignLock()) {
      phaseRef.current = 'bloqueado-outra-aba'
      setPhase('bloqueado-outra-aba')
    } else {
      startTest(false)
    }

    const onVisibilityChange = () => {
      const rodando: FasePainel[] = ['preparando', 'latencia', 'download', 'upload', 'processando']
      if (document.hidden && rodando.includes(phaseRef.current) && engineRef.current) {
        engineRef.current.cancel()
      }
    }
    document.addEventListener('visibilitychange', onVisibilityChange)
    window.addEventListener('beforeunload', releaseLock)

    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange)
      window.removeEventListener('beforeunload', releaseLock)
      stopHeartbeat()
      releaseLock()
      if (engineRef.current) engineRef.current.cancel()
    }
    // roda uma única vez ao montar — comportamento equivalente ao componentDidMount original
  }, [])

  const cancelTest = useCallback(() => {
    if (engineRef.current) engineRef.current.cancel()
  }, [])

  const retry = useCallback(() => startTest(true), [startTest])
  const forceStart = useCallback(() => startTest(false), [startTest])

  return { phase, liveValue, phaseResults, result, cancelTest, retry, forceStart }
}
