# Engineering Flow for Agents

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Versão: v0.23.0 · 2026-07-05.

## Objetivos

- Código de alta qualidade nos módulos `:app`, `:core*`, `:feature*` (Android) e no `SignallQ Admin/` (React/TS).
- Seguir padrões documentados em `technical/`.
- Código performático, testável e documentado.

## Workflow

1. **Recebimento** — o implementador recebe task pequena e clara da Claudete (ou direto do usuário em bugfix simples).
2. **Análise** — ler codebase via Read/Grep/Glob (ferramentas nativas; não há mais agente de busca).
3. **Planejamento** — mapear arquivos afetados, risco de regressão, ordem de execução.
4. **Implementação** — Camilo nos módulos Android (MVVM + Compose); Felipe no Admin Panel.
5. **Testes** — escrever/atualizar testes em `test/` e `androidTest/`.
6. **Build** — `.\android\gradlew.bat build`, lint, test.
7. **Handoff para Gema** — review + QA + release + higiene (changelog, bump de versão). Loop de correção: máximo 2 rodadas.

Skills de plataforma: `/regras-android`, `/regras-diagnostico-rede`, `/motor-diagnostico`, `/padroes-compose`.

## Implementadores

| Agente | Responsabilidade |
|---|---|
| Camilo | Android (Kotlin, Compose, MVVM, Room, Coroutines, integração IA) |
| Felipe | Admin Panel (React/TS/Vite/Tailwind) e análise de dados de app |
| Lia | UI/layout/microcopy quando a task é visual |
| Gema | Review, QA, regressão, release, higiene — gate único de Done |

## Comandos de build

```powershell
.\android\gradlew.bat build   # Build completo
.\android\gradlew.bat lint    # Análise estática
.\android\gradlew.bat test    # Testes unitários
```

## Referências

- `TECNICO.md` — sistema de build, dependências
- `ARQUITETURA/README.md` — arquitetura do sistema
- `TECNICO.md / ARQUITETURA/MODULOS/` — módulos e responsabilidades
- `ai/TASK_BREAKDOWN.md` — decomposição de tasks
