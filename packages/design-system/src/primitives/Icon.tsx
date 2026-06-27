import React from 'react';

export interface IconProps {
  /** Material Symbols icon name (e.g. "home", "wifi", "speed") */
  name: string;
  size?: number;
  color?: string;
  /** Fill variation: 0 = outlined, 1 = filled */
  fill?: 0 | 1;
  weight?: 100 | 200 | 300 | 400 | 500 | 600 | 700;
  style?: React.CSSProperties;
}

/** Material Symbols Outlined icon. Requires the Material Symbols font loaded from Google Fonts. */
export function Icon({ name, size = 24, color = 'currentColor', fill = 0, weight = 400, style = {} }: IconProps) {
  return (
    <span
      className="material-symbols-outlined"
      style={{
        fontSize: size,
        color,
        lineHeight: 1,
        fontVariationSettings: `'FILL' ${fill}, 'wght' ${weight}, 'GRAD' 0, 'opsz' 24`,
        userSelect: 'none',
        ...style,
      }}
    >
      {name}
    </span>
  );
}
