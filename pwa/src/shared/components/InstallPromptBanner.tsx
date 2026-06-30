import { Download, Share2, X } from 'lucide-react';
import { Button } from '@/design-system';
import type { InstallEnvironment } from '@/shared/pwa/installPrompt';

export interface InstallPromptBannerProps {
  environment: InstallEnvironment;
  isPrompting: boolean;
  onDismiss: () => void;
  onInstall: () => void;
}

export function InstallPromptBanner({ environment, isPrompting, onDismiss, onInstall }: InstallPromptBannerProps) {
  if (environment.support === 'installed') return null;

  const hasNativePrompt = environment.support === 'native_prompt';
  const title = hasNativePrompt ? 'Instalar SignallQ' : 'Adicionar à tela inicial';

  return (
    <aside aria-label="Instalação do PWA" className="install-prompt-banner">
      <div className="install-prompt-banner__icon" aria-hidden="true">
        {environment.support === 'ios_manual' ? <Share2 size={20} /> : <Download size={20} />}
      </div>
      <div>
        <h2>{title}</h2>
        <p>{environment.instruction}</p>
      </div>
      <div className="install-prompt-banner__actions">
        {hasNativePrompt ? (
          <Button isLoading={isPrompting} onClick={onInstall} variant="tonal">
            Instalar
          </Button>
        ) : null}
        <button aria-label="Dispensar instalação" className="install-prompt-banner__dismiss" type="button" onClick={onDismiss}>
          <X size={18} />
        </button>
      </div>
    </aside>
  );
}
