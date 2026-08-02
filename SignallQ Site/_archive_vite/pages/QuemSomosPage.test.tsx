import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import QuemSomosPage from './QuemSomosPage'

describe('QuemSomosPage', () => {
  it('mostra as 3 seções do protótipo v2: gratuito, PRO e visão', () => {
    render(<QuemSomosPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /conectividade explicada, não só medida/i })).toBeInTheDocument()
    expect(screen.getByText('O SignallQ gratuito')).toBeInTheDocument()
    expect(screen.getByText('O SignallQ PRO')).toBeInTheDocument()
    expect(screen.getByText('Onde queremos chegar')).toBeInTheDocument()
  })
})
