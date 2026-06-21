import React from "react";

interface LoadingStateProps {
  message?: string;
  rows?: number;
  id?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({
  message = "Buscando dados no Cloudflare Edge...",
  rows = 3,
  id,
}) => {
  return (
    <div
      id={id || "loading-state"}
      className="flex flex-col items-center justify-center py-12 px-4 rounded-[18px] border border-dashed border-zinc-800/80 bg-zinc-900/10"
    >
      <div className="relative flex items-center justify-center w-12 h-12 mb-4">
        {/* Glowing orbit spinner */}
        <div className="absolute inset-0 border-2 border-indigo-500/20 rounded-full" />
        <div className="absolute inset-0 border-2 border-transparent border-t-indigo-500 rounded-full animate-spin" />
        <div className="w-2.5 h-2.5 bg-indigo-500 rounded-full animate-pulse" />
      </div>

      <p className="text-xs text-neutral-400 font-mono tracking-wide">{message}</p>

      {/* Skeletons */}
      <div className="w-full max-w-md mt-6 space-y-2.5 animate-pulse">
        {Array.from({ length: rows }).map((_, idx) => (
          <div key={idx} className="h-4 bg-zinc-800/50 rounded-lg w-full flex items-center px-4 gap-4">
            <div className={`h-2 rounded bg-zinc-700 ${idx % 2 === 0 ? "w-1/4" : "w-1/3"}`} />
            <div className={`h-2 rounded bg-zinc-700/60 ${idx % 2 === 0 ? "w-2/3" : "w-1/2"}`} />
          </div>
        ))}
      </div>
    </div>
  );
};
