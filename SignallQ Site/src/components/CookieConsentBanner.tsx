"use client";

import { useSyncExternalStore } from "react";
import Link from "next/link";
import { getAdConsent, getAdConsentServerSnapshot, setAdConsent, subscribeToAdConsent } from "../lib/adConsent";

// Gate legal mínimo antes de carregar qualquer script de publicidade
// (`AdSenseScript`) — aceitar/recusar, persiste a escolha, sem categorias.
// `useSyncExternalStore` sincroniza com o `localStorage` (só existe no
// client) sem precisar de setState síncrono em `useEffect`: no servidor e no
// primeiro paint do client, `getServerSnapshot` sempre retorna "não decidiu"
// — sem flash, sem mismatch de hidratação.
export function CookieConsentBanner() {
  const consent = useSyncExternalStore(subscribeToAdConsent, getAdConsent, getAdConsentServerSnapshot);

  if (consent !== null) return null;

  const decide = (accepted: boolean) => {
    setAdConsent(accepted ? "accepted" : "declined");
  };

  return (
    <div
      role="dialog"
      aria-label="Consentimento de cookies"
      className="fixed inset-x-0 bottom-0 z-[40] box-border w-full border-t border-[color-mix(in_srgb,_var(--border)_25%,_transparent)] bg-[color:var(--bg-card)] p-4 lg:p-5"
    >
      <div className="mx-auto flex w-full max-w-[1280px] flex-wrap items-center justify-between gap-4">
        <p className="m-0 max-w-[640px] font-normal text-[13px] leading-[1.45] text-[color:var(--text-secondary)]">
          Usamos cookies essenciais ao funcionamento do site e, só com sua autorização, cookies de
          publicidade. Veja a{" "}
          <Link href="/privacidade" className="text-[color:var(--accent)]">
            Política de Privacidade
          </Link>
          .
        </p>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => decide(false)}
            className="h-[40px] rounded-[var(--radius-button)] border border-[color:var(--border)] px-4 font-medium text-[13px] leading-[1.3] text-[color:var(--text-primary)]"
          >
            Recusar
          </button>
          <button
            type="button"
            onClick={() => decide(true)}
            className="h-[40px] rounded-[var(--radius-button)] bg-[color:var(--accent)] px-4 font-medium text-[13px] leading-[1.3] text-[color:var(--on-accent)]"
          >
            Aceitar
          </button>
        </div>
      </div>
    </div>
  );
}
