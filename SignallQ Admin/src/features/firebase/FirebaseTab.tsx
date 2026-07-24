import React from "react";
import { FirebasePage } from "./FirebasePage";
import { AppEnvironment } from "../../types/admin";

interface FirebaseTabProps {
  environment: AppEnvironment;
  period: string;
  onNavigate: (path: string) => void;
  triggerRefreshCounter: number;
}

export const FirebaseTab: React.FC<FirebaseTabProps> = ({
  environment,
  period,
  onNavigate,
  triggerRefreshCounter,
}) => {
  return (
    <FirebasePage
      environment={environment}
      period={period}
      onNavigate={onNavigate}
      triggerRefreshCounter={triggerRefreshCounter}
    />
  );
};
