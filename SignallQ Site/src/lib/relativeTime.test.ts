import { describe, expect, it } from 'vitest'
import { formatarTempoRelativo } from './relativeTime'

const MINUTO_MS = 60_000
const HORA_MS = 60 * MINUTO_MS
const DIA_MS = 24 * HORA_MS

describe('formatarTempoRelativo', () => {
  const agora = new Date('2026-07-19T12:00:00Z').getTime()

  it('menos de 1 minuto -> "agora mesmo"', () => {
    expect(formatarTempoRelativo(agora - 30_000, agora)).toBe('agora mesmo')
  })

  it('minutos -> "há N min"', () => {
    expect(formatarTempoRelativo(agora - 5 * MINUTO_MS, agora)).toBe('há 5 min')
  })

  it('horas -> "há N horas"', () => {
    expect(formatarTempoRelativo(agora - 3 * HORA_MS, agora)).toBe('há 3 horas')
  })

  it('exatamente 1 hora -> "há 1 hora" (singular)', () => {
    expect(formatarTempoRelativo(agora - 1 * HORA_MS, agora)).toBe('há 1 hora')
  })

  it('~1 dia -> "ontem"', () => {
    expect(formatarTempoRelativo(agora - 25 * HORA_MS, agora)).toBe('ontem')
  })

  it('mais de 7 dias -> data absoluta', () => {
    const dez = agora - 10 * DIA_MS
    expect(formatarTempoRelativo(dez, agora)).toBe(new Date(dez).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' }))
  })
})
