import { ButtonHTMLAttributes, ReactNode } from 'react';

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  icon?: ReactNode;
  isLoading?: boolean;
  variant?: 'primary' | 'secondary' | 'tonal' | 'text';
}

export function Button({ children, className = '', icon, isLoading = false, variant = 'primary', ...props }: ButtonProps) {
  const classes = ['sq-button', `sq-button--${variant}`, className].filter(Boolean).join(' ');

  return (
    <button className={classes} disabled={isLoading || props.disabled} type="button" {...props}>
      {isLoading ? <span aria-hidden="true" className="sq-button__spinner" /> : icon}
      <span>{isLoading ? 'Carregando' : children}</span>
    </button>
  );
}
