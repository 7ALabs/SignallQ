# Decisão — Consolidação de squad da 7ALabs (2026-07-23)

- **Status:** ativo
- **Escopo:** squad do SignallQ (Claudete, Camilo, Lia, Rhodolfo, Juninho)
- **Documento canônico:** `C:\Projetos\docs\decisions\DECISAO_CONSOLIDACAO_SQUAD_7ALABS_2026-07-23.md`
  (raiz do workspace) — este arquivo é um espelho local com o recorte específico do SignallQ.

## O que mudou para este repo

Os 5 agentes do squad (`claudete`, `camilo`, `lia`, `rhodolfo`, `juninho`) deixaram de viver em
`.claude/agents/` e passaram a ser **agentes de nível de usuário**
(`~/.claude/agents/<nome>.md`), compartilhados com o squad do SignallQ Nethal. Os arquivos antigos foram
movidos para `.claude/agents/_archive/*_2026-07-23_consolidado.md` — mantidos só como histórico,
não editar.

Motivo: o SignallQ e o SignallQ Nethal tinham squads funcionalmente idênticos com nomes diferentes
(Claudete≈Rafael, Camilo≈Caio, Lia≈Vera, Rhodolfo≈Marisa). O Luiz pediu pra tratar a 7ALabs como
uma empresa só, não uma "empresa por produto" clonada — ver decisão canônica para o raciocínio
completo.

## O que não mudou

- Convenções deste repo (issues no GitHub, hierarquia Épico > Feature > Task via Projects #10-13,
  `.claude/rules/higiene-e-padronizacao-repositorio.md`, design system) continuam exatamente as
  mesmas — os agentes globais leem este `CLAUDE.md` antes de agir, igual antes.
- Personalidade, regras operacionais e checklists de cada agente para o SignallQ são idênticos aos
  que já existiam aqui — apenas o arquivo passou a viver em outro lugar e ganhou consciência do
  SignallQ Nethal como segundo produto possível.

## Referência cruzada

Ver também `_archive/gema_2026-07-10_substituida.md` e o histórico de Felipe — esta é a terceira
mudança de composição de squad documentada com o mesmo padrão (persona arquivada, não deletada).
