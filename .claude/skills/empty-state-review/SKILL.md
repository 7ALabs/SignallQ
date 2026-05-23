---
description: Verifica se todos os estados vazios (empty states) de uma tela estão definidos, comunicados e visualmente corretos.
---

## Quando usar
Ao revisar telas novas ou modificadas. Pré-requisito para fechar Done em qualquer feature com lista ou conteúdo dinâmico.

## Checklist
Para cada tela/componente com conteúdo dinâmico:
- [ ] Estado vazio existe (não mostra lista vazia sem mensagem).
- [ ] Microcopy do estado vazio explica o que fazer, não só "Nenhum resultado".
- [ ] Ícone ou ilustração se aplicável.
- [ ] Estado de loading definido (antes dos dados chegarem).
- [ ] Estado de erro definido (quando a busca falha).
- [ ] Botão de ação no estado vazio quando aplicável (ex: "Executar diagnóstico").

## Output
Por tela: ✅ Todos os estados definidos | ❌ [estado faltando] + sugestão.

## Limites
- Não decide o conteúdo do estado — apenas verifica se existe.
- Sugestões de microcopy → `/ux-copy-review`.
