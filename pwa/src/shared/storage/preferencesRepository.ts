export interface InstallPromptPreferences {
  dismissedAt: string | null;
}

export interface PreferencesRepository {
  dismissInstallPrompt: () => void;
  getInstallPromptPreferences: () => InstallPromptPreferences;
  resetInstallPromptPreferences: () => void;
}

const INSTALL_PROMPT_DISMISSED_AT_KEY = 'signallq.installPrompt.dismissedAt';

function getStorage(): Storage | null {
  try {
    return typeof window === 'undefined' ? null : window.localStorage;
  } catch {
    return null;
  }
}

export const preferencesRepository: PreferencesRepository = {
  dismissInstallPrompt() {
    getStorage()?.setItem(INSTALL_PROMPT_DISMISSED_AT_KEY, new Date().toISOString());
  },

  getInstallPromptPreferences() {
    return {
      dismissedAt: getStorage()?.getItem(INSTALL_PROMPT_DISMISSED_AT_KEY) ?? null,
    };
  },

  resetInstallPromptPreferences() {
    getStorage()?.removeItem(INSTALL_PROMPT_DISMISSED_AT_KEY);
  },
};
