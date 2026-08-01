"use client";

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

export function SiteNav({ mobile = false }: { mobile?: boolean }) {
  const pathname = usePathname();
  let active = "home";
  if (pathname?.includes("/historico")) active = "historico";
  else if (pathname?.includes("/como-medimos")) active = "como-medimos";
  else if (pathname?.includes("/quem-somos")) active = "sobre";
  else if (pathname?.includes("/privacidade")) active = "privacidade";

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

        {!mobile && (
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
        )}

        {mobile && (
          <div className="w-[40px] h-[40px] rounded-full flex items-center justify-center cursor-pointer">
            <span className="material-symbols-outlined text-[24px]">menu</span>
          </div>
        )}
      </div>
      <div className="absolute left-0 right-0 -bottom-[16px] h-[16px] pointer-events-none bg-gradient-to-b from-[color-mix(in_srgb,_var(--bg-primary)_92%,_transparent)] to-transparent" />
    </div>
  );
}
