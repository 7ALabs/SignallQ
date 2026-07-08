import React from "react";
import { ErrorsPage } from "./ErrorsPage";
import { AppEnvironment } from "../../types/admin";

interface ErrorsTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

export const ErrorsTab: React.FC<ErrorsTabProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  return (
    <ErrorsPage
      environment={environment}
      period={period}
      onNavigate={onNavigate}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
