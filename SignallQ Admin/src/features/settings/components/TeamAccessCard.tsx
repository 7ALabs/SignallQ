import React from "react";

interface TeamMember {
  name: string;
  role: string;
}

// Roster estático do squad — dado literal do próprio time, não depende de integração externa.
const TEAM: TeamMember[] = [
  { name: "Claudete Souza", role: "Produto" },
  { name: "Camilo Reis", role: "Engenharia" },
  { name: "Felipe Nunes", role: "Engenharia" },
  { name: "Lia Prado", role: "Design" },
  { name: "Gema Duarte", role: "Parceiros" },
];

function initials(name: string): string {
  const parts = name.trim().split(/\s+/);
  return parts.slice(0, 2).map((p) => p.charAt(0).toUpperCase()).join("");
}

/**
 * Card compacto de ACESSO DA EQUIPE — paridade com o grid 2 colunas do
 * mockup (sec-settings), pareado com "Feature flags".
 */
export const TeamAccessCard: React.FC = () => {
  return (
    <div
      className="rounded-[var(--radius-card)] overflow-hidden sq-card-hover p-5"
      style={{ backgroundColor: "var(--bg-surface)", border: "1px solid var(--border)" }}
    >
      <div
        className="text-[11px] font-sans font-semibold uppercase tracking-[0.08em] mb-4"
        style={{ color: "var(--text-tertiary)" }}
      >
        Acesso da equipe
      </div>

      <ul className="space-y-3">
        {TEAM.map((member) => (
          <li key={member.name} className="flex items-center gap-3">
            <div
              className="w-8 h-8 rounded-full flex items-center justify-center text-[10px] font-sans font-semibold text-white shrink-0 select-none"
              style={{ background: "linear-gradient(135deg, var(--primary), var(--sq-accent-blue))" }}
            >
              {initials(member.name)}
            </div>
            <div className="min-w-0 flex-1">
              <span className="text-[12px] font-sans font-semibold block truncate" style={{ color: "var(--text-primary)" }}>
                {member.name}
              </span>
              <span className="text-[10.5px] font-sans block leading-tight truncate" style={{ color: "var(--text-tertiary)" }}>
                {member.role}
              </span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};
