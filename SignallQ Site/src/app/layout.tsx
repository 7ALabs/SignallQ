import type { Metadata } from "next";
import "../index.css";

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
    <html lang="pt-BR" className="h-full antialiased" data-sq-theme="system">
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
      <body className="min-h-full flex flex-col overflow-x-hidden bg-[radial-gradient(circle_at_50%_0%,_color-mix(in_srgb,_var(--accent)_10%,_transparent),_transparent_55%),_linear-gradient(180deg,_var(--bg-primary)_0%,_color-mix(in_srgb,_var(--bg-secondary)_55%,_var(--bg-primary))_100%)] text-[color:var(--text-primary)]">
        {children}
      </body>
    </html>
  );
}
