---
description: Converte um pedido bruto (ideia, bug, dor, melhoria) em feature estruturada com contexto, motivação e escopo definido — antes de virar user story ou task.
---

## Quando usar
Quando chegar um pedido vago, ideia ou bug sem estrutura. Sempre antes de `/refine-story`.

## Passos
1. **Capturar pedido bruto** — exatamente como chegou, sem interpretar.
2. **Identificar tipo**: feature / bugfix / melhoria / dor / tech debt / docs.
3. **Identificar plataforma**: Android / PWA / Ambos / Infra.
4. **Identificar usuário afetado** e qual dor ele sente hoje.
5. **Verificar duplicata**: chamar Marcelo para buscar se algo similar já existe.
6. **Rascunhar escopo mínimo**: o menor incremento que gera valor real.
7. **Identificar dependências óbvias**: o que precisa existir antes.
8. **Passar para `/refine-story`** se o contexto for suficiente.

## Output esperado
- Tipo e plataforma
- Problema real do usuário (1-2 frases)
- Escopo mínimo proposto
- Dependências identificadas
- Próximo passo recomendado

## Limites
- Não é task ainda. Não tem critério de aceite aqui.
- Não decide prioridade — isso é da Claudete depois.
- Se o pedido for realmente um bugfix simples (≤5 arquivos) → encaminhar direto para Marcelo + Camilo/Renan.
