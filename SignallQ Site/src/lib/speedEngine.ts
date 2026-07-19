// Motor de medição real, isolado da interface (troque SPEEDTEST_DOWNLOAD_URL/
// SPEEDTEST_UPLOAD_URL em lib/config.ts). Usa os endpoints públicos de medição
// da Cloudflare (__down/__up) — a mesma infraestrutura usada pelo pacote
// open-source oficial @cloudflare/speedtest. Nenhum valor é simulado: todo
// número vem de XMLHttpRequest real, cronometrado com performance.now() e os
// eventos de progresso do navegador. Porte quase 1:1 do protótipo
// shared/speed-engine.js — só tipagem, sem reescrever a lógica de medição.
import { SPEEDTEST_DOWNLOAD_URL, SPEEDTEST_SERVER_LABEL, SPEEDTEST_UPLOAD_URL } from './config'

export type SpeedTestErrorCode =
  | 'no-connection'
  | 'connection-interrupted'
  | 'endpoint-unavailable'
  | 'cancelled'
  | 'unexpected-error'

export class SpeedTestError extends Error {
  code: SpeedTestErrorCode
  partial?: ChunkResult[]

  constructor(code: SpeedTestErrorCode, message?: string) {
    super(message || code)
    this.code = code
  }
}

interface ChunkResult {
  bytes: number
  ms: number
}

interface CancelToken {
  cancelled: boolean
  xhr: XMLHttpRequest | null
}

interface XhrRequestOptions {
  method: 'GET' | 'POST'
  url: string
  body?: Blob | null
  onProgress?: (p: { loaded: number; total: number; elapsed: number }) => void
  timeoutMs?: number
  cancelToken: CancelToken
}

function xhrRequest({
  method,
  url,
  body,
  onProgress,
  timeoutMs = 15000,
  cancelToken,
}: XhrRequestOptions): Promise<{ status: number; duration: number; bytes: number }> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    cancelToken.xhr = xhr
    xhr.open(method, url, true)
    xhr.timeout = timeoutMs
    if (method === 'GET') xhr.responseType = 'arraybuffer'
    const progressTarget = method === 'POST' ? xhr.upload : xhr
    const start = performance.now()
    progressTarget.onprogress = (e) => {
      if (onProgress) onProgress({ loaded: e.loaded, total: e.total, elapsed: performance.now() - start })
    }
    xhr.onload = () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        resolve({
          status: xhr.status,
          duration: performance.now() - start,
          bytes: body ? body.size : xhr.response ? (xhr.response as ArrayBuffer).byteLength : 0,
        })
      } else {
        reject(new SpeedTestError('endpoint-unavailable', `HTTP ${xhr.status}`))
      }
    }
    xhr.onerror = () =>
      reject(new SpeedTestError(navigator.onLine === false ? 'no-connection' : 'endpoint-unavailable', 'network error'))
    xhr.ontimeout = () => reject(new SpeedTestError('endpoint-unavailable', 'timeout'))
    xhr.onabort = () => reject(new SpeedTestError('cancelled', 'aborted'))
    try {
      xhr.send(body ?? null)
    } catch (e) {
      reject(new SpeedTestError('unexpected-error', e instanceof Error ? e.message : String(e)))
    }
  })
}

function randomBlob(bytes: number): Blob {
  const chunkSize = 65536
  const parts: Uint8Array[] = []
  let remaining = bytes
  while (remaining > 0) {
    const size = Math.min(chunkSize, remaining)
    const arr = new Uint8Array(size)
    crypto.getRandomValues(arr)
    parts.push(arr)
    remaining -= size
  }
  return new Blob(parts as BlobPart[])
}

export function median(nums: number[]): number {
  const s = [...nums].sort((a, b) => a - b)
  const mid = Math.floor(s.length / 2)
  return s.length % 2 ? s[mid] : (s[mid - 1] + s[mid]) / 2
}

export function meanAbsJitter(nums: number[]): number | null {
  if (nums.length < 2) return null
  let sum = 0
  for (let i = 1; i < nums.length; i++) sum += Math.abs(nums[i] - nums[i - 1])
  return sum / (nums.length - 1)
}

export function bytesToMbps(bytes: number, ms: number): number {
  if (ms <= 0) return 0
  return (bytes * 8) / (ms / 1000) / 1e6
}

interface ThroughputPhaseArgs {
  phase: 'download' | 'upload'
  sizes: number[]
  makeRequest: (size: number, onProgress: (p: { loaded: number; elapsed: number }) => void) => Promise<{ duration: number }>
  onTick: (t: { phase: 'download' | 'upload'; instantMbps: number; elapsedMs: number }) => void
  cancelToken: CancelToken
}

async function runThroughputPhase({ phase, sizes, makeRequest, onTick, cancelToken }: ThroughputPhaseArgs): Promise<number> {
  const chunkResults: ChunkResult[] = []
  let succeededOnce = false
  const phaseStart = performance.now()
  for (let i = 0; i < sizes.length; i++) {
    if (cancelToken.cancelled) throw new SpeedTestError('cancelled')
    let lastLoaded = 0
    let lastElapsed = 0
    try {
      const res = await makeRequest(sizes[i], ({ loaded, elapsed }) => {
        const deltaBytes = loaded - lastLoaded
        const deltaMs = elapsed - lastElapsed
        if (deltaMs > 60 && deltaBytes > 0) {
          onTick({ phase, instantMbps: bytesToMbps(deltaBytes, deltaMs), elapsedMs: performance.now() - phaseStart })
          lastLoaded = loaded
          lastElapsed = elapsed
        }
      })
      succeededOnce = true
      chunkResults.push({ bytes: sizes[i], ms: res.duration })
    } catch (err) {
      if (err instanceof SpeedTestError && err.code === 'cancelled') throw err
      if (succeededOnce) {
        const interrupted = new SpeedTestError('connection-interrupted', err instanceof Error ? err.message : String(err))
        interrupted.partial = chunkResults
        throw interrupted
      }
      throw err
    }
    if (performance.now() - phaseStart > 12000) break
  }
  const usable = chunkResults.length > 1 ? chunkResults.slice(1) : chunkResults
  const totalBytes = usable.reduce((s, c) => s + c.bytes, 0)
  const totalMs = usable.reduce((s, c) => s + c.ms, 0)
  return bytesToMbps(totalBytes, totalMs)
}

export interface SpeedTestResult {
  id: string
  timestamp: number
  download: { mbps: number }
  upload: { mbps: number }
  latency: { ms: number }
  jitter: { ms: number } | null
  loadedLatency: { ms: number } | null
  connectionType: string | null
  server: string
  partial: boolean
}

export type SpeedTestPhase = 'preparando' | 'latencia' | 'download' | 'upload' | 'processando'

interface SpeedTestCallbacks {
  onPhase?: (phase: SpeedTestPhase) => void
  onTick?: (t: { phase: 'download' | 'upload'; instantMbps: number; elapsedMs: number }) => void
  onLatencySample?: (ms: number) => void
}

interface NavigatorConnection {
  effectiveType?: string
}

export function createSpeedTest() {
  const cancelToken: CancelToken = { cancelled: false, xhr: null }

  function cancel() {
    cancelToken.cancelled = true
    if (cancelToken.xhr) {
      try {
        cancelToken.xhr.abort()
      } catch {
        // xhr já finalizado — nada a fazer
      }
    }
  }

  async function measureLatencyOnce(): Promise<number> {
    const start = performance.now()
    await xhrRequest({
      method: 'GET',
      url: `${SPEEDTEST_DOWNLOAD_URL}?bytes=0&t=${Date.now()}${Math.random()}`,
      timeoutMs: 6000,
      cancelToken,
    })
    return performance.now() - start
  }

  async function run(callbacks: SpeedTestCallbacks = {}): Promise<SpeedTestResult> {
    const { onPhase = () => {}, onTick = () => {}, onLatencySample = () => {} } = callbacks
    const id = crypto.randomUUID ? crypto.randomUUID() : `m_${Date.now()}_${Math.random().toString(36).slice(2)}`

    if (navigator.onLine === false) throw new SpeedTestError('no-connection', 'Sem conexão com a internet.')

    onPhase('preparando')
    try {
      await measureLatencyOnce()
    } catch (err) {
      if (cancelToken.cancelled) throw new SpeedTestError('cancelled')
      throw err instanceof SpeedTestError ? err : new SpeedTestError('endpoint-unavailable')
    }

    onPhase('latencia')
    const latencySamples: number[] = []
    for (let i = 0; i < 7; i++) {
      if (cancelToken.cancelled) throw new SpeedTestError('cancelled')
      try {
        const ms = await measureLatencyOnce()
        latencySamples.push(ms)
        onLatencySample(ms)
      } catch (err) {
        if (cancelToken.cancelled || (err instanceof SpeedTestError && err.code === 'cancelled')) {
          throw new SpeedTestError('cancelled')
        }
        if (latencySamples.length < 3) throw err
        break
      }
      await new Promise((r) => setTimeout(r, 120))
    }
    const latencyMs = median(latencySamples)
    const jitterMs = meanAbsJitter(latencySamples)

    let loadedLatencyMs: number | null = null
    const loadedLatencyPromise = measureLatencyOnce()
      .then((v) => {
        loadedLatencyMs = v
      })
      .catch(() => {})

    onPhase('download')
    let downloadMbps: number
    let partial = false
    try {
      downloadMbps = await runThroughputPhase({
        phase: 'download',
        sizes: [4e6, 8e6, 16e6, 32e6],
        makeRequest: (size, onProgress) =>
          xhrRequest({
            method: 'GET',
            url: `${SPEEDTEST_DOWNLOAD_URL}?bytes=${size}&t=${Date.now()}`,
            onProgress,
            timeoutMs: 20000,
            cancelToken,
          }),
        onTick,
        cancelToken,
      })
    } catch (err) {
      if (err instanceof SpeedTestError && err.code === 'connection-interrupted' && err.partial?.length) {
        const b = err.partial.reduce((s, c) => s + c.bytes, 0)
        const m = err.partial.reduce((s, c) => s + c.ms, 0)
        downloadMbps = bytesToMbps(b, m)
        partial = true
      } else throw err
    }
    await loadedLatencyPromise

    onPhase('upload')
    let uploadMbps: number
    try {
      uploadMbps = await runThroughputPhase({
        phase: 'upload',
        sizes: [2e6, 4e6, 8e6, 16e6],
        makeRequest: (size, onProgress) =>
          xhrRequest({ method: 'POST', url: SPEEDTEST_UPLOAD_URL, body: randomBlob(size), onProgress, timeoutMs: 20000, cancelToken }),
        onTick,
        cancelToken,
      })
    } catch (err) {
      if (err instanceof SpeedTestError && err.code === 'connection-interrupted' && err.partial?.length) {
        const b = err.partial.reduce((s, c) => s + c.bytes, 0)
        const m = err.partial.reduce((s, c) => s + c.ms, 0)
        uploadMbps = bytesToMbps(b, m)
        partial = true
      } else throw err
    }

    onPhase('processando')
    await new Promise((r) => setTimeout(r, 350))

    const connection = (navigator as unknown as { connection?: NavigatorConnection }).connection
    return {
      id,
      timestamp: Date.now(),
      download: { mbps: downloadMbps },
      upload: { mbps: uploadMbps },
      latency: { ms: latencyMs },
      jitter: jitterMs != null ? { ms: jitterMs } : null,
      loadedLatency: loadedLatencyMs != null ? { ms: loadedLatencyMs } : null,
      connectionType: connection?.effectiveType ?? null,
      server: SPEEDTEST_SERVER_LABEL,
      partial,
    }
  }

  return { run, cancel }
}
