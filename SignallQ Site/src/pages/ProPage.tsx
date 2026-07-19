import { Fragment, useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { EmailCaptureDialog } from '../components/EmailCaptureDialog'
import { SiteFooter } from '../components/SiteFooter'
import { SiteNav } from '../components/SiteNav'
import { useDocumentMeta } from '../hooks/useDocumentMeta'
import { FEATURE_PRO_LISTA_ESPERA, trackFeatureUsed, trackScreenView } from '../lib/telemetry'
import { submitWaitlistSignup } from '../lib/waitlist'

const PRO_GRADIENT = 'linear-gradient(135deg, #0B6CFF, #6558E8)'

const PAINS = [
  'Medições ficam soltas no WhatsApp ou na memória — e somem antes do próximo atendimento.',
  'O cliente vê um número na tela, mas não entende o que você realmente fez.',
  'Você entrega um trabalho técnico com cara de favor, não de serviço profissional.',
]

const AUDIENCES = ['Técnicos de informática', 'Instaladores de redes', 'Consultores', 'Profissionais autônomos', 'Pequenos provedores', 'Prestadores de Wi-Fi e conectividade']

// Achado da Lia: agrupar em 3 blocos temáticos em vez de lista plana de 11 itens.
const FEATURE_GROUPS = [
  {
    title: 'Organização de clientes e visitas',
    items: [
      { icon: 'group', label: 'Cadastro de clientes' },
      { icon: 'location_on', label: 'Cadastro de locais' },
      { icon: 'event_available', label: 'Organização de visitas técnicas' },
      { icon: 'meeting_room', label: 'Registro de ambientes e cômodos' },
    ],
  },
  {
    title: 'Registro técnico',
    items: [
      { icon: 'speed', label: 'Medições por ambiente' },
      { icon: 'photo_camera', label: 'Fotos, observações e evidências' },
      { icon: 'compare', label: 'Comparação antes e depois' },
      { icon: 'history', label: 'Histórico por cliente' },
    ],
  },
  {
    title: 'Laudo e entrega',
    items: [
      { icon: 'picture_as_pdf', label: 'Laudo profissional em PDF' },
      { icon: 'branding_watermark', label: 'Personalização com logo e dados do técnico' },
      { icon: 'ios_share', label: 'Compartilhamento do resultado com o cliente' },
    ],
  },
]

const FLOW_STEPS = ['Cliente', 'Local', 'Visita', 'Ambientes', 'Medições', 'Evidências', 'Laudo']

const COMPARISONS = [
  { free: 'Você mede a sua própria conexão', pro: 'Você mede e documenta a conexão de cada cliente' },
  { free: 'Resultado fica só na tela', pro: 'Resultado vira um laudo em PDF com a sua marca' },
  { free: 'Sem histórico por cliente', pro: 'Histórico organizado por cliente e por visita' },
  { free: 'Sem evidências nem comparação', pro: 'Fotos, observações e comparação antes/depois' },
]

function StatusChip({ label, tone = 'accent' }: { label: string; tone?: 'accent' | 'muted' }) {
  return (
    <span
      className="rounded-full px-3.5 py-1.5 label-medium"
      style={tone === 'accent' ? { background: PRO_GRADIENT, color: '#fff' } : { background: 'var(--bg-secondary)', color: 'var(--text-primary)' }}
    >
      {label}
    </span>
  )
}

export default function ProPage() {
  useDocumentMeta({
    title: 'SignallQ PRO — venda seu diagnóstico de Wi-Fi como serviço',
    // Achado bloqueante da Lia: description prometia trial ("Experimente grátis por
    // 14 dias") contradizendo "Em breve" da seção Planos/modal. Corrigido para vitrine honesta.
    description: 'Organize clientes, registre medições por ambiente e entregue um laudo profissional com a sua marca. Em breve — entre na lista de espera.',
    path: '/pro',
  })
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

      <div className="mx-auto flex w-full max-w-[960px] flex-1 flex-col gap-16 px-5 pb-20 pt-14 box-border">
        <div className="flex flex-col items-center gap-4 text-center">
          <span className="rounded-full px-3.5 py-1.5 text-[11px] font-bold uppercase tracking-wide text-white" style={{ background: PRO_GRADIENT }}>
            SignallQ PRO
          </span>
          <div className="display-small max-w-[700px]">Sua visita técnica merece um resultado que o cliente guarda — e paga por ele.</div>
          <div className="body-large max-w-[580px]">
            O SignallQ PRO transforma medições soltas em um serviço organizado: cliente, ambientes, comparações e um laudo em PDF com a sua marca.
          </div>
          <div className="mt-1.5 flex flex-wrap justify-center gap-3">
            <button onClick={openModal} className="h-12 rounded-[var(--radius-button)] px-6 label-large text-white" style={{ background: PRO_GRADIENT }}>
              Entrar na lista de espera
            </button>
            <button onClick={() => navigate('/')} className="h-12 rounded-[var(--radius-button)] border px-6 label-large" style={{ borderColor: 'var(--border)' }}>
              Usar o SignallQ gratuito
            </button>
          </div>
          {/* Achado bloqueante da Lia: chip prometia "14 dias grátis" contradizendo "Em breve" das Planos/modal — trocado para vitrine honesta. */}
          <StatusChip label="Em breve — entre na lista de espera" />
        </div>

        <div className="mx-auto flex max-w-[640px] flex-col gap-4 text-center">
          <div className="headline-small">O que acontece hoje, sem o PRO</div>
          <div className="flex flex-col gap-2.5 text-left">
            {PAINS.map((p) => (
              <div key={p} className="flex items-start gap-2.5">
                <span className="mt-2 h-1.5 w-1.5 flex-shrink-0 rounded-full" style={{ background: 'var(--error)' }} />
                <div className="body-medium">{p}</div>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="headline-small">Se você mede rede para viver disso, é para você</div>
          <div className="flex flex-wrap gap-2.5">
            {AUDIENCES.map((a) => (
              <StatusChip key={a} label={a} tone="muted" />
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-6">
          <div className="headline-small">O que o PRO entrega</div>
          {FEATURE_GROUPS.map((group) => (
            <div key={group.title} className="flex flex-col gap-3">
              <div className="title-small" style={{ color: 'var(--text-secondary)' }}>
                {group.title}
              </div>
              <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
                {group.items.map((f) => (
                  <div key={f.label} className="flex items-start gap-3 rounded-2xl p-4" style={{ background: 'var(--bg-card)' }}>
                    <span className="material-symbols-outlined" style={{ fontSize: 22, color: '#0B6CFF' }}>
                      {f.icon}
                    </span>
                    <div className="body-medium">{f.label}</div>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="flex flex-col gap-4">
          <div className="headline-small">Como funciona na prática</div>
          <div className="flex flex-wrap items-center gap-2">
            {FLOW_STEPS.map((step, i) => (
              <div key={step} className="flex items-center gap-2">
                <StatusChip label={step} />
                {i < FLOW_STEPS.length - 1 && <span style={{ color: 'var(--text-secondary)', fontWeight: 600 }}>→</span>}
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col gap-4">
          <div className="headline-small">SignallQ gratuito × SignallQ PRO</div>
          <div className="grid grid-cols-2 overflow-hidden rounded-2xl border" style={{ borderColor: 'var(--border)' }}>
            <div className="px-4 py-3.5 label-large" style={{ background: 'var(--bg-secondary)' }}>
              Gratuito
            </div>
            <div className="border-l px-4 py-3.5 font-bold text-white" style={{ background: PRO_GRADIENT, borderColor: 'var(--border)' }}>
              PRO
            </div>
            {COMPARISONS.map((c) => (
              <Fragment key={c.free}>
                <div className="border-t px-4 py-3.5 body-small" style={{ borderColor: 'var(--border)' }}>
                  {c.free}
                </div>
                <div className="border-l border-t px-4 py-3.5 body-small font-medium" style={{ borderColor: 'var(--border)' }}>
                  {c.pro}
                </div>
              </Fragment>
            ))}
          </div>
        </div>

        <div className="flex flex-col items-center gap-4">
          <div className="headline-small">Planos</div>
          <div className="grid w-full max-w-[520px] grid-cols-1 gap-4 sm:grid-cols-2">
            {['Mensal', 'Anual'].map((plano) => (
              <div key={plano} className="flex flex-col gap-1.5 rounded-2xl p-4" style={{ background: 'var(--bg-card)' }}>
                <div className="overline">{plano}</div>
                <div className="title-large">Em breve</div>
                <div className="body-small">Cobrança recorrente {plano.toLowerCase()}.</div>
              </div>
            ))}
          </div>
        </div>

        <div className="flex flex-col items-center gap-4 rounded-2xl px-6 py-11 text-center" style={{ background: 'var(--bg-secondary)' }}>
          <div className="title-large max-w-[480px]">Seu próximo atendimento pode ser o primeiro com laudo profissional.</div>
          <div className="flex flex-wrap justify-center gap-3">
            <button onClick={openModal} className="h-12 rounded-[var(--radius-button)] px-6 label-large text-white" style={{ background: PRO_GRADIENT }}>
              Entrar na lista de espera
            </button>
            <button onClick={() => navigate('/')} className="h-12 rounded-[var(--radius-button)] border px-6 label-large" style={{ borderColor: 'var(--border)' }}>
              Usar o SignallQ gratuito
            </button>
          </div>
        </div>
      </div>

      <SiteFooter />

      {modalOpen && (
        <EmailCaptureDialog
          icon="workspace_premium"
          accentColor={PRO_GRADIENT}
          title="O SignallQ PRO estará disponível em breve"
          body="Deixe seu e-mail e avisamos assim que o PRO lançar."
          inputLabel="Seu e-mail"
          inputPlaceholder="seu@email.com"
          submitButtonText="Quero ser avisado"
          successMessage="Anotado! Você será avisado assim que o SignallQ PRO lançar."
          secondaryLabel="Usar o SignallQ gratuito"
          onSecondary={() => navigate('/')}
          onSubmit={registrarInteresse}
          onClose={() => setModalOpen(false)}
        />
      )}
    </div>
  )
}
