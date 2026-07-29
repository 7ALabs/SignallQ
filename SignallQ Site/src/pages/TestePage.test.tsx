import { fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TestePage from './TestePage'

// Página de convite para o teste fechado (Android) — sem AdRail/AdBannerWide
// nesta rota (decisão de foco em conversão), por isso não precisa do mock de
// catálogo de anúncios que ProPage/BufferbloatPage exigem. `trackScreenView`
// ainda chama fetch/sendBeacon (telemetria server-side já existente no site) —
// mockado só pra não depender de rede real no teste.
describe('TestePage', () => {
  beforeEach(() => {
    vi.spyOn(global, 'fetch').mockResolvedValue(new Response(null, { status: 204 }))
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('renderiza o hero com o pedido de ajuda, não como campanha de lançamento', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.getByRole('heading', { level: 1, name: /preciso da sua ajuda para testar o signallq/i })).toBeInTheDocument()
    expect(screen.getByText(/o teste é gratuito e está disponível para android/i)).toBeInTheDocument()
  })

  it('CTA principal abre o grupo de testadores em nova aba, sem afirmar aprovação automática', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    const cta = screen.getByRole('link', { name: /quero participar do teste/i })
    expect(cta).toHaveAttribute('href', 'https://groups.google.com/g/testadores-signallq')
    expect(cta).toHaveAttribute('target', '_blank')
    expect(cta).toHaveAttribute('rel', expect.stringContaining('noopener'))
  })

  it('CTA secundário abre a página de teste fechado da Play Store em nova aba', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    const cta = screen.getByRole('link', { name: /já estou no grupo/i })
    expect(cta).toHaveAttribute('href', 'https://play.google.com/apps/testing/io.signallq.app')
    expect(cta).toHaveAttribute('target', '_blank')
    expect(cta).toHaveAttribute('rel', expect.stringContaining('noopener'))
  })

  it('lista os 4 passos de "Como participar" e o CTA final de entrar no grupo', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.getByText('Entre no grupo de testadores')).toBeInTheDocument()
    expect(screen.getByText('Acesse o teste fechado')).toBeInTheDocument()
    expect(screen.getByText('Instale e use o aplicativo')).toBeInTheDocument()
    expect(screen.getByText('Compartilhe sua experiência')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /entrar no grupo de testadores/i })).toHaveAttribute(
      'href',
      'https://groups.google.com/g/testadores-signallq'
    )
  })

  it('linka para a política de privacidade existente em "Antes de participar"', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    // SiteFooter também linka pra /privacidade — escopo pro item da lista
    // "Antes de participar" pra não colidir com esse outro link.
    const item = screen.getByText(/os dados são tratados conforme a/i).closest('li')
    expect(item).not.toBeNull()
    expect(within(item as HTMLElement).getByRole('link', { name: /política de privacidade/i })).toHaveAttribute('href', '/privacidade')
  })

  it('renderiza as screenshots reais do app com alt descritivo (não genérico)', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    const img = screen.getByAltText(/tela inicial do signallq/i)
    expect(img).toHaveAttribute('src', '/teste/01-home.png')
  })

  it('renderiza o QR code apontando só para a URL pública de /teste', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.getByLabelText('QR code para https://signallq.pages.dev/teste')).toBeInTheDocument()
  })

  it('botão "Compartilhar convite" copia o link com feedback acessível quando não há Web Share API', async () => {
    const writeText = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { clipboard: { writeText } })

    render(<TestePage />, { wrapper: MemoryRouter })

    fireEvent.click(screen.getByRole('button', { name: /compartilhar convite/i }))

    await waitFor(() => expect(writeText).toHaveBeenCalledWith(expect.stringContaining('https://signallq.pages.dev/teste')))
    expect(await screen.findByText('Link copiado')).toBeInTheDocument()
  })

  it('usa a Web Share API quando disponível, em vez do fallback de clipboard', async () => {
    const share = vi.fn().mockResolvedValue(undefined)
    Object.assign(navigator, { share })
    const writeText = vi.fn()
    Object.assign(navigator, { clipboard: { writeText } })

    render(<TestePage />, { wrapper: MemoryRouter })

    fireEvent.click(screen.getByRole('button', { name: /compartilhar convite/i }))

    await waitFor(() => expect(share).toHaveBeenCalledWith({ text: expect.stringContaining('https://signallq.pages.dev/teste') }))
    expect(writeText).not.toHaveBeenCalled()

    // @ts-expect-error -- limpar o stub global entre testes
    delete navigator.share
  })

  it('não destaca a Etapa 2 na primeira visita (sem registro local de entrada no grupo)', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.queryByText(/já entrou no grupo\? agora continue para instalar o signallq/i)).not.toBeInTheDocument()
  })

  it('clicar em qualquer CTA de entrar no grupo grava a etapa 1 localmente, sem validar participação real', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    fireEvent.click(screen.getByRole('link', { name: /quero participar do teste/i }))

    expect(localStorage.getItem('signallq_teste_grupo_iniciado')).toBe('1')
  })

  it('ao voltar para a landing com a etapa 1 já registrada, destaca a Etapa 2 com CTA para a Play Store', () => {
    localStorage.setItem('signallq_teste_grupo_iniciado', '1')

    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.getByText(/já entrou no grupo\? agora continue para instalar o signallq pela play store/i)).toBeInTheDocument()
    const continuarLink = screen.getByRole('link', { name: /continuar para a play store/i })
    expect(continuarLink).toHaveAttribute('href', 'https://play.google.com/apps/testing/io.signallq.app')
  })
})
