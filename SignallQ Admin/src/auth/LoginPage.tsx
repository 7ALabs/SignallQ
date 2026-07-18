import React, { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { alpha } from "../utils/color";

interface LoginPageProps {
  onLogin: () => void;
}

type LoginView = "login" | "forgot" | "forgot-sent";

export function LoginPage({ onLogin }: LoginPageProps) {
  const [view, setView] = useState<LoginView>("login");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const [forgotEmail, setForgotEmail] = useState("");

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

  // Não existe endpoint de recuperação de senha no signallq-admin-worker hoje
  // (confirmado via grep antes de implementar). O protótipo To-Be MD3
  // (Md3LoginForm.dc.html) define só a segunda tela do formulário, sem
  // especificar o comportamento real de envio — decisão de produto pendente
  // com a Claudete (ver PLANO_APLICACAO_TOBE_CONSOLE_2026-07-17.md, tela 00).
  // Até essa decisão, o botão só troca de view — sem chamada de rede, mesmo
  // padrão já usado em cards "Em breve" no resto do Console.
  function handleForgotSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!forgotEmail.trim()) return;
    setView("forgot-sent");
  }

  return (
    <div
      className="min-h-screen flex items-center justify-center px-4"
      style={{ backgroundColor: "var(--sq-bg-primary)" }}
    >
      <div className="w-full max-w-[460px] flex flex-col items-center">
        {/* Logo */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center mb-5">
            {/* GH#443: caminho relativo ao BASE_URL — o Console pode ser servido em /console */}
            <img
              src={`${import.meta.env.BASE_URL}icon-192.png`}
              alt="SignallQ"
              className="w-16 h-16 rounded-[var(--radius-card)]"
            />
          </div>
          <h1
            className="text-[26px] font-sans font-bold tracking-tight"
            style={{ color: "var(--sq-text-primary)" }}
          >
            SignallQ Admin
          </h1>
          <p className="text-[15px] mt-1.5" style={{ color: "var(--sq-text-tertiary)" }}>
            Console técnico do SignallQ
          </p>
        </div>

        {/* Formulário — sem card/container, direto sobre o background da tela
            (Md3LoginContent.dc.html) */}
        {view === "login" && (
          <form onSubmit={handleSubmit} className="w-full flex flex-col gap-[22px]">
            <div>
              <label
                htmlFor="login-email"
                className="block text-xs font-medium uppercase tracking-wider mb-2"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                E-mail
              </label>
              <input
                id="login-email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@exemplo.com"
                autoFocus
                autoComplete="email"
                className="w-full h-[54px] rounded-[var(--radius-input)] px-4 text-sm transition-colors focus:outline-none"
                style={{
                  backgroundColor: "var(--sq-bg-primary)",
                  border: "1px solid var(--sq-border)",
                  color: "var(--sq-text-primary)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = alpha("var(--sq-accent)", 60);
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${alpha("var(--sq-accent)", 15)}`;
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = "var(--sq-border)";
                  e.currentTarget.style.boxShadow = "";
                }}
              />
            </div>

            <div>
              <label
                htmlFor="login-password"
                className="block text-xs font-medium uppercase tracking-wider mb-2"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                Senha
              </label>
              <input
                id="login-password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••••"
                autoComplete="current-password"
                className="w-full h-[54px] rounded-[var(--radius-input)] px-4 text-sm transition-colors focus:outline-none"
                style={{
                  backgroundColor: "var(--sq-bg-primary)",
                  border: "1px solid var(--sq-border)",
                  color: "var(--sq-text-primary)",
                }}
                onFocus={(e) => {
                  e.currentTarget.style.borderColor = alpha("var(--sq-accent)", 60);
                  e.currentTarget.style.boxShadow = `0 0 0 2px ${alpha("var(--sq-accent)", 15)}`;
                }}
                onBlur={(e) => {
                  e.currentTarget.style.borderColor = "var(--sq-border)";
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
              className="w-full h-[54px] font-sans font-bold text-[15px] rounded-[var(--radius-button)] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              style={{ backgroundColor: "var(--sq-accent)", color: "var(--on-primary)" }}
            >
              {loading ? "Verificando..." : "Entrar"}
            </button>

            <button
              type="button"
              onClick={() => setView("forgot")}
              className="text-[13.5px] font-medium text-center cursor-pointer"
              style={{ color: "var(--sq-text-tertiary)" }}
            >
              Esqueci minha senha
            </button>
          </form>
        )}

        {view === "forgot" && (
          <form onSubmit={handleForgotSubmit} className="w-full flex flex-col gap-[18px]">
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setView("login")}
                aria-label="Voltar para login"
                className="flex items-center justify-center cursor-pointer"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                <ArrowLeft className="w-[19px] h-[19px]" />
              </button>
              <h2 className="text-[17px] font-sans font-semibold" style={{ color: "var(--sq-text-primary)" }}>
                Recuperar senha
              </h2>
            </div>
            <p className="text-[13.5px] leading-relaxed" style={{ color: "var(--sq-text-tertiary)" }}>
              Informe seu e-mail cadastrado para receber o link de redefinição.
            </p>
            <div>
              <label
                htmlFor="forgot-email"
                className="block text-xs font-medium uppercase tracking-wider mb-2"
                style={{ color: "var(--sq-text-secondary)" }}
              >
                E-mail
              </label>
              <input
                id="forgot-email"
                type="email"
                value={forgotEmail}
                onChange={(e) => setForgotEmail(e.target.value)}
                placeholder="admin@exemplo.com"
                autoFocus
                autoComplete="email"
                className="w-full h-[54px] rounded-[var(--radius-input)] px-4 text-sm transition-colors focus:outline-none"
                style={{
                  backgroundColor: "var(--sq-bg-primary)",
                  border: "1px solid var(--sq-border)",
                  color: "var(--sq-text-primary)",
                }}
              />
            </div>
            <button
              type="submit"
              disabled={!forgotEmail.trim()}
              className="w-full h-[54px] font-sans font-bold text-[15px] rounded-[var(--radius-button)] transition-colors disabled:opacity-40 disabled:cursor-not-allowed"
              style={{ backgroundColor: "var(--sq-accent)", color: "var(--on-primary)" }}
            >
              Enviar link de recuperação
            </button>
            <button
              type="button"
              onClick={() => setView("login")}
              className="text-[13.5px] font-medium text-center cursor-pointer"
              style={{ color: "var(--sq-text-tertiary)" }}
            >
              Voltar para login
            </button>
          </form>
        )}

        {view === "forgot-sent" && (
          <div className="w-full flex flex-col gap-[18px] items-center text-center">
            <p className="text-[13.5px] leading-relaxed" style={{ color: "var(--sq-text-tertiary)" }}>
              Recuperação de senha ainda não está disponível neste Console. Peça a redefinição
              diretamente ao time técnico.
            </p>
            <button
              type="button"
              onClick={() => setView("login")}
              className="text-[13.5px] font-medium text-center cursor-pointer"
              style={{ color: "var(--sq-text-tertiary)" }}
            >
              Voltar para login
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
