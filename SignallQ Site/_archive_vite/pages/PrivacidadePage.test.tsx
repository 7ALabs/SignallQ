import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import PrivacidadePage from './PrivacidadePage'

describe('PrivacidadePage', () => {
  it('reconcilia as 11 seções do protótipo v2 (ScreenDoc.dc.html)', () => {
    render(<PrivacidadePage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /como este site trata seus dados/i })).toBeInTheDocument()
    // Seções que a versão anterior desta página não tinha (achado 3 do README da
    // reconstrução v2) -- confirmam que o conteúdo foi reconciliado, não só o layout.
    expect(screen.getByText('Cloudflare Web Analytics')).toBeInTheDocument()
    expect(screen.getByText('Lista de espera do PRO')).toBeInTheDocument()
    expect(screen.getByText(/giammattey\.luiz@gmail\.com/)).toBeInTheDocument()
  })

  it('mostra a data de atualização', () => {
    render(<PrivacidadePage />, { wrapper: MemoryRouter })

    expect(screen.getByText('Última atualização: 18 de julho de 2026')).toBeInTheDocument()
  })
})
