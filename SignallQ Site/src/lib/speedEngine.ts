import { SPEEDTEST_DOWNLOAD_URL, SPEEDTEST_LATENCY_URL, SPEEDTEST_SERVER_LABEL, SPEEDTEST_UPLOAD_URL } from './config'

export type SpeedTestMode = 'rapido' | 'completo' | 'triplo'
export type MeasurementStatus = 'complete' | 'partial' | 'inconclusive' | 'contaminated' | 'cancelled'
export type SpeedTestPhase = 'preparando' | 'latencia' | 'download' | 'upload' | 'processando'
export type BufferbloatSeverity = 'none' | 'mild' | 'moderate' | 'severe'

export type SpeedTestErrorCode = 'no-connection' | 'connection-interrupted' | 'endpoint-unavailable' | 'cancelled' | 'unexpected-error'

export class SpeedTestError extends Error {
  constructor(public code: SpeedTestErrorCode, message: string = code) {
    super(message)
  }
}

interface CancelToken {
  cancelled: boolean
  contaminated: boolean
  xhrs: Set<XMLHttpRequest>
}

interface Sample {
  tMs: number
  mbps: number
}

interface LatencySummary {
  ms: number
  jitterMs: number
  packetLossPercent: number
  totalSamples: number
  validSamples: number
  timeouts: number
  maxMs: number
  p95Ms: number
  peaks: number
}

interface ThroughputSummary {
  mbps: number
  peakMbps: number
  samples: number[]
  endedBy: 'time_elapsed' | 'network_changed' | 'cancelled'
  requestErrors: number
}

export interface SpeedTestResult {
  id: string
  timestamp: number
  mode: SpeedTestMode
  status: MeasurementStatus
  download: { mbps: number; peakMbps: number }
  upload: { mbps: number; peakMbps: number }
  latency: { ms: number; samples: number; validSamples: number; timeouts: number; maxMs: number; p95Ms: number; peaks: number }
  jitter: { ms: number } | null
  packetLoss: { percent: number }
  loadedLatency: { downloadMs: number; uploadMs: number } | null
  bufferbloat: { ms: number; severity: BufferbloatSeverity }
  stabilityScore: number
  dns: { latencyMs: number | null; resolverIp: string | null; provider: string | null }
  connectionType: string | null
  server: string
  /** Tempo efetivamente decorrido nesta rodada, medido no navegador. */
  durationMs?: number
  partial: boolean
  rounds?: Array<{ downloadMbps: number; uploadMbps: number; latencyMs: number }>
}

export const SPEED_TEST_MODE_CONFIG: Record<Exclude<SpeedTestMode, 'triplo'>, {
  latencySampleCount: number
  downloadDurationMs: number
  uploadDurationMs: number
  downloadPayloadBytes: number
  uploadPayloadBytes: number
  downloadInitialStreams: number
  downloadMaxStreams: number
  uploadInitialStreams: number
  uploadMaxStreams: number
  warmupMs: number
}> = {
  rapido: {
    latencySampleCount: 15, downloadDurationMs: 7000, uploadDurationMs: 7000,
    downloadPayloadBytes: 10_000_000, uploadPayloadBytes: 5_000_000,
    downloadInitialStreams: 2, downloadMaxStreams: 4, uploadInitialStreams: 4, uploadMaxStreams: 4, warmupMs: 1000,
  },
  completo: {
    latencySampleCount: 25, downloadDurationMs: 18_000, uploadDurationMs: 18_000,
    downloadPayloadBytes: 25_000_000, uploadPayloadBytes: 10_000_000,
    downloadInitialStreams: 2, downloadMaxStreams: 8, uploadInitialStreams: 8, uploadMaxStreams: 8, warmupMs: 2000,
  },
}

export function median(nums: number[]): number {
  if (!nums.length) return 0
  const values = [...nums].sort((a, b) => a - b)
  const middle = Math.floor(values.length / 2)
  return values.length % 2 ? values[middle]! : (values[middle - 1]! + values[middle]!) / 2
}

export function meanAbsJitter(nums: number[]): number | null {
  if (nums.length < 2) return null
  return nums.slice(1).reduce((sum, value, index) => sum + Math.abs(value - nums[index]!), 0) / (nums.length - 1)
}

export function bytesToMbps(bytes: number, ms: number): number {
  return ms > 0 ? (bytes * 8) / (ms / 1000) / 1e6 : 0
}

export function summarizeLatency(raw: Array<number | null>): LatencySummary {
  const samples = raw.slice(1)
  const timeouts = samples.filter((sample) => sample == null).length
  const valid = samples.filter((sample): sample is number => sample != null && Number.isFinite(sample))
  const baseMedian = median(valid)
  const filtered = baseMedian > 0 ? valid.filter((sample) => sample <= baseMedian * 3) : valid
  const used = filtered.length ? filtered : valid
  const ordered = [...valid].sort((a, b) => a - b)
  const p95Index = ordered.length ? Math.min(ordered.length - 1, Math.max(0, Math.ceil(ordered.length * 0.95) - 1)) : 0
  return {
    ms: median(used),
    jitterMs: meanAbsJitter(used) ?? 0,
    packetLossPercent: samples.length ? (timeouts / samples.length) * 100 : 0,
    totalSamples: samples.length,
    validSamples: used.length,
    timeouts,
    maxMs: ordered.at(-1) ?? 0,
    p95Ms: ordered[p95Index] ?? 0,
    peaks: valid.length - used.length,
  }
}

function xhrRequest(method: 'GET' | 'POST', url: string, body: Blob | null, token: CancelToken, onProgress?: (loaded: number) => void): Promise<{ bytes: number; duration: number }> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest()
    token.xhrs.add(xhr)
    xhr.open(method, url, true)
    xhr.timeout = 20_000
    if (method === 'GET') xhr.responseType = 'arraybuffer'
    const startedAt = performance.now()
    const target = method === 'POST' ? xhr.upload : xhr
    target.onprogress = (event) => onProgress?.(event.loaded)
    const finish = () => token.xhrs.delete(xhr)
    xhr.onload = () => {
      finish()
      if (xhr.status >= 200 && xhr.status < 300) resolve({ bytes: body?.size ?? (xhr.response as ArrayBuffer | null)?.byteLength ?? 0, duration: performance.now() - startedAt })
      else reject(new SpeedTestError('endpoint-unavailable', `HTTP ${xhr.status}`))
    }
    xhr.onerror = () => { finish(); reject(new SpeedTestError(navigator.onLine ? 'endpoint-unavailable' : 'no-connection')) }
    xhr.ontimeout = () => { finish(); reject(new SpeedTestError('endpoint-unavailable', 'timeout')) }
    xhr.onabort = () => { finish(); reject(new SpeedTestError('cancelled')) }
    xhr.send(body)
  })
}

function uploadBlob(bytes: number): Blob {
  const chunks: Uint8Array[] = []
  for (let remaining = bytes; remaining > 0; remaining -= 65_536) {
    const chunk = new Uint8Array(Math.min(65_536, remaining))
    crypto.getRandomValues(chunk)
    chunks.push(chunk)
  }
  return new Blob(chunks as any)
}

async function measureLatency(token: CancelToken): Promise<number | null> {
  const startedAt = performance.now()
  try {
    await xhrRequest('GET', `${SPEEDTEST_LATENCY_URL}${SPEEDTEST_LATENCY_URL.includes('?') ? '&' : '?'}_cb=${Date.now()}_${Math.random()}`, null, token)
    return performance.now() - startedAt
  } catch (error) {
    if (error instanceof SpeedTestError && error.code === 'cancelled') throw error
    return null
  }
}

async function collectLatency(count: number, token: CancelToken, onSample: (ms: number) => void): Promise<LatencySummary> {
  const raw: Array<number | null> = []
  for (let index = 0; index < count; index++) {
    if (token.cancelled) throw new SpeedTestError('cancelled')
    const sample = await measureLatency(token)
    raw.push(sample)
    if (sample != null) onSample(sample)
  }
  return summarizeLatency(raw)
}

async function runThroughput(
  phase: 'download' | 'upload',
  config: typeof SPEED_TEST_MODE_CONFIG.rapido,
  token: CancelToken,
  onTick: (mbps: number) => void,
): Promise<{ throughput: ThroughputSummary; loadLatencyMs: number }> {
  const durationMs = phase === 'download' ? config.downloadDurationMs : config.uploadDurationMs
  const payloadBytes = phase === 'download' ? config.downloadPayloadBytes : config.uploadPayloadBytes
  const initialStreams = phase === 'download' ? config.downloadInitialStreams : config.uploadInitialStreams
  const maxStreams = phase === 'download' ? config.downloadMaxStreams : config.uploadMaxStreams
  const startedAt = performance.now()
  const samples: Sample[] = []
  const loadPings: number[] = []
  let bytesTick = 0
  let targetStreams = initialStreams
  let requestErrors = 0
  let lastScaleAt = 0
  let previousAverage = 0
  const stopAt = startedAt + durationMs

  const worker = async (index: number) => {
    if (index) await new Promise((resolve) => setTimeout(resolve, index * 200))
    while (!token.cancelled && performance.now() < stopAt) {
      if (index >= targetStreams) { await new Promise((resolve) => setTimeout(resolve, 120)); continue }
      try {
        let previousLoaded = 0
        const onProgress = (loaded: number) => {
          bytesTick += Math.max(0, loaded - previousLoaded)
          previousLoaded = loaded
        }
        const request = phase === 'download'
          ? xhrRequest('GET', `${SPEEDTEST_DOWNLOAD_URL}?bytes=${payloadBytes}&_cb=${Date.now()}_${Math.random()}`, null, token, onProgress)
          : xhrRequest('POST', `${SPEEDTEST_UPLOAD_URL}?_cb=${Date.now()}_${Math.random()}`, uploadBlob(payloadBytes), token, onProgress)
        const response = await request
        // Alguns navegadores não emitem progresso de upload para payloads pequenos.
        if (phase === 'upload' && previousLoaded === 0) bytesTick += response.bytes
      } catch (error) {
        if (error instanceof SpeedTestError && error.code === 'cancelled') return
        requestErrors++
        if (!navigator.onLine) { token.contaminated = true; return }
      }
    }
  }

  const sampler = window.setInterval(() => {
    const elapsed = performance.now() - startedAt
    const instant = bytesToMbps(bytesTick, 1000)
    bytesTick = 0
    if (instant > 0) { samples.push({ tMs: elapsed, mbps: instant }); onTick(instant) }
    if (elapsed - lastScaleAt >= 4000 && targetStreams < maxStreams) {
      const recent = samples.filter((sample) => sample.tMs >= Math.max(0, elapsed - 4000)).map((sample) => sample.mbps)
      const average = recent.length ? recent.reduce((sum, value) => sum + value, 0) / recent.length : 0
      if (!previousAverage || average >= previousAverage * 1.1) targetStreams = Math.min(maxStreams, targetStreams + 2)
      previousAverage = average
      lastScaleAt = elapsed
    }
  }, 1000)
  const pingLoop = (async () => {
    while (!token.cancelled && performance.now() < stopAt) {
      const before = performance.now()
      const ping = await measureLatency(token)
      if (ping != null) loadPings.push(ping)
      await new Promise((resolve) => setTimeout(resolve, Math.max(0, 1000 - (performance.now() - before))))
    }
  })()

  await Promise.all(Array.from({ length: maxStreams }, (_, index) => worker(index)))
  window.clearInterval(sampler)
  await pingLoop
  const valid = samples.filter((sample) => sample.tMs >= config.warmupMs && sample.mbps > 0)
  const stable = valid.slice(Math.min(valid.length, Math.ceil(valid.length * 0.35)))
  const measured = stable.length ? stable : valid
  const endedBy = token.cancelled ? 'cancelled' : token.contaminated ? 'network_changed' : 'time_elapsed'
  return {
    throughput: { mbps: measured.length ? measured.reduce((sum, sample) => sum + sample.mbps, 0) / measured.length : 0, peakMbps: valid.reduce((peak, sample) => Math.max(peak, sample.mbps), 0), samples: valid.map((sample) => sample.mbps), endedBy, requestErrors },
    loadLatencyMs: median(loadPings),
  }
}

async function measureDns(): Promise<SpeedTestResult['dns']> {
  const url = 'https://cloudflare-dns.com/dns-query?name=whoami.cloudflare.com&type=TXT'
  const startedAt = performance.now()
  try {
    const response = await fetch(url, { headers: { accept: 'application/dns-json' }, cache: 'no-store', signal: AbortSignal.timeout(4000) })
    if (!response.ok) return { latencyMs: null, resolverIp: null, provider: null }
    const body = JSON.stringify(await response.json())
    const resolverIp = body.match(/\b(?:\d{1,3}\.){3}\d{1,3}\b/)?.[0] ?? null
    return { latencyMs: Math.round(performance.now() - startedAt), resolverIp, provider: body.toLowerCase().includes('cloudflare') ? 'cloudflare' : resolverIp ? 'desconhecido' : null }
  } catch {
    return { latencyMs: null, resolverIp: null, provider: null }
  }
}

function bufferbloatSeverity(ms: number): BufferbloatSeverity {
  if (ms < 5) return 'none'
  if (ms <= 30) return 'mild'
  if (ms <= 100) return 'moderate'
  return 'severe'
}

function stability(samples: number[]): number {
  if (samples.length < 2) return 0
  const mean = samples.reduce((sum, sample) => sum + sample, 0) / samples.length
  if (!mean) return 0
  const deviation = Math.sqrt(samples.reduce((sum, sample) => sum + (sample - mean) ** 2, 0) / samples.length)
  return Math.max(0, Math.min(100, 100 - (deviation / mean) * 100))
}

interface Callbacks {
  onPhase?: (phase: SpeedTestPhase) => void
  onTick?: (tick: { phase: 'download' | 'upload'; instantMbps: number; elapsedMs: number }) => void
  onLatencySample?: (ms: number) => void
  onRound?: (round: number, total: number) => void
}

export function createSpeedTest(mode: SpeedTestMode = 'rapido') {
  const token: CancelToken = { cancelled: false, contaminated: false, xhrs: new Set() }
  const cancel = () => { token.cancelled = true; token.xhrs.forEach((xhr) => xhr.abort()) }
  const markContaminated = () => { token.contaminated = true }

  const runSingle = async (singleMode: Exclude<SpeedTestMode, 'triplo'>, callbacks: Callbacks): Promise<SpeedTestResult> => {
    const startedAt = performance.now()
    const config = SPEED_TEST_MODE_CONFIG[singleMode]
    const onPhase = callbacks.onPhase ?? (() => {})
    const onTick = callbacks.onTick ?? (() => {})
    const onLatencySample = callbacks.onLatencySample ?? (() => {})
    if (!navigator.onLine) throw new SpeedTestError('no-connection')
    onPhase('preparando')
    onPhase('latencia')
    const latency = await collectLatency(config.latencySampleCount, token, onLatencySample)
    onPhase('download')
    const download = await runThroughput('download', config, token, (instantMbps) => onTick({ phase: 'download', instantMbps, elapsedMs: 0 }))
    onPhase('upload')
    const dnsPromise = measureDns()
    const upload = await runThroughput('upload', config, token, (instantMbps) => onTick({ phase: 'upload', instantMbps, elapsedMs: 0 }))
    const dns = await dnsPromise
    if (token.cancelled) throw new SpeedTestError('cancelled')
    onPhase('processando')
    const bufferbloatMs = Math.max(download.loadLatencyMs, upload.loadLatencyMs) - latency.ms
    const partial = download.throughput.endedBy !== 'time_elapsed' || upload.throughput.endedBy !== 'time_elapsed' || download.throughput.mbps <= 0 || upload.throughput.mbps <= 0
    const status: MeasurementStatus = token.contaminated ? 'contaminated' : latency.validSamples < 5 ? 'inconclusive' : partial ? 'partial' : 'complete'
    return {
      id: crypto.randomUUID(), timestamp: Date.now(), mode: singleMode, status,
      download: { mbps: download.throughput.mbps, peakMbps: download.throughput.peakMbps },
      upload: { mbps: upload.throughput.mbps, peakMbps: upload.throughput.peakMbps },
      latency: { ms: latency.ms, samples: latency.totalSamples, validSamples: latency.validSamples, timeouts: latency.timeouts, maxMs: latency.maxMs, p95Ms: latency.p95Ms, peaks: latency.peaks },
      jitter: latency.validSamples >= 2 ? { ms: latency.jitterMs } : null,
      packetLoss: { percent: latency.packetLossPercent },
      loadedLatency: { downloadMs: download.loadLatencyMs, uploadMs: upload.loadLatencyMs },
      bufferbloat: { ms: Math.max(bufferbloatMs, 0), severity: bufferbloatSeverity(Math.max(bufferbloatMs, 0)) },
      stabilityScore: stability([...download.throughput.samples, ...upload.throughput.samples]), dns,
      connectionType: (navigator as Navigator & { connection?: { effectiveType?: string } }).connection?.effectiveType ?? null,
      server: SPEEDTEST_SERVER_LABEL, partial,
      durationMs: Math.round(performance.now() - startedAt),
    }
  }

  const run = async (callbacks: Callbacks = {}): Promise<SpeedTestResult> => {
    if (mode !== 'triplo') return runSingle(mode, callbacks)
    const rounds: Array<{ downloadMbps: number; uploadMbps: number; latencyMs: number }> = []
    let finalRound: SpeedTestResult | null = null
    for (let index = 0; index < 3; index++) {
      callbacks.onRound?.(index + 1, 3)
      const result = await runSingle('rapido', callbacks)
      if (result.status !== 'complete') return { ...result, mode: 'triplo', rounds }
      rounds.push({ downloadMbps: result.download.mbps, uploadMbps: result.upload.mbps, latencyMs: result.latency.ms })
      finalRound = result
    }
    const last = finalRound!
    // A terceira rodada ja e completa; usa sua telemetria e substitui apenas as metricas agregadas.
    return {
      ...last, mode: 'triplo', rounds,
      download: { ...last.download, mbps: median(rounds.map((round) => round.downloadMbps)) },
      upload: { ...last.upload, mbps: median(rounds.map((round) => round.uploadMbps)) },
      latency: { ...last.latency, ms: median(rounds.map((round) => round.latencyMs)) },
    }
  }

  return { run, cancel, markContaminated }
}

