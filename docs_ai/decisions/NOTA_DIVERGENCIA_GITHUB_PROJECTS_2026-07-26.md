# Nota — Divergência entre `.claude/CLAUDE.md` e os GitHub Projects reais

- **Status:** ativo (aguardando decisão de correção)
- **Última validação:** 2026-07-26
- **Fonte de verdade:** `gh project list --owner 7ALabs` (comando executado nesta data)
- **Escopo:** governança de backlog do repositório `7ALabs/SignallQ`
- **Responsável:** Claudete (dono do processo de backlog/Projects)

## Achado

`.claude/CLAUDE.md`, seção "Fontes da Verdade" → "Hierarquia obrigatória por Project", afirma que
toda issue nasce classificada em um dos **4 GitHub Projects segmentados por produto**: `SignallQ`
(#10), `SignallQ PRO` (#11), `SignallQ Admin` (#12), `SignallQ Site` (#13).

Esses 4 Projects **não existem** na org. O comando real:

```
$ gh project list --owner 7ALabs
3  SignallQ Consumer            open  PVT_kwDOEbr3Lc4BeSFX
2  Técnico Virtual — Execução   open  PVT_kwDOEbr3Lc4BeE0R
1  Técnico Virtual — Roadmap    open  PVT_kwDOEbr3Lc4BcJfY
```

retorna só **3 Projects reais**: `SignallQ Consumer` (#3) — onde a issue #952 (e as subissues desta
quebra, #1441–#1447) já estão classificadas — e dois Projects de "Técnico Virtual" (nome de marca
anterior ao rebrand para "Agente Virtual", também desatualizado, mas fora do escopo desta nota).

## Origem provável da divergência

O texto atual do `.claude/CLAUDE.md` descreve a "Hierarquia obrigatória por Project — Epico >
Feature > Task (decisão 2026-07-21)" citando os números #10–#13. É possível que esses Projects
tenham sido planejados/propostos na auditoria de governança de 20/07/2026
(`docs_ai/_archive/2026-07-20_AUDITORIA-ISSUES-GOVERNANCA-GITHUB.md`, arquivada em 2026-07-23 com a
nota "proposta nunca aprovada/executada") e a decisão de 2026-07-21 tenha documentado a intenção sem
que os Projects tenham sido de fato criados no GitHub — ou tenham sido criados e depois removidos.
Não investigado a fundo aqui; não é o escopo desta nota resolver a causa, só registrar o estado real
divergente do documentado.

## Impacto prático hoje

Nenhum bloqueio operacional: as issues seguem sendo classificadas por campos de Project (`Tipo`,
`Epico`, `Feature`) dentro do único Project real que já é usado, `SignallQ Consumer` (#3) — inclusive
a quebra de #952 em #1441–#1447 (ver comentário de quebra na própria #952, 2026-07-26). O mecanismo
de campos funciona; só a contagem/numeração de Projects no `CLAUDE.md` está errada.

## Recomendação

Corrigir `.claude/CLAUDE.md` para refletir a realidade: descrever a hierarquia Épico > Feature >
Task usando o Project real #3 (`SignallQ Consumer`), removendo a menção aos Projects #10–#13
inexistentes — ou, alternativamente, se a intenção de segmentar por produto (SignallQ/PRO/Admin/Site)
em Projects separados ainda for válida, criar os 4 Projects de fato antes de manter a instrução como
está. Decisão de qual caminho seguir é da Claudete — esta nota só documenta o achado, não resolve
sozinha (edição mecânica, candidata a handoff pro Juninho após decisão).

Achado originado na auditoria da issue #1441 (reconciliação de #952), registrado aqui por já haver
menção equivalente no comentário de quebra de #952 (2026-07-26).
