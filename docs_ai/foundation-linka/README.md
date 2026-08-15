---
title: "Foundation Linka"
description: "Material provisório para o repo Linka, preparado aqui até o repositório nascer"
type: "referência"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-15"
---

# Foundation Linka

Esta pasta guarda o material fundacional do produto **Linka** — o segundo produto do portfólio
Buildea, exclusivo do ecossistema Apple, declarado em
[ADR-016](../decisions/ADR-016-portfolio-buildea.md). É trabalho da **Fase 8 do épico
[#1623](https://github.com/buildea-labs/signallq/issues/1623)**, a última fase.

## Por que isso está aqui e não em `linka`

O repositório `buildea-labs/linka` ainda não existe. A decisão de portfólio que autoriza sua
criação (ADR-016) vive neste repo, e a Fase 8 do épico #1623 pede que a foundation esteja pronta
*antes* do repo nascer — não que o repo nasça agora. Então o conteúdo nasce aqui, como material de
referência, e migra fisicamente quando `linka` for criado.

Nada neste diretório executa, roda CI, ou é consumido por ferramenta do SignallQ. É só texto —
templates e checklist — esperando o repositório de destino.

## Conteúdo

| Arquivo | Propósito |
|---|---|
| [`AGENTS.md.template`](AGENTS.md.template) | Template do `AGENTS.md` que vai virar a raiz do repo `linka` |
| [`squad-template.md`](squad-template.md) | Esqueleto do squad canônico Linka (3 personas em rascunho) |
| [`skills-apple-checklist.md`](skills-apple-checklist.md) | Lista do que precisa ser criado em `.claude/skills/` do repo `linka`, e o que pode ser reaproveitado como processo cross-produto |

## Instruções de migração (quando `buildea-labs/linka` nascer)

1. Criar o repositório `buildea-labs/linka` (aprovação de Luiz — publicação/criação de repo novo
   não é decisão de Camilo sozinho).
2. Copiar o conteúdo desta pasta para a raiz do repo novo:
   - `AGENTS.md.template` → `AGENTS.md` (remover o placeholder `<!-- TEMPLATE -->` do topo,
     preencher o que ainda estiver em aberto).
   - `squad-template.md` → base para `.claude/agents/{persona-pm}.md`,
     `.claude/agents/{persona-dev}.md`, `.claude/agents/{persona-revisor}.md` (Luiz aprova nomes e
     tom final antes de virar arquivo de agente ativo).
   - `skills-apple-checklist.md` → guia de criação de `.claude/skills/` do repo novo; cada skill
     nasce do zero lá, este arquivo é só a lista do que falta.
3. Adaptar caminhos relativos: as referências a `ADR-016` e a `ai-governance/policies/` que hoje
   apontam para dentro deste repo (`../decisions/...`, `../../../ai-governance/...`) viram
   referência cross-repo (`https://github.com/buildea-labs/signallq/blob/main/docs_ai/decisions/ADR-016-portfolio-buildea.md`
   ou caminho relativo equivalente, dependendo de como os repos ficam posicionados no disco de
   quem desenvolve).
4. O primeiro ADR do repo `linka` deve referenciar o ADR-016 deste repo como decisão de origem
   (conforme a seção "Cross-repo" do próprio ADR-016).
5. Confirmar a migração completa e **remover `docs_ai/foundation-linka/` deste repositório** — o
   destino final do conteúdo é `linka`, não os dois lugares ao mesmo tempo. Seguir a regra de
   remoção da [política de documentação viva](../../.claude/rules/politica-documentacao-viva.md):
   registrar no commit o SHA em que a pasta ainda existia aqui.

## Fora de escopo desta pasta

- Não cria o repositório `linka`.
- Não cria `.claude/agents/` do Linka neste repo — apenas o template em `squad-template.md`.
- Não implementa nenhuma skill Apple — apenas lista o que precisa existir.
- Não decide nomes finais de persona, stack de detalhe além do que ADR-016 já declarou, ou data de
  lançamento.
