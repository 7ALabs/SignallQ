import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import ComoMedimosPage from './ComoMedimosPage'

describe('ComoMedimosPage', () => {
  it('rota nova da reconstrução v2 — mostra título e as 5 seções da metodologia', () => {
    render(<ComoMedimosPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /como medimos sua conexão/i })).toBeInTheDocument()
    expect(screen.getByText('O que medimos')).toBeInTheDocument()
    expect(screen.getByText('Limites do teste no navegador')).toBeInTheDocument()
  })

  it('CTA leva pro teste de velocidade', () => {
    render(<ComoMedimosPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('link', { name: /iniciar teste/i })).toHaveAttribute('href', '/')
  })
})
