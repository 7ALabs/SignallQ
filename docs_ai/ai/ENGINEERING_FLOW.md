# Engineering Flow for Agents

> Referência: `AGENTS.md` na raiz deste projeto.
> Codebase Android: módulos `app/`, `core*` e `feature*` na raiz.

## Objetivos

- Código de alta qualidade nos módulos `:app`, `:core*`, `:feature*`.
- Seguir padrões documentados em `technical/`.
- Código performático, testável e documentado.
- Aderência ao sistema de build (`technical/BUILD_SYSTEM.md`).

## Workflow de engenharia

1. **Recebimento**: agente recebe task pequena e clara do Cláudio (ou diretamente do usuário em bugfixes).
2. **Análise**: ler codebase via Grep e Read; delegar buscas simples ao Marcelo (Haiku).
3. **Planejamento**: mapear arquivos afetados, riscos de regressão, ordem de execução.
4. **Implementação**: codificar nos módulos corretos, seguindo MVVM e Compose patterns.
5. **Testes**: escrever ou atualizar testes em `androidTest/` e `test/`.
6. **Build**: `./gradlew build`, lint, `./gradlew test`.
7. **Documentação**: atualizar `docs_ai/` quando necessário; acionar Nina para changelog e bump de versão.

## Agentes de engenharia

| Agente | Responsabilidade |
|---|---|
| Cláudio | Arquitetura, decisões complexas, decomposição de tasks |
| Camilo | Implementação Android (Kotlin, Jetpack Compose, MD3, MVVM, Room, Coroutines) |
| Otávio | Validação de APIs Android, permissões, OEM quirks — obrigatório antes de Camilo em tasks com hardware/OS |
| Gema | Revisão de bugs, regressão, risco técnico — passo 5 do fluxo |
| Marcelo | Busca de código, grep de símbolos (Haiku — não edita código) |

## Comandos de build

```bash
./gradlew build          # Build completo
./gradlew lint           # Análise estática
./gradlew test           # Testes unitários
./gradlew archiveReleaseApk # APK release arquivado em builds/apk/release/<versionName>/
```

## Referências

- `technical/BUILD_SYSTEM.md` — sistema de build, dependências
- `technical/ARCHITECTURE.md` — arquitetura do sistema
- `technical/MODULES.md` — lista de módulos e responsabilidades
- `ai/TASK_BREAKDOWN.md` — regras de decomposição de tasks
