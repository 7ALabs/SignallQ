import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import HistoricoPage from './HistoricoPage'
import * as historyStore from '../lib/historyStore'
import type { MedicaoRegistro } from '../lib/historyStore'

const AGORA = new Date('2026-07-25T12:00:00Z').getTime()
const HORA = 60 * 60 * 1000

function registro(overrides: Partial<MedicaoRegistro>): MedicaoRegistro {
  return {
    id: 'r',
    timestamp: AGORA,
    download: 50,
    upload: 20,
    latency: 20,
    jitter: null,
    connectionType: '4g',
    connectionKind: 'wifi',
    server: 'sp',
    ...overrides,
  }
}

const REGISTROS: MedicaoRegistro[] = [
  registro({ id: 'wifi-1', timestamp: AGORA, connectionKind: 'wifi', download: 92.4 }),
  registro({ id: 'movel-1', timestamp: AGORA - HORA, connectionKind: 'celular', download: 21.5 }),
  registro({ id: 'eth-1', timestamp: AGORA - 4 * 24 * HORA, connectionKind: 'ethernet', download: 188.4 }),
]

function renderPage() {
  return render(
    <MemoryRouter>
      <HistoricoPage />
    </MemoryRouter>
  )
}

// "3 medições salvas" é composto de vários nós de texto (plural condicional via
// JSX) — getByText com string exata não casa texto partido entre elementos.
function getContagemSalva() {
  return screen.getByText((_, node) => node?.textContent === '3 medições salvas')
}

describe('HistoricoPage', () => {
  beforeEach(() => {
    // AdRail/AdBannerWide buscam o catálogo local — nunca resolve nestes testes
    // (o foco é o comportamento da tela de Histórico, não o slot de anúncio).
    vi.spyOn(global, 'fetch').mockImplementation(() => new Promise(() => {}))
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('mostra o estado de carregando enquanto listRecords() está pendente', () => {
    vi.spyOn(historyStore, 'listRecords').mockImplementation(() => new Promise(() => {}))
    renderPage()
    expect(screen.getByText('Carregando histórico…')).toBeInTheDocument()
  })

  it('mostra o estado indisponível quando listRecords() rejeita', async () => {
    vi.spyOn(historyStore, 'listRecords').mockRejectedValue(new Error('indexeddb-unavailable'))
    renderPage()
    await waitFor(() => expect(screen.getByText('Histórico indisponível')).toBeInTheDocument())
    expect(screen.getByText('Tentar novamente')).toBeInTheDocument()
  })

  it('mostra o estado vazio quando não há registros', async () => {
    vi.spyOn(historyStore, 'listRecords').mockResolvedValue([])
    renderPage()
    await waitFor(() => expect(screen.getByText('Nenhuma medição ainda')).toBeInTheDocument())
    expect(screen.getByText('Testar velocidade')).toBeInTheDocument()
  })

  it('lista os registros, mostra o gráfico de evolução e o filtro com Ethernet', async () => {
    vi.spyOn(historyStore, 'listRecords').mockResolvedValue(REGISTROS)
    renderPage()

    await waitFor(() => expect(getContagemSalva()).toBeInTheDocument())
    expect(screen.getByText('Todos')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Wi-Fi' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Rede móvel' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Ethernet' })).toBeInTheDocument()
    expect(screen.getByText('Evolução das medições')).toBeInTheDocument()
  })

  it('filtra por Ethernet mostrando só o registro daquele tipo', async () => {
    vi.spyOn(historyStore, 'listRecords').mockResolvedValue(REGISTROS)
    renderPage()

    await waitFor(() => expect(getContagemSalva()).toBeInTheDocument())
    fireEvent.click(screen.getByRole('button', { name: 'Ethernet' }))

    expect(screen.getByText('188.4 Mbps')).toBeInTheDocument()
    expect(screen.queryByText('92.4 Mbps')).not.toBeInTheDocument()
  })

  it('abre o dialog de confirmação ao clicar em "Limpar histórico" e limpa tudo ao confirmar', async () => {
    vi.spyOn(historyStore, 'listRecords').mockResolvedValue(REGISTROS)
    vi.spyOn(historyStore, 'clearAll').mockResolvedValue(undefined)
    renderPage()

    await waitFor(() => expect(getContagemSalva()).toBeInTheDocument())
    fireEvent.click(screen.getByText('Limpar histórico'))

    expect(screen.getByText('Limpar todo o histórico?')).toBeInTheDocument()
    fireEvent.click(screen.getByText('Limpar tudo'))

    await waitFor(() => expect(screen.getByText('Nenhuma medição ainda')).toBeInTheDocument())
    expect(historyStore.clearAll).toHaveBeenCalledTimes(1)
  })

  it('cancela o dialog de limpar sem apagar nada', async () => {
    vi.spyOn(historyStore, 'listRecords').mockResolvedValue(REGISTROS)
    const clearSpy = vi.spyOn(historyStore, 'clearAll')
    renderPage()

    await waitFor(() => expect(getContagemSalva()).toBeInTheDocument())
    fireEvent.click(screen.getByText('Limpar histórico'))
    fireEvent.click(screen.getByText('Cancelar'))

    expect(screen.queryByText('Limpar todo o histórico?')).not.toBeInTheDocument()
    expect(clearSpy).not.toHaveBeenCalled()
    expect(getContagemSalva()).toBeInTheDocument()
  })
})
