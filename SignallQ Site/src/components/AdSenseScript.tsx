"use client";

import Script from "next/script";
import { useSyncExternalStore } from "react";
import { ADSENSE_PUBLISHER_ID } from "../lib/config";
import { getAdConsent, getAdConsentServerSnapshot, subscribeToAdConsent } from "../lib/adConsent";

// Infraestrutura técnica de AdSense — fase Fundação da reconstrução v4. Sem
// posição de anúncio decidida ainda (AdRail/AdBannerWide foram removidos
// desta fase): só carrega o script real do Google, sem reservar nenhum
// espaço visual. Dupla trava: precisa da env var configurada (publisher ID
// real do Luiz) E do consentimento aceito (`CookieConsentBanner`) — sem as
// duas, nada é carregado, comportamento visualmente idêntico ao atual.
export function AdSenseScript() {
  const consent = useSyncExternalStore(subscribeToAdConsent, getAdConsent, getAdConsentServerSnapshot);

  if (!ADSENSE_PUBLISHER_ID || consent !== "accepted") return null;

  return (
    <Script
      id="adsbygoogle-loader"
      strategy="afterInteractive"
      async
      crossOrigin="anonymous"
      src={`https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js?client=${ADSENSE_PUBLISHER_ID}`}
    />
  );
}
