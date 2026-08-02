import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import ProPage from './ProPage'

// Reconstrução v2 (Feat #1402/#1403) — cobre a composição SiteNav → AdRail(a) + conteúdo
// + AdRail(b) → AdBannerWide → SiteFooter e o modal de lista de espera. AdRail/AdBannerWide
// buscam o catálogo local via fetch (useAdFallback/localAdsClient) — mockado sem anúncios
// pra não depender de rede real nem acoplar este teste ao conteúdo do catálogo.
describe('ProPage', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(JSON.stringify({ ads: [] }), { status: 200 }))
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renderiza o hero com paleta PRO (azul), não a violeta do consumer', () => {
    render(<ProPage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /sua visita técnica merece um resultado/i })).toBeInTheDocument()
    expect(screen.getAllByText('SignallQ PRO').length).toBeGreaterThan(0)
  })

  it('mostra as três seções de proposta (dores, para quem é, o que o PRO entrega) e o fluxo/comparativo/planos — scroll único, sem paginação', () => {
    render(<ProPage />, { wrapper: MemoryRouter })

    expect(screen.getByText('Sem o PRO, hoje')).toBeInTheDocument()
    expect(screen.getByText('Para quem é')).toBeInTheDocument()
    expect(screen.getByText('O que o PRO entrega')).toBeInTheDocument()
    expect(screen.getByText('Como funciona na prática')).toBeInTheDocument()
    expect(screen.getByText('Gratuito × PRO')).toBeInTheDocument()
    expect(screen.getAllByText('Em breve').length).toBe(2)
    expect(screen.getByText('Seu próximo atendimento pode ser o primeiro com laudo profissional.')).toBeInTheDocument()
  })

  it('abre o modal de lista de espera ao clicar em "Entrar na lista de espera" e desabilita o CTA até preencher e-mail válido', () => {
    render(<ProPage />, { wrapper: MemoryRouter })

    fireEvent.click(screen.getAllByText('Entrar na lista de espera')[0])

    expect(screen.getByRole('dialog', { name: /o pro chega em breve/i })).toBeInTheDocument()
    const submitButton = screen.getByRole('button', { name: /quero ser avisado/i })
    expect(submitButton).toBeDisabled()

    fireEvent.change(screen.getByPlaceholderText('seu@email.com'), { target: { value: 'tecnico@exemplo.com' } })
    expect(submitButton).not.toBeDisabled()
  })

  it('modal tem opção de fechar e de usar o gratuito', () => {
    render(<ProPage />, { wrapper: MemoryRouter })

    fireEvent.click(screen.getAllByText('Entrar na lista de espera')[0])

    expect(screen.getByRole('button', { name: 'Fechar' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Usar o gratuito' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: 'Fechar' }))
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })

  it('renderiza o AdBannerWide (variant b) antes do rodapé', async () => {
    render(<ProPage />, { wrapper: MemoryRouter })

    // getByLabelText('Publicidade') deixou de ser único depois que o AdRail passou a
    // sempre montar a coluna (achado da Marina/auditoria 1:1 de 2026-07-25) — agora há
    // 2 <aside aria-label="Publicidade"> (AdRail a/b) além do <AdBannerWide>. A asserção
    // usa o texto de fallback específico do AdBannerWide para não colidir com o AdRail.
    await waitFor(() => expect(screen.getAllByText('Banner simulado 320×50').length).toBeGreaterThan(0))
    expect(screen.getAllByLabelText('Publicidade').length).toBe(3)
  })
})
