import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import CgnatPage from './CgnatPage'

describe('CgnatPage', () => {
  it('explica lag em jogos online e CGNAT como causa', () => {
    render(<CgnatPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /lag em jogos online/i })).toBeInTheDocument()
    expect(screen.getAllByText(/CGNAT/).length).toBeGreaterThan(0)
  })

  it('linka pro teste de velocidade e pra página de bufferbloat', () => {
    render(<CgnatPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('link', { name: /testar minha conexão/i })).toHaveAttribute('href', '/')
    expect(screen.getByRole('link', { name: /internet boa mas travando e o bufferbloat/i })).toHaveAttribute('href', '/internet-boa-mas-travando')
  })
})
