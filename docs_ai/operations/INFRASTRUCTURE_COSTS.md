# Custos de Infraestrutura — SignallQ

> Atualizado em 2026-06-28. Revisão obrigatória a cada milestone.

## Resumo

O SignallQ opera **inteiramente em free tiers** na fase atual. Custos reais surgem apenas com escala de usuários ou publicação na Play Store.

## Serviços e Limites

### Cloudflare Workers (Free)

| Recurso | Limite Free | Uso Estimado (1k usuários) | Risco |
|---|---|---|---|
| Requests/dia | 100.000 | ~5.000 | Baixo |
| CPU time/invocation | 10ms | ~3-5ms | Baixo |
| Workers ativos | 10 | 3 (ai-diagnosis, admin, privacy) | Baixo |

### Cloudflare D1 (Free)

| Recurso | Limite Free | Uso Estimado (1k usuários) | Risco |
|---|---|---|---|
| Rows read/dia | 5.000.000 | ~50.000 | Baixo |
| Rows written/dia | 100.000 | ~5.000 | Baixo |
| Storage | 5 GB | ~100 MB | Baixo |

### Cloudflare AI (Workers AI)

| Recurso | Limite Free | Uso Estimado | Risco |
|---|---|---|---|
| Neurons/dia | 10.000 | Variável por modelo | Médio |
| Modelo atual | Qwen3 30B MoE FP8 | ~300 neurons/request | Médio |
| Fallback | Gemini Flash (Google) | Free tier separado | Baixo |

**Alerta:** Qwen3 30B consome mais neurons que modelos menores. Com 1k usuários fazendo 2-3 diagnósticos/dia, pode exceder o free tier. Monitorar via Admin Panel.

### Firebase (Spark — Free)

| Recurso | Limite Free | Uso Estimado (1k usuários) | Risco |
|---|---|---|---|
| Crashlytics | Ilimitado | N/A | Nenhum |
| Analytics | Ilimitado | N/A | Nenhum |
| App Distribution | Ilimitado (testers) | ~20 testers | Nenhum |
| Cloud Storage | 5 GB | Não usado | Nenhum |

### Google Play Console

| Item | Custo | Recorrência |
|---|---|---|
| Conta de desenvolvedor | $25 (R$~130) | Único |
| Listagem | Grátis | — |
| Play App Signing | Grátis | — |

### GitHub

| Recurso | Limite Free | Uso |
|---|---|---|
| Repositórios privados | Ilimitado | 1 (monorepo) |
| Actions (CI/CD) | 2.000 min/mês | ~200 min/mês estimado |
| Storage | 500 MB (packages) | Não usado |

## Custo Total Estimado

| Fase | Custo Mensal | Notas |
|---|---|---|
| **Desenvolvimento (atual)** | R$ 0 | Tudo em free tier |
| **Beta Fechado (M2)** | R$ 0 | ~50 usuários, dentro dos limites |
| **Play Store (M3)** | R$ 130 (único) | Conta Google Play |
| **Open Beta (M4, ~500 users)** | R$ 0 | Provável dentro dos limites |
| **Produção (M5, ~5k users)** | R$ 0 - R$ 50/mês | Workers AI pode exceder free tier |
| **Escala (10k+ users)** | R$ 50 - R$ 200/mês | Workers Paid ($5/mês) + D1 + AI |

## Gatilhos de Upgrade

| Gatilho | Ação | Custo |
|---|---|---|
| >80k requests/dia Workers | Upgrade para Workers Paid | $5/mês |
| >8k neurons/dia AI | Reduzir modelo ou upgrade | $5-20/mês |
| >4M rows read/dia D1 | Upgrade D1 | $0.001/M reads |
| >1.5k min/mês GitHub Actions | Upgrade ou otimizar CI | $4/mês |

## Monitoramento

- **Cloudflare Dashboard:** Workers analytics, D1 metrics, AI usage
- **SignallQ Admin Panel:** `/admin/ai-usage`, `/admin/diagnostics/intelligence`
- **Firebase Console:** Crashlytics, Analytics
- **GitHub:** Actions usage em Settings > Billing

## Decisões Registradas

1. **Modelo AI:** Qwen3 30B escolhido por qualidade de resposta em PT-BR. Se custo escalar, considerar downgrade para modelo menor.
2. **Sem banco pago:** D1 (SQLite) atende a necessidade. Sem Supabase, PlanetScale ou similar.
3. **Sem CDN adicional:** Cloudflare já serve como CDN para Workers e pages.
