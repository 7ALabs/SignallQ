"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import clsx from "clsx";

const ITENS = [
  { key: "home", label: "Velocidade", href: "/" },
  { key: "historico", label: "Histórico", href: "/historico" },
  { key: "como-medimos", label: "Como funciona", href: "/como-medimos" },
  { key: "sobre", label: "Quem somos", href: "/quem-somos" },
  { key: "privacidade", label: "Privacidade", href: "/privacidade" },
];

export function SiteNav() {
  const pathname = usePathname();
  const [menuOpen, setMenuOpen] = useState(false);

  // Fecha o menu mobile ao navegar (troca de rota) — ajuste de estado durante o
  // render (padrão React para "resetar estado quando uma prop muda"), não em
  // efeito, para não disparar setState síncrono dentro de useEffect.
  const [lastPathname, setLastPathname] = useState(pathname);
  if (pathname !== lastPathname) {
    setLastPathname(pathname);
    setMenuOpen(false);
  }

  let active = "home";
  if (pathname?.includes("/historico")) active = "historico";
  else if (pathname?.includes("/como-medimos")) active = "como-medimos";
  else if (pathname?.includes("/quem-somos")) active = "sobre";
  else if (pathname?.includes("/privacidade")) active = "privacidade";

  // Fecha o menu mobile ao pressionar Esc.
  useEffect(() => {
    if (!menuOpen) return;
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Escape") setMenuOpen(false);
    };
    document.addEventListener("keydown", onKeyDown);
    return () => document.removeEventListener("keydown", onKeyDown);
  }, [menuOpen]);

  return (
    <div className="sticky top-0 z-[3] w-full box-border bg-[color:var(--bg-primary)]">
      <div className="mx-auto max-w-[1280px] min-h-[76px] flex items-center justify-between gap-4 py-[14px] px-[20px] box-border">
        <Link href="/">
          <img
            className="sq-logo-light h-[32px] w-auto block shrink-0"
            src="/assets/signallq-lockup-light-bg-v5.png"
            alt="SignallQ"
          />
          <img
            className="sq-logo-dark h-[32px] w-auto hidden shrink-0"
            src="/assets/signallq-lockup-dark-bg-v5.png"
            alt="SignallQ"
          />
        </Link>

        <div className="hidden md:flex items-center gap-[28px]">
          {ITENS.map((it) => {
            const isActive = it.key === active;
            return (
              <Link
                key={it.key}
                href={it.href}
                className={clsx(
                  "whitespace-nowrap pb-[4px] font-medium text-[14px] leading-[1.43] font-sans transition-colors border-b-2",
                  isActive
                    ? "text-[color:var(--accent)] border-[color:var(--accent)]"
                    : "text-[color:var(--text-primary)] border-transparent hover:text-[color:var(--accent)]"
                )}
              >
                {it.label}
              </Link>
            );
          })}
          <div className="flex items-center gap-[6px] rounded-full py-[8px] px-[14px] bg-[color-mix(in_srgb,_var(--accent)_12%,_transparent)]">
            <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
              android
            </span>
            <span className="whitespace-nowrap font-medium text-[13px] leading-[1.3] text-[color:var(--accent)]">
              App
            </span>
          </div>
        </div>

        <button
          type="button"
          aria-label={menuOpen ? "Fechar menu" : "Abrir menu"}
          aria-expanded={menuOpen}
          aria-controls="site-nav-mobile-menu"
          onClick={() => setMenuOpen((v) => !v)}
          className="md:hidden w-[40px] h-[40px] rounded-full flex items-center justify-center cursor-pointer bg-transparent border-none p-0"
        >
          <span className="material-symbols-outlined text-[24px] text-[color:var(--text-primary)]">
            {menuOpen ? "close" : "menu"}
          </span>
        </button>
      </div>

      {menuOpen && (
        <>
          <div
            aria-hidden="true"
            onClick={() => setMenuOpen(false)}
            className="md:hidden fixed inset-x-0 bottom-0 top-[76px] z-[2] bg-black/40"
          />
          <div
            id="site-nav-mobile-menu"
            role="menu"
            className="md:hidden absolute left-0 right-0 top-full z-[3] flex flex-col gap-1 border-t border-[color-mix(in_srgb,_var(--border)_18%,_transparent)] bg-[color:var(--bg-primary)] p-3 box-border"
          >
            {ITENS.map((it) => {
              const isActive = it.key === active;
              return (
                <Link
                  key={it.key}
                  role="menuitem"
                  href={it.href}
                  className={clsx(
                    "rounded-[12px] px-4 py-3 font-medium text-[15px] leading-[1.4] font-sans",
                    isActive
                      ? "text-[color:var(--accent)] bg-[color-mix(in_srgb,_var(--accent)_12%,_transparent)]"
                      : "text-[color:var(--text-primary)]"
                  )}
                >
                  {it.label}
                </Link>
              );
            })}
            <div className="mt-1 self-start flex items-center gap-[6px] rounded-full py-[8px] px-[14px] bg-[color-mix(in_srgb,_var(--accent)_12%,_transparent)]">
              <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
                android
              </span>
              <span className="whitespace-nowrap font-medium text-[13px] leading-[1.3] text-[color:var(--accent)]">
                App
              </span>
            </div>
          </div>
        </>
      )}

      <div className="absolute left-0 right-0 -bottom-[16px] h-[16px] pointer-events-none bg-gradient-to-b from-[color-mix(in_srgb,_var(--bg-primary)_92%,_transparent)] to-transparent" />
    </div>
  );
}
