---
title: "ADR-014 — Squad canônico: ai-governance/agents/ como única fonte da verdade"
description: "Consolida a governança de agentes em torno de ai-governance/agents/ (7 agentes org) e superseda ADR-006 (squad de 5) e ADR-007 (agente iOS pendente)."
type: "adr"
status: "ativo"
owner: "Luiz (CEO)"
last_updated: "2026-08-15"
version: "1.0.0"
---

# ADR-014 — Squad canônico: `ai-governance/agents/` como única fonte da verdade

- **Status:** Aceito
- **Data:** 2026-08-15
- **Autor:** Luiz (CEO) via consolidação orientada por Claudete
- **Substitui:** [ADR-006](ADR-006-workflow-squad-5-agentes.md), [ADR-007](ADR-007-ios-scaffolding-sem-agente.md)
- **Documentos correlatos que passam a ser histórico:** [DECISAO_DEMISSAO_FELIPE_2026-07-09](DECISAO_DEMISSAO_FELIPE_2026-07-09.md), [DECISAO_DEMISSAO_LIA_2026-07-25](DECISAO_DEMISSAO_LIA_2026-07-25.md), [DECISAO_CONSOLIDACAO_SQUAD_7ALABS_2026-07-23](DECISAO_CONSOLIDACAO_SQUAD_7ALABS_2026-07-23.md)

## Contexto

Auditoria de 2026-08-15 encontrou **três squads distintos** citados como ativos em documentos vigentes deste repositório:

1. **`ai-governance/agents/`** (7 agentes org): Claudete, Camilo, Renan, Juliana, Marcos, Gustavo, Caio.
2. **Squad de 5** definido em [ADR-006](ADR-006-workflow-squad-5-agentes.md) (2026-07-05): Claudete, Camilo, Felipe, Lia, Gema — referenciado em `.claude/fluxos/*.md`, `.claude/commands/task.md` e no gate único "Rhodolfo" citado em [REVIEW_FLOW](../../.claude/fluxos/REVIEW_FLOW.md).
3. **Squad 7ALabs** consolidado em [2026-07-23](DECISAO_CONSOLIDACAO_SQUAD_7ALABS_2026-07-23.md) e vivendo em `~/.claude/agents/`: Claudete, Camilo, Lia, Rhodolfo, Juninho — posteriormente Marina substituindo Lia ([2026-07-25](DECISAO_DEMISSAO_LIA_2026-07-25.md)).

A drift criou:
- Fluxos em `.claude/fluxos/*.md` apontando para `.claude/agents/*.md` que **não existe** neste repo.
- Comando `/task` roteando features para Linear (substituído por GitHub Issues em 2026-07-09) e chamando `scripts/agent-handoff.sh` (declarado depreciado pelo próprio ADR-006).
- Agentes fantasmas (Felipe demitido, Lia demitida, Gema/Rhodolfo/Juninho/Marina nunca formalizados na governança organizacional) continuando a aparecer como responsáveis em documentação operacional viva.

O [AGENTS.md](../../AGENTS.md) deste repositório já declarava `ai-governance/agents/` como fonte canônica, mas nada operacional havia sido alinhado a essa declaração.

## Decisão

### 1. Única fonte da verdade

**`ai-governance/agents/`** é a fonte canônica dos agentes aplicáveis ao SignallQ. Para este repositório, os agentes aplicáveis são:

| Papel | Agente | Domínio no SignallQ |
|---|---|---|
| Produto e portfólio (líder funcional) | **Claudete** | Priorização, critérios de aceite, decisões de produto |
| Mobile, backend, integrações, plataforma (responsável técnico) | **Camilo** | Android Kotlin/Compose, Workers Cloudflare, Firebase |
| Design, UX, jornadas | **Juliana** | Material 3, design system, protótipos |
| Growth, aquisição, ASO | **Marcos** | Store, campanhas, funil, SEO editorial |
| Operações e dados | **Gustavo** | Métricas, observabilidade, qualidade de dados |
| Revisão independente (gate) | **Caio** | Arquitetura, segurança, testes, prontidão de release |

Renan (frontend web) não é aplicável neste repo — SignallQ é Android + Workers. Renan atua em `signallq-web`.

### 2. Personas legadas

Ficam explicitamente fora da descoberta e do roteamento ativo:

- **Felipe** — demitido em 2026-07-09; Admin/Cloudflare herdado por Camilo (mantido).
- **Lia** — demitida em 2026-07-25; substituída conceitualmente por **Juliana** no papel de design.
- **Gema, Rhodolfo, Juninho, Marina** — nunca formalizados em `ai-governance/`; papéis absorvidos por **Caio** (revisão/QA/release) e **Juliana** (UX).
- **Claudio, Nina, Taisa, Marcelo, Otávio** — arquivados por ADR-006 e mantidos arquivados.

Personas legadas devem viver em `docs_ai/archive/ai-governance/legacy-agents/` (pasta a ser criada quando houver material a mover) e nunca ser citadas como responsáveis ativos em fluxos, comandos ou skills.

### 3. Fluxo de trabalho

Substitui o "squad de 5" do ADR-006. O fluxo canônico está definido em [`ai-governance/policies/demand-routing.md`](../../../ai-governance/policies/demand-routing.md) e no [contrato operacional](../../../ai-governance/policies/agent-operating-contract.md). Resumo aplicado ao SignallQ:

1. **Refino e decomposição** — Claudete refina a demanda, define critérios de aceite, roteia.
2. **Design condicional** — Juliana entra **antes** da implementação apenas quando a mudança é visual/de fluxo (tela nova, navegação, microcopy). Bug ou lógica pura pula Juliana.
3. **Implementação** — Camilo executa Android/Workers/Admin/Firebase.
4. **Revisão independente** — Caio revisa quando houver código, segurança, produção, regressão ou risco relevante. Caio não implementa o que revisa.
5. **Escalação** — conflitos de escopo, custo, risco ou prioridade sobem para Luiz.

**Loop de revisão:** máximo 2 rodadas Caio ↔ implementador; 3ª divergência escala para Claudete decidir (aceitar débito, repriorizar, reescopar).

**WIP:** máximo 1 task In Progress por agente.

### 4. Handoff

Estado do trabalho vive em **GitHub Issues** (status) e **GitHub PR** (execução). Linear deixou de ser fonte da verdade em 2026-07-09. Scripts `agent-handoff.sh`, `notify.sh`, `discord_notify.sh` estão depreciados e não são mecanismo de handoff — ficam no repo apenas por inércia até auditoria de remoção.

Roteamento: **bug** → GitHub Issues label `type:bug`; **feature/task/refactor/docs** → GitHub Issues com título `Task - ...` ou `Feat - ...`.

Formato do handoff (comentário na issue):

```
De: [agente] Para: [agente] — Decisão: [o que foi decidido]. Pendente: [o que falta]. Riscos: [riscos].
```

### 5. iOS

Substitui ADR-007. iOS permanece descontinuado (nota de 2026-07-04 no ADR-007). Se voltar a ser desenvolvimento ativo, Camilo é o responsável técnico natural — nova persona `camilo-ios` só é criada por decisão explícita do Luiz, não por presunção.

## Consequências

- `.claude/fluxos/*.md` reescritos para apontar `ai-governance/agents/` como fonte da verdade e usar o squad canônico. Referências a `.claude/agents/*.md` (path inexistente) removidas.
- `.claude/commands/task.md` atualizado: repo `buildea-labs/signallq`, roteamento GitHub Issues em ambos trilhos, sem chamada a `agent-handoff.sh`, agentes canônicos (Camilo para código, Juliana para design, Caio para review).
- ADR-006 e ADR-007 marcados como **Superseded** com header apontando para este documento.
- Decisões de demissão (Felipe, Lia) e consolidação 7ALabs preservadas como histórico — não são fonte da verdade operacional.
- `agent-state.json` obsoleto (2026-05-26, mecanismo de handoff Discord) removido.

## Pendências (Luiz)

- **Formalizar Marina/Juninho/Rhodolfo em `ai-governance/`** se o Luiz quiser que a estrutura organizacional reflita a operação real de 7ALabs. Enquanto essa decisão não vier, a operação SignallQ usa os 6 agentes listados acima.
- **Mover skills que citam Lia/Felipe/Gema como responsáveis** (`auditar-ux`, `motor-diagnostico`, `signallq-pro-design`, `cloudflare-d1-console`, `estimativa-impacto`, `protocolo-ci-android`, `gerar-docs`, `checar-release`) para citar Juliana/Camilo/Caio conforme aplicável. Fora do escopo desta rodada — cada skill deve ser corrigida oportunisticamente quando tocada, conforme regra de higiene (seção 8).
- **Auditar `~/.claude/agents/`** (agentes user-level) e decidir se coexistem com `ai-governance/` (interpretação atual) ou se são substituídos.
- ~~**Remover fisicamente** `scripts/agent-handoff.sh`, `notify.sh`, `discord_notify.sh` após confirmar que nada em CI/hooks os invoca.~~ **Resolvido em 2026-08-15:** movidos para `scripts/legacy/` (não removidos — git preserva, mas fora do fluxo ativo). Ver [`scripts/legacy/README.md`](../../../scripts/legacy/README.md).
