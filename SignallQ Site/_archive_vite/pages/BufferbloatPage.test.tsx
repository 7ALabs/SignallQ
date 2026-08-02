import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import BufferbloatPage from './BufferbloatPage'

describe('BufferbloatPage', () => {
  it('ancora o H1 na frase sintomática, não no termo técnico', () => {
    render(<BufferbloatPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /internet boa mas travando/i })).toBeInTheDocument()
  })

  it('explica bufferbloat como causa dentro do texto', () => {
    render(<BufferbloatPage />, { wrapper: MemoryRouter })

    expect(screen.getAllByText(/bufferbloat/i).length).toBeGreaterThan(0)
  })

  it('linka pro teste de velocidade e pra página de CGNAT', () => {
    render(<BufferbloatPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('link', { name: /testar minha conexão/i })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: /lag em jogos online e o cgnat/i })).toHaveAttribute('href', '/lag-em-jogos-online')
  })
})
