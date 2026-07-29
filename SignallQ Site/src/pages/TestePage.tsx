import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import { Link } from 'react-router-dom'
import { SiteFooter } from '../components/SiteFooter'
import { SiteNav } from '../components/SiteNav'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { SIGNALLQ_CLOSED_TESTING_URL, SIGNALLQ_TEST_GROUP_URL } from '../lib/config'
import { PAGE_META } from '../lib/pageMetaCatalog'
import { trackScreenView } from '../lib/telemetry'

// Página de convite para o teste fechado do SignallQ (Android) — pedido de ajuda
// genuíno pré-lançamento, não campanha de lançamento oficial (o app ainda não
// está publicado). Composição própria (não usa `PageLayout`/`DocPage`, que são o
// template genérico das páginas institucionais/editoriais) porque o foco aqui é
// conversão para dois destinos externos fixos (grupo de testadores e teste
// fechado da Play Store), não texto corrido em seções.
//
// Sem `AdRail`/`AdBannerWide` nesta rota especificamente — decisão deliberada:
// o objetivo da página é ajuda genuína ao teste, e o espaço de anúncio
// competiria visualmente com os dois CTAs que realmente importam aqui.
//
// Reformulação 2026-07-29 (Marina, crítica de design/UX do Luiz sobre a versão
// em produção): menos texto, menos "caixinha atrás de caixinha", hero com
// screenshot real, sinal de "já entrou no grupo" concentrado em um único lugar
// (a seção "Como participar", não mais um card solto), fluxo de participação
// explicado uma única vez. Ver PR #1509 para o detalhe completo por item.
// `SiteFooter` recebe `variant="minimal"` só nesta rota (ver componente).

const SITE_TESTE_URL = 'https://signallq.pages.dev/teste'

// Só orienta a jornada (etapa 1 concluída → destacar etapa 2 e trocar o CTA
// da seção "Como participar" para o próximo destino) — não valida participação
// real no Google Groups, o que não é possível a partir do frontend.
const GRUPO_INICIADO_STORAGE_KEY = 'signallq_teste_grupo_iniciado'

function marcarGrupoIniciado() {
  try {
    localStorage.setItem(GRUPO_INICIADO_STORAGE_KEY, '1')
  } catch {
    // localStorage indisponível (ex. modo privado) — não bloqueia a navegação
  }
}

const SHARE_TEXT =
  '📶 Estou procurando pessoas para testar o SignallQ, um aplicativo Android de diagnóstico de internet, Wi-Fi, fibra e rede móvel. Você pode ajudar entrando pelo link: https://signallq.pages.dev/teste'

// Screenshot do hero — a única imagem do produto na primeira dobra (antes só
// havia texto + botões). Início foi escolhida por comunicar o produto como um
// todo (caminho da conexão, velocidade, Wi-Fi) já na primeira vista.
const HERO_SCREENSHOT = {
  src: '/teste/01-home-dark.png',
  alt: 'Tela Início do SignallQ em modo escuro, mostrando o caminho da conexão entre aparelho, roteador e provedor, o resultado da última medição de velocidade e o status do Wi-Fi.',
}

// Galeria reduzida de 7 para 4 imagens (1 principal maior + 3 secundárias) —
// prioriza demonstrar o produto em vez de só provar que ele existe. A
// principal mostra o resultado do teste (o valor central do app); as
// secundárias cobrem os dois diferenciais que a concorrência não tem (Modo
// Gamer e Diagnóstico assistido por IA) mais a análise de canal Wi-Fi.
const GALLERY_MAIN = {
  src: '/teste/03-speedtest-resultado-dark.png',
  alt: 'Resultado do teste de velocidade do SignallQ em modo escuro, com download, upload e a avaliação de qualidade da internet.',
  caption: 'Resultado do teste de velocidade, com download, upload e qualidade da conexão.',
}

const GALLERY_SECONDARY = [
  {
    src: '/teste/06-modo-gamer.png',
    alt: 'Diagnóstico do Modo Gamer do SignallQ para Free Fire no Android, com latência, jitter e download medidos pelo motor SignallQ e a explicação gerada por IA sobre a qualidade da conexão para jogar.',
    caption: 'Modo Gamer — diagnóstico de latência para jogos online.',
  },
  {
    src: '/teste/07-diagnostico-ia.png',
    alt: 'Diagnóstico guiado do SignallQ assistido por IA, com perda de pacotes e oscilação medidas pelo motor local e a explicação gerada por IA sobre a causa da instabilidade.',
    caption: 'Diagnóstico assistido por IA explica a causa da instabilidade.',
  },
  {
    src: '/teste/04-canal-wifi.png',
    alt: 'Análise do canal de Wi-Fi no SignallQ, com o gráfico de intensidade por canal na faixa 2.4GHz e a recomendação de migrar para o canal mais livre.',
    caption: 'Canal de Wi-Fi, com recomendação do canal mais livre.',
  },
]

const PASSOS = [
  {
    title: 'Entre no grupo de testadores',
    body: 'Acesse o grupo oficial usando a mesma conta Google utilizada no seu celular Android.',
  },
  {
    title: 'Acesse o teste fechado',
    body: 'Depois de entrar no grupo, abra a página de teste do SignallQ na Play Store.',
  },
  {
    title: 'Instale e use o aplicativo',
    body: 'Teste as funções normalmente, principalmente velocidade, diagnóstico, Wi-Fi, fibra e rede móvel.',
  },
  {
    title: 'Compartilhe sua experiência',
    body: 'Informe erros, dificuldades ou sugestões diretamente no grupo de testadores.',
  },
]

const POR_QUE_AJUDAR = [
  {
    title: 'Teste em aparelhos reais',
    body: 'Ajuda a identificar problemas que não aparecem no ambiente de desenvolvimento.',
  },
  {
    title: 'Feedback direto',
    body: 'Você pode dizer o que está confuso, ruim ou faltando.',
  },
  {
    title: 'Melhoria antes do lançamento',
    body: 'Os testes ajudam a corrigir falhas antes da publicação oficial.',
  },
]

// Reagrupado de 6 itens soltos para 4 blocos temáticos (item G da revisão).
const O_QUE_TESTA = [
  { title: 'Velocidade', body: 'Download, upload e latência.' },
  { title: 'Wi-Fi', body: 'Sinal, qualidade e estabilidade.' },
  { title: 'Rede móvel', body: 'Qualidade da conexão.' },
  { title: 'Diagnóstico', body: 'Possíveis causas de lentidão ou falhas.' },
]

const ANTES_DE_PARTICIPAR = [
  'O aplicativo ainda está em desenvolvimento.',
  'Algumas telas podem apresentar erros.',
  'O teste é gratuito.',
  'Não há cobrança em nenhuma etapa.',
  'Você pode sair do grupo quando quiser.',
]

function SectionLabel({ children }: { children: string }) {
  return (
    <div className="label-overline" style={{ color: 'var(--text-tertiary)' }}>
      {children}
    </div>
  )
}

// Moldura de smartphone em CSS puro — as screenshots reais desta galeria são
// cruas (sem bezel desenhado na própria imagem, ao contrário do release
// anterior). Bezel fixo escuro (não segue o tema claro/escuro do site,
// igual a um chassi de aparelho real) com raio e proporção inspirados no
// `PhoneFrame` do design system, mas sem importar o componente React do
// pacote (`@signallq/design-system`) — decisão de arquitetura do site (ver
// `CLAUDE.md` local): o site consome o design system só via CSS.
// `className` controla a altura (hero, galeria principal e secundária usam
// tamanhos diferentes) sem precisar de três componentes quase idênticos.
function PhoneShot({ src, alt, className }: { src: string; alt: string; className?: string }) {
  return (
    <div
      className={`flex-shrink-0 rounded-[30px] p-[6px] shadow-[0_18px_40px_-14px_rgba(0,0,0,0.5)] ${className ?? 'h-[320px] lg:h-[420px]'}`}
      style={{ background: '#16181d', scrollSnapAlign: 'start' }}
    >
      <img src={src} alt={alt} className="h-full w-auto rounded-[22px]" loading="lazy" />
    </div>
  )
}

// Acento de marca (violeta → azul, tokens `--accent`/`--accent-blue` — não
// existe um terceiro tom ciano no design system oficial, ver `brand/README.md`,
// então o gradiente usa só os dois tons documentados) — decorativo, atrás do
// bloco de texto do hero e do CTA final, com moderação (blur + opacidade
// baixa, contido por `overflow-hidden` no container).
function AccentGlow({ className }: { className: string }) {
  return (
    <div
      aria-hidden="true"
      className={`pointer-events-none absolute -z-10 rounded-full opacity-60 blur-3xl ${className}`}
      style={{ background: 'linear-gradient(135deg, var(--accent), var(--accent-blue))' }}
    />
  )
}

export default function TestePage() {
  useDocumentMeta(PAGE_META['/teste'])
  const [copyStatus, setCopyStatus] = useState<'idle' | 'copiado' | 'erro'>('idle')
  const [grupoIniciado, setGrupoIniciado] = useState(false)

  useEffect(() => {
    trackScreenView('teste')
  }, [])

  useEffect(() => {
    try {
      setGrupoIniciado(localStorage.getItem(GRUPO_INICIADO_STORAGE_KEY) === '1')
    } catch {
      // localStorage indisponível — mantém o estado padrão (etapa 1 não destacada)
    }
  }, [])

  useEffect(() => {
    if (copyStatus === 'idle') return
    const timeout = setTimeout(() => setCopyStatus('idle'), 3000)
    return () => clearTimeout(timeout)
  }, [copyStatus])

  const handleShare = async () => {
    if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
      try {
        await navigator.share({ text: SHARE_TEXT })
      } catch {
        // usuário cancelou o compartilhamento nativo — não é um erro a reportar
      }
      return
    }
    try {
      await navigator.clipboard.writeText(SHARE_TEXT)
      setCopyStatus('copiado')
    } catch {
      setCopyStatus('erro')
    }
  }

  // Sinal de "já entrou no grupo" concentrado num único lugar (item B da
  // revisão): antes existia um card flutuante entre o hero e a galeria — agora
  // só reflete no passo 2 e no CTA da seção "Como participar" abaixo.
  const passoParticiparCta = grupoIniciado
    ? { label: 'Continuar para a Play Store', href: SIGNALLQ_CLOSED_TESTING_URL, onClick: undefined }
    : { label: 'Entrar no grupo de testadores', href: SIGNALLQ_TEST_GROUP_URL, onClick: marcarGrupoIniciado }

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="teste" />

      <div className="mx-auto flex w-full max-w-[960px] flex-1 flex-col gap-14 px-5 py-8 box-border lg:px-6 lg:py-12">
        {/* Hero */}
        <section className="relative flex flex-col gap-6 overflow-hidden sq-fade-up lg:flex-row lg:items-center lg:gap-12">
          <AccentGlow className="-top-16 -right-16 h-64 w-64 lg:-top-24 lg:-right-10 lg:h-96 lg:w-96" />

          <div className="flex flex-1 flex-col gap-3.5">
            <SectionLabel>Teste fechado · Android</SectionLabel>
            <h1 className="m-0 text-pretty text-[32px] font-bold leading-[1.12] lg:text-[46px]" style={{ color: 'var(--text-primary)' }}>
              Preciso da sua ajuda para testar o SignallQ
            </h1>
            <p className="m-0 max-w-[480px] body-medium">
              O SignallQ ajuda a identificar problemas na internet, no Wi-Fi, na fibra e na rede móvel. Entre no teste fechado e ajude a
              melhorar o app antes do lançamento.
            </p>

            <div className="flex flex-wrap gap-3 pt-1">
              <a
                href={SIGNALLQ_TEST_GROUP_URL}
                target="_blank"
                rel="noopener noreferrer"
                onClick={marcarGrupoIniciado}
                className="flex h-12 items-center justify-center rounded-[var(--radius-button)] px-6 no-underline"
                style={{ background: 'var(--accent)' }}
              >
                <span className="label-large" style={{ color: 'var(--on-accent)' }}>
                  Quero participar do teste
                </span>
              </a>
              <a
                href={SIGNALLQ_CLOSED_TESTING_URL}
                target="_blank"
                rel="noopener noreferrer"
                className="flex h-12 items-center justify-center rounded-[var(--radius-button)] border px-6 no-underline"
                style={{ borderColor: 'var(--border)', color: 'var(--text-primary)' }}
              >
                <span className="label-large" style={{ color: 'var(--text-primary)' }}>
                  Já estou no grupo
                </span>
              </a>
            </div>
            <p className="m-0 body-small" style={{ color: 'var(--text-tertiary)' }}>
              O teste é gratuito e está disponível para Android.
            </p>
          </div>

          <div className="flex justify-center lg:justify-end">
            <PhoneShot src={HERO_SCREENSHOT.src} alt={HERO_SCREENSHOT.alt} className="h-[240px] sm:h-[280px] lg:h-[380px]" />
          </div>
        </section>

        {/* Galeria de screenshots reais — 1 imagem principal + 3 secundárias com legenda */}
        <section className="flex flex-col gap-5">
          <SectionLabel>O aplicativo</SectionLabel>
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center">
            <figure className="m-0 flex flex-1 flex-col items-center gap-2.5 lg:items-start">
              <PhoneShot src={GALLERY_MAIN.src} alt={GALLERY_MAIN.alt} className="h-[320px] lg:h-[440px]" />
              <figcaption className="body-small max-w-[320px] text-center lg:text-left" style={{ color: 'var(--text-tertiary)' }}>
                {GALLERY_MAIN.caption}
              </figcaption>
            </figure>
            <div className="flex gap-4 overflow-x-auto pb-1 lg:w-[240px] lg:flex-col lg:overflow-visible" style={{ scrollSnapType: 'x mandatory' }}>
              {GALLERY_SECONDARY.map((shot) => (
                <figure key={shot.src} className="m-0 flex flex-col items-center gap-1.5 lg:items-start">
                  <PhoneShot src={shot.src} alt={shot.alt} className="h-[200px] lg:h-[220px]" />
                  <figcaption className="body-small max-w-[170px] text-center lg:text-left" style={{ color: 'var(--text-tertiary)' }}>
                    {shot.caption}
                  </figcaption>
                </figure>
              ))}
            </div>
          </div>
        </section>

        {/* Por que sua ajuda é importante — 3 blocos curtos (era 1 card com 2 parágrafos) */}
        <section className="flex flex-col gap-5">
          <h2 className="title-large m-0">Por que sua ajuda é importante?</h2>
          <div className="grid grid-cols-1 gap-6 sm:grid-cols-3">
            {POR_QUE_AJUDAR.map((item) => (
              <div key={item.title} className="flex flex-col gap-1">
                <div className="label-large" style={{ color: 'var(--accent)' }}>
                  {item.title}
                </div>
                <p className="m-0 body-medium">{item.body}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Como participar — único lugar que explica o fluxo. Cards simples e
            iguais entre si (sem linha vertical, sem círculo numerado, sem
            moldura fixa) — o único realce é condicional (passo 2, quando
            grupoIniciado). CTA único ao final. */}
        <section className="flex flex-col gap-5">
          <h2 className="title-large m-0">Como participar</h2>
          <ul className="m-0 grid grid-cols-1 gap-4 p-0 sm:grid-cols-2" style={{ listStyle: 'none' }}>
            {PASSOS.map((passo, i) => {
              const destacado = grupoIniciado && i === 1
              return (
                <li
                  key={passo.title}
                  className="flex flex-col gap-1.5 rounded-2xl p-5"
                  style={{ background: 'var(--bg-secondary)', ...(destacado ? { outline: '1px solid var(--accent)' } : {}) }}
                >
                  <div className="label-overline" style={{ color: 'var(--accent)' }}>
                    Passo {i + 1}
                  </div>
                  <div className="label-large">{passo.title}</div>
                  <p className="m-0 body-medium">{passo.body}</p>
                </li>
              )
            })}
          </ul>
          <a
            href={passoParticiparCta.href}
            target="_blank"
            rel="noopener noreferrer"
            onClick={passoParticiparCta.onClick}
            className="flex h-11 w-fit items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
            style={{ background: 'var(--accent)' }}
          >
            <span className="label-large" style={{ color: 'var(--on-accent)' }}>
              {passoParticiparCta.label}
            </span>
          </a>
        </section>

        {/* O que o SignallQ testa — 4 blocos temáticos (era lista de 6 itens soltos) */}
        <section className="flex flex-col gap-5">
          <h2 className="title-large m-0">O que o SignallQ testa</h2>
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {O_QUE_TESTA.map((item) => (
              <div key={item.title} className="flex flex-col gap-1">
                <div className="label-large">{item.title}</div>
                <p className="m-0 body-medium">{item.body}</p>
              </div>
            ))}
          </div>
        </section>

        {/* Antes de participar */}
        <section className="flex flex-col gap-4">
          <h2 className="title-large m-0">Antes de participar</h2>
          <ul className="m-0 flex flex-col gap-2.5 p-0" style={{ listStyle: 'none' }}>
            {ANTES_DE_PARTICIPAR.map((item) => (
              <li key={item} className="flex items-start gap-2.5">
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--text-tertiary)' }} aria-hidden="true">
                  info
                </span>
                <span className="body-medium">{item}</span>
              </li>
            ))}
            <li className="flex items-start gap-2.5">
              <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--text-tertiary)' }} aria-hidden="true">
                info
              </span>
              <span className="body-medium">
                Os dados são tratados conforme a{' '}
                <Link to="/privacidade" style={{ color: 'var(--accent)' }}>
                  política de privacidade
                </Link>{' '}
                já publicada pelo SignallQ.
              </span>
            </li>
          </ul>
        </section>

        {/* CTA final — repete só o CTA, não a explicação do fluxo */}
        <section className="relative flex flex-col gap-4 overflow-hidden rounded-[28px] p-7" style={{ background: 'var(--bg-secondary)' }}>
          <AccentGlow className="-bottom-20 -left-20 h-64 w-64" />

          <div className="flex flex-col gap-1.5">
            <h2 className="title-large m-0">Ajude a preparar o SignallQ para o lançamento</h2>
            <p className="m-0 body-medium">Entre no grupo, instale o app e compartilhe sua experiência.</p>
          </div>
          <div className="flex flex-wrap gap-3">
            <a
              href={SIGNALLQ_TEST_GROUP_URL}
              target="_blank"
              rel="noopener noreferrer"
              onClick={marcarGrupoIniciado}
              className="flex h-11 items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
              style={{ background: 'var(--accent)' }}
            >
              <span className="label-large" style={{ color: 'var(--on-accent)' }}>
                Entrar no grupo de testadores
              </span>
            </a>
            <a
              href={SIGNALLQ_CLOSED_TESTING_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex h-11 items-center justify-center rounded-[var(--radius-button)] border px-5 no-underline"
              style={{ borderColor: 'var(--border)', color: 'var(--text-primary)' }}
            >
              <span className="label-large" style={{ color: 'var(--text-primary)' }}>
                Já participo — abrir Play Store
              </span>
            </a>
          </div>

          <div className="flex flex-wrap items-center gap-4 border-t pt-4" style={{ borderColor: 'color-mix(in srgb, var(--border) 25%, transparent)' }}>
            <div className="flex flex-col gap-2">
              <button
                type="button"
                onClick={handleShare}
                className="flex h-10 w-fit items-center gap-2 rounded-[var(--radius-button)] border px-4"
                style={{ borderColor: 'var(--border)', color: 'var(--text-primary)', background: 'transparent' }}
              >
                <span className="material-symbols-outlined" style={{ fontSize: 18 }} aria-hidden="true">
                  ios_share
                </span>
                <span className="label-large">Compartilhar convite</span>
              </button>
              <span role="status" aria-live="polite" className="body-small" style={{ color: 'var(--accent)', minHeight: '1em' }}>
                {copyStatus === 'copiado' && 'Link copiado'}
                {copyStatus === 'erro' && 'Não foi possível copiar o link'}
              </span>
            </div>

            {/* QR code é praticamente inútil no celular (a pessoa já está vendo
                a página no próprio aparelho) — só a partir do breakpoint md,
                priorizando "Compartilhar convite" abaixo disso. */}
            <div className="ml-auto hidden flex-col items-center gap-1.5 md:flex">
              <div className="rounded-xl bg-white p-2">
                <QRCodeSVG value={SITE_TESTE_URL} size={88} aria-label={`QR code para ${SITE_TESTE_URL}`} />
              </div>
              <span className="body-small max-w-[140px] text-center" style={{ color: 'var(--text-tertiary)' }}>
                Escaneie para abrir o convite no celular
              </span>
            </div>
          </div>
        </section>
      </div>

      <SiteFooter variant="minimal" />
    </div>
  )
}
