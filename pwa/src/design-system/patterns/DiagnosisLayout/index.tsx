import { ReactNode } from 'react';

export interface DiagnosisLayoutProps {
  children: ReactNode;
}

export function DiagnosisLayout({ children }: DiagnosisLayoutProps) {
  return <div className="sq-diagnosis-layout">{children}</div>;
}
