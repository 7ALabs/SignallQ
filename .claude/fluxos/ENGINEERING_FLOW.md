# Engineering Flow for Agents

> **Fonte da verdade:** [`ai-governance/agents/*.md`](../../../ai-governance/agents/) e as políticas em [`ai-governance/policies/`](../../../ai-governance/policies/). Este arquivo é um resumo apontador.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Objetivos

- Código de alta qualidade nos módulos `:app`, `:core*`, `:feature*` (Android) e nos Workers Cloudflare.
- Seguir os padrões documentados em [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md) e [`docs_ai/ARQUITETURA/`](../../docs_ai/ARQUITETURA/).
- Código performático, testável e documentado.

## Workflow

1. **Recebimento** — Camilo recebe task pequena e clara da Claudete (ou direto do usuário em bugfix simples).
2. **Análise** — ler codebase via Read/Grep/Glob (ferramentas nativas; não há agente dedicado a busca).
3. **Planejamento** — mapear arquivos afetados, risco de regressão, ordem de execução.
4. **Implementação** — Camilo em Android (MVVM + Compose), Admin (React/TS) e Workers Cloudflare.
5. **Testes** — escrever/atualizar testes em `test/` e `androidTest/`.
6. **Build** — `./android/gradlew ktlintCheck detekt test assembleDebug` (mínimo aplicável).
7. **Handoff para Caio** — revisão independente de código, segurança, testes e regressão. Loop máx. 2 rodadas.

Skills de plataforma disponíveis: `/regras-android`, `/regras-diagnostico-rede`, `/motor-diagnostico`, `/padroes-compose`, `/cloudflare-d1-console`, `/protocolo-ci-android`, `/protocolo-ktlint`.

## Implementadores

| Agente | Responsabilidade |
|---|---|
| Camilo | Android (Kotlin, Compose, MVVM, Room, Coroutines, integração IA), Admin (React/TS/Vite/Tailwind), Workers Cloudflare |
| Juliana | Spec de UI/layout/microcopy quando a task é visual (não edita código Kotlin diretamente) |
| Gustavo | Especifica métricas e observabilidade quando aplicável (não edita código de app) |
| Caio | Revisão independente de código, segurança, testes, regressão — gate único de Done |

## Comandos de build

```bash
./android/gradlew ktlintCheck detekt   # análise estática
./android/gradlew test                  # testes unitários
./android/gradlew assembleDebug         # build debug
```

Em Windows: `.\android\gradlew.bat ...`.

## Referências

- [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md) — sistema de build, dependências
- [`docs_ai/ARQUITETURA/README.md`](../../docs_ai/ARQUITETURA/README.md) — arquitetura do sistema
- [`docs_ai/ARQUITETURA/MODULOS/`](../../docs_ai/ARQUITETURA/MODULOS/) — módulos e responsabilidades
- [`.claude/fluxos/TASK_BREAKDOWN.md`](TASK_BREAKDOWN.md) — decomposição de tasks
