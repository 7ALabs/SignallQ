# Checklist de PR e Release

## Checklist de PR

Todo PR PWA deve informar:

- issue relacionada;
- branch usada;
- área afetada;
- arquivos principais alterados;
- objetivo da mudança;
- fora do escopo;
- comandos rodados;
- resultado dos comandos;
- riscos;
- pendências;
- se tocou ou não fora de `pwa/`.

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

Título deve começar com:

```text
Codex PWA —
```

## Validação local mínima

Rodar o que existir:

```bash
npm run typecheck
npm run build
npm run lint
npm test
```

Se algum script não existir, declarar no PR.

## Bloqueadores de merge

- Alterou Android sem aprovação.
- Alterou CI/CD global sem aprovação.
- Build falha.
- Typecheck falha.
- Adicionou dependência sem justificar.
- Inventou métrica.
- Exposição de segredo no client.
- PR mistura feature com refatoração grande.
- PR muda contrato de diagnóstico sem documentar.

## Checklist PWA

Quando aplicável:

- Manifest válido.
- App abre em mobile.
- App abre em desktop.
- Layout não quebra em 360px.
- Estados de loading/erro/vazio/sucesso existem.
- Sem console debug desnecessário.
- Sem texto técnico sem explicação.

## Checklist Cloudflare Pages

Quando aplicável:

- build command definido;
- output directory definido;
- preview deploy validado;
- env vars revisadas;
- secrets fora do client;
- redirects/headers documentados.

## Checklist antes de produção

- Lighthouse rodado.
- QA Chrome desktop.
- QA Chrome Android.
- QA Safari/iOS quando possível.
- Teste de instalação PWA quando possível.
- Teste de histórico local quando existir.
- Teste de fallback da IA quando existir.
- README atualizado.
- `pwa/docs` atualizado.

## Template de resumo de PR

```md
## Objetivo

## Issue

## Área afetada

## Arquivos principais

## Fora do escopo

## Validação

- [ ] npm run typecheck
- [ ] npm run build
- [ ] npm run lint
- [ ] npm test

## Riscos

## Pendências

## Tocou fora de /pwa?

Não.
```

## Regra final

Sem validação, não é pronto.

Sem escopo claro, não é PR bom.

Sem documentação de contrato, não muda contrato.
