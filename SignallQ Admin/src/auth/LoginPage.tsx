import React, { useState } from "react";

interface LoginPageProps {
  onLogin: () => void;
}

export function LoginPage({ onLogin }: LoginPageProps) {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const baseUrl = import.meta.env.VITE_ADMIN_API_BASE_URL ?? "";

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!email.trim() || !password.trim()) return;

    setLoading(true);
    setError("");

    try {
      const res = await fetch(`${baseUrl}/admin/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ email: email.trim(), password: password.trim() }),
      });

      if (res.ok) {
        onLogin();
      } else if (res.status === 401) {
        setError("E-mail ou senha inválidos.");
      } else if (res.status === 429) {
        setError("Muitas tentativas. Aguarde 15 minutos.");
      } else {
        setError("Erro inesperado. Tente novamente.");
      }
    } catch {
      setError("Não foi possível conectar ao servidor.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{ backgroundColor: "var(--sq-bg-primary)" }}
    >
      <div className="w-full max-w-sm">
        {/* Logo */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center mb-4">
            <img src="/icon-192.png" alt="SignallQ" className="w-16 h-16 rounded-2xl" />
          </div>
          <h1
            className="text-xl font-semibold tracking-tight"
            style={{ color: "var(--sq-text-primary)" }}
          >
            SignallQ Admin
          </h1>
          <p className="text-sm mt-1" style={{ color: "var(--sq-text-tertiary)" }}>
            Painel de administração
          </p>
        </div>

        {/* Card */}
        <div
          className="rounded-2xl p-6"
          style={{
            backgroundColor: "var(--sq-bg-elevated)",
            border: "1px solid color-mix(in srgb, white 8%, transparent)",
          }}
        >
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label
                className="block text-xs font-medium uppercase tracking-wider mb-2"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                E-mail
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@exemplo.com"
                autoFocus
                autoComplete="email"
                className="w-full rounded-xl px-4 py-3 text-sm transition-colors focus:outline-none"
                style={{
                  backgroundColor: "var(--sq-bg-primary)",
                  border: "1px solid color-mix(in srgb, white 10%, transparent)",
                  color: "var(--sq-text-primary)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = "color-mix(in srgb, var(--sq-accent) 60%, transparent)";
                  e.currentTarget.style.boxShadow = "0 0 0 2px color-mix(in srgb, var(--sq-accent) 15%, transparent)";
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = "color-mix(in srgb, white 10%, transparent)";
                  e.currentTarget.style.boxShadow = "";
                }}
              />
            </div>

            <div>
              <label
                className="block text-xs font-medium uppercase tracking-wider mb-2"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                Senha
              </label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••••"
                autoComplete="current-password"
                className="w-full rounded-xl px-4 py-3 text-sm transition-colors focus:outline-none"
                style={{
                  backgroundColor: "var(--sq-bg-primary)",
                  border: "1px solid color-mix(in srgb, white 10%, transparent)",
                  color: "var(--sq-text-primary)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = "color-mix(in srgb, var(--sq-accent) 60%, transparent)";
                  e.currentTarget.style.boxShadow = "0 0 0 2px color-mix(in srgb, var(--sq-accent) 15%, transparent)";
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = "color-mix(in srgb, white 10%, transparent)";
                  e.currentTarget.style.boxShadow = "";
                }}
              />
            </div>

            {error && (
              <p
                className="text-xs rounded-lg px-3 py-2"
                style={{
                  color: "var(--sq-error)",
                  backgroundColor: "color-mix(in srgb, var(--sq-error) 10%, transparent)",
                  border: "1px solid color-mix(in srgb, var(--sq-error) 20%, transparent)",
                }}
              >
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={loading || !email.trim() || !password.trim()}
              className="w-full text-white font-medium text-sm rounded-xl py-3 transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              style={{ backgroundColor: "var(--sq-accent)" }}
            >
              {loading ? "Verificando..." : "Entrar"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
