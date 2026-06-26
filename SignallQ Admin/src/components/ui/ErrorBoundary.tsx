import React from "react";

interface ErrorBoundaryState {
  hasError: boolean;
  message: string;
}

// Evita que um erro de render em uma aba derrube o painel inteiro (tela preta).
// Sem isto, qualquer exceção no render desmonta toda a árvore React.
// `declare` em props/setState: o projeto não usa @types/react, então os membros
// herdados de React.Component não são tipados — declaramos os que usamos.
export class ErrorBoundary extends React.Component {
  declare props: { children?: React.ReactNode };
  declare setState: (state: ErrorBoundaryState) => void;
  state: ErrorBoundaryState = { hasError: false, message: "" };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, message: error?.message ?? "" };
  }

  componentDidCatch(error: Error, info: unknown) {
    console.error("Erro de render capturado pelo ErrorBoundary:", error, info);
  }

  handleReset = () => {
    this.setState({ hasError: false, message: "" });
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex flex-col items-center justify-center min-h-[300px] text-center p-6 border border-red-500/20 bg-[#FF4D4F]/5 rounded-2xl">
          <h4 className="text-sm font-semibold text-[#FF4D4F] uppercase tracking-wider font-mono">
            Erro ao renderizar a tela
          </h4>
          <p className="text-xs text-neutral-400 mt-2 font-sans max-w-md">
            {this.state.message || "Ocorreu um erro inesperado ao montar este painel."}
          </p>
          <button
            onClick={this.handleReset}
            className="mt-4 px-4 py-2 text-xs bg-[#FF4D4F]/10 border border-[#FF4D4F]/20 text-[#FF4D4F] hover:bg-[#FF4D4F]/20 transition-all rounded-xl font-mono"
          >
            TENTAR NOVAMENTE
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
