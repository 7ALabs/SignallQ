---
description: Checklist de release gate — critérios que devem ser atendidos antes de qualquer entrega ir para produção (Android ou PWA).
---

## Quando usar
Gema executa antes de marcar qualquer task como DONE e antes de toda release.

## Critérios obrigatórios (gate bloqueante)

### Código
- [ ] Todos os critérios de aceite da user story atendidos?
- [ ] Sem regressão detectada nas flows principais?
- [ ] TypeScript / Kotlin compilando sem erros?
- [ ] Nenhum TODO crítico não resolvido introduzido?

### Testes
- [ ] Testes unitários passando?
- [ ] Testes de integração (se existem) passando?
- [ ] Fluxo principal testado manualmente (smoke test)?

### Qualidade
- [ ] Sem crash crashante nos logs?
- [ ] Performance aceitável (sem janks óbvios na UI)?
- [ ] Sem log de debug excessivo em produção?

### Documentação
- [ ] CHANGELOG atualizado?
- [ ] versionCode/versionName incrementados (Android)?
- [ ] Manifest version atualizado (PWA)?

## Critérios desejáveis (não bloqueantes)

- [ ] Cobertura de testes mantida ou melhorada?
- [ ] Lighthouse score PWA ≥ 80?
- [ ] Screenshots/docs atualizados se UI mudou?

## Resultado

**APROVADO** → task vai para DONE, Gema registra data e artefatos.

**BLOQUEADO** → task volta para IN_PROGRESS com lista dos itens não atendidos.

**PARCIAL** → itens desejáveis pendentes documentados como follow-up task no backlog.
