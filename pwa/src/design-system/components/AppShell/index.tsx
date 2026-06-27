import { ReactNode } from 'react';

export interface AppShellProps {
  children: ReactNode;
  header: ReactNode;
}

export function AppShell({ children, header }: AppShellProps) {
  return (
    <div className="sq-app-shell">
      {header}
      <main className="sq-app-shell__main">{children}</main>
    </div>
  );
}
