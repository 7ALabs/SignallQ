import { ReactNode } from 'react';

export interface ResultLayoutProps {
  children: ReactNode;
  sidebar?: ReactNode;
}

export function ResultLayout({ children, sidebar }: ResultLayoutProps) {
  return (
    <div className="sq-result-layout">
      <div>{children}</div>
      {sidebar ? <aside>{sidebar}</aside> : null}
    </div>
  );
}
