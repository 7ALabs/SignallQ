export interface InstallPromptPreferences {
  dismissedAt: string | null;
}

export type ThemePreference = 'light' | 'dark';

export interface PreferencesRepository {
  dismissInstallPrompt: () => void;
  getInstallPromptPreferences: () => InstallPromptPreferences;
  getThemePreference: () => ThemePreference;
  resetInstallPromptPreferences: () => void;
  setThemePreference: (mode: ThemePreference) => void;
}

const INSTALL_PROMPT_DISMISSED_AT_KEY = 'signallq.installPrompt.dismissedAt';
const THEME_MODE_KEY = 'signallq.theme.mode';

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

  getThemePreference() {
    return getStorage()?.getItem(THEME_MODE_KEY) === 'light' ? 'light' : 'dark';
  },

  resetInstallPromptPreferences() {
    getStorage()?.removeItem(INSTALL_PROMPT_DISMISSED_AT_KEY);
  },

  setThemePreference(mode) {
    getStorage()?.setItem(THEME_MODE_KEY, mode);
  },
};
