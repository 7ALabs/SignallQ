import { AppEnvironment } from "../types/admin";

export interface ApiClientConfig {
  baseUrl: string;
  environment: AppEnvironment;
  timeoutMs: number;
  mocksEnabled: boolean;
}

type HttpMethod = "GET" | "POST" | "PUT" | "DELETE" | "PATCH";
type RequestBody = Record<string, unknown> | unknown[] | string | number | boolean | null;
type RequestHeaders = Record<string, string>;

const readBooleanEnv = (value: string | boolean | undefined, fallback: boolean): boolean => {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") return value.toLowerCase() === "true";
  return fallback;
};

class ApiClient {
  private config: ApiClientConfig;
  private authToken: string | null = null;
  private onAuthErrorCallback?: () => void;

  constructor() {
    const envFromVite = (import.meta.env.VITE_APP_ENV ?? "production") as AppEnvironment;
    const baseUrl = import.meta.env.VITE_ADMIN_API_BASE_URL ?? "";
    const mocksEnabled = readBooleanEnv(import.meta.env.VITE_ENABLE_MOCKS, true);

    this.config = {
      baseUrl,
      environment: envFromVite,
      timeoutMs: Number(import.meta.env.VITE_API_TIMEOUT_MS ?? 15000),
      mocksEnabled,
    };

    // Inicializa token do env — evita chamada manual de setToken() em cada página
    const secret = import.meta.env.VITE_ADMIN_API_SECRET ?? "";
    if (secret) this.authToken = secret;
  }

  public setEnvironment(env: AppEnvironment) {
    this.config.environment = env;

    if (typeof window !== "undefined") {
      window.dispatchEvent(new CustomEvent("signallq-env-changed", { detail: env }));
    }
  }

  public getEnvironment(): AppEnvironment {
    return this.config.environment;
  }

  public isMockEnabled(): boolean {
    return this.config.mocksEnabled;
  }

  public setToken(token: string | null) {
    this.authToken = token;
  }

  public onAuthError(callback: () => void) {
    this.onAuthErrorCallback = callback;
  }

  public async request<T>(
    method: HttpMethod,
    path: string,
    body?: RequestBody,
    headers: RequestHeaders = {}
  ): Promise<T> {
    if (this.config.mocksEnabled || !this.config.baseUrl) {
      throw new Error(
        `ApiClient está em modo mock ou sem VITE_ADMIN_API_BASE_URL. Use os services mockados para ${method} ${path}.`
      );
    }

    const controller = new AbortController();
    const timeoutId = window.setTimeout(() => controller.abort(), this.config.timeoutMs);

    try {
      const response = await fetch(`${this.config.baseUrl}${path}`, {
        method,
        headers: {
          "Content-Type": "application/json",
          "X-Environment": this.config.environment,
          ...(this.authToken ? { Authorization: `Bearer ${this.authToken}` } : {}),
          ...headers,
        },
        body: body === undefined ? undefined : JSON.stringify(body),
        signal: controller.signal,
      });

      if (response.status === 401 && this.onAuthErrorCallback) {
        this.onAuthErrorCallback();
      }

      if (!response.ok) {
        throw new Error(`Admin API ${method} ${path} falhou com HTTP ${response.status}`);
      }

      return (await response.json()) as T;
    } finally {
      window.clearTimeout(timeoutId);
    }
  }

  public async simulateFetch<T>(mockValue: T, filterLogs?: unknown): Promise<T> {
    console.debug("[SignallQ Admin Mock]", filterLogs ?? {});
    await new Promise((resolve) => setTimeout(resolve, 180));
    return JSON.parse(JSON.stringify(mockValue)) as T;
  }
}

export const apiClient = new ApiClient();
