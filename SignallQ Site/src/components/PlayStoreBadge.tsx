import { useState } from 'react'
import { EmailCaptureDialog } from './EmailCaptureDialog'
import { SIGNALLQ_BETA_DOWNLOAD_URL } from '../lib/config'
import { FEATURE_DOWNLOAD_APP_CLICADO, FEATURE_SIGNALLQ_LISTA_ESPERA_EMAIL_CAPTURADO, trackFeatureUsed } from '../lib/telemetry'
import { submitWaitlistSignup } from '../lib/waitlist'

interface PlayStoreBadgeProps {
  height?: number
  source: string
}

export function PlayStoreBadge({ height = 44, source }: PlayStoreBadgeProps) {
  const [modalOpen, setModalOpen] = useState(false)

  const onClick = () => {
    trackFeatureUsed(FEATURE_DOWNLOAD_APP_CLICADO)
    if (SIGNALLQ_BETA_DOWNLOAD_URL) {
      window.open(SIGNALLQ_BETA_DOWNLOAD_URL, '_blank', 'noopener,noreferrer')
    } else {
      // Sem link de download configurado: app ainda em teste fechado, só quem
      // foi convidado instala. Captura o e-mail em vez do window.alert() cru
      // de antes — no dia em que a URL for configurada, este caminho some sozinho.
      setModalOpen(true)
    }
  }

  const registrarInteresse = (email: string) => {
    trackFeatureUsed(FEATURE_SIGNALLQ_LISTA_ESPERA_EMAIL_CAPTURADO)
    submitWaitlistSignup(email, 'signallq', source)
  }

  return (
    <>
      <button onClick={onClick} data-source={source} className="block cursor-pointer border-none bg-transparent p-0 leading-none">
        <img src="/google-play-badge.png" alt="Disponível no Google Play (Beta)" style={{ height, width: 'auto', display: 'block' }} />
      </button>

      {modalOpen && (
        <EmailCaptureDialog
          icon="mail"
          accentColor="var(--accent)"
          title="O SignallQ ainda está em teste fechado"
          body="Por enquanto, só quem foi convidado consegue instalar pela Play Store. Deixe seu e-mail que avisamos assim que o app abrir para todo mundo — sem spam, só esse aviso."
          inputLabel="Seu e-mail"
          inputPlaceholder="nome@email.com"
          submitButtonText="Avisar quando lançar"
          successMessage="Pronto. Avisamos por e-mail assim que o SignallQ abrir para todo mundo."
          errorMessage="Não foi possível registrar seu e-mail agora. Tente de novo em instantes."
          onSubmit={registrarInteresse}
          onClose={() => setModalOpen(false)}
        />
      )}
    </>
  )
}
