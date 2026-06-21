---
description: Registra uma decisão importante de produto, arquitetura ou processo no task file ou no log de decisões da feature. Evita que decisões se percam na conversa.
---

## Quando usar
Quando uma decisão não óbvia for tomada durante o trabalho — alternativas descartadas, trade-offs aceitos, restrições identificadas.

## Passos
1. **Identificar a decisão**: o que foi decidido.
2. **Registrar contexto**: por que essa opção foi escolhida.
3. **Registrar alternativas descartadas** e por quê foram descartadas.
4. **Adicionar ao task file** na seção `## Decisions`.
5. **Datar e assinar** com o agente que decidiu.

## Formato
```markdown
## Decisions
### [DATA] — [Agente]
**Decisão:** [o que foi decidido]
**Motivo:** [por que essa opção]
**Alternativas descartadas:** [o que foi rejeitado e por quê]
**Impacto:** [o que muda com essa decisão]
```

## Limites
- Não registrar decisões triviais (escolha de nome de variável não conta).
- Focar em: arquitetura, escopo, trade-off técnico real, restrição de plataforma.
