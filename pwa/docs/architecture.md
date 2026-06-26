# Arquitetura Operacional

## Objetivo

Definir uma arquitetura prática para o SignallQ PWA, pronta para orientar implementação com React, TypeScript e Vite, considerando a coexistência com o Android, Admin Panel, Cloudflare Workers e contratos técnicos já existentes.

## Contexto de coexistência

O SignallQ possui múltiplos artefatos convivendo no mesmo ecossistema:

- Android nativo: Kotlin/Compose, com recursos de rede e hardware indisponíveis no browser.
- PWA: React/TypeScript/Vite, limitado por APIs web.
- Admin Panel: frontend administrativo.
- Workers Cloudflare: IA, Admin API, privacidade e ingestão.
- D1: banco do Admin Worker.
- Firebase: Analytics/Crashlytics/App Distribution no Android.

O PWA deve reaproveitar contratos e infraestrutura quando fizer sentido, mas não deve tentar copiar recursos nativos inviáveis no navegador.

## Stack PWA

- React.
- TypeScript.
- Vite.
- CSS simples no início.
- Cloudflare Pages para deploy.
- Cloudflare Workers quando houver backend/IA.
- IndexedDB para histórico local no MVP.
- Cloudflare D1 somente quando houver necessidade real via Worker.

## Princípios

- Custo zero como regra.
- Código do PWA restrito a `pwa/`.
- Sem dependência pesada sem justificativa.
- Não prometer no navegador o que depende de recurso nativo.
- Diagnóstico precisa ser honesto.
- TypeScript deve proteger contratos de dados.
- Commits e PRs pequenos.
- Paridade com Android deve seguir `pwa/docs/parity.md`.
- Segredos nunca entram no bundle do PWA.

## Estrutura alvo

```text
pwa/
  src/
    main.tsx
    App.tsx
    styles/
      tokens.css
      global.css
    components/
    features/
      landing/
      speedtest/
      signal/
      diagnosis/
      history/
      settings/
      about/
    hooks/
    lib/
      browser/
      speedtest/
      diagnosis/
      storage/
      admin-api/
      analytics/
    types/
      diagnosis.ts
      speedtest.ts
      history.ts
      parity.ts
  docs/
```

## Responsabilidades

### `src/components`

Componentes reutilizáveis, pequenos e sem regra de negócio pesada.

### `src/features`

Fluxos de produto. Cada feature pode ter componentes, hooks e helpers próprios.

Features iniciais:

- landing;
- speedtest;
- diagnosis;
- history;
- settings;
- about.

Feature degradada possível:

- signal.

Features fora do MVP por limitação web:

- devices;
- fibra/modem;
- DNS benchmark real.

### `src/lib`

Código técnico reutilizável:

- medição de rede;
- normalização de payload;
- storage;
- detecção de capacidades do browser;
- integração com Worker;
- cliente Admin API futuro;
- helpers de eventos futuros.

### `src/types`

Contratos TypeScript compartilhados entre features.

## Estratégia de dados

### M0

Sem persistência real.

### M1

Histórico local com IndexedDB.

### Pós-M1

D1 somente via Worker, se houver necessidade de:

- laudo compartilhável por link;
- sincronização;
- telemetria agregada;
- painel admin;
- ingestão estruturada.

## Admin API

A Admin API existente usa Cloudflare Worker e D1.

Fonte técnica:

```text
docs_ai/technical/admin-api-schema.md
```

Regras para o PWA:

- Não consumir `/admin/*` diretamente para uso de usuário final.
- Não expor `INGEST_KEY` no client.
- Qualquer ingestão futura do PWA precisa passar por endpoint seguro ou Worker intermediário.
- Se o PWA enviar diagnósticos para painel, o payload deve seguir contrato documentado e ser atualizado no mesmo PR.
- Respostas devem ser tratadas como `camelCase`, salvo endpoint explicitamente documentado como `snake_case`.

## Analytics e eventos

Fonte técnica:

```text
docs_ai/technical/analytics-events.md
```

Regras para o PWA:

- Não instrumentar analytics no M0.
- No MVP, eventos podem ser apenas planejados.
- Quando instrumentar, usar `snake_case`.
- Sem PII: não enviar SSID, IP público, BSSID, MAC ou identificadores sensíveis.
- Enums como string lowercase.
- Unidade explícita no nome do parâmetro: `_ms`, `_mbps`, `_pct`.
- Adicionar `plataforma: "pwa"` em eventos do PWA.

## Workers

Workers entram quando houver contrato claro.

Não chamar IA direto do browser se envolver segredo. Integração com IA deve passar por Worker intermediário.

## AI Diagnosis Worker

O PWA deve tratar IA como camada opcional.

Regras:

- Diagnóstico local deve funcionar sem IA.
- IA recebe payload estruturado.
- IA retorna JSON ou erro controlado.
- Falha de IA usa fallback local.
- Modelo primário/fallback deve seguir contrato real do Worker vigente no momento da implementação.

Observação: documentos técnicos atuais citam AI Provider Router com Cloudflare Workers AI e Gemini como providers. Antes de implementar a chamada real, validar endpoint, schema e ordem de provider no worker atual.

## Variáveis de ambiente

Variáveis `VITE_*` só podem conter dados públicos.

Segredos ficam em Cloudflare Workers, nunca no bundle do PWA.

## Diagnóstico

Separar:

- coleta de métricas;
- normalização;
- classificação local;
- payload para IA;
- resposta da IA;
- apresentação para usuário.

Não misturar tudo em componente React.

## Paridade com Android

O PWA deve classificar cada funcionalidade relevante como:

- equivalente;
- degradado;
- ausente;
- `n/a-browser`;
- `n/a-design`.

O contrato operacional está em:

```text
pwa/docs/parity.md
```

## Testes

M0:

- typecheck;
- build.

M1:

- testes unitários para classificação;
- testes para normalização de payload;
- testes para storage local.

M2/M3:

- QA cross-browser;
- Lighthouse;
- testes visuais manuais;
- cenários mínimos definidos em `release-checklist.md`.

## Decisões pendentes

- Router: React Router ou navegação simples por estado.
- Service Worker: M0 ou depois do app shell estabilizado.
- Upload: endpoint antes de implementar.
- D1: fora até necessidade real.
- Admin ingest PWA: só após contrato seguro, sem chave exposta no client.
- IA: confirmar endpoint e schema real do Worker antes da implementação.

## Recomendação inicial

Começar sem React Router e sem state manager global.

Usar componentes simples e estado local até o fluxo exigir mais.

Não implementar DNS benchmark real, scan Wi-Fi, scan de dispositivos, fibra/modem ou monitoramento passivo contínuo no MVP.