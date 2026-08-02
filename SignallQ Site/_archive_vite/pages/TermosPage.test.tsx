import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import TermosPage from './TermosPage'

describe('TermosPage', () => {
  it('reconcilia as 11 seções numeradas do protótipo v2 (ScreenDoc.dc.html)', () => {
    render(<TermosPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /termos de uso do site signallq/i })).toBeInTheDocument()
    expect(screen.getByText('1. Aceitação dos termos')).toBeInTheDocument()
    expect(screen.getByText('11. Contato')).toBeInTheDocument()
    expect(screen.getByText(/suporte@signallq\.com/)).toBeInTheDocument()
  })
})
