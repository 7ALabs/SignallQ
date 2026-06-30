export type InstallSupport = 'installed' | 'native_prompt' | 'ios_manual' | 'manual_instructions';

export type InstallPromptOutcome = 'accepted' | 'dismissed';

export interface BeforeInstallPromptEvent extends Event {
  readonly platforms?: string[];
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: InstallPromptOutcome; platform: string }>;
}

export interface InstallEnvironment {
  instruction: string;
  isStandalone: boolean;
  support: InstallSupport;
}

export interface InstallPromptVisibilityInput {
  dismissed: boolean;
  eligible: boolean;
  isDiagnosisLoading: boolean;
  isHomeRoute: boolean;
  isRunning: boolean;
  isStandalone: boolean;
}

interface InstallEnvironmentOptions {
  hasNativePrompt?: boolean;
  navigatorLike?: Navigator;
  standalone?: boolean;
  userAgent?: string;
  vendor?: string;
}

function hasNavigatorStandalone(navigatorLike: Navigator | undefined): boolean {
  return Boolean(
    navigatorLike &&
      'standalone' in navigatorLike &&
      (navigatorLike as Navigator & { standalone?: boolean }).standalone === true,
  );
}

function isIosLike(navigatorLike: Navigator | undefined, userAgent: string): boolean {
  const platform = navigatorLike?.platform ?? '';
  const maxTouchPoints = navigatorLike?.maxTouchPoints ?? 0;
  return /iphone|ipad|ipod/i.test(userAgent) || (platform === 'MacIntel' && maxTouchPoints > 1);
}

export function getInstallEnvironment(options: InstallEnvironmentOptions = {}): InstallEnvironment {
  const navigatorLike = options.navigatorLike ?? (typeof navigator !== 'undefined' ? navigator : undefined);
  const userAgent = options.userAgent ?? navigatorLike?.userAgent ?? '';
  const detectedStandalone =
    hasNavigatorStandalone(navigatorLike) ||
    Boolean(typeof window !== 'undefined' && window.matchMedia?.('(display-mode: standalone)').matches);
  const isStandalone = options.standalone ?? detectedStandalone;

  if (isStandalone) {
    return {
      instruction: 'O SignallQ já está instalado neste navegador.',
      isStandalone: true,
      support: 'installed',
    };
  }

  if (options.hasNativePrompt) {
    return {
      instruction: 'Instale o SignallQ para abrir o diagnóstico direto pela tela inicial.',
      isStandalone: false,
      support: 'native_prompt',
    };
  }

  if (isIosLike(navigatorLike, userAgent)) {
    return {
      instruction: 'No iPhone ou iPad, use Compartilhar e depois Adicionar à Tela de Início.',
      isStandalone: false,
      support: 'ios_manual',
    };
  }

  return {
    instruction: 'Se o navegador oferecer instalação, use o menu do browser e escolha instalar ou adicionar à tela inicial.',
    isStandalone: false,
    support: 'manual_instructions',
  };
}

export function listenForBeforeInstallPrompt(onPromptReady: (event: BeforeInstallPromptEvent) => void): () => void {
  if (typeof window === 'undefined') return () => undefined;

  const handleBeforeInstallPrompt = (event: Event) => {
    event.preventDefault();
    onPromptReady(event as BeforeInstallPromptEvent);
  };

  window.addEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
  return () => window.removeEventListener('beforeinstallprompt', handleBeforeInstallPrompt);
}

export async function requestNativeInstallPrompt(event: BeforeInstallPromptEvent): Promise<InstallPromptOutcome> {
  await event.prompt();
  const choice = await event.userChoice;
  return choice.outcome;
}

export function canShowInstallPromptBanner(input: InstallPromptVisibilityInput): boolean {
  return (
    input.isHomeRoute &&
    input.eligible &&
    !input.dismissed &&
    !input.isRunning &&
    !input.isDiagnosisLoading &&
    !input.isStandalone
  );
}
