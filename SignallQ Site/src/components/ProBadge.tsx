// Chip "PRO" com o degradê azul/roxo da identidade do SignallQ PRO (#0B6CFF →
// #6558E8) — não confundir com a paleta violeta do produto consumer.
export function ProBadge() {
  return (
    <span
      className="rounded-md px-[7px] py-[2px] text-[10px] font-bold uppercase tracking-wide text-white"
      style={{ background: 'linear-gradient(135deg, #0B6CFF, #6558E8)' }}
    >
      PRO
    </span>
  )
}
