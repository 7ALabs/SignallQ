import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState, type ReactNode } from 'react'
import { buscarAnunciosLocais, type AnuncioLocal } from '../lib/localAdsClient'

// Coordenação de anúncio por página — issue #1402/#1405. Achado da auditoria 1:1 de
// 2026-07-25 (Marina): antes deste provider, cada espaço de anúncio (`AdRail` a/b,
// `AdBannerWide`, `ResultAdCard`) buscava e sorteava o catálogo local de forma
// independente, podendo repetir o mesmo anúncio em 2 espaços da mesma página.
//
// Regras implementadas aqui (decisão do Luiz, 2026-07-25):
// 1. Um único fetch do catálogo por carregamento de página — este provider fica em
//    `PageLayout`/`HomePage`, acima de todos os espaços de anúncio da tela.
// 2. `useAdSlotItems(slotId, count)` distribui itens distintos por slot a partir de uma
//    ordem embaralhada compartilhada, só repetindo quando o catálogo (descontados os já
//    entregues) não tiver itens suficientes — caso extremo previsto no pedido.
// 3. TODO espaço de anúncio da página se registra no pool de preenchimento (regra 4) —
//    `AdBannerWide` via `useAdSenseCoordination` (pode alternar pra AdSense real) e
//    `AdRail`/`ResultAdCard` via `useAlwaysLocalAdSlot` (nunca tentam AdSense, reportam
//    `local` assim que montam). Depois que TODOS os espaços registrados sinalizarem o
//    resultado, se NENHUM ficou com o catálogo interno, um deles é sorteado e forçado a
//    trocar pro catálogo (regra 4 do pedido: pelo menos 1 espaço interno por página, mesmo
//    com AdSense preenchendo tudo).
//
// Importante: registrar `AdRail`/`ResultAdCard` no MESMO pool (não só `AdBannerWide`) evita
// um bug sutil — sem isso, numa página com só 1 slot de AdSense de verdade (o caso comum
// hoje), "nenhum slot capaz ficou local" seria quase sempre verdade assim que esse único
// slot preenchesse, forçando-o a virar catálogo interno sempre, mesmo já havendo 2 `AdRail`
// mostrando conteúdo local ao lado. Com todos no mesmo pool, a regra 4 só força de verdade
// no caso raro em que TODOS os espaços da página (inclusive os que nunca tentam AdSense)
// entrariam vazios — o que não acontece, já que `AdRail`/`ResultAdCard` sempre reportam
// `local` imediatamente.

export type AdSlotFillStatus = 'adsense' | 'local'

interface AdSlotsContextValue {
  drawItems: (slotId: string, count: number) => AnuncioLocal[]
  registerAdSlot: (slotId: string) => () => void
  reportFillStatus: (slotId: string, status: AdSlotFillStatus) => void
  isForcedLocal: (slotId: string) => boolean
}

const AdSlotsContext = createContext<AdSlotsContextValue | null>(null)

function embaralhar<T>(lista: T[]): T[] {
  const copia = [...lista]
  for (let i = copia.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[copia[i], copia[j]] = [copia[j], copia[i]]
  }
  return copia
}

export function AdSlotsProvider({ children }: { children: ReactNode }) {
  const [catalogo, setCatalogo] = useState<AnuncioLocal[]>([])
  const ordemRef = useRef<AnuncioLocal[]>([])
  const cursorRef = useRef(0)
  const atribuidosRef = useRef<Map<string, AnuncioLocal[]>>(new Map())

  const [slotsCapazes, setSlotsCapazes] = useState<string[]>([])
  const [statusPorSlot, setStatusPorSlot] = useState<Map<string, AdSlotFillStatus>>(new Map())
  const [slotForcado, setSlotForcado] = useState<string | null>(null)

  useEffect(() => {
    let cancelado = false
    const controller = typeof AbortController !== 'undefined' ? new AbortController() : undefined

    buscarAnunciosLocais(controller?.signal).then((lista) => {
      if (cancelado) return
      ordemRef.current = embaralhar(lista)
      setCatalogo(lista)
    })

    return () => {
      cancelado = true
      controller?.abort()
    }
  }, [])

  const drawItems = useCallback(
    (slotId: string, count: number): AnuncioLocal[] => {
      const jaAtribuido = atribuidosRef.current.get(slotId)
      if (jaAtribuido) return jaAtribuido

      const ordem = ordemRef.current
      if (ordem.length === 0) return []

      const resultado: AnuncioLocal[] = []
      for (let i = 0; i < count; i++) {
        resultado.push(ordem[cursorRef.current % ordem.length])
        cursorRef.current += 1
      }
      atribuidosRef.current.set(slotId, resultado)
      return resultado
    },
    // `catalogo` só entra como dependência pra trocar a identidade de `drawItems` quando o
    // fetch resolver (a leitura real acontece via `ordemRef`, não via este state).
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [catalogo]
  )

  const registerAdSlot = useCallback((slotId: string) => {
    setSlotsCapazes((prev) => (prev.includes(slotId) ? prev : [...prev, slotId]))
    return () => {
      setSlotsCapazes((prev) => prev.filter((id) => id !== slotId))
      setStatusPorSlot((prev) => {
        if (!prev.has(slotId)) return prev
        const next = new Map(prev)
        next.delete(slotId)
        return next
      })
    }
  }, [])

  const reportFillStatus = useCallback((slotId: string, status: AdSlotFillStatus) => {
    setStatusPorSlot((prev) => {
      if (prev.get(slotId) === status) return prev
      const next = new Map(prev)
      next.set(slotId, status)
      return next
    })
  }, [])

  // Regra 4: só decide depois que TODOS os slots capazes de AdSense já resolveram
  // (reportaram `adsense` ou `local`). Se nenhum organicamente caiu pro catálogo, sorteia
  // um pra forçar. Recalcula sempre que o conjunto de slots ou os status mudarem — cobre
  // remoção/adição de slot (ex.: navegação SPA) sem travar numa decisão obsoleta.
  useEffect(() => {
    if (slotsCapazes.length === 0) return
    const todosResolvidos = slotsCapazes.every((id) => statusPorSlot.has(id))
    if (!todosResolvidos) return

    const algumLocal = slotsCapazes.some((id) => statusPorSlot.get(id) === 'local')
    if (algumLocal) {
      setSlotForcado((atual) => (atual && !slotsCapazes.includes(atual) ? null : atual))
      return
    }

    setSlotForcado((atual) => {
      if (atual && slotsCapazes.includes(atual)) return atual
      return slotsCapazes[Math.floor(Math.random() * slotsCapazes.length)]
    })
  }, [slotsCapazes, statusPorSlot])

  const isForcedLocal = useCallback((slotId: string) => slotForcado === slotId, [slotForcado])

  const value = useMemo<AdSlotsContextValue>(
    () => ({ drawItems, registerAdSlot, reportFillStatus, isForcedLocal }),
    [drawItems, registerAdSlot, reportFillStatus, isForcedLocal]
  )

  return <AdSlotsContext.Provider value={value}>{children}</AdSlotsContext.Provider>
}

function useAdSlotsContext(): AdSlotsContextValue {
  const ctx = useContext(AdSlotsContext)
  if (!ctx) throw new Error('useAdSlotItems/useAdSenseCoordination precisam estar dentro de <AdSlotsProvider>')
  return ctx
}

/**
 * Pega `count` itens distintos do catálogo pra um slot (`slotId` estável — normalmente vem de
 * `useId()`). A mesma identidade de slot sempre recebe a mesma atribuição enquanto o
 * componente estiver montado, mesmo que o provider re-renderize por outro motivo.
 */
export function useAdSlotItems(slotId: string, count: number): AnuncioLocal[] {
  const { drawItems } = useAdSlotsContext()
  return useMemo(() => drawItems(slotId, count), [drawItems, slotId, count])
}

export interface AdSenseCoordination {
  /** `true` quando o provider decidiu forçar este slot a mostrar o catálogo interno mesmo
   * que o AdSense dele tivesse preenchido de verdade (regra 4: pelo menos 1 espaço interno
   * por página). */
  forcedLocal: boolean
  /** Reporta o resultado final do AdSense deste slot — chamar quando resolver (`adsense` =
   * preencheu de verdade / `local` = caiu pro catálogo ou nunca tentou). */
  reportFillStatus: (status: AdSlotFillStatus) => void
}

/**
 * Registra um espaço de anúncio que pode alternar entre AdSense real e catálogo local — usado
 * pelos componentes que de fato tentam AdSense (hoje, `AdBannerWide`, via `useAdFallback`).
 */
export function useAdSenseCoordination(slotId: string): AdSenseCoordination {
  const { registerAdSlot, reportFillStatus, isForcedLocal } = useAdSlotsContext()

  useEffect(() => {
    return registerAdSlot(slotId)
  }, [registerAdSlot, slotId])

  const report = useCallback((status: AdSlotFillStatus) => reportFillStatus(slotId, status), [reportFillStatus, slotId])

  return { forcedLocal: isForcedLocal(slotId), reportFillStatus: report }
}

/**
 * Registra um espaço de anúncio que NUNCA tenta AdSense real (`AdRail`, `ResultAdCard`) —
 * reporta `local` assim que monta, garantindo que ele conte pro pool da regra 4 (ver nota no
 * topo do arquivo: sem isso, uma página com só 1 `AdBannerWide` capaz de AdSense forçaria
 * esse único slot a virar catálogo interno toda vez que preenchesse de verdade, mesmo já
 * havendo `AdRail`s mostrando conteúdo local ao lado).
 */
export function useAlwaysLocalAdSlot(slotId: string): void {
  const { registerAdSlot, reportFillStatus } = useAdSlotsContext()

  useEffect(() => {
    const unregister = registerAdSlot(slotId)
    reportFillStatus(slotId, 'local')
    return unregister
  }, [registerAdSlot, reportFillStatus, slotId])
}
