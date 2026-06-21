---
description: Revisa o fluxo completo de diagnóstico do Linka da perspectiva do usuário — coerência de estados, progressão lógica, microcopy e clareza do resultado final.
---

## Quando usar
Ao modificar qualquer parte do fluxo de diagnóstico. Lia executa em modo Sonnet (decisão de produto).

## Passos
1. Mapear estados do fluxo atual: idle → iniciando → coletando → analisando → resultado.
2. Verificar transições: cada estado tem microcopy claro e duração previsível.
3. Verificar o resultado: o usuário entende o problema e sabe o que fazer.
4. Verificar fallbacks: o que acontece se o diagnóstico falha ou incompleto.
5. Verificar consistência Android ↔ PWA: onde diverge e se é aceitável.

## Checklist
- [ ] Cada estado de loading comunica o que está acontecendo.
- [ ] Resultado explica o problema em linguagem não-técnica.
- [ ] Resultado oferece ação clara (não só "seu sinal está fraco").
- [ ] Estado de erro explica o que falhou e o que o usuário pode tentar.
- [ ] Animações de progresso não bloqueiam leitura de informação.
- [ ] PWA não promete diagnóstico nativo impossível no browser.

## Output
Avaliação por estado + problemas encontrados + sugestão de melhoria.
