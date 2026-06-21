import { AppEnvironment } from "./admin";

export interface SystemError {
  id: string;
  timestamp: string;
  source: "ai_gateway" | "android_app" | "worker" | "analytics_db";
  message: string;
  stackTrace: string;
  count: number;
  environment: AppEnvironment;
  resolved: boolean;
  affectedUserCount: number;
}
