# Arquitetura Operacional

## Objetivo

Definir uma arquitetura prática para o SignallQ PWA, pronta para orientar implementação com React, TypeScript e Vite.

## Stack

- React.
- TypeScript.
- Vite.
- CSS simples no início.
- Cloudflare Pages para deploy.
- Cloudflare Workers quando houver backend/IA.
- Cloudflare D1 somente quando houver necessidade real.
- IndexedDB para histórico local no MVP.

## Princípios

- Custo zero como regra.
- Código do PWA restrito a `pwa/`.
- Sem dependência pesada sem justificativa.
- Não prometer no navegador o que depende de recurso nativo.
- Diagnóstico precisa ser honesto.
- TypeScript deve proteger contratos de dados.
- Commits e PRs pequenos.

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
    types/
      diagnosis.ts
      speedtest.ts
      history.ts
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

### `src/lib`

Código técnico reutilizável:

- medição de rede;
- normalização de payload;
- storage;
- detecção de capacidades do browser;
- integração com Worker no futuro.

### `src/types`

Contratos TypeScript compartilhados entre features.

## Estratégia de dados

### M0

Sem persistência real.

### M1

Histórico local com IndexedDB.

### Pós-M1

D1 somente se houver necessidade de:

- laudo compartilhável por link;
- sincronização;
- telemetria agregada;
- painel admin.

## Workers

Workers entram quando houver contrato claro.

Não chamar IA direto do browser se envolver segredo. Integração com IA deve passar por Worker intermediário.

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
- testes visuais manuais quando necessário.

## Decisões pendentes

- Router: React Router ou navegação simples por estado.
- Service Worker: M0 ou depois do app shell estabilizado.
- Upload: endpoint antes de implementar.
- D1: fora até necessidade real.

## Recomendação inicial

Começar sem React Router e sem state manager global.

Usar componentes simples e estado local até o fluxo exigir mais.
