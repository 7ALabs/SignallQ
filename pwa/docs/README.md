# SignallQ PWA — Docs

Documentação operacional do SignallQ PWA.

O Notion guarda contexto, decisões e alinhamento. Esta pasta guarda instruções versionadas junto do código.

## Sincronização Notion

Esta pasta é a transposição versionada das páginas operacionais em:

```text
SignallQ → SignallQ WebApp → Mapa Notion → /pwa/docs
```

Estado atual: transposta na branch `codex-pwa-sig-39-docs-operacionais`.

## Documentos

- `m0-definition-of-done.md` — fundação M0.
- `architecture.md` — arquitetura operacional.
- `functional-spec.md` — telas e fluxos.
- `parity.md` — contrato de paridade Android ↔ PWA.
- `speedtest-contract.md` — contrato do teste de conexão web.
- `diagnosis-contract.md` — contrato do diagnóstico.
- `history-model.md` — histórico local.
- `design-system.md` — design system operacional.
- `release-checklist.md` — checklist de PR e release.
- `ci-cd.md` — contrato atual de CI/CD e Pages.
- `deploy-status.md` — status real de deploy/domínio/Cloudflare.
- `qa-evidence.md` — evidências locais de QA e Lighthouse.
- `dns-doh-investigation.md` — conclusão browser-honesta da investigação DoH.
- `decisions.md` — decisões relevantes.
- `openapi.yaml` — contrato HTTP das rotas backend PWA em `/api/*`.

## Ordem sugerida

1. `m0-definition-of-done.md`
2. `architecture.md`
3. `parity.md`
4. `functional-spec.md`
5. `speedtest-contract.md`
6. `diagnosis-contract.md`
7. `design-system.md`
8. `release-checklist.md`
9. `ci-cd.md`
10. `history-model.md`
11. `deploy-status.md`
12. `qa-evidence.md`
13. `dns-doh-investigation.md`
14. `decisions.md`
15. `openapi.yaml`

## Fonte técnica de paridade

O documento `parity.md` deriva do contrato técnico existente em:

```text
docs_ai/technical/paridade-plataformas.md
```

Quando houver dúvida entre copiar comportamento Android ou adaptar para Web, consultar primeiro `parity.md`.

Outros documentos fonte que podem impactar a PWA:

- `docs_ai/technical/admin-api-schema.md`
- `docs_ai/technical/analytics-events.md`

## Padrão de branch

Preferido:

```text
codex/pwa/<sig-id>-<descricao-curta>
```

Fallback:

```text
codex-pwa-<sig-id>-<descricao-curta>
```

## Padrão de PR

```text
Codex PWA — <descrição>
```

## Escopo

O trabalho do PWA deve ficar dentro de `pwa/`, salvo aprovação explícita.
