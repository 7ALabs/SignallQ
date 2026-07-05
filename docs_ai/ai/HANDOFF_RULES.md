# Handoff Rules

> Referência completa do fluxo de trabalho: `AGENTS.md`, `docs/PIPELINE_AUTONOMO.md` e `docs_ai/ai/AGENT_WORKFLOW.md`.
> Comandos de agentes: `.claude/commands/`

## Quando fazer handoff

- A task requer expertise diferente da do agente atual.
- A conclusão da task viabiliza trabalho paralelo de outro agente.
- O escopo ultrapassa a especialização do agente.

## Formato obrigatório de handoff

```
De: [agente] Para: [agente] — Decisão: [o que foi decidido]. Pendente: [o que falta]. Riscos: [riscos identificados].
```

Não repita contexto completo — apenas o delta relevante para o próximo agente.

## Mapeamento de handoffs por situação

| Situação | De | Para |
|---|---|---|
| Task grande ou ambígua | Claudete | Cláudio |
| Implementação Android pronta para codificação | Cláudio | Camilo |
| Task Android com APIs de sistema/permissões | Cláudio | Otávio → Camilo |
| Task com impacto visual | Cláudio | Lia (antes da implementação) |
| Revisão final | Camilo | Gema + Lia (paralelo) |
| Bump de versão e changelog | Gema + Lia | Nina |
| Doc funcional/técnica/fluxo/PPT/HTML | Nina | Taisa (condicional) |
| Busca em código | Qualquer agente | Marcelo (Haiku) |
| Busca em documentação | Qualquer agente | Nina (Haiku) |

## Agentes atuais e seus papéis

| Agente | Arquivo | Papel |
|---|---|---|
| Claudete | `.claude/agents/claudete.md` | Product Owner |
| Cláudio | `.claude/agents/claudio.md` | Líder Técnico |
| Lia | `.claude/agents/lia.md` | UX/UI, Material Design 3 |
| Otávio | `.claude/agents/otavio.md` | Android Device/OS/Hardware |
| Camilo | `.claude/agents/camilo.md` | Dev Android |
| Gema | `.claude/agents/gema.md` | QA |
| Nina | `.claude/agents/nina.md` | Documentação leve, changelog (Haiku) |
| Taisa | `.claude/agents/taisa.md` | Documentação especializada |
| Marcelo | `.claude/agents/marcelo.md` | Busca e triagem de código (Haiku) |

## Referências

- `ai/AGENT_WORKFLOW.md` — fluxo completo com passo a passo
- `ai/TASK_BREAKDOWN.md` — decomposição de tasks
- `AGENTS.md` — contrato curto de operacao
