import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import NotFoundPage from './NotFoundPage'

describe('NotFoundPage', () => {
  it('mostra a mensagem de 404 e o CTA pro teste de velocidade (Screen404.dc.html)', () => {
    render(<NotFoundPage />, { wrapper: MemoryRouter })

    expect(screen.getByText('Erro 404')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: /página não encontrada/i })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /ir para o teste de velocidade/i })).toHaveAttribute('href', '/')
  })
})
