# Transição Closed Beta → Open Beta → Produção

> Atualizado em 2026-06-28.

## Visão Geral

| Fase | Track Play Store | Audiência | Duração mín. | Milestone |
|---|---|---|---|---|
| Closed Beta | Teste interno/fechado | 10-30 convidados | 14 dias | M2 (31/07) |
| Open Beta | Teste aberto | Qualquer pessoa | 14 dias | M4 (21/08) |
| Produção | Produção (staged) | Público | Permanente | M5 (04/09) |

## Closed Beta → Open Beta

### Pré-requisitos

- Critérios de saída do Closed Beta atendidos (ver BETA_CRITERIA.md)
- Go/no-go M3 aprovado (ver GO_NOGO_CHECKLIST.md)
- App publicado na Play Store (track produção com acesso restrito ou track de teste aberto)

### Passos

1. Play Console: criar release no track de teste aberto
2. Usar o mesmo AAB já aprovado no closed beta
3. Ativar link público de opt-in
4. Publicar landing page com link para o teste aberto
5. Monitorar métricas por 14 dias

## Open Beta → Produção

### Pré-requisitos

- Critérios de saída do Open Beta atendidos
- Go/no-go M5 aprovado
- Plano de hypercare ativo (ver HYPERCARE_PLAN.md)

### Passos

1. Play Console: promover release do teste aberto para produção
2. Staged rollout: 10% (D+0) → 25% (D+2) → 50% (D+4) → 100% (D+7)
3. Ativar hypercare (monitoramento intensivo 30 dias)
4. Comunicar lançamento

## Cadência de Releases Pós-lançamento

| Tipo | Frequência | Conteúdo |
|---|---|---|
| Patch (x.y.Z) | Conforme necessário | Bugfixes, hotfixes |
| Minor (x.Y.0) | Quinzenal/mensal | Features novas, melhorias |
| Major (X.0.0) | Trimestral+ | Mudanças arquiteturais |
