import { describe, expect, it, vi } from 'vitest';
import {
  type BeforeInstallPromptEvent,
  canShowInstallPromptBanner,
  getInstallEnvironment,
  requestNativeInstallPrompt,
} from '../src/shared/pwa/installPrompt';

describe('install prompt support', () => {
  it('uses native install prompt when beforeinstallprompt is available', () => {
    expect(getInstallEnvironment({ hasNativePrompt: true })).toMatchObject({
      isStandalone: false,
      support: 'native_prompt',
    });
  });

  it('uses manual iOS instructions when native prompt is unavailable', () => {
    const navigatorLike = {
      maxTouchPoints: 5,
      platform: 'MacIntel',
      userAgent: 'Mozilla/5.0',
    } as Navigator;

    expect(getInstallEnvironment({ navigatorLike })).toMatchObject({
      instruction: 'No iPhone ou iPad, use Compartilhar e depois Adicionar à Tela de Início.',
      support: 'ios_manual',
    });
  });

  it('does not suggest installation when app is already standalone', () => {
    expect(getInstallEnvironment({ standalone: true })).toMatchObject({
      isStandalone: true,
      support: 'installed',
    });
  });

  it('returns the user choice from the native prompt event', async () => {
    const event = {
      prompt: vi.fn().mockResolvedValue(undefined),
      userChoice: Promise.resolve({ outcome: 'accepted', platform: 'web' }),
    } as unknown as BeforeInstallPromptEvent;

    await expect(requestNativeInstallPrompt(event)).resolves.toBe('accepted');
    expect(event.prompt).toHaveBeenCalledOnce();
  });

  it('hides the banner while a speed test or AI diagnosis is running', () => {
    const baseInput = {
      dismissed: false,
      eligible: true,
      isDiagnosisLoading: false,
      isHomeRoute: true,
      isRunning: false,
      isStandalone: false,
    };

    expect(canShowInstallPromptBanner(baseInput)).toBe(true);
    expect(canShowInstallPromptBanner({ ...baseInput, isRunning: true })).toBe(false);
    expect(canShowInstallPromptBanner({ ...baseInput, isDiagnosisLoading: true })).toBe(false);
  });

  it('hides the banner when dismissed, installed or outside home', () => {
    const baseInput = {
      dismissed: false,
      eligible: true,
      isDiagnosisLoading: false,
      isHomeRoute: true,
      isRunning: false,
      isStandalone: false,
    };

    expect(canShowInstallPromptBanner({ ...baseInput, dismissed: true })).toBe(false);
    expect(canShowInstallPromptBanner({ ...baseInput, isStandalone: true })).toBe(false);
    expect(canShowInstallPromptBanner({ ...baseInput, isHomeRoute: false })).toBe(false);
  });
});
