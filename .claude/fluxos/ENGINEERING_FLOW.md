# Engineering Flow

> **Fonte da verdade:** [`.claude/agents/camilo.md`](../agents/camilo.md).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Objetivos

- Código de alta qualidade em `:app`, `:core:*`, `:feature:*` (Android) e nos Workers Cloudflare.
- Padrões documentados em [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md) e [`docs_ai/ARQUITETURA/`](../../docs_ai/ARQUITETURA/).
- Zero duplicação — reuso via módulos `:core:*` compartilhados, nunca monolítico.

## Workflow (Camilo)

1. **Recebimento** — task via `/handoff` da Claudete (ou direto do Luiz em urgência).
2. **Inventário** — obrigatório antes de código novo: `/inventario` + `/verificar-modulo <nome>`. Se algo parecido existe, ou reusa, ou justifica.
3. **Análise** — Read/Grep/Glob para mapear arquivos afetados e risco de regressão.
4. **Implementação** — Android (Kotlin/Compose/MVVM), Web/Workers (TS), Admin (React/TS).
5. **Testes** — unitários e instrumentados; escreve antes de dizer "concluído".
6. **Build local** — `./android/gradlew ktlintCheck detekt test assembleDebug` (mínimo aplicável).
7. **Handoff para Caio** — `/handoff` com resultado dos checks. Loop máx 2 rodadas.
8. **`/check-done`** antes de fechar issue ou mergear.

Skills de plataforma: `/regras-android`, `/regras-diagnostico-rede`, `/motor-diagnostico`, `/padroes-compose`, `/cloudflare-d1-console`, `/protocolo-ci-android`, `/protocolo-ktlint`.

## Comandos de build

```bash
./android/gradlew ktlintCheck detekt   # análise estática
./android/gradlew test                  # testes unitários
./android/gradlew assembleDebug         # build debug
```

Windows: `.\android\gradlew.bat ...`.

## Model / effort (Camilo)

- Sonnet default (implementação, bugfix médio, refactor local, teste).
- Opus (arquitetura material, security-sensitive, contrato novo, migração com risco de regressão ampla).
- Haiku (bugfix trivial, chore de dependência, comentário de PR com contexto claro).

Regra: se o pior caso é "quebra 1 tela até o próximo commit", Haiku basta. Se é "quebra o app em produção", Opus.

## Referências

- [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md), [`docs_ai/ARQUITETURA/`](../../docs_ai/ARQUITETURA/)
- [`TASK_BREAKDOWN.md`](TASK_BREAKDOWN.md)
- [`.claude/rules/higiene-e-padronizacao-repositorio.md`](../rules/higiene-e-padronizacao-repositorio.md)
