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
    // Existe em dois lugares agora (CTA da seção "Como participar" + CTA final,
    // item C/I da revisão) — ambos devem apontar para o mesmo destino.
    const ctasEntrarNoGrupo = screen.getAllByRole('link', { name: /entrar no grupo de testadores/i })
    expect(ctasEntrarNoGrupo.length).toBeGreaterThan(0)
    ctasEntrarNoGrupo.forEach((cta) => expect(cta).toHaveAttribute('href', 'https://groups.google.com/g/testadores-signallq'))
  })

  it('linka para a política de privacidade existente em "Antes de participar"', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    // SiteFooter também linka pra /privacidade — escopo pro item da lista
    // "Antes de participar" pra não colidir com esse outro link.
    const item = screen.getByText(/os dados são tratados conforme a/i).closest('li')
    expect(item).not.toBeNull()
    expect(within(item as HTMLElement).getByRole('link', { name: /política de privacidade/i })).toHaveAttribute('href', '/privacidade')
  })

  it('renderiza a screenshot real do produto já no hero (primeira dobra)', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    const hero = screen.getByAltText(/tela início do signallq em modo escuro/i)
    expect(hero).toHaveAttribute('src', '/teste/01-home-dark.png')
  })

  it('renderiza a galeria com 1 screenshot principal e 3 secundárias com legenda visível, incluindo os diferenciais Modo Gamer e IA', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    const principal = screen.getByAltText(/resultado do teste de velocidade do signallq em modo escuro/i)
    expect(principal).toHaveAttribute('src', '/teste/03-speedtest-resultado-dark.png')
    expect(screen.getByText(/resultado do teste de velocidade, com download, upload e qualidade da conexão/i)).toBeInTheDocument()

    const modoGamer = screen.getByAltText(/diagnóstico do modo gamer do signallq para free fire/i)
    expect(modoGamer).toHaveAttribute('src', '/teste/06-modo-gamer.png')
    expect(screen.getByText(/modo gamer — diagnóstico de latência para jogos online/i)).toBeInTheDocument()

    const diagnosticoIa = screen.getByAltText(/diagnóstico guiado do signallq assistido por ia/i)
    expect(diagnosticoIa).toHaveAttribute('src', '/teste/07-diagnostico-ia.png')
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

  it('não destaca a Etapa 2 na primeira visita (sem registro local de entrada no grupo) — CTA único aponta para o grupo', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.queryByText(/já entrou no grupo/i)).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /continuar para a play store/i })).not.toBeInTheDocument()
  })

  it('clicar em qualquer CTA de entrar no grupo grava a etapa 1 localmente, sem validar participação real', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    fireEvent.click(screen.getByRole('link', { name: /quero participar do teste/i }))

    expect(localStorage.getItem('signallq_teste_grupo_iniciado')).toBe('1')
  })

  it('ao voltar para a landing com a etapa 1 já registrada, o CTA único de "Como participar" vira "Continuar para a Play Store"', () => {
    localStorage.setItem('signallq_teste_grupo_iniciado', '1')

    render(<TestePage />, { wrapper: MemoryRouter })

    const continuarLink = screen.getByRole('link', { name: /continuar para a play store/i })
    expect(continuarLink).toHaveAttribute('href', 'https://play.google.com/apps/testing/io.signallq.app')
    // não existe mais um card solto repetindo a explicação do fluxo
    expect(screen.queryByText(/já entrou no grupo/i)).not.toBeInTheDocument()
  })

  it('usa a variante minimal do SiteFooter (sem os links editoriais "Internet boa mas travando"/"Lag em jogos online")', () => {
    render(<TestePage />, { wrapper: MemoryRouter })

    expect(screen.queryByRole('link', { name: /internet boa mas travando/i })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: /lag em jogos online/i })).not.toBeInTheDocument()
    // "Quem somos" também existe no SiteNav — aqui só confirmamos que o rodapé
    // minimal contém os links institucionais mínimos pedidos (não duplicamos
    // a asserção de exclusividade, que pertence ao teste do SiteNav).
    expect(screen.getAllByRole('link', { name: /quem somos/i }).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('link', { name: /política de privacidade/i }).length).toBeGreaterThan(0)
  })
})
