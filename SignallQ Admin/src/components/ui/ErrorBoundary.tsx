import React from "react";
import { alpha } from "../../utils/color";

interface ErrorBoundaryProps {
  children: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  message: string;
}

// Evita que um erro de render em uma aba derrube o painel inteiro (tela preta).
// Sem isto, qualquer exceção no render desmonta toda a árvore React.
export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { hasError: false, message: "" };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, message: error?.message ?? "" };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo) {
    console.error("Erro de render capturado pelo ErrorBoundary:", error, info);
  }

  handleReset = () => {
    this.setState({ hasError: false, message: "" });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div
          className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 rounded-[8px]"
          style={{
            border: `1px solid ${alpha("var(--sq-error)", 20)}`,
            backgroundColor: alpha("var(--sq-error)", 5),
          }}
        >
          <h4
            className="text-sm font-semibold uppercase tracking-wider font-mono"
            style={{ color: "var(--sq-error)" }}
          >
            Erro ao renderizar a tela
          </h4>
          <p
            className="text-xs mt-2 max-w-md"
            style={{ color: "var(--sq-text-secondary)" }}
          >
            {this.state.message || "Ocorreu um erro inesperado ao montar este painel."}
          </p>
          <button
            onClick={this.handleReset}
            className="mt-4 px-4 py-2 text-xs rounded-xl font-mono transition-all"
            style={{
              backgroundColor: alpha("var(--sq-error)", 10),
              border: `1px solid ${alpha("var(--sq-error)", 20)}`,
              color: "var(--sq-error)",
            }}
          >
            TENTAR NOVAMENTE
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
