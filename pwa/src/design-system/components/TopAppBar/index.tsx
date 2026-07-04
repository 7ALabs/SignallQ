import { ReactNode } from 'react';
import { Icon } from '../Icon';

export interface TopAppBarNavItem {
  href: string;
  label: string;
}

export interface TopAppBarProps {
  actions?: ReactNode;
  activeHref?: string;
  leading?: ReactNode;
  mobileAction?: ReactNode;
  mobileMode?: 'brand' | 'title' | 'back';
  mobileTitle?: string;
  navItems?: TopAppBarNavItem[];
  onMobileBack?: () => void;
}

export function TopAppBar({
  actions,
  activeHref,
  leading,
  mobileAction,
  mobileMode = 'brand',
  mobileTitle,
  navItems = [],
  onMobileBack,
}: TopAppBarProps) {
  const brand = (
    <div className="sq-top-app-bar__brand">
      {/* GH#443: caminho relativo ao BASE_URL — o PWA pode ser servido em /app */}
      <img alt="" className="sq-top-app-bar__mark" src={`${import.meta.env.BASE_URL}logo-signallq.svg`} />
      <span className="sq-top-app-bar__wordmark">
        Signall<span className="sq-top-app-bar__wordmark-accent">Q</span>
      </span>
    </div>
  );

  return (
    <header className="sq-top-app-bar">
      <div className="sq-top-app-bar__row sq-top-app-bar__row--desktop">
        <div className="sq-top-app-bar__leading">
          {leading}
          {brand}
        </div>
        {navItems.length > 0 ? (
          <nav aria-label="Navegação principal" className="sq-top-app-bar__nav">
            {navItems.map((item) => (
              <a
                aria-current={activeHref === item.href ? 'page' : undefined}
                className={activeHref === item.href ? 'sq-top-app-bar__nav-item sq-top-app-bar__nav-item--active' : 'sq-top-app-bar__nav-item'}
                href={item.href}
                key={item.href}
              >
                <span>{item.label}</span>
                <span className="sq-top-app-bar__nav-indicator" />
              </a>
            ))}
          </nav>
        ) : null}
        {actions ? <div className="sq-top-app-bar__actions">{actions}</div> : null}
      </div>

      <div className="sq-top-app-bar__row sq-top-app-bar__row--mobile">
        {mobileMode === 'brand' ? (
          <>
            {brand}
            {mobileAction ? <div className="sq-top-app-bar__mobile-action">{mobileAction}</div> : null}
          </>
        ) : (
          <>
            <div className="sq-top-app-bar__mobile-title">
              {mobileMode === 'back' ? (
                <button aria-label="Voltar" className="sq-top-app-bar__back" onClick={onMobileBack} type="button">
                  <Icon name="arrow_back" size={24} />
                </button>
              ) : null}
              <strong>{mobileTitle}</strong>
            </div>
            {mobileAction ? <div className="sq-top-app-bar__mobile-action">{mobileAction}</div> : null}
          </>
        )}
      </div>
    </header>
  );
}
