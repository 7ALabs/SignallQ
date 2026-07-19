// Hook de estado de rede — adaptado do app Android/linka-speedtest para o
// Site (sem a camada Capacitor: aqui só existe navegador).
//
// Camada 1 (passiva): tipo de interface via navigator.onLine / navigator.connection.
// Camada 2 (ativa): checagem real de internet via GET ao mesmo endpoint usado
// pelo motor de speedtest (SPEEDTEST_DOWNLOAD_URL, lib/config.ts) — necessária
// porque navigator.onLine só reflete a interface de rede, não a internet real
// (ex.: Wi-Fi conectado a um roteador sem internet, portal cativo).
import { useCallback, useEffect, useState } from 'react'
import { SPEEDTEST_DOWNLOAD_URL } from '../lib/config'
import type { TipoRede } from '../lib/connection'

export interface EstadoRede {
  tipo: TipoRede
  internet: boolean
  timestamp: number
}

interface NetworkInformation {
  type?: string
  effectiveType?: string
  addEventListener(type: string, listener: EventListenerOrEventListenerObject): void
  removeEventListener(type: string, listener: EventListenerOrEventListenerObject): void
}

function getNavConn(): NetworkInformation | undefined {
  return (navigator as Navigator & { connection?: NetworkInformation }).connection
}

function tipoDeInterface(): TipoRede {
  if (!navigator.onLine) return 'nenhuma'
  const conn = getNavConn()
  const t = conn?.type
  const eff = conn?.effectiveType
  if (t === 'wifi') return 'wifi'
  if (t === 'cellular') return 'celular'
  if (t === 'ethernet' || t === 'wimax') return 'ethernet'
  if (t === 'bluetooth') return 'celular'
  if (!t && (eff === '2g' || eff === '3g' || eff === 'slow-2g')) return 'celular'
  return 'desconhecida'
}

async function verificarInternet(): Promise<boolean> {
  if (!navigator.onLine) return false
  try {
    const cb = `${Date.now()}_${Math.random().toString(36).slice(2)}`
    const resp = await fetch(`${SPEEDTEST_DOWNLOAD_URL}?bytes=0&_cb=${cb}`, {
      cache: 'no-store',
      signal: AbortSignal.timeout(1500),
    })
    return resp.ok
  } catch {
    return false
  }
}

const ESTADO_INICIAL: EstadoRede = { tipo: 'desconhecida', internet: true, timestamp: Date.now() }
const POLLING_ESTAVEL_MS = 60_000

export function useEstadoRede(): EstadoRede & { revalidarAgora: () => Promise<EstadoRede> } {
  const [estado, setEstado] = useState<EstadoRede>(ESTADO_INICIAL)

  const atualizarEstado = useCallback(async (): Promise<EstadoRede> => {
    const tipo = tipoDeInterface()
    const internet = await verificarInternet()
    const novo: EstadoRede = { tipo, internet, timestamp: Date.now() }
    setEstado((prev) => (prev.tipo === novo.tipo && prev.internet === novo.internet ? { ...prev, timestamp: novo.timestamp } : novo))
    return novo
  }, [])

  const revalidarAgora = useCallback((): Promise<EstadoRede> => atualizarEstado(), [atualizarEstado])

  useEffect(() => {
    void atualizarEstado()
    const timer = setInterval(() => void atualizarEstado(), POLLING_ESTAVEL_MS)
    return () => clearInterval(timer)
  }, [atualizarEstado])

  useEffect(() => {
    const handler = () => void atualizarEstado()
    window.addEventListener('online', handler)
    window.addEventListener('offline', handler)
    return () => {
      window.removeEventListener('online', handler)
      window.removeEventListener('offline', handler)
    }
  }, [atualizarEstado])

  return { ...estado, revalidarAgora }
}
