---
description: Verifica se os critérios de aceite de uma user story foram atendidos — checklist estruturado para Gema antes de aprovar entrega.
---

## Quando usar
Gema executa após implementação de Camilo ou Renan, antes de mover task para REVIEW ou DONE.

## Processo

1. Ler o arquivo de task em `.claude/tasks/active/` para localizar os critérios de aceite.
2. Para cada critério, verificar se foi atendido (leitura de código ou pergunta ao implementador).
3. Registrar resultado no arquivo de task (seção `## QA Check`).
4. Decidir: APROVADO / BLOQUEADO / PARCIAL.

## Template de QA Check (inserir no arquivo de task)

```markdown
## QA Check — [DATA]
**Revisor:** Gema
**Status:** APROVADO | BLOQUEADO | PARCIAL

### Critérios de aceite
- [x] Critério 1 — OK
- [ ] Critério 2 — FALHOU: [detalhe do problema]

### Bugs encontrados
- Bug crítico: [descrição] — BLOQUEANTE
- Bug menor: [descrição] — follow-up task

### Riscos restantes
- [risco identificado, se houver]

### Próximo passo
- APROVADO → mover para DONE
- BLOQUEADO → devolver para IN_PROGRESS com lista de itens
```

## Regras de decisão

| Situação | Decisão |
|---|---|
| Todos critérios atendidos, sem bug crítico | APROVADO |
| Critério bloqueante não atendido | BLOQUEADO |
| Critérios atendidos, bugs menores | PARCIAL — follow-up task criado |
| Critério ambíguo | Perguntar ao implementador antes de decidir |

## Limites
- Gema não reescreve código — apenas classifica e documenta.
- Bug crítico → devolver para Camilo/Renan com detalhe do problema.
