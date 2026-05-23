---
description: Checklist de conformidade com Material Design 3 para telas Android e PWA do Linka. Executada por Lia em modo Haiku para revisões simples.
---

## Quando usar
Após implementação de tela nova ou modificação visual. Antes de Gema fechar Done.

## Checklist
### Cores e tema
- [ ] Usa tokens do `MaterialTheme` / `LocalLkTokens` — sem hardcode de cor.
- [ ] Dark mode funciona sem quebrar contraste.
- [ ] Cores seguem role MD3: primary, secondary, surface, on-surface, etc.

### Tipografia
- [ ] Usa `MaterialTheme.typography.*` — sem `fontSize` avulso.
- [ ] Hierarquia legível: headline > title > body > label.

### Componentes
- [ ] Usa componentes MD3 nativos quando existem (Button, Card, TextField, etc).
- [ ] Não cria componente custom quando o nativo resolve.
- [ ] Elevation correta por nível de superfície.

### Layout e espaçamento
- [ ] Margens e paddings seguem grid de 4dp/8dp.
- [ ] Nenhum elemento colado na borda sem padding mínimo de 16dp.

### Estados
- [ ] Loading state definido.
- [ ] Empty state definido.
- [ ] Error state definido.
- [ ] Todos os estados têm microcopy.

## Output
Lista de itens: ✅ OK | ❌ Problema | ⚠️ Atenção + descrição do problema.

## Limites
- Esta skill não decide sobre UX ou fluxo — apenas verifica conformidade MD3.
- Problemas de UX → escalar Lia para modo Sonnet.
