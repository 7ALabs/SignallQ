import { HTMLAttributes, ReactNode } from 'react';

export interface CardProps extends HTMLAttributes<HTMLElement> {
  children: ReactNode;
  variant?: 'surface' | 'outlined' | 'tonal';
}

export function Card({ children, className = '', variant = 'surface', ...props }: CardProps) {
  const classes = ['sq-card', `sq-card--${variant}`, className].filter(Boolean).join(' ');

  return (
    <article className={classes} {...props}>
      {children}
    </article>
  );
}
