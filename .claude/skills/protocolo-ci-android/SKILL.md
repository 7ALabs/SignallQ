---
description: Protocolo de CI/CD e dependências Android — dependabot travado em action_required, mismatch kapt/kotlin-metadata-jvm, e a decisão de strict=false em required_status_checks. Consultar antes de mergear PR de dependabot ou investigar falha de build após bump de versão do Kotlin/Compose.
---

## Quando usar

Antes de declarar uma PR de dependabot "segura pra mergear", ao investigar falha de build depois
de um bump de `org.jetbrains.kotlin.plugin.compose` (ou Kotlin base), ou ao lidar com uma PR presa
em `mergeStateStatus: BEHIND` sem entender por quê.

---

## Regra de ouro

> **`mergeable: true` não significa CI validado. Sempre confira `gh run list` antes de confiar no status de uma PR de dependabot.**

---

## 1. Dependabot preso em `action_required`

GitHub exige aprovação manual pra rodar workflow em PR de bot/fork por política de segurança
(`Secret source: Dependabot` aparece no log do job). Isso pode deixar uma PR "mergeable" com todos
os checks aparentando verde, quando na real só um check irrelevante rodou (ex: Cloudflare Pages
numa PR que só toca `/android`) e o CI de verdade nunca executou no HEAD atual.

**Diagnóstico:**
```bash
gh run list --repo gmmattey/linka-android --branch <branch> --json status,conclusion,workflowName
```
Se o run mais recente tiver `conclusion: action_required`, aprove antes de confiar em qualquer
status:
```bash
gh api -X POST repos/gmmattey/linka-android/actions/runs/<run-id>/approve
```
Espere o resultado real antes de mergear.

## 2. Mismatch `kapt`/`kotlin-metadata-jvm` ao bumpar `kotlin.plugin.compose`

Bumpar `org.jetbrains.kotlin.plugin.compose` (ex: 2.3.21→2.4.0) faz o compilador emitir `@Metadata`
numa versão que a lib `kotlin-metadata-jvm` (transitiva de `androidx.room:room-compiler-processing`,
e também empacotada de forma shadada dentro do Dagger/Hilt) ainda não sabe ler. Quebra
`:<modulo>:kaptDebugKotlin` e `:app:hiltJavaCompileDebug` com erro de leitura de metadata, **não**
erro de compilação Kotlin.

**A hipótese óbvia ("bumpar o Kotlin base junto") não resolve** — o problema não é o compilador, é a
lib leitora de metadata bundlada pelo Room/Hilt, sem update disponível ainda.

**Fix:** forçar a versão certa de `kotlin-metadata-jvm` via `resolutionStrategy.force` nas configs
`kapt*`/`*annotationProcessor*`, centralizado no `android/build.gradle.kts` raiz (`subprojects`).
Já aplicado no repo com comentário "remover quando Room publicar versão compatível" — é gambiarra de
compatibilidade temporária, não solução definitiva. A solução limpa de verdade é migrar Room de
KAPT pra KSP (mudança maior, decisão de arquitetura, não fazer dentro de PR de dependabot).

Se um bump de Kotlin/Compose quebrar kapt/hilt de novo com erro parecido, comece a investigação
por aqui, com `--stacktrace`, antes de tentar bumpar Kotlin base "pra ver se resolve".

## 3. `strict=false` em `required_status_checks` de `main`

Decisão registrada em 2026-07-15 (`docs_ai/operations/PROCESSO_PR_E_AGENTES_2026-07-16.md`): Merge
Queue não está disponível pra conta pessoal (só orgs Team/Enterprise — confirmado via rejeição da
API de rulesets). Em vez de manter `strict: true` (que exige a branch 100% atualizada com `main`
sem nenhum mecanismo automático de re-sync), `strict` foi desligado. PRs mergeiam assim que os
checks obrigatórios (Detekt/Ktlint/Unit Tests) passam na própria branch, sem precisar estar
byte-a-byte atualizada com o `main` mais recente.

**Se uma PR aparecer `mergeStateStatus: BEHIND` mesmo assim:** normal em dia de merge concorrente
alto — é só reflexo de `main` ter avançado, não bloqueia merge com `strict=false`. Se quiser
atualizar mesmo assim (recomendado se a divergência for grande): `gh api -X PUT
repos/gmmattey/linka-android/pulls/<N>/update-branch`.

## Referência

Diagnóstico completo com números reais em
`docs_ai/operations/PROCESSO_PR_E_AGENTES_2026-07-16.md`. Ver também `/protocolo-ktlint` (suppressão
de regra Ktlint especificamente, escopo diferente deste).
