---
description: Gera um resumo estruturado de falhas de teste — identifica o que falhou, a severidade e o próximo passo. Executado por Gema após qualquer falha de teste.
---

## Quando usar
Após `./gradlew test` (Android) ou `npm test` (PWA) reportar falhas. Gema executa para resumir e priorizar antes de devolver para implementador.

## Formato de saída

```
## Resumo de Falhas de Teste — [DATA] [PROJETO]

### Resultado geral
- Total de testes: X
- Passando: X
- Falhando: X
- Ignorados: X

### Falhas críticas (bloqueiam entrega)
1. [NomeDaClasse.nomeDoTeste]
   Erro: [mensagem de erro resumida]
   Provável causa: [hipótese em 1 linha]
   Ação: [o que Camilo/Renan deve fazer]

### Falhas menores (não bloqueiam)
1. [NomeDaClasse.nomeDoTeste]
   Erro: [mensagem resumida]
   Ação: follow-up task

### Testes novos ausentes
- Feature [X] implementada sem testes unitários correspondentes.
  Recomendação: criar testes para [lista de casos críticos]
```

## Critério de severidade

| Severidade | Critério |
|---|---|
| Crítico (bloqueia) | Teste de integração ou smoke test falhou |
| Crítico (bloqueia) | Falha em módulo `:core*` que afeta múltiplas features |
| Menor | Falha em teste unitário isolado sem impacto em contrato |
| Aviso | Feature sem teste unitário correspondente |

## Limites
- Gema resume, não diagnostica a causa raiz — isso é tarefa de Camilo/Renan.
- O resumo vai para o arquivo de task em `.claude/tasks/active/` na seção `## QA Check`.
