# Plano de Hypercare (30 dias) — SignallQ

> Atualizado em 2026-06-28. Vigência: M5 (04/09) até H1 (04/10/2026).

## Objetivo

Monitoramento intensivo nos primeiros 30 dias pós-lançamento em produção para garantir estabilidade, identificar problemas rapidamente e manter crash-free rate >= 99.5%.

## Métricas Monitoradas

| Métrica | Target | Alerta | Crítico |
|---|---|---|---|
| Crash-free rate | >= 99.5% | < 99.5% | < 99% |
| ANR rate | < 0.47% | > 0.47% | > 1% |
| Cold start | < 2s | > 2s | > 4s |
| Worker error rate | < 1% | > 1% | > 5% |
| AI diagnosis success | > 90% | < 90% | < 70% |
| DAU retention D1 | > 40% | < 40% | < 20% |
| DAU retention D7 | > 20% | < 20% | < 10% |

## Cadência

### Semana 1 (D+1 a D+7) — Vigilância máxima

- **Diário:** revisar Crashlytics, Analytics events, Worker metrics
- **2x/dia:** verificar crash rate e ANR rate
- **Ação imediata:** qualquer crash novo com >10 ocorrências = investigar em 4h
- Rollout staged: 10% → 25% (D+2) → 50% (D+4) → 100% (D+7)

### Semana 2 (D+8 a D+14) — Estabilização

- **Diário:** revisar Crashlytics
- **Semanal:** relatório de métricas consolidado
- Hotfixes se necessário (SLA 24h para P1)

### Semanas 3-4 (D+15 a D+30) — Monitoramento regular

- **3x/semana:** revisar Crashlytics
- **Semanal:** relatório consolidado
- Transição para monitoramento regular ao final

## Ferramentas

| Ferramenta | O que monitorar | URL/Acesso |
|---|---|---|
| Firebase Crashlytics | Crashes, ANRs, stack traces | Firebase Console |
| Firebase Analytics | Events, DAU, retenção | Firebase Console |
| Cloudflare Dashboard | Workers requests, errors, D1 | dash.cloudflare.com |
| SignallQ Admin | AI usage, custos, diagnósticos | Admin Panel |
| Play Console | Ratings, reviews, install stats | Play Console |

## Escalation

| Nível | Condição | Ação |
|---|---|---|
| L1 — Agentes | Crash isolado, bug menor | Fix no próximo ciclo |
| L2 — Claudete | Crash rate > 0.5%, feature quebrada | Priorizar hotfix, comunicar Luiz |
| L3 — Luiz | Crash rate > 2%, rollback necessário, custo inesperado | Decisão de rollback/halt |

## Critérios de Saída do Hypercare

- [ ] 30 dias sem P0
- [ ] Crash-free rate >= 99.5% por 7 dias consecutivos
- [ ] ANR rate < 0.47% estável
- [ ] Nenhum rollback necessário na última semana
- [ ] Retenção D7 > 20%

Ao atender todos os critérios, transicionar para monitoramento regular (semanal).
