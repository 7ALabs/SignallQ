// Consentimento mínimo de cookies/publicidade (LGPD) — gate exigido antes de
// carregar qualquer script de terceiro que use cookies para publicidade
// (Google AdSense). Auditado o repo antes de criar: não existia mecanismo de
// consentimento prévio (site só tinha placeholders visuais de anúncio, sem
// script de terceiro de fato, ver `AdSenseScript.tsx`) — fase Fundação da
// reconstrução v4.
//
// Escopo deliberadamente mínimo: aceitar/recusar, persistido, sem categorias
// (analytics/marketing/necessário) — o único script de terceiro condicionado
// hoje é o AdSense.

export type AdConsentStatus = "accepted" | "declined";

const STORAGE_KEY = "signallq_ad_consent";
const CONSENT_EVENT = "signallq:ad-consent-changed";

function isValidStatus(value: string | null): value is AdConsentStatus {
  return value === "accepted" || value === "declined";
}

/** Lê o consentimento salvo. `null` = usuário ainda não decidiu. */
export function getAdConsent(): AdConsentStatus | null {
  if (typeof window === "undefined") return null;
  try {
    const value = window.localStorage.getItem(STORAGE_KEY);
    return isValidStatus(value) ? value : null;
  } catch {
    // localStorage indisponível (ex. modo privado) — trata como "não decidiu".
    return null;
  }
}

/** Persiste a decisão e notifica assinantes na mesma aba (ex.: `AdSenseScript`). */
export function setAdConsent(status: AdConsentStatus): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(STORAGE_KEY, status);
  } catch {
    // localStorage indisponível — a escolha só vale para esta sessão de página.
  }
  window.dispatchEvent(new CustomEvent(CONSENT_EVENT, { detail: status }));
}

/**
 * Assina mudanças de consentimento (evento local + `storage` entre abas) — par
 * `subscribe`/`getSnapshot` pronto para `useSyncExternalStore` (evita setState
 * síncrono em `useEffect` para sincronizar com esta store externa).
 */
export function subscribeToAdConsent(onChange: () => void): () => void {
  if (typeof window === "undefined") return () => {};
  window.addEventListener(CONSENT_EVENT, onChange);
  window.addEventListener("storage", onChange);
  return () => {
    window.removeEventListener(CONSENT_EVENT, onChange);
    window.removeEventListener("storage", onChange);
  };
}

/** Snapshot inicial no servidor: sempre "não decidiu" (sem `localStorage`). */
export function getAdConsentServerSnapshot(): AdConsentStatus | null {
  return null;
}
