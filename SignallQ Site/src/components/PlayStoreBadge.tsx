import { SIGNALLQ_BETA_DOWNLOAD_URL } from '../lib/config'
import { FEATURE_DOWNLOAD_APP_CLICADO, trackFeatureUsed } from '../lib/telemetry'

interface PlayStoreBadgeProps {
  height?: number
  source: string
}

export function PlayStoreBadge({ height = 44, source }: PlayStoreBadgeProps) {
  const onClick = () => {
    trackFeatureUsed(FEATURE_DOWNLOAD_APP_CLICADO)
    if (SIGNALLQ_BETA_DOWNLOAD_URL) {
      window.open(SIGNALLQ_BETA_DOWNLOAD_URL, '_blank', 'noopener,noreferrer')
    } else {
      window.alert('O link oficial de download do SignallQ ainda não foi configurado.')
    }
  }

  return (
    <button onClick={onClick} data-source={source} className="block cursor-pointer border-none bg-transparent p-0 leading-none">
      <img src="/google-play-badge.png" alt="Disponível no Google Play (Beta)" style={{ height, width: 'auto', display: 'block' }} />
    </button>
  )
}
