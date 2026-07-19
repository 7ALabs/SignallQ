// Lockup oficial da marca (brand/README.md) — nunca recriar em CSS/SVG à mão.
// Variante escolhida pelo tema atual (claro/escuro), altura controlável.
interface LogoProps {
  isDark?: boolean
  height?: number
  className?: string
}

export function Logo({ isDark = false, height = 32, className }: LogoProps) {
  const src = isDark ? '/brand/signallq-lockup-dark-bg.png' : '/brand/signallq-lockup-light-bg.png'
  return <img src={src} alt="SignallQ" height={height} style={{ height, width: 'auto', display: 'block' }} className={className} />
}
