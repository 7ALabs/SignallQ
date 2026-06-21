---
description: Checklist de acessibilidade para telas Android (Compose/TalkBack) e PWA (WCAG/ARIA) do Linka.
---

## Quando usar
Após implementação de tela nova ou elemento interativo. Lia executa antes de Gema fechar Done.

## Checklist Android (Compose)
- [ ] Todos os elementos interativos têm `contentDescription` ou `semantics`.
- [ ] Tamanho mínimo de toque: 48dp x 48dp.
- [ ] Contraste de texto: mínimo 4.5:1 (normal), 3:1 (grande).
- [ ] TalkBack funciona sem elementos invisíveis lidos.
- [ ] Ordem de foco lógica com `traversalOrder` se necessário.
- [ ] Ícones decorativos têm `contentDescription = null`.

## Checklist PWA
- [ ] Elementos interativos têm `aria-label` ou texto visível.
- [ ] Contraste WCAG AA (4.5:1 texto normal, 3:1 texto grande).
- [ ] Foco visível em todos os elementos interativos.
- [ ] Imagens têm `alt` text.
- [ ] Formulários têm `label` associado.

## Output
Lista de itens: ✅ OK | ❌ Problema + arquivo + linha + sugestão.

## Limites
- Esta skill não implementa — apenas identifica.
- Correções vão para Camilo (Android) ou Renan (PWA).
