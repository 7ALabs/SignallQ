import { render, screen } from '@testing-library/react'

import { describe, expect, it } from 'vitest'
import { DocPage } from './DocPage'

describe('DocPage', () => {
  it('renderiza overline, título, intro e seções', () => {
    render(
      <DocPage overline="Overline" title="Título" intro="Intro" sections={[{ title: 'Seção 1', text: 'Texto 1' }]} />,
      { wrapper: MemoryRouter }
    )

    expect(screen.getByText('Overline')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 1, name: 'Título' })).toBeInTheDocument()
    expect(screen.getByText('Intro')).toBeInTheDocument()
    expect(screen.getByRole('heading', { level: 2, name: 'Seção 1' })).toBeInTheDocument()
  })

  it('sem CTA não renderiza link nenhum', () => {
    render(<DocPage overline="Overline" title="Título" sections={[{ title: 'Seção', text: 'Texto' }]} />, { wrapper: MemoryRouter })

    expect(screen.queryByRole('link')).not.toBeInTheDocument()
  })

  it('com CTA renderiza o link pro destino informado', () => {
    render(
      <DocPage overline="Overline" title="Título" sections={[{ title: 'Seção', text: 'Texto' }]} ctaLabel="Ir" ctaTo="/destino" />,
      { wrapper: MemoryRouter }
    )

    expect(screen.getByRole('link', { name: 'Ir' })).toHaveAttribute('href', '/destino')
  })

  it('renderiza a data de atualização quando informada', () => {
    render(
      <DocPage overline="Overline" title="Título" updated="Atualizado em X" sections={[{ title: 'Seção', text: 'Texto' }]} />,
      { wrapper: MemoryRouter }
    )

    expect(screen.getByText('Atualizado em X')).toBeInTheDocument()
  })
})

