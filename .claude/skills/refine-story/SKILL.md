---
description: Transforma uma feature estruturada em user story formal com critérios de aceite, fora de escopo e definição de Done. Executada pela Claudete antes do task breakdown.
---

## Quando usar
Após `/intake-feature`. Antes de `/task-breakdown`. Não pular este passo.

## Passos
1. **Escrever a user story**: "Como [papel], quero [ação] para que [valor]."
2. **Definir critérios de aceite**: lista verificável de comportamentos esperados.
3. **Definir fora de escopo**: o que explicitamente NÃO está nesta entrega.
4. **Definir Definition of Done** para esta story específica.
5. **Validar com Marcelo** se a evidência de código suporta o escopo proposto.
6. **Confirmar com o usuário** se o entendimento está correto antes de prosseguir.

## Output esperado
```markdown
## User Story
Como [papel], quero [ação] para que [valor].

## Critérios de aceite
- [ ] [comportamento verificável 1]
- [ ] [comportamento verificável 2]

## Fora de escopo
- [o que não será feito nesta entrega]

## Definition of Done
- [ ] [critério de done específico]
```

## Limites
- Story deve ser pequena o suficiente para caber em 1-3 dias de trabalho focado.
- Se a story for grande demais → dividir em múltiplas stories antes de avançar.
- Critério de aceite deve ser verificável por Gema sem interpretação.
