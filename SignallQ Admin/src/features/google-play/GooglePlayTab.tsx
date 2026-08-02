import React from "react";
import { GooglePlayPage } from "./GooglePlayPage";
import { AppEnvironment } from "../../types/admin";

interface GooglePlayTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

export const GooglePlayTab: React.FC<GooglePlayTabProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  return (
    <GooglePlayPage
      environment={environment}
      period={period}
      onNavigate={onNavigate}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
