// Timestamp relativo em PT-BR para os cards de histórico ("há 2 horas",
// "ontem", "há 3 dias") — cai para data absoluta depois de 7 dias, ponto em
// que "relativo" deixa de ser útil pra localizar uma medição.
const MINUTO = 60_000
const HORA = 60 * MINUTO
const DIA = 24 * HORA

export function formatarTempoRelativo(timestamp: number, agora: number = Date.now()): string {
  const diff = Math.max(0, agora - timestamp)
  if (diff < MINUTO) return 'agora mesmo'
  if (diff < HORA) {
    const min = Math.round(diff / MINUTO)
    return `há ${min} min`
  }
  if (diff < DIA) {
    const horas = Math.round(diff / HORA)
    return `há ${horas} ${horas === 1 ? 'hora' : 'horas'}`
  }
  const dias = Math.round(diff / DIA)
  if (dias === 1) return 'ontem'
  if (dias < 7) return `há ${dias} dias`
  return new Date(timestamp).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: '2-digit' })
}
