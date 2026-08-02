"use client";
import { useDocumentMeta } from '../../hooks/useDocumentMeta'
import { PAGE_META } from '../../lib/pageMetaCatalog'

// Cores literais nesta tela representam os próprios ativos de marca (swatch da
// paleta oficial + fundo real em que cada favicon é exibido) — conteúdo, não
// estilização de UI. Valores idênticos ao `PALETTE`/aos fundos de
// `ScreenBrand.dc.html` (protótipo v4); não trocar por token do design
// system, que descreve a UI do site, não a marca sendo documentada.
const PALETTE = [
  { name: 'Accent (violeta)', hex: '#5B21D6' },
  { name: 'Accent · escuro', hex: '#D0BCFF' },
  { name: 'Accent blue', hex: '#2851B8' },
  { name: 'Success', hex: '#146C2E' },
  { name: 'Warning', hex: '#8A5000' },
  { name: 'Error', hex: '#BA1A1A' },
  { name: 'Neutro', hex: '#79747E' },
]

const CARD = "flex flex-col gap-3 rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]"
const OVERLINE = "font-medium text-[11px] leading-[1.45] text-[color:var(--text-tertiary)] tracking-[.3px] uppercase"

// Shell próprio (não o `PageShell` compartilhado): ScreenBrand.dc.html define
// padding, gap e breakpoint de mobile próprios (48px de topo no desktop, gap
// de 40px entre seções, troca em 640px), diferentes dos defaults do
// PageShell — mesmo precedente de shell dedicado usado em ScreenApp (`/app`).
// SiteNav/SiteFooter não são renderizados aqui — vivem no layout raiz.
export default function BrandPage() {
  useDocumentMeta(PAGE_META['/brand'])

  return (
    <div className="flex w-full flex-1 flex-col overflow-x-hidden">
      <div className="w-full box-border flex justify-center p-[28px_20px_40px] sm:p-[48px_var(--safe-x)_16px]">
        <div className="w-full flex flex-col gap-10" style={{ maxWidth: '1080px' }}>

          <div className="flex flex-col gap-2 w-full">
            <div className={OVERLINE}>Marca</div>
            <h1 className="m-0 font-bold text-[28px] sm:text-[34px] leading-[1.2] text-[color:var(--text-primary)]">
              Identidade visual do SignallQ
            </h1>
            <p className="m-0 max-w-[640px] font-normal text-[14px] leading-[1.45] text-[color:var(--text-secondary)]">
              Logotipo, variações de cor e paleta oficial, para uso consistente em qualquer aplicação: site, app e materiais de comunicação.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 w-full">
            <div className={CARD}>
              <span className={OVERLINE}>Logo principal</span>
              <div className="flex items-center justify-center min-h-[140px] rounded-[14px] p-6 bg-[#FFFFFF]">
                <img src="/assets/signallq-lockup-light-bg-v5.png" alt="Logotipo SignallQ, versão para fundo claro" className="h-[44px] w-auto block" />
              </div>
            </div>

            <div className={CARD}>
              <span className={OVERLINE}>Versão em fundo escuro</span>
              <div className="flex items-center justify-center min-h-[140px] rounded-[14px] p-6 bg-[#131217]">
                <img src="/assets/signallq-lockup-dark-bg-v5.png" alt="Logotipo SignallQ, versão para fundo escuro" className="h-[44px] w-auto block" />
              </div>
            </div>

            <div className={CARD}>
              <span className={OVERLINE}>Versão monocromática</span>
              <div className="flex items-center justify-center min-h-[140px] rounded-[14px] p-6 bg-[#F8F5FB]">
                <img src="/assets/signallq-lockup-light-bg-v5.png" alt="Logotipo SignallQ, versão monocromática" className="h-[44px] w-auto block grayscale contrast-[1.05]" />
              </div>
            </div>

            <div className={CARD}>
              <span className={OVERLINE}>Ícones do app</span>
              <div className="flex items-center justify-center gap-[40px] min-h-[140px] rounded-[14px]">
                <div className="flex flex-col items-center gap-[10px]">
                  <img src="/assets/signallq-icon-512-play-store-dark.png" alt="Ícone do aplicativo SignallQ para a Play Store" className="w-[92px] h-[92px] rounded-[22px] block shadow-[0_8px_20px_rgba(0,0,0,.2)]" />
                  <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)]">Google Play</span>
                </div>
                <div className="flex flex-col items-center gap-[10px]">
                  <img src="/assets/signallq-icon-1024-app-store-dark.png" alt="Ícone do aplicativo SignallQ para a App Store" className="w-[92px] h-[92px] rounded-[20px] block shadow-[0_8px_20px_rgba(0,0,0,.2)]" />
                  <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)]">App Store</span>
                </div>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-[14px] w-full">
            <h2 className="m-0 font-bold text-[20px] leading-[1.3] text-[color:var(--text-primary)]">Paleta de cores</h2>
            <div className="grid grid-cols-2 sm:grid-cols-7 gap-3">
              {PALETTE.map((c) => (
                <div key={c.hex} className="flex flex-col gap-2">
                  <div className="h-[64px] rounded-[12px] shadow-[0_6px_16px_rgba(0,0,0,.14)]" style={{ background: c.hex }} />
                  <div className="font-medium text-[12px] leading-[1.3] text-[color:var(--text-primary)]">{c.name}</div>
                  <div className="font-normal text-[11px] leading-[1.3] text-[color:var(--text-tertiary)]">{c.hex}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="flex flex-col gap-[14px] w-full">
            <h2 className="m-0 font-bold text-[20px] leading-[1.3] text-[color:var(--text-primary)]">Favicons (Android / iOS)</h2>
            <div className="grid grid-cols-2 sm:grid-cols-6 gap-4">
              <div className="flex flex-col items-center gap-[10px] rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="flex items-center justify-center w-[64px] h-[64px] rounded-[16px] bg-[#FFFFFF] overflow-hidden">
                  <img src="/assets/signallq-favicon-light-bg.png" alt="Favicon Android, versão clara" className="w-full h-full block" />
                </div>
                <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] text-center">Android · Claro</span>
              </div>

              <div className="flex flex-col items-center gap-[10px] rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="flex items-center justify-center w-[64px] h-[64px] rounded-[16px] bg-[#131217] overflow-hidden">
                  <img src="/assets/signallq-favicon-dark-bg.png" alt="Favicon Android, versão escura" className="w-full h-full block" />
                </div>
                <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] text-center">Android · Escuro</span>
              </div>

              <div className="flex flex-col items-center gap-[10px] rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="flex items-center justify-center w-[64px] h-[64px] rounded-[14px] bg-[#FFFFFF] overflow-hidden">
                  <img src="/assets/signallq-favicon-light-bg.png" alt="Favicon iOS, versão clara" className="w-full h-full block" />
                </div>
                <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] text-center">iOS · Claro</span>
              </div>

              <div className="flex flex-col items-center gap-[10px] rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="flex items-center justify-center w-[64px] h-[64px] rounded-[14px] bg-[#131217] overflow-hidden">
                  <img src="/assets/signallq-favicon-dark-bg.png" alt="Favicon iOS, versão escura" className="w-full h-full block" />
                </div>
                <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] text-center">iOS · Escuro</span>
              </div>

              <div className="flex flex-col items-center gap-[10px] rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="flex items-center justify-center w-[64px] h-[64px] rounded-[12px] bg-[#F1EAFB] overflow-hidden">
                  <img src="/assets/signallq-favicon-web-light-bg.png" alt="Favicon WebApp, versão clara" className="w-full h-full block" />
                </div>
                <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] text-center">WebApp · Claro</span>
              </div>

              <div className="flex flex-col items-center gap-[10px] rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="flex items-center justify-center w-[64px] h-[64px] rounded-[12px] bg-[#1B1130] overflow-hidden">
                  <img src="/assets/signallq-favicon-web-dark-bg.png" alt="Favicon WebApp, versão escura" className="w-full h-full block" />
                </div>
                <span className="font-medium text-[11px] leading-[1.3] text-[color:var(--text-tertiary)] text-center">WebApp · Escuro</span>
              </div>
            </div>
          </div>

          <div className="flex flex-col gap-[14px] w-full">
            <h2 className="m-0 font-bold text-[20px] leading-[1.3] text-[color:var(--text-primary)]">Área de proteção e uso mínimo</h2>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className="rounded-[12px] p-8 flex items-center justify-center bg-[#FFFFFF] outline-dashed outline-1 outline-[color-mix(in_srgb,_var(--accent)_45%,_transparent)] outline-offset-[-14px]">
                  <img src="/assets/signallq-lockup-light-bg-v5.png" alt="Área de proteção do logotipo SignallQ" className="h-[32px] w-auto block" />
                </div>
                <p className="mt-3 mb-0 font-normal text-[12px] leading-[1.45] text-[color:var(--text-secondary)]">
                  A área de proteção garante respiro e legibilidade da marca em qualquer aplicação: nada deve invadir esse espaço ao redor do logotipo.
                </p>
              </div>
              <div className="flex flex-col gap-3 rounded-[20px] p-6 box-border bg-[color:var(--bg-secondary)] shadow-[0_10px_26px_rgba(0,0,0,.14)]">
                <div className={OVERLINE}>Uso em reduzidas</div>
                <div className="flex items-center gap-2 bg-[#FFFFFF] rounded-[10px] p-[10px_14px] w-fit">
                  <img src="/assets/signallq-lockup-light-bg-v5.png" alt="Logotipo SignallQ em tamanho reduzido" className="h-[20px] w-auto block" />
                </div>
                <p className="m-0 font-normal text-[12px] leading-[1.45] text-[color:var(--text-secondary)]">
                  Abaixo de ~20px de altura, priorize apenas o ícone (as 4 barras): a wordmark perde legibilidade em tamanhos muito pequenos.
                </p>
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  )
}
