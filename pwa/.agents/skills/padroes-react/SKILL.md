---
name: padroes-react
description: Use ao criar, revisar ou refatorar componentes, hooks, estado, tipos, módulos e estrutura React + TypeScript + Vite do SignallQ PWA. Não use para código Android.
---

# Padrões React — SignallQ PWA

## Objetivo

Manter o PWA simples, modular, tipado e fácil de revisar.

## Componentes

- Componentes pequenos e coesos.
- Props tipadas.
- Evitar componente gigante.
- Separar apresentação de lógica pesada.
- Preferir composition a condicionais enormes.
- Não colocar regra de negócio complexa direto no JSX.

## Hooks

- Use hooks para lógica reutilizável.
- Nomeie hooks com intenção clara.
- Encapsule side effects.
- Evite duplicar estado derivado.
- Limpe timers, listeners e abort controllers quando necessário.

## TypeScript

- Evitar `any` desnecessário.
- Tipar props, payloads e respostas externas.
- Preferir tipos pequenos e explícitos.
- Não criar abstração genérica sem uso real.

## Estado

- Estado local quando o uso for local.
- Não introduzir store global sem necessidade.
- Persistência local deve ser isolada em módulo próprio.
- Dados derivados devem ser calculados, não duplicados.

## Estrutura sugerida

- `src/components/` para componentes reutilizáveis.
- `src/features/` para fluxos de produto.
- `src/hooks/` para hooks compartilhados.
- `src/lib/` para utilitários e integrações.
- `src/types/` para tipos compartilhados.
- `src/styles/` para tokens/estilos globais quando necessário.

## Antes de alterar

Verifique:

- A tarefa pede feature ou refactor?
- Dá para resolver com mudança menor?
- O arquivo pertence ao PWA?
- Há impacto visual?
- Há impacto em diagnóstico?

## Validação

Rode o que existir:

- `npm run lint`
- `npm run typecheck`
- `npm run build`
- `npm test`

Se não existir, registre a pendência.

## Saída esperada

Ao usar esta skill, entregue:

- arquivos alterados;
- decisão técnica;
- riscos;
- validação executada;
- pendências.
