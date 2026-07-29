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

const SITE_TESTE_URL = 'https://signallq.pages.dev/teste'

// Só orienta a jornada (etapa 1 concluída → destacar etapa 2) — não valida
// participação real no Google Groups, o que não é possível a partir do frontend.
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

const SCREENSHOTS = [
  {
    src: '/teste/01-home.png',
    alt: 'Tela inicial do SignallQ, com a velocidade medida mais recente, atalhos para medir agora, DNS, ping e diagnóstico, e o status do Wi-Fi e da rede móvel.',
  },
  {
    src: '/teste/02-speedtest-rodando.png',
    alt: 'Teste de velocidade em andamento no SignallQ, com o velocímetro medindo o download em tempo real.',
  },
  {
    src: '/teste/03-speedtest-resultado.png',
    alt: 'Resultado do teste de velocidade do SignallQ, com download, upload, latência e bufferbloat, além da avaliação para streaming, jogos e videochamada.',
  },
  {
    src: '/teste/05-canal-wifi.png',
    alt: 'Análise do canal de Wi-Fi no SignallQ, mostrando o congestionamento por canal e a recomendação do melhor canal para o roteador.',
  },
  {
    src: '/teste/06-sinal.png',
    alt: 'Tela de sinal do SignallQ, com a força do sinal do Wi-Fi e da rede móvel.',
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

const O_QUE_TESTA = [
  'Velocidade de download e upload',
  'Latência e estabilidade',
  'Qualidade do Wi-Fi',
  'Sinal da rede móvel',
  'Diagnóstico da conexão',
  'Identificação de possíveis problemas na internet',
]

const ANTES_DE_PARTICIPAR = [
  'O aplicativo ainda está em desenvolvimento.',
  'Algumas funções podem apresentar erros.',
  'O teste é gratuito — não é necessário realizar nenhum pagamento.',
  'Você pode sair do grupo quando quiser.',
]

function SectionLabel({ children }: { children: string }) {
  return (
    <div className="label-overline" style={{ color: 'var(--text-tertiary)' }}>
      {children}
    </div>
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

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="teste" />

      <div className="mx-auto flex w-full max-w-[860px] flex-1 flex-col gap-10 px-5 py-8 box-border lg:px-6 lg:py-10">
        {/* Hero */}
        <section className="flex flex-col gap-3 sq-fade-up">
          <SectionLabel>Teste fechado · Android</SectionLabel>
          <h1 className="m-0 text-pretty text-[28px] font-bold leading-[1.2] lg:text-[34px]" style={{ color: 'var(--text-primary)' }}>
            Preciso da sua ajuda para testar o SignallQ
          </h1>
          <p className="m-0 max-w-[620px] body-medium">
            Estou desenvolvendo um aplicativo brasileiro que ajuda a identificar problemas na internet, no Wi-Fi, na fibra e na rede
            móvel. Antes do lançamento oficial na Play Store, preciso de pessoas dispostas a testar o aplicativo e compartilhar sua
            experiência.
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
        </section>

        {/* Destaque da Etapa 2 — só orienta a jornada, não valida participação real no grupo */}
        {grupoIniciado && (
          <section
            className="flex flex-wrap items-center gap-4 rounded-2xl border p-5 sq-fade-up"
            style={{ background: 'var(--bg-secondary)', borderColor: 'var(--accent)' }}
          >
            <p className="m-0 flex-1 label-large" style={{ color: 'var(--text-primary)' }}>
              Já entrou no grupo? Agora continue para instalar o SignallQ pela Play Store.
            </p>
            <a
              href={SIGNALLQ_CLOSED_TESTING_URL}
              target="_blank"
              rel="noopener noreferrer"
              className="flex h-11 items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
              style={{ background: 'var(--accent)' }}
            >
              <span className="label-large" style={{ color: 'var(--on-accent)' }}>
                Continuar para a Play Store
              </span>
            </a>
          </section>
        )}

        {/* Galeria de screenshots reais */}
        <section className="flex flex-col gap-3">
          <SectionLabel>O aplicativo</SectionLabel>
          <div className="flex gap-4 overflow-x-auto pb-2" style={{ scrollSnapType: 'x mandatory' }}>
            {SCREENSHOTS.map((shot) => (
              <img
                key={shot.src}
                src={shot.src}
                alt={shot.alt}
                className="h-[320px] w-auto flex-shrink-0 rounded-2xl lg:h-[420px]"
                style={{ scrollSnapAlign: 'start' }}
                loading="lazy"
              />
            ))}
          </div>
        </section>

        {/* Por que sua ajuda é importante */}
        <section className="flex flex-col gap-2.5 rounded-2xl p-5" style={{ background: 'var(--bg-secondary)' }}>
          <h2 className="title-large m-0">Por que sua ajuda é importante?</h2>
          <p className="m-0 body-medium">
            O teste fechado é uma etapa obrigatória antes da publicação do aplicativo. Seu uso e seu feedback ajudam a encontrar erros,
            melhorar os textos e validar se o diagnóstico realmente é fácil de entender.
          </p>
          <p className="m-0 body-medium">
            Você não precisa entender de redes. Basta usar o aplicativo normalmente e contar o que funcionou, o que ficou confuso e o
            que pode melhorar.
          </p>
        </section>

        {/* Como participar */}
        <section className="flex flex-col gap-4">
          <h2 className="title-large m-0">Como participar</h2>
          <ol className="m-0 flex list-none flex-col gap-3.5 p-0">
            {PASSOS.map((passo, i) => {
              const destacado = grupoIniciado && i === 1
              return (
                <li
                  key={passo.title}
                  className="flex gap-3.5 rounded-xl p-2 -m-2"
                  style={destacado ? { background: 'var(--bg-secondary)', outline: '1px solid var(--accent)' } : undefined}
                >
                  <span
                    className="flex h-7 w-7 flex-shrink-0 items-center justify-center rounded-full label-medium"
                    style={{ background: 'var(--accent)', color: 'var(--on-accent)' }}
                    aria-hidden="true"
                  >
                    {i + 1}
                  </span>
                  <div className="flex flex-col gap-0.5">
                    <div className="label-large">{passo.title}</div>
                    <p className="m-0 body-medium">{passo.body}</p>
                  </div>
                </li>
              )
            })}
          </ol>
          <a
            href={SIGNALLQ_TEST_GROUP_URL}
            target="_blank"
            rel="noopener noreferrer"
            onClick={marcarGrupoIniciado}
            className="flex h-11 w-fit items-center justify-center rounded-[var(--radius-button)] px-5 no-underline"
            style={{ background: 'var(--accent)' }}
          >
            <span className="label-large" style={{ color: 'var(--on-accent)' }}>
              Entrar no grupo de testadores
            </span>
          </a>
        </section>

        {/* O que o SignallQ testa */}
        <section className="flex flex-col gap-3">
          <h2 className="title-large m-0">O que o SignallQ testa</h2>
          <ul className="m-0 grid grid-cols-1 gap-2.5 p-0 sm:grid-cols-2" style={{ listStyle: 'none' }}>
            {O_QUE_TESTA.map((item) => (
              <li key={item} className="flex items-start gap-2.5">
                <span className="material-symbols-outlined" style={{ fontSize: 18, color: 'var(--accent)' }} aria-hidden="true">
                  check_circle
                </span>
                <span className="body-medium">{item}</span>
              </li>
            ))}
          </ul>
        </section>

        {/* Antes de participar */}
        <section className="flex flex-col gap-3">
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

        {/* CTA final */}
        <section className="flex flex-col gap-4 rounded-2xl p-6" style={{ background: 'var(--bg-secondary)' }}>
          <div className="flex flex-col gap-1.5">
            <h2 className="title-large m-0">Pode me ajudar a testar?</h2>
            <p className="m-0 body-medium">Sua participação ajuda diretamente a colocar o SignallQ na Play Store.</p>
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
                Entrar no grupo
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
                Abrir página do teste
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

            <div className="ml-auto flex flex-col items-center gap-1.5">
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

      <SiteFooter />
    </div>
  )
}
