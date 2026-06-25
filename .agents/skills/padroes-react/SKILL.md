---
description: Checklist de qualidade React/TypeScript para o PWA SignallQ — padrões de componente, hooks, tipos, estado e anti-padrões a evitar.
---

## Quando usar
Antes de implementar ou revisar código React/TypeScript no PWA (`pwa/`).

## Padrões obrigatórios

### Componentes
- Componentes funcionais com TypeScript tipado. Sem class components.
- Props explicitamente tipadas com interface ou type alias.
- Componente não recebe mais do que necessita — sem "god props".
- Um componente, uma responsabilidade.

### Estado e dados
- `useState` para estado local simples.
- `useReducer` para estado complexo com múltiplas transições.
- Nunca mutar estado diretamente — sempre retornar novo objeto/array.
- `useCallback` e `useMemo` apenas quando o custo de recomputação é real e mensurável.

### Hooks customizados
- Nome sempre começa com `use`.
- Extrair lógica repetida de componentes em hooks, não em helpers com side-effects.
- Hook não acessa DOM diretamente — isso fica no componente.

### TypeScript
- Sem `any` — usar `unknown` com type guard quando necessário.
- Sem `as` (type assertion) sem comentário explicando por quê.
- Interfaces para objetos de domínio (resultado de speedtest, estado de conexão).
- Enums para estados discretos (ConnectionStatus, SpeedtestPhase).

### Imports
- Imports absolutos via alias configurado no Vite (`@/`).
- Sem imports circulares.
- Sem import de módulo Android em código PWA.

## Anti-padrões
- ❌ `useEffect` com dependências vazias como substituto de "componentDidMount" sem entender o ciclo.
- ❌ Fetch de dados diretamente em Componente — usar hook customizado ou service.
- ❌ Estado global para dados locais — só usar contexto/store para estado genuinamente global.
- ❌ Hardcodar cor fora do Tailwind config.
- ❌ Conditional hooks (hooks dentro de if/for).

## Limites
- Esta skill orienta, não implementa.
- Implementação → Renan.
