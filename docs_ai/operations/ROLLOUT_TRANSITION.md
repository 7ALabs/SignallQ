# Transição Closed Beta → Open Beta → Produção

- **Status:** ativo
- **Última validação:** 2026-07-23
- **Fonte de verdade das datas:** GitHub issue
  [#1222](https://github.com/7ALabs/SignallQ/issues/1222) e
  `docs_ai/decisions/DECISAO_CRONOGRAMA_LANCAMENTO_2026-07-20.md` — lançamento público (M5) em
  **21/08/2026** (revisão mais recente, substitui o 07/08 abaixo, que é de 2026-07-17). Datas de
  M4 não reconfirmadas contra a decisão de 2026-07-20 — tratar como **[a confirmar]**.

## Visão Geral

| Fase | Track Play Store | Audiência | Duração mín. | Milestone |
|---|---|---|---|---|
| Closed Beta | `internal` → `alpha` | 10-30 convidados | 14 dias | M2 (21/07, já satisfeito — ativo desde 10/07) |
| Open Beta | `beta` (teste aberto) | Qualquer pessoa | 14 dias | M4 — **[a confirmar, ver issue #1222]** |
| Produção | `production` (staged) | Público | Permanente | M5 — **21/08/2026** (issue #1222) |

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
