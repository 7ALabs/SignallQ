import Link from "next/link";

export interface ItemChip {
  icon: string;
  label: string;
  /** Se presente, o chip vira link de navegação em vez de bloco decorativo. */
  href?: string;
}

// Chips com texto curto (ícone + rótulo), regra de segurança de texto do
// Guia de Implementação (`Guia de Implementação.dc.html`, §5): nowrap no
// texto do chip + o container em grid 2 colunas no mobile (testado nessa
// largura) e flex-wrap centralizado no desktop — nunca grid de N colunas
// fixas que pode estourar. Hoje usado pelos chips de diagnóstico da Home;
// reaproveitável pelos filtros do Histórico (mesma regra de nowrap+wrap).
export function LinhaChips({ items }: { items: ItemChip[] }) {
  return (
    <div className="w-full grid grid-cols-2 gap-2 sm:flex sm:flex-wrap sm:justify-center">
      {items.map((item) => {
        const className =
          "flex items-center gap-2 rounded-full py-2 pr-[14px] pl-[10px] bg-[color:var(--bg-secondary)] shadow-[0_4px_14px_rgba(0,0,0,.16)]";
        const content = (
          <>
            <span className="material-symbols-outlined text-[16px] text-[color:var(--accent)]">
              {item.icon}
            </span>
            <span className="font-medium text-[13px] leading-[1.3] text-[color:var(--text-primary)] whitespace-nowrap">
              {item.label}
            </span>
          </>
        );
        return item.href ? (
          <Link key={item.label} href={item.href} className={`${className} no-underline hover:brightness-95 transition-[filter]`}>
            {content}
          </Link>
        ) : (
          <div key={item.label} className={className}>
            {content}
          </div>
        );
      })}
    </div>
  );
}
