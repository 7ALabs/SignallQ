import { ReactNode } from 'react';

export interface TopAppBarProps {
  actions?: ReactNode;
  navItems?: string[];
  subtitle?: string;
  title: string;
}

export function TopAppBar({ actions, navItems = [], subtitle, title }: TopAppBarProps) {
  return (
    <header className="sq-top-app-bar">
      <div className="sq-top-app-bar__brand">
        <img alt="" className="sq-top-app-bar__mark" src="/icon-192.png" />
        <div>
          <p className="sq-overline">{subtitle ?? 'SignallQ PWA'}</p>
          <strong>{title}</strong>
        </div>
      </div>
      {navItems.length > 0 ? (
        <nav aria-label="Navegação principal" className="sq-top-app-bar__nav">
          {navItems.map((item) => (
            <a href="#" key={item}>
              {item}
            </a>
          ))}
        </nav>
      ) : null}
      {actions ? <div className="sq-top-app-bar__actions">{actions}</div> : null}
    </header>
  );
}
