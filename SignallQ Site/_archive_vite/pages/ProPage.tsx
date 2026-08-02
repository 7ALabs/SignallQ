import { Fragment, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { AdBannerWide } from '../components/AdBannerWide'
import { AdRail } from '../components/AdRail'
import { AdSlotsProvider } from '../components/AdSlotsProvider'
import { EmailCaptureDialog } from '../components/EmailCaptureDialog'
import { SiteFooter } from '../components/SiteFooter'
import { SiteNav } from '../components/SiteNav'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { PAGE_META } from '../lib/pageMetaCatalog'
import { FEATURE_PRO_LISTA_ESPERA, trackFeatureUsed, trackScreenView } from '../lib/telemetry'
import { submitWaitlistSignup } from '../lib/waitlist'

// Reconstrução v2 (`.claude/design-specs/2026-07-25-site-webapp-v2/ScreenPro.dc.html`) —
// Feat #1402. O protótipo divide o conteúdo em "parte 1" e "parte 2" só como artifício de
// paginação da galeria de mockups estáticos do Claude Design (prop `part`, cada parte é um
// frame separado dentro da mesma tela de iPhone) — numa página web real não existe essa
// paginação, então as duas partes viram uma única rota `/pro` com scroll contínuo (decisão
// da Lia, sem gate necessário: é a leitura mais natural pra web, e o próprio protótipo já
// remonta as duas partes juntas quando `mobile === true`, ou seja, nunca existiu de fato
// como duas rotas independentes).
const PRO_GRADIENT = 'linear-gradient(135deg, #0B6CFF, #6558E8)'

const PAINS = [
  'Medições ficam soltas no WhatsApp e somem antes do próximo atendimento.',
  'O cliente vê um número na tela, mas não entende o que você fez.',
  'Trabalho técnico com cara de favor, não de serviço profissional.',
]

const AUDIENCES = ['Técnicos de informática', 'Instaladores de redes', 'Consultores', 'Profissionais autônomos', 'Pequenos provedores', 'Prestadores de Wi-Fi']

const FEATURE_GROUPS = [
  {
    title: 'Clientes e visitas',
    items: [
      { icon: 'group', label: 'Cadastro de clientes' },
      { icon: 'location_on', label: 'Cadastro de locais' },
      { icon: 'event_available', label: 'Visitas técnicas' },
      { icon: 'meeting_room', label: 'Ambientes e cômodos' },
    ],
  },
  {
    title: 'Registro técnico',
    items: [
      { icon: 'speed', label: 'Medições por ambiente' },
      { icon: 'photo_camera', label: 'Fotos e evidências' },
      { icon: 'compare', label: 'Antes e depois' },
      { icon: 'history', label: 'Histórico por cliente' },
    ],
  },
  {
    title: 'Laudo e entrega',
    items: [
      { icon: 'picture_as_pdf', label: 'Laudo em PDF' },
      { icon: 'branding_watermark', label: 'Sua marca no laudo' },
      { icon: 'ios_share', label: 'Envio ao cliente' },
    ],
  },
]

const FLOW_STEPS = ['Cliente', 'Local', 'Visita', 'Ambientes', 'Medições', 'Evidências', 'Laudo']

const COMPARISONS = [
  { free: 'Você mede a sua própria conexão', pro: 'Você mede e documenta a de cada cliente' },
  { free: 'Resultado fica só na tela', pro: 'Resultado vira laudo em PDF com sua marca' },
  { free: 'Sem histórico por cliente', pro: 'Histórico por cliente e por visita' },
  { free: 'Sem evidências nem comparação', pro: 'Fotos, observações e antes/depois' },
]

const PLANS = [
  { title: 'Mensal', body: 'Cobrança recorrente mensal.' },
  { title: 'Anual', body: 'Cobrança recorrente anual.' },
]

export default function ProPage() {
  useDocumentMeta(PAGE_META['/pro'])
  const navigate = useNavigate()
  const [modalOpen, setModalOpen] = useState(false)

  useEffect(() => {
    trackScreenView('pro')
  }, [])

  const openModal = () => {
    trackFeatureUsed(FEATURE_PRO_LISTA_ESPERA)
    setModalOpen(true)
  }

  const registrarInteresse = (email: string) => {
    trackFeatureUsed(FEATURE_PRO_LISTA_ESPERA)
    submitWaitlistSignup(email, 'pro', 'pro_page')
  }

  return (
    <div className="flex min-h-screen flex-col overflow-x-hidden" style={{ background: 'var(--bg-primary)' }}>
      <SiteNav active="pro" />

      {/* `AdSlotsProvider` coordena os espaços de anúncio desta página — busca o catálogo
          uma vez e distribui itens distintos, sem repetir o mesmo anúncio (#1402/#1405). */}
      <AdSlotsProvider>
      <div className="flex w-full flex-1 items-start justify-center gap-6 px-5 pt-7 pb-10 box-border lg:px-6 lg:pt-5 lg:pb-4">
        <AdRail variant="a" />

        <div className="flex w-full max-w-[860px] flex-1 flex-col gap-6">
          {/* Hero */}
          <div className="flex flex-col gap-3">
            <span
              className="w-fit rounded-full px-3 py-[5px] text-[11px] font-bold uppercase text-white"
              style={{ background: PRO_GRADIENT, font: '700 11px/1.2 var(--font-sans)', letterSpacing: '0.04em' }}
            >
              SignallQ PRO
            </span>
            <h1 className="m-0 max-w-[640px] text-pretty text-[28px] font-bold leading-[1.2] lg:text-[30px]" style={{ color: 'var(--text-primary)' }}>
              Sua visita técnica merece um resultado que o cliente guarda.
            </h1>
            <p className="m-0 max-w-[560px] body-medium" style={{ color: 'var(--text-secondary)' }}>
              Cliente, ambientes, comparações e um laudo em PDF com a sua marca.
            </p>
            <div className="flex flex-wrap gap-3 pt-1">
              <button onClick={openModal} className="h-11 rounded-[var(--radius-button)] px-5 label-large text-white" style={{ background: PRO_GRADIENT }}>
                Entrar na lista de espera
              </button>
              <button onClick={() => navigate('/')} className="h-11 rounded-[var(--radius-button)] border px-5 label-large" style={{ borderColor: 'var(--border)', color: 'var(--text-primary)' }}>
                Usar o SignallQ gratuito
              </button>
            </div>
          </div>

          {/* Sem o PRO, hoje + Para quem é */}
          <div className="grid grid-cols-1 gap-5 lg:grid-cols-2">
            <section className="flex flex-col gap-2.5 rounded-2xl p-4.5" style={{ background: 'var(--bg-secondary)' }}>
              <div style={{ font: '600 16px/1.38 var(--font-sans)', color: 'var(--text-primary)' }}>
                Sem o PRO, hoje
              </div>
              {PAINS.map((p) => (
                <div key={p} className="flex items-start gap-2.5">
                  <span className="mt-1.5 h-1.5 w-1.5 flex-shrink-0 rounded-full" style={{ background: 'var(--error)' }} />
                  <div style={{ font: '400 13px/1.45 var(--font-sans)', color: 'var(--text-secondary)' }}>
                    {p}
                  </div>
                </div>
              ))}
            </section>

            <section className="flex flex-col gap-2.5 rounded-2xl p-4.5" style={{ background: 'var(--bg-secondary)' }}>
              <div style={{ font: '600 16px/1.38 var(--font-sans)', color: 'var(--text-primary)' }}>
                Para quem é
              </div>
              <div className="flex flex-wrap gap-2">
                {AUDIENCES.map((a) => (
                  <span key={a} className="rounded-full px-3 py-1.5 label-medium" style={{ color: 'var(--text-primary)', background: 'var(--bg-primary)' }}>
                    {a}
                  </span>
                ))}
              </div>
            </section>
          </div>

          {/* O que o PRO entrega */}
          <div className="flex flex-col gap-3">
            <div style={{ font: '600 16px/1.38 var(--font-sans)', color: 'var(--text-primary)' }}>
              O que o PRO entrega
            </div>
            <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
              {FEATURE_GROUPS.map((group) => (
                <div key={group.title} className="flex flex-col gap-2">
                  <div className="label-overline" style={{ color: 'var(--text-tertiary)' }}>
                    {group.title}
                  </div>
                  {group.items.map((f) => (
                    <div key={f.label} className="flex items-center gap-2">
                      <span className="material-symbols-outlined" style={{ fontSize: 18, color: '#0B6CFF' }}>
                        {f.icon}
                      </span>
                      <div style={{ font: '400 13px/1.4 var(--font-sans)', color: 'var(--text-secondary)' }}>
                        {f.label}
                      </div>
                    </div>
                  ))}
                </div>
              ))}
            </div>
          </div>

          {/* Como funciona na prática */}
          <div className="flex flex-col gap-3">
            <div style={{ font: '600 16px/1.38 var(--font-sans)', color: 'var(--text-primary)' }}>
              Como funciona na prática
            </div>
            <div className="flex flex-wrap items-center gap-2">
              {FLOW_STEPS.map((step, i) => (
                <div key={step} className="flex items-center gap-2">
                  <span className="rounded-full px-3 py-1.5 label-medium text-white" style={{ background: PRO_GRADIENT }}>
                    {step}
                  </span>
                  {i < FLOW_STEPS.length - 1 && (
                    <span style={{ font: '600 13px/1 var(--font-sans)', color: 'var(--text-tertiary)' }}>
                      →
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Gratuito × PRO */}
          <div className="flex flex-col gap-3">
            <div style={{ font: '600 16px/1.38 var(--font-sans)', color: 'var(--text-primary)' }}>
              Gratuito × PRO
            </div>
            <div className="grid grid-cols-2 overflow-hidden rounded-2xl border" style={{ borderColor: 'color-mix(in srgb, var(--border) 30%, transparent)' }}>
              <div className="px-3.5 py-2.5" style={{ background: 'var(--bg-secondary)', font: '500 13px/1.4 var(--font-sans)', color: 'var(--text-primary)' }}>
                Gratuito
              </div>
              <div className="px-3.5 py-2.5" style={{ background: PRO_GRADIENT, font: '700 13px/1.4 var(--font-sans)', color: '#fff' }}>
                PRO
              </div>
              {COMPARISONS.map((c) => (
                <Fragment key={c.free}>
                  <div
                    className="border-t px-3.5 py-2.5"
                    style={{ borderColor: 'color-mix(in srgb, var(--border) 20%, transparent)', font: '400 12px/1.45 var(--font-sans)', color: 'var(--text-secondary)' }}
                  >
                    {c.free}
                  </div>
                  <div
                    className="border-l border-t px-3.5 py-2.5"
                    style={{ borderColor: 'color-mix(in srgb, var(--border) 20%, transparent)', font: '500 12px/1.45 var(--font-sans)', color: 'var(--text-primary)' }}
                  >
                    {c.pro}
                  </div>
                </Fragment>
              ))}
            </div>
          </div>

          {/* Planos */}
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            {PLANS.map((plano) => (
              <div key={plano.title} className="flex flex-col gap-1 rounded-2xl p-4" style={{ background: 'var(--bg-secondary)' }}>
                <div className="label-overline" style={{ color: 'var(--text-tertiary)' }}>
                  {plano.title}
                </div>
                <div className="title-large" style={{ color: 'var(--text-primary)' }}>
                  Em breve
                </div>
                <div style={{ font: '400 12px/1.4 var(--font-sans)', color: 'var(--text-secondary)' }}>
                  {plano.body}
                </div>
              </div>
            ))}
          </div>

          {/* CTA final */}
          <div
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl px-5 py-4.5"
            style={{ background: 'var(--bg-secondary)' }}
          >
            <div className="max-w-[420px]" style={{ font: '600 16px/1.38 var(--font-sans)', color: 'var(--text-primary)' }}>
              Seu próximo atendimento pode ser o primeiro com laudo profissional.
            </div>
            <button onClick={openModal} className="h-11 rounded-[var(--radius-button)] px-5 label-large text-white" style={{ background: PRO_GRADIENT }}>
              Entrar na lista de espera
            </button>
          </div>
        </div>

        <AdRail variant="b" />
      </div>

      <div className="mx-auto w-full max-w-[860px] px-5 pb-7 box-border">
        <AdBannerWide variant="b" />
      </div>
      </AdSlotsProvider>

      <SiteFooter />

      {modalOpen && (
        <EmailCaptureDialog
          icon="workspace_premium"
          accentColor="#0B6CFF"
          title="O PRO chega em breve"
          body="Deixe seu e-mail e avisamos no lançamento."
          inputLabel="Seu e-mail"
          inputPlaceholder="seu@email.com"
          submitButtonText="Quero ser avisado"
          successMessage="Anotado! Você será avisado assim que o SignallQ PRO lançar."
          secondaryLabel="Usar o gratuito"
          onSecondary={() => navigate('/')}
          onSubmit={registrarInteresse}
          onClose={() => setModalOpen(false)}
        />
      )}
    </div>
  )
}
