# M0 — Definição de Pronto

## Objetivo

Definir o que significa considerar a fundação M0 do SignallQ PWA pronta.

M0 não é feature de produto. M0 é a fundação técnica mínima para permitir desenvolvimento seguro, revisável e sem dependência de Android.

## Escopo do M0

M0 cobre:

- criação da base React + TypeScript + Vite;
- scripts mínimos de desenvolvimento e build;
- estrutura inicial de pastas;
- app shell inicial;
- manifesto PWA básico;
- tokens visuais mínimos;
- README atualizado;
- build e typecheck funcionando.

## Estrutura base esperada

```text
pwa/
  package.json
  index.html
  vite.config.ts
  tsconfig.json
  public/
    manifest.webmanifest
  src/
    main.tsx
    App.tsx
    styles/
      tokens.css
      global.css
    components/
    features/
    hooks/
    lib/
    types/
```

## Scripts mínimos

O `package.json` deve ter pelo menos:

```json
{
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "typecheck": "tsc -b",
    "preview": "vite preview"
  }
}
```

`lint` e `test` são desejáveis, mas podem entrar em etapa separada se isso reduzir risco.

## App shell mínimo

O app inicial deve conter:

- tela base carregando no navegador;
- identidade textual SignallQ;
- layout mobile-first;
- estrutura visual simples;
- aviso de que o PWA está em desenvolvimento;
- nenhuma métrica falsa.

## PWA mínimo

M0 deve conter `public/manifest.webmanifest` básico com:

- name: `SignallQ`;
- short_name: `SignallQ`;
- display: `standalone`;
- theme_color;
- background_color.

Ícones podem ser placeholders documentados até existir pacote visual final.

Service Worker pode ficar fora do primeiro commit de setup se isso reduzir risco.

## Design mínimo

M0 deve incluir tokens mínimos:

- cor primária;
- fundo;
- superfície;
- texto principal;
- texto secundário;
- borda;
- espaçamentos;
- radius.

## Validação obrigatória

Antes do PR:

```bash
npm install
npm run typecheck
npm run build
```

Se existirem:

```bash
npm run lint
npm test
```

## Fora do escopo

- SpeedTest real.
- Diagnóstico IA.
- Histórico real.
- Login.
- D1.
- Telemetria.
- Compartilhamento de laudo.
- CI/CD completo.
- Mudança no Android.

## Critério de aceite

- PWA roda localmente com `npm run dev`.
- Build passa com `npm run build`.
- TypeScript passa com `npm run typecheck`.
- README informa como rodar.
- App shell aparece no navegador sem erro.
- PR altera apenas área PWA.

## Regra de corte

Se algo atrasar M0, cortar. M0 é fundação. Feature começa depois.
