---
name: design-review
description: Revisão de UX/UI, Material Design 3, hierarquia visual, acessibilidade, microcopy e consistência de telas do Linka Android e PWA.
---

Revise a tela ou feature abaixo do ponto de vista de UX/UI:

$ARGUMENTS

Use a **Lia** para análise visual e implementação de UI.

[Invocando Lia — revisão de UX/UI]

---

## Checkpoint Momento 1 — pré-implementação (OBRIGATÓRIO)

Lia entra **antes de Camilo/Renan implementar** quando Cláudio sinalizar UI obrigatória.

**Entrada:** Plano técnico do Cláudio com impacto visual

**Saída:** Lia entrega ao final do Momento 1 um handoff estruturado para Camilo/Renan:
```
Veredito visual: [Aprovado / Ressalvas / Reprovado]
Estados visuais mapeados: [lista: loading, erro, sucesso, vazio]
Composables/componentes existentes: [lista de reutilizar]
Microcopy aprovado: [textos finais, sem jargão]
Tokens MD3 relevantes: [cores, spacing, tipografia]
O que NÃO implementar: [lista de cortes]
```

**Regra:** Camilo/Renan só começam após Lia aprovar o plano visual (veredito ≠ Reprovado).

---

## Avalie

1. Hierarquia visual — o que o olho vê primeiro, segundo e terceiro
2. Clareza da informação — o usuário entende o que está acontecendo?
3. Espaçamento — padding, margin e densidade adequados ao MD3
4. Tipografia — tamanhos, pesos e roles corretos
5. Contraste — relação cor/fundo dentro dos limites de acessibilidade
6. Acessibilidade — semantics, tamanho de toque, leitura por TalkBack
7. Estados cobertos — loading, erro, sucesso e vazio
8. Microcopy — textos curtos, objetivos, sem jargão técnico
9. Material Design 3 — componentes e tokens canônicos em uso
10. Consistência com Android e PWA — divergências injustificadas
11. Excesso de informação — o que pode ser cortado sem perda de valor
12. Hierarquia de ações — botões primários e secundários bem definidos

---

## Entregue

1. **Veredito visual** — Aprovado / Aprovado com ressalvas / Reprovado
2. **Problemas críticos** — bloqueiam UX, exigem correção imediata
3. **Problemas médios** — afetam qualidade, resolver antes do release
4. **Ajustes rápidos** — melhorias pontuais, baixo risco
5. **Proposta de layout** — estrutura visual sugerida se necessário
6. **Componentes prováveis** — Composables ou componentes React existentes a usar
7. **O que deve ser cortado** — elementos que poluem sem agregar valor

---

## Checkpoint Momento 2 — pós-implementação (OBRIGATÓRIO)

Lia entra **em paralelo com Gema** após a implementação estar pronta.

**Entrada:** Código implementado de Camilo/Renan

**Foco exclusivo:** Visual implementado vs. visual planejado no Momento 1.
- ✓ O microcopy aprovado foi usado textualmente?
- ✓ Todos os estados mapeados foram implementados?
- ✓ Os tokens MD3 corretos foram aplicados (cores, spacing, tipografia)?
- ✓ Algum elemento foi adicionado/removido sem estar no handoff do Momento 1?
- ✓ Acessibilidade mantida (contraste, tamanho de toque, TalkBack)?

**Saída:** Aprovado / Aprovado com ressalvas / Reprovado

[PRÓXIMO: Gema (bugs, regressão, arquitetura) + Lia (visual, MD3, microcopy) em paralelo]
