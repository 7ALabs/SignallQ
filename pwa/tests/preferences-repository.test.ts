import { afterEach, describe, expect, it, vi } from 'vitest';
import { preferencesRepository } from '../src/shared/storage/preferencesRepository';

function createMemoryStorage(): Storage {
  const values = new Map<string, string>();

  return {
    get length() {
      return values.size;
    },
    clear: () => values.clear(),
    getItem: (key) => values.get(key) ?? null,
    key: (index) => Array.from(values.keys())[index] ?? null,
    removeItem: (key) => values.delete(key),
    setItem: (key, value) => values.set(key, value),
  };
}

describe('preferences repository', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it('persists install prompt dismissal locally', () => {
    vi.stubGlobal('window', { localStorage: createMemoryStorage() });
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-06-30T12:00:00.000Z'));

    preferencesRepository.dismissInstallPrompt();

    expect(preferencesRepository.getInstallPromptPreferences()).toEqual({
      dismissedAt: '2026-06-30T12:00:00.000Z',
    });
  });

  it('clears install prompt dismissal', () => {
    vi.stubGlobal('window', { localStorage: createMemoryStorage() });

    preferencesRepository.dismissInstallPrompt();
    preferencesRepository.resetInstallPromptPreferences();

    expect(preferencesRepository.getInstallPromptPreferences()).toEqual({ dismissedAt: null });
  });
});
