import type { Metadata } from "next";
import "../index.css";
import { AdSenseScript } from "../components/AdSenseScript";
import { CookieConsentBanner } from "../components/CookieConsentBanner";
import { SiteNav } from "../components/SiteNav";
import { SiteFooter } from "../components/SiteFooter";

export const metadata: Metadata = {
  title: "SignallQ - Teste de Velocidade e Qualidade",
  description: "Diagnóstico completo da sua conexão de internet. Medição de download, upload, latência, bufferbloat e jitter.",
  manifest: "/manifest.json",
};

export const viewport = {
  themeColor: "#131217",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="pt-BR" className="antialiased" data-sq-theme="system" suppressHydrationWarning>
      <head>
        <link rel="stylesheet" href="/assets/google-sans-flex.css" />
        <link rel="stylesheet" href="/_ds/signallq-design-system-2d25d7a1-31b2-4ac3-881f-72dbc8f35a29/_ds_bundle.css" />
        <link rel="stylesheet" href="/_ds/signallq-design-system-2d25d7a1-31b2-4ac3-881f-72dbc8f35a29/styles.css" />
        <script src="/_ds/signallq-design-system-2d25d7a1-31b2-4ac3-881f-72dbc8f35a29/_ds_bundle.js" async></script>
        <script
          dangerouslySetInnerHTML={{
            __html: `
              (function () {
                var mq = window.matchMedia('(prefers-color-scheme: dark)')
                document.documentElement.classList.toggle('dark', mq.matches)
              })()
            `,
          }}
        />
      </head>
      <body className="overflow-x-hidden bg-[radial-gradient(circle_at_50%_0%,_color-mix(in_srgb,_var(--accent)_10%,_transparent),_transparent_55%),_linear-gradient(180deg,_var(--bg-primary)_0%,_color-mix(in_srgb,_var(--bg-secondary)_55%,_var(--bg-primary))_100%)] text-[color:var(--text-primary)]">
        {/* SiteNav + miolo em min-h-screen própria (não a <body> inteira, que
            também engloba o SiteFooter abaixo) — garante que o rodapé nunca
            apareça na primeira vista, mesmo em telas com pouco conteúdo
            (ex.: 404, Marca): é preciso rolar pra passar da altura de uma
            viewport antes de alcançá-lo. SiteNav/SiteFooter vivem aqui, no
            layout raiz, pra persistir entre navegações (guia §1) — mover
            página não deve remontar o header/rodapé (achado 01/08/2026,
            "topbar sambando" ao trocar de rota). */}
        <div className="min-h-screen w-full">
          <SiteNav />
          <main className="w-full">{children}</main>
        </div>
        <SiteFooter />
        <CookieConsentBanner />
        <AdSenseScript />
      </body>
    </html>
  );
}
