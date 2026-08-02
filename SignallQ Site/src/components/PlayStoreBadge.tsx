import { SIGNALLQ_TEST_GROUP_URL } from '../lib/config'
import { FEATURE_DOWNLOAD_APP_CLICADO, trackFeatureUsed } from '../lib/telemetry'

interface PlayStoreBadgeProps {
  height?: number
  source: string
}

export function PlayStoreBadge({ height = 44, source }: PlayStoreBadgeProps) {
  const onClick = () => {
    trackFeatureUsed(FEATURE_DOWNLOAD_APP_CLICADO)
    // O teste é fechado: entrar no grupo é a etapa correta antes da instalação
    // pela Play Store. Não coletamos e-mail para o SignallQ gratuito.
    window.open(SIGNALLQ_TEST_GROUP_URL, '_blank', 'noopener,noreferrer')
  }

  return (
    <button onClick={onClick} data-source={source} className="block cursor-pointer border-none bg-transparent p-0 leading-none">
      <img src="/google-play-badge.png" alt="Entrar no grupo de testes do SignallQ" style={{ height, width: 'auto', display: 'block' }} />
    </button>
  )
}
