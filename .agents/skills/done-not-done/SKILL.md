---
description: Checklist Done/Not Done — critérios objetivos que definem se uma entrega está completa. Gema executa antes de mover qualquer task para DONE.
---

## Quando usar
Gema executa como última verificação antes de marcar task como DONE.

## Done significa

Uma task está DONE quando **todos** os itens abaixo são verdadeiros:

### Código
- [ ] Código implementado e revisado por Gema (`/qa-acceptance-check`)
- [ ] Todos os critérios de aceite da user story atendidos
- [ ] Nenhum bug crítico sem resolução
- [ ] Sem regressão nos flows principais (`/regression-check`)
- [ ] Compilando sem erros (TypeScript / Kotlin)

### Qualidade
- [ ] Smoke test do flow principal passou
- [ ] Performance aceitável (sem janks ou travamentos óbvios)

### Documentação
- [ ] CHANGELOG atualizado (`/changelog-update`)
- [ ] Versão bumped se for release (versionCode/versionName ou manifest)
- [ ] Arquivo de task atualizado com status DONE e data

### Handoff
- [ ] Claudete notificada do fechamento
- [ ] Follow-up tasks (bugs menores, melhorias futuras) criadas no backlog se necessário

## Not Done — situações comuns

| Situação | Status correto |
|---|---|
| Código feito, mas sem teste do flow | IN_PROGRESS — testar antes |
| Critério de aceite não atendido | BLOCKED — devolver para implementador |
| CHANGELOG não atualizado | IN_PROGRESS — atualizar primeiro |
| Bug crítico encontrado | BLOCKED |
| Bug menor, flow principal OK | DONE com follow-up task criada |

## Registro no arquivo de task

```markdown
## Fechamento — [DATA]
**Status:** DONE
**Fechado por:** Gema
**Critérios atendidos:** X/X
**Follow-up tasks:** [lista ou "nenhuma"]
**CHANGELOG:** atualizado em [arquivo]
```
