import { InstallPwaPrompt } from './InstallPwaPrompt'
import { PwaUpdatePrompt } from './PwaUpdatePrompt'

// Pilha coordenada dos dois toasts fixos do PWA (atualizar + instalar) — cada
// um era antes um elemento `fixed` independente brigando pelo mesmo canto da
// tela (instalar em bottom-4 left-4, atualizar em bottom-center quase
// full-width — achado da Lia, ver
// .claude/design-specs/2026-07-19-site-pwa-redesign/SPEC.md). Agora os dois
// vivem na mesma casca visual dentro de um único container fixo,
// column-reverse com gap fixo — nunca dois `fixed` disputando espaço.
//
// `bottom` soma a altura real do banner de anúncio (achado do Rhodolfo, QA de PR
// #1186: nas telas Velocidade/Resultado/Histórico o banner ocupa a faixa
// inferior e o toast cortava o texto dele). A var `--ad-banner-height` é
// publicada pelo `AdBannerWide` (antigo `AdBanner`, substituído na reconstrução
// v2 — issue #1402/#1403) e cai pra 0px nas páginas sem banner.
//
// A classe `pwa-toast-stack` (estilo em `index.css`) é o gancho que o
// `SiteFooter` usa pra esconder este container quando o rodapé entra na
// viewport — sem isso o toast (fixed na base) fica sobreposto ao conteúdo do
// rodapé ao rolar até o fim (bug real reportado pelo Luiz, 2026-07-26). A
// var `--ad-banner-height` resolve a colisão só com o banner; o rodapé é um
// caso à parte porque, quando ele aparece, o banner já ficou pra trás no
// fluxo do documento e não está mais na base da viewport.
export function PwaToastStack() {
  return (
    <div
      className="pwa-toast-stack fixed inset-x-0 z-[1000] flex flex-col-reverse items-center gap-2 px-4"
      style={{
        bottom: 'calc(1rem + var(--ad-banner-height, 0px))',
        paddingBottom: 'env(safe-area-inset-bottom, 0px)',
      }}
    >
      <PwaUpdatePrompt />
      <InstallPwaPrompt />
    </div>
  )
}
