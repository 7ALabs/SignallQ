import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { AdBanner } from './AdBanner'

describe('AdBanner', () => {
  it('sem AdSense configurado -> mostra aviso honesto de espaço reservado, sem affordance falsa', () => {
    render(<AdBanner />)
    expect(screen.getByText('Espaço para anúncio')).toBeInTheDocument()
    expect(screen.getByText('PUBLICIDADE')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
