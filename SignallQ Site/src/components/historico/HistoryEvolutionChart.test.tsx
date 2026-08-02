import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { HistoryEvolutionChart } from './HistoryEvolutionChart'
import type { MedicaoRegistro } from '../../lib/historyStore'

const AGORA = new Date('2026-07-25T12:00:00Z').getTime()
const HORA = 60 * 60 * 1000

function registro(overrides: Partial<MedicaoRegistro>): MedicaoRegistro {
  return {
    id: 'r',
    timestamp: AGORA,
    download: 50,
    upload: 20,
    latency: 20,
    jitter: null,
    connectionType: '4g',
    connectionKind: 'wifi',
    server: 'sp',
    ...overrides,
  }
}

describe('HistoryEvolutionChart', () => {
  it('não renderiza nada com menos de 2 medições (linha não faz sentido com 1 ponto só)', () => {
    const { container } = render(<HistoryEvolutionChart records={[registro({ id: 'a' })]} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('não renderiza nada sem histórico', () => {
    const { container } = render(<HistoryEvolutionChart records={[]} />)
    expect(container).toBeEmptyDOMElement()
  })

  it('renderiza o gráfico com legenda Download/Upload e o SVG com um ponto por medição', () => {
    const records = [
      registro({ id: 'a', timestamp: AGORA - 2 * HORA, download: 40, upload: 10 }),
      registro({ id: 'b', timestamp: AGORA - HORA, download: 80, upload: 30 }),
      registro({ id: 'c', timestamp: AGORA, download: 60, upload: 20 }),
    ]
    render(<HistoryEvolutionChart records={records} />)

    expect(screen.getByText('Evolução das medições')).toBeInTheDocument()
    expect(screen.getByText('Download')).toBeInTheDocument()
    expect(screen.getByText('Upload')).toBeInTheDocument()

    const svg = screen.getByRole('img', { name: /gráfico de evolução/i })
    expect(svg.querySelectorAll('circle').length).toBe(records.length * 2) // download + upload dots
  })
})
