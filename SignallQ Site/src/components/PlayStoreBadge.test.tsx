import { fireEvent, render, screen } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

vi.mock('../lib/config', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../lib/config')>()),
  SIGNALLQ_TEST_GROUP_URL: 'https://groups.google.com/g/testadores-signallq',
}))

import { PlayStoreBadge } from './PlayStoreBadge'

describe('PlayStoreBadge', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('encaminha para o grupo oficial de testadores sem pedir e-mail', () => {
    const openSpy = vi.spyOn(window, 'open').mockImplementation(() => null)
    render(<PlayStoreBadge source="teste" />)

    fireEvent.click(screen.getByRole('button', { name: /grupo de testes/i }))

    expect(openSpy).toHaveBeenCalledWith('https://groups.google.com/g/testadores-signallq', '_blank', 'noopener,noreferrer')
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
