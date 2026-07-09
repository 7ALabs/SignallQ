import React, { useState } from "react";
import { alpha } from "../utils/color";

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
            {/* GH#443: caminho relativo ao BASE_URL — o Console pode ser servido em /console */}
            <img
              src={`${import.meta.env.BASE_URL}icon-192.png`}
              alt="7Agents"
              className="w-16 h-16 rounded-[var(--radius-button)]"
            />
          </div>
          <h1
            className="text-xl font-semibold tracking-tight"
            style={{ color: "var(--sq-text-primary)" }}
          >
            7Agents Admin Console
          </h1>
          <p className="text-sm mt-1" style={{ color: "var(--sq-text-tertiary)" }}>
            Painel de administração
          </p>
        </div>

        {/* Card */}
        <div
          className="rounded-[var(--radius-card)] p-6"
          style={{
            backgroundColor: "var(--sq-bg-elevated)",
            border: `1px solid ${alpha("white", 8)}`,
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
                  border: `1px solid ${alpha("white", 10)}`,
                  color: "var(--sq-text-primary)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = alpha("var(--sq-accent)", 60);
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${alpha("var(--sq-accent)", 15)}`;
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = alpha("white", 10);
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
                  border: `1px solid ${alpha("white", 10)}`,
                  color: "var(--sq-text-primary)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = alpha("var(--sq-accent)", 60);
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${alpha("var(--sq-accent)", 15)}`;
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = alpha("white", 10);
                  e.currentTarget.style.boxShadow = "";
                }}
              />
            </div>

            {error && (
              <p
                className="text-xs rounded-lg px-3 py-2"
                style={{
                  color: "var(--sq-error)",
                  backgroundColor: alpha("var(--sq-error)", 10),
                  border: `1px solid ${alpha("var(--sq-error)", 20)}`,
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
