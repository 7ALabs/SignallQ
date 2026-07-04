import { CSSProperties } from 'react';
import { ICON_PATHS } from './iconPaths';

export interface IconProps {
  className?: string;
  name: string;
  size?: number;
  style?: CSSProperties;
}

/**
 * Renderiza glifos Material Symbols Outlined como SVG inline.
 *
 * Migrado de ligadura de fonte (span + font-feature-settings: 'liga') para SVG
 * porque o WebKit/Safari nao aplica a ligadura tipografica de forma confiavel
 * — o nome do icone (ex: "arrow_back") aparecia como texto cru em vez do glifo.
 * SVG inline elimina a dependencia de fonte externa/CDN e funciona em qualquer
 * navegador. Paths bundled localmente em ./iconPaths.ts (sem fetch em runtime).
 */
export function Icon({ className, name, size = 24, style }: IconProps) {
  const path = ICON_PATHS[name];

  if (!path) {
    return (
      <svg
        aria-hidden="true"
        className={['sq-icon', className].filter(Boolean).join(' ')}
        fill="currentColor"
        height={size}
        style={style}
        viewBox="0 -960 960 960"
        width={size}
      >
        <rect height="200" opacity="0.3" rx="24" width="200" x="380" y="380" />
      </svg>
    );
  }

  return (
    <svg
      aria-hidden="true"
      className={['sq-icon', className].filter(Boolean).join(' ')}
      fill="currentColor"
      height={size}
      style={style}
      viewBox="0 -960 960 960"
      width={size}
    >
      <path d={path} />
    </svg>
  );
}
