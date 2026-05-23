---
description: Revisa microcopy visível ao usuário — títulos, botões, estados, mensagens de erro, respostas de IA — garantindo clareza, tom adequado e ausência de jargão técnico.
---

## Quando usar
Sempre que houver texto novo ou modificado visível ao usuário. Lia executa em modo Haiku para revisão simples.

## Checklist de microcopy
- [ ] Sem jargão técnico que o usuário comum não entende.
- [ ] Frases curtas (máximo 2 linhas em estado de erro/loading).
- [ ] Tom consistente: direto, humano, não robótico.
- [ ] Botões: verbo + objeto. Ex: "Testar velocidade" não "OK".
- [ ] Erros explicam o que aconteceu E o que fazer. Não só "Erro".
- [ ] Loading states comunicam o que está acontecendo. Não só "Carregando...".
- [ ] Respostas de IA não usam "Olá!" ou "Claro!" como abertura.
- [ ] Nomes próprios do produto corretos: "Linka", "linka SpeedTest".

## Output
Lista de itens: ✅ OK | ❌ Problema + sugestão de correção.

## Limites
- Não decide fluxo — apenas verifica copy.
- Mudanças de copy de impacto alto (diagnóstico, IA response) → Lia em modo Sonnet.
