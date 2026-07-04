# Decisões Operacionais

## ADR-001 — SignallQ PWA como projeto separado

Data: 25/06/2026

### Decisão

Criar documentação e operação próprias para o SignallQ PWA, separadas da documentação geral do Android.

### Motivo

O PWA tem stack, limitações, backlog e entrega próprios.

### Consequência

O SignallQ continua como guarda-chuva, mas o PWA tem documentação operacional própria.

## ADR-002 — Custo zero como regra

Data: 25/06/2026

### Decisão

A versão inicial deve priorizar Cloudflare Pages, Workers e IndexedDB/local-first sempre que possível.

### Motivo

Validar produto e acelerar entrega sem abrir custo fixo cedo.

## ADR-003 — Sem chat livre no diagnóstico

Data: 25/06/2026

### Decisão

O PWA seguirá diagnóstico guiado e acionável, sem chat aberto como experiência principal.

### Motivo

Chat livre aumenta custo, reduz previsibilidade e pode deixar usuário comum perdido.

## ADR-004 — Repositório compartilhado Android + PWA

Data: 25/06/2026

### Decisão

Android e PWA continuam no mesmo repositório Git:

```text
gmmattey/linka-android
```

Android e PWA têm áreas de trabalho separadas por pasta, branch e PR.

### Regras

- PWA trabalha principalmente em `pwa/`.
- Android não deve alterar `pwa/` sem alinhamento.
- PWA não deve alterar Android sem alinhamento.
- Mudanças compartilhadas exigem revisão explícita.
- PRs devem ser pequenos, focados e rastreados.

## ADR-005 — Padrão Codex para branches, PRs e docs

Data: 25/06/2026

### Decisão

Branches e PRs do fluxo PWA/Codex devem começar com `codex`.

Formato preferido:

```text
codex/pwa/<sig-id>-<descricao-curta>
```

Fallback:

```text
codex-pwa-<sig-id>-<descricao-curta>
```

PRs devem começar com:

```text
Codex PWA —
```

### Motivo

Como o repositório é compartilhado com Android, o prefixo deixa claro que o fluxo pertence ao PWA/Codex.

### Documentação

Notion guarda contexto e decisões.

`pwa/docs` guarda instruções operacionais versionadas.

Quando uma regra do Notion impactar execução, ela deve ser transposta para `pwa/docs`.

## ADR-006 — Paridade PWA segue contrato Android/PWA

Data: 25/06/2026

### Decisão

A implementação PWA deve seguir o contrato de paridade derivado de:

```text
docs_ai/technical/paridade-plataformas.md
```

O documento operacional do PWA fica em:

```text
pwa/docs/parity.md
```

### Motivo

O Android possui recursos nativos que o browser não expõe. O PWA não deve prometer nem simular recursos impossíveis.

### Consequência

Cada feature relevante deve ser tratada como:

- equivalente;
- degradada;
- ausente;
- `n/a-browser`;
- `n/a-design`.

Features `n/a-browser` não entram no MVP e não podem aparecer como promessa de produto.

## ADR-007 — Telemetria PWA só após contrato seguro

Data: 25/06/2026

### Decisão

O PWA não instrumenta analytics nem ingestão no M0.

Quando instrumentar, deve seguir:

```text
docs_ai/technical/analytics-events.md
```

E qualquer ingestão administrativa deve respeitar:

```text
docs_ai/technical/admin-api-schema.md
```

### Motivo

O PWA roda no browser. Segredos como `INGEST_KEY` não podem ser expostos no client.

### Consequência

Qualquer envio futuro para Admin API precisa de Worker intermediário, token efêmero ou outro mecanismo seguro documentado.

Eventos futuros do PWA devem usar `snake_case`, sem PII, com `plataforma: "pwa"`.

### Atualização — 04/07/2026 (GH#441/GH#442)

O contrato ficou seguro e documentado (`SignallQ Admin/docs/architecture/data-architecture.md`,
SIG-295) e o proxy server-side (`pwa/functions/api/admin/ingest.ts`, já existia
mas nunca era chamado pela UI) cumpre exatamente a condição desta ADR: o
`ADMIN_INGEST_KEY` vive só na Cloudflare Pages Function, nunca no bundle do
navegador. O PWA agora chama `POST /api/admin/ingest` (`kind: "diagnostic"`)
ao final de cada teste (`pwa/src/App.tsx` → `sendAdminDiagnostic`), corrigindo
GH#441 (dados do WebApp não apareciam no Console). Cada payload leva
`platform: "web"` (campo em inglês, para bater com o contrato já existente
`environment`/`dist_channel`/`build_type` em `AdminDiagnosticPayload` — não
`plataforma: "pwa"` como um rascunho anterior desta ADR sugeria) para permitir
diferenciar Android vs. WebApp no Console (GH#442). Envio é fire-and-forget,
sem retry/fila local — mesma postura hoje aceita no Android (ver gap 1 de
`data-architecture.md`). Analytics de produto (`analytics_events`) e uso de IA
(`ai_usage`) continuam fora do MVP do PWA — o diagnóstico por IA chama o AI
Worker direto, sem persistir em `ai_usage`.

## ADR-008 — DNS benchmark real fora do MVP PWA

Data: 25/06/2026

### Decisão

DNS benchmark real fica fora do MVP PWA.

### Motivo

O browser não expõe `dns.resolve()` e fetch indireto não é confiável para benchmark real.

### Consequência

DNS pode voltar como checagem indireta ou via Worker/proxy dedicado, mas não deve ser prometido como benchmark real no MVP.
