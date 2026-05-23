---
description: Resume as mudanças recentes no repositório (git diff, git log) de forma objetiva para contextualizar agentes que precisam entender o que mudou sem reler tudo.
---

## Quando usar
Ao retomar sessão. Ao fazer handoff. Quando Gema precisa saber o que foi alterado sem ler cada arquivo.

## Passos
1. Rodar `git status` para ver estado atual.
2. Rodar `git diff --stat HEAD` para ver arquivos alterados.
3. Rodar `git log --oneline -10` para ver commits recentes.
4. Para cada arquivo alterado relevante, extrair trecho significativo se necessário.
5. Resumir em formato compacto.

## Output esperado
```
Mudanças recentes:
- [arquivo]: [o que mudou em 1 linha]
- [arquivo]: [o que mudou em 1 linha]
Commits: [últimos 3 commits em 1 linha cada]
Estado: [Clean | Uncommitted changes | Conflicts]
```

## Limites
- Não interpretar se a mudança é correta — apenas resumir.
- Sem análise de qualidade aqui — isso é da Gema.
