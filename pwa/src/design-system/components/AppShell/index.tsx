import { ReactNode } from 'react';

export interface AppShellProps {
  children: ReactNode;
  header: ReactNode;
  maxWidth?: number;
}

export function AppShell({ children, header, maxWidth = 800 }: AppShellProps) {
  return (
    <div className="sq-app-shell">
      {header}
      <main className="sq-app-shell__main" style={{ maxWidth }}>
        {children}
      </main>
    </div>
  );
}
